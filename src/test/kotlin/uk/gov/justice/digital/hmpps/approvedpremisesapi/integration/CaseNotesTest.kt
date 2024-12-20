package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseNoteFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.caseNotesAPIMockSuccessfulCaseNotesCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNotesPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PrisonCaseNoteTransformer
import java.time.LocalDate

class CaseNotesTest : IntegrationTestBase() {
  @Autowired
  lateinit var caseNoteTransformer: PrisonCaseNoteTransformer

  @Test
  fun `Getting case notes without a JWT returns 401`() {
    webTestClient.get()
      .uri("/people/CRN/prison-case-notes")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting case notes with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis",
    )

    webTestClient.get()
      .uri("/people/CRN/prison-case-notes")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting case notes without ROLE_PROBATION returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
    )

    webTestClient.get()
      .uri("/people/CRN/prison-case-notes")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting case notes for a CRN that does not exist returns 404`() {
    givenAUser { userEntity, jwt ->
      val crn = "CRN345"

      wiremockServer.stubFor(
        WireMock.get(WireMock.urlEqualTo("/secure/offenders/crn/$crn"))
          .willReturn(
            WireMock.aResponse()
              .withHeader("Content-Type", "application/json")
              .withStatus(404),
          ),
      )

      webTestClient.get()
        .uri("/people/$crn/prison-case-notes")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting case notes for a CRN without a NOMS number returns 404`() {
    givenAUser { _, jwt ->
      givenAnOffender(
        offenderDetailsConfigBlock = {
          withNomsNumber(null)
        },
      ) { offenderDetails, _ ->

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/prison-case-notes")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }

  @Test
  fun `Getting case notes returns OK with correct body`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val caseNotes = listOf(
          CaseNoteFactory().produce(),
          CaseNoteFactory().produce(),
          CaseNoteFactory().produce(),
          CaseNoteFactory().produce(),
        )

        caseNotesAPIMockSuccessfulCaseNotesCall(
          0,
          LocalDate.now().minusDays(365),
          offenderDetails.otherIds.nomsNumber!!,
          CaseNotesPage(
            totalElements = 4,
            totalPages = 1,
            number = 1,
            content = caseNotes,
          ),
        )

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/prison-case-notes")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(caseNotes.map(caseNoteTransformer::transformModelToApi)),
          )
      }
    }
  }

  @Test
  fun `Getting case notes returns filtered results when call is for cas1 so getCas1SpecificNoteTypes is true`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->

        val caseNoteWithType = CaseNoteFactory()
          .withTypeDescription(null)
          .withType("Enforcement")
          .produce()
        val caseNoteWithTypeDescription = CaseNoteFactory()
          .withTypeDescription("Alert")
          .produce()

        val caseNotes = listOf(
          CaseNoteFactory().withType("Other").produce(),
          CaseNoteFactory().withType("Other").produce(),
          caseNoteWithType,
          CaseNoteFactory().withType("Other").produce(),
          caseNoteWithTypeDescription,
        )

        caseNotesAPIMockSuccessfulCaseNotesCall(
          0,
          LocalDate.now().minusDays(365),
          offenderDetails.otherIds.nomsNumber!!,
          CaseNotesPage(
            totalElements = 5,
            totalPages = 1,
            number = 1,
            content = caseNotes,
          ),
        )

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/prison-case-notes")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(listOf(caseNoteWithType, caseNoteWithTypeDescription).map(caseNoteTransformer::transformModelToApi)),
          )
      }
    }
  }
}
