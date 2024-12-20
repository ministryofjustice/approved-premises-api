package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AlertFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonAPIMockSuccessfulAlertsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AlertTransformer

class PersonAcctAlertsTest : InitialiseDatabasePerClassTestBase() {
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
      authSource = "nomis",
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
      authSource = "delius",
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
    givenAUser { _, jwt ->
      val crn = "CRN123"

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
    givenAUser { _, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val alerts = listOf(
          AlertFactory().produce(),
          AlertFactory().produce(),
          AlertFactory().produce(),
        )

        prisonAPIMockSuccessfulAlertsCall(offenderDetails.otherIds.nomsNumber!!, alerts)

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/acct-alerts")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              alerts.map(alertTransformer::transformToApi),
            ),
          )
      }
    }
  }

  @Test
  fun `Getting ACCT alerts for a CRN without a NOMS number returns 404`() {
    givenAUser { _, jwt ->
      givenAnOffender(
        offenderDetailsConfigBlock = {
          withNomsNumber(null)
        },
      ) { offenderDetails, _ ->

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/acct-alerts")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }
}
