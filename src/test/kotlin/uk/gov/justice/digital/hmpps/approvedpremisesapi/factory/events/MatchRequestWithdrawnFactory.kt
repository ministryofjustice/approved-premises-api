package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.MatchRequestWithdrawn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.WithdrawnBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class MatchRequestWithdrawnFactory : Factory<MatchRequestWithdrawn> {
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var matchRequestId: Yielded<UUID> = { UUID.randomUUID() }
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var deliusEventNumber: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var submittedAt: Yielded<Instant> = { Instant.now().randomDateTimeBefore(7) }
  private var withdrawnByStaffMember: Yielded<StaffMember> = { StaffMemberFactory().produce() }
  private var withdrawnByProbationArea: Yielded<ProbationArea> = { ProbationAreaFactory().produce() }
  private var withdrawnAt: Yielded<Instant> = { Instant.now().randomDateTimeBefore(5) }
  private var withdrawalReason: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var datePeriod: Yielded<DatePeriod> = { DatePeriod(LocalDate.now(), LocalDate.now()) }

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

  fun withSubmittedAt(submittedAt: Instant) = apply {
    this.submittedAt = { submittedAt }
  }

  fun withWithdrawnByStaffMember(staffMember: StaffMember) = apply {
    this.withdrawnByStaffMember = { staffMember }
  }

  fun withWithdrawnByProbationArea(probationArea: ProbationArea) = apply {
    this.withdrawnByProbationArea = { probationArea }
  }

  fun withWithdrawnAt(withdrawnAt: Instant) = apply {
    this.withdrawnAt = { withdrawnAt }
  }

  fun withWithdrawalReason(withdrawalReason: String) = apply {
    this.withdrawalReason = { withdrawalReason }
  }

  fun withDatePeriod(datePeriod: DatePeriod) = apply {
    this.datePeriod = { datePeriod }
  }

  override fun produce() = MatchRequestWithdrawn(
    applicationId = this.applicationId(),
    applicationUrl = this.applicationUrl(),
    matchRequestId = this.matchRequestId(),
    personReference = this.personReference(),
    deliusEventNumber = this.deliusEventNumber(),
    withdrawnBy = WithdrawnBy(
      staffMember = this.withdrawnByStaffMember(),
      probationArea = this.withdrawnByProbationArea(),
    ),
    withdrawnAt = this.withdrawnAt(),
    withdrawalReason = this.withdrawalReason(),
    datePeriod = this.datePeriod(),
  )
}
