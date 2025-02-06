package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ManagePomCasesClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PomAllocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PomDeallocated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PomNotAllocated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PrisonerLocationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PrisonerLocationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.HmppsDomainEvent
import java.net.URI
import java.time.ZoneOffset
import java.util.UUID

@ConditionalOnProperty(prefix = "feature-flags", name = ["cas2-sqs-listener-enabled"], havingValue = "true")
@Service
class PomAllocationService(
  private val prisonerLocationRepository: PrisonerLocationRepository,
  private val managePomCasesClient: ManagePomCasesClient,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun handlePomAllocationChangedMessage(message: HmppsDomainEvent) = try {
    val pomAllocation = try {
      message.detailUrl?.let { managePomCasesClient.getPomAllocation(URI.create(it)) }
        ?: throw Exception(
          "No POM Allocation data available, ${mapOf("detailUrl" to message.detailUrl.orNotProvided())}",
        )
    } catch (e: HttpStatusCodeException) {
      when (e.statusCode) {
        HttpStatus.NOT_FOUND -> {
          val error = e.getResponseBodyAs(ErrorResponse::class.java)
          when (error?.message) {
            "Not allocated" -> PomDeallocated
            else -> PomNotAllocated
          }
        }

        else -> throw e
      }
    }

    val nomsNumber = message.personReference.findNomsNumber() ?: throw Exception("PersonNotFound")
    val startDate = message.occurredAt.toInstant().atOffset(ZoneOffset.UTC)
    val staffCode = message.staffCode ?: throw Exception("StaffCodeNotFound")

    when (pomAllocation) {
      is PomAllocation -> {
        prisonerLocationRepository.updateEndDateOfLatest(nomsNumber, startDate)

        prisonerLocationRepository.save(
          PrisonerLocationEntity(
            id = UUID.randomUUID(),
            nomsNumber = nomsNumber,
            prisonCode = pomAllocation.prison.code,
            pomId = staffCode,
            startDate = startDate,
            endDate = null,
          ),
        )
      }

      is PomDeallocated -> {
        throw Exception("PomDeallocated")
      }

      else -> {
        throw Exception("PomNotAllocated")
      }
    }
  } catch (e: Exception) {
    log.error(e.message)

    log.error("Failed to handlePomAllocationChangedMessage")
  }

  fun String?.orNotProvided() = this ?: "Not Provided"
}

data class ErrorResponse(val message: String?)
