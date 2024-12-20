package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingKeyWorkerAssigned
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

class BookingKeyWorkerAssignedFactory : Factory<BookingKeyWorkerAssigned> {
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var keyWorker: Yielded<StaffMember> = { StaffMemberFactory().produce() }
  private var bookingId: Yielded<UUID> = { UUID.randomUUID() }
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var deliusEventNumber: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var premises: Yielded<Premises> = { EventPremisesFactory().produce() }
  private var arrivalDate: Yielded<LocalDate> = { LocalDate.now() }
  private var departureDate: Yielded<LocalDate> = { LocalDate.now() }
  private var assignedKeyWorkerName: Yielded<String> = { "assigned name" }
  private var previousKeyWorkerName: Yielded<String>? = null

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

  fun withKeyWorker(keyWorker: StaffMember) = apply {
    this.keyWorker = { keyWorker }
  }

  fun withPremises(premises: Premises) = apply {
    this.premises = { premises }
  }

  fun withArrivalDate(arrivalDate: LocalDate) = apply {
    this.arrivalDate = { arrivalDate }
  }

  fun withDepartureDate(departureDate: LocalDate) = apply {
    this.departureDate = { departureDate }
  }

  fun withAssignedKeyWorkerName(assignedKeyWorkerName: String) = apply {
    this.assignedKeyWorkerName = { assignedKeyWorkerName }
  }

  fun withPreviousKeyWorkerName(previousKeyWorkerName: String) = apply {
    this.previousKeyWorkerName = { previousKeyWorkerName }
  }

  override fun produce() = BookingKeyWorkerAssigned(
    applicationId = this.applicationId(),
    applicationUrl = this.applicationUrl(),
    personReference = this.personReference(),
    deliusEventNumber = this.deliusEventNumber(),
    bookingId = this.bookingId(),
    keyWorker = this.keyWorker(),
    premises = this.premises(),
    arrivalDate = this.arrivalDate(),
    departureDate = this.departureDate(),
    assignedKeyWorkerName = this.assignedKeyWorkerName(),
    previousKeyWorkerName = this.previousKeyWorkerName?.invoke(),
  )
}
