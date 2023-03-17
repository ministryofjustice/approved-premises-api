package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmitted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedSubmittedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Ldu
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Region
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Team
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.Period
import java.util.UUID
import jakarta.transaction.Transactional

@Service
class ApplicationService(
  private val userRepository: UserRepository,
  private val applicationRepository: ApplicationRepository,
  private val jsonSchemaService: JsonSchemaService,
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val assessmentService: AssessmentService,
  private val offlineApplicationRepository: OfflineApplicationRepository,
  private val domainEventService: DomainEventService,
  private val communityApiClient: CommunityApiClient,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val applicationTeamCodeRepository: ApplicationTeamCodeRepository,
  private val objectMapper: ObjectMapper,
  @Value("\${application-url-template}") private val applicationUrlTemplate: String
) {
  fun getAllApplicationsForUsername(userDistinguishedName: String, serviceName: ServiceName): List<ApplicationEntity> {
    val userEntity = userRepository.findByDeliusUsername(userDistinguishedName)
      ?: return emptyList()

    val entityType = if (serviceName == ServiceName.approvedPremises) {
      ApprovedPremisesApplicationEntity::class.java
    } else {
      TemporaryAccommodationApplicationEntity::class.java
    }

    val userDetailsResult = communityApiClient.getStaffUserDetails(userEntity.deliusUsername)
    val userDetails = when (userDetailsResult) {
      is ClientResult.Success -> userDetailsResult.body
      is ClientResult.Failure -> userDetailsResult.throwException()
    }

    val applications = if (userEntity.hasAnyRole(UserRole.WORKFLOW_MANAGER, UserRole.ASSESSOR, UserRole.MATCHER, UserRole.MANAGER)) {
      applicationRepository.findAll()
    } else if (serviceName == ServiceName.approvedPremises) {
      applicationRepository.findAllByManagingTeam(userDetails.teams?.map { it.code } ?: emptyList(), entityType)
    } else {
      applicationRepository.findAllByCreatedByUser_Id(userEntity.id, entityType)
    }

    return applications
      .map(jsonSchemaService::checkSchemaOutdated)
      .filter {
        offenderService.canAccessOffender(userDistinguishedName, it.crn)
      }
  }

  fun getAllOfflineApplicationsForUsername(deliusUsername: String, serviceName: ServiceName): List<OfflineApplicationEntity> {
    val userEntity = userRepository.findByDeliusUsername(deliusUsername)
      ?: return emptyList()

    val applications = if (userEntity.hasAnyRole(UserRole.WORKFLOW_MANAGER, UserRole.ASSESSOR, UserRole.MATCHER, UserRole.MANAGER)) {
      offlineApplicationRepository.findAllByService(serviceName.value)
    } else {
      emptyList()
    }

    return applications
      .filter {
        offenderService.canAccessOffender(deliusUsername, it.crn)
      }
  }

  fun getApplicationForUsername(applicationId: UUID, userDistinguishedName: String): AuthorisableActionResult<ApplicationEntity> {
    val applicationEntity = applicationRepository.findByIdOrNull(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    val userEntity = userRepository.findByDeliusUsername(userDistinguishedName)
      ?: throw RuntimeException("Could not get user")

    if (userEntity.id == applicationEntity.createdByUser.id || userEntity.hasAnyRole(UserRole.WORKFLOW_MANAGER, UserRole.ASSESSOR, UserRole.MATCHER, UserRole.MANAGER)) {
      return AuthorisableActionResult.Success(jsonSchemaService.checkSchemaOutdated(applicationEntity))
    }

    if (applicationEntity is ApprovedPremisesApplicationEntity) {
      val userDetailsResult = communityApiClient.getStaffUserDetails(userEntity.deliusUsername)
      val userDetails = when (userDetailsResult) {
        is ClientResult.Success -> userDetailsResult.body
        is ClientResult.Failure -> userDetailsResult.throwException()
      }

      if (applicationEntity.hasAnyTeamCode(userDetails.teams?.map { it.code } ?: emptyList())) {
        return AuthorisableActionResult.Success(jsonSchemaService.checkSchemaOutdated(applicationEntity))
      }
    }

    return AuthorisableActionResult.Unauthorised()
  }

  fun getOfflineApplicationForUsername(applicationId: UUID, deliusUsername: String): AuthorisableActionResult<OfflineApplicationEntity> {
    val applicationEntity = offlineApplicationRepository.findByIdOrNull(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    val userEntity = userRepository.findByDeliusUsername(deliusUsername)
      ?: throw RuntimeException("Could not get user")

    if (userEntity.hasAnyRole(UserRole.WORKFLOW_MANAGER, UserRole.ASSESSOR, UserRole.MATCHER, UserRole.MANAGER) &&
      offenderService.canAccessOffender(deliusUsername, applicationEntity.crn)
    ) {
      return AuthorisableActionResult.Success(applicationEntity)
    }

    return AuthorisableActionResult.Unauthorised()
  }

  fun createApplication(crn: String, user: UserEntity, jwt: String, service: String, convictionId: Long?, deliusEventNumber: String?, offenceId: String?, createWithRisks: Boolean? = true) = validated<ApplicationEntity> {
    if (service != ServiceName.approvedPremises.value) {
      "$.service" hasValidationError "onlyCas1Supported"
      return fieldValidationError
    }

    when (offenderService.getOffenderByCrn(crn, user.deliusUsername)) {
      is AuthorisableActionResult.NotFound -> return "$.crn" hasSingleValidationError "doesNotExist"
      is AuthorisableActionResult.Unauthorised -> return "$.crn" hasSingleValidationError "userPermission"
      is AuthorisableActionResult.Success -> Unit
    }

    val managingTeamsResult = apDeliusContextApiClient.getTeamsManagingCase(crn, user.deliusStaffCode!!)

    val managingTeamCodes = when (managingTeamsResult) {
      is ClientResult.Success -> managingTeamsResult.body.teamCodes
      is ClientResult.Failure -> managingTeamsResult.throwException()
    }

    if (managingTeamCodes.isEmpty()) {
      return "$.crn" hasSingleValidationError "notInCaseload"
    }

    if (convictionId == null) {
      "$.convictionId" hasValidationError "empty"
    }

    if (deliusEventNumber == null) {
      "$.deliusEventNumber" hasValidationError "empty"
    }

    if (offenceId == null) {
      "$.offenceId" hasValidationError "empty"
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    var riskRatings: PersonRisks? = null

    if (createWithRisks == true) {
      val riskRatingsResult = offenderService.getRiskByCrn(crn, jwt, user.deliusUsername)

      riskRatings = when (riskRatingsResult) {
        is AuthorisableActionResult.NotFound -> return "$.crn" hasSingleValidationError "doesNotExist"
        is AuthorisableActionResult.Unauthorised -> return "$.crn" hasSingleValidationError "userPermission"
        is AuthorisableActionResult.Success -> riskRatingsResult.entity
      }
    }

    val createdApplication = applicationRepository.save(
      ApprovedPremisesApplicationEntity(
        id = UUID.randomUUID(),
        crn = crn,
        createdByUser = user,
        data = null,
        document = null,
        schemaVersion = jsonSchemaService.getNewestSchema(ApprovedPremisesApplicationJsonSchemaEntity::class.java),
        createdAt = OffsetDateTime.now(),
        submittedAt = null,
        isWomensApplication = null,
        isPipeApplication = null,
        convictionId = convictionId!!,
        eventNumber = deliusEventNumber!!,
        offenceId = offenceId!!,
        schemaUpToDate = true,
        riskRatings = riskRatings,
        assessments = mutableListOf(),
        teamCodes = mutableListOf()
      )
    )

    managingTeamCodes.forEach {
      createdApplication.teamCodes += applicationTeamCodeRepository.save(
        ApplicationTeamCodeEntity(
          id = UUID.randomUUID(),
          application = createdApplication,
          teamCode = it
        )
      )
    }

    return success(createdApplication.apply { schemaUpToDate = true })
  }

  fun updateApplication(applicationId: UUID, data: String, username: String): AuthorisableActionResult<ValidatableActionResult<ApplicationEntity>> {
    val application = applicationRepository.findByIdOrNull(applicationId)?.let(jsonSchemaService::checkSchemaOutdated)
      ?: return AuthorisableActionResult.NotFound()

    val user = userService.getUserForRequest()

    if (application.createdByUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (!application.schemaUpToDate) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The schema version is outdated")
      )
    }

    if (application.submittedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has already been submitted")
      )
    }

    application.data = data

    val savedApplication = applicationRepository.save(application)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedApplication)
    )
  }

  @Transactional
  fun submitApplication(applicationId: UUID, submitApplication: SubmitApplication, username: String, jwt: String): AuthorisableActionResult<ValidatableActionResult<ApplicationEntity>> {
    var application = applicationRepository.findByIdOrNull(applicationId)?.let(jsonSchemaService::checkSchemaOutdated)
      ?: return AuthorisableActionResult.NotFound()

    val serializedTranslatedDocument = objectMapper.writeValueAsString(submitApplication.translatedDocument)

    val user = userService.getUserForRequest()

    if (application.createdByUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (application !is ApprovedPremisesApplicationEntity) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("onlyCas1Supported")
      )
    }

    if (application.submittedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has already been submitted")
      )
    }

    if (!application.schemaUpToDate) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The schema version is outdated")
      )
    }

    val validationErrors = ValidationErrors()
    val applicationData = application.data

    if (applicationData == null) {
      validationErrors["$.data"] = "empty"
    } else if (!jsonSchemaService.validate(application.schemaVersion, applicationData)) {
      validationErrors["$.data"] = "invalid"
    }

    if (validationErrors.any()) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.FieldValidationError(validationErrors)
      )
    }

    val schema = application.schemaVersion as? ApprovedPremisesApplicationJsonSchemaEntity
      ?: throw RuntimeException("Incorrect type of JSON schema referenced by AP Application")

    application.apply {
      isWomensApplication = submitApplication.isWomensApplication
      isPipeApplication = submitApplication.isPipeApplication
      submittedAt = OffsetDateTime.now()
      document = serializedTranslatedDocument
    }

    assessmentService.createAssessment(application)

    application = applicationRepository.save(application)

    createApplicationSubmittedEvent(application, submitApplication, username, jwt)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(application)
    )
  }

  private fun createApplicationSubmittedEvent(application: ApprovedPremisesApplicationEntity, submitApplication: SubmitApplication, username: String, jwt: String) {
    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = OffsetDateTime.now()

    val offenderDetails = when (val offenderDetailsResult = offenderService.getOffenderByCrn(application.crn, username)) {
      is AuthorisableActionResult.Success -> offenderDetailsResult.entity
      is AuthorisableActionResult.Unauthorised -> throw RuntimeException("Unable to get Offender Details when creating Application Submitted Domain Event: Unauthorised")
      is AuthorisableActionResult.NotFound -> throw RuntimeException("Unable to get Offender Details when creating Application Submitted Domain Event: Not Found")
    }

    val risks = when (val riskResult = offenderService.getRiskByCrn(application.crn, jwt, username)) {
      is AuthorisableActionResult.Success -> riskResult.entity
      is AuthorisableActionResult.Unauthorised -> throw RuntimeException("Unable to get Risks when creating Application Submitted Domain Event: Unauthorised")
      is AuthorisableActionResult.NotFound -> throw RuntimeException("Unable to get Risks when creating Application Submitted Domain Event: Not Found")
    }

    val mappaLevel = risks.mappa.value?.level

    val staffDetailsResult = communityApiClient.getStaffUserDetails(username)
    val staffDetails = when (staffDetailsResult) {
      is ClientResult.Success -> staffDetailsResult.body
      is ClientResult.Failure -> staffDetailsResult.throwException()
    }

    // TODO: The integrations team have agreed to build us an endpoint to determine which team's caseload an Offender belongs to
    //       once that is done we should use that here to select the right team when the user is a memnber of more than one

    val team = staffDetails.teams?.firstOrNull()
      ?: throw RuntimeException("No teams present on Staff Details when creating Application Submitted Domain Event")

    domainEventService.saveApplicationSubmittedDomainEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        occurredAt = eventOccurredAt,
        data = ApplicationSubmittedEnvelope(
          id = domainEventId,
          timestamp = eventOccurredAt,
          eventType = "approved-premises.application.submitted",
          eventDetails = ApplicationSubmitted(
            applicationId = application.id,
            applicationUrl = applicationUrlTemplate
              .replace("#id", application.id.toString()),
            personReference = PersonReference(
              crn = application.crn,
              noms = offenderDetails.otherIds.nomsNumber!!
            ),
            deliusEventNumber = application.eventNumber,
            mappa = mappaLevel,
            offenceId = application.offenceId,
            releaseType = submitApplication.releaseType.toString(),
            age = Period.between(offenderDetails.dateOfBirth, LocalDate.now()).years,
            gender = when (offenderDetails.gender.lowercase()) {
              "male" -> ApplicationSubmitted.Gender.male
              "female" -> ApplicationSubmitted.Gender.female
              else -> throw RuntimeException("Unknown gender: ${offenderDetails.gender}")
            },
            targetLocation = submitApplication.targetLocation,
            submittedAt = OffsetDateTime.now(),
            submittedBy = ApplicationSubmittedSubmittedBy(
              staffMember = StaffMember(
                staffCode = staffDetails.staffCode,
                staffIdentifier = staffDetails.staffIdentifier,
                forenames = staffDetails.staff.forenames,
                surname = staffDetails.staff.surname,
                username = staffDetails.username
              ),
              probationArea = ProbationArea(
                code = staffDetails.probationArea.code,
                name = staffDetails.probationArea.description
              ),
              team = Team(
                code = team.code,
                name = team.description
              ),
              ldu = Ldu(
                code = team.teamType.code,
                name = team.teamType.description
              ),
              region = Region(
                code = staffDetails.probationArea.code,
                name = staffDetails.probationArea.description
              )
            ),
            sentenceLengthInMonths = null
          )
        )
      )
    )
  }

  fun getApplicationsForCrn(crn: String, serviceName: ServiceName): List<ApplicationEntity> {
    val entityType = if (serviceName == ServiceName.approvedPremises) {
      ApprovedPremisesApplicationEntity::class.java
    } else {
      TemporaryAccommodationApplicationEntity::class.java
    }

    return applicationRepository.findByCrn(crn, entityType)
  }

  fun getOfflineApplicationsForCrn(crn: String, serviceName: ServiceName) =
    offlineApplicationRepository.findAllByServiceAndCrn(crn, serviceName.value)
}
