package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
/**
 * Seeds users, along with their roles and qualifications.
 *
 *  NB: this clears roles and qualifications.
 *
 *  If you want to seed users without touching the pre-existing
 *  roles/qualifications of pre-existing users, then consider
 *  using ApStaffUsersSeedJob.
 */
@SuppressWarnings("TooGenericExceptionThrown", "TooGenericExceptionCaught")
abstract class AbstractUsersSeedJob(
  private val useRolesForServices: List<ServiceName>,
  private val userService: UserService,
) : SeedJob<UsersSeedCsvRow>(
  requiredHeaders = setOf(
    "deliusUsername",
    "roles",
    "qualifications",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = UsersSeedCsvRow(
    deliusUsername = columns["deliusUsername"]!!.trim().uppercase(),
    roles = parseAllRolesOrThrow(columns["roles"]!!.split(",").filter(String::isNotBlank).map(String::trim)),
    qualifications = parseAllQualificationsOrThrow(columns["qualifications"]!!.split(",").filter(String::isNotBlank).map(String::trim)),
  )

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

  override fun processRow(row: UsersSeedCsvRow) {
    log.info("Setting roles for ${row.deliusUsername} to exactly ${row.roles.joinToString(",")}, qualifications to exactly: ${row.qualifications.joinToString(",")}")

    val user = try {
      userService.getExistingUserOrCreateDeprecated(row.deliusUsername)
    } catch (exception: Exception) {
      throw RuntimeException("Could not get user ${row.deliusUsername}", exception)
    }

    useRolesForServices.forEach { userService.clearRolesForService(user, it) }

    userService.clearQualifications(user)
    row.roles.forEach {
      userService.addRoleToUser(user, it)
    }
    row.qualifications.forEach {
      userService.addQualificationToUser(user, it)
    }
  }
}

data class UsersSeedCsvRow(
  val deliusUsername: String,
  val roles: List<UserRole>,
  val qualifications: List<UserQualification>,
)
