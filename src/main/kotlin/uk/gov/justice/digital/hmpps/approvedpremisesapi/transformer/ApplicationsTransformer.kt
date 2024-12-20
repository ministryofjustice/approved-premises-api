package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OfflineApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.asApiType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1ApplicationUserDetailsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1CruManagementAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSummary as ApiApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationStatus as ApiApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationSummary as ApiApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplicationSummary as ApiTemporaryAccommodationApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSummary as DomainApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationSummary as DomainApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity as DomainTemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationSummary as DomainTemporaryAccommodationApplicationSummary

@Component
class ApplicationsTransformer(
  private val objectMapper: ObjectMapper,
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer,
  private val apAreaTransformer: ApAreaTransformer,
  private val cas1ApplicationUserDetailsTransformer: Cas1ApplicationUserDetailsTransformer,
  private val cas1CruManagementAreaTransformer: Cas1CruManagementAreaTransformer,
) {
  @SuppressWarnings("TooGenericExceptionThrown")
  fun transformJpaToApi(applicationEntity: ApplicationEntity, personInfo: PersonInfoResult): Application {
    val latestAssessment = applicationEntity.getLatestAssessment()

    return when (applicationEntity) {
      is ApprovedPremisesApplicationEntity -> ApprovedPremisesApplication(
        id = applicationEntity.id,
        person = personTransformer.transformModelToPersonApi(personInfo),
        createdByUserId = applicationEntity.createdByUser.id,
        schemaVersion = applicationEntity.schemaVersion.id,
        outdatedSchema = !applicationEntity.schemaUpToDate,
        createdAt = applicationEntity.createdAt.toInstant(),
        submittedAt = applicationEntity.submittedAt?.toInstant(),
        isWomensApplication = applicationEntity.isWomensApplication,
        isPipeApplication = applicationEntity.isPipeApplication,
        arrivalDate = applicationEntity.arrivalDate?.toInstant(),
        data = if (applicationEntity.data != null) objectMapper.readTree(applicationEntity.data) else null,
        document = if (applicationEntity.document != null) objectMapper.readTree(applicationEntity.document) else null,
        risks = if (applicationEntity.riskRatings != null) {
          risksTransformer.transformDomainToApi(
            applicationEntity.riskRatings!!,
            applicationEntity.crn,
          )
        } else {
          null
        },
        status = applicationEntity.status.apiValue,
        assessmentDecision = transformJpaDecisionToApi(latestAssessment?.decision),
        assessmentId = latestAssessment?.id,
        assessmentDecisionDate = latestAssessment?.submittedAt?.toLocalDate(),
        personStatusOnSubmission = personTransformer.inmateStatusToPersonInfoApiStatus(
          InmateStatus.entries.firstOrNull { it.name == applicationEntity.inmateInOutStatusOnSubmission },
        ),
        type = "CAS1",
        apArea = applicationEntity.apArea?.let { apAreaTransformer.transformJpaToApi(it) },
        cruManagementArea = applicationEntity.cruManagementArea?. let { cas1CruManagementAreaTransformer.transformJpaToApi(it) },
        applicantUserDetails = applicationEntity.applicantUserDetails?.let { cas1ApplicationUserDetailsTransformer.transformJpaToApi(it) },
        caseManagerIsNotApplicant = applicationEntity.caseManagerIsNotApplicant,
        caseManagerUserDetails = applicationEntity.caseManagerUserDetails?.let { cas1ApplicationUserDetailsTransformer.transformJpaToApi(it) },
        apType = applicationEntity.apType.asApiType(),
        licenceExpiryDate = applicationEntity.licenceExpiryDate,
      )

      is DomainTemporaryAccommodationApplicationEntity -> TemporaryAccommodationApplication(
        id = applicationEntity.id,
        person = personTransformer.transformModelToPersonApi(personInfo),
        createdByUserId = applicationEntity.createdByUser.id,
        schemaVersion = applicationEntity.schemaVersion.id,
        outdatedSchema = !applicationEntity.schemaUpToDate,
        createdAt = applicationEntity.createdAt.toInstant(),
        submittedAt = applicationEntity.submittedAt?.toInstant(),
        arrivalDate = applicationEntity.arrivalDate?.toInstant(),
        data = if (applicationEntity.data != null) objectMapper.readTree(applicationEntity.data) else null,
        document = if (applicationEntity.document != null) objectMapper.readTree(applicationEntity.document) else null,
        risks = if (applicationEntity.riskRatings != null) {
          risksTransformer.transformDomainToApi(
            applicationEntity.riskRatings!!,
            applicationEntity.crn,
          )
        } else {
          null
        },
        status = getStatus(applicationEntity, latestAssessment),
        type = "CAS3",
        offenceId = applicationEntity.offenceId,
        assessmentId = latestAssessment?.id,
      )

      else -> throw RuntimeException("Unrecognised application type when transforming: ${applicationEntity::class.qualifiedName}")
    }
  }

  fun transformDomainToApiSummary(
    domain: DomainApplicationSummary,
    personInfo: PersonInfoResult,
  ): ApiApplicationSummary = when (domain) {
    is DomainApprovedPremisesApplicationSummary -> {
      val riskRatings =
        if (domain.getRiskRatings() != null) objectMapper.readValue<PersonRisks>(domain.getRiskRatings()!!) else null

      ApiApprovedPremisesApplicationSummary(
        id = domain.getId(),
        person = personTransformer.transformModelToPersonApi(personInfo),
        createdByUserId = domain.getCreatedByUserId(),
        createdAt = domain.getCreatedAt(),
        submittedAt = domain.getSubmittedAt(),
        isWomensApplication = domain.getIsWomensApplication(),
        isPipeApplication = domain.getIsPipeApplication(),
        arrivalDate = domain.getArrivalDate(),
        risks = if (riskRatings != null) risksTransformer.transformDomainToApi(riskRatings, domain.getCrn()) else null,
        status = getStatusFromSummary(domain),
        type = "CAS1",
        tier = domain.getTier(),
        isWithdrawn = domain.getIsWithdrawn(),
        releaseType = domain.getReleaseType()?.let { ReleaseTypeOption.valueOf(it) },
        hasRequestsForPlacement = domain.getHasRequestsForPlacement(),
      )
    }

    is DomainTemporaryAccommodationApplicationSummary -> {
      val riskRatings =
        if (domain.getRiskRatings() != null) objectMapper.readValue<PersonRisks>(domain.getRiskRatings()!!) else null

      ApiTemporaryAccommodationApplicationSummary(
        id = domain.getId(),
        person = personTransformer.transformModelToPersonApi(personInfo),
        createdByUserId = domain.getCreatedByUserId(),
        createdAt = domain.getCreatedAt(),
        submittedAt = domain.getSubmittedAt(),
        risks = if (riskRatings != null) risksTransformer.transformDomainToApi(riskRatings, domain.getCrn()) else null,
        status = getStatusFromSummary(domain),
        type = "CAS3",
      )
    }

    else -> throw RuntimeException("Unrecognised application type when transforming: ${domain::class.qualifiedName}")
  }

  fun transformJpaToApi(jpa: OfflineApplicationEntity, personInfo: PersonInfoResult) = OfflineApplication(
    id = jpa.id,
    person = personTransformer.transformModelToPersonApi(personInfo),
    createdAt = jpa.createdAt.toInstant(),
    type = "Offline",
  )

  private fun getStatus(entity: ApplicationEntity, latestAssessment: AssessmentEntity?): ApplicationStatus {
    return when {
      latestAssessment?.clarificationNotes?.any { it.response == null } == true -> ApplicationStatus.requestedFurtherInformation
      entity.submittedAt !== null -> ApplicationStatus.submitted
      else -> ApplicationStatus.inProgress
    }
  }

  private fun getStatusFromSummary(entity: DomainTemporaryAccommodationApplicationSummary): ApplicationStatus {
    return when {
      entity.getLatestAssessmentHasClarificationNotesWithoutResponse() -> ApplicationStatus.requestedFurtherInformation
      entity.getSubmittedAt() !== null -> ApplicationStatus.submitted
      else -> ApplicationStatus.inProgress
    }
  }

  private fun getStatusFromSummary(entity: DomainApprovedPremisesApplicationSummary): ApiApprovedPremisesApplicationStatus =
    ApprovedPremisesApplicationStatus.valueOf(entity.getStatus()).apiValue

  fun transformJpaDecisionToApi(decision: AssessmentDecision?) = when (decision) {
    AssessmentDecision.ACCEPTED -> uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision.accepted
    AssessmentDecision.REJECTED -> uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision.rejected
    null -> null
  }
}
