package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmitted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmittedSubmittedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Ldu
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Region
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Team
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TeamFactoryDeliusContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.util.UUID

class ApplicationSubmittedFactory : Factory<ApplicationSubmitted> {
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var deliusEventNumber: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var mappa: Yielded<String> = { "CAT C3/LEVEL L2" }
  private var offenceId: Yielded<String> = { randomStringMultiCaseWithNumbers(5) }
  private var releaseType: Yielded<String> = { "rotl" }
  private var age: Yielded<Int> = { randomInt(18, 99) }
  private var gender: Yielded<ApplicationSubmitted.Gender> = { randomOf(listOf(ApplicationSubmitted.Gender.male, ApplicationSubmitted.Gender.female)) }
  private var targetLocation: Yielded<String> = { randomOf(listOf("AB", "KY", "L")) }
  private var submittedAt: Yielded<Instant> = { Instant.now().randomDateTimeBefore(7) }
  private var submittedByStaffMember: Yielded<StaffMember> = { StaffMemberFactory().produce() }
  private var submittedByProbationArea: Yielded<ProbationArea> = { ProbationAreaFactory().produce() }
  private var submittedByTeam: Yielded<Team> = { TeamFactoryDeliusContext.team().toDomainEventTeam() }
  private var submittedByLdu: Yielded<Ldu> = { LduFactory().produce() }
  private var submittedByRegion: Yielded<Region> = { RegionFactory().produce() }
  private var sentenceLengthInMonths: Yielded<Int?> = { null }

  private fun uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Team.toDomainEventTeam() =
    Team(this.code, this.name)

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

  fun withMappa(mappa: String) = apply {
    this.mappa = { mappa }
  }

  fun withOffenceId(offenceId: String) = apply {
    this.offenceId = { offenceId }
  }

  fun withReleaseType(releaseType: String) = apply {
    this.releaseType = { releaseType }
  }

  fun withAge(age: Int) = apply {
    this.age = { age }
  }

  fun withGender(gender: ApplicationSubmitted.Gender) = apply {
    this.gender = { gender }
  }

  fun withTargetLocation(targetLocation: String) = apply {
    this.targetLocation = { targetLocation }
  }

  fun withSubmittedAt(submittedAt: Instant) = apply {
    this.submittedAt = { submittedAt }
  }

  fun withSubmittedByStaffMember(staffMember: StaffMember) = apply {
    this.submittedByStaffMember = { staffMember }
  }

  fun withSubmittedByProbationArea(probationArea: ProbationArea) = apply {
    this.submittedByProbationArea = { probationArea }
  }

  fun withSubmittedByTeam(team: Team) = apply {
    this.submittedByTeam = { team }
  }

  fun withSubmittedByLdu(ldu: Ldu) = apply {
    this.submittedByLdu = { ldu }
  }

  fun withSubmittedByRegion(region: Region) = apply {
    this.submittedByRegion = { region }
  }

  fun withSentenceLengthInMonths(sentenceLengthInMonths: Int?) = apply {
    this.sentenceLengthInMonths = { sentenceLengthInMonths }
  }

  override fun produce() = ApplicationSubmitted(
    applicationId = this.applicationId(),
    applicationUrl = this.applicationUrl(),
    personReference = this.personReference(),
    deliusEventNumber = this.deliusEventNumber(),
    mappa = this.mappa(),
    offenceId = this.offenceId(),
    releaseType = this.releaseType(),
    age = this.age(),
    gender = this.gender(),
    targetLocation = this.targetLocation(),
    submittedAt = this.submittedAt(),
    submittedBy = ApplicationSubmittedSubmittedBy(
      staffMember = this.submittedByStaffMember(),
      probationArea = this.submittedByProbationArea(),
      team = this.submittedByTeam(),
      ldu = this.submittedByLdu(),
      region = this.submittedByRegion(),
    ),
    sentenceLengthInMonths = this.sentenceLengthInMonths(),
  )
}
