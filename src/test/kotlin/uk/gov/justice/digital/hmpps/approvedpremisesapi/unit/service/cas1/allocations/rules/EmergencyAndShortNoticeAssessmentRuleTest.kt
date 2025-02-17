package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.allocations.rules

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1CruManagementAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.UserAllocatorRuleOutcome
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.rules.EmergencyAndShortNoticeAssessmentRule
import java.time.OffsetDateTime

class EmergencyAndShortNoticeAssessmentRuleTest {
  val assessmentRule = EmergencyAndShortNoticeAssessmentRule()

  @Nested
  inner class EvaluateAssessment {
    @ParameterizedTest
    @EnumSource(Cas1ApplicationTimelinessCategory::class, names = ["emergency", "shortNotice"])
    fun `Returns AllocateToUser with configured username for the application's cru management area when the application is for CAS1, submitted, and the application is emergency or short notice`(
      noticeType: Cas1ApplicationTimelinessCategory,
    ) {
      val createdByUser = UserEntityFactory()
        .withDefaults()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(createdByUser)
        .withSubmittedAt(OffsetDateTime.now())
        .withNoticeType(noticeType)
        .withCruManagementArea(
          Cas1CruManagementAreaEntityFactory()
            .withAssessmentAutoAllocationUsername("the cru assessor")
            .produce(),
        )
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val result = assessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.AllocateToUser("the cru assessor"))
    }

    @Test
    fun `Returns Skip if the application is not for CAS1`() {
      val apArea = ApAreaEntityFactory()
        .produce()

      val probationRegion = ProbationRegionEntityFactory()
        .withApArea(apArea)
        .produce()

      val createdByUser = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(createdByUser)
        .withProbationRegion(probationRegion)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val result = assessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }

    @ParameterizedTest
    @EnumSource(Cas1ApplicationTimelinessCategory::class, names = ["emergency", "shortNotice"])
    fun `Returns Skip if the application is not submitted`(noticeType: Cas1ApplicationTimelinessCategory) {
      val apArea = ApAreaEntityFactory()
        .produce()

      val probationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea {
          ApAreaEntityFactory()
            .produce()
        }
        .produce()

      val createdByUser = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(createdByUser)
        .withSubmittedAt(null)
        .withNoticeType(noticeType)
        .produce()
        .apply {
          this.apArea = apArea
        }

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val result = assessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }

    @Test
    fun `Returns Skip if the application has a standard noticeType`() {
      val apArea = ApAreaEntityFactory()
        .produce()

      val probationRegion = ProbationRegionEntityFactory()
        .withApArea(apArea)
        .produce()

      val createdByUser = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(createdByUser)
        .withSubmittedAt(OffsetDateTime.now())
        .withNoticeType(Cas1ApplicationTimelinessCategory.standard)
        .produce()
        .apply {
          this.apArea = apArea
        }

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val result = assessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }

    @ParameterizedTest
    @EnumSource(Cas1ApplicationTimelinessCategory::class, names = ["emergency", "shortNotice"])
    fun `Returns Skip with when no user is configured for the cru management area`(noticeType: Cas1ApplicationTimelinessCategory) {
      val createdByUser = UserEntityFactory()
        .withDefaults()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(createdByUser)
        .withSubmittedAt(OffsetDateTime.now())
        .withNoticeType(noticeType)
        .withCruManagementArea(
          Cas1CruManagementAreaEntityFactory()
            .withAssessmentAutoAllocationUsername(null)
            .produce(),
        )
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val result = assessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }
  }

  @Nested
  inner class EvaluatePlacementApplication {
    @Test
    fun `Always returns Skip`() {
      val result = assessmentRule.evaluatePlacementApplication(mockk<PlacementApplicationEntity>())

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }
  }
}
