package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserRepository
import java.util.UUID

@Service
class ExternalUserService(
  private val httpAuthService: HttpAuthService,
  private val userRepository: ExternalUserRepository,
  private val manageUsersApiClient: ManageUsersApiClient,
) {
  fun getUserForRequest(): ExternalUserEntity {
    val authenticatedPrincipal = httpAuthService.getCas2AuthenticatedPrincipalOrThrow()
    val username = authenticatedPrincipal.name

    return getUserForUsername(username)
  }

  fun getUserForUsername(username: String): ExternalUserEntity {
    val normalisedUsername = username.uppercase()

    val existingUser = userRepository.findByUsername(normalisedUsername)
    if (existingUser != null) return existingUser

    val jwt = httpAuthService.getCas2AuthenticatedPrincipalOrThrow().token.tokenValue
    val externalUserDetailsResponse = manageUsersApiClient.getExternalUserDetails(normalisedUsername, jwt)

    val externalUserDetails = when (externalUserDetailsResponse) {
      is ClientResult.Success -> externalUserDetailsResponse.body
      is ClientResult.Failure -> externalUserDetailsResponse.throwException()
    }

    return userRepository.save(
      ExternalUserEntity(
        id = UUID.randomUUID(),
        username = externalUserDetails.username,
        isEnabled = externalUserDetails.enabled,
        name = "${externalUserDetails.firstName} ${externalUserDetails.lastName}",
        email = externalUserDetails.email,
        origin = "NACRO",
      ),
    )
  }
}
