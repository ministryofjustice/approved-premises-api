package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import jakarta.transaction.Transactional
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationTimelineNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EnvironmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService.GetUserResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromValidatableActionResult
import java.io.IOException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@SuppressWarnings("MagicNumber", "MaxLineLength", "TooGenericExceptionCaught")
@Component
class Cas1AutoScript(
  private val seedLogger: SeedLogger,
  private val applicationService: ApplicationService,
  private val userService: UserService,
  private val offenderService: OffenderService,
  private val cruManagementAreaRepository: Cas1CruManagementAreaRepository,
  private val environmentService: EnvironmentService,
  private val approvedPremisesRepository: ApprovedPremisesRepository,
  private val bookingRepository: BookingRepository,
  private val applicationTimelineNoteService: ApplicationTimelineNoteService,
  private val spaceBookingRepository: Cas1SpaceBookingRepository,
) {

  @Transactional
  fun script() {
    seedLogger.info("Auto-Scripting for CAS1")

    if (environmentService.isLocal()) {
      scriptLocal()
    } else if (environmentService.isDev()) {
      scriptDev()
    }
  }

  fun scriptLocal() {
    seedLogger.info("Auto-Scripting for CAS1 local")
    seedUsers(usersToSeedLocal())

    createApplication(deliusUserName = "JIMSNOWLDAP", crn = "X320741")
    createApplication(deliusUserName = "LAOFULLACCESS", crn = "X400000")
    createApplication(deliusUserName = "LAOFULLACCESS", crn = "X400001")
    createOfflineApplicationWithBooking(deliusUserName = "JIMSNOWLDAP", crn = "X320741")
  }

  fun scriptDev() {
    seedLogger.info("Auto-Scripting for CAS1 dev")
    seedUsers(usersToSeedDev())
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun createApplication(deliusUserName: String, crn: String) {
    seedLogger.info("Auto-scripting application for CRN $crn")
    try {
      createApplicationInternal(deliusUserName = deliusUserName, crn = crn)
    } catch (e: Exception) {
      seedLogger.error("Creating application with crn $crn failed", e)
    }
  }

  private fun createOfflineApplicationWithBooking(deliusUserName: String, crn: String) {
    seedLogger.info("Auto-scripting offline for CRN $crn")
    try {
      createOfflineApplicationInternal(deliusUserName, crn)
    } catch (e: Exception) {
      seedLogger.error("Creating offline application with crn $crn failed", e)
    }
  }

  private fun seedUsers(usersToSeed: List<SeedUser>) = usersToSeed.forEach { seedUser(it) }

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun seedUser(seedUser: SeedUser) {
    seedLogger.info("Auto-scripting user ${seedUser.username}")
    try {
      val getUserResponse = userService.getExistingUserOrCreate(username = seedUser.username)

      when (getUserResponse) {
        GetUserResponse.StaffRecordNotFound -> seedLogger.error("Seeding user with ${seedUser.username} failed as staff record not found")
        is GetUserResponse.Success -> {
          val user = getUserResponse.user
          seedUser.roles.forEach { role ->
            userService.addRoleToUser(user = user, role = role)
          }
          seedUser.qualifications.forEach { qualification ->
            userService.addQualificationToUser(user, qualification)
          }
          val roles = user.roles.map { it.role }.joinToString(", ")
          seedLogger.info("  -> User '${user.name}' (${user.deliusUsername}) seeded with roles $roles")

          if (seedUser.cruManagementAreaOverrideId != null) {
            user.cruManagementAreaOverride = cruManagementAreaRepository.findByIdOrNull(seedUser.cruManagementAreaOverrideId)
          }
        }
      }
    } catch (e: Exception) {
      seedLogger.error("Seeding user with ${seedUser.username} failed", e)
    }
  }

  private fun usersToSeedLocal(): List<SeedUser> {
    return listOf(
      SeedUser(
        username = "JIMSNOWLDAP",
        roles = listOf(
          UserRole.CAS1_CRU_MEMBER,
          UserRole.CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA,
          UserRole.CAS1_ASSESSOR,
          UserRole.CAS1_MATCHER,
          UserRole.CAS1_WORKFLOW_MANAGER,
          UserRole.CAS1_REPORT_VIEWER,
          UserRole.CAS1_APPEALS_MANAGER,
          UserRole.CAS1_CRU_MEMBER,
        ),
        qualifications = UserQualification.entries.toList(),
        documentation = "For local use in development and testing",
      ),
      SeedUser(
        username = "LAOFULLACCESS",
        roles = listOf(
          UserRole.CAS1_CRU_MEMBER,
          UserRole.CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA,
          UserRole.CAS1_ASSESSOR,
          UserRole.CAS1_MATCHER,
          UserRole.CAS1_WORKFLOW_MANAGER,
          UserRole.CAS1_REPORT_VIEWER,
          UserRole.CAS1_APPEALS_MANAGER,
          UserRole.CAS1_FUTURE_MANAGER,
          UserRole.CAS1_CRU_MEMBER,
        ),
        qualifications = emptyList(),
        documentation = "For local use in development and testing. This user has an exclusion (whitelisted) for LAO CRN X400000",
      ),
      SeedUser(
        username = "LAORESTRICTED",
        roles = listOf(
          UserRole.CAS1_CRU_MEMBER,
          UserRole.CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA,
          UserRole.CAS1_ASSESSOR,
          UserRole.CAS1_MATCHER,
          UserRole.CAS1_WORKFLOW_MANAGER,
          UserRole.CAS1_REPORT_VIEWER,
          UserRole.CAS1_APPEALS_MANAGER,
          UserRole.CAS1_FUTURE_MANAGER,
          UserRole.CAS3_ASSESSOR,
        ),
        qualifications = emptyList(),
        documentation = "For local use in development and testing. This user has a restriction (blacklisted) for LAO CRN X400001",
      ),
      SeedUser(
        username = "CRUWOMENSESTATE",
        roles = listOf(
          UserRole.CAS1_CRU_MEMBER,
          UserRole.CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA,
          UserRole.CAS1_ASSESSOR,
          UserRole.CAS1_MATCHER,
          UserRole.CAS1_WORKFLOW_MANAGER,
          UserRole.CAS1_REPORT_VIEWER,
          UserRole.CAS1_APPEALS_MANAGER,
          UserRole.CAS1_FUTURE_MANAGER,
        ),
        qualifications = UserQualification.entries.toList(),
        cruManagementAreaOverrideId = Cas1CruManagementAreaEntity.WOMENS_ESTATE_ID,
        documentation = "For local use in development and testing. This user's CRU Management Area is overridden to women's estate",
      ),
    )
  }

  private fun usersToSeedDev(): List<SeedUser> =
    listOf("AP_USER_TEST_1", "AP_USER_TEST_3", "AP_USER_TEST_4", "AP_USER_TEST_5")
      .map {
        SeedUser(
          username = it,
          roles = listOf(
            UserRole.CAS1_CRU_MEMBER,
            UserRole.CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA,
            UserRole.CAS1_ASSESSOR,
            UserRole.CAS1_MATCHER,
            UserRole.CAS1_WORKFLOW_MANAGER,
            UserRole.CAS1_REPORT_VIEWER,
            UserRole.CAS1_APPEALS_MANAGER,
            UserRole.CAS1_CRU_MEMBER,
            UserRole.CAS1_FUTURE_MANAGER,
          ),
          qualifications = UserQualification.entries.toList(),
          documentation = "Generic E2E test user",
        )
      } +
      listOf(
        SeedUser(
          username = "AP_USER_TEST_2",
          roles = listOf(
            UserRole.CAS1_USER_MANAGER,
            UserRole.CAS1_REPORT_VIEWER,
          ),
          qualifications = emptyList(),
          documentation = "Admin and Reports Test User",
        ),
      )

  private fun createApplicationInternal(deliusUserName: String, crn: String) {
    if (applicationService.getApplicationsForCrn(crn, ServiceName.approvedPremises).isNotEmpty()) {
      seedLogger.info("Already have CAS1 application for $crn, not seeding a new application")
      return
    }

    seedLogger.info("Auto creating a CAS1 application for $crn")

    val personInfo = getPersonInfo(crn)
    val createdByUser = (userService.getExistingUserOrCreate(deliusUserName) as GetUserResponse.Success).user

    val newApplicationEntity = extractEntityFromValidatableActionResult(
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
      seedLogger.info("Already have an offline CAS1 application for $crn, not seeding a new application")
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
      seedLogger.warn("Failed to load seed fixture $path: " + e.message!!)
      return "{}"
    }
  }
}

data class SeedUser(
  val username: String,
  val roles: List<UserRole>,
  val qualifications: List<UserQualification>,
  val documentation: String,
  val cruManagementAreaOverrideId: UUID? = null,
)
