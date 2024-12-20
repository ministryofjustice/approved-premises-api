package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddStaffDetailResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedUsersTest : SeedTestBase() {
  @Test
  fun `Attempting to seed a non existent user logs an error`() {
    withCsv(
      "invalid-user",
      userRoleAssignmentSeedCsvRowsToCsv(
        listOf(
          UserRoleAssignmentsSeedCsvRowFactory()
            .withDeliusUsername("INVALID-USER")
            .produce(),
        ),
      ),
    )

    seedService.seedData(SeedFileType.user, "invalid-user.csv")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message.contains("Error on row 1:") &&
        it.throwable != null &&
        it.throwable.cause != null &&
        it.throwable.message!!.contains("Could not get user INVALID-USER") &&
        it.throwable.cause!!.message!!.contains("Could not find staff record for user INVALID-USER")
    }
  }

  @Test
  fun `Attempting to seed a real but currently unknown user succeeds`() {
    val probationRegion = givenAProbationRegion()

    val probationRegionDeliusMapping = probationAreaProbationRegionMappingFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    apDeliusContextAddStaffDetailResponse(
      StaffDetailFactory.staffDetail(
        deliusUsername = "UNKNOWN-USER",
        probationArea = ProbationArea(
          code = probationRegionDeliusMapping.probationAreaDeliusCode,
          description = randomStringMultiCaseWithNumbers(10),
        ),
      ),

    )

    withCsv(
      "unknown-user",
      userRoleAssignmentSeedCsvRowsToCsv(
        listOf(
          UserRoleAssignmentsSeedCsvRowFactory()
            .withDeliusUsername("unknown-user")
            .withTypedRoles(listOf(UserRole.CAS1_ASSESSOR, UserRole.CAS1_WORKFLOW_MANAGER))
            .withTypedQualifications(listOf(UserQualification.PIPE))
            .produce(),
        ),
      ),
    )

    seedService.seedData(SeedFileType.user, "unknown-user.csv")

    val persistedUser = userRepository.findByDeliusUsername("UNKNOWN-USER")

    assertThat(persistedUser).isNotNull
    assertThat(persistedUser!!.roles.map(UserRoleAssignmentEntity::role)).containsExactlyInAnyOrder(
      UserRole.CAS1_ASSESSOR,
      UserRole.CAS1_WORKFLOW_MANAGER,
    )
    assertThat(persistedUser.qualifications.map(UserQualificationAssignmentEntity::qualification)).containsExactlyInAnyOrder(
      UserQualification.PIPE,
    )
  }

  @Test
  fun `Attempting to assign roles to a currently known user succeeds`() {
    userEntityFactory.produceAndPersist {
      withDeliusUsername("KNOWN-USER")
      withYieldedProbationRegion { givenAProbationRegion() }
    }

    withCsv(
      "known-user",
      userRoleAssignmentSeedCsvRowsToCsv(
        listOf(
          UserRoleAssignmentsSeedCsvRowFactory()
            .withDeliusUsername("KNOWN-USER")
            .withTypedRoles(listOf(UserRole.CAS1_ASSESSOR, UserRole.CAS1_WORKFLOW_MANAGER))
            .withTypedQualifications(listOf(UserQualification.PIPE))
            .produce(),
        ),
      ),
    )

    seedService.seedData(SeedFileType.user, "known-user.csv")

    val persistedUser = userRepository.findByDeliusUsername("KNOWN-USER")

    assertThat(persistedUser).isNotNull
    assertThat(persistedUser!!.roles.map(UserRoleAssignmentEntity::role)).containsExactlyInAnyOrder(
      UserRole.CAS1_ASSESSOR,
      UserRole.CAS1_WORKFLOW_MANAGER,
    )
    assertThat(persistedUser.qualifications.map(UserQualificationAssignmentEntity::qualification)).containsExactlyInAnyOrder(
      UserQualification.PIPE,
    )
  }

  @Test
  fun `Attempting to assign a non-existent role logs an error`() {
    userEntityFactory.produceAndPersist {
      withDeliusUsername("known-user")
      withYieldedProbationRegion { givenAProbationRegion() }
    }

    withCsv(
      "unknown-role",
      userRoleAssignmentSeedCsvRowsToCsv(
        listOf(
          UserRoleAssignmentsSeedCsvRowFactory()
            .withDeliusUsername("known-user")
            .withUntypedRoles(listOf("WORKFLOW_MANAGEF"))
            .withTypedQualifications(listOf(UserQualification.PIPE))
            .produce(),
        ),
      ),
    )

    seedService.seedData(SeedFileType.user, "unknown-role.csv")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Seed Job" &&
        it.throwable != null &&
        it.throwable.message != null &&
        it.throwable.message!!.contains("Unable to deserialize CSV at row: 1: Unrecognised User Role(s): [WORKFLOW_MANAGEF]")
    }
  }

  @Test
  fun `Attempting to assign a non-existent qualification logs an error`() {
    userEntityFactory.produceAndPersist {
      withDeliusUsername("known-user")
      withYieldedProbationRegion { givenAProbationRegion() }
    }

    withCsv(
      "unknown-qualification",
      userRoleAssignmentSeedCsvRowsToCsv(
        listOf(
          UserRoleAssignmentsSeedCsvRowFactory()
            .withDeliusUsername("known-user")
            .withUntypedQualifications(listOf("PIPEE"))
            .produce(),
        ),
      ),
    )

    seedService.seedData(SeedFileType.user, "unknown-qualification.csv")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Seed Job" &&
        it.throwable != null &&
        it.throwable.message != null &&
        it.throwable.message!!.contains("Unable to deserialize CSV at row: 1: Unrecognised User Qualifications(s): [PIPEE]")
    }
  }

  @Test
  fun `Attempting to assign untyped roles to a user succeeds`() {
    userEntityFactory.produceAndPersist {
      withDeliusUsername("KNOWN-USER")
      withYieldedProbationRegion { givenAProbationRegion() }
    }

    withCsv(
      "known-user",
      userRoleAssignmentSeedCsvRowsToCsv(
        listOf(
          UserRoleAssignmentsSeedCsvRowFactory()
            .withDeliusUsername("KNOWN-USER")
            .withUntypedRoles(listOf("CAS1_APPLICANT", "CAS1_ASSESSOR", "CAS1_FUTURE_MANAGER", "CAS1_MATCHER", "CAS1_WORKFLOW_MANAGER"))
            .withTypedQualifications(listOf(UserQualification.PIPE))
            .produce(),
        ),
      ),
    )

    seedService.seedData(SeedFileType.user, "known-user.csv")

    val persistedUser = userRepository.findByDeliusUsername("KNOWN-USER")

    assertThat(persistedUser).isNotNull
    assertThat(persistedUser!!.roles.map(UserRoleAssignmentEntity::role)).containsExactlyInAnyOrder(
      UserRole.CAS1_APPLICANT,
      UserRole.CAS1_ASSESSOR,
      UserRole.CAS1_FUTURE_MANAGER,
      UserRole.CAS1_MATCHER,
      UserRole.CAS1_WORKFLOW_MANAGER,
    )
    assertThat(persistedUser.qualifications.map(UserQualificationAssignmentEntity::qualification)).containsExactlyInAnyOrder(
      UserQualification.PIPE,
    )
  }

  @Test
  fun `Service specific user seed jobs only overwrite roles for that service`() {
    val user = userEntityFactory.produceAndPersist {
      withDeliusUsername("MULTI-SERVICE-USER")
      withYieldedProbationRegion { givenAProbationRegion() }
    }

    val roles = listOf(UserRole.CAS1_ASSESSOR, UserRole.CAS1_WORKFLOW_MANAGER, UserRole.CAS3_ASSESSOR).map { role ->
      userRoleAssignmentEntityFactory.produceAndPersist {
        withUser(user)
        withRole(role)
      }
    }

    user.roles.addAll(roles)

    withCsv(
      "multi-service-user",
      userRoleAssignmentSeedCsvRowsToCsv(
        listOf(
          UserRoleAssignmentsSeedCsvRowFactory()
            .withDeliusUsername("MULTI-SERVICE-USER")
            .withTypedRoles(listOf(UserRole.CAS3_REFERRER))
            .produce(),
        ),
      ),
    )

    seedService.seedData(SeedFileType.temporaryAccommodationUsers, "multi-service-user.csv")

    val persistedUser = userRepository.findByDeliusUsername("MULTI-SERVICE-USER")

    assertThat(persistedUser).isNotNull
    assertThat(persistedUser!!.roles.map(UserRoleAssignmentEntity::role)).containsExactlyInAnyOrder(
      UserRole.CAS1_ASSESSOR,
      UserRole.CAS1_WORKFLOW_MANAGER,
      UserRole.CAS3_REFERRER,
    )
  }

  @Test
  fun `Seeding same users multiple times works every time for base user seed job`() {
    val probationRegion = givenAProbationRegion()

    val probationRegionDeliusMapping = probationAreaProbationRegionMappingFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }
    StaffDetailFactory.staffDetail(
      probationArea = ProbationArea(
        code = probationRegionDeliusMapping.probationAreaDeliusCode,
        description = randomStringMultiCaseWithNumbers(10),
      ),
    )

    val seedInfos = listOf(
      SeedInfo(
        staffUserDetails = StaffDetailFactory.staffDetail(
          probationArea = ProbationArea(
            code = probationRegionDeliusMapping.probationAreaDeliusCode,
            description = randomStringMultiCaseWithNumbers(10),
          ),
        ),
        expectedRoles = listOf(UserRole.CAS1_FUTURE_MANAGER, UserRole.CAS1_WORKFLOW_MANAGER, UserRole.CAS1_ASSESSOR, UserRole.CAS3_ASSESSOR, UserRole.CAS3_REFERRER),
        expectedQualifications = listOf(UserQualification.EMERGENCY, UserQualification.LAO),
      ),
      SeedInfo(
        staffUserDetails = StaffDetailFactory.staffDetail(
          probationArea = ProbationArea(
            code = probationRegionDeliusMapping.probationAreaDeliusCode,
            description = randomStringMultiCaseWithNumbers(10),
          ),
        ),
        expectedRoles = listOf(UserRole.CAS1_FUTURE_MANAGER),
        expectedQualifications = listOf(),
      ),
      SeedInfo(
        staffUserDetails = StaffDetailFactory.staffDetail(
          probationArea = ProbationArea(
            code = probationRegionDeliusMapping.probationAreaDeliusCode,
            description = randomStringMultiCaseWithNumbers(10),
          ),
        ),
        expectedRoles = listOf(),
        expectedQualifications = listOf(UserQualification.LAO),
      ),
    )

    seedInfos.forEach {
      apDeliusContextAddStaffDetailResponse(
        it.staffUserDetails,
      )
    }

    withCsv(
      "users-many-times-base-job",
      userRoleAssignmentSeedCsvRowsToCsv(
        seedInfos.map {
          UserRoleAssignmentsSeedCsvRowFactory()
            .withDeliusUsername(it.staffUserDetails.username!!)
            .withTypedRoles(it.expectedRoles)
            .withTypedQualifications(it.expectedQualifications)
            .produce()
        },
      ),
    )

    var iteration = 1
    repeat(20) {
      seedService.seedData(SeedFileType.user, "users-many-times-base-job.csv")

      seedInfos.forEach {
        val persistedUser = userRepository.findByDeliusUsername(it.staffUserDetails.username!!.uppercase())!!

        it.iterationValidations += IterationValidation(
          rolesCorrect = persistedUser.roles.map(UserRoleAssignmentEntity::role).containsAll(it.expectedRoles),
          qualificationsCorrect = persistedUser.qualifications.map(UserQualificationAssignmentEntity::qualification).containsAll(it.expectedQualifications),
        )
      }

      iteration += 1
    }

    seedInfos.forEach {
      println("${it.staffUserDetails.username}\n" + it.iterationValidations.mapIndexed { index, validation -> "   Run $index: roles correct = ${validation.rolesCorrect}, qalifications correct = ${validation.qualificationsCorrect}" }.joinToString("\n"))
    }

    seedInfos.forEach {
      it.iterationValidations.forEach {
        assertThat(it.rolesCorrect).isTrue
        assertThat(it.qualificationsCorrect).isTrue
      }
    }
  }

  @Test
  fun `Seeding same users multiple times works every time for AP user seed job`() {
    val probationRegion = givenAProbationRegion()

    val probationRegionDeliusMapping = probationAreaProbationRegionMappingFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    val seedInfos = listOf(
      SeedInfo(
        staffUserDetails = StaffDetailFactory.staffDetail(
          probationArea = ProbationArea(
            code = probationRegionDeliusMapping.probationAreaDeliusCode,
            description = randomStringMultiCaseWithNumbers(10),
          ),
        ),
        expectedRoles = listOf(UserRole.CAS1_FUTURE_MANAGER, UserRole.CAS1_WORKFLOW_MANAGER, UserRole.CAS1_ASSESSOR),
        expectedQualifications = listOf(UserQualification.EMERGENCY, UserQualification.LAO),
      ),
      SeedInfo(
        staffUserDetails = StaffDetailFactory.staffDetail(
          probationArea = ProbationArea(
            code = probationRegionDeliusMapping.probationAreaDeliusCode,
            description = randomStringMultiCaseWithNumbers(10),
          ),
        ),
        expectedRoles = listOf(UserRole.CAS1_FUTURE_MANAGER),
        expectedQualifications = listOf(),
      ),
      SeedInfo(
        staffUserDetails = StaffDetailFactory.staffDetail(
          probationArea = ProbationArea(
            code = probationRegionDeliusMapping.probationAreaDeliusCode,
            description = randomStringMultiCaseWithNumbers(10),
          ),
        ),
        expectedRoles = listOf(),
        expectedQualifications = listOf(UserQualification.LAO),
      ),
    )

    seedInfos.forEach {
      apDeliusContextAddStaffDetailResponse(
        it.staffUserDetails,
      )
    }

    withCsv(
      "users-many-times-ap-job",
      userRoleAssignmentSeedCsvRowsToCsv(
        seedInfos.map {
          UserRoleAssignmentsSeedCsvRowFactory()
            .withDeliusUsername(it.staffUserDetails.username!!)
            .withTypedRoles(it.expectedRoles)
            .withTypedQualifications(it.expectedQualifications)
            .produce()
        },
      ),
    )

    var iteration = 1
    repeat(20) {
      seedService.seedData(SeedFileType.approvedPremisesUsers, "users-many-times-ap-job.csv")

      seedInfos.forEach {
        val persistedUser = userRepository.findByDeliusUsername(it.staffUserDetails.username!!.uppercase())!!

        it.iterationValidations += IterationValidation(
          rolesCorrect = persistedUser.roles.map(UserRoleAssignmentEntity::role).containsAll(it.expectedRoles),
          qualificationsCorrect = persistedUser.qualifications.map(UserQualificationAssignmentEntity::qualification).containsAll(it.expectedQualifications),
        )
      }

      iteration += 1
    }

    seedInfos.forEach {
      println("${it.staffUserDetails.username}\n" + it.iterationValidations.mapIndexed { index, validation -> "   Run $index: roles correct = ${validation.rolesCorrect}, qalifications correct = ${validation.qualificationsCorrect}" }.joinToString("\n"))
    }

    seedInfos.forEach {
      it.iterationValidations.forEach {
        assertThat(it.rolesCorrect).isTrue
        assertThat(it.qualificationsCorrect).isTrue
      }
    }
  }

  @Test
  fun `Attempting to assign new role CAS3_REPORTER to a currently known user succeeds`() {
    userEntityFactory.produceAndPersist {
      withDeliusUsername("KNOWN-USER")
      withYieldedProbationRegion { givenAProbationRegion() }
    }

    withCsv(
      "known-user",
      userRoleAssignmentSeedCsvRowsToCsv(
        listOf(
          UserRoleAssignmentsSeedCsvRowFactory()
            .withDeliusUsername("KNOWN-USER")
            .withTypedRoles(listOf(UserRole.CAS3_REPORTER))
            .withTypedQualifications(listOf(UserQualification.PIPE))
            .produce(),
        ),
      ),
    )

    seedService.seedData(SeedFileType.user, "known-user.csv")

    val persistedUser = userRepository.findByDeliusUsername("KNOWN-USER")

    assertThat(persistedUser).isNotNull
    assertThat(persistedUser!!.roles.map(UserRoleAssignmentEntity::role)).containsExactlyInAnyOrder(
      UserRole.CAS3_REPORTER,
    )
    assertThat(persistedUser.qualifications.map(UserQualificationAssignmentEntity::qualification)).containsExactlyInAnyOrder(
      UserQualification.PIPE,
    )
  }

  private fun userRoleAssignmentSeedCsvRowsToCsv(rows: List<UsersSeedUntypedEnumsCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "deliusUsername",
        "roles",
        "qualifications",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.deliusUsername)
        .withQuotedField(it.roles.joinToString(","))
        .withQuotedField(it.qualifications.joinToString(","))
        .newRow()
    }

    return builder.build()
  }
}

class UserRoleAssignmentsSeedCsvRowFactory : Factory<UsersSeedUntypedEnumsCsvRow> {
  private var deliusUsername: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var roles: Yielded<List<String>> = { listOf(UserRole.CAS1_ASSESSOR.name) }
  private var qualifications: Yielded<List<String>> = { listOf(UserQualification.PIPE.name) }

  fun withDeliusUsername(deliusUsername: String) = apply {
    this.deliusUsername = { deliusUsername }
  }

  fun withTypedRoles(roles: List<UserRole>) = apply {
    this.roles = { roles.map { it.name } }
  }

  fun withUntypedRoles(roles: List<String>) = apply {
    this.roles = { roles }
  }

  fun withTypedQualifications(qualifications: List<UserQualification>) = apply {
    this.qualifications = { qualifications.map { it.name } }
  }

  fun withUntypedQualifications(qualifications: List<String>) = apply {
    this.qualifications = { qualifications }
  }

  override fun produce() = UsersSeedUntypedEnumsCsvRow(
    deliusUsername = this.deliusUsername(),
    roles = this.roles(),
    qualifications = this.qualifications(),
  )
}

data class UsersSeedUntypedEnumsCsvRow(
  val deliusUsername: String,
  val roles: List<String>,
  val qualifications: List<String>,
)

data class SeedInfo(
  val staffUserDetails: StaffDetail,
  val expectedRoles: List<UserRole>,
  val expectedQualifications: List<UserQualification>,
  val iterationValidations: MutableList<IterationValidation> = mutableListOf(),
)

data class IterationValidation(
  val rolesCorrect: Boolean,
  val qualificationsCorrect: Boolean,
)
