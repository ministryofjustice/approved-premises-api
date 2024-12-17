package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEventDetailsSubmittedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSummaryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2LockableApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UpstreamApiException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageableOrAllPages
import java.time.OffsetDateTime
import java.util.UUID

@Service("Cas2ApplicationService")
class ApplicationService(
  private val applicationRepository: Cas2ApplicationRepository,
  private val lockableApplicationRepository: Cas2LockableApplicationRepository,
  private val applicationSummaryRepository: ApplicationSummaryRepository,
  private val jsonSchemaService: JsonSchemaService,
  private val offenderService: OffenderService,
  private val userAccessService: UserAccessService,
  private val domainEventService: DomainEventService,
  private val emailNotificationService: EmailNotificationService,
  private val assessmentService: AssessmentService,
  private val notifyConfig: NotifyConfig,
  private val objectMapper: ObjectMapper,
  @Value("\${url-templates.frontend.cas2.application}") private val applicationUrlTemplate: String,
  @Value("\${url-templates.frontend.cas2.submitted-application-overview}") private val submittedApplicationUrlTemplate: String,
) {

  val repositoryUserFunctionMap = mapOf(
    null to applicationSummaryRepository::findByUserId,
    true to applicationSummaryRepository::findByUserIdAndSubmittedAtIsNotNull,
    false to applicationSummaryRepository::findByUserIdAndSubmittedAtIsNull,
  )

  val repositoryPrisonFunctionMap = mapOf(
    null to applicationSummaryRepository::findByPrisonCode,
    true to applicationSummaryRepository::findByPrisonCodeAndSubmittedAtIsNotNull,
    false to applicationSummaryRepository::findByPrisonCodeAndSubmittedAtIsNull,
  )

  fun getApplications(
    prisonCode: String?,
    isSubmitted: Boolean?,
    user: NomisUserEntity,
    pageCriteria: PageCriteria<String>,
  ): Pair<MutableList<Cas2ApplicationSummaryEntity>, PaginationMetadata?> {
    val response = if (prisonCode == null) {
      repositoryUserFunctionMap.get(isSubmitted)!!(user.id.toString(), getPageableOrAllPages(pageCriteria))
    } else {
      repositoryPrisonFunctionMap.get(isSubmitted)!!(prisonCode, getPageableOrAllPages(pageCriteria))
    }
    val metadata = getMetadata(response, pageCriteria)
    return Pair(response.content, metadata)
  }

  fun getAllSubmittedApplicationsForAssessor(pageCriteria: PageCriteria<String>): Pair<List<Cas2ApplicationSummaryEntity>, PaginationMetadata?> {
    val pageable = getPageableOrAllPages(pageCriteria)

    val response = applicationSummaryRepository.findBySubmittedAtIsNotNull(pageable)

    val metadata = getMetadata(response, pageCriteria)

    return Pair(response.content, metadata)
  }

  fun getSubmittedApplicationForAssessor(applicationId: UUID): AuthorisableActionResult<Cas2ApplicationEntity> {
    val applicationEntity = applicationRepository.findSubmittedApplicationById(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    return AuthorisableActionResult.Success(
      jsonSchemaService.checkSchemaOutdated(applicationEntity),
    )
  }

  fun getApplicationForUser(applicationId: UUID, user: NomisUserEntity): AuthorisableActionResult<Cas2ApplicationEntity> {
    val applicationEntity = applicationRepository.findByIdOrNull(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    if (applicationEntity.abandonedAt != null) {
      return AuthorisableActionResult.NotFound()
    }

    val canAccess = userAccessService.userCanViewApplication(user, applicationEntity)

    return if (canAccess) {
      AuthorisableActionResult.Success(
        jsonSchemaService.checkSchemaOutdated
          (applicationEntity),
      )
    } else {
      AuthorisableActionResult.Unauthorised()
    }
  }

  fun createApplication(crn: String, user: NomisUserEntity, jwt: String, applicationOrigin: ApplicationOrigin? = ApplicationOrigin.homeDetentionCurfew) =
    validated<Cas2ApplicationEntity> {
      val offenderDetailsResult = offenderService.getOffenderByCrn(crn)

      val offenderDetails = when (offenderDetailsResult) {
        is AuthorisableActionResult.NotFound -> return "$.crn" hasSingleValidationError "doesNotExist"
        is AuthorisableActionResult.Unauthorised -> return "$.crn" hasSingleValidationError "userPermission"
        is AuthorisableActionResult.Success -> offenderDetailsResult.entity
      }

      if (offenderDetails.otherIds.nomsNumber == null) {
        throw RuntimeException("Cannot create an Application for an Offender without a NOMS number")
      }

      if (validationErrors.any()) {
        return fieldValidationError
      }

      val createdApplication = applicationRepository.save(
        Cas2ApplicationEntity(
          id = UUID.randomUUID(),
          crn = crn,
          createdByUser = user,
          data = null,
          document = null,
          schemaVersion = jsonSchemaService.getNewestSchema(Cas2ApplicationJsonSchemaEntity::class.java),
          createdAt = OffsetDateTime.now(),
          submittedAt = null,
          schemaUpToDate = true,
          nomsNumber = offenderDetails.otherIds.nomsNumber,
          telephoneNumber = null,
          applicationOrigin = applicationOrigin?.toString(),
        ),
      )

      return success(createdApplication.apply { schemaUpToDate = true })
    }

  @SuppressWarnings("ReturnCount")
  fun updateApplication(applicationId: UUID, data: String?, user: NomisUserEntity): AuthorisableActionResult<ValidatableActionResult<Cas2ApplicationEntity>> {
    val application = applicationRepository.findByIdOrNull(applicationId)?.let(jsonSchemaService::checkSchemaOutdated)
      ?: return AuthorisableActionResult.NotFound()

    if (application.createdByUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (application.submittedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has already been submitted"),
      )
    }

    if (application.abandonedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has been abandoned"),
      )
    }

    if (!application.schemaUpToDate) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The schema version is outdated"),
      )
    }

    application.apply {
      this.data = removeXssCharacters(data)
    }

    val savedApplication = applicationRepository.save(application)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedApplication),
    )
  }

  @SuppressWarnings("ReturnCount")
  fun abandonApplication(applicationId: UUID, user: NomisUserEntity): AuthorisableActionResult<ValidatableActionResult<Cas2ApplicationEntity>> {
    val application = applicationRepository.findByIdOrNull(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    if (application.createdByUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (application.submittedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.ConflictError(applicationId, "This application has already been submitted"),
      )
    }

    if (application.abandonedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.Success(application),
      )
    }

    application.apply {
      this.abandonedAt = OffsetDateTime.now()
      this.data = null
    }

    val savedApplication = applicationRepository.save(application)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedApplication),
    )
  }

  @SuppressWarnings("ReturnCount")
  @Transactional
  fun submitApplication(
    submitApplication: SubmitCas2Application,
    user: NomisUserEntity,
  ): AuthorisableActionResult<ValidatableActionResult<Cas2ApplicationEntity>> {
    val applicationId = submitApplication.applicationId

    lockableApplicationRepository.acquirePessimisticLock(applicationId)

    var application = applicationRepository.findByIdOrNull(applicationId)
      ?.let(jsonSchemaService::checkSchemaOutdated)
      ?: return AuthorisableActionResult.NotFound()

    val serializedTranslatedDocument = objectMapper.writeValueAsString(submitApplication.translatedDocument)

    if (application.createdByUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (application.abandonedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has already been abandoned"),
      )
    }

    if (application.submittedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has already been submitted"),
      )
    }

    if (!application.schemaUpToDate) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The schema version is outdated"),
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
        ValidatableActionResult.FieldValidationError(validationErrors),
      )
    }

    val schema = application.schemaVersion as? Cas2ApplicationJsonSchemaEntity
      ?: throw RuntimeException("Incorrect type of JSON schema referenced by CAS2 Application")

    try {
      application.apply {
        submittedAt = OffsetDateTime.now()
        document = serializedTranslatedDocument
        referringPrisonCode = retrievePrisonCode(application)
        preferredAreas = submitApplication.preferredAreas
        hdcEligibilityDate = submitApplication.hdcEligibilityDate
        conditionalReleaseDate = submitApplication.conditionalReleaseDate
        telephoneNumber = submitApplication.telephoneNumber
        applicationOrigin = submitApplication.applicationOrigin?.toString()
      }
    } catch (error: UpstreamApiException) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError(error.message.toString()),
      )
    }

    application = applicationRepository.save(application)

    createCas2ApplicationSubmittedEvent(application)

    createAssessment(application)

    sendEmailApplicationSubmitted(user, application)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(application),
    )
  }

  fun createCas2ApplicationSubmittedEvent(application: Cas2ApplicationEntity) {
    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = application.submittedAt ?: OffsetDateTime.now()

    domainEventService.saveCas2ApplicationSubmittedDomainEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        nomsNumber = application.nomsNumber,
        occurredAt = eventOccurredAt.toInstant(),
        applicationOrigin = application.applicationOrigin,
        data = Cas2ApplicationSubmittedEvent(
          id = domainEventId,
          timestamp = eventOccurredAt.toInstant(),
          eventType = EventType.applicationSubmitted,
          eventDetails = Cas2ApplicationSubmittedEventDetails(
            applicationId = application.id,
            applicationUrl = applicationUrlTemplate
              .replace("#id", application.id.toString()),
            submittedAt = eventOccurredAt.toInstant(),
            personReference = PersonReference(
              noms = application.nomsNumber ?: "Unknown NOMS Number",
              crn = application.crn,
            ),
            referringPrisonCode = application.referringPrisonCode,
            preferredAreas = application.preferredAreas,
            hdcEligibilityDate = application.hdcEligibilityDate,
            conditionalReleaseDate = application.conditionalReleaseDate,
            submittedBy = Cas2ApplicationSubmittedEventDetailsSubmittedBy(
              staffMember = Cas2StaffMember(
                staffIdentifier = application.createdByUser.nomisStaffId,
                name = application.createdByUser.name,
                username = application.createdByUser.nomisUsername,
              ),
            ),
            applicationOrigin = application.applicationOrigin,
          ),
        ),
      ),
    )
  }

  fun createAssessment(application: Cas2ApplicationEntity) {
    assessmentService.createCas2Assessment(application)
  }

  @SuppressWarnings("ThrowsCount")
  private fun retrievePrisonCode(application: Cas2ApplicationEntity): String {
    val inmateDetailResult = offenderService.getInmateDetailByNomsNumber(
      crn = application.crn,
      nomsNumber = application.nomsNumber.toString(),
    )
    val inmateDetail = when (inmateDetailResult) {
      is AuthorisableActionResult.NotFound -> throw UpstreamApiException("Inmate Detail not found")
      is AuthorisableActionResult.Unauthorised -> throw UpstreamApiException("Inmate Detail unauthorised")
      is AuthorisableActionResult.Success -> inmateDetailResult.entity
    }

    return inmateDetail?.assignedLivingUnit?.agencyId ?: throw UpstreamApiException("No prison code available")
  }

  private fun sendEmailApplicationSubmitted(user: NomisUserEntity, application: Cas2ApplicationEntity) {
    emailNotificationService.sendEmail(
      recipientEmailAddress = notifyConfig.emailAddresses.cas2Assessors,
      templateId = notifyConfig.templates.cas2ApplicationSubmitted,
      personalisation = mapOf(
        "name" to user.name,
        "email" to user.email,
        "prisonNumber" to application.nomsNumber,
        "telephoneNumber" to application.telephoneNumber,
        "applicationUrl" to submittedApplicationUrlTemplate.replace("#applicationId", application.id.toString()),
      ),
      replyToEmailId = notifyConfig.emailAddresses.cas2ReplyToId,
    )
  }

  private fun removeXssCharacters(data: String?): String? {
    if (data != null) {
      val xssCharacters = setOf('<', '＜', '〈', '〈', '>', '＞', '〉', '〉')
      var sanitisedData = data
      xssCharacters.forEach { character ->
        sanitisedData = sanitisedData?.replace(character.toString(), "")
      }
      return sanitisedData
    }
    return null
  }
}
