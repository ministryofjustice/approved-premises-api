package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.allocations

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.UserAllocator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.UserAllocatorRule
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.UserAllocatorRuleOutcome
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.addQualificationForUnitTest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.addRoleForUnitTest
import java.util.stream.Stream

class UserAllocatorTest {
  private val mockUserRepository = mockk<UserRepository>()
  private val mockSentryService = mockk<SentryService>()

  @BeforeEach
  fun beforeEach() {
    every { mockUserRepository.findActiveUsersWithQualification(UserQualification.EMERGENCY) } returns listOf(user1)
    every { mockUserRepository.findActiveUsersWithRole(UserRole.CAS1_CRU_MEMBER) } returns listOf(user2)

    every { mockUserRepository.findUserWithLeastAssessmentsPendingOrCompletedInLastWeek(listOf(user1.id)) } returns user1
    every { mockUserRepository.findUserWithLeastAssessmentsPendingOrCompletedInLastWeek(listOf(user2.id)) } returns user2

    every { mockUserRepository.findUserWithLeastPlacementApplicationsPendingOrCompletedInLastWeek(listOf(user1.id)) } returns user1
    every { mockUserRepository.findUserWithLeastPlacementApplicationsPendingOrCompletedInLastWeek(listOf(user2.id)) } returns user2
  }

  @Nested
  inner class GetUserForAssessmentAllocation {
    @Test
    fun `Returns null when no rules are active`() {
      val userAllocator = UserAllocator(listOf(), mockUserRepository, mockSentryService)

      val result = userAllocator.getUserForAssessmentAllocation(assessment)

      assertThat(result).isNull()
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.allocations.UserAllocatorTest#assessmentRules")
    fun `Returns the expected user according to the rules outcome and precedence`(
      rules: List<UserAllocatorRule>,
      expectedUserEntity: UserEntity?,
    ) {
      val userAllocator = UserAllocator(rules, mockUserRepository, mockSentryService)

      if (expectedUserEntity != null) {
        every { mockUserRepository.findByDeliusUsername(expectedUserEntity.deliusUsername) } returns expectedUserEntity
      }

      val result = userAllocator.getUserForAssessmentAllocation(assessment)

      assertThat(result).isEqualTo(expectedUserEntity)

      if (expectedUserEntity == null) {
        verify(exactly = 0) { mockUserRepository.findByDeliusUsername(any()) }
      }
    }

    @Test
    fun `A rule is skipped if the user it attempts to allocate to could not be found`() {
      val appender = mockk<Appender<ILoggingEvent>>()
      val capturedLogs = mutableListOf<ILoggingEvent>()

      val logger = LoggerFactory.getLogger(UserAllocator::class.java) as ch.qos.logback.classic.Logger
      logger.addAppender(appender)

      every { appender.doAppend(capture(capturedLogs)) } returns Unit

      val rules = listOf(
        TestRule.allocateToUser(RuleType.ASSESSMENT, user1, priority = 1, name = "unknown-user-rule"),
        TestRule.allocateToUser(RuleType.ASSESSMENT, user2, priority = 2, name = "known-user-rule"),
      )

      every { mockUserRepository.findByDeliusUsername(user1.deliusUsername) } returns null
      every { mockUserRepository.findByDeliusUsername(user2.deliusUsername) } returns user2
      every { mockSentryService.captureErrorMessage(any()) } returns Unit

      val userAllocator = UserAllocator(rules, mockUserRepository, mockSentryService)

      val result = userAllocator.getUserForAssessmentAllocation(assessment)

      assertThat(result).isEqualTo(user2)

      assertThat(capturedLogs).size().isEqualTo(1)
      assertThat(capturedLogs.first()).matches {
        it.message.contains("'unknown-user-rule'") &&
          it.message.contains("'USER-1'") &&
          it.message.contains("could not be found") &&
          it.message.contains("skipped") &&
          it.level == Level.WARN
      }

      verify { mockSentryService.captureErrorMessage("Rule 'unknown-user-rule' attempted to allocate a task to user 'USER-1', but they could not be found. This rule has been skipped.") }
    }
  }

  @Nested
  inner class GetUserForPlacementApplicationAllocation {
    @Test
    fun `Returns null when no rules are active`() {
      val userAllocator = UserAllocator(listOf(), mockUserRepository, mockSentryService)

      val result = userAllocator.getUserForPlacementApplicationAllocation(placementApplication)

      assertThat(result).isNull()
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.allocations.UserAllocatorTest#placementApplicationRules")
    fun `Returns the expected user according to the rules outcome and precedence`(
      rules: List<UserAllocatorRule>,
      expectedUserEntity: UserEntity?,
    ) {
      val userAllocator = UserAllocator(rules, mockUserRepository, mockSentryService)

      if (expectedUserEntity != null) {
        every { mockUserRepository.findByDeliusUsername(expectedUserEntity.deliusUsername) } returns expectedUserEntity
      }

      val result = userAllocator.getUserForPlacementApplicationAllocation(placementApplication)

      assertThat(result).isEqualTo(expectedUserEntity)

      if (expectedUserEntity == null) {
        verify(exactly = 0) { mockUserRepository.findByDeliusUsername(any()) }
      }
    }

    @Test
    fun `A rule is skipped if the user it attempts to allocate to could not be found`() {
      val appender = mockk<Appender<ILoggingEvent>>()
      val capturedLogs = mutableListOf<ILoggingEvent>()

      val logger = LoggerFactory.getLogger(UserAllocator::class.java) as ch.qos.logback.classic.Logger
      logger.addAppender(appender)

      every { appender.doAppend(capture(capturedLogs)) } returns Unit

      val rules = listOf(
        TestRule.allocateToUser(RuleType.PLACEMENT_APPLICATION, user1, priority = 1, name = "unknown-user-rule"),
        TestRule.allocateToUser(RuleType.PLACEMENT_APPLICATION, user2, priority = 2, name = "known-user-rule"),
      )

      every { mockUserRepository.findByDeliusUsername(user1.deliusUsername) } returns null
      every { mockUserRepository.findByDeliusUsername(user2.deliusUsername) } returns user2
      every { mockSentryService.captureErrorMessage(any()) } returns Unit

      val userAllocator = UserAllocator(rules, mockUserRepository, mockSentryService)

      val result = userAllocator.getUserForPlacementApplicationAllocation(placementApplication)

      assertThat(result).isEqualTo(user2)

      assertThat(capturedLogs).size().isEqualTo(1)
      assertThat(capturedLogs.first()).matches {
        it.message.contains("'unknown-user-rule'") &&
          it.message.contains("'USER-1'") &&
          it.message.contains("could not be found") &&
          it.message.contains("skipped") &&
          it.level == Level.WARN
      }
      verify { mockSentryService.captureErrorMessage("Rule 'unknown-user-rule' attempted to allocate a task to user 'USER-1', but they could not be found. This rule has been skipped.") }
    }
  }

  companion object {
    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea {
        ApAreaEntityFactory()
          .produce()
      }
      .produce()

    private val createdByUser = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    val user1 = UserEntityFactory()
      .withDeliusUsername("USER-1")
      .withProbationRegion(probationRegion)
      .produce()
      .addQualificationForUnitTest(UserQualification.EMERGENCY)

    val user2 = UserEntityFactory()
      .withDeliusUsername("USER-2")
      .withProbationRegion(probationRegion)
      .produce()
      .addRoleForUnitTest(UserRole.CAS1_CRU_MEMBER)

    private val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(createdByUser)
      .produce()

    private val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .produce()

    private val placementApplication = PlacementApplicationEntityFactory()
      .withApplication(application)
      .withCreatedByUser(createdByUser)
      .produce()

    @JvmStatic
    fun assessmentRules(): Stream<Arguments> = generateRules(RuleType.ASSESSMENT)

    @JvmStatic
    fun placementApplicationRules(): Stream<Arguments> = generateRules(RuleType.PLACEMENT_APPLICATION)

    private fun generateRules(ruleType: RuleType): Stream<Arguments> {
      val arguments = listOf(
        // Single active rules.
        // --------------------

        // Outcome is `Skip`.
        Arguments.of(
          listOf(
            TestRule.skip(ruleType),
          ),
          null,
        ),

        // Outcome is `DoNotAllocate`.
        Arguments.of(
          listOf(
            TestRule.doNotAllocate(ruleType),
          ),
          null,
        ),

        // Outcome is `AllocateToUser("USER-1")`.
        Arguments.of(
          listOf(
            TestRule.allocateToUser(ruleType, user1),
          ),
          user1,
        ),

        // Outcome is `AllocateToUser("USER-2")`.
        Arguments.of(
          listOf(
            TestRule.allocateToUser(ruleType, user2),
          ),
          user2,
        ),

        // Outcome is `AllocateByQualification(EMERGENCY)`.
        Arguments.of(
          listOf(
            TestRule.allocateByQualification(ruleType, UserQualification.EMERGENCY),
          ),
          user1,
        ),

        // Outcome is `AllocateByRole(CAS1_CRU_MEMBER)`.
        Arguments.of(
          listOf(
            TestRule.allocateByRole(ruleType, UserRole.CAS1_CRU_MEMBER),
          ),
          user2,
        ),

        // Multiple active rules. Rules with a lower priority value takes precedence.
        // --------------------------------------------------------------------------

        // Two rules, both outcomes are `AllocateToUser`.
        Arguments.of(
          listOf(
            TestRule.allocateToUser(ruleType, user1, priority = 2),
            TestRule.allocateToUser(ruleType, user2, priority = 1),
          ),
          user2,
        ),

        // Two rules, `DoNotAllocate` taking precedence over `AllocateToUser`.
        Arguments.of(
          listOf(
            TestRule.allocateToUser(ruleType, user1, priority = 2),
            TestRule.doNotAllocate(ruleType, priority = 1),
          ),
          null,
        ),

        // Two rules, `AllocateToUser` taking precedence over `DoNotAllocate`.
        Arguments.of(
          listOf(
            TestRule.allocateToUser(ruleType, user1, priority = 1),
            TestRule.doNotAllocate(ruleType, priority = 2),
          ),
          user1,
        ),

        // Two rules, `Skip` taking precedence over `DoNotAllocate`.
        Arguments.of(
          listOf(
            TestRule.doNotAllocate(ruleType, priority = 2),
            TestRule.skip(ruleType, priority = 1),
          ),
          null,
        ),

        // Two rules, `Skip` taking precedence over `AllocateToUser`.
        Arguments.of(
          listOf(
            TestRule.skip(ruleType, priority = 1),
            TestRule.allocateToUser(ruleType, user1, priority = 2),
          ),
          user1,
        ),
      )

      return arguments.stream()
    }
  }
}

data class TestRule(
  override val priority: Int = 0,
  override val name: String = "test rule",
  private val evaluateAssessmentOutcome: UserAllocatorRuleOutcome = UserAllocatorRuleOutcome.Skip,
  private val evaluatePlacementApplicationOutcome: UserAllocatorRuleOutcome = UserAllocatorRuleOutcome.Skip,
) : UserAllocatorRule {
  override fun evaluateAssessment(assessmentEntity: AssessmentEntity): UserAllocatorRuleOutcome =
    evaluateAssessmentOutcome

  override fun evaluatePlacementApplication(placementApplicationEntity: PlacementApplicationEntity): UserAllocatorRuleOutcome =
    evaluatePlacementApplicationOutcome

  companion object {
    fun skip(ruleType: RuleType, priority: Int = 0, name: String = "test rule") = when (ruleType) {
      RuleType.ASSESSMENT -> TestRule(priority = priority, name = name, evaluateAssessmentOutcome = UserAllocatorRuleOutcome.Skip)
      RuleType.PLACEMENT_APPLICATION -> TestRule(priority = priority, name = name, evaluatePlacementApplicationOutcome = UserAllocatorRuleOutcome.Skip)
    }

    fun doNotAllocate(ruleType: RuleType, priority: Int = 0, name: String = "test rule") = when (ruleType) {
      RuleType.ASSESSMENT -> TestRule(priority = priority, name = name, evaluateAssessmentOutcome = UserAllocatorRuleOutcome.DoNotAllocate)
      RuleType.PLACEMENT_APPLICATION -> TestRule(priority = priority, name = name, evaluatePlacementApplicationOutcome = UserAllocatorRuleOutcome.DoNotAllocate)
    }

    fun allocateToUser(ruleType: RuleType, user: UserEntity, priority: Int = 0, name: String = "test rule") = when (ruleType) {
      RuleType.ASSESSMENT -> TestRule(priority = priority, name = name, evaluateAssessmentOutcome = UserAllocatorRuleOutcome.AllocateToUser(user.deliusUsername))
      RuleType.PLACEMENT_APPLICATION -> TestRule(priority = priority, name = name, evaluatePlacementApplicationOutcome = UserAllocatorRuleOutcome.AllocateToUser(user.deliusUsername))
    }

    fun allocateByQualification(ruleType: RuleType, qualification: UserQualification, priority: Int = 0, name: String = "test rule") = when (ruleType) {
      RuleType.ASSESSMENT -> TestRule(priority = priority, name = name, evaluateAssessmentOutcome = UserAllocatorRuleOutcome.AllocateByQualification(qualification))
      RuleType.PLACEMENT_APPLICATION -> TestRule(priority = priority, name = name, evaluatePlacementApplicationOutcome = UserAllocatorRuleOutcome.AllocateByQualification(qualification))
    }

    fun allocateByRole(ruleType: RuleType, role: UserRole, priority: Int = 0, name: String = "test rule") = when (ruleType) {
      RuleType.ASSESSMENT -> TestRule(priority = priority, name = name, evaluateAssessmentOutcome = UserAllocatorRuleOutcome.AllocateByRole(role))
      RuleType.PLACEMENT_APPLICATION -> TestRule(priority = priority, name = name, evaluatePlacementApplicationOutcome = UserAllocatorRuleOutcome.AllocateByRole(role))
    }
  }
}

enum class RuleType {
  ASSESSMENT,
  PLACEMENT_APPLICATION,
}
