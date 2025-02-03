package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService.GetUserResponse

@SuppressWarnings("TooGenericExceptionThrown", "TooGenericExceptionCaught")
abstract class AbstractUsersSeedJob(
  private val useRolesForServices: List<ServiceName>,
  private val userService: UserService,
) : SeedJob<UsersSeedCsvRow>(
  requiredHeaders = setOf(
    "delius_username",
    "roles",
    "qualifications",
    "remove_existing_roles_and_qualifications",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>): UsersSeedCsvRow {
    val seedColumns = SeedColumns(columns)

    return UsersSeedCsvRow(
      deliusUsername = seedColumns.getStringOrNull("delius_username")!!.uppercase(),
      roles = parseAllRolesOrThrow(seedColumns.getCommaSeparatedValues("roles")),
      qualifications = parseAllQualificationsOrThrow(seedColumns.getCommaSeparatedValues("qualifications")),
      removeExistingRolesAndQualifications = seedColumns.getYesNoBooleanOrNull("remove_existing_roles_and_qualifications")!!,
    )
  }

  override fun processRow(row: UsersSeedCsvRow) {
    val username = row.deliusUsername
    val removeExistingRolesAndQualifications = row.removeExistingRolesAndQualifications

    val rolesString = row.roles.joinToString(",")
    val qualsString = row.qualifications.joinToString(",")

    log.info(
      "Adding/updating $username. Roles $rolesString, Qualifications $qualsString. " +
        "Remove existing roles and qualifications? $removeExistingRolesAndQualifications",
    )

    val user = try {
      when (val result = userService.getExistingUserOrCreate(username)) {
        GetUserResponse.StaffRecordNotFound -> throw RuntimeException("Could not find staff record for user $username")
        is GetUserResponse.Success -> result.user
      }
    } catch (exception: Exception) {
      throw RuntimeException("Could not get user $username", exception)
    }

    if (row.removeExistingRolesAndQualifications) {
      useRolesForServices.forEach { userService.clearRolesForService(user, it) }
      userService.clearQualifications(user)
    }

    row.roles.forEach {
      userService.addRoleToUser(user, it)
    }
    row.qualifications.forEach {
      userService.addQualificationToUser(user, it)
    }
  }

  private fun parseAllRolesOrThrow(roleNames: List<String>): List<UserRole> {
    val unknownRoles = mutableListOf<String>()

    val roles = roleNames.mapNotNull {
      try {
        UserRole.valueOf(it)
      } catch (_: Exception) {
        unknownRoles += it
        null
      }
    }

    if (unknownRoles.any()) {
      throw RuntimeException("Unrecognised User Role(s): $unknownRoles")
    }

    return roles
  }

  private fun parseAllQualificationsOrThrow(qualificationNames: List<String>): List<UserQualification> {
    val unknownQualifications = mutableListOf<String>()

    val roles = qualificationNames.mapNotNull {
      try {
        UserQualification.valueOf(it)
      } catch (_: Exception) {
        unknownQualifications += it
        null
      }
    }

    if (unknownQualifications.any()) {
      throw RuntimeException("Unrecognised User Qualifications(s): $unknownQualifications")
    }

    return roles
  }
}

data class UsersSeedCsvRow(
  val deliusUsername: String,
  val roles: List<UserRole>,
  val qualifications: List<UserQualification>,
  val removeExistingRolesAndQualifications: Boolean,
)
