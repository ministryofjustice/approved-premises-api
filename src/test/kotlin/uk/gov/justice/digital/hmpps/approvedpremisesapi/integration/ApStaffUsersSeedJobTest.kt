package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.communityApiMockNotFoundOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.communityApiMockSuccessfulStaffUserDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.ApStaffUserSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.OffsetDateTime

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class APStaffUsersSeedJobTest : SeedTestBase() {
  @Test
  fun `Attempting to seed a non existent user logs an error`() {
    communityApiMockNotFoundOffenderDetailsCall("INVALID-USER")

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

    seedService.seedData(SeedFileType.approvedPremisesApStaffUsers, "invalid-user")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message.contains("Error on row 1:") &&
        it.throwable != null &&
        it.throwable.cause != null &&
        it.throwable.message!!.contains("Could not get user INVALID-USER") &&
        it.throwable.cause!!.message!!.contains("Unable to complete GET request to /secure/staff/username/INVALID-USER")
    }
  }

  @Test
  fun `Attempting to seed a real but currently unknown user succeeds`() {
    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
    }

    val probationRegionDeliusMapping = probationAreaProbationRegionMappingFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    communityApiMockSuccessfulStaffUserDetailsCall(
      StaffUserDetailsFactory()
        .withUsername("UNKNOWN-USER")
        .withStaffIdentifier(6789)
        .withProbationAreaCode(probationRegionDeliusMapping.probationAreaDeliusCode)
        .produce(),
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

    seedService.seedData(SeedFileType.approvedPremisesApStaffUsers, "unknown-user")

    val persistedUser = userRepository.findByDeliusUsername("UNKNOWN-USER")

    assertThat(persistedUser).isNotNull
    assertThat(persistedUser!!.deliusStaffIdentifier).isEqualTo(6789)

    assertThat(logEntries).anyMatch {
      it.level == "info" &&
        it.message.contains("User record for: UNKNOWN-USER last updated")
    }
  }

  @Test fun `Seeding a pre-existing user leaves roles and qualifications untouched`() {
    val user = userEntityFactory.produceAndPersist {
      withDeliusUsername("PRE-EXISTING-USER")
      withUpdatedAt(OffsetDateTime.now().minusDays(3))
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist {
          withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
        }
      }
    }

    val roleEntities = listOf(UserRole.CAS1_ASSESSOR, UserRole.CAS1_WORKFLOW_MANAGER).map { role ->
      userRoleAssignmentEntityFactory.produceAndPersist {
        withUser(user)
        withRole(role)
      }
    }
    user.roles.addAll(roleEntities)

    val qualificationEntities = listOf(UserQualification.PIPE, UserQualification.WOMENS).map { qualification ->
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

    seedService.seedData(SeedFileType.approvedPremisesApStaffUsers, "pre-existing-user")

    val persistedUser = userRepository.findByDeliusUsername("PRE-EXISTING-USER")

    assertThat(persistedUser).isNotNull

    assertThat(persistedUser!!.roles.map(UserRoleAssignmentEntity::role)).containsExactlyInAnyOrder(
      UserRole.CAS1_ASSESSOR,
      UserRole.CAS1_WORKFLOW_MANAGER,
    )
    assertThat(persistedUser.qualifications.map(UserQualificationAssignmentEntity::qualification)).containsExactlyInAnyOrder(
      UserQualification.PIPE,
      UserQualification.WOMENS,
    )

    assertThat(logEntries).anyMatch {
      it.level == "info" &&
        it.message.contains("User record for: PRE-EXISTING-USER last updated")
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
