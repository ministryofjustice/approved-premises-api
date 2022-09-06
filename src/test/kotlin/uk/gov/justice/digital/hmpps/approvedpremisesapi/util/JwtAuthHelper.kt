package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.time.Duration
import java.util.Date
import java.util.UUID

@Component
class JwtAuthHelper {
  private val keyPair: KeyPair

  init {
    val gen = KeyPairGenerator.getInstance("RSA")
    gen.initialize(2048)
    keyPair = gen.generateKeyPair()
  }

  @Bean
  fun jwtDecoder(): JwtDecoder = NimbusJwtDecoder.withPublicKey(keyPair.public as RSAPublicKey).build()

  internal fun createValidClientCredentialsJwt() = createClientCredentialsJwt(
    expiryTime = Duration.ofMinutes(2),
    roles = listOf("ROLE_COMMUNITY")
  )

  internal fun createExpiredClientCredentialsJwt() = createClientCredentialsJwt(
    expiryTime = Duration.ofMinutes(-2),
    roles = listOf("ROLE_COMMUNITY")
  )

  internal fun createClientCredentialsJwt(
    username: String? = null,
    scope: List<String>? = listOf(),
    roles: List<String>? = listOf(),
    authSource: String = if (username == null) "none" else "delius",
    expiryTime: Duration = Duration.ofHours(1),
    jwtId: String = UUID.randomUUID().toString()
  ): String =
    mutableMapOf<String, Any>()
      .also { it["user_name"] = username ?: "integration-test-client-id" }
      .also { it["client_id"] = "integration-test-client-id" }
      .also { it["grant_type"] = "client_credentials" }
      .also { it["auth_source"] = authSource }
      .also { roles?.let { roles -> it["authorities"] = roles } }
      .also { scope?.let { scope -> it["scope"] = scope } }
      .let {
        Jwts.builder()
          .setId(jwtId)
          .setSubject(username ?: "integration-test-client-id")
          .addClaims(it.toMap())
          .setExpiration(Date(System.currentTimeMillis() + expiryTime.toMillis()))
          .signWith(SignatureAlgorithm.RS256, keyPair.private)
          .compact()
      }

  internal fun createValidAuthorizationCodeJwt() = createAuthorizationCodeJwt(
    subject = "username",
    authSource = "delius",
    roles = listOf("ROLE_PROBATION")
  )

  internal fun createAuthorizationCodeJwt(
    subject: String,
    scope: List<String>? = listOf(),
    roles: List<String>? = listOf(),
    authSource: String = "delius",
    expiryTime: Duration = Duration.ofHours(1),
    jwtId: String = UUID.randomUUID().toString()
  ): String =
    mutableMapOf<String, Any>()
      .also { it["auth_source"] = authSource }
      .also { it["user_id"] = UUID.randomUUID().toString() }
      .also { roles?.let { roles -> it["authorities"] = roles } }
      .also { scope?.let { scope -> it["scope"] = scope } }
      .let {
        Jwts.builder()
          .setId(jwtId)
          .setSubject(subject)
          .addClaims(it.toMap())
          .setExpiration(Date(System.currentTimeMillis() + expiryTime.toMillis()))
          .signWith(SignatureAlgorithm.RS256, keyPair.private)
          .compact()
      }
}
