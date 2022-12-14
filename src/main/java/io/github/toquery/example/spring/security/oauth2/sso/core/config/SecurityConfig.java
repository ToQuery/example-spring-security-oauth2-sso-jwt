package io.github.toquery.example.spring.security.oauth2.sso.core.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.toquery.example.spring.security.oauth2.sso.core.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import io.github.toquery.example.spring.security.oauth2.sso.core.oauth2.user.AppOAuth2UserService;
import io.github.toquery.example.spring.security.oauth2.sso.core.oauth2.user.AppOidcUserService;
import io.github.toquery.example.spring.security.oauth2.sso.core.properties.AppProperties;
import io.github.toquery.example.spring.security.oauth2.sso.core.security.AppAccessDeniedHandler;
import io.github.toquery.example.spring.security.oauth2.sso.core.security.AppAuthenticationEntryPoint;
import io.github.toquery.example.spring.security.oauth2.sso.core.security.AppLogoutSuccessHandler;
import io.github.toquery.example.spring.security.oauth2.sso.core.security.AppOAuth2AuthenticationFailureHandler;
import io.github.toquery.example.spring.security.oauth2.sso.core.security.AppOAuth2AuthenticationSuccessHandler;
import io.github.toquery.example.spring.security.oauth2.sso.core.security.AppSecurityContextRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;

/**
 *
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true, prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public AuthenticationEntryPoint appAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new AppAuthenticationEntryPoint(objectMapper);
    }


    @Bean
    public LogoutSuccessHandler logoutSuccessHandler(ObjectMapper objectMapper) {
        return new AppLogoutSuccessHandler(objectMapper);
    }

    /**
     * ??????????????????
     */
    @Bean
    @ConditionalOnMissingBean
    public SecurityContextRepository securityContextRepository() {
        return new AppSecurityContextRepository();
    }


    @Bean
    @Order(1)
    public SecurityFilterChain oauth2LoginSecurityFilterChain(
            HttpSecurity http,
            CorsConfiguration corsConfiguration,
            LogoutSuccessHandler logoutSuccessHandler,
            AuthenticationEntryPoint authenticationEntryPoint,
            SecurityContextRepository securityContextRepository,
            BearerTokenResolver bearerTokenResolver,
            AccessDeniedHandler accessDeniedHandler,
            AuthenticationEntryPoint appAuthenticationEntryPoint,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            OAuth2UserService<OidcUserRequest, OidcUser> appOidcUserService,
            OAuth2UserService<OAuth2UserRequest, OAuth2User> appOAuth2UserService,
            AuthenticationSuccessHandler appOAuth2AuthenticationSuccessHandler,
            AuthenticationFailureHandler appOAuth2AuthenticationFailureHandler,
            AuthorizationRequestRepository<OAuth2AuthorizationRequest> httpCookieOAuth2AuthorizationRequestRepository
    ) throws Exception {

        http.cors(corsConfigurer -> {
            corsConfigurer.configurationSource(exchange -> corsConfiguration);
        });

        http.csrf(AbstractHttpConfigurer::disable);
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);

        http.sessionManagement(httpSecuritySessionManagementConfigurer -> {
            httpSecuritySessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        });

        // security ?????????
        http.securityContext(httpSecuritySecurityContextConfigurer -> {
            httpSecuritySecurityContextConfigurer.requireExplicitSave(true);
            // ????????????????????????
            httpSecuritySecurityContextConfigurer.securityContextRepository(securityContextRepository);
        });

        http.authorizeHttpRequests(authorizationManagerRequestMatcherRegistry -> {
            // ?????????
            authorizationManagerRequestMatcherRegistry.requestMatchers("/actuator", "/actuator/*").permitAll();
            authorizationManagerRequestMatcherRegistry.requestMatchers("/", "/error").permitAll();
            authorizationManagerRequestMatcherRegistry.requestMatchers("/favicon.ico", "/*/*.png", "/*/*.gif", "/*/*.svg", "/*/*.jpg", "/*/*.html", "/*/*.css", "/*/*.js").permitAll();

            // authorizationManagerRequestMatcherRegistry.requestMatchers("/oauth2/authorization/*").permitAll();

            authorizationManagerRequestMatcherRegistry.anyRequest().authenticated();
        });

        http.exceptionHandling(httpSecurityExceptionHandlingConfigurer -> {
            httpSecurityExceptionHandlingConfigurer.authenticationEntryPoint(appAuthenticationEntryPoint);
            httpSecurityExceptionHandlingConfigurer.authenticationEntryPoint(authenticationEntryPoint);
        });

        // ????????????
        http.logout(logoutConfigurer -> {
            logoutConfigurer.logoutSuccessHandler(logoutSuccessHandler);
        });

        http.oauth2Login(oauth2LoginConfigurer -> {
            oauth2LoginConfigurer.loginPage("/oauth2/authorization/toquery");

            oauth2LoginConfigurer.authorizationEndpoint(authorizationEndpointConfig -> {
                authorizationEndpointConfig.authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository);
            });

            oauth2LoginConfigurer.userInfoEndpoint(userInfoEndpointConfig -> {
                userInfoEndpointConfig.userService(appOAuth2UserService);
                userInfoEndpointConfig.oidcUserService(appOidcUserService);
            });

            oauth2LoginConfigurer.successHandler(appOAuth2AuthenticationSuccessHandler);
            oauth2LoginConfigurer.failureHandler(appOAuth2AuthenticationFailureHandler);
        });

        //
        http.oauth2ResourceServer(auth2ResourceServerConfigurer -> {
            // ?????? bearerToken?????????????????????header???param???body?????????
            auth2ResourceServerConfigurer.bearerTokenResolver(bearerTokenResolver);
            auth2ResourceServerConfigurer.accessDeniedHandler(accessDeniedHandler);
            // ???????????????????????????
            auth2ResourceServerConfigurer.authenticationEntryPoint(appAuthenticationEntryPoint);

            auth2ResourceServerConfigurer.jwt(jwtConfigurer -> {
                jwtConfigurer.jwtAuthenticationConverter(jwtAuthenticationConverter);
            });
        });

        return http.build();
    }


    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> appOAuth2UserService() {
        return new AppOAuth2UserService();
    }

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> appOidcUserService() {
        return new AppOidcUserService();
    }


    @Bean
    public HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository() {
        return new HttpCookieOAuth2AuthorizationRequestRepository();
    }

    @Bean
    public AuthenticationFailureHandler appOAuth2AuthenticationFailureHandler(
            HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository
    ) {
        return new AppOAuth2AuthenticationFailureHandler(httpCookieOAuth2AuthorizationRequestRepository);
    }


    @Bean
    public AuthenticationSuccessHandler appOAuth2AuthenticationSuccessHandler(
            AppProperties appProperties,
            HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository
    ) {
        return new AppOAuth2AuthenticationSuccessHandler(appProperties, httpCookieOAuth2AuthorizationRequestRepository);
    }


    @Bean
    public AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return new AppAccessDeniedHandler(objectMapper);
    }

    /**
     * ???request??????????????????????????????token
     */
    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver bearerTokenResolver = new DefaultBearerTokenResolver();
        // ???????????????uri?????????????????????token
        bearerTokenResolver.setAllowUriQueryParameter(true);
        bearerTokenResolver.setAllowFormEncodedBodyParameter(true);
        return bearerTokenResolver;
    }


    /**
     * ??? JWT ??? scope ?????????????????? ?????? SCOPE_ ?????????
     * ????????? jwt claim ???????????????????????????
     * ?????????????????????????????????????????????????????????url?????????????????????????????????????????????jwtAuthenticationConverter()?????????????????????
     *
     * @return JwtAuthenticationConverter
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // ?????? SCOPE_ ?????????
        // authoritiesConverter.setAuthorityPrefix("");
        // ???jwt claim ?????????????????????????????????????????? scope ??? scp ???????????????
        authoritiesConverter.setAuthoritiesClaimName("scope");
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

}
