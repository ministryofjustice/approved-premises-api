package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import java.util.UUID

@Component
class UserAllocator(
  private val userAllocatorRules: List<UserAllocatorRule>,
  private val userRepository: UserRepository,
  private val sentryService: SentryService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  private val userAllocatorRulesInPriorityOrder: List<UserAllocatorRule> by lazy {
    userAllocatorRules.sortedBy { it.priority }
  }

  fun getUserForAssessmentAllocation(assessmentEntity: AssessmentEntity): UserEntity? =
    getUserForAllocation(
      evaluate = { userAllocatorRule -> userAllocatorRule.evaluateAssessment(assessmentEntity) },
      selectUser = { suitableUsers -> userRepository.findUserWithLeastAssessmentsPendingOrCompletedInLastWeek(suitableUsers) },
    )

  @Deprecated(
    """
    This function was added to support the switch over from the Legacy behaviour to the
    new allocation behaviour. The new allocation behaviour will never auto allocate a 
    placement request, so this function will always return null
  """,
    replaceWith = ReplaceWith("remove any call to this function as it will always return null"),
  )
  fun getUserForPlacementApplicationAllocation(placementApplicationEntity: PlacementApplicationEntity): UserEntity? =
    getUserForAllocation(
      evaluate = { userAllocatorRule -> userAllocatorRule.evaluatePlacementApplication(placementApplicationEntity) },
      selectUser = { suitableUsers -> userRepository.findUserWithLeastPlacementApplicationsPendingOrCompletedInLastWeek(suitableUsers) },
    )

  private fun getUserForAllocation(evaluate: (UserAllocatorRule) -> UserAllocatorRuleOutcome, selectUser: (List<UUID>) -> UserEntity?): UserEntity? {
    userAllocatorRulesInPriorityOrder.forEach { rule ->
      val allocationResult = when (val outcome = evaluate(rule)) {
        is UserAllocatorRuleOutcome.AllocateToUser -> allocateToUser(outcome.userName, rule.name)
        is UserAllocatorRuleOutcome.AllocateByQualification -> allocateByQualification(outcome.qualification, rule.name, selectUser)
        is UserAllocatorRuleOutcome.AllocateByRole -> allocateByRole(outcome.role, rule.name, selectUser)
        UserAllocatorRuleOutcome.Skip -> AllocationResult.Failed
        UserAllocatorRuleOutcome.DoNotAllocate -> return null
      }

      when (allocationResult) {
        is AllocationResult.Success -> return allocationResult.user
        is AllocationResult.Failed -> {
          // Do nothing.
        }
      }
    }

    return null
  }

  private fun allocateToUser(userName: String, ruleName: String): AllocationResult =
    when (val user = userRepository.findByDeliusUsername(userName)) {
      null -> {
        val message = "Rule '$ruleName' attempted to allocate a task to user '$userName', but they could not be found. This rule has been skipped."
        log.warn(message)
        sentryService.captureErrorMessage(message)
        AllocationResult.Failed
      }

      else -> AllocationResult.Success(user)
    }

  private fun allocateByQualification(qualification: UserQualification, ruleName: String, selectUser: (List<UUID>) -> UserEntity?): AllocationResult {
    val users = userRepository.findActiveUsersWithQualification(qualification)

    val user = selectUser(users.map { it.id })

    return when (user) {
      null -> {
        log.warn("Rule '$ruleName' attempted to allocate a task to a user with qualification '$qualification', but not suitable user could be found. This rule has been skipped.")
        AllocationResult.Failed
      }

      else -> AllocationResult.Success(user)
    }
  }

  private fun allocateByRole(role: UserRole, ruleName: String, selectUser: (List<UUID>) -> UserEntity?): AllocationResult {
    val users = userRepository.findActiveUsersWithRole(role)

    val user = selectUser(users.map { it.id })

    return when (user) {
      null -> {
        log.warn("Rule '$ruleName' attempted to allocate a task to a user with role '$role', but not suitable user could be found. This rule has been skipped.")
        AllocationResult.Failed
      }

      else -> AllocationResult.Success(user)
    }
  }

  private sealed interface AllocationResult {
    data class Success(val user: UserEntity) : AllocationResult
    data object Failed : AllocationResult
  }
}
