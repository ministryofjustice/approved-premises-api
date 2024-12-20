package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ApprovedPremisesApplicationEntityFactory : Factory<ApprovedPremisesApplicationEntity> {
  companion object {
    val DEFAULT = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(UserEntityFactory.DEFAULT)
      .produce()
  }

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
  private var isWomensApplication: Yielded<Boolean?> = { null }
  private var isEmergencyApplication: Yielded<Boolean?> = { null }
  private var apType: Yielded<ApprovedPremisesType> = { ApprovedPremisesType.NORMAL }
  private var convictionId: Yielded<Long> = { randomInt(0, 1000).toLong() }
  private var eventNumber: Yielded<String> = { randomInt(1, 9).toString() }
  private var offenceId: Yielded<String> = { randomStringMultiCaseWithNumbers(5) }
  private var riskRatings: Yielded<PersonRisks> = { PersonRisksFactory().produce() }
  private var assessments: Yielded<MutableList<AssessmentEntity>> = { mutableListOf() }
  private var teamCodes: Yielded<MutableList<ApplicationTeamCodeEntity>> = { mutableListOf() }
  private var placementRequests: Yielded<MutableList<PlacementRequestEntity>> = { mutableListOf() }
  private var releaseType: Yielded<String?> = { null }
  private var sentenceType: Yielded<String?> = { null }
  private var situation: Yielded<String?> = { null }
  private var arrivalDate: Yielded<OffsetDateTime?> = { null }
  private var isInapplicable: Yielded<Boolean?> = { null }
  private var isWithdrawn: Yielded<Boolean> = { false }
  private var withdrawalReason: Yielded<String?> = { null }
  private var otherWithdrawalReason: Yielded<String?> = { null }
  private var nomsNumber: Yielded<String?> = { randomStringUpperCase(6) }
  private var name: Yielded<String> = { "${randomStringUpperCase(4)} ${randomStringUpperCase(6)}" }
  private var targetLocation: Yielded<String?> = { null }
  private var status: Yielded<ApprovedPremisesApplicationStatus> = { ApprovedPremisesApplicationStatus.STARTED }
  private var inmateInOutStatusOnSubmission: Yielded<String?> = { null }
  private var apArea: Yielded<ApAreaEntity?> = { null }
  private var cruManagementArea: Yielded<Cas1CruManagementAreaEntity?> = { null }
  private var applicantUserDetails: Yielded<Cas1ApplicationUserDetailsEntity?> = { null }
  private var caseManagerIsNotApplicant: Yielded<Boolean?> = { null }
  private var caseManagerUserDetails: Yielded<Cas1ApplicationUserDetailsEntity?> = { null }
  private var noticeType: Yielded<Cas1ApplicationTimelinessCategory?> = { null }
  private var licenseExpiryDate: Yielded<LocalDate?> = { null }

  fun withDefaults() = apply {
    withCreatedByUser(UserEntityFactory().withDefaults().produce())
  }

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

  fun withIsWomensApplication(isWomensApplication: Boolean?) = apply {
    this.isWomensApplication = { isWomensApplication }
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

  fun withOtherWithdrawalReason(otherWithdrawalReason: String) = apply {
    this.otherWithdrawalReason = { otherWithdrawalReason }
  }

  fun withNomsNumber(nomsNumber: String?) = apply {
    this.nomsNumber = { nomsNumber }
  }

  fun withIsEmergencyApplication(isEmergencyApplication: Boolean?) = apply {
    this.isEmergencyApplication = { isEmergencyApplication }
  }

  fun withApType(apType: ApprovedPremisesType) = apply {
    this.apType = { apType }
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

  fun withCruManagementArea(cruManagementArea: Cas1CruManagementAreaEntity?) = apply {
    this.cruManagementArea = { cruManagementArea }
  }

  fun withApplicantUserDetails(applicantUserDetails: Cas1ApplicationUserDetailsEntity?) = apply {
    this.applicantUserDetails = { applicantUserDetails }
  }

  fun withCaseManagerIsNotApplicant(caseManagerIsNotApplicant: Boolean?) = apply {
    this.caseManagerIsNotApplicant = { caseManagerIsNotApplicant }
  }

  fun withCaseManagerUserDetails(caseManagerUserDetails: Cas1ApplicationUserDetailsEntity?) = apply {
    this.caseManagerUserDetails = { caseManagerUserDetails }
  }

  fun withNoticeType(noticeType: Cas1ApplicationTimelinessCategory?) = apply {
    this.noticeType = { noticeType }
  }

  fun withLicenseExpiredDate(licenseExpiredDate: LocalDate?) = apply {
    this.licenseExpiryDate = { licenseExpiredDate }
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
    deletedAt = this.deletedAt(),
    isWomensApplication = this.isWomensApplication(),
    isEmergencyApplication = this.isEmergencyApplication(),
    apType = this.apType(),
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
    otherWithdrawalReason = otherWithdrawalReason(),
    nomsNumber = this.nomsNumber(),
    name = this.name(),
    targetLocation = this.targetLocation(),
    status = this.status(),
    situation = this.situation(),
    inmateInOutStatusOnSubmission = this.inmateInOutStatusOnSubmission(),
    apArea = this.apArea(),
    cruManagementArea = this.cruManagementArea(),
    applicantUserDetails = this.applicantUserDetails(),
    caseManagerIsNotApplicant = this.caseManagerIsNotApplicant(),
    caseManagerUserDetails = this.caseManagerUserDetails(),
    noticeType = this.noticeType(),
    licenceExpiryDate = this.licenseExpiryDate(),
  )
}
