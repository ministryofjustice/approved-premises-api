package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenceDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APOASysContext_mockSuccessfulOffenceDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APOASysContext_mockSuccessfulRoSHSummaryCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APOASysContext_mockUnsuccessfulRoshCallWithDelay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockNotFoundOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysSectionsTransformer

class PersonOASysRoshTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var oaSysSectionsTransformer: OASysSectionsTransformer

  @Test
  fun `Getting RoSH by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/people/CRN/oasys/rosh")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting RoSH  for a CRN with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "other-auth-source",
    )

    webTestClient.get()
      .uri("/people/CRN/oasys/rosh")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting RoSH  for a CRN with a nomis JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis",
    )

    webTestClient.get()
      .uri("/people/CRN/oasys/rosh")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting RoSH for a CRN without ROLE_PROBATION returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
      roles = listOf("ROLE_OTHER"),
    )

    webTestClient.get()
      .uri("/people/CRN/oasys/rosh")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting RoSH for a CRN with ROLE_POM returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
      roles = listOf("ROLE_POM"),
    )

    webTestClient.get()
      .uri("/people/CRN/oasys/rosh")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting Rosh for a CRN that does not exist returns 404`() {
    `Given a User` { userEntity, jwt ->
      val crn = "CRN123"

      CommunityAPI_mockNotFoundOffenderDetailsCall(crn)
      loadPreemptiveCacheForOffenderDetails(crn)

      webTestClient.get()
        .uri("/people/$crn/oasys/rosh")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting RoSH for a CRN returns OK with correct body`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val offenceDetails = OffenceDetailsFactory().produce()
        APOASysContext_mockSuccessfulOffenceDetailsCall(offenderDetails.otherIds.crn, offenceDetails)

        val rosh = RoshSummaryFactory().produce()
        APOASysContext_mockSuccessfulRoSHSummaryCall(offenderDetails.otherIds.crn, rosh)

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/oasys/rosh")
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
  fun `Getting RoSH when upstream times out returns 404`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val rosh = RoshSummaryFactory().produce()
        APOASysContext_mockUnsuccessfulRoshCallWithDelay(offenderDetails.otherIds.crn, rosh, 2500)

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/oasys/rosh")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }
}
