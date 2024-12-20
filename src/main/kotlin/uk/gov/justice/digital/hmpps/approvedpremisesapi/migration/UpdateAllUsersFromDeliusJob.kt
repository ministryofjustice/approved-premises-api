package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService

@Component
class UpdateAllUsersFromDeliusJob(
  private val userRepository: UserRepository,
  private val userService: UserService,
) : MigrationJob() {
  private val log = LoggerFactory.getLogger(this::class.java)
  override val shouldRunInTransaction = false

  @SuppressWarnings("TooGenericExceptionCaught", "MagicNumber")
  override fun process(pageSize: Int) {
    userRepository.findAll().forEach {
      log.info("Updating user ${it.id}")
      try {
        when (userService.updateUserFromDelius(it, ServiceName.approvedPremises)) {
          UserService.GetUserResponse.StaffRecordNotFound -> log.error("Unable to update ${it.id}, no staff record")
          is UserService.GetUserResponse.Success -> {}
        }
      } catch (exception: Exception) {
        log.error("Unable to update user ${it.id}", exception)
      }
      Thread.sleep(500)
    }
  }
}
