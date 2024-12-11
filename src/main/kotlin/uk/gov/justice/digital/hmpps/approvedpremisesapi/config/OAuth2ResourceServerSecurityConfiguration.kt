package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import java.time.Duration
import java.util.Base64

@Configuration
@EnableMethodSecurity
@EnableWebSecurity
class OAuth2ResourceServerSecurityConfiguration {
  @Bean
  @Throws(Exception::class)
  fun securityFilterChain(http: HttpSecurity, @Autowired objectMapper: ObjectMapper): SecurityFilterChain {
    http {
      csrf { disable() }

      authorizeHttpRequests {
        authorize(HttpMethod.GET, "/swagger-ui.html", permitAll)
        authorize(HttpMethod.GET, "/health/**", permitAll)
        authorize(HttpMethod.GET, "/swagger-ui/**", permitAll)
        authorize(HttpMethod.GET, "/v3/api-docs/**", permitAll)
        authorize(HttpMethod.GET, "/v3/api-docs.yaml", permitAll)
        authorize(HttpMethod.GET, "/api.yml", permitAll)
        authorize(HttpMethod.GET, "/cas1-api.yml", permitAll)
        authorize(HttpMethod.GET, "/cas2-api.yml", permitAll)
        authorize(HttpMethod.GET, "/_shared.yml", permitAll)
        authorize(HttpMethod.GET, "/domain-events-api.yml", permitAll)
        authorize(HttpMethod.GET, "/cas2-domain-events-api.yml", permitAll)
        authorize(HttpMethod.GET, "/favicon.ico", permitAll)
        authorize(HttpMethod.GET, "/info", permitAll)
        authorize(HttpMethod.POST, "/seed", permitAll)
        authorize(HttpMethod.DELETE, "/cache/*", permitAll)
        authorize(HttpMethod.POST, "/migration-job", permitAll)
        authorize(HttpMethod.DELETE, "/internal/premises/*", permitAll)
        authorize(HttpMethod.DELETE, "/internal/room/*", permitAll)
        authorize(HttpMethod.GET, "/events/cas2/**", hasAuthority("ROLE_CAS2_EVENTS"))
        authorize(HttpMethod.GET, "/events/**", hasAuthority("ROLE_APPROVED_PREMISES_EVENTS"))
        authorize(HttpMethod.PUT, "/cas2/assessments/**", hasRole("CAS2_ASSESSOR"))
        authorize(HttpMethod.GET, "/cas2/assessments/**", hasAnyRole("CAS2_ASSESSOR", "CAS2_ADMIN"))
        authorize(HttpMethod.POST, "/cas2/assessments/*/status-updates", hasRole("CAS2_ASSESSOR"))
        authorize(HttpMethod.POST, "/cas2/assessments/*/notes", hasAnyRole("LICENCE_CA", "POM", "CAS2_ASSESSOR"))
        authorize(HttpMethod.GET, "/cas2/submissions/**", hasAnyRole("CAS2_ASSESSOR", "CAS2_ADMIN"))
        authorize(HttpMethod.POST, "/cas2/submissions/*/status-updates", hasRole("CAS2_ASSESSOR"))
        authorize(HttpMethod.GET, "/cas2/reference-data/**", hasAnyRole("CAS2_ASSESSOR", "POM"))
        authorize(HttpMethod.GET, "/cas2/reports/**", hasRole("CAS2_MI"))
        authorize("/cas2/**", hasAnyAuthority("ROLE_POM", "ROLE_LICENCE_CA"))

        authorize(HttpMethod.PUT, "/cas2bail/assessments/**", hasRole("CAS2_ASSESSOR"))
        authorize(HttpMethod.GET, "/cas2bail/assessments/**", hasAnyRole("CAS2_ASSESSOR", "CAS2_ADMIN"))
        authorize(HttpMethod.POST, "/cas2bail/assessments/*/status-updates", hasRole("CAS2_ASSESSOR"))
        authorize(HttpMethod.POST, "/cas2bail/assessments/*/notes", hasAnyRole("LICENCE_CA", "POM", "CAS2_ASSESSOR"))
        authorize(HttpMethod.GET, "/cas2bail/submissions/**", hasAnyRole("CAS2_ASSESSOR", "CAS2_ADMIN"))
        authorize(HttpMethod.POST, "/cas2bail/submissions/*/status-updates", hasRole("CAS2_ASSESSOR"))
        authorize(HttpMethod.GET, "/cas2bail/reference-data/**", hasAnyRole("CAS2_ASSESSOR", "POM"))
        authorize(HttpMethod.GET, "/cas2bail/reports/**", hasRole("CAS2_MI"))
        authorize("/cas2bail/**", hasAnyAuthority("ROLE_POM", "ROLE_LICENCE_CA"))

        authorize(HttpMethod.GET, "/cas3-api.yml", permitAll)
        authorize(HttpMethod.GET, "/subject-access-request", hasAnyRole("SAR_DATA_ACCESS"))
        authorize(anyRequest, hasAuthority("ROLE_PROBATION"))
      }

      anonymous { disable() }

      oauth2ResourceServer {
        jwt { jwtAuthenticationConverter = AuthAwareTokenConverter() }

        authenticationEntryPoint = AuthenticationEntryPoint { _, response, _ ->
          response.apply {
            status = 401
            contentType = "application/problem+json"
            characterEncoding = "UTF-8"

            writer.write(
              objectMapper.writeValueAsString(
                object {
                  val title = "Unauthenticated"
                  val status = 401
                  val detail =
                    "A valid HMPPS Auth JWT must be supplied via bearer authentication to access this endpoint"
                },
              ),
            )
          }
        }
      }

      sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
    }

    return http.build()
  }
}

class AuthAwareTokenConverter() : Converter<Jwt, AbstractAuthenticationToken> {
  private val jwtGrantedAuthoritiesConverter: Converter<Jwt, Collection<GrantedAuthority>> =
    JwtGrantedAuthoritiesConverter()

  override fun convert(jwt: Jwt): AbstractAuthenticationToken {
    val claims = jwt.claims
    val principal = findPrincipal(claims)
    val authorities = extractAuthorities(jwt)
    return AuthAwareAuthenticationToken(jwt, principal, authorities)
  }

  private fun extractAuthSource(claims: Map<String, Any?>): String {
    return claims[CLAIM_AUTH_SOURCE] as String
  }

  private fun findPrincipal(claims: Map<String, Any?>): String {
    return if (claims.containsKey(CLAIM_USERNAME)) {
      claims[CLAIM_USERNAME] as String
    } else if (claims.containsKey(CLAIM_USER_ID)) {
      claims[CLAIM_USER_ID] as String
    } else if (claims.containsKey(CLAIM_CLIENT_ID)) {
      claims[CLAIM_CLIENT_ID] as String
    } else {
      throw RuntimeException("Unable to find a claim to identify Subject by")
    }
  }

  private fun extractAuthorities(jwt: Jwt): Collection<GrantedAuthority> {
    val authorities = mutableListOf<GrantedAuthority>().apply { addAll(jwtGrantedAuthoritiesConverter.convert(jwt)!!) }
    if (jwt.claims.containsKey(CLAIM_AUTHORITY)) {
      @Suppress("UNCHECKED_CAST")
      val claimAuthorities = when (val claims = jwt.claims[CLAIM_AUTHORITY]) {
        is String -> claims.split(',')
        is Collection<*> -> (claims as Collection<String>).toList()
        else -> emptyList()
      }
      authorities.addAll(claimAuthorities.map(::SimpleGrantedAuthority))
    }
    return authorities.toSet()
  }

  companion object {
    const val CLAIM_USERNAME = "user_name"
    const val CLAIM_USER_ID = "user_id"
    const val CLAIM_AUTH_SOURCE = "auth_source"
    const val CLAIM_CLIENT_ID = "client_id"
    const val CLAIM_AUTHORITY = "authorities"
  }
}

class AuthAwareAuthenticationToken(
  jwt: Jwt,
  private val aPrincipal: String,
  authorities: Collection<GrantedAuthority>,
) : JwtAuthenticationToken(jwt, authorities) {

  private val jwt = jwt

  override fun getPrincipal(): String {
    return aPrincipal
  }

  fun isExternalUser(): Boolean {
    return jwt.claims["auth_source"] == "auth"
  }
}

@Configuration
class AuthorizedClientServiceConfiguration(
  @Value("\${log-client-credentials-jwt-info}") private val logClintCredentialsJwtInfo: Boolean,
  private val clientRegistrationRepository: ClientRegistrationRepository,
  private val objectMapper: ObjectMapper,
) {
  @Bean
  fun inMemoryOAuth2AuthorizedClientService(): OAuth2AuthorizedClientService {
    if (logClintCredentialsJwtInfo) return LoggingInMemoryOAuth2AuthorizedClientService(clientRegistrationRepository, objectMapper)

    return InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository)
  }
}

class LoggingInMemoryOAuth2AuthorizedClientService(clientRegistrationRepository: ClientRegistrationRepository, private val objectMapper: ObjectMapper) : OAuth2AuthorizedClientService {
  private val backingImplementation = InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository)
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun <T : OAuth2AuthorizedClient?> loadAuthorizedClient(
    clientRegistrationId: String?,
    principalName: String?,
  ): T = backingImplementation.loadAuthorizedClient<T>(clientRegistrationId, principalName)

  override fun saveAuthorizedClient(authorizedClient: OAuth2AuthorizedClient?, principal: Authentication?) {
    val tokenValue = authorizedClient?.accessToken?.tokenValue

    if (tokenValue != null) {
      try {
        val tokenBodyBase64 = tokenValue.split(".")[1]
        val tokenBodyRaw = Base64.getDecoder().decode(tokenBodyBase64)
        val info = objectMapper.readValue(tokenBodyRaw, JwtLogInfo::class.java)
        log.info("Retrieved a client_credentials JWT for service->service calls for client ${authorizedClient.clientRegistration.clientId} with authorities: ${info.authorities}, scopes: ${info.scope}, expiry: ${info.exp}")
      } catch (exception: Exception) {
        // Deliberately not logging exception message
        log.error("Unable to get token info to log, exception of type: ${exception::class.java.name}")
      }
    }

    backingImplementation.saveAuthorizedClient(authorizedClient, principal)
  }

  override fun removeAuthorizedClient(clientRegistrationId: String?, principalName: String?) = backingImplementation.removeAuthorizedClient(clientRegistrationId, principalName)
}

@Configuration
class JwksCacheConfig {
  @Bean
  fun locallyCachedJwtDecoder(
    applicationContext: ApplicationContext,
    @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") jwkSetUri: String,
    @Value("\${caches.jwks.expiry-seconds}") jwksExpirySeconds: Long,
  ): JwtDecoder {
    val cache = CaffeineCache(
      "jwks",
      Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(jwksExpirySeconds))
        .build(),
    )

    return NimbusJwtDecoder
      .withJwkSetUri(jwkSetUri)
      .cache(cache)
      .build()
  }
}

data class JwtLogInfo(
  val authorities: List<String>,
  val scope: List<String>,
  val exp: Long,
)
