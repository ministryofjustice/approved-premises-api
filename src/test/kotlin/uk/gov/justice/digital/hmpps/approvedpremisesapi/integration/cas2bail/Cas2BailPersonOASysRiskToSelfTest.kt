package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2bail

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenceDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RiskToTheIndividualFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockSuccessfulOffenceDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockSuccessfulRiskToTheIndividualCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockUnsuccessfulRisksToTheIndividualCallWithDelay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.communityAPIMockNotFoundOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysSectionsTransformer

class Cas2BailPersonOASysRiskToSelfTest : IntegrationTestBase() {
  @Autowired
  lateinit var oaSysSectionsTransformer: OASysSectionsTransformer

  @Test
  fun `Getting cas2bail Risk to Self by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/cas2bail/people/CRN/oasys/risk-to-self")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting cas2bail Risk to Self for a CRN with an invalid auth-source JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "bananas",
    )

    webTestClient.get()
      .uri("/cas2bail/people/CRN/oasys/risk-to-self")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting cas2bail oasys sections for a CRN without ROLE_PROBATION or ROLE_POM returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
      roles = listOf("ROLE_OTHER"),
    )

    webTestClient.get()
      .uri("/cas2bail/people/CRN/oasys/risk-to-self")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting cas2bail Risk To Self for a CRN that does not exist returns 404`() {
    givenACas2PomUser { userEntity, jwt ->
      val crn = "CRN123"

      communityAPIMockNotFoundOffenderDetailsCall(crn)

      webTestClient.get()
        .uri("/cas2bail/people/$crn/oasys/risk-to-self")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting cas2bail Risk to Self for a CRN returns OK with correct body`() {
    givenACas2PomUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val offenceDetails = OffenceDetailsFactory().produce()
        apOASysContextMockSuccessfulOffenceDetailsCall(offenderDetails.otherIds.crn, offenceDetails)

        val risksToTheIndividual = RiskToTheIndividualFactory().produce()
        apOASysContextMockSuccessfulRiskToTheIndividualCall(offenderDetails.otherIds.crn, risksToTheIndividual)

        webTestClient.get()
          .uri("/cas2bail/people/${offenderDetails.otherIds.crn}/oasys/risk-to-self")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              oaSysSectionsTransformer.transformRiskToIndividual(
                offenceDetails,
                risksToTheIndividual,
              ),
            ),
          )
      }
    }
  }

  @Test
  fun `Getting cas2bail Risk to Self when upstream times out returns 404`() {
    givenACas2PomUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val risksToTheIndividual = RiskToTheIndividualFactory().produce()
        apOASysContextMockUnsuccessfulRisksToTheIndividualCallWithDelay(offenderDetails.otherIds.crn, risksToTheIndividual, 2500)

        webTestClient.get()
          .uri("/cas2bail/people/${offenderDetails.otherIds.crn}/oasys/risk-to-self")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }
}
