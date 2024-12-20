package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationTimelineNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiFormat
import java.util.UUID

@Component
class Cas1LinkedBookingToPlacementRequestSeedJob(
  private val placementRequestRepository: PlacementRequestRepository,
  private val bookingRepository: BookingRepository,
  private val applicationTimelineNoteService: ApplicationTimelineNoteService,
) : SeedJob<Cas1LinkBookingToPlacementRequestSeedJobCsvRow>(
  requiredHeaders = setOf(
    "booking_id",
    "placement_request_id",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas1LinkBookingToPlacementRequestSeedJobCsvRow(
    bookingId = UUID.fromString(columns["booking_id"]!!.trim()),
    placementRequestId = UUID.fromString(columns["placement_request_id"]!!.trim()),
  )

  override fun processRow(row: Cas1LinkBookingToPlacementRequestSeedJobCsvRow) {
    val placementRequestId = row.placementRequestId
    val bookingId = row.bookingId

    log.info("Attempting to link booking $bookingId to placement request $placementRequestId")

    val placementRequest = placementRequestRepository.findByIdOrNull(placementRequestId)
      ?: error("Could not find placement request with id $placementRequestId")

    val booking = bookingRepository.findByIdOrNull(bookingId)
      ?: error("Could not find booking with id $bookingId")

    if (booking.adhoc != true) {
      error("Can't link to non-adhoc booking $bookingId")
    }

    if (booking.application?.id != placementRequest.application.id) {
      error("Can only link booking to placement request from same application")
    }

    if (booking.placementRequest != null) {
      error("Booking $bookingId is already linked to a placement request $placementRequestId")
    }

    if (placementRequest.booking != null) {
      error("Placement Request $placementRequestId is already linked to booking $bookingId")
    }

    placementRequest.booking = booking
    placementRequestRepository.save(placementRequest)

    applicationTimelineNoteService.saveApplicationTimelineNote(
      applicationId = placementRequest.application.id,
      note = "Adhoc booking with arrival date '${booking.arrivalDate.toUiFormat()}' linked to corresponding request for placement by Application Support",
      user = null,
    )

    log.info("Have linked booking $bookingId to placement request $placementRequestId")
  }
}

data class Cas1LinkBookingToPlacementRequestSeedJobCsvRow(
  val bookingId: UUID,
  val placementRequestId: UUID,
)
