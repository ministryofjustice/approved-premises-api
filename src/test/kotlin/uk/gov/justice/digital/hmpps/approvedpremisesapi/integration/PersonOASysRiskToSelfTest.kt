package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenceDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RiskToTheIndividualFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockSuccessfulOffenceDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockSuccessfulRiskToTheIndividualCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockUnsuccessfulRisksToTheIndividualCallWithDelay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysSectionsTransformer

class PersonOASysRiskToSelfTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var oaSysSectionsTransformer: OASysSectionsTransformer

  @Test
  fun `Getting Risk to Self by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/people/CRN/oasys/risk-to-self")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting Risk to Self  for a CRN with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis",
    )

    webTestClient.get()
      .uri("/people/CRN/oasys/risk-to-self")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting oasys sections for a CRN without ROLE_PROBATION returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
      roles = listOf("ROLE_OTHER"),
    )

    webTestClient.get()
      .uri("/people/CRN/oasys/risk-to-self")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting oasys sections for a CRN with ROLE_POM returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
      roles = listOf("ROLE_POM"),
    )

    webTestClient.get()
      .uri("/people/CRN/oasys/risk-to-self")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting Risk To Self for a CRN that does not exist returns 404`() {
    givenAUser { userEntity, jwt ->
      val crn = "CRN123"

      webTestClient.get()
        .uri("/people/$crn/oasys/risk-to-self")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting Risk to Self for a CRN returns OK with correct body`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val offenceDetails = OffenceDetailsFactory().produce()
        apOASysContextMockSuccessfulOffenceDetailsCall(offenderDetails.otherIds.crn, offenceDetails)

        val risksToTheIndividual = RiskToTheIndividualFactory().produce()
        apOASysContextMockSuccessfulRiskToTheIndividualCall(offenderDetails.otherIds.crn, risksToTheIndividual)

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/oasys/risk-to-self")
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
  fun `Getting Risk to Self when upstream times out returns 404`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val risksToTheIndividual = RiskToTheIndividualFactory().produce()
        apOASysContextMockUnsuccessfulRisksToTheIndividualCallWithDelay(offenderDetails.otherIds.crn, risksToTheIndividual, 2500)

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/oasys/risk-to-self")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }
}
