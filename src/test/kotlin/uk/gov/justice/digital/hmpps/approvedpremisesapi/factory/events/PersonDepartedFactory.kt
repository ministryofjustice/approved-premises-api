package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonDeparted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonDepartedDestination
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.util.UUID

class PersonDepartedFactory : Factory<PersonDeparted> {
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var deliusEventNumber: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var bookingId: Yielded<UUID> = { UUID.randomUUID() }
  private var premises: Yielded<Premises> = { EventPremisesFactory().produce() }
  private var keyWorker: Yielded<StaffMember> = { StaffMemberFactory().produce() }
  private var recordedBy: Yielded<StaffMember> = { StaffMemberFactory().produce() }
  private var departedAt: Yielded<Instant> = { Instant.now().randomDateTimeBefore(5) }
  private var reason: Yielded<String> = { randomOf(listOf("Bed Withdrawn", "Died", "Other", "Planned move-on")) }
  private var legacyReasonCode: Yielded<String> = { randomOf(listOf("A", "B", "C", "D")) }
  private var personDepartedDestination: Yielded<PersonDepartedDestination> = { PersonDepartedDestinationFactory().produce() }

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

  fun withPremises(premises: Premises) = apply {
    this.premises = { premises }
  }

  fun withDepartedAt(departedAt: Instant) = apply {
    this.departedAt = { departedAt }
  }
  fun withReason(reason: String) = apply {
    this.reason = { reason }
  }
  fun withLegacyReasonCode(legacyReasonCode: String) = apply {
    this.legacyReasonCode = { legacyReasonCode }
  }
  fun withPersonDepartedDestination(personDepartedDestination: PersonDepartedDestination) = apply {
    this.personDepartedDestination = { personDepartedDestination }
  }

  override fun produce() = PersonDeparted(
    applicationId = this.applicationId(),
    applicationUrl = this.applicationUrl(),
    personReference = this.personReference(),
    deliusEventNumber = this.deliusEventNumber(),
    bookingId = this.bookingId(),
    premises = this.premises(),
    keyWorker = this.keyWorker(),
    recordedBy = this.recordedBy(),
    departedAt = this.departedAt(),
    reason = this.reason(),
    legacyReasonCode = this.legacyReasonCode(),
    destination = this.personDepartedDestination(),
  )
}
