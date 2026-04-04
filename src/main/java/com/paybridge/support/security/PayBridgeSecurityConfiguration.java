package com.paybridge.support.security;

import com.paybridge.support.config.PayBridgeProperties;
import java.util.List;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class PayBridgeSecurityConfiguration {

    private final PayBridgeProperties payBridgeProperties;

    public PayBridgeSecurityConfiguration(PayBridgeProperties payBridgeProperties) {
        this.payBridgeProperties = payBridgeProperties;
    }

    @Bean
    SecurityFilterChain payBridgeSecurityFilterChain(HttpSecurity http) throws Exception {
        boolean nicePayOperatorOnly = payBridgeProperties.getFeatures().isNicepayLocalOnly();
        boolean operatorApiEnabled = payBridgeProperties.getFeatures().isOperatorApiEnabled();

        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                "/api/providers/stripe/webhooks",
                "/api/ops/**"
            ))
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll();
                auth.requestMatchers(
                        "/",
                        "/checkout",
                        "/operator/login",
                        "/error",
                        "/swagger-ui/**",
                        "/api-docs/**",
                        "/v3/api-docs/**",
                        "/actuator/health",
                        "/api/system/info")
                    .permitAll();
                auth.requestMatchers(HttpMethod.GET, "/payments/stripe/checkout", "/payments/stripe/return").permitAll();
                auth.requestMatchers(HttpMethod.POST, "/payments/stripe/payment-intent").permitAll();
                auth.requestMatchers(HttpMethod.POST, "/api/providers/stripe/webhooks").permitAll();
                if (!nicePayOperatorOnly) {
                    auth.requestMatchers(HttpMethod.GET, "/payments/nicepay/keyin").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/payments/nicepay/keyin/approve").permitAll();
                }
                if (operatorApiEnabled) {
                    auth.requestMatchers("/api/ops/**").hasRole("OPERATOR");
                } else {
                    auth.requestMatchers("/api/ops/**").denyAll();
                }
                auth.requestMatchers("/ops/**").hasRole("OPERATOR");
                auth.requestMatchers("/payments/**").hasRole("OPERATOR");
                auth.anyRequest().authenticated();
            })
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    new AntPathRequestMatcher("/api/ops/**")
                )
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/operator/login"),
                    new AntPathRequestMatcher("/ops/**")
                )
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/operator/login"),
                    new AntPathRequestMatcher("/payments/**")
                )
            )
            .formLogin(form -> form
                .loginPage("/operator/login")
                .loginProcessingUrl("/operator/login")
                .defaultSuccessUrl("/ops/transactions/search", true)
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/operator/logout")
                .logoutSuccessUrl("/operator/login?logout")
                .permitAll())
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        String username = payBridgeProperties.getSecurity().getOperatorUsername();
        String rawPassword = payBridgeProperties.getSecurity().getOperatorPassword();
        return new InMemoryUserDetailsManager(User.withUsername(username)
            .password(passwordEncoder.encode(rawPassword))
            .roles("OPERATOR")
            .build());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(payBridgeProperties.getSecurity().getConsoleAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Correlation-Id"));
        configuration.setExposedHeaders(List.of("X-Correlation-Id"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/ops/**", configuration);
        return source;
    }
}
