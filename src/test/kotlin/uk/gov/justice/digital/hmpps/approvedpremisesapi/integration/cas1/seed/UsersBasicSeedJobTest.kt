package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddStaffDetailResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockNotFoundStaffDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.ApStaffUserSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.OffsetDateTime

class UsersBasicSeedJobTest : SeedTestBase() {
  @Test
  fun `Seeding a non existent user logs an error`() {
    apDeliusContextMockNotFoundStaffDetailCall("INVALID-USER")

    withCsv(
      "invalid-user",
      apStaffUserSeedCsvRowsToCsv(
        listOf(
          ApStaffUserSeedCsvRowFactory()
            .withDeliusUsername("INVALID-USER")
            .produce(),
        ),
      ),
    )

    seedService.seedData(SeedFileType.usersBasic, "invalid-user.csv")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message.contains("Error on row 1:") &&
        it.throwable != null &&
        it.throwable.cause != null &&
        it.throwable.cause!!.message!!.contains("Could not find staff record for user INVALID-USER")
    }
  }

  @Test
  fun `Seeding a real but currently unknown user succeeds`() {
    val probationRegion = givenAProbationRegion()

    val probationRegionDeliusMapping = probationAreaProbationRegionMappingFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    apDeliusContextAddStaffDetailResponse(
      StaffDetailFactory.staffDetail(
        deliusUsername = "UNKNOWN-USER",
        probationArea = ProbationArea(
          code = probationRegionDeliusMapping.probationAreaDeliusCode,
          description = "description",
        ),
      ),
    )

    withCsv(
      "unknown-user",
      apStaffUserSeedCsvRowsToCsv(
        listOf(
          ApStaffUserSeedCsvRowFactory()
            .withDeliusUsername("unknown-user")
            .produce(),
        ),
      ),
    )

    seedService.seedData(SeedFileType.usersBasic, "unknown-user.csv")

    val persistedUser = userRepository.findByDeliusUsername("UNKNOWN-USER")

    assertThat(persistedUser).isNotNull

    assertThat(logEntries).anyMatch {
      it.level == "info" &&
        it.message.contains("User record for 'UNKNOWN-USER' created")
    }
  }

  @Test fun `Seeding an existing user leaves roles and qualifications untouched`() {
    val user = userEntityFactory.produceAndPersist {
      withDeliusUsername("PRE-EXISTING-USER")
      withUpdatedAt(OffsetDateTime.now().minusDays(3))
      withYieldedProbationRegion { givenAProbationRegion() }
    }

    val roleEntities = listOf(UserRole.CAS1_ASSESSOR, UserRole.CAS1_CRU_MEMBER).map { role ->
      userRoleAssignmentEntityFactory.produceAndPersist {
        withUser(user)
        withRole(role)
      }
    }
    user.roles.addAll(roleEntities)

    val qualificationEntities = listOf(UserQualification.PIPE, UserQualification.EMERGENCY).map { qualification ->
      userQualificationAssignmentEntityFactory.produceAndPersist {
        withUser(user)
        withQualification(qualification)
      }
    }
    user.qualifications.addAll(qualificationEntities)

    withCsv(
      "pre-existing-user",
      apStaffUserSeedCsvRowsToCsv(
        listOf(
          ApStaffUserSeedCsvRowFactory()
            .withDeliusUsername("PRE-EXISTING-USER")
            .produce(),
        ),
      ),
    )

    seedService.seedData(SeedFileType.usersBasic, "pre-existing-user.csv")

    val persistedUser = userRepository.findByDeliusUsername("PRE-EXISTING-USER")

    assertThat(persistedUser).isNotNull

    assertThat(persistedUser!!.roles.map(UserRoleAssignmentEntity::role)).containsExactlyInAnyOrder(
      UserRole.CAS1_ASSESSOR,
      UserRole.CAS1_CRU_MEMBER,
    )
    assertThat(persistedUser.qualifications.map(UserQualificationAssignmentEntity::qualification)).containsExactlyInAnyOrder(
      UserQualification.PIPE,
      UserQualification.EMERGENCY,
    )

    assertThat(logEntries).anyMatch {
      it.level == "info" &&
        it.message.contains("User record for 'PRE-EXISTING-USER' already exists. Last updated")
    }
  }

  private fun apStaffUserSeedCsvRowsToCsv(rows: List<ApStaffUserSeedCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "deliusUsername",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.deliusUsername)
        .newRow()
    }

    return builder.build()
  }
}

class ApStaffUserSeedCsvRowFactory : Factory<ApStaffUserSeedCsvRow> {
  private var deliusUsername: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }

  fun withDeliusUsername(deliusUsername: String) = apply {
    this.deliusUsername = { deliusUsername }
  }

  override fun produce() = ApStaffUserSeedCsvRow(
    deliusUsername = this.deliusUsername(),
  )
}
