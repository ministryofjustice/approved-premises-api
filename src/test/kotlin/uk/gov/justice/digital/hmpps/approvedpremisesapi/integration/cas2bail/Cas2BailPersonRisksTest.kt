package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2bail

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FlagsEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MappaEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskEnvelopeStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RoshRisksEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshRatingsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockSuccessfulRoshRatingsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RiskLevel
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas2BailPersonRisksTest : IntegrationTestBase() {
  @Test
  fun `Getting cas2bail risks by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/cas2bail/people/CRN/risks")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting cas2bail risks for a CRN with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis",
    )

    webTestClient.get()
      .uri("/cas2bail/people/CRN/risks")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting cas2bail risks for a CRN without ROLE_POM returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "nomis",
      roles = listOf("ROLE_OTHER"),
    )

    webTestClient.get()
      .uri("/cas2bail/people/CRN/risks")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting cas2bail risks for a CRN that does not exist returns 404`() {
    givenACas2PomUser { userEntity, jwt ->
      val crn = "CRN123"

      webTestClient.get()
        .uri("/cas2bail/people/$crn/risks")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting cas2bail risks for a CRN returns OK with correct body`() {
    givenACas2PomUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        apOASysContextMockSuccessfulRoshRatingsCall(
          offenderDetails.otherIds.crn,
          RoshRatingsFactory().apply {
            withDateCompleted(OffsetDateTime.parse("2022-09-06T15:15:15Z"))
            withAssessmentId(34853487)
            withRiskChildrenCommunity(RiskLevel.LOW)
            withRiskPublicCommunity(RiskLevel.MEDIUM)
            withRiskKnownAdultCommunity(RiskLevel.HIGH)
            withRiskStaffCommunity(RiskLevel.VERY_HIGH)
          }.produce(),
        )

        webTestClient.get()
          .uri("/cas2bail/people/${offenderDetails.otherIds.crn}/risks")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              PersonRisks(
                crn = offenderDetails.otherIds.crn,
                roshRisks = RoshRisksEnvelope(
                  status = RiskEnvelopeStatus.retrieved,
                  value = RoshRisks(
                    overallRisk = "Very High",
                    riskToChildren = "Low",
                    riskToPublic = "Medium",
                    riskToKnownAdult = "High",
                    riskToStaff = "Very High",
                    lastUpdated = LocalDate.parse("2022-09-06"),
                  ),
                ),
                tier = RiskTierEnvelope(
                  status = RiskEnvelopeStatus.notFound,
                  value = null,
                ),
                flags = FlagsEnvelope(
                  status = RiskEnvelopeStatus.notFound,
                  value = null,
                ),
                mappa = MappaEnvelope(
                  status = RiskEnvelopeStatus.notFound,
                  value = null,
                ),
              ),
            ),
          )
      }
    }
  }
}
