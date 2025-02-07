package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.allocations.rules

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AppealEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.UserAllocatorRuleOutcome
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.rules.AppealedAssessmentRule
import java.time.OffsetDateTime

class AppealedAssessmentRuleTest {
  private val mockAppealRepository = mockk<AppealRepository>()

  val appealedAssessmentRule = AppealedAssessmentRule(mockAppealRepository)

  @Nested
  inner class EvaluateAssessment {
    @Test
    fun `Returns AllocateToUser with the appeal arbitrator when the application is for Approved Premises, is submitted, and the assessment is the result of an appeal`() {
      val (assessment, appeal) = createAssessmentAndAppeal(true, OffsetDateTime.now())
      val result = appealedAssessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.AllocateToUser(appeal.createdBy.deliusUsername))
    }

    @Test
    fun `Returns Skip if the application is not for Approved Premises`() {
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

      val result = appealedAssessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }

    @Test
    fun `Returns Skip if the application is not submitted`() {
      val (assessment, _) = createAssessmentAndAppeal(true, null)
      val result = appealedAssessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }

    @Test
    fun `Returns Skip if the application is not the result of an appeal`() {
      val (assessment, _) = createAssessmentAndAppeal(false, OffsetDateTime.now())
      val result = appealedAssessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }

    @Test
    fun `Returns Skip with when no appeal is found for the assessment`() {
      val (assessment, _) = createAssessmentAndAppeal(true, OffsetDateTime.now())
      every { mockAppealRepository.findByApplication(assessment.application) } returns listOf()

      val result = appealedAssessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }

    private fun createAssessmentAndAppeal(createdFromAppeal: Boolean, submittedAt: OffsetDateTime?): Pair<ApprovedPremisesAssessmentEntity, AppealEntity> {
      val probationRegion = ProbationRegionEntityFactory()
        .withApArea(
          ApAreaEntityFactory()
            .produce(),
        )
        .produce()

      val createdByUser = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      val appealUser = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(createdByUser)
        .withSubmittedAt(submittedAt)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withCreatedFromAppeal(createdFromAppeal)
        .produce()

      val appeal = AppealEntityFactory()
        .withAssessment(assessment)
        .withApplication(application)
        .withCreatedBy(appealUser)
        .produce()

      every { mockAppealRepository.findByApplication(application) } returns listOf(appeal)

      return Pair(assessment, appeal)
    }
  }

  @Nested
  inner class EvaluatePlacementApplication {
    @Test
    fun `Always returns Skip`() {
      val result = appealedAssessmentRule.evaluatePlacementApplication(mockk<PlacementApplicationEntity>())

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }
  }
}
