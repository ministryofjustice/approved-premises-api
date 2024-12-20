package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository

@Component
class BookingStatusMigrationJob(
  private val bookingRepository: BookingRepository,
  private val entityManager: EntityManager,
) : MigrationJob() {
  private val log = LoggerFactory.getLogger(this::class.java)
  override val shouldRunInTransaction = true

  override fun process(pageSize: Int) {
    log.info("Starting Booking Migration process...")

    var page = 1
    var hasNext = true
    var slice: Slice<BookingEntity>

    while (hasNext) {
      log.info("Getting page $page")
      slice = bookingRepository.findAllCas3bookingsWithNullStatus(PageRequest.of(0, pageSize))
      slice.content.forEach {
        setStatus(it)
      }
      entityManager.clear()
      hasNext = slice.hasNext()
      page += 1
    }
    log.info("Booking Status Migration process complete!")
  }

  private fun setStatus(booking: BookingEntity) {
    booking.status = when {
      booking.cancellations.isNotEmpty() -> BookingStatus.cancelled
      booking.departures.isNotEmpty() -> BookingStatus.departed
      booking.arrivals.isNotEmpty() -> BookingStatus.arrived
      booking.confirmation != null -> BookingStatus.confirmed
      else -> BookingStatus.provisional
    }

    log.info("Updating booking status ${booking.id} to ${booking.status}")
    entityManager.detach(booking)
    bookingRepository.updateBookingStatus(booking.id, booking.status!!)
  }
}
