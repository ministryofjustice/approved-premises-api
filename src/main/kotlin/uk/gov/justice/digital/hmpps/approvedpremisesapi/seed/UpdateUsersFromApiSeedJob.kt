package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService

@Component
class UpdateUsersFromApiSeedJob(
  val userService: UserService,
) : SeedJob<UpdateUserFromApiCsvRow>(
  requiredHeaders = setOf(
    "delius_username",
    "service_name",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = UpdateUserFromApiCsvRow(
    deliusUsername = columns["delius_username"]!!.trim(),
    serviceName = ServiceName.valueOf(columns["service_name"]!!.trim()),
  )

  override fun processRow(row: UpdateUserFromApiCsvRow) {
    val username = row.deliusUsername
    val service = row.serviceName

    log.info("Updating user with username $username for service $service")
    val user = userService.getExistingUserOrCreateDeprecated(username)
    when (userService.updateUserFromDelius(user, service)) {
      UserService.GetUserResponse.StaffRecordNotFound -> error("Could not find staff record for user $username")
      is UserService.GetUserResponse.Success -> { }
    }
  }
}

data class UpdateUserFromApiCsvRow(
  val deliusUsername: String,
  val serviceName: ServiceName,
)
