package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedsDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockSuccessfulNeedsDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockUnsuccessfulNeedsDetailsCallWithDelay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NeedsDetailsTransformer

class PersonOASysSelectionTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var needsDetailsTransformer: NeedsDetailsTransformer

  @Test
  fun `Getting oasys section selection by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/people/CRN/oasys/selection")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting oasys section selection for a CRN with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis",
    )

    webTestClient.get()
      .uri("/people/CRN/oasys/selection")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting oasys section selection for a CRN without ROLE_PROBATION returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
    )

    webTestClient.get()
      .uri("/people/CRN/oasys/selection")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting oasys section selection for a CRN that does not exist returns 404`() {
    givenAUser { userEntity, jwt ->
      val crn = "CRN123"

      webTestClient.get()
        .uri("/people/$crn/oasys/selection")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting oasys section selection for a CRN returns OK with correct body`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val needsDetails = NeedsDetailsFactory().apply {
          withAssessmentId(34853487)
          withAccommodationIssuesDetails("Accommodation", true, false)
          withAttitudeIssuesDetails("Attitude", false, true)
          withFinanceIssuesDetails(null, null, null)
        }.produce()

        apOASysContextMockSuccessfulNeedsDetailsCall(offenderDetails.otherIds.crn, needsDetails)

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/oasys/selection")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              needsDetailsTransformer.transformToApi(needsDetails),
            ),
          )
      }
    }
  }

  @Test
  fun `Getting oasys section selection when upstream times out returns 404`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val needsDetails = NeedsDetailsFactory().apply {
          withAssessmentId(34853487)
          withAccommodationIssuesDetails("Accommodation", true, false)
          withAttitudeIssuesDetails("Attitude", false, true)
          withFinanceIssuesDetails(null, null, null)
        }.produce()

        apOASysContextMockUnsuccessfulNeedsDetailsCallWithDelay(offenderDetails.otherIds.crn, needsDetails, 2500)

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/oasys/selection")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }
}
