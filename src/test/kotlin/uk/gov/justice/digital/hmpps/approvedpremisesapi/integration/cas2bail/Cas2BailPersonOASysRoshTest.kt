package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2bail

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenceDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockSuccessfulOffenceDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockSuccessfulRoSHSummaryCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockUnsuccessfulRoshCallWithDelay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysSectionsTransformer

class Cas2BailPersonOASysRoshTest : IntegrationTestBase() {
  @Autowired
  lateinit var oaSysSectionsTransformer: OASysSectionsTransformer

  @Test
  fun `Getting cas2bail RoSH by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/cas2bail/people/CRN/oasys/rosh")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Gettingcas2bail  RoSH  for a CRN with an invalid auth-source JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "bananas",
    )

    webTestClient.get()
      .uri("/cas2bail/people/CRN/oasys/rosh")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting cas2bail RoSH for a CRN without ROLE_PROBATION or ROLE_POM returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "nomis",
      roles = listOf("ROLE_OTHER"),
    )

    webTestClient.get()
      .uri("/cas2bail/people/CRN/oasys/rosh")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting cas2bail Rosh for a CRN that does not exist returns 404`() {
    givenACas2PomUser { userEntity, jwt ->
      val crn = "CRN123"

      webTestClient.get()
        .uri("/cas2bail/people/$crn/oasys/rosh")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting cas2bail RoSH for a CRN returns OK with correct body`() {
    givenACas2PomUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val offenceDetails = OffenceDetailsFactory().produce()
        apOASysContextMockSuccessfulOffenceDetailsCall(offenderDetails.otherIds.crn, offenceDetails)

        val rosh = RoshSummaryFactory().produce()
        apOASysContextMockSuccessfulRoSHSummaryCall(offenderDetails.otherIds.crn, rosh)

        webTestClient.get()
          .uri("/cas2bail/people/${offenderDetails.otherIds.crn}/oasys/rosh")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              oaSysSectionsTransformer.transformRiskOfSeriousHarm(
                offenceDetails,
                rosh,
              ),
            ),
          )
      }
    }
  }

  @Test
  fun `Getting cas2bail RoSH when upstream times out returns 404`() {
    givenACas2PomUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val rosh = RoshSummaryFactory().produce()
        apOASysContextMockUnsuccessfulRoshCallWithDelay(offenderDetails.otherIds.crn, rosh, 2500)

        webTestClient.get()
          .uri("/cas2bail/people/${offenderDetails.otherIds.crn}/oasys/rosh")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }
}
