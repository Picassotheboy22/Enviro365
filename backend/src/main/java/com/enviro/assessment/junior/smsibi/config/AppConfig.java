package com.enviro.assessment.junior.smsibi.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info =
                @Info(
                        title = "Enviro365 Withdrawal Notice API",
                        version = "2.0",
                        description = "View investor portfolios, submit withdrawal notices and export CSV statements. "
                                + "Sign in first with POST /api/auth/login (form fields username and password); "
                                + "the session cookie is then sent automatically."),
        security = @SecurityRequirement(name = "session"))
@SecurityScheme(
        name = "session",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.COOKIE,
        paramName = "JSESSIONID",
        description = "Session cookie issued by POST /api/auth/login")
public class AppConfig {

    /**
     * The current time is injected as a Clock bean instead of calling {@code LocalDate.now()} directly. Production
     * uses the system clock; tests pass a fixed clock, so date-dependent rules (the age check, the sign-in lock-out)
     * give the same result every time the tests run.
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
