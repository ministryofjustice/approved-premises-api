package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class TemporaryAccommodationApplicationEntityFactory : Factory<TemporaryAccommodationApplicationEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var crn: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var createdByUser: Yielded<UserEntity>? = null
  private var data: Yielded<String?> = { "{}" }
  private var document: Yielded<String?> = { "{}" }
  private var applicationSchema: Yielded<JsonSchemaEntity> = {
    ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()
  }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(30) }
  private var submittedAt: Yielded<OffsetDateTime?> = { null }
  private var deletedAt: Yielded<OffsetDateTime?> = { null }
  private var assessments: Yielded<MutableList<AssessmentEntity>> = { mutableListOf() }
  private var convictionId: Yielded<Long> = { randomInt(0, 1000).toLong() }
  private var eventNumber: Yielded<String> = { randomInt(1, 9).toString() }
  private var offenceId: Yielded<String> = { randomStringMultiCaseWithNumbers(5) }
  private var riskRatings: Yielded<PersonRisks> = { PersonRisksFactory().produce() }
  private var probationRegion: Yielded<ProbationRegionEntity>? = null
  private var nomsNumber: Yielded<String> = { randomStringUpperCase(6) }
  private var arrivalDate: Yielded<OffsetDateTime?> = { null }
  private var hasRegisteredSexOffender: Yielded<Boolean?> = { null }
  private var needsAccessibleProperty: Yielded<Boolean?> = { null }
  private var hasHistoryOfArson: Yielded<Boolean?> = { null }
  private var isDutyToReferSubmitted: Yielded<Boolean?> = { null }
  private var dutyToReferSubmissionDate: Yielded<LocalDate?> = { null }
  private var isEligible: Yielded<Boolean?> = { null }
  private var eligibilityReason: Yielded<String?> = { null }
  private var dutyToReferLocalAuthorityAreaName: Yielded<String?> = { null }
  private var prisonNameAtReferral: Yielded<String?> = { null }
  private var personReleaseDate: Yielded<LocalDate?> = { null }
  private var pdu: Yielded<String?> = { null }
  private var name: Yielded<String?> = { null }
  private var hasHistoryOfSexualOffence: Yielded<Boolean?> = { null }
  private var isConcerningSexualBehaviour: Yielded<Boolean?> = { null }
  private var isConcerningArsonBehaviour: Yielded<Boolean?> = { null }
  private var dutyToReferOutcome: Yielded<String?> = { null }
  private var prisonReleaseTypes: Yielded<String?> = { null }
  private var probationDeliveryUnit: Yielded<ProbationDeliveryUnitEntity?> = { null }
  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withCrn(crn: String) = apply {
    this.crn = { crn }
  }

  fun withCreatedByUser(createdByUser: UserEntity) = apply {
    this.createdByUser = { createdByUser }
  }

  fun withYieldedCreatedByUser(createdByUser: Yielded<UserEntity>) = apply {
    this.createdByUser = createdByUser
  }

  fun withData(data: String?) = apply {
    this.data = { data }
  }

  fun withDocument(document: String?) = apply {
    this.document = { document }
  }

  fun withApplicationSchema(applicationSchema: JsonSchemaEntity) = apply {
    this.applicationSchema = { applicationSchema }
  }

  fun withYieldedApplicationSchema(applicationSchema: Yielded<JsonSchemaEntity>) = apply {
    this.applicationSchema = applicationSchema
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withSubmittedAt(submittedAt: OffsetDateTime?) = apply {
    this.submittedAt = { submittedAt }
  }

  fun withAssessments(assessments: MutableList<AssessmentEntity>) = apply {
    this.assessments = { assessments }
  }

  fun withConvictionId(convictionId: Long) = apply {
    this.convictionId = { convictionId }
  }

  fun withEventNumber(eventNumber: String) = apply {
    this.eventNumber = { eventNumber }
  }

  fun withOffenceId(offenceId: String) = apply {
    this.offenceId = { offenceId }
  }

  fun withProbationRegion(probationRegion: ProbationRegionEntity) = apply {
    this.probationRegion = { probationRegion }
  }

  fun withYieldedProbationRegion(probationRegion: Yielded<ProbationRegionEntity>) = apply {
    this.probationRegion = probationRegion
  }

  fun withNomsNumber(nomsNumber: String) = apply {
    this.nomsNumber = { nomsNumber }
  }

  fun withArrivalDate(arrivalDate: LocalDate?) = apply {
    this.arrivalDate = { arrivalDate?.let { OffsetDateTime.of(it, LocalTime.MIDNIGHT, ZoneOffset.UTC) } }
  }

  fun withArrivalDate(arrivalDate: OffsetDateTime) = apply {
    this.arrivalDate = { arrivalDate }
  }

  fun withHasRegisteredSexOffender(hasRegisteredSexOffender: Boolean?) = apply {
    this.hasRegisteredSexOffender = { hasRegisteredSexOffender }
  }

  fun withNeedsAccessibleProperty(needsAccessibleProperty: Boolean?) = apply {
    this.needsAccessibleProperty = { needsAccessibleProperty }
  }

  fun withHasHistoryOfArson(hasHistoryOfArson: Boolean?) = apply {
    this.hasHistoryOfArson = { hasHistoryOfArson }
  }

  fun withIsDutyToReferSubmitted(isDutyToReferSubmitted: Boolean?) = apply {
    this.isDutyToReferSubmitted = { isDutyToReferSubmitted }
  }

  fun withDutyToReferSubmissionDate(dutyToReferSubmissionDate: LocalDate?) = apply {
    this.dutyToReferSubmissionDate = { dutyToReferSubmissionDate }
  }

  fun withIsEligible(isEligible: Boolean?) = apply {
    this.isEligible = { isEligible }
  }

  fun withEligiblilityReason(eligibilityReason: String?) = apply {
    this.eligibilityReason = { eligibilityReason }
  }

  fun withRiskRatings(configuration: PersonRisksFactory.() -> Unit) = apply {
    this.riskRatings = { PersonRisksFactory().apply(configuration).produce() }
  }

  fun withRiskRatings(riskRatings: PersonRisks) = apply {
    this.riskRatings = { riskRatings }
  }

  fun withDutyToReferLocalAuthorityAreaName(dutyToReferLocalAuthorityAreaName: String?) = apply {
    this.dutyToReferLocalAuthorityAreaName = { dutyToReferLocalAuthorityAreaName }
  }

  fun withPrisonNameAtReferral(prisonNameAtReferral: String?) = apply {
    this.prisonNameAtReferral = { prisonNameAtReferral }
  }

  fun withPersonReleaseDate(personReleaseDate: LocalDate?) = apply {
    this.personReleaseDate = { personReleaseDate }
  }

  fun withPdu(pdu: String?) = apply {
    this.pdu = { pdu }
  }

  fun withName(name: String?) = apply {
    this.name = { name }
  }

  fun withHasHistoryOfSexualOffence(hasHistoryOfSexualOffence: Boolean?) = apply {
    this.hasHistoryOfSexualOffence = { hasHistoryOfSexualOffence }
  }

  fun withIsConcerningSexualBehaviour(isConcerningSexualBehaviour: Boolean?) = apply {
    this.isConcerningSexualBehaviour = { isConcerningSexualBehaviour }
  }

  fun withIsConcerningArsonBehaviour(isConcerningArsonBehaviour: Boolean?) = apply {
    this.isConcerningArsonBehaviour = { isConcerningArsonBehaviour }
  }

  fun withDutyToReferOutcome(dutyToReferOutcome: String?) = apply {
    this.dutyToReferOutcome = { dutyToReferOutcome }
  }

  fun withPrisonReleaseTypes(prisonReleaseTypes: String?) = apply {
    this.prisonReleaseTypes = { prisonReleaseTypes }
  }

  fun withProbationDeliveryUnit(probationDeliveryUnit: ProbationDeliveryUnitEntity?) = apply {
    this.probationDeliveryUnit = { probationDeliveryUnit }
  }

  override fun produce(): TemporaryAccommodationApplicationEntity = TemporaryAccommodationApplicationEntity(
    id = this.id(),
    crn = this.crn(),
    createdByUser = this.createdByUser?.invoke() ?: throw RuntimeException("Must provide a createdByUser"),
    data = this.data(),
    document = this.document(),
    schemaVersion = this.applicationSchema(),
    createdAt = this.createdAt(),
    submittedAt = this.submittedAt(),
    deletedAt = this.deletedAt(),
    schemaUpToDate = false,
    assessments = this.assessments(),
    convictionId = this.convictionId(),
    eventNumber = this.eventNumber(),
    offenceId = this.offenceId(),
    riskRatings = this.riskRatings(),
    probationRegion = this.probationRegion?.invoke() ?: throw RuntimeException("A probation region must be provided"),
    nomsNumber = this.nomsNumber(),
    arrivalDate = this.arrivalDate(),
    isRegisteredSexOffender = this.hasRegisteredSexOffender(),
    isHistoryOfSexualOffence = this.hasHistoryOfSexualOffence(),
    isConcerningSexualBehaviour = this.isConcerningSexualBehaviour(),
    needsAccessibleProperty = this.needsAccessibleProperty(),
    hasHistoryOfArson = this.hasHistoryOfArson(),
    isConcerningArsonBehaviour = this.isConcerningArsonBehaviour(),
    isDutyToReferSubmitted = this.isDutyToReferSubmitted(),
    dutyToReferSubmissionDate = this.dutyToReferSubmissionDate(),
    dutyToReferOutcome = this.dutyToReferOutcome(),
    isEligible = this.isEligible(),
    eligibilityReason = this.eligibilityReason(),
    dutyToReferLocalAuthorityAreaName = this.dutyToReferLocalAuthorityAreaName(),
    prisonNameOnCreation = this.prisonNameAtReferral(),
    personReleaseDate = this.personReleaseDate(),
    pdu = this.pdu(),
    name = this.name(),
    prisonReleaseTypes = this.prisonReleaseTypes(),
    probationDeliveryUnit = this.probationDeliveryUnit(),
  )

  fun withDefaults() = TemporaryAccommodationApplicationEntityFactory()
    .withCreatedByUser(UserEntityFactory.DEFAULT)
    .withProbationRegion(ProbationRegionEntityFactory().withDefaults().produce())
}
