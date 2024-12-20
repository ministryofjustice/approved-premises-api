package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FlagsEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MappaEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskEnvelopeStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RoshRisksEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshRatingsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.from
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulCaseDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockSuccessfulRoshRatingsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.hmppsTierMockSuccessfulTierCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.MappaDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Registration
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.hmppstier.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RiskLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseDetail
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.UUID

class PersonRisksTest : InitialiseDatabasePerClassTestBase() {
  @Test
  fun `Getting risks by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/people/CRN/risks")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting risks for a CRN with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis",
    )

    webTestClient.get()
      .uri("/people/CRN/risks")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting risks for a CRN without ROLE_PROBATION returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
    )

    webTestClient.get()
      .uri("/people/CRN/risks")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting risks for a CRN that does not exist returns 404`() {
    givenAUser { userEntity, jwt ->
      val crn = "CRN123"

      webTestClient.get()
        .uri("/people/$crn/risks")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting risks for a CRN returns OK with correct body`() {
    givenAUser { userEntity, jwt ->
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

        hmppsTierMockSuccessfulTierCall(
          offenderDetails.otherIds.crn,
          Tier(
            tierScore = "M2",
            calculationId = UUID.randomUUID(),
            calculationDate = LocalDateTime.parse("2022-09-06T14:59:00"),
          ),
        )

        apDeliusContextMockSuccessfulCaseDetailCall(
          offenderDetails.otherIds.crn,
          CaseDetailFactory()
            .from(offenderDetails.asCaseDetail())
            .withRegistrations(
              listOf(
                Registration("FLAG", "RISK FLAG", LocalDate.now()),
              ),
            )
            .withMappaDetail(
              MappaDetail(
                1,
                "L1",
                1,
                "C1",
                LocalDate.parse("2022-09-06"),
                ZonedDateTime.parse("2022-09-06T00:00:00Z"),
              ),
            )
            .produce(),
        )

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/risks")
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
                  status = RiskEnvelopeStatus.retrieved,
                  value = RiskTier(
                    level = "M2",
                    lastUpdated = LocalDate.parse("2022-09-06"),
                  ),
                ),
                flags = FlagsEnvelope(
                  status = RiskEnvelopeStatus.retrieved,
                  value = listOf("RISK FLAG"),
                ),
                mappa = MappaEnvelope(
                  status = RiskEnvelopeStatus.retrieved,
                  value = Mappa(
                    level = "CAT C1/LEVEL L1",
                    lastUpdated = LocalDate.parse("2022-09-06"),
                  ),
                ),
              ),
            ),
          )
      }
    }
  }
}
