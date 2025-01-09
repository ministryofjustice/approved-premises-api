package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationTimelineNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EnvironmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService.GetUserResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.io.IOException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@SuppressWarnings("MagicNumber", "TooGenericExceptionCaught")
@Service
class Cas1ApplicationSeedService(
  private val approvedPremisesRepository: ApprovedPremisesRepository,
  private val bookingRepository: BookingRepository,
  private val applicationTimelineNoteService: ApplicationTimelineNoteService,
  private val spaceBookingRepository: Cas1SpaceBookingRepository,
  private val offenderService: OffenderService,
  private val applicationService: ApplicationService,
  private val userService: UserService,
  private val environmentService: EnvironmentService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @SuppressWarnings("TooGenericExceptionCaught")
  fun createApplication(
    deliusUserName: String,
    crn: String,
    createIfExistingApplicationForCrn: Boolean = false,
  ) {
    if (environmentService.isNotATestEnvironment()) {
      error("Cannot create test applications as not in a test environment")
    }

    log.info("Auto-scripting application for CRN $crn")
    try {
      createApplicationInternal(
        deliusUserName = deliusUserName,
        crn = crn,
        createIfExistingApplicationForCrn = createIfExistingApplicationForCrn,
      )
    } catch (e: Exception) {
      log.error("Creating application with crn $crn failed", e)
    }
  }

  fun createOfflineApplicationWithBooking(deliusUserName: String, crn: String) {
    if (environmentService.isNotATestEnvironment()) {
      error("Cannot create test applications as not in a test environment")
    }

    log.info("Auto-scripting offline for CRN $crn")
    try {
      createOfflineApplicationInternal(deliusUserName, crn)
    } catch (e: Exception) {
      log.error("Creating offline application with crn $crn failed", e)
    }
  }

  private fun createApplicationInternal(
    deliusUserName: String,
    crn: String,
    createIfExistingApplicationForCrn: Boolean,
  ) {
    if (!createIfExistingApplicationForCrn && applicationService.getApplicationsForCrn(crn, ServiceName.approvedPremises).isNotEmpty()) {
      log.info("Already have CAS1 application for $crn, not seeding a new application")
      return
    }

    log.info("Auto creating a CAS1 application for $crn")

    val personInfo = getPersonInfo(crn)
    val createdByUser = (userService.getExistingUserOrCreate(deliusUserName) as GetUserResponse.Success).user

    val newApplicationEntity = extractEntityFromCasResult(
      applicationService.createApprovedPremisesApplication(
        offenderDetails = personInfo.offenderDetailSummary,
        user = createdByUser,
        convictionId = 2500295345,
        deliusEventNumber = "2",
        offenceId = "M2500295343",
        createWithRisks = true,
      ),
    )

    val updateResult = applicationService.updateApprovedPremisesApplication(
      applicationId = newApplicationEntity.id,
      updateFields = ApplicationService.Cas1ApplicationUpdateFields(
        isWomensApplication = false,
        isPipeApplication = null,
        isEmergencyApplication = false,
        isEsapApplication = null,
        apType = ApType.normal,
        releaseType = "licence",
        arrivalDate = LocalDate.of(2025, 12, 12),
        data = applicationData(),
        isInapplicable = false,
        noticeType = Cas1ApplicationTimelinessCategory.standard,
      ),
      userForRequest = createdByUser,
    )

    extractEntityFromCasResult(updateResult)

    applicationTimelineNoteService.saveApplicationTimelineNote(
      applicationId = newApplicationEntity.id,
      note = "Application automatically created by Cas1 Auto Script",
      user = null,
    )
  }

  private fun createOfflineApplicationInternal(deliusUserName: String, crn: String) {
    if (applicationService.getOfflineApplicationsForCrn(crn, ServiceName.approvedPremises).isNotEmpty()) {
      log.info("Already have an offline CAS1 application for $crn, not seeding a new application")
      return
    }

    val personInfo = getPersonInfo(crn)
    val offenderDetail = personInfo.offenderDetailSummary

    val offlineApplication = applicationService.createOfflineApplication(
      OfflineApplicationEntity(
        id = UUID.randomUUID(),
        crn = crn,
        service = ServiceName.approvedPremises.value,
        createdAt = OffsetDateTime.now(),
        eventNumber = "2",
        name = "${offenderDetail.firstName.uppercase()} ${offenderDetail.surname.uppercase()}",
      ),
    )

    val bookingArrivalDate = LocalDate.of(2027, 1, 2)
    val bookingDepartureDate = LocalDate.of(2027, 1, 6)

    bookingRepository.save(
      BookingEntity(
        id = UUID.randomUUID(),
        crn = crn,
        arrivalDate = bookingArrivalDate,
        departureDate = bookingDepartureDate,
        keyWorkerStaffCode = null,
        arrivals = mutableListOf(),
        departures = mutableListOf(),
        nonArrival = null,
        cancellations = mutableListOf(),
        confirmation = null,
        extensions = mutableListOf(),
        premises = approvedPremisesRepository.findAll().first(),
        bed = null,
        service = ServiceName.approvedPremises.value,
        originalArrivalDate = bookingArrivalDate,
        originalDepartureDate = bookingDepartureDate,
        createdAt = OffsetDateTime.now(),
        application = null,
        offlineApplication = offlineApplication,
        turnarounds = mutableListOf(),
        dateChanges = mutableListOf(),
        nomsNumber = personInfo.offenderDetailSummary.otherIds.nomsNumber,
        placementRequest = null,
        status = BookingStatus.confirmed,
        adhoc = true,
      ),
    )

    val spaceBookingArrivalDate = LocalDate.of(2028, 5, 12)
    val spaceBookingDepartureDate = LocalDate.of(2028, 7, 6)
    val createdByUser = (userService.getExistingUserOrCreate(deliusUserName) as GetUserResponse.Success).user

    spaceBookingRepository.save(
      Cas1SpaceBookingEntity(
        id = UUID.randomUUID(),
        premises = approvedPremisesRepository.findAll().first { it.supportsSpaceBookings },
        application = null,
        offlineApplication = offlineApplication,
        placementRequest = null,
        createdBy = createdByUser,
        createdAt = OffsetDateTime.now(),
        expectedArrivalDate = spaceBookingArrivalDate,
        expectedDepartureDate = spaceBookingDepartureDate,
        actualArrivalDate = null,
        actualArrivalTime = null,
        actualDepartureDate = null,
        actualDepartureTime = null,
        canonicalArrivalDate = spaceBookingArrivalDate,
        canonicalDepartureDate = spaceBookingDepartureDate,
        crn = crn,
        keyWorkerStaffCode = null,
        keyWorkerName = null,
        keyWorkerAssignedAt = null,
        cancellationOccurredAt = null,
        cancellationRecordedAt = null,
        cancellationReason = null,
        cancellationReasonNotes = null,
        departureMoveOnCategory = null,
        departureReason = null,
        departureNotes = null,
        criteria = emptyList<CharacteristicEntity>().toMutableList(),
        nonArrivalConfirmedAt = null,
        nonArrivalNotes = null,
        nonArrivalReason = null,
        deliusEventNumber = "2",
        migratedManagementInfoFrom = null,
      ),
    )
  }

  private fun getPersonInfo(crn: String) =
    when (
      val personInfoResult = offenderService.getPersonInfoResult(
        crn = crn,
        deliusUsername = null,
        ignoreLaoRestrictions = true,
      )
    ) {
      is PersonInfoResult.NotFound, is PersonInfoResult.Unknown -> throw NotFoundProblem(
        personInfoResult.crn,
        "Offender",
      )

      is PersonInfoResult.Success.Restricted -> throw ForbiddenProblem()
      is PersonInfoResult.Success.Full -> personInfoResult
    }

  private fun applicationData(): String {
    return dataFixtureFor(questionnaire = "application")
  }

  private fun dataFixtureFor(questionnaire: String): String {
    return loadFixtureAsResource("${questionnaire}_data.json")
  }

  private fun loadFixtureAsResource(filename: String): String {
    val path = "db/seed/local+dev+test/cas1_application_data/$filename"

    try {
      return DefaultResourceLoader().getResource(path).inputStream.bufferedReader().use { it.readText() }
    } catch (e: IOException) {
      log.warn("Failed to load seed fixture $path: " + e.message!!)
      return "{}"
    }
  }
}
