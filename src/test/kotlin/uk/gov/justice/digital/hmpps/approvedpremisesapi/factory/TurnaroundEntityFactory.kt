package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TurnaroundEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import java.time.OffsetDateTime
import java.util.UUID

class TurnaroundEntityFactory : Factory<TurnaroundEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var workingDayCount: Yielded<Int> = { randomInt(0, 14) }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().minusDays(14L).randomDateTimeBefore(14) }
  private var booking: Yielded<BookingEntity>? = null

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withWorkingDayCount(workingDayCount: Int) = apply {
    this.workingDayCount = { workingDayCount }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withBooking(booking: BookingEntity) = apply {
    this.booking = { booking }
  }

  fun withYieldedBooking(booking: Yielded<BookingEntity>) = apply {
    this.booking = booking
  }

  override fun produce() = TurnaroundEntity(
    id = this.id(),
    workingDayCount = this.workingDayCount(),
    createdAt = this.createdAt(),
    booking = this.booking?.invoke() ?: throw RuntimeException("Must provide a Booking"),
  )
}
