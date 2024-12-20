package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeBookedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class BookingMadeFactory : Factory<BookingMade> {
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var deliusEventNumber: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var bookingId: Yielded<UUID> = { UUID.randomUUID() }
  private var createdAt: Yielded<Instant> = { Instant.now() }
  private var bookedBy: Yielded<BookingMadeBookedBy> = { BookingMadeBookedByFactory().produce() }
  private var premises: Yielded<Premises> = { EventPremisesFactory().produce() }
  private var arrivalOn: Yielded<LocalDate> = { LocalDate.now() }
  private var departureOn: Yielded<LocalDate> = { LocalDate.now() }

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

  fun withCreatedAt(createdAt: Instant) = apply {
    this.createdAt = { createdAt }
  }

  fun withBookedBy(bookedBy: BookingMadeBookedBy) = apply {
    this.bookedBy = { bookedBy }
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

  override fun produce() = BookingMade(
    applicationId = this.applicationId(),
    applicationUrl = this.applicationUrl(),
    personReference = this.personReference(),
    deliusEventNumber = this.deliusEventNumber(),
    bookingId = this.bookingId(),
    createdAt = this.createdAt(),
    bookedBy = this.bookedBy(),
    premises = this.premises(),
    arrivalOn = this.arrivalOn(),
    departureOn = this.departureOn(),
  )
}
