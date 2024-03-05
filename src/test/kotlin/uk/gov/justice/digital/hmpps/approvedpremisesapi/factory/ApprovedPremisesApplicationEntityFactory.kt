package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.OffsetDateTime
import java.util.UUID

class ApprovedPremisesApplicationEntityFactory : Factory<ApprovedPremisesApplicationEntity> {
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
  private var isWomensApplication: Yielded<Boolean?> = { null }
  private var isPipeApplication: Yielded<Boolean?> = { null }
  private var isEmergencyApplication: Yielded<Boolean?> = { null }
  private var isEsapApplication: Yielded<Boolean?> = { null }
  private var convictionId: Yielded<Long> = { randomInt(0, 1000).toLong() }
  private var eventNumber: Yielded<String> = { randomInt(1, 9).toString() }
  private var offenceId: Yielded<String> = { randomStringMultiCaseWithNumbers(5) }
  private var riskRatings: Yielded<PersonRisks> = { PersonRisksFactory().produce() }
  private var assessments: Yielded<MutableList<AssessmentEntity>> = { mutableListOf<AssessmentEntity>() }
  private var teamCodes: Yielded<MutableList<ApplicationTeamCodeEntity>> = { mutableListOf() }
  private var placementRequests: Yielded<MutableList<PlacementRequestEntity>> = { mutableListOf() }
  private var releaseType: Yielded<String?> = { null }
  private var sentenceType: Yielded<String?> = { null }
  private var situation: Yielded<String?> = { null }
  private var arrivalDate: Yielded<OffsetDateTime?> = { null }
  private var isInapplicable: Yielded<Boolean?> = { null }
  private var isWithdrawn: Yielded<Boolean> = { false }
  private var withdrawalReason: Yielded<String?> = { null }
  private var nomsNumber: Yielded<String?> = { randomStringUpperCase(6) }
  private var name: Yielded<String> = { "${randomStringUpperCase(4)} ${randomStringUpperCase(6)}" }
  private var targetLocation: Yielded<String?> = { null }
  private var status: Yielded<ApprovedPremisesApplicationStatus> = { ApprovedPremisesApplicationStatus.STARTED }
  private var inmateInOutStatusOnSubmission: Yielded<String?> = { null }
  private var apArea: Yielded<ApAreaEntity?> = { null }

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

  fun withIsWomensApplication(isWomensApplication: Boolean) = apply {
    this.isWomensApplication = { isWomensApplication }
  }

  fun withIsPipeApplication(isPipeApplication: Boolean) = apply {
    this.isPipeApplication = { isPipeApplication }
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

  fun withRiskRatings(riskRatings: PersonRisks) = apply {
    this.riskRatings = { riskRatings }
  }

  fun withAssessments(assessments: MutableList<AssessmentEntity>) = apply {
    this.assessments = { assessments }
  }

  fun withTeamCodes(teamCodes: MutableList<ApplicationTeamCodeEntity>) = apply {
    this.teamCodes = { teamCodes }
  }

  fun withPlacementRequests(placementRequests: MutableList<PlacementRequestEntity>) = apply {
    this.placementRequests = { placementRequests }
  }

  fun withReleaseType(releaseType: String) = apply {
    this.releaseType = { releaseType }
  }

  fun withSentenceType(sentenceType: String) = apply {
    this.sentenceType = { sentenceType }
  }

  fun withSituation(situation: String) = apply {
    this.situation = { situation }
  }

  fun withArrivalDate(arrivalDate: OffsetDateTime?) = apply {
    this.arrivalDate = { arrivalDate }
  }

  fun withIsInapplicable(isInapplicable: Boolean) = apply {
    this.isInapplicable = { isInapplicable }
  }

  fun withIsWithdrawn(isWithdrawn: Boolean) = apply {
    this.isWithdrawn = { isWithdrawn }
  }

  fun withWithdrawalReason(withdrawalReason: String) = apply {
    this.withdrawalReason = { withdrawalReason }
  }

  fun withNomsNumber(nomsNumber: String?) = apply {
    this.nomsNumber = { nomsNumber }
  }

  fun withIsEmergencyApplication(isEmergencyApplication: Boolean?) = apply {
    this.isEmergencyApplication = { isEmergencyApplication }
  }

  fun withIsEsapApplication(isEsapApplication: Boolean?) = apply {
    this.isEsapApplication = { isEsapApplication }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withTargetLocation(targetLocation: String?) = apply {
    this.targetLocation = { targetLocation }
  }

  fun withStatus(status: ApprovedPremisesApplicationStatus) = apply {
    this.status = { status }
  }

  fun withInmateInOutStatusOnSubmission(inmateInOutStatusOnSubmission: String?) = apply {
    this.inmateInOutStatusOnSubmission = { inmateInOutStatusOnSubmission }
  }

  fun withApArea(apArea: ApAreaEntity?) = apply {
    this.apArea = { apArea }
  }

  override fun produce(): ApprovedPremisesApplicationEntity = ApprovedPremisesApplicationEntity(
    id = this.id(),
    crn = this.crn(),
    createdByUser = this.createdByUser?.invoke() ?: throw RuntimeException("Must provide a createdByUser"),
    data = this.data(),
    document = this.document(),
    schemaVersion = this.applicationSchema(),
    createdAt = this.createdAt(),
    submittedAt = this.submittedAt(),
    isWomensApplication = this.isWomensApplication(),
    isPipeApplication = this.isPipeApplication(),
    isEmergencyApplication = this.isEmergencyApplication(),
    isEsapApplication = this.isEsapApplication(),
    convictionId = this.convictionId(),
    eventNumber = this.eventNumber(),
    offenceId = this.offenceId(),
    schemaUpToDate = false,
    riskRatings = this.riskRatings(),
    assessments = this.assessments(),
    teamCodes = this.teamCodes(),
    placementRequests = this.placementRequests(),
    releaseType = this.releaseType(),
    sentenceType = this.sentenceType(),
    arrivalDate = this.arrivalDate(),
    isInapplicable = this.isInapplicable(),
    isWithdrawn = this.isWithdrawn(),
    withdrawalReason = this.withdrawalReason(),
    otherWithdrawalReason = null,
    nomsNumber = this.nomsNumber(),
    name = this.name(),
    targetLocation = this.targetLocation(),
    status = this.status(),
    situation = this.situation(),
    inmateInOutStatusOnSubmission = this.inmateInOutStatusOnSubmission(),
    apArea = this.apArea(),
    applicantUserDetails = null,
    caseManagerNotApplicant = null,
    casManagerUserDetails = null,
  )
}
