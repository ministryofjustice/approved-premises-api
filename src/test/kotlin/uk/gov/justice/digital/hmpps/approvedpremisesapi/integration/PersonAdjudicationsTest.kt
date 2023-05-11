package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AdjudicationFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AdjudicationsPageFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AgencyFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockNotFoundOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.PrisonAPI_mockSuccessfulAdjudicationsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AdjudicationTransformer

class PersonAdjudicationsTest : IntegrationTestBase() {
  @Autowired
  lateinit var adjudicationTransformer: AdjudicationTransformer

  @Test
  fun `Getting adjudications by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/people/CRN/adjudications")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting adjudications for a CRN with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis"
    )

    webTestClient.get()
      .uri("/people/CRN/adjudications")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting adjudications for a CRN without ROLE_PROBATION returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius"
    )

    webTestClient.get()
      .uri("/people/CRN/adjudications")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting adjudications for a CRN that does not exist returns 404`() {
    `Given a User` { userEntity, jwt ->
      val crn = "CRN123"

      CommunityAPI_mockNotFoundOffenderDetailsCall(crn)
      loadPreemptiveCacheForOffenderDetails(crn)

      webTestClient.get()
        .uri("/people/$crn/adjudications")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting adjudications for a CRN returns OK with correct body`() {
    `Given a User` { _, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val adjudicationsResponse = AdjudicationsPageFactory()
          .withResults(
            listOf(
              AdjudicationFactory().withAgencyId("AGNCY1").produce(),
              AdjudicationFactory().withAgencyId("AGNCY2").produce()
            )
          )
          .withAgencies(
            listOf(
              AgencyFactory().withAgencyId("AGNCY1").produce(),
              AgencyFactory().withAgencyId("AGNCY2").produce()
            )
          )
          .produce()

        PrisonAPI_mockSuccessfulAdjudicationsCall(offenderDetails.otherIds.nomsNumber!!, adjudicationsResponse)

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/adjudications")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(adjudicationTransformer.transformToApi(adjudicationsResponse))
          )
      }
    }
  }
}
