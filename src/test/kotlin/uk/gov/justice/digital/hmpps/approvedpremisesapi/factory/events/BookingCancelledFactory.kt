package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingCancelled
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class BookingCancelledFactory : Factory<BookingCancelled> {
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var deliusEventNumber: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var bookingId: Yielded<UUID> = { UUID.randomUUID() }
  private var cancelledAt: Yielded<Instant> = { Instant.now() }
  private var cancelledAtDate: Yielded<LocalDate> = { LocalDate.now() }
  private var cancelledBy: Yielded<StaffMember> = { StaffMemberFactory().produce() }
  private var premises: Yielded<Premises> = { EventPremisesFactory().produce() }
  private var arrivalOn: Yielded<LocalDate> = { LocalDate.now() }
  private var departureOn: Yielded<LocalDate> = { LocalDate.now() }
  private var cancellationReason: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var cancellationRecordedAt: Yielded<Instant> = { Instant.now() }

  fun withApplicationId(applicationId: UUID) = apply {
    this.applicationId = { applicationId }
  }

  fun withApplicationUrl(applicationUrl: String) = apply {
    this.applicationUrl = { applicationUrl }
  }

  fun withPersonReference(personReference: PersonReference) = apply {
    this.personReference = { personReference }
  }

  fun withDeliusEventNumber(deliusEventNumber: String) = apply {
    this.deliusEventNumber = { deliusEventNumber }
  }

  fun withBookingId(bookingId: UUID) = apply {
    this.bookingId = { bookingId }
  }

  fun withCancelledAt(cancelledAt: Instant) = apply {
    this.cancelledAt = { cancelledAt }
  }

  fun withCancelledAtDate(cancelledAtDate: LocalDate) = apply {
    this.cancelledAtDate = { cancelledAtDate }
  }

  fun withCancelledBy(cancelledBy: StaffMember) = apply {
    this.cancelledBy = { cancelledBy }
  }

  fun withPremises(premises: Premises) = apply {
    this.premises = { premises }
  }

  fun withArrivalOn(arrivalOn: LocalDate) = apply {
    this.arrivalOn = { arrivalOn }
  }

  fun withDepartureOn(departureOn: LocalDate) = apply {
    this.departureOn = { departureOn }
  }

  fun withCancellationReason(cancellationReason: String) = apply {
    this.cancellationReason = { cancellationReason }
  }

  fun withCancellationRecordedAt(cancellationRecordedAt: Instant) = apply {
    this.cancellationRecordedAt = { cancellationRecordedAt }
  }

  override fun produce() = BookingCancelled(
    applicationId = this.applicationId(),
    applicationUrl = this.applicationUrl(),
    personReference = this.personReference(),
    deliusEventNumber = this.deliusEventNumber(),
    bookingId = this.bookingId(),
    cancelledAt = this.cancelledAt(),
    cancelledAtDate = this.cancelledAtDate(),
    cancelledBy = this.cancelledBy(),
    premises = this.premises(),
    cancellationReason = this.cancellationReason(),
    cancellationRecordedAt = this.cancellationRecordedAt(),
  )
}
