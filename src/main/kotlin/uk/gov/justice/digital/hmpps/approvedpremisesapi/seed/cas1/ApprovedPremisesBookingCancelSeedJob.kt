package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredBySeedJob
import java.time.LocalDate
import java.util.UUID

@Component
class ApprovedPremisesBookingCancelSeedJob(
  private val bookingService: BookingService,
  private val bookingRepository: BookingRepository,
) : SeedJob<CancelBookingSeedCsvRow>(
  requiredHeaders = setOf(
    "id",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = CancelBookingSeedCsvRow(
    id = UUID.fromString(columns["id"]!!),
  )

  @SuppressWarnings("TooGenericExceptionThrown")
  override fun processRow(row: CancelBookingSeedCsvRow) {
    val errorInBookingDetailsCancellationReason = UUID.fromString("7c310cfd-3952-456d-b0ee-0f7817afe64a")

    val booking = bookingRepository.findByIdOrNull(row.id)
      ?: throw RuntimeException("No Booking with Id of ${row.id} exists")

    if (booking.service != ServiceName.approvedPremises.value) {
      throw RuntimeException("Booking ${row.id} is not an Approved Premises Booking")
    }

    val validationResult = bookingService.createCas1Cancellation(
      booking = booking,
      cancelledAt = LocalDate.now(),
      userProvidedReason = errorInBookingDetailsCancellationReason,
      notes = null,
      otherReason = null,
      withdrawalContext = WithdrawalContext(
        withdrawalTriggeredBy = WithdrawalTriggeredBySeedJob,
        triggeringEntityType = WithdrawableEntityType.Booking,
        triggeringEntityId = booking.id,
      ),
    )

    when (validationResult) {
      is CasResult.ConflictError -> throw RuntimeException("Conflict trying to create Cancellation: ${validationResult.message}")
      is CasResult.FieldValidationError -> throw RuntimeException("Field error trying to create Cancellation: ${validationResult.validationMessages}")
      is CasResult.GeneralValidationError -> throw RuntimeException("General error trying to create Cancellation: ${validationResult.message}")
      is CasResult.Success -> log.info("Cancelled Booking ${row.id}")
      is CasResult.NotFound -> throw RuntimeException("Not found error trying to create Cancellation: ${validationResult.id} ${validationResult.entityType}")
      is CasResult.Unauthorised -> throw RuntimeException("Unauthorised trying to create Cancellation")
    }
  }
}

data class CancelBookingSeedCsvRow(
  val id: UUID,
)
