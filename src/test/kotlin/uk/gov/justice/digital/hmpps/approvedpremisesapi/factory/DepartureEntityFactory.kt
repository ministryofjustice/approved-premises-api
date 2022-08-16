package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DestinationProviderEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.DepartureTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.OffsetDateTime
import java.util.UUID

class DepartureEntityFactory(
  departureTestRepository: DepartureTestRepository?
) : PersistedFactory<DepartureEntity, UUID>(departureTestRepository) {
  constructor() : this(null)

  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var dateTime: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeAfter() }
  private var reason: Yielded<DepartureReasonEntity>? = null
  private var moveOnCategory: Yielded<MoveOnCategoryEntity>? = null
  private var destinationProvider: Yielded<DestinationProviderEntity>? = null
  private var notes: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var booking: Yielded<BookingEntity>? = null

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withDateTime(dateTime: OffsetDateTime) = apply {
    this.dateTime = { dateTime }
  }

  fun withYieldedReason(reason: Yielded<DepartureReasonEntity>) = apply {
    this.reason = reason
  }

  fun withReason(reason: DepartureReasonEntity) = apply {
    this.reason = { reason }
  }

  fun withYieldedMoveOnCategory(moveOnCategory: Yielded<MoveOnCategoryEntity>) = apply {
    this.moveOnCategory = moveOnCategory
  }

  fun withMoveOnCategory(moveOnCategory: MoveOnCategoryEntity) = apply {
    this.moveOnCategory = { moveOnCategory }
  }

  fun withNotes(notes: String) = apply {
    this.notes = { notes }
  }

  fun withYieldedDestinationProvider(destinationProvider: Yielded<DestinationProviderEntity>) = apply {
    this.destinationProvider = destinationProvider
  }

  fun withDestinationProvider(destinationProvider: DestinationProviderEntity) = apply {
    this.destinationProvider = { destinationProvider }
  }

  fun withYieldedBooking(booking: Yielded<BookingEntity>) = apply {
    this.booking = booking
  }

  fun withBooking(booking: BookingEntity) = apply {
    this.booking = { booking }
  }

  override fun produce(): DepartureEntity = DepartureEntity(
    id = this.id(),
    dateTime = this.dateTime(),
    reason = this.reason?.invoke() ?: throw RuntimeException("Reason must be provided"),
    moveOnCategory = this.moveOnCategory?.invoke() ?: throw RuntimeException("MoveOnCategory must be provided"),
    destinationProvider = this.destinationProvider?.invoke() ?: throw RuntimeException("DestinationProvider must be provided"),
    notes = this.notes(),
    booking = this.booking?.invoke() ?: throw RuntimeException("Booking must be provided")
  )
}
