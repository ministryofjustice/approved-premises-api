package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseNoteFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CaseNotesAPI_mockSuccessfulCaseNotesCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummaries
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
    `Given a User` { userEntity, jwt ->
      val crn = "CRN345"

      wiremockServer.stubFor(
        WireMock.get(WireMock.urlEqualTo("/probation-cases/summaries"))
          .withRequestBody(equalTo(objectMapper.writeValueAsString(listOf(crn))))
          .willReturn(
            WireMock.aResponse()
              .withHeader("Content-Type", "application/json")
              .withStatus(200)
              .withBody(objectMapper.writeValueAsString(CaseSummaries(listOf()))),
          ),
      )

      webTestClient.get()
        .uri("/people/$crn/prison-case-notes")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting case notes for a CRN without a NOMS number returns 404`() {
    `Given a User` { _, jwt ->
      `Given an Offender`(
        offenderDetailsConfigBlock = {
          withNomsNumber(null)
        },
      ) { offenderDetails, _ ->

        webTestClient.get()
          .uri("/people/${offenderDetails.case.crn}/prison-case-notes")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }

  @Test
  fun `Getting case notes returns OK with correct body`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val caseNotes = listOf(
          CaseNoteFactory().produce(),
          CaseNoteFactory().produce(),
          CaseNoteFactory().produce(),
          CaseNoteFactory().produce(),
        )

        CaseNotesAPI_mockSuccessfulCaseNotesCall(
          0,
          LocalDate.now().minusDays(365),
          offenderDetails.case.nomsId!!,
          CaseNotesPage(
            totalElements = 4,
            totalPages = 1,
            number = 1,
            content = caseNotes,
          ),
        )

        webTestClient.get()
          .uri("/people/${offenderDetails.case.crn}/prison-case-notes")
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
}
