package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.seed.cas1

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1RemoveAssessmentDetailsSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationTimelineNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.ObjectMapperFactory

class Cas1RedactAssessmentDetailsSeedJobTest {

  val service = Cas1RemoveAssessmentDetailsSeedJob(
    assessmentRepository = mockk<AssessmentRepository>(),
    objectMapper = ObjectMapperFactory.createRuntimeLikeObjectMapper(),
    applicationTimelineNoteService = mockk<ApplicationTimelineNoteService>(),
  )

  @SuppressWarnings("MaxLineLength")
  @Test
  fun removeAllButSufficientInformation() {
    val originalJson = """{"review-application":{"review":{"reviewed":"yes"}},
      |"sufficient-information":{"sufficient-information":{"sufficientInformation":"yes","query":""}},
      |"suitability-assessment":{"suitability-assessment":
      |{"riskFactors":"yes","riskFactorsComments":"asd","riskManagement":"yes","riskManagementComments":"","locationOfPlacement":"yes","locationOfPlacementComments":"","moveOnPlan":"yes","moveOnPlanComments":""}},"required-actions":{"required-actions":{"additionalActions":"no","additionalActionsComments":"","curfewsOrSignIns":"no","curfewsOrSignInsComments":"","concernsOfUnmanagableRisk":"no","concernsOfUnmanagableRiskComments":"","additionalRecommendations":"no","additionalRecommendationsComments":"","nameOfAreaManager":"","dateOfDiscussion-year":"","dateOfDiscussion-month":"","dateOfDiscussion-day":"","outlineOfDiscussion":""}},
      |"make-a-decision":{"make-a-decision":{"decision":"accept","decisionRationale":""}},"matching-information":{"matching-information":{"apType":"normal","lengthOfStayAgreed":"yes","lengthOfStayWeeks":"","lengthOfStayDays":"","cruInformation":"asd","isWheelchairDesignated":"notRelevant","isArsonDesignated":"notRelevant","isSingle":"notRelevant","isCatered":"essential","isSuitedForSexOffenders":"notRelevant","isStepFreeDesignated":"notRelevant","hasEnSuite":"notRelevant","isSuitableForVulnerable":"notRelevant","acceptsSexOffenders":"notRelevant","acceptsChildSexOffenders":"notRelevant","acceptsNonSexualChildOffenders":"notRelevant","acceptsHateCrimeOffenders":"notRelevant","isArsonSuitable":"notRelevant"}},
      |"check-your-answers":{"check-your-answers":{"reviewed":"1"}}}
    """.trimMargin()
    val updatedJson = service.removeAllButSufficientInformation(originalJson)

    assertThat(updatedJson).isEqualTo(
      """{"sufficient-information":{"sufficient-information":{"sufficientInformation":"yes","query":""}}}""",
    )
  }
}
