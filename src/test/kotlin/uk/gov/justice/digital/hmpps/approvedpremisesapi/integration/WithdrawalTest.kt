package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawableType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.jsonForObject
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

/**
 * This test class tests common withdrawal functionality that spans multiple entity types and accordingly,
 * multiple API endpoints. Specifically:
 *
 * 1. Getting a list of withdrawable elements
 * 2. Cascading withdrawals
 *
 * When considering withdrawals, an application can be considered as a tree or elements:
 *
 * ```
 * application
 *  - assessment
 *    - placement request
 *      - booking
 *    - placement application
 *      - placement request
 *        - booking
 * ```
 *
 * Withdrawals should cascade down the tree
 *
 * Note : The general functionality of each entities' withdrawal endpoint is tested in the corresponding API Test class
 */
class WithdrawalTest : IntegrationTestBase() {

  @Nested
  inner class GetWithdrawables {

    @Test
    fun `Get withdrawables for an application returns empty list if no associated withdrawables`() {
      `Given a User` { applicationCreator, _ ->
        `Given a User` { _, jwt ->
          `Given an Offender` { offenderDetails, _ ->
            val application = produceAndPersistBasicApplication(offenderDetails.otherIds.crn, applicationCreator, "TEAM")

            webTestClient.get()
              .uri("/applications/${application.id}/withdrawables")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json("[]")
          }
        }
      }
    }

    @Test
    fun `Get withdrawables for an application returns the application if user is application creator`() {
      `Given a User` { applicationCreator, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val application = produceAndPersistBasicApplication(offenderDetails.otherIds.crn, applicationCreator, "TEAM")

          val expected = listOf(
            Withdrawable(
              application.id,
              WithdrawableType.application,
              emptyList(),
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
            .jsonForObject(expected)
        }
      }
    }

    @Test
    fun `Get withdrawables for an application returns withdrawable placement requests for application creator`() {
      `Given a User` { applicant, jwt ->
        `Given a User` { allocatedTo, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val (application, _) = createApplicationAndAssessment(applicant, allocatedTo, offenderDetails)

            val placementRequest1 = createPlacementRequest(application)
            val placementRequest2 = createPlacementRequest(application)

            createPlacementRequest(application) {
              withReallocatedAt(OffsetDateTime.now())
            }

            val placementRequestWithBooking = createPlacementRequest(application) {
              val premises = approvedPremisesEntityFactory.produceAndPersist {
                withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
                withYieldedProbationRegion { probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } } }
              }

              withBooking(
                bookingEntityFactory.produceAndPersist {
                  withPremises(premises)
                },
              )
            }

            createPlacementRequest(application) {
              withIsWithdrawn(true)
            }

            val expected = listOf(
              Withdrawable(
                application.id,
                WithdrawableType.application,
                emptyList(),
              ),
              Withdrawable(
                placementRequest1.id,
                WithdrawableType.placementRequest,
                listOf(datePeriodForDuration(placementRequest1.expectedArrival, placementRequest1.duration)),
              ),
              Withdrawable(
                placementRequest2.id,
                WithdrawableType.placementRequest,
                listOf(datePeriodForDuration(placementRequest2.expectedArrival, placementRequest2.duration)),
              ),
              Withdrawable(
                placementRequestWithBooking.id,
                WithdrawableType.placementRequest,
                listOf(datePeriodForDuration(placementRequestWithBooking.expectedArrival, placementRequestWithBooking.duration)),
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
              .jsonForObject(expected)
          }
        }
      }
    }

    @Test
    fun `Get withdrawables for an application returns withdrawable placement applications`() {
      `Given a User` { applicant, jwt ->
        `Given a User` { allocatedTo, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val (application, _) = createApplicationAndAssessment(applicant, allocatedTo, offenderDetails)

            val submittedApplication1ExpectedArrival1 = LocalDate.now().plusDays(1)
            val submittedApplication1Duration1 = 5
            val submittedApplication1ExpectedArrival2 = LocalDate.now().plusDays(10)
            val submittedApplication1Duration2 = 10

            val submittedPlacementApplication1 = createPlacementApplication(
              application,
              listOf(
                submittedApplication1ExpectedArrival1 to submittedApplication1Duration1,
                submittedApplication1ExpectedArrival2 to submittedApplication1Duration2,
              ),
            )

            val submittedApplication2ExpectedArrival = LocalDate.now().plusDays(50)
            val submittedApplication2Duration = 6
            val submittedPlacementApplication2 = createPlacementApplication(
              application,
              listOf(submittedApplication2ExpectedArrival to submittedApplication2Duration),
            )

            val unsubmittedApplicationExpectedArrival = LocalDate.now().plusDays(50)
            val unsubmittedApplicationDuration = 6
            val unsubmittedPlacementApplication = createPlacementApplication(
              application,
              listOf(unsubmittedApplicationExpectedArrival to unsubmittedApplicationDuration),
            ) {
              withSubmittedAt(null)
            }

            createPlacementApplication(application, listOf(LocalDate.now() to 2)) {
              withReallocatedAt(OffsetDateTime.now())
            }

            val applicationWithAcceptedDecisionExpectedArrival = LocalDate.now().plusDays(50)
            val applicationWithAcceptedDecisionDuration = 6
            val applicationWithAcceptedDecision = createPlacementApplication(
              application,
              listOf(applicationWithAcceptedDecisionExpectedArrival to applicationWithAcceptedDecisionDuration),
            ) {
              withDecision(PlacementApplicationDecision.ACCEPTED)
            }

            createPlacementApplication(application, listOf(LocalDate.now() to 2)) {
              withDecision(PlacementApplicationDecision.WITHDRAW)
            }

            createPlacementApplication(application, listOf(LocalDate.now() to 2)) {
              withDecision(PlacementApplicationDecision.WITHDRAWN_BY_PP)
            }

            val applicationWithRejectedDecisionExpectedArrival = LocalDate.now().plusDays(50)
            val applicationWithRejectedDecisionDuration = 6
            val applicationWithRejectedDecision = createPlacementApplication(
              application,
              listOf(applicationWithRejectedDecisionExpectedArrival to applicationWithRejectedDecisionDuration),
            ) {
              withDecision(PlacementApplicationDecision.REJECTED)
            }

            val expected = listOf(
              Withdrawable(
                application.id,
                WithdrawableType.application,
                emptyList(),
              ),
              Withdrawable(
                submittedPlacementApplication1.id,
                WithdrawableType.placementApplication,
                listOf(
                  datePeriodForDuration(submittedApplication1ExpectedArrival1, submittedApplication1Duration1),
                  datePeriodForDuration(submittedApplication1ExpectedArrival2, submittedApplication1Duration2),
                ),
              ),
              Withdrawable(
                submittedPlacementApplication2.id,
                WithdrawableType.placementApplication,
                listOf(datePeriodForDuration(submittedApplication2ExpectedArrival, submittedApplication2Duration)),
              ),
              Withdrawable(
                unsubmittedPlacementApplication.id,
                WithdrawableType.placementApplication,
                listOf(datePeriodForDuration(unsubmittedApplicationExpectedArrival, unsubmittedApplicationDuration)),
              ),
              Withdrawable(
                applicationWithAcceptedDecision.id,
                WithdrawableType.placementApplication,
                listOf(datePeriodForDuration(applicationWithAcceptedDecisionExpectedArrival, applicationWithAcceptedDecisionDuration)),
              ),
              Withdrawable(
                applicationWithRejectedDecision.id,
                WithdrawableType.placementApplication,
                listOf(datePeriodForDuration(applicationWithRejectedDecisionExpectedArrival, applicationWithRejectedDecisionDuration)),
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
              .jsonForObject(expected)
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_WORKFLOW_MANAGER"])
    fun `Get withdrawables for an application returns withdrawable bookings when a user can manage bookings`(role: UserRole) {
      `Given a User` { applicant, _ ->
        `Given a User`(roles = listOf(role)) { allocatedTo, jwt ->
          `Given an Offender` { offenderDetails, _ ->

            val (application, _) = createApplicationAndAssessment(applicant, allocatedTo, offenderDetails)

            val booking1expectedArrival = LocalDate.now().plusDays(1)
            val booking1expectedDeparture = LocalDate.now().plusDays(6)
            val booking1 = createBooking(
              application,
              booking1expectedArrival,
              booking1expectedDeparture,
            )

            val booking2expectedArrival = LocalDate.now().plusDays(1)
            val booking2expectedDeparture = LocalDate.now().plusDays(6)
            val booking2 = createBooking(
              application,
              booking2expectedArrival,
              booking2expectedDeparture,
            )

            val cancelledBooking = createBooking(
              application,
              LocalDate.now(),
              LocalDate.now().plusDays(1),
            )
            cancellationEntityFactory.produceAndPersist {
              withBooking(cancelledBooking)
              withReason(cancellationReasonEntityFactory.produceAndPersist())
            }

            val bookingWithArrival = createBooking(
              application,
              LocalDate.now(),
              LocalDate.now().plusDays(1),
            )
            arrivalEntityFactory.produceAndPersist() {
              withBooking(bookingWithArrival)
            }

            val expected = listOfNotNull(
              if (role == UserRole.CAS1_WORKFLOW_MANAGER) {
                Withdrawable(
                  application.id,
                  WithdrawableType.application,
                  emptyList(),
                )
              } else {
                null
              },
              Withdrawable(
                booking1.id,
                WithdrawableType.booking,
                listOf(DatePeriod(booking1expectedArrival, booking1expectedDeparture)),
              ),
              Withdrawable(
                booking2.id,
                WithdrawableType.booking,
                listOf(DatePeriod(booking2expectedArrival, booking2expectedDeparture)),
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
              .jsonForObject(expected)
          }
        }
      }
    }

    @Test
    fun `Get withdrawables for all possible types when a user can manage bookings`() {
      `Given a User` { applicant, _ ->
        `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { allocatedTo, jwt ->
          `Given an Offender` { offenderDetails, _ ->

            val (application, _) = createApplicationAndAssessment(applicant, allocatedTo, offenderDetails)

            val booking1ExpectedArrival = LocalDate.now().plusDays(1)
            val booking1ExpectedDeparture = LocalDate.now().plusDays(6)
            val booking1 = createBooking(
              application,
              booking1ExpectedArrival,
              booking1ExpectedDeparture,
            )

            val placementApplicationExpectedArrival = LocalDate.now().plusDays(1)
            val placementApplicationDuration = 5

            val placementApplication = createPlacementApplication(
              application,
              listOf(placementApplicationExpectedArrival to placementApplicationDuration),
            )

            val placementRequest = createPlacementRequest(application)

            val expected = listOf(
              Withdrawable(
                application.id,
                WithdrawableType.application,
                emptyList(),
              ),
              Withdrawable(
                booking1.id,
                WithdrawableType.booking,
                listOf(DatePeriod(booking1ExpectedArrival, booking1ExpectedDeparture)),
              ),
              Withdrawable(
                placementApplication.id,
                WithdrawableType.placementApplication,
                listOf(datePeriodForDuration(placementApplicationExpectedArrival, placementApplicationDuration)),
              ),
              Withdrawable(
                placementRequest.id,
                WithdrawableType.placementRequest,
                listOf(datePeriodForDuration(placementRequest.expectedArrival, placementRequest.duration)),
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
              .jsonForObject(expected)
          }
        }
      }
    }

    @Test
    fun `Get withdrawables for all possible types filters out bookings when a user cannot manage bookings`() {
      `Given a User` { applicant, jwt ->
        `Given an Offender` { offenderDetails, _ ->

          val (application, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)

          val booking1ExpectedArrival = LocalDate.now().plusDays(1)
          val booking1ExpectedDeparture = LocalDate.now().plusDays(6)
          createBooking(
            application,
            booking1ExpectedArrival,
            booking1ExpectedDeparture,
          )

          val placementApplicationExpectedArrival = LocalDate.now().plusDays(1)
          val placementApplicationDuration = 5

          val placementApplication = createPlacementApplication(
            application,
            listOf(placementApplicationExpectedArrival to placementApplicationDuration),
          )

          val placementRequest = createPlacementRequest(application)

          val expected = listOf(
            Withdrawable(
              application.id,
              WithdrawableType.application,
              emptyList(),
            ),
            Withdrawable(
              placementApplication.id,
              WithdrawableType.placementApplication,
              listOf(datePeriodForDuration(placementApplicationExpectedArrival, placementApplicationDuration)),
            ),
            Withdrawable(
              placementRequest.id,
              WithdrawableType.placementRequest,
              listOf(datePeriodForDuration(placementRequest.expectedArrival, placementRequest.duration)),
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
            .jsonForObject(expected)
        }
      }
    }
  }

  @Nested
  inner class WithdrawalCascading {

    /**
     * ```
     * elements                         withdrawn
     * ------------------------------------------
     * application                      YES
     *  - assessment                    YES
     *    - placement application       YES
     *      - placement request 1       YES
     *        - booking 1 no arrival    YES
     *      - placement request 2       YES
     *    - placement request 3         YES
     *      - booking 2 has arrival     NO
     * ```
     */
    @Test
    fun `Withdrawing an application cascades to all possible entities`() {
      `Given a User` { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val (application, assessment) = createApplicationAndAssessment(user, user, offenderDetails)

          val placementApplication = createPlacementApplication(application)
          val placementRequest1 = createPlacementRequest(application) {
            withPlacementApplication(placementApplication)
          }
          val booking1NoArrival = createBooking(
            application = application,
            hasArrival = false,
            startDate = LocalDate.now().plusDays(1),
            endDate = LocalDate.now().plusDays(6),
          )
          addBookingToPlacementRequest(placementRequest1, booking1NoArrival)

          val placementRequest2 = createPlacementRequest(application) {
            withPlacementApplication(placementApplication)
          }

          val placementRequest3 = createPlacementRequest(application)
          val booking2HasArrival = createBooking(
            application = application,
            hasArrival = true,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(1),
          )
          addBookingToPlacementRequest(placementRequest3, booking2HasArrival)

          withdrawApplication(application, jwt)

          assertApplicationWithdrawn(application)
          assertAssessmentWithdrawn(assessment)

          assertPlacementApplicationWithdrawn(
            placementApplication,
            PlacementApplicationDecision.WITHDRAWN_BY_PP,
            PlacementApplicationWithdrawalReason.WITHDRAWN_BY_PP,
          )
          assertPlacementRequestWithdrawn(placementRequest1, PlacementRequestWithdrawalReason.WITHDRAWN_BY_PP)
          assertBookingWithdrawn(booking1NoArrival, "The probation practitioner requested it")
          assertPlacementRequestWithdrawn(placementRequest2, PlacementRequestWithdrawalReason.WITHDRAWN_BY_PP)

          assertPlacementRequestWithdrawn(placementRequest3, PlacementRequestWithdrawalReason.WITHDRAWN_BY_PP)
          assertBookingNotWithdrawn(booking2HasArrival)
        }
      }
    }

    /**
     * ```
     * elements                         withdrawn
     * ------------------------------------------
     * application                      NO
     *  - assessment                    NO
     *    - placement application       YES
     *      - placement request 1       YES
     *        - booking 1 no arrival    YES
     *      - placement request 2       YES
     *        - booking 2 has arrival   NO
     * ```
     */
    @Test
    fun `Withdrawing a placement application cascades to applicable placement requests and bookings entities`() {
      `Given a User` { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val (application, assessment) = createApplicationAndAssessment(user, user, offenderDetails)

          val placementApplication = createPlacementApplication(application)
          val placementRequest1 = createPlacementRequest(application) {
            withPlacementApplication(placementApplication)
          }
          val booking1NoArrival = createBooking(
            application = application,
            hasArrival = false,
            startDate = LocalDate.now().plusDays(1),
            endDate = LocalDate.now().plusDays(6),
          )
          addBookingToPlacementRequest(placementRequest1, booking1NoArrival)

          val placementRequest2 = createPlacementRequest(application) {
            withPlacementApplication(placementApplication)
          }
          val booking2HasArrival = createBooking(
            application = application,
            hasArrival = true,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(1),
          )
          addBookingToPlacementRequest(placementRequest2, booking2HasArrival)

          withdrawPlacementApplication(
            placementApplication,
            WithdrawPlacementRequestReason.duplicatePlacementRequest,
            jwt
          )

          assertApplicationNotWithdrawn(application)
          assertAssessmentNotWithdrawn(assessment)

          assertPlacementApplicationWithdrawn(
            placementApplication,
            PlacementApplicationDecision.WITHDRAWN_BY_PP,
            PlacementApplicationWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST,
          )

          assertPlacementRequestWithdrawn(placementRequest1, PlacementRequestWithdrawalReason.WITHDRAWN_BY_PP)
          assertBookingWithdrawn(booking1NoArrival, "The probation practitioner requested it")

          assertPlacementRequestWithdrawn(placementRequest2, PlacementRequestWithdrawalReason.WITHDRAWN_BY_PP)
          assertBookingNotWithdrawn(booking2HasArrival)
        }
      }
    }

    /**
     * ```
     * elements                         withdrawn
     * ------------------------------------------
     * application                      NO
     *  - assessment                    NO
     *    - placement request 1         YES
     *      - booking 1 no arrival      YES
     * ```
     */
    @Test
    fun `Withdrawing a placement request cascades to booking with arrival`() {
      `Given a User` { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val (application, assessment) = createApplicationAndAssessment(user, user, offenderDetails)

          val placementRequest = createPlacementRequest(application)
          val bookingNoArrival = createBooking(
            application = application,
            hasArrival = false,
            startDate = LocalDate.now().plusDays(1),
            endDate = LocalDate.now().plusDays(6),
          )
          addBookingToPlacementRequest(placementRequest, bookingNoArrival)

          withdrawPlacementRequest(
            placementRequest,
            WithdrawPlacementRequestReason.duplicatePlacementRequest,
            jwt
          )

          assertApplicationNotWithdrawn(application)
          assertAssessmentNotWithdrawn(assessment)

          assertPlacementRequestWithdrawn(placementRequest, PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST)
          assertBookingWithdrawn(bookingNoArrival, "The probation practitioner requested it")
        }
      }
    }

    /**
     * ```
     * elements                         withdrawn
     * ------------------------------------------
     * application                      NO
     *  - assessment                    NO
     *    - placement request 1         YES
     *      - booking 1 with arrival    NO
     * ```
     */
    @Test
    fun `Withdrawing a placement request doesn't cascade to booking with arrival`() {
      `Given a User` { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val (application, assessment) = createApplicationAndAssessment(user, user, offenderDetails)

          val placementRequest = createPlacementRequest(application)
          val bookingNoArrival = createBooking(
            application = application,
            hasArrival = true,
            startDate = LocalDate.now().plusDays(1),
            endDate = LocalDate.now().plusDays(6),
          )
          addBookingToPlacementRequest(placementRequest, bookingNoArrival)

          withdrawPlacementRequest(
            placementRequest,
            WithdrawPlacementRequestReason.duplicatePlacementRequest,
            jwt
          )

          assertApplicationNotWithdrawn(application)
          assertAssessmentNotWithdrawn(assessment)

          assertPlacementRequestWithdrawn(placementRequest, PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST)
          assertBookingNotWithdrawn(bookingNoArrival)
        }
      }
    }

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

  private fun withdrawPlacementApplication(placementApplication: PlacementApplicationEntity,
                                           reason: WithdrawPlacementRequestReason,
                                           jwt: String) {
    webTestClient.post()
      .uri("/placement-applications/${placementApplication.id}/withdraw")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(WithdrawPlacementApplication(reason))
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun withdrawPlacementRequest(placementRequest: PlacementRequestEntity,
                                       reason: WithdrawPlacementRequestReason,
                                       jwt: String) {
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

  private fun assertPlacementApplicationWithdrawn(placementApplication: PlacementApplicationEntity,
                                                  decision: PlacementApplicationDecision,
                                                  reason: PlacementApplicationWithdrawalReason) {
    val updatedPlacementApplication = placementApplicationRepository.findByIdOrNull(placementApplication.id)!!
    assertThat(updatedPlacementApplication.decision).isEqualTo(decision)
    assertThat(updatedPlacementApplication.withdrawalReason).isEqualTo(reason)
  }


  private fun assertApplicationNotWithdrawn(application: ApprovedPremisesApplicationEntity) {
    val updatedApplication = approvedPremisesApplicationRepository.findByIdOrNull(application.id)!!
    assertThat(updatedApplication.isWithdrawn).isFalse
  }

  private fun assertApplicationWithdrawn(application: ApprovedPremisesApplicationEntity) {
    val updatedApplication = approvedPremisesApplicationRepository.findByIdOrNull(application.id)!!
    assertThat(updatedApplication.isWithdrawn).isTrue
  }

  private fun assertPlacementRequestWithdrawn(placementRequest: PlacementRequestEntity, reason: PlacementRequestWithdrawalReason) {
    val updatedPlacementRequest = placementRequestRepository.findByIdOrNull(placementRequest.id)!!
    assertThat(updatedPlacementRequest.isWithdrawn).isEqualTo(true)
    assertThat(updatedPlacementRequest.withdrawalReason).isEqualTo(reason)
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

  private fun createApplicationAndAssessment(
    applicant: UserEntity,
    assignee: UserEntity,
    offenderDetails: OffenderDetailSummary,
  ): Pair<ApprovedPremisesApplicationEntity, AssessmentEntity> {
    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
      withAddedAt(OffsetDateTime.now())
    }

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(applicant)
      withApplicationSchema(applicationSchema)
      withSubmittedAt(OffsetDateTime.now())
    }

    val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
      withAllocatedToUser(assignee)
      withApplication(application)
      withAssessmentSchema(assessmentSchema)
      withSubmittedAt(OffsetDateTime.now())
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

  private fun createPlacementApplication(
    application: ApprovedPremisesApplicationEntity,
    arrivalsToDuration: List<Pair<LocalDate, Int>> = emptyList(),
    configuration: (PlacementApplicationEntityFactory.() -> Unit)? = null,
  ): PlacementApplicationEntity {
    val placementApplication = placementApplicationFactory.produceAndPersist {
      withCreatedByUser(application.createdByUser)
      withAllocatedToUser(application.createdByUser)
      withApplication(application)
      withSchemaVersion(
        approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        },
      )
      withSubmittedAt(OffsetDateTime.now())
      withDecision(null)
      withPlacementType(PlacementType.ADDITIONAL_PLACEMENT)
      configuration?.invoke(this)
    }

    arrivalsToDuration.forEach { (start, duration) ->
      placementDateFactory.produceAndPersist {
        withPlacementApplication(placementApplication)
        withExpectedArrival(start)
        withDuration(duration)
      }
    }

    return placementApplication
  }

  private fun createPlacementRequest(
    application: ApprovedPremisesApplicationEntity,
    configuration: (PlacementRequestEntityFactory.() -> Unit)? = null,
  ) =
    placementRequestFactory.produceAndPersist {
      val assessment = application.assessments.get(0)

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
      configuration?.invoke(this)
    }

  private fun createBooking(
    application: ApprovedPremisesApplicationEntity,
    startDate: LocalDate,
    endDate: LocalDate,
    hasArrival: Boolean = false,
    configuration: (BookingEntityFactory.() -> Unit)? = null,
  ): BookingEntity {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion { probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } } }
    }

    val booking = bookingEntityFactory.produceAndPersist {
      withApplication(application)
      withPremises(premises)
      withCrn(application.crn)
      withServiceName(ServiceName.approvedPremises)
      withArrivalDate(startDate)
      withDepartureDate(endDate)
      configuration?.invoke(this)
    }

    if(hasArrival) {
      arrivalEntityFactory.produceAndPersist {
        withBooking(booking)
      }
    }

    return booking
  }

  private fun datePeriodForDuration(start: LocalDate, duration: Int) = DatePeriod(start, start.plusDays(duration.toLong()))
}
