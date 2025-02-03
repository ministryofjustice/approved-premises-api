package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService.GetUserResponse
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
/**
 * Seeds users, without touching any existing user's roles and qualifications.
 *
 * If you want to set roles and qualifications as part of the seeding then look at [UsersSeedJob].
 */
@Component
class UsersBasicSeedJob(
  private val userService: UserService,
  private val seedLogger: SeedLogger,
) : SeedJob<ApStaffUserSeedCsvRow>(
  requiredHeaders = setOf(
    "deliusUsername",
  ),
) {

  override fun deserializeRow(columns: Map<String, String>) = ApStaffUserSeedCsvRow(
    deliusUsername = columns["deliusUsername"]!!.trim().uppercase(),
  )

  @SuppressWarnings("TooGenericExceptionThrown", "TooGenericExceptionCaught")
  override fun processRow(row: ApStaffUserSeedCsvRow) {
    val username = row.deliusUsername
    seedLogger.info("Ensuring user exists with username '$username'")

    val result = try {
      when (val result = userService.getExistingUserOrCreate(username)) {
        GetUserResponse.StaffRecordNotFound -> throw RuntimeException("Could not find staff record for user $username")
        is GetUserResponse.Success -> result
      }
    } catch (exception: Exception) {
      throw RuntimeException("Could not get user $username", exception)
    }
    seedLogger.info(seedingReport(result))
  }

  private fun seedingReport(response: GetUserResponse.Success): String {
    val user = response.user
    val username = user.deliusUsername
    if (response.createdOnGet) {
      return "-> User record for '$username' created"
    } else {
      val timestamp = user.updatedAt ?: user.createdAt
      val ageInMinutes = ChronoUnit.MINUTES.between(timestamp, OffsetDateTime.now())
      return "-> User record for '$username' already exists. Last updated $ageInMinutes mins ago"
    }
  }
}

data class ApStaffUserSeedCsvRow(
  val deliusUsername: String,
)
