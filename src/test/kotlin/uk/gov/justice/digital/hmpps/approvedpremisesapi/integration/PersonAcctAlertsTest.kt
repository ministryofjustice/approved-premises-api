package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AlertFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockNotFoundOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.PrisonAPI_mockSuccessfulAlertsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AlertTransformer

class PersonAcctAlertsTest : IntegrationTestBase() {
  @Autowired
  lateinit var alertTransformer: AlertTransformer

  @Test
  fun `Getting ACCT alerts by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/people/CRN/acct-alerts")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting ACCT alerts for a CRN with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis"
    )

    webTestClient.get()
      .uri("/people/CRN/acct-alerts")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting ACCT alerts for a CRN without ROLE_PROBATION returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius"
    )

    webTestClient.get()
      .uri("/people/CRN/acct-alerts")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting ACCT alerts for a CRN that does not exist returns 404`() {
    `Given a User` { _, jwt ->
      val crn = "CRN123"

      CommunityAPI_mockNotFoundOffenderDetailsCall(crn)
      loadPreemptiveCacheForOffenderDetails(crn)

      webTestClient.get()
        .uri("/people/$crn/acct-alerts")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting ACCT alerts for a CRN returns OK with correct body`() {
    `Given a User` { _, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val alerts = listOf(
          AlertFactory().produce(),
          AlertFactory().produce(),
          AlertFactory().produce()
        )

        PrisonAPI_mockSuccessfulAlertsCall(offenderDetails.otherIds.nomsNumber!!, alerts)

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/acct-alerts")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              alerts.map(alertTransformer::transformToApi)
            )
          )
      }
    }
  }
}
