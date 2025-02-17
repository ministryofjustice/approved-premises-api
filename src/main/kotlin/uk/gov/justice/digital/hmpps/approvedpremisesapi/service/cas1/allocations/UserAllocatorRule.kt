package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity

interface UserAllocatorRule {
  val name: String

  /**
   * The lower the number, the higher the priority
   */
  val priority: Int

  fun evaluateAssessment(assessmentEntity: AssessmentEntity): UserAllocatorRuleOutcome =
    UserAllocatorRuleOutcome.Skip

  @Deprecated(
    """
    This function was added to support the switch over from the Legacy behaviour to the
    new allocation behaviour. The new allocation behaviour will never auto allocate a 
    placement application, so this function will always return UserAllocatorRuleOutcome.Skip
  """,
    replaceWith = ReplaceWith("remove any call to this function as it will always return UserAllocatorRuleOutcome.Skip"),
  )
  fun evaluatePlacementApplication(placementApplicationEntity: PlacementApplicationEntity): UserAllocatorRuleOutcome =
    UserAllocatorRuleOutcome.Skip
}
