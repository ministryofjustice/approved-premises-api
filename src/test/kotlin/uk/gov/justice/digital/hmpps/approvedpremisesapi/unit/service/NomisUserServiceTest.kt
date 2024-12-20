package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.NomisUserRolesApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.NomisUserService

class NomisUserServiceTest {
  private val mockHttpAuthService = mockk<HttpAuthService>()
  private val mockNomisUserRolesApiClient = mockk<NomisUserRolesApiClient>()
  private val mockUserRepository = mockk<NomisUserRepository>()

  private val userService = NomisUserService(
    mockHttpAuthService,
    mockNomisUserRolesApiClient,
    mockUserRepository,
  )

  @Nested
  inner class GetUserForRequest {

    @Nested
    inner class WhenExistingUser {

      @Test
      fun `does not update user if Nomis-User-Roles API returns same email and activeCaseLoadId`() {
        val username = "SOMEPERSON"
        // setup auth service
        val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
        every { mockHttpAuthService.getNomisPrincipalOrThrow() } returns mockPrincipal
        every { mockPrincipal.token.tokenValue } returns "abc123"
        every { mockPrincipal.name } returns username

        // setup nomis roles api
        val oldUserData = NomisUserEntityFactory()
          .withNomisUsername(username)
          .withName("Bob Robson")
          .withEmail("same@example.com")
          .withActiveCaseloadId("123")
          .produce()

        val newUserData = NomisUserDetailFactory()
          .withFirstName("Jim")
          .withLastName("Jimmerson")
          .withEmail("same@example.com")
          .withActiveCaseloadId("123")
          .produce()

        every { mockNomisUserRolesApiClient.getUserDetails("abc123") } returns ClientResult.Success(
          HttpStatus.OK,
          newUserData,
        )
        // setup repository
        every { mockUserRepository.findByNomisUsername(username) } returns oldUserData
        verify(exactly = 0) { mockUserRepository.save(any()) }

        assertThat(userService.getUserForRequest()).matches {
          it.nomisUsername == username &&
            it.email == "same@example.com" &&
            it.activeCaseloadId == "123"
        }
      }

      @Test
      fun `updates user if Nomis-User-Roles API returns new data`() {
        val username = "SOMEPERSON"
        // setup auth service
        val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
        every { mockHttpAuthService.getNomisPrincipalOrThrow() } returns mockPrincipal
        every { mockPrincipal.token.tokenValue } returns "abc123"
        every { mockPrincipal.name } returns username

        // setup nomis roles api
        val oldUserData = NomisUserEntityFactory()
          .withNomisUsername(username)
          .withEmail("old@example.com")
          .withActiveCaseloadId("DEF")
          .produce()
        val newUserData = NomisUserDetailFactory()
          .withUsername(username)
          .withEmail("new@example.com")
          .withActiveCaseloadId("ABC")
          .produce()

        every { mockNomisUserRolesApiClient.getUserDetails("abc123") } returns ClientResult.Success(
          HttpStatus.OK,
          newUserData,
        )
        // setup repository
        every { mockUserRepository.findByNomisUsername(username) } returns oldUserData
        every { mockUserRepository.save(any()) } answers { it.invocation.args[0] as NomisUserEntity }

        assertThat(userService.getUserForRequest()).matches {
          it.nomisUsername == username &&
            it.email == "new@example.com" &&
            it.activeCaseloadId == "ABC"
        }
      }
    }

    @Nested
    inner class WhenNewUser {
      @Test
      fun `saves and returns new User with details from Nomis-User-Roles API`() {
        val username = "SOMEPERSON"
        // setup auth service
        val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
        every { mockHttpAuthService.getNomisPrincipalOrThrow() } returns mockPrincipal
        every { mockPrincipal.token.tokenValue } returns "abc123"
        every { mockPrincipal.name } returns username

        // setup nomis roles api
        val newUserData = NomisUserDetailFactory()
          .withUsername(username)
          .withFirstName("Jim")
          .withLastName("Jimmerson")
          .withStaffId(5678)
          .withAccountType("CLOSED")
          .withEmail("example@example.com")
          .withEnabled(false)
          .withActive(false)
          .withActiveCaseloadId("456")
          .produce()

        every { mockNomisUserRolesApiClient.getUserDetails("abc123") } returns ClientResult.Success(
          HttpStatus.OK,
          newUserData,
        )
        // setup repository
        every { mockUserRepository.findByNomisUsername(username) } returns null
        every { mockUserRepository.save(any()) } answers { it.invocation.args[0] as NomisUserEntity }

        assertThat(userService.getUserForRequest()).matches {
          it.nomisUsername == username &&
            it.name == "Jim Jimmerson" &&
            it.accountType == "CLOSED" &&
            it.nomisStaffId.toInt() == 5678 &&
            it.email == "example@example.com" &&
            !it.isEnabled &&
            !it.isActive &&
            it.activeCaseloadId == "456"
        }
      }
    }
  }
}
