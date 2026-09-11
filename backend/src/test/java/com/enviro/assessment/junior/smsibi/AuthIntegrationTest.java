package com.enviro.assessment.junior.smsibi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The real sign-in flow: session creation, generic failure messages, CSRF, lock-out, sign-out and security headers.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    private static final String LOGIN = "/api/auth/login";
    private static final String LOGOUT = "/api/auth/logout";
    private static final String THABO = "thabo.mokoena@example.com";
    private static final String SIPHO = "sipho.ndlovu@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Value("${app.seed.demo-password}")
    private String demoPassword;

    @Test
    void apiRequiresSignIn() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Please sign in to continue."));
    }

    @Test
    void csrfEndpointIssuesAReadableTokenCookie() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                // The React app must be able to read this cookie to copy it into the X-XSRF-TOKEN header.
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false));
    }

    @Test
    void signInStartsASessionForTheUser() throws Exception {
        MockHttpSession session = signIn(THABO, demoPassword);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(THABO))
                .andExpect(jsonPath("$.displayName").value("Thabo Mokoena"))
                .andExpect(jsonPath("$.role").value("INVESTOR"))
                .andExpect(jsonPath("$.investorId").isNumber());
    }

    @Test
    void usernameIsCaseInsensitive() throws Exception {
        assertThat(signIn("  Thabo.Mokoena@Example.COM", demoPassword)).isNotNull();
    }

    @Test
    void wrongPasswordAndUnknownUserGetTheSameGenericMessage() throws Exception {
        mockMvc.perform(formLogin(LOGIN).user(THABO).password("wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username or password."));

        mockMvc.perform(formLogin(LOGIN).user("nobody@example.com").password("whatever"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username or password."));
    }

    @Test
    void signInWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post(LOGIN).param("username", THABO).param("password", demoPassword))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Invalid CSRF token"));
    }

    @Test
    void repeatedFailuresLockTheUsernameEvenForTheCorrectPassword() throws Exception {
        for (int attempt = 1; attempt <= 4; attempt++) {
            mockMvc.perform(formLogin(LOGIN).user(SIPHO).password("wrong-" + attempt))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(formLogin(LOGIN).user(SIPHO).password("wrong-5"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.title").value("Account temporarily locked"));

        // While locked, even the right password is refused.
        mockMvc.perform(formLogin(LOGIN).user(SIPHO).password(demoPassword)).andExpect(status().isTooManyRequests());
    }

    @Test
    void signOutEndsTheSession() throws Exception {
        MockHttpSession session = signIn(THABO, demoPassword);

        mockMvc.perform(post(LOGOUT).session(session).with(csrf())).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void responsesCarrySecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"))
                .andExpect(header().string("Referrer-Policy", "same-origin"));
    }

    private MockHttpSession signIn(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(formLogin(LOGIN).user(username).password(password))
                .andExpect(status().isNoContent())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
