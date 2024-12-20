package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawableType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawables
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulGetReferralDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.jsonForObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.UUID

/**
 * This test class tests common withdrawal functionality that spans multiple entity types and accordingly,
 * multiple API endpoints. Specifically:
 *
 * 1. Getting a list of withdrawable elements
 * 2. Cascading withdrawals
 *
 * When considering withdrawals, an application can be considered as a tree of elements:
 *
 * ```
 * application
 *  - assessment
 *    - request for placement (PlacementRequest)
 *      - placement (Booking)
 *    - request for placement (PlacementApplication)
 *      - matching request (PlacementRequest)
 *        - placement (Booking)
 *    - adhoc placement (Booking)
 * ```
 *
 * Withdrawals should cascade down the tree, although a booking with arrivals will block any ancestors in the tree
 * from being withdrawn
 *
 * Note: The general functionality of each entities' withdrawal endpoint is tested in the corresponding API Test Class
 */
@SuppressWarnings("LongParameterList")
class WithdrawalTest : IntegrationTestBase() {

  @Nested
  inner class GetWithdrawables {

    /**
     * ```
     * | Entities                         | Withdrawable |
     * | -------------------------------- | ------------ |
     * | Application                      | -            |
     * ```
     */
    @Test
    fun `Returns application only for a sparse application if user is not the applicant`() {
      givenAUser { applicationCreator, _ ->
        givenAUser { _, jwt ->
          givenAnOffender { offenderDetails, _ ->
            val application = produceAndPersistBasicApplication(offenderDetails.otherIds.crn, applicationCreator, "TEAM")

            val expected = Withdrawables(
              notes = emptyList(),
              withdrawables = emptyList(),
            )

            webTestClient.get()
              .uri("/applications/${application.id}/withdrawables")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .jsonForObject(expected.withdrawables)

            webTestClient.get()
              .uri("/applications/${application.id}/withdrawablesWithNotes")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .jsonForObject(expected)
          }
        }
      }
    }

    /**
     * ```
     * | Entities                         | Withdrawable |
     * | -------------------------------- | ------------ |
     * | Application                      | YES            |
     * ```
     */
    @Test
    fun `Returns application only for a sparse application if user is applicant`() {
      givenAUser { applicationCreator, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val application = produceAndPersistBasicApplication(offenderDetails.otherIds.crn, applicationCreator, "TEAM")

          val expected = Withdrawables(
            notes = emptyList(),
            withdrawables = listOf(toWithdrawable(application)),
          )

          webTestClient.get()
            .uri("/applications/${application.id}/withdrawables")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonForObject(expected.withdrawables)

          webTestClient.get()
            .uri("/applications/${application.id}/withdrawablesWithNotes")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonForObject(expected)
        }
      }
    }

    /**
     * ```
     * | Entities                         | Withdrawable |
     * | -------------------------------- | ------------ |
     * | Application                      | -            |
     * | -> Match Request 1               | Yes          |
     * | ---> Booking without arrival     | -            |
     * | -> Match Request (reallocated)   | -            |
     * | -> Match Request (withdrawn)     | -            |
     * | -> Request for placement         | Yes          |
     * | ---> Match Request 2             | -            |
     * ```
     */
    @Test
    fun `Returns match request for the original app dates only`() {
      givenAUser { applicant, jwt ->
        givenAUser { allocatedTo, _ ->
          givenAnOffender { offenderDetails, _ ->

            val (application, _) = createApplicationAndAssessment(applicant, allocatedTo, offenderDetails)

            val placementRequest = createPlacementRequest(application)
            val bookingNoArrival = createBooking(
              application,
              hasArrivalInCas1 = false,
              hasArrivalInDelius = false,
              startDate = nowPlusDays(1),
              endDate = nowPlusDays(6),
            )
            addBookingToPlacementRequest(placementRequest, bookingNoArrival)

            createPlacementRequest(application, reallocatedAt = OffsetDateTime.now())

            createPlacementRequest(application, isWithdrawn = true)

            val placementApplication = createPlacementApplication(application, DateSpan(nowPlusDays(50), duration = 6))
            createPlacementRequest(application, placementApplication = placementApplication)

            val expected = Withdrawables(
              notes = emptyList(),
              withdrawables = listOf(
                toWithdrawable(application),
                toWithdrawable(placementRequest),
                toWithdrawable(placementApplication),
              ),
            )

            webTestClient.get()
              .uri("/applications/${application.id}/withdrawables")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .jsonForObject(expected.withdrawables)

            webTestClient.get()
              .uri("/applications/${application.id}/withdrawablesWithNotes")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .jsonForObject(expected)
          }
        }
      }
    }

    /**
     * ```
     * | Entities                                     | Withdrawable |
     * | -------------------------------------------- | ------------ |
     * | Application                                  | -            |
     * | -> Request for placement 1                   | Yes          |
     * | -> Request for placement 2                   | Yes          |
     * | -> Request for placement 3 (Unsubmitted)     | -            |
     * | -> Request for placement 4 (Reallocated)     | -            |
     * | -> Request for placement 5 (With Decision)   | Yes          |
     * | -> Request for placement 6 (Withdrawn)       | -            |
     * | -> Request for placement 7 (Rejected)        | Yes          |
     * ```
     */
    @Test
    fun `Returns requests for placements applications`() {
      givenAUser { applicant, jwt ->
        givenAUser { allocatedTo, _ ->
          givenAnOffender { offenderDetails, _ ->

            val (application, _) = createApplicationAndAssessment(applicant, allocatedTo, offenderDetails)

            val submittedPlacementApplication1 = createPlacementApplication(
              application,
              dateSpans = listOf(
                DateSpan(nowPlusDays(1), duration = 5),
                DateSpan(nowPlusDays(10), duration = 10),
              ),
            )

            val submittedPlacementApplication2 = createPlacementApplication(
              application,
              DateSpan(nowPlusDays(50), duration = 6),
            )

            createPlacementApplication(
              application,
              isSubmitted = false,
              decision = null,
            )

            createPlacementApplication(
              application,
              DateSpan(LocalDate.now(), duration = 2),
              reallocatedAt = OffsetDateTime.now(),
            )

            val applicationWithAcceptedDecision = createPlacementApplication(
              application,
              DateSpan(nowPlusDays(50), duration = 6),
              decision = PlacementApplicationDecision.ACCEPTED,
            )

            createPlacementApplication(
              application,
              DateSpan(now(), duration = 2),
              decision = PlacementApplicationDecision.ACCEPTED,
              isWithdrawn = true,
            )

            val applicationWithRejectedDecision = createPlacementApplication(
              application,
              DateSpan(nowPlusDays(50), duration = 6),
              decision = PlacementApplicationDecision.REJECTED,
            )

            val expected = Withdrawables(
              notes = emptyList(),
              withdrawables = listOf(
                toWithdrawable(application),
                toWithdrawable(submittedPlacementApplication1),
                toWithdrawable(submittedPlacementApplication2),
                toWithdrawable(applicationWithAcceptedDecision),
                toWithdrawable(applicationWithRejectedDecision),
              ),
            )

            webTestClient.get()
              .uri("/applications/${application.id}/withdrawables")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .jsonForObject(expected.withdrawables)

            webTestClient.get()
              .uri("/applications/${application.id}/withdrawablesWithNotes")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .jsonForObject(expected)
          }
        }
      }
    }

    /**
     * ```
     * | Entities                         | Withdrawable |
     * | -------------------------------- | ------------ |
     * | Application                      | BLOCKED      |
     * | -> Request for placement 1       | YES          |
     * | ---> Match request 1             | -            |
     * | -----> Booking 1 arrival pending | YES          |
     * | ---> Match request 2             | -            |
     * | -> Request for placement 2       | YES          |
     * | -> Match request 3               | BLOCKED      |
     * | ---> Booking 2 has arrival       | BLOCKING     |
     * | -> Adhoc Booking                 | YES          |
     * ```
     */
    @Test
    fun `Returns all possible types when a user can manage bookings, with booking arrivals in CAS1 blocking bookings`() {
      givenAUser { applicant, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
          givenAUser { requestForPlacementAssessor, _ ->
            givenAnOffender { offenderDetails, _ ->
              val (application, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)
              val (otherApplication, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)

              val placementApplication1 = createPlacementApplication(application, DateSpan(now(), duration = 2))
              val placementRequest1 = createPlacementRequest(application, placementApplication = placementApplication1)
              val booking1NoArrival = createBooking(
                application = application,
                hasArrivalInCas1 = false,
                hasArrivalInDelius = false,
                adhoc = false,
                startDate = nowPlusDays(1),
                endDate = nowPlusDays(6),
              )
              addBookingToPlacementRequest(placementRequest1, booking1NoArrival)

              createPlacementRequest(application, placementApplication = placementApplication1)

              val placementApplication2 = createPlacementApplication(
                application,
                DateSpan(now(), duration = 2),
                allocatedTo = requestForPlacementAssessor,
              )

              val placementRequest3 = createPlacementRequest(application)
              val booking2HasArrival = createBooking(
                application = application,
                hasArrivalInCas1 = true,
                startDate = LocalDate.now(),
                endDate = nowPlusDays(1),
              )
              addBookingToPlacementRequest(placementRequest3, booking2HasArrival)

              val adhocBooking = createBooking(
                application = application,
                adhoc = true,
                hasArrivalInCas1 = false,
                hasArrivalInDelius = false,
                startDate = nowPlusDays(20),
                endDate = nowPlusDays(26),
              )

              createBooking(
                application = otherApplication,
                adhoc = true,
                hasArrivalInCas1 = false,
                startDate = nowPlusDays(20),
                endDate = nowPlusDays(26),
              )
              createBooking(
                application = otherApplication,
                adhoc = null,
                hasArrivalInCas1 = false,
                startDate = nowPlusDays(20),
                endDate = nowPlusDays(26),
              )

              val expected = Withdrawables(
                notes = listOf("1 or more placements cannot be withdrawn as they have an arrival"),
                withdrawables = listOf(
                  toWithdrawable(placementApplication1),
                  toWithdrawable(booking1NoArrival),
                  toWithdrawable(placementApplication2),
                  toWithdrawable(adhocBooking),
                ),
              )

              webTestClient.get()
                .uri("/applications/${application.id}/withdrawables")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonForObject(expected.withdrawables)

              webTestClient.get()
                .uri("/applications/${application.id}/withdrawablesWithNotes")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonForObject(expected)
            }
          }
        }
      }
    }

    /**
     * ```
     * | Entities                             | Withdrawable |
     * | ------------------------------------ | ------------ |
     * | Application                          | BLOCKED      |
     * | -> Request for placement 1           | YES          |
     * | ---> Match request 1                 | -            |
     * | -----> Space Booking arrival pending | YES          |
     * | ---> Match request 2                 | -            |
     * | -> Request for placement 2           | YES          |
     * | -> Match request 3                   | BLOCKED      |
     * | ---> Booking has arrival in Delius   | BLOCKING     |
     * | -> Adhoc Booking                     | YES          |
     * ```
     */
    @Test
    fun `Returns all possible types when a user can manage bookings, with booking arrivals in Delius blocking bookings`() {
      givenAUser { applicant, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { _, jwt ->
          givenAUser { requestForPlacementAssessor, _ ->
            givenAnOffender { offenderDetails, _ ->
              val (application, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)
              val (otherApplication, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)

              val placementApplication1 = createPlacementApplication(application, DateSpan(now(), duration = 2))
              val placementRequest1 = createPlacementRequest(application, placementApplication = placementApplication1)
              val spaceBookingNoArrival = createSpaceBooking(
                application = application,
                startDate = nowPlusDays(1),
                endDate = nowPlusDays(6),
                arrivalDate = null,
                placementRequest = placementRequest1,
              )

              createPlacementRequest(application, placementApplication = placementApplication1)

              val placementApplication2 = createPlacementApplication(
                application,
                DateSpan(now(), duration = 2),
                allocatedTo = requestForPlacementAssessor,
              )

              val placementRequest3 = createPlacementRequest(application)
              val bookingHasArrivalInDelius = createBooking(
                application = application,
                hasArrivalInCas1 = false,
                hasArrivalInDelius = true,
                startDate = LocalDate.now(),
                endDate = nowPlusDays(1),
              )
              addBookingToPlacementRequest(placementRequest3, bookingHasArrivalInDelius)

              val adhocBooking = createBooking(
                application = application,
                adhoc = true,
                hasArrivalInCas1 = false,
                hasArrivalInDelius = false,
                startDate = nowPlusDays(20),
                endDate = nowPlusDays(26),
              )

              createBooking(
                application = otherApplication,
                adhoc = true,
                hasArrivalInCas1 = false,
                startDate = nowPlusDays(20),
                endDate = nowPlusDays(26),
              )
              createBooking(
                application = otherApplication,
                adhoc = null,
                hasArrivalInCas1 = false,
                startDate = nowPlusDays(20),
                endDate = nowPlusDays(26),
              )

              val expected = Withdrawables(
                notes = listOf("1 or more placements cannot be withdrawn as they have an arrival recorded in Delius"),
                withdrawables = listOf(
                  toWithdrawable(placementApplication1),
                  toWithdrawable(spaceBookingNoArrival),
                  toWithdrawable(placementApplication2),
                  toWithdrawable(adhocBooking),
                ),
              )

              webTestClient.get()
                .uri("/applications/${application.id}/withdrawables")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonForObject(expected.withdrawables)

              webTestClient.get()
                .uri("/applications/${application.id}/withdrawablesWithNotes")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonForObject(expected)
            }
          }
        }
      }
    }

    /**
     * ```
     * | Entities                             | Withdrawable |
     * | ------------------------------------ | ------------ |
     * | Application                          | BLOCKED      |
     * | -> Request for placement 1           | BLOCKED      |
     * | ---> Match request 1                 | -            |
     * | -----> Booking 1 has arrival in CAS1 | BLOCKING     |
     * | ---> Match request 2                 | -            |
     * | -> Request for placement 2           | YES          |
     * | -> Match request 3                   | BLOCKED      |
     * | ---> Booking 2 has arrival in Delius | BLOCKING     |
     * | -> Adhoc Booking                     | YES          |
     * ```
     */
    @Test
    fun `Returns all possible types when a user can manage bookings, with booking arrivals in CAS1 and Delius blocking bookings`() {
      givenAUser { applicant, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
          givenAUser { requestForPlacementAssessor, _ ->
            givenAnOffender { offenderDetails, _ ->
              val (application, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)
              val (otherApplication, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)

              val placementApplication1 = createPlacementApplication(application, DateSpan(now(), duration = 2))
              val placementRequest1 = createPlacementRequest(application, placementApplication = placementApplication1)
              val booking1HasArrivalInCas1 = createBooking(
                application = application,
                hasArrivalInCas1 = true,
                hasArrivalInDelius = false,
                adhoc = false,
                startDate = nowPlusDays(1),
                endDate = nowPlusDays(6),
              )
              addBookingToPlacementRequest(placementRequest1, booking1HasArrivalInCas1)

              createPlacementRequest(application, placementApplication = placementApplication1)

              val placementApplication2 = createPlacementApplication(
                application,
                DateSpan(now(), duration = 2),
                allocatedTo = requestForPlacementAssessor,
              )

              val placementRequest3 = createPlacementRequest(application)
              val booking2HasArrivalInDelius = createBooking(
                application = application,
                hasArrivalInCas1 = false,
                hasArrivalInDelius = true,
                startDate = LocalDate.now(),
                endDate = nowPlusDays(1),
              )
              addBookingToPlacementRequest(placementRequest3, booking2HasArrivalInDelius)

              val adhocBooking = createBooking(
                application = application,
                adhoc = true,
                hasArrivalInCas1 = false,
                hasArrivalInDelius = false,
                startDate = nowPlusDays(20),
                endDate = nowPlusDays(26),
              )

              createBooking(
                application = otherApplication,
                adhoc = true,
                hasArrivalInCas1 = false,
                startDate = nowPlusDays(20),
                endDate = nowPlusDays(26),
              )
              createBooking(
                application = otherApplication,
                adhoc = null,
                hasArrivalInCas1 = false,
                startDate = nowPlusDays(20),
                endDate = nowPlusDays(26),
              )

              val expected = Withdrawables(
                notes = listOf(
                  "1 or more placements cannot be withdrawn as they have an arrival",
                  "1 or more placements cannot be withdrawn as they have an arrival recorded in Delius",
                ),
                withdrawables = listOf(
                  toWithdrawable(placementApplication2),
                  toWithdrawable(adhocBooking),
                ),
              )

              webTestClient.get()
                .uri("/applications/${application.id}/withdrawables")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonForObject(expected.withdrawables)

              webTestClient.get()
                .uri("/applications/${application.id}/withdrawablesWithNotes")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonForObject(expected)
            }
          }
        }
      }
    }

    /**
     * ```
     * | Entities                         | Withdrawable |
     * | -------------------------------- | ------------ |
     * | Application                      | BLOCKED      |
     * | -> Request for placement 1       | YES          |
     * | ---> Match request 1             | -            |
     * | -----> Booking 1 arrival pending | -            |
     * | ---> Match request 2             | -            |
     * | -> Request for placement 2       | YES          |
     * | -> Match request 3               | BLOCKED      |
     * | ---> Booking 2 has arrival       | -            |
     * | -> Match request 4               | BLOCKED      |
     * | ---> Space Booking has arrival   | BLOCKED      |
     * | -> Adhoc Booking                 | -            |
     * ```
     */
    @Test
    fun `Returns all possible types when a user cannot manage bookings, with booking and space booking arrivals in CAS1 blocking bookings`() {
      givenAUser { applicant, jwt ->
        givenAUser { requestForPlacementAssessor, _ ->
          givenAnOffender { offenderDetails, _ ->
            val (application, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)

            val placementApplication1 = createPlacementApplication(application, DateSpan(now(), duration = 2))
            val placementRequest1 = createPlacementRequest(application, placementApplication = placementApplication1)
            val booking1NoArrival = createBooking(
              application = application,
              hasArrivalInCas1 = false,
              hasArrivalInDelius = false,
              startDate = nowPlusDays(1),
              endDate = nowPlusDays(6),
            )
            addBookingToPlacementRequest(placementRequest1, booking1NoArrival)

            createPlacementRequest(application, placementApplication = placementApplication1)

            val placementApplication2 = createPlacementApplication(
              application,
              DateSpan(now(), duration = 2),
              allocatedTo = requestForPlacementAssessor,
            )

            val placementRequest3 = createPlacementRequest(application)
            val booking2HasArrival = createBooking(
              application = application,
              hasArrivalInCas1 = true,
              startDate = LocalDate.now(),
              endDate = nowPlusDays(1),
            )
            addBookingToPlacementRequest(placementRequest3, booking2HasArrival)

            val placementRequest4 = createPlacementRequest(application)
            createSpaceBooking(
              application = application,
              startDate = LocalDate.now(),
              endDate = nowPlusDays(1),
              arrivalDate = LocalDateTime.now(),
              placementRequest = placementRequest4,
            )

            createBooking(
              application = application,
              hasArrivalInCas1 = false,
              adhoc = false,
              startDate = nowPlusDays(20),
              endDate = nowPlusDays(26),
            )

            val expected = Withdrawables(
              notes = listOf("1 or more placements cannot be withdrawn as they have an arrival"),
              withdrawables = listOf(
                toWithdrawable(placementApplication1),
                toWithdrawable(placementApplication2),
              ),
            )

            webTestClient.get()
              .uri("/applications/${application.id}/withdrawablesWithNotes")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .jsonForObject(expected)
          }
        }
      }
    }
  }

  @Nested
  inner class WithdrawalCascading {

    /**
     * ```
     * |                                  |           |         Receive Email          |
     * | Entities                         | Withdrawn | PP   | Case Manager | Assessor |
     * | -------------------------------- | --------- | ---- | ------------ | -------- |
     * | Application                      | YES       | YES  | YES          | -        |
     * | -> Assessment (pending)          | YES       | -    | -            | YES      |
     * ```
     */
    @Test
    fun `Withdrawing an application cascades to an assessment and sends mail to assessor if pending`() {
      givenAUser { applicant, jwt ->
        givenAUser { assessor, _ ->
          givenAnOffender { offenderDetails, _ ->
            val caseManagerEmail = "caseManager@test.com"

            val (application, assessment) = createApplicationAndAssessment(
              applicant = applicant,
              assignee = applicant,
              offenderDetails = offenderDetails,
              assessmentSubmitted = false,
              assessmentAllocatedTo = assessor,
              caseManager = cas1ApplicationUserDetailsEntityFactory.produceAndPersist {
                withEmailAddress(caseManagerEmail)
              },
            )

            withdrawApplication(application, jwt)

            assertApplicationWithdrawn(application)
            assertAssessmentWithdrawn(assessment)

            val applicantEmail = applicant.email!!
            val assessorEmail = assessment.allocatedToUser!!.email!!

            emailAsserter.assertEmailsRequestedCount(3)
            assertApplicationWithdrawnEmail(applicantEmail, application)
            assertApplicationWithdrawnEmail(caseManagerEmail, application)
            assertAssessmentWithdrawnEmail(assessorEmail)
          }
        }
      }
    }

    /**
     * For this test the applicant is not the same person as the case manager, meaning
     * emails typically sent to the applicant will also be sent to the case manager
     *
     * ```
     * |                                                |             Receive Email                 |
     * | Entities                           | Withdrawn | PP  | Case Manager | AP  | CRU | Assessor |
     * | ---------------------------------- | --------- | --- | ------------ | --- | --- | -------- |
     * | Application                        | YES       | YES | YES          | -   | -   | -        |
     * | -> Assessment                      | YES       | -   | -            | -   | -   | -        |
     * | -> Request for placement 1         | YES       | YES | YES          | -   | -   | -        |
     * | ---> Match request 1               | YES       | -   | -            | -   | -   | -        |
     * | -----> Booking arrival pending     | YES       | YES | YES          | YES | YES | -        |
     * | ---> Match request 2               | YES       | -   | -            | -   | YES | -        |
     * | -> Request for placement 2         | YES       | YES | YES          | -   | -   | YES      |
     * | -> Match request 2                 | YES       | YES | YES          | -   | -   | -        |
     * | ---> Space Booking arrival pending | YES       | YES | YES          | YES | YES | -        |
     * | -> Adhoc Booking 1 arrival pending | YES       | YES | YES          | YES | YES | -        |
     * | -> Adhoc Booking 2 arrival pending | YES       | YES | YES          | YES | YES | -        |
     * ```
     */
    @Test
    fun `Withdrawing an application cascades to applicable entities`() {
      givenAUser { applicant, jwt ->
        givenAUser { requestForPlacementAssessor, _ ->
          givenAnOffender { offenderDetails, _ ->
            val caseManagerEmail = "caseManager@test.com"

            val (application, assessment) = createApplicationAndAssessment(
              applicant = applicant,
              assignee = applicant,
              offenderDetails = offenderDetails,
              caseManager = cas1ApplicationUserDetailsEntityFactory.produceAndPersist {
                withEmailAddress(caseManagerEmail)
              },
            )
            val (otherApplication, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)

            val placementApplication1 = createPlacementApplication(application, DateSpan(now(), duration = 2))
            val placementRequest1 = createPlacementRequest(application, placementApplication = placementApplication1)
            val bookingNoArrival = createBooking(
              application = application,
              hasArrivalInCas1 = false,
              hasArrivalInDelius = false,
              startDate = nowPlusDays(1),
              endDate = nowPlusDays(6),
            )
            addBookingToPlacementRequest(placementRequest1, bookingNoArrival)

            val placementApplication2NoBookingBeingAssessed = createPlacementApplication(
              application,
              DateSpan(nowPlusDays(2), duration = 2),
              allocatedTo = requestForPlacementAssessor,
              decision = null,
            )

            val placementRequest2 = createPlacementRequest(application)
            val spaceBookingNoArrival = createSpaceBooking(
              application = application,
              startDate = nowPlusDays(5),
              endDate = nowPlusDays(15),
              arrivalDate = null,
              placementRequest = placementRequest2,
            )

            val adhocBooking1NoArrival = createBooking(
              application = application,
              hasArrivalInCas1 = false,
              hasArrivalInDelius = false,
              startDate = nowPlusDays(1),
              endDate = nowPlusDays(6),
              adhoc = true,
            )

            // we don't know the adhoc status for some legacy
            // applications. in this case adhoc is 'null'
            // for these cases we treat them as adhoc bookings
            val adhocBooking2NoArrival = createBooking(
              application = application,
              hasArrivalInCas1 = false,
              hasArrivalInDelius = false,
              startDate = nowPlusDays(5),
              endDate = nowPlusDays(7),
              adhoc = null,
            )

            // regression test to ensure other application's
            // bookings aren't affected
            createBooking(
              application = otherApplication,
              adhoc = true,
              hasArrivalInCas1 = false,
              startDate = nowPlusDays(20),
              endDate = nowPlusDays(26),
            )
            createBooking(
              application = otherApplication,
              adhoc = null,
              hasArrivalInCas1 = false,
              startDate = nowPlusDays(25),
              endDate = nowPlusDays(28),
            )

            withdrawApplication(application, jwt)

            assertApplicationWithdrawn(application)
            assertAssessmentWithdrawn(assessment)

            assertPlacementApplicationWithdrawn(
              placementApplication1,
              PlacementApplicationWithdrawalReason.RELATED_APPLICATION_WITHDRAWN,
            )
            assertPlacementRequestWithdrawn(
              placementRequest1,
              PlacementRequestWithdrawalReason.RELATED_APPLICATION_WITHDRAWN,
            )
            assertBookingWithdrawn(bookingNoArrival, "Related application withdrawn")

            assertPlacementApplicationWithdrawn(
              placementApplication2NoBookingBeingAssessed,
              PlacementApplicationWithdrawalReason.RELATED_APPLICATION_WITHDRAWN,
            )

            assertPlacementRequestWithdrawn(
              placementRequest2,
              PlacementRequestWithdrawalReason.RELATED_APPLICATION_WITHDRAWN,
            )
            assertSpaceBookingWithdrawn(spaceBookingNoArrival, "Related application withdrawn")

            assertBookingWithdrawn(adhocBooking1NoArrival, "Related application withdrawn")
            assertBookingWithdrawn(adhocBooking2NoArrival, "Related application withdrawn")

            val applicantEmail = applicant.email!!
            val cruEmail = application.cruManagementArea!!.emailAddress!!
            val requestForPlacementAssessorEmail = requestForPlacementAssessor.email!!

            emailAsserter.assertEmailsRequestedCount(25)
            assertApplicationWithdrawnEmail(applicantEmail, application)
            assertApplicationWithdrawnEmail(caseManagerEmail, application)

            assertPlacementRequestWithdrawnEmail(applicantEmail, placementApplication1)
            assertPlacementRequestWithdrawnEmail(caseManagerEmail, placementApplication1)

            assertBookingWithdrawnEmail(applicantEmail, bookingNoArrival)
            assertBookingWithdrawnEmail(caseManagerEmail, bookingNoArrival)
            assertBookingWithdrawnEmail(bookingNoArrival.premises.emailAddress!!, bookingNoArrival)
            assertBookingWithdrawnEmail(cruEmail, bookingNoArrival)

            assertPlacementRequestWithdrawnEmail(applicantEmail, placementApplication2NoBookingBeingAssessed)
            assertPlacementRequestWithdrawnEmail(caseManagerEmail, placementApplication2NoBookingBeingAssessed)
            assertPlacementRequestWithdrawnEmail(requestForPlacementAssessorEmail, placementApplication2NoBookingBeingAssessed)

            assertPlacementRequestWithdrawnEmail(applicantEmail, placementRequest2)
            assertPlacementRequestWithdrawnEmail(caseManagerEmail, placementRequest2)

            assertSpaceBookingWithdrawnEmail(applicantEmail, spaceBookingNoArrival)
            assertSpaceBookingWithdrawnEmail(caseManagerEmail, spaceBookingNoArrival)
            assertSpaceBookingWithdrawnEmail(spaceBookingNoArrival.premises.emailAddress!!, spaceBookingNoArrival)
            assertSpaceBookingWithdrawnEmail(cruEmail, spaceBookingNoArrival)

            assertBookingWithdrawnEmail(applicantEmail, adhocBooking1NoArrival)
            assertBookingWithdrawnEmail(caseManagerEmail, adhocBooking1NoArrival)
            assertBookingWithdrawnEmail(adhocBooking1NoArrival.premises.emailAddress!!, adhocBooking1NoArrival)
            assertBookingWithdrawnEmail(cruEmail, adhocBooking1NoArrival)

            assertBookingWithdrawnEmail(applicantEmail, adhocBooking2NoArrival)
            assertBookingWithdrawnEmail(caseManagerEmail, adhocBooking2NoArrival)
            assertBookingWithdrawnEmail(adhocBooking2NoArrival.premises.emailAddress!!, adhocBooking2NoArrival)
            assertBookingWithdrawnEmail(cruEmail, adhocBooking2NoArrival)
          }
        }
      }
    }

    /**
     * For this test the applicant is also the case manager
     *
     * ```
     * | Entities                         | Withdrawn | Email PP | Email AP | Email CRU | Email Assessor |
     * | ---------------------------------| --------- | -------- | -------- | --------- | -------------- |
     * | Application                      | BLOCKED   | -        | -        | -         | -              |
     * | -> Placement Request             | BLOCKED   | -        | -        | -         | -              |
     * | ---> Match request               | BLOCKED   | -        | -        | -         | -              |
     * | -----> Booking has arrival       | BLOCKED   | -        | -        | -         | -              |
     * ```
     */
    @Test
    fun `Withdrawing an application is not allowed if has a booking with arrivals`() {
      givenAUser { applicant, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val (application, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)

          val placementApplication = createPlacementApplication(application, DateSpan(now(), duration = 2))
          val placementRequest = createPlacementRequest(application, placementApplication = placementApplication)
          val bookingWithArrival = createBooking(
            application = application,
            hasArrivalInCas1 = true,
            startDate = nowPlusDays(1),
            endDate = nowPlusDays(6),
          )
          addBookingToPlacementRequest(placementRequest, bookingWithArrival)

          webTestClient.post()
            .uri("/applications/${application.id}/withdrawal")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewWithdrawal(
                reason = WithdrawalReason.duplicateApplication,
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
        }
      }
    }

    /**
     * For this test the applicant is also the case manager
     *
     * ```
     * | Entities                         | Withdrawn | Email PP | Email AP | Email CRU | Email Assessor |
     * | ---------------------------------| --------- | -------- | -------- | --------- | -------------- |
     * | Application                      | BLOCKED   | -        | -        | -         | -              |
     * | -> Placement Request             | BLOCKED   | -        | -        | -         | -              |
     * | ---> Match request               | BLOCKED   | -        | -        | -         | -              |
     * | -----> Space Booking has arrival | BLOCKED   | -        | -        | -         | -              |
     * ```
     */
    @Test
    fun `Withdrawing an application is not allowed if has a space with arrivals`() {
      givenAUser { applicant, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val (application, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)

          val placementApplication = createPlacementApplication(application, DateSpan(now(), duration = 2))
          val placementRequest = createPlacementRequest(application, placementApplication = placementApplication)
          createSpaceBooking(
            application = application,
            startDate = nowPlusDays(1),
            endDate = nowPlusDays(6),
            arrivalDate = nowPlusDays(1).atStartOfDay(),
            placementRequest = placementRequest,
          )

          webTestClient.post()
            .uri("/applications/${application.id}/withdrawal")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewWithdrawal(
                reason = WithdrawalReason.duplicateApplication,
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
        }
      }
    }

    /**
     * For this test the applicant is also the case manager
     *
     * ```
     * | Entities                         | Withdrawn | Email PP | Email AP | Email CRU | Email Assessor |
     * | -------------------------------- | --------- | -------- | -------- | --------- | -------------- |
     * | Application                      | -         | -        | -        | -         | -              |
     * | -> Assessment                    | -         | -        | -        | -         | -              |
     * | -> Request for placement 1       | YES       | YES      | -        | -         | -              |
     * | ---> Match request 1             | YES       | -        | -        | -         | -              |
     * | -----> Booking 1 arrival pending | YES       | YES      | YES      | YES       | -              |
     * | ---> Match request 2             | YES       | -        | -        | -         | -              |
     * | -----> Booking 2 arrival pending | YES       | YES      | YES      | YES       | -              |
     * | -> Request for placement 2       | -         | -        | -        | -         | -              |
     * | ---> Match request 3             | -         | -        | -        | -         | -              |
     * | -----> Booking 3 arrival pending | -         | YES      | -        | -         | -              |
     * ```
     */
    @Test
    fun `Withdrawing a request for placement cascades to applicable entities`() {
      givenAUser { applicant, _ ->
        givenAUser { placementAppCreator, jwt ->
          givenAnOffender { offenderDetails, _ ->
            val (application, assessment) = createApplicationAndAssessment(applicant, applicant, offenderDetails)

            val placementApplication1 = createPlacementApplication(application, DateSpan(now(), duration = 2), createdBy = placementAppCreator)
            val placementRequest1 = createPlacementRequest(application, placementApplication = placementApplication1)
            val booking1NoArrival = createBooking(
              application = application,
              hasArrivalInCas1 = false,
              hasArrivalInDelius = false,
              startDate = nowPlusDays(1),
              endDate = nowPlusDays(6),
            )
            addBookingToPlacementRequest(placementRequest1, booking1NoArrival)

            val placementRequest2 = createPlacementRequest(application, placementApplication = placementApplication1)
            val booking2NoArrival = createBooking(
              application = application,
              hasArrivalInCas1 = false,
              hasArrivalInDelius = false,
              startDate = nowPlusDays(10),
              endDate = nowPlusDays(21),
            )
            addBookingToPlacementRequest(placementRequest2, booking2NoArrival)

            val placementApplication2 = createPlacementApplication(application, DateSpan(now(), duration = 2))
            val placementRequest3 = createPlacementRequest(application, placementApplication = placementApplication2)
            val booking3NoArrival = createBooking(
              application = application,
              hasArrivalInCas1 = false,
              startDate = nowPlusDays(10),
              endDate = nowPlusDays(21),
            )
            addBookingToPlacementRequest(placementRequest3, booking3NoArrival)

            withdrawPlacementApplication(
              placementApplication1,
              WithdrawPlacementRequestReason.duplicatePlacementRequest,
              jwt,
            )

            assertApplicationNotWithdrawn(application)
            assertAssessmentNotWithdrawn(assessment)

            assertPlacementApplicationWithdrawn(
              placementApplication1,
              PlacementApplicationWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST,
            )

            assertPlacementRequestWithdrawn(
              placementRequest1,
              PlacementRequestWithdrawalReason.RELATED_PLACEMENT_APPLICATION_WITHDRAWN,
            )
            assertBookingWithdrawn(booking1NoArrival, "Related request for placement withdrawn")

            assertPlacementRequestWithdrawn(
              placementRequest2,
              PlacementRequestWithdrawalReason.RELATED_PLACEMENT_APPLICATION_WITHDRAWN,
            )
            assertBookingWithdrawn(booking2NoArrival, "Related request for placement withdrawn")

            assertPlacementApplicationNotWithdrawn(placementApplication2)
            assertPlacementRequestNotWithdrawn(placementRequest3)
            assertBookingNotWithdrawn(booking3NoArrival)

            val applicantEmail = applicant.email!!
            val placementAppCreatorEmail = placementAppCreator.email!!
            val cruEmail = application.cruManagementArea!!.emailAddress!!

            emailAsserter.assertEmailsRequestedCount(10)
            assertPlacementRequestWithdrawnEmail(applicantEmail, placementRequest1)
            assertPlacementRequestWithdrawnEmail(placementAppCreatorEmail, placementRequest1)

            assertBookingWithdrawnEmail(applicantEmail, booking1NoArrival)
            assertBookingWithdrawnEmail(placementAppCreatorEmail, booking1NoArrival)
            assertBookingWithdrawnEmail(booking1NoArrival.premises.emailAddress!!, booking1NoArrival)
            assertBookingWithdrawnEmail(cruEmail, booking1NoArrival)

            assertBookingWithdrawnEmail(applicantEmail, booking2NoArrival)
            assertBookingWithdrawnEmail(placementAppCreatorEmail, booking2NoArrival)
            assertBookingWithdrawnEmail(booking2NoArrival.premises.emailAddress!!, booking2NoArrival)
            assertBookingWithdrawnEmail(cruEmail, booking2NoArrival)
          }
        }
      }
    }

    /**
     * For this test the applicant is also the case manager
     *
     * ```
     * | Entities                           | Withdrawn | Email PP | Email AP | Email CRU | Email Assessor |
     * | ---------------------------------- | --------- | -------- | -------- | --------- | -------------- |
     * | Application                        | -         | -        | -        | -         | -              |
     * | -> Assessment                      | -         | -        | -        | -         | -              |
     * | -> Request for placement 1         | YES       | YES      | -        | -         | -              |
     * | ---> Match request 1               | YES       | -        | -        | -         | -              |
     * | -----> Booking 1 adhoc             | -         | -        | -        | -         | -              |
     * | ---> Match request 2               | YES       | -        | -        | -         | -              |
     * | -----> Booking 2 not adhoc         | YES       | YES      | YES      | YES       | -              |
     * | ---> Match request 3               | YES       | -        | -        | -         | -              |
     * | -----> Booking 3 potentially adhoc | -         | -        | -        | -         | -              |
     * ```
     */
    @Test
    fun `Withdrawing a request for placement does not cascade to incorrectly linked adhoc placements`() {
      givenAUser { applicant, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val (application, assessment) = createApplicationAndAssessment(applicant, applicant, offenderDetails)

          val placementApplication1 = createPlacementApplication(application, DateSpan(now(), duration = 2))
          val placementRequest1 = createPlacementRequest(application, placementApplication = placementApplication1)
          val booking1Adhoc = createBooking(
            application = application,
            adhoc = true,
            hasArrivalInCas1 = false,
            startDate = nowPlusDays(1),
            endDate = nowPlusDays(6),
          )
          addBookingToPlacementRequest(placementRequest1, booking1Adhoc)

          val placementRequest2 = createPlacementRequest(application, placementApplication = placementApplication1)
          val booking2NoArrival = createBooking(
            application = application,
            adhoc = false,
            hasArrivalInCas1 = false,
            hasArrivalInDelius = false,
            startDate = nowPlusDays(10),
            endDate = nowPlusDays(21),
          )
          addBookingToPlacementRequest(placementRequest2, booking2NoArrival)

          val placementRequest3 = createPlacementRequest(application, placementApplication = placementApplication1)
          val booking3PotentiallyAdhoc = createBooking(
            application = application,
            adhoc = null,
            hasArrivalInCas1 = false,
            startDate = nowPlusDays(10),
            endDate = nowPlusDays(21),
          )
          addBookingToPlacementRequest(placementRequest3, booking3PotentiallyAdhoc)

          withdrawPlacementApplication(
            placementApplication1,
            WithdrawPlacementRequestReason.duplicatePlacementRequest,
            jwt,
          )

          assertApplicationNotWithdrawn(application)
          assertAssessmentNotWithdrawn(assessment)

          assertPlacementApplicationWithdrawn(
            placementApplication1,
            PlacementApplicationWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST,
          )

          assertPlacementRequestWithdrawn(
            placementRequest1,
            PlacementRequestWithdrawalReason.RELATED_PLACEMENT_APPLICATION_WITHDRAWN,
          )
          assertBookingNotWithdrawn(booking1Adhoc)

          assertPlacementRequestWithdrawn(
            placementRequest2,
            PlacementRequestWithdrawalReason.RELATED_PLACEMENT_APPLICATION_WITHDRAWN,
          )
          assertBookingWithdrawn(booking2NoArrival, "Related request for placement withdrawn")

          assertPlacementRequestWithdrawn(
            placementRequest3,
            PlacementRequestWithdrawalReason.RELATED_PLACEMENT_APPLICATION_WITHDRAWN,
          )
          assertBookingNotWithdrawn(booking3PotentiallyAdhoc)

          val applicantEmail = applicant.email!!
          val cruEmail = application.cruManagementArea!!.emailAddress!!

          emailAsserter.assertEmailsRequestedCount(4)
          assertPlacementRequestWithdrawnEmail(applicantEmail, placementRequest1)

          assertBookingWithdrawnEmail(applicantEmail, booking2NoArrival)
          assertBookingWithdrawnEmail(booking2NoArrival.premises.emailAddress!!, booking2NoArrival)
          assertBookingWithdrawnEmail(cruEmail, booking2NoArrival)
        }
      }
    }

    /**
     * For this test the applicant is also the case manager
     *
     * ```
     * | Entities                         | Withdrawn | Email PP | Email AP | Email CRU | Email Assessor |
     * | -------------------------------- | --------- | -------- | -------- | --------- | -------------- |
     * | Application                      | -         | -        | -        | -         | -              |
     * | -> Assessment                    | -         | -        | -        | -         | -              |
     * | -> Match request                 | YES       | YES      | -        | YES       | -              |
     * ```
     */
    @Test
    fun `Withdrawing a match request for original app dates without booking emails CRU`() {
      givenAUser { applicant, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val (application, assessment) = createApplicationAndAssessment(
            applicant = applicant,
            assignee = applicant,
            offenderDetails = offenderDetails,
            assessmentAllocatedTo = applicant,
          )

          val placementRequest = createPlacementRequest(application)

          assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.STARTED)

          withdrawPlacementRequest(
            placementRequest,
            WithdrawPlacementRequestReason.duplicatePlacementRequest,
            jwt,
          )

          assertApplicationStatus(application, ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST)

          assertApplicationNotWithdrawn(application)
          assertAssessmentNotWithdrawn(assessment)

          assertPlacementRequestWithdrawn(placementRequest, PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST)

          val applicantEmail = applicant.email!!
          val cruEmail = application.cruManagementArea!!.emailAddress!!

          emailAsserter.assertEmailsRequestedCount(2)
          assertPlacementRequestWithdrawnEmail(applicantEmail, placementRequest)
          assertMatchRequestWithdrawnEmail(cruEmail, placementRequest)
        }
      }
    }

    /**
     * For this test the applicant is also the case manager
     *
     * ```
     * | Entities                         | Withdrawn | Email PP | Email AP | Email CRU | Email Assessor |
     * | -------------------------------- | --------- | -------- | -------- | --------- | -------------- |
     * | Application                      | -         | -        | -        | -         | -              |
     * | -> Assessment                    | -         | -        | -        | -         | -              |
     * | -> Match request                 | YES       | YES      | -        | -         | -              |
     * | ---> Booking arrival pending     | YES       | YES      | YES      | YES       | -              |
     * ```
     */
    @Test
    fun `Withdrawing a match request for original app dates cascades to applicable booking entity and updates the application status`() {
      givenAUser { applicant, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val (application, assessment) = createApplicationAndAssessment(
            applicant = applicant,
            assignee = applicant,
            offenderDetails = offenderDetails,
            assessmentAllocatedTo = applicant,
          )

          val placementRequest = createPlacementRequest(application)
          val bookingNoArrival = createBooking(
            application = application,
            hasArrivalInCas1 = false,
            hasArrivalInDelius = false,
            startDate = nowPlusDays(1),
            endDate = nowPlusDays(6),
          )
          addBookingToPlacementRequest(placementRequest, bookingNoArrival)

          assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.STARTED)

          withdrawPlacementRequest(
            placementRequest,
            WithdrawPlacementRequestReason.duplicatePlacementRequest,
            jwt,
          )

          assertApplicationStatus(application, ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST)

          assertApplicationNotWithdrawn(application)
          assertAssessmentNotWithdrawn(assessment)

          assertPlacementRequestWithdrawn(placementRequest, PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST)
          assertBookingWithdrawn(bookingNoArrival, "Related placement request withdrawn")

          val applicantEmail = applicant.email!!
          val cruEmail = application.cruManagementArea!!.emailAddress!!
          val apEmail = bookingNoArrival.premises.emailAddress!!

          emailAsserter.assertEmailsRequestedCount(4)
          assertPlacementRequestWithdrawnEmail(applicantEmail, placementRequest)
          assertBookingWithdrawnEmail(applicantEmail, bookingNoArrival)
          assertBookingWithdrawnEmail(apEmail, bookingNoArrival)
          assertBookingWithdrawnEmail(cruEmail, bookingNoArrival)
        }
      }
    }

    /**
     * For this test the applicant is also the case manager
     *
     * ```
     * | Entities                           | Withdrawn | Email PP | Email AP | Email CRU | Email Assessor |
     * | ---------------------------------- | --------- | -------- | -------- | --------- | -------------- |
     * | Application                        | -         | -        | -        | -         | -              |
     * | -> Assessment                      | -         | -        | -        | -         | -              |
     * | -> Match request                   | YES       | YES      | -        | -         | -              |
     * | ---> Space Booking arrival pending | YES       | YES      | YES      | YES       | -              |
     * ```
     */
    @Test
    fun `Withdrawing a match request for original app dates cascades to applicable space booking entity and updates the application status`() {
      givenAUser { applicant, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val (application, assessment) = createApplicationAndAssessment(
            applicant = applicant,
            assignee = applicant,
            offenderDetails = offenderDetails,
            assessmentAllocatedTo = applicant,
          )

          val placementRequest = createPlacementRequest(application)
          val spaceBookingNoArrival = createSpaceBooking(
            application = application,
            startDate = nowPlusDays(1),
            endDate = nowPlusDays(6),
            arrivalDate = null,
            placementRequest = placementRequest,
          )

          assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.STARTED)

          withdrawPlacementRequest(
            placementRequest,
            WithdrawPlacementRequestReason.duplicatePlacementRequest,
            jwt,
          )

          assertApplicationStatus(application, ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST)

          assertApplicationNotWithdrawn(application)
          assertAssessmentNotWithdrawn(assessment)

          assertPlacementRequestWithdrawn(placementRequest, PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST)
          assertSpaceBookingWithdrawn(spaceBookingNoArrival, "Related placement request withdrawn")

          val applicantEmail = applicant.email!!
          val cruEmail = application.cruManagementArea!!.emailAddress!!
          val apEmail = spaceBookingNoArrival.premises.emailAddress!!

          emailAsserter.assertEmailsRequestedCount(4)
          assertPlacementRequestWithdrawnEmail(applicantEmail, placementRequest)
          assertSpaceBookingWithdrawnEmail(applicantEmail, spaceBookingNoArrival)
          assertSpaceBookingWithdrawnEmail(apEmail, spaceBookingNoArrival)
          assertSpaceBookingWithdrawnEmail(cruEmail, spaceBookingNoArrival)
        }
      }
    }

    private fun assertAssessmentWithdrawnEmail(emailAddress: String) =
      emailAsserter.assertEmailRequested(
        emailAddress,
        notifyConfig.templates.assessmentWithdrawnV2,
      )

    private fun assertApplicationWithdrawnEmail(emailAddress: String, application: ApplicationEntity) =
      emailAsserter.assertEmailRequested(
        emailAddress,
        notifyConfig.templates.applicationWithdrawnV2,
        mapOf("crn" to application.crn),
      )

    private fun assertBookingWithdrawnEmail(emailAddress: String, booking: BookingEntity) =
      emailAsserter.assertEmailRequested(
        emailAddress,
        notifyConfig.templates.bookingWithdrawnV2,
        mapOf(
          "startDate" to booking.arrivalDate.toString(),
          "endDate" to booking.departureDate.toString(),
        ),
      )

    private fun assertSpaceBookingWithdrawnEmail(emailAddress: String, booking: Cas1SpaceBookingEntity) =
      emailAsserter.assertEmailRequested(
        emailAddress,
        notifyConfig.templates.bookingWithdrawnV2,
        mapOf(
          "startDate" to booking.canonicalArrivalDate.toString(),
          "endDate" to booking.canonicalDepartureDate.toString(),
        ),
      )

    private fun assertPlacementRequestWithdrawnEmail(emailAddress: String, placementApplication: PlacementApplicationEntity) =
      emailAsserter.assertEmailRequested(
        emailAddress,
        notifyConfig.templates.placementRequestWithdrawnV2,
        mapOf("startDate" to placementApplication.placementDates[0].expectedArrival.toString()),
      )

    private fun assertPlacementRequestWithdrawnEmail(emailAddress: String, placementRequest: PlacementRequestEntity) =
      emailAsserter.assertEmailRequested(
        emailAddress,
        notifyConfig.templates.placementRequestWithdrawnV2,
        mapOf("startDate" to placementRequest.expectedArrival.toString()),
      )

    private fun assertMatchRequestWithdrawnEmail(emailAddress: String, placementRequest: PlacementRequestEntity) =
      emailAsserter.assertEmailRequested(
        emailAddress,
        notifyConfig.templates.matchRequestWithdrawnV2,
        mapOf("startDate" to placementRequest.expectedArrival.toString()),
      )
  }

  private fun addBookingToPlacementRequest(placementRequest: PlacementRequestEntity, booking: BookingEntity) {
    placementRequest.booking = booking
    placementRequestRepository.save(placementRequest)
  }

  private fun withdrawApplication(application: ApprovedPremisesApplicationEntity, jwt: String) {
    webTestClient.post()
      .uri("/applications/${application.id}/withdrawal")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewWithdrawal(
          reason = WithdrawalReason.duplicateApplication,
        ),
      )
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun withdrawPlacementApplication(
    placementApplication: PlacementApplicationEntity,
    reason: WithdrawPlacementRequestReason,
    jwt: String,
  ) {
    webTestClient.post()
      .uri("/placement-applications/${placementApplication.id}/withdraw")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(WithdrawPlacementApplication(reason))
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun withdrawPlacementRequest(
    placementRequest: PlacementRequestEntity,
    reason: WithdrawPlacementRequestReason,
    jwt: String,
  ) {
    webTestClient.post()
      .uri("/placement-requests/${placementRequest.id}/withdrawal")
      .bodyValue(WithdrawPlacementRequest(reason))
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun assertAssessmentNotWithdrawn(assessment: AssessmentEntity) {
    val updatedAssessment = approvedPremisesAssessmentRepository.findByIdOrNull(assessment.id)!!
    assertThat(updatedAssessment.isWithdrawn).isFalse
  }

  private fun assertAssessmentWithdrawn(assessment: AssessmentEntity) {
    val updatedAssessment = approvedPremisesAssessmentRepository.findByIdOrNull(assessment.id)!!
    assertThat(updatedAssessment.isWithdrawn).isTrue
  }

  private fun assertPlacementApplicationWithdrawn(
    placementApplication: PlacementApplicationEntity,
    reason: PlacementApplicationWithdrawalReason,
  ) {
    val updatedPlacementApplication = placementApplicationRepository.findByIdOrNull(placementApplication.id)!!
    assertThat(updatedPlacementApplication.withdrawalReason).isEqualTo(reason)
    assertThat(updatedPlacementApplication.isWithdrawn).isTrue()
  }

  private fun assertPlacementApplicationNotWithdrawn(placementApplication: PlacementApplicationEntity) {
    val updatedPlacementApplication = placementApplicationRepository.findByIdOrNull(placementApplication.id)!!
    assertThat(updatedPlacementApplication.isWithdrawn).isFalse()
    assertThat(updatedPlacementApplication.withdrawalReason).isNull()
  }

  private fun assertApplicationStatus(application: ApprovedPremisesApplicationEntity, expectedStatus: ApprovedPremisesApplicationStatus) {
    val updatedApplication = approvedPremisesApplicationRepository.findByIdOrNull(application.id)!!
    assertThat(updatedApplication.status).isEqualTo(expectedStatus)
  }

  private fun assertApplicationNotWithdrawn(application: ApprovedPremisesApplicationEntity) {
    val updatedApplication = approvedPremisesApplicationRepository.findByIdOrNull(application.id)!!
    assertThat(updatedApplication.isWithdrawn).isFalse
    assertThat(updatedApplication.status).isNotEqualTo(ApprovedPremisesApplicationStatus.WITHDRAWN)
  }

  private fun assertApplicationWithdrawn(application: ApprovedPremisesApplicationEntity) {
    val updatedApplication = approvedPremisesApplicationRepository.findByIdOrNull(application.id)!!
    assertThat(updatedApplication.isWithdrawn).isTrue
    assertThat(updatedApplication.status).isEqualTo(ApprovedPremisesApplicationStatus.WITHDRAWN)
  }

  private fun assertPlacementRequestWithdrawn(placementRequest: PlacementRequestEntity, reason: PlacementRequestWithdrawalReason) {
    val updatedPlacementRequest = placementRequestRepository.findByIdOrNull(placementRequest.id)!!
    assertThat(updatedPlacementRequest.isWithdrawn).isEqualTo(true)
    assertThat(updatedPlacementRequest.withdrawalReason).isEqualTo(reason)
  }

  private fun assertPlacementRequestNotWithdrawn(placementRequest: PlacementRequestEntity) {
    val updatedPlacementRequest = placementRequestRepository.findByIdOrNull(placementRequest.id)!!
    assertThat(updatedPlacementRequest.isWithdrawn).isEqualTo(false)
  }

  private fun assertBookingWithdrawn(booking: BookingEntity, cancellationReason: String) {
    val updatedBooking = bookingRepository.findByIdOrNull(booking.id)!!
    assertThat(updatedBooking.isCancelled).isTrue()
    assertThat(updatedBooking.cancellation!!.reason.name).isEqualTo(cancellationReason)
  }

  private fun assertBookingNotWithdrawn(booking: BookingEntity) {
    val updatedBooking2WithArrival = bookingRepository.findByIdOrNull(booking.id)!!
    assertThat(updatedBooking2WithArrival.isCancelled).isFalse()
  }

  private fun assertSpaceBookingWithdrawn(spaceBooking: Cas1SpaceBookingEntity, cancellationReason: String) {
    val updatedBooking = cas1SpaceBookingRepository.findByIdOrNull(spaceBooking.id)!!
    assertThat(updatedBooking.cancellationOccurredAt).isNotNull()
    assertThat(updatedBooking.cancellationReason!!.name).isEqualTo(cancellationReason)
  }

  @SuppressWarnings("LongParameterList")
  private fun createApplicationAndAssessment(
    applicant: UserEntity,
    assignee: UserEntity,
    offenderDetails: OffenderDetailSummary,
    assessmentSubmitted: Boolean = true,
    assessmentAllocatedTo: UserEntity? = null,
    caseManager: Cas1ApplicationUserDetailsEntity? = null,
  ): Pair<ApprovedPremisesApplicationEntity, AssessmentEntity> {
    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
      withAddedAt(OffsetDateTime.now())
    }

    val apArea = givenAnApArea()

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(applicant)
      withApplicationSchema(applicationSchema)
      withSubmittedAt(OffsetDateTime.now())
      withApArea(apArea)
      withCruManagementArea(givenACas1CruManagementArea())
      withReleaseType("licence")
      withCaseManagerUserDetails(caseManager)
      withCaseManagerIsNotApplicant(caseManager != null)
    }

    val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
      withAllocatedToUser(assignee)
      withApplication(application)
      withAssessmentSchema(assessmentSchema)
      withSubmittedAt(if (assessmentSubmitted) OffsetDateTime.now() else null)
      withAllocatedToUser(assessmentAllocatedTo)
    }

    assessment.schemaUpToDate = true
    application.assessments.add(assessment)

    return Pair(application, assessment)
  }

  private fun produceAndPersistBasicApplication(
    crn: String,
    userEntity: UserEntity,
    managingTeamCode: String,
  ): ApplicationEntity {
    val jsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
      withSchema(
        """
        {
          "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
          "${"\$id"}": "https://example.com/product.schema.json",
          "title": "Thing",
          "description": "A thing",
          "type": "object",
          "properties": {
            "thingId": {
              "description": "The unique identifier for a thing",
              "type": "integer"
            }
          },
          "required": [ "thingId" ]
        }
        """,
      )
    }

    val application =
      approvedPremisesApplicationEntityFactory.produceAndPersist {
        withApplicationSchema(jsonSchema)
        withCrn(crn)
        withCreatedByUser(userEntity)
        withData(
          """
          {
             "thingId": 123
          }
          """,
        )
      }

    application.teamCodes += applicationTeamCodeRepository.save(
      ApplicationTeamCodeEntity(
        id = UUID.randomUUID(),
        application = application,
        teamCode = managingTeamCode,
      ),
    )

    return application
  }

  private data class DateSpan(val start: LocalDate, val duration: Int)

  private fun createPlacementApplication(
    application: ApprovedPremisesApplicationEntity,
    dateSpan: DateSpan? = null,
    dateSpans: List<DateSpan> = emptyList(),
    isSubmitted: Boolean = true,
    reallocatedAt: OffsetDateTime? = null,
    decision: PlacementApplicationDecision? = PlacementApplicationDecision.ACCEPTED,
    allocatedTo: UserEntity? = null,
    isWithdrawn: Boolean = false,
    createdBy: UserEntity? = null,
  ): PlacementApplicationEntity {
    val placementApplication = placementApplicationFactory.produceAndPersist {
      withCreatedByUser(createdBy ?: application.createdByUser)
      withApplication(application)
      withSchemaVersion(
        approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        },
      )
      withSubmittedAt(if (isSubmitted) OffsetDateTime.now() else null)
      withDecision(decision)
      withPlacementType(PlacementType.ADDITIONAL_PLACEMENT)
      withReallocatedAt(reallocatedAt)
      withAllocatedToUser(allocatedTo)
      withIsWithdrawn(isWithdrawn)
    }

    if (isSubmitted) {
      val dates = (listOfNotNull(dateSpan) + dateSpans).map {
        placementDateFactory.produceAndPersist {
          withPlacementApplication(placementApplication)
          withExpectedArrival(it.start)
          withDuration(it.duration)
        }
      }
      placementApplication.placementDates.addAll(dates)
    }

    return placementApplication
  }

  private fun createPlacementRequest(
    application: ApprovedPremisesApplicationEntity,
    reallocatedAt: OffsetDateTime? = null,
    isWithdrawn: Boolean = false,
    placementApplication: PlacementApplicationEntity? = null,
  ) =
    placementRequestFactory.produceAndPersist {
      val assessment = application.assessments[0] as ApprovedPremisesAssessmentEntity

      val placementRequirements = placementRequirementsFactory.produceAndPersist {
        withApplication(application)
        withAssessment(assessment)
        withPostcodeDistrict(postCodeDistrictFactory.produceAndPersist())
        withDesirableCriteria(
          characteristicEntityFactory.produceAndPersistMultiple(5),
        )
        withEssentialCriteria(
          characteristicEntityFactory.produceAndPersistMultiple(3),
        )
      }

      withAllocatedToUser(application.createdByUser)
      withApplication(application)
      withAssessment(assessment)
      withPlacementRequirements(placementRequirements)
      withReallocatedAt(reallocatedAt)
      withIsWithdrawn(isWithdrawn)
      withPlacementApplication(placementApplication)
    }

  private fun createBooking(
    application: ApprovedPremisesApplicationEntity,
    startDate: LocalDate,
    endDate: LocalDate,
    hasArrivalInCas1: Boolean = false,
    hasArrivalInDelius: Boolean = false,
    adhoc: Boolean? = false,
  ): BookingEntity {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion { givenAProbationRegion() }
    }

    val booking = bookingEntityFactory.produceAndPersist {
      withApplication(application)
      withPremises(premises)
      withCrn(application.crn)
      withServiceName(ServiceName.approvedPremises)
      withArrivalDate(startDate)
      withDepartureDate(endDate)
      withAdhoc(adhoc)
    }

    if (hasArrivalInCas1) {
      arrivalEntityFactory.produceAndPersist {
        withBooking(booking)
      }
    }

    apDeliusContextMockSuccessfulGetReferralDetails(
      crn = booking.crn,
      bookingId = booking.id.toString(),
      arrivedAt = if (hasArrivalInDelius) {
        ZonedDateTime.now()
      } else {
        null
      },
      departedAt = null,
    )

    return booking
  }

  private fun createSpaceBooking(
    application: ApprovedPremisesApplicationEntity,
    startDate: LocalDate,
    endDate: LocalDate,
    arrivalDate: LocalDateTime? = null,
    placementRequest: PlacementRequestEntity,
  ): Cas1SpaceBookingEntity {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion { givenAProbationRegion() }
    }

    val spaceBooking = cas1SpaceBookingEntityFactory.produceAndPersist {
      withApplication(application)
      withPlacementRequest(placementRequest)
      withCreatedBy(application.createdByUser)
      withPremises(premises)
      withCrn(application.crn)
      withExpectedArrivalDate(startDate)
      withExpectedDepartureDate(endDate)
      withActualArrivalDate(arrivalDate?.toLocalDate())
      withActualArrivalTime(arrivalDate?.toLocalTime())
    }

    return spaceBooking
  }

  private fun now() = LocalDate.now()

  private fun nowPlusDays(days: Long) = LocalDate.now().plusDays(days)

  private fun toDatePeriod(start: LocalDate, duration: Int) = DatePeriod(start, start.plusDays(duration.toLong()))

  fun toWithdrawable(application: ApplicationEntity) = Withdrawable(
    application.id,
    WithdrawableType.application,
    emptyList(),
  )

  fun toWithdrawable(placementRequest: PlacementRequestEntity) = Withdrawable(
    placementRequest.id,
    WithdrawableType.placementRequest,
    listOf(toDatePeriod(placementRequest.expectedArrival, placementRequest.duration)),
  )

  fun toWithdrawable(placementApplication: PlacementApplicationEntity) =
    Withdrawable(
      placementApplication.id,
      WithdrawableType.placementApplication,
      placementApplication.placementDates.map { toDatePeriod(it.expectedArrival, it.duration) },
    )

  fun toWithdrawable(booking: BookingEntity) =
    Withdrawable(
      booking.id,
      WithdrawableType.booking,
      listOf(DatePeriod(booking.arrivalDate, booking.departureDate)),
    )

  fun toWithdrawable(spaceBooking: Cas1SpaceBookingEntity) =
    Withdrawable(
      spaceBooking.id,
      WithdrawableType.spaceBooking,
      listOf(DatePeriod(spaceBooking.canonicalArrivalDate, spaceBooking.canonicalDepartureDate)),
    )
}
