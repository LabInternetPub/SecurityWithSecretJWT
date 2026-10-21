package cat.tecnocampus.securityjwt.security.authorization;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.boot.security.autoconfigure.web.servlet.PathRequest.toH2Console;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfigurationAuthorization {

    //TODO add configuration so that
    // 1. /moderator and all the endpoints beginning with /moderator are accessible only by users with the role "MODERATOR"
    // 2. /moderator/whatever/admin are accessible by users with the role "ADMIN" (and also "MODERATOR"). "Whatever" is a single path element that can be anything.
    //    For example, /moderator/aaa/admin, /moderator/bbb/admin, /moderator/ccc/admin, are all accessible by users with the role "ADMIN" (and also "MODERATOR")
    //    but /moderator/aaa/bbb/admin is not accessible by users with the role "ADMIN" (Moderators can)

    // Login chain: only /token. The client sends "Authorization: Basic base64(username:password)"
    // and Spring Security checks it (UserLabDetailsService + PasswordEncoder) before the controller runs.
    @Bean
    @Order(1)
    SecurityFilterChain tokenFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/token")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    // API chain: every other request. Only "Authorization: Bearer <JWT>" is accepted (no HTTP Basic here).
    @Bean
    @Order(2)
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())   // uses the CorsConfigurationSource bean below
                .csrf(csrf -> csrf.disable())      // Safe because the browser does not attach the Authorization header automatically. If the token were sent in a cookie, CSRF protection would be needed
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions
                        .sameOrigin()))            // This is to allow the h2-console to be used in the browser (protection against clickjacking for other origins)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(toH2Console()).permitAll()
                        .requestMatchers("/error", "/helloWorld").permitAll()
                        .requestMatchers("/helloUser").hasRole("USER")
                        .requestMatchers("/helloAdmin").hasRole("ADMIN")
                        .requestMatchers("/helloUserAdmin").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))  // uses the JwtDecoder and JwtAuthenticationConverter beans
                .build();
    }

    // Origins (where the JavaScript front end is served from) allowed to call this API from a browser
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
