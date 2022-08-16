package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.NonArrivalTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

class NonArrivalEntityFactory(
  nonArrivalTestRepository: NonArrivalTestRepository?
) : PersistedFactory<NonArrivalEntity, UUID>(nonArrivalTestRepository) {
  constructor() : this(null)

  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var date: Yielded<LocalDate> = { LocalDate.now().randomDateBefore() }
  private var reason: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var notes: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var booking: Yielded<BookingEntity>? = null

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withDate(date: LocalDate) = apply {
    this.date = { date }
  }

  fun withReason(reason: String) = apply {
    this.reason = { reason }
  }

  fun withNotes(notes: String) = apply {
    this.notes = { notes }
  }

  fun withYieldedBooking(booking: Yielded<BookingEntity>) = apply {
    this.booking = booking
  }

  fun withBooking(booking: BookingEntity) = apply {
    this.booking = { booking }
  }

  override fun produce(): NonArrivalEntity = NonArrivalEntity(
    id = this.id(),
    date = this.date(),
    reason = this.reason(),
    notes = this.notes(),
    booking = this.booking?.invoke() ?: throw RuntimeException("Booking must be provided")
  )
}
