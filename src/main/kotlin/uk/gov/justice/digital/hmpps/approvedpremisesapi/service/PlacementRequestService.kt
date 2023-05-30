package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeBookedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Cru
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostcodeDistrictRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import java.time.OffsetDateTime
import java.util.UUID
import javax.transaction.Transactional

@Service
class PlacementRequestService(
  private val placementRequestRepository: PlacementRequestRepository,
  private val postcodeDistrictRepository: PostcodeDistrictRepository,
  private val characteristicRepository: CharacteristicRepository,
  private val userRepository: UserRepository,
  private val bookingNotMadeRepository: BookingNotMadeRepository,
  private val domainEventService: DomainEventService,
  private val offenderService: OffenderService,
  private val communityApiClient: CommunityApiClient,
  private val cruService: CruService,
  private val placementRequirementsRepository: PlacementRequirementsRepository,
  @Value("\${application-url-template}") private val applicationUrlTemplate: String
) {

  fun getVisiblePlacementRequestsForUser(user: UserEntity): List<PlacementRequestEntity> {
    return placementRequestRepository.findAllByAllocatedToUser_IdAndReallocatedAtNull(user.id)
  }

  fun getAllReallocatable(): List<PlacementRequestEntity> {
    return placementRequestRepository.findAllByReallocatedAtNullAndBooking_IdNull()
  }

  fun getPlacementRequestForUser(user: UserEntity, id: UUID): AuthorisableActionResult<PlacementRequestEntity> {
    val placementRequest = placementRequestRepository.findByIdOrNull(id)
      ?: return AuthorisableActionResult.NotFound()

    if (placementRequest.allocatedToUser.id != user.id && !user.hasRole(UserRole.WORKFLOW_MANAGER)) {
      return AuthorisableActionResult.Unauthorised()
    }

    return AuthorisableActionResult.Success(placementRequest)
  }

  fun getPlacementRequestForUserAndApplication(user: UserEntity, applicationID: UUID): AuthorisableActionResult<PlacementRequestEntity> {
    val placementRequest = placementRequestRepository.findByApplication_IdAndReallocatedAtNull(applicationID)
      ?: return AuthorisableActionResult.NotFound()

    if (!user.hasRole(UserRole.WORKFLOW_MANAGER) && placementRequest.allocatedToUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    return AuthorisableActionResult.Success(placementRequest)
  }

  fun reallocatePlacementRequest(assigneeUser: UserEntity, application: ApprovedPremisesApplicationEntity): AuthorisableActionResult<ValidatableActionResult<PlacementRequestEntity>> {
    val currentPlacementRequest = placementRequestRepository.findByApplication_IdAndReallocatedAtNull(application.id)
      ?: return AuthorisableActionResult.NotFound()

    if (currentPlacementRequest.booking != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This placement request has already been completed")
      )
    }

    if (!assigneeUser.hasRole(UserRole.MATCHER)) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.FieldValidationError(ValidationErrors().apply { this["$.userId"] = "lackingMatcherRole" })
      )
    }

    currentPlacementRequest.reallocatedAt = OffsetDateTime.now()
    placementRequestRepository.save(currentPlacementRequest)

    // Make the timestamp precision less precise, so we don't have any issues with microsecond resolution in tests
    val dateTimeNow = OffsetDateTime.now().withNano(0)

    val newPlacementRequest = placementRequestRepository.save(
      currentPlacementRequest.copy(
        id = UUID.randomUUID(),
        reallocatedAt = null,
        allocatedToUser = assigneeUser,
        createdAt = dateTimeNow,
        placementRequirements = currentPlacementRequest.placementRequirements,
      ),
    )

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(
        newPlacementRequest
      )
    )
  }

  fun createPlacementRequest(assessment: AssessmentEntity, requirements: PlacementRequirements): ValidatableActionResult<PlacementRequestEntity> =
    validated {
      val postcodeDistrict = postcodeDistrictRepository.findByOutcode(requirements.location)
        ?: return@validated ValidatableActionResult.FieldValidationError(ValidationErrors().apply { this["$.postcodeDistrict"] = "doesNotExist" })

      val user = userRepository.findRandomMatcher() ?: throw RuntimeException("No Matchers could be found")

      val desirableCriteria = characteristicRepository.findAllWherePropertyNameIn(requirements.desirableCriteria.map { it.toString() })
      val essentialCriteria = characteristicRepository.findAllWherePropertyNameIn(requirements.essentialCriteria.map { it.toString() })

      val application = (assessment.application as? ApprovedPremisesApplicationEntity) ?: throw RuntimeException("Only Approved Premises Assessments are currently supported for Placement Requests")

      val placementRequirements = placementRequirementsRepository.save(
        PlacementRequirementsEntity(
          id = UUID.randomUUID(),
          apType = requirements.type,
          gender = requirements.gender,
          postcodeDistrict = postcodeDistrict!!,
          radius = requirements.radius,
          desirableCriteria = desirableCriteria,
          essentialCriteria = essentialCriteria,
          createdAt = OffsetDateTime.now(),
          application = application,
          assessment = assessment,
        )
      )

      val placementRequestEntity = placementRequestRepository.save(
        PlacementRequestEntity(
          id = UUID.randomUUID(),
          placementRequirements = placementRequirements,
          expectedArrival = requirements.expectedArrival,
          duration = requirements.duration,
          createdAt = OffsetDateTime.now(),
          application = application,
          assessment = assessment,
          allocatedToUser = user,
          booking = null,
          bookingNotMades = mutableListOf(),
          reallocatedAt = null,
          notes = requirements.notes,
        )
      )

      return success(placementRequestEntity)
    }

  @Transactional
  fun createBookingNotMade(
    user: UserEntity,
    placementRequestId: UUID,
    notes: String?
  ): AuthorisableActionResult<BookingNotMadeEntity> {
    val bookingNotCreatedAt = OffsetDateTime.now()

    val placementRequest = placementRequestRepository.findByIdOrNull(placementRequestId)
      ?: return AuthorisableActionResult.NotFound()

    if (placementRequest.allocatedToUser.id != user.id) {
      return AuthorisableActionResult.Unauthorised()
    }

    val bookingNotMade = BookingNotMadeEntity(
      id = UUID.randomUUID(),
      placementRequest = placementRequest,
      createdAt = bookingNotCreatedAt,
      notes = notes
    )

    saveBookingNotMadeDomainEvent(user, placementRequest, bookingNotCreatedAt, notes)

    return AuthorisableActionResult.Success(
      bookingNotMadeRepository.save(bookingNotMade)
    )
  }

  private fun saveBookingNotMadeDomainEvent(
    user: UserEntity,
    placementRequest: PlacementRequestEntity,
    bookingNotCreatedAt: OffsetDateTime,
    notes: String?
  ) {
    val domainEventId = UUID.randomUUID()

    val application = placementRequest.application

    val offenderDetails = when (val offenderDetailsResult = offenderService.getOffenderByCrn(application.crn, user.deliusUsername)) {
      is AuthorisableActionResult.Success -> offenderDetailsResult.entity
      is AuthorisableActionResult.Unauthorised -> throw RuntimeException("Unable to get Offender Details when creating Booking Not Made Domain Event: Unauthorised")
      is AuthorisableActionResult.NotFound -> throw RuntimeException("Unable to get Offender Details when creating Booking Not Made Domain Event: Not Found")
    }

    val staffDetailsResult = communityApiClient.getStaffUserDetails(user.deliusUsername)
    val staffDetails = when (staffDetailsResult) {
      is ClientResult.Success -> staffDetailsResult.body
      is ClientResult.Failure -> staffDetailsResult.throwException()
    }

    domainEventService.saveBookingNotMadeEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        occurredAt = bookingNotCreatedAt.toInstant(),
        data = BookingNotMadeEnvelope(
          id = domainEventId,
          timestamp = bookingNotCreatedAt.toInstant(),
          eventType = "approved-premises.booking.not-made",
          eventDetails = BookingNotMade(
            applicationId = application.id,
            applicationUrl = applicationUrlTemplate.replace("#id", application.id.toString()),
            personReference = PersonReference(
              crn = application.crn,
              noms = offenderDetails.otherIds.nomsNumber!!
            ),
            deliusEventNumber = application.eventNumber,
            attemptedAt = bookingNotCreatedAt.toInstant(),
            attemptedBy = BookingMadeBookedBy(
              staffMember = StaffMember(
                staffCode = staffDetails.staffCode,
                staffIdentifier = staffDetails.staffIdentifier,
                forenames = staffDetails.staff.forenames,
                surname = staffDetails.staff.surname,
                username = staffDetails.username
              ),
              cru = Cru(
                name = cruService.cruNameFromProbationAreaCode(staffDetails.probationArea.code)
              )
            ),
            failureDescription = notes
          )
        )
      )
    )
  }
}
