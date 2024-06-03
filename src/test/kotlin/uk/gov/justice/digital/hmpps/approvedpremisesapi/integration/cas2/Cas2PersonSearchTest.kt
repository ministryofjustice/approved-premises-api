package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationOffenderDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonApiMockSuccessfulInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.probationOffenderSearchApiMockForbiddenOffenderSearchCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.probationOffenderSearchApiMockNotFoundSearchCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.probationOffenderSearchApiMockServerErrorSearchCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.probationOffenderSearchApiMockSuccessfulOffenderSearchCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.probationoffendersearchapi.IDs
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.probationoffendersearchapi.OffenderProfile
import java.time.LocalDate

class Cas2PersonSearchTest : IntegrationTestBase() {
  @Nested
  inner class PeopleSearchGet {
    @Test
    fun `Searching by NOMIS ID without a JWT returns 401`() {
      webTestClient.get()
        .uri("/cas2/people/search?nomsNumber=nomsNumber").exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Searching for a NOMIS ID with a non-Delius or NOMIS JWT returns 403`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "other source",
      )

      webTestClient.get()
        .uri("/cas2/people/search?nomsNumber=nomsNumber")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Searching for a NOMIS ID without ROLE_POM returns 403`() {
      val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
        subject = "username",
        authSource = "nomis",
        roles = listOf("ROLE_OTHER"),
      )

      webTestClient.get()
        .uri("/cas2/people/search?nomsNumber=nomsNumber")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Searching for a NOMIS ID that does not exist returns 404`() {
      mockClientCredentialsJwtRequest()

      givenACas2PomUser { userEntity, jwt ->
        wiremockServer.stubFor(
          get(WireMock.urlEqualTo("/search?nomsNumber=nomsNumber"))
            .willReturn(
              aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(404),
            ),
        )

        webTestClient.get()
          .uri("/cas2/people/search?nomsNumber=nomsNumber")
          .header("Authorization", "Bearer $jwt")
          .exchange().expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `Searching for a NOMIS ID returns OK with correct body`() {
      givenACas2PomUser { userEntity, jwt ->
        val offender = ProbationOffenderDetailFactory()
          .withOtherIds(IDs(crn = "CRN", nomsNumber = "NOMS321", pncNumber = "PNC123"))
          .withFirstName("James")
          .withSurname("Someone")
          .withDateOfBirth(
            LocalDate
              .parse("1985-05-05"),
          )
          .withGender("Male")
          .withOffenderProfile(OffenderProfile(nationality = "English"))
          .produce()

        val inmateDetail = InmateDetailFactory().withOffenderNo("NOMS321")
          .withCustodyStatus(InmateStatus.IN)
          .withAssignedLivingUnit(
            AssignedLivingUnit(
              agencyId = "BRI",
              locationId = 5,
              description = "B-2F-004",
              agencyName = "HMP Bristol",
            ),
          )
          .produce()

        probationOffenderSearchApiMockSuccessfulOffenderSearchCall("NOMS321", listOf(offender))
        prisonApiMockSuccessfulInmateDetailsCall(inmateDetail = inmateDetail)

        webTestClient.get()
          .uri("/cas2/people/search?nomsNumber=NOMS321")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              FullPerson(
                type = PersonType.fullPerson,
                crn = "CRN",
                name = "James Someone",
                dateOfBirth = LocalDate.parse("1985-05-05"),
                sex = "Male",
                status = PersonStatus.inCustody,
                nomsNumber = "NOMS321",
                pncNumber = "PNC123",
                nationality = "English",
                isRestricted = false,
                prisonName = "HMP Bristol",
              ),
            ),
          )
      }
    }

    @Test
    fun `Searching for a NOMIS ID returns Unauthorised error when it is unauthorized`() {
      givenACas2PomUser { userEntity, jwt ->
        probationOffenderSearchApiMockForbiddenOffenderSearchCall()

        webTestClient.get()
          .uri("/cas2/people/search?nomsNumber=NOMS321")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Searching for a NOMIS ID returns Unauthorised error when it is not found`() {
      givenACas2PomUser { userEntity, jwt ->
        probationOffenderSearchApiMockNotFoundSearchCall()

        webTestClient.get()
          .uri("/cas2/people/search?nomsNumber=NOMS321")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `Searching for a NOMIS ID returns server error when there is a server error`() {
      givenACas2PomUser { userEntity, jwt ->
        probationOffenderSearchApiMockServerErrorSearchCall()

        webTestClient.get()
          .uri("/cas2/people/search?nomsNumber=NOMS321")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .is5xxServerError
      }
    }
  }
}
