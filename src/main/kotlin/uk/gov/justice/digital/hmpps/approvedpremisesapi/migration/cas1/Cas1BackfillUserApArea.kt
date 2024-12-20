package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService

@Component
class Cas1BackfillUserApArea(
  private val userRepository: UserRepository,
  private val userService: UserService,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val transactionTemplate: TransactionTemplate,
) : MigrationJob() {
  private val log = LoggerFactory.getLogger(this::class.java)
  override val shouldRunInTransaction = false

  @SuppressWarnings("MagicNumber")
  override fun process(pageSize: Int) {
    userRepository.findAll().forEach { user ->
      transactionTemplate.executeWithoutResult {
        updateUser(user)
      }
      Thread.sleep(50)
    }

    log.info("ap area update complete")
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun updateUser(user: UserEntity) {
    log.info("Update ap area and teams for user ${user.id}")
    try {
      val staffDetailsResult = apDeliusContextApiClient.getStaffDetail(user.deliusUsername)

      when (staffDetailsResult) {
        is ClientResult.Success -> {
          log.info("Updating user ${user.id} using the user update service")

          val updatedUser = userService.updateUserEntity(
            user,
            staffDetailsResult.body,
            ServiceName.approvedPremises,
            user.deliusUsername,
          )

          log.info(
            "Updated user ${updatedUser.id} probation region to ${updatedUser.probationRegion.name}, " +
              "AP Area to ${updatedUser.apArea?.name} " +
              "and team codes to ${updatedUser.teamCodes}",
          )
        }
        is ClientResult.Failure.StatusCode -> {
          if (staffDetailsResult.status == HttpStatus.NOT_FOUND) {
            log.warn("Could not find staff details for ${user.id}, will fall back to probationRegion.apArea")

            val apArea = user.probationRegion.apArea!!
            val teamCodes = emptyList<String>()

            log.info("Updating user ${user.id} AP Area to ${apArea.name} and team codes to $teamCodes")

            val userToUpdate = userRepository.findByIdOrNull(user.id)!!
            userToUpdate.apArea = apArea
            userToUpdate.teamCodes = teamCodes
            userRepository.save(userToUpdate)
          } else {
            staffDetailsResult.throwException()
          }
        }
        is ClientResult.Failure -> {
          staffDetailsResult.throwException()
        }
      }
    } catch (exception: Exception) {
      log.error("Unable to find ap area for user ${user.id}", exception)
    }
  }
}
