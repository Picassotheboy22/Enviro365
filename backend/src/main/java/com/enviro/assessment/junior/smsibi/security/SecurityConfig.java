package com.enviro.assessment.junior.smsibi.security;

import com.enviro.assessment.junior.smsibi.entity.UserAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * How the API is secured.
 *
 * <ul>
 *   <li><b>Authentication:</b> a server-side session. The React app posts the username and password to
 *       {@code /api/auth/login}; Spring checks them against the BCrypt hash and keeps the user in the HTTP session.
 *       The browser only holds an opaque, HttpOnly session id that JavaScript cannot read, so an XSS bug cannot
 *       steal a token.</li>
 *   <li><b>CSRF:</b> browsers send cookies automatically, so another website could try to submit a withdrawal with
 *       the victim's session. Spring puts a random token in a readable {@code XSRF-TOKEN} cookie and every POST must
 *       echo it in an {@code X-XSRF-TOKEN} header. A foreign site can make the browser <em>send</em> our cookies but
 *       cannot <em>read</em> them, so it cannot produce the header.</li>
 *   <li><b>Authorisation:</b> URL rules per role below, plus ownership checks in the services ({@link AccessGuard}).</li>
 *   <li><b>Errors:</b> 401/403/429 go to the GlobalExceptionHandler, so they use the same Problem Details JSON as
 *       every other error, instead of Spring's default redirect to an HTML login page.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    public static final String LOGIN_URL = "/api/auth/login";
    public static final String LOGOUT_URL = "/api/auth/logout";

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final LoginAttemptService loginAttempts;
    private final HandlerExceptionResolver exceptionResolver;

    public SecurityConfig(
            LoginAttemptService loginAttempts,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.loginAttempts = loginAttempts;
        this.exceptionResolver = exceptionResolver;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
                // spa(): token in a JavaScript-readable XSRF-TOKEN cookie, expected back in the X-XSRF-TOKEN header.
                // The H2 console (dev only, staff only) posts its own forms, so it is excluded.
                .csrf(csrf -> csrf.spa().ignoringRequestMatchers("/h2-console/**"))
                // The first matching rule wins, so specific rules come before general ones.
                .authorizeHttpRequests(auth -> auth.requestMatchers("/error")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf")
                        .permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .requestMatchers("/h2-console/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/investors", "/api/dashboard")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/withdrawals")
                        .hasRole("INVESTOR")
                        // The notice workflow: staff review and pay notices; only investors cancel their own.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/withdrawals/*/approve",
                                "/api/withdrawals/*/reject",
                                "/api/withdrawals/*/pay")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/withdrawals/*/cancel")
                        .hasRole("INVESTOR")
                        .requestMatchers("/api/**")
                        .authenticated()
                        // Deny by default: anything not listed above is refused.
                        .anyRequest()
                        .denyAll())
                .formLogin(form -> form
                        // Pointing loginPage at the API disables Spring's generated HTML login page;
                        // the React app has its own.
                        .loginPage(LOGIN_URL)
                        .loginProcessingUrl(LOGIN_URL)
                        .successHandler((request, response, authentication) ->
                                onLoginSuccess(authentication.getName(), response))
                        .failureHandler(this::onLoginFailure)
                        .permitAll())
                .logout(logout -> logout.logoutUrl(LOGOUT_URL)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .exceptionHandling(exceptions -> exceptions
                        // Not signed in: 401 JSON rather than a redirect to a login page.
                        .authenticationEntryPoint((request, response, ex) ->
                                exceptionResolver.resolveException(request, response, null, ex))
                        // Signed in but not allowed, or a bad CSRF token: 403 JSON.
                        .accessDeniedHandler((request, response, ex) ->
                                exceptionResolver.resolveException(request, response, null, ex)))
                .headers(headers -> headers
                        // Spring already sends X-Content-Type-Options, Cache-Control and similar headers.
                        // SAMEORIGIN (rather than DENY) lets the dev-only H2 console use its frames.
                        .frameOptions(frame -> frame.sameOrigin())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.SAME_ORIGIN)));
        return http.build();
    }

    /**
     * Delegating encoder: new passwords are hashed with BCrypt (slow and salted, so leaked hashes are expensive to
     * crack) and stored as "{bcrypt}...", which leaves room to change algorithm later.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Only needed when the UI calls the API from another origin (in development Vite proxies /api, so the browser
     * sees one origin). With cookies involved the origins must be listed explicitly: "*" is not allowed together
     * with credentials.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST"));
        config.setAllowedHeaders(List.of(HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, "X-XSRF-TOKEN"));
        config.setExposedHeaders(List.of(HttpHeaders.CONTENT_DISPOSITION, HttpHeaders.LOCATION));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private void onLoginSuccess(String username, HttpServletResponse response) {
        loginAttempts.recordSuccess(username);
        log.info("Sign-in succeeded for {}", forLog(username));
        // 204: the session cookie is already set. The UI then calls GET /api/auth/me for the user details.
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    private void onLoginFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        String username = UserAccount.normaliseUsername(request.getParameter("username"));
        // Attempts made while already locked do not extend the lock.
        if (!loginAttempts.isBlocked(username)) {
            loginAttempts.recordFailure(username);
        }
        AuthenticationException result = loginAttempts
                .remainingLock(username)
                .<AuthenticationException>map(TooManyLoginAttemptsException::new)
                .orElse(exception);
        log.warn(
                "Sign-in failed for {} ({})",
                forLog(username),
                result.getClass().getSimpleName());
        exceptionResolver.resolveException(request, response, null, result);
    }

    /** Usernames are user input: strip line breaks so nobody can forge extra log lines ("log injection"). */
    private static String forLog(String value) {
        String cleaned = value == null ? "" : value.replaceAll("[\\r\\n\\t]", "_");
        return cleaned.length() > 100 ? cleaned.substring(0, 100) + "..." : cleaned;
    }
}
