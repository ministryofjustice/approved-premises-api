package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Appeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewAppeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventAssociatedUrl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventUrlType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.withinSeconds
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision as ApiAssessmentDecision

class AppealsTest : InitialiseDatabasePerClassTestBase() {
  @Test
  fun `Get appeal without JWT returns 401`() {
    webTestClient.get()
      .uri("/applications/${UUID.randomUUID()}/appeals/${UUID.randomUUID()}")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get appeal returns 404 when application could not be found`() {
    givenAUser(roles = listOf(UserRole.CAS1_APPEALS_MANAGER)) { _, jwt ->
      webTestClient.get()
        .uri("/applications/${UUID.randomUUID()}/appeals/${UUID.randomUUID()}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Get appeal returns 403 when application is not accessible to user`() {
    govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

    givenAUser { createdByUser, _ ->
      val offender = givenAnOffender(
        offenderDetailsConfigBlock = {
          withCurrentRestriction(true)
        },
      ).first
      givenAnAssessmentForApprovedPremises(
        allocatedToUser = createdByUser,
        createdByUser = createdByUser,
        crn = offender.otherIds.crn,
      ) { _, application ->
        givenAUser(roles = listOf(UserRole.CAS1_APPEALS_MANAGER)) { _, jwt ->
          webTestClient.get()
            .uri("/applications/${application.id}/appeals/${UUID.randomUUID()}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }
  }

  @Test
  fun `Get appeal returns 404 when appeal could not be found`() {
    givenAUser(roles = listOf(UserRole.CAS1_APPEALS_MANAGER)) { userEntity, jwt ->
      givenAnApplication(userEntity) { application ->
        webTestClient.get()
          .uri("/applications/${application.id}/appeals/${UUID.randomUUID()}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }

  @Test
  fun `Get appeal returns 200 with correct body`() {
    givenAUser(roles = listOf(UserRole.CAS1_APPEALS_MANAGER)) { userEntity, jwt ->
      givenAnAssessmentForApprovedPremises(userEntity, userEntity) { assessment, application ->
        val appeal = appealEntityFactory.produceAndPersist {
          withApplication(application)
          withAssessment(assessment as ApprovedPremisesAssessmentEntity)
          withCreatedBy(userEntity)
          withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
        }

        webTestClient.get()
          .uri("/applications/${application.id}/appeals/${appeal.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.appealDate").isEqualTo(appeal.appealDate.toString())
          .jsonPath("$.appealDetail").isEqualTo(appeal.appealDetail)
          .jsonPath("$.createdAt").isEqualTo(appeal.createdAt.toString())
          .jsonPath("$.applicationId").isEqualTo(application.id.toString())
          .jsonPath("$.decision").isEqualTo(appeal.decision)
          .jsonPath("$.decisionDetail").isEqualTo(appeal.decisionDetail)
          .jsonPath("$.assessmentId").isEqualTo(appeal.assessment.id.toString())
          .jsonPath("$.createdByUser.id").isEqualTo(appeal.createdBy.id.toString())
          .jsonPath("$.createdByUser.name").isEqualTo(appeal.createdBy.name)
      }
    }
  }

  @Test
  fun `Create new appeal without JWT returns 401`() {
    webTestClient.post()
      .uri("/applications/${UUID.randomUUID()}/appeals")
      .bodyValue(
        NewAppeal(
          appealDate = LocalDate.parse("2024-01-01"),
          appealDetail = "Some details about the appeal.",
          decision = AppealDecision.accepted,
          decisionDetail = "Some details about the decision.",
        ),
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Create new appeal returns 404 when application could not be found`() {
    givenAUser(roles = listOf(UserRole.CAS1_APPEALS_MANAGER)) { _, jwt ->
      webTestClient.post()
        .uri("/applications/${UUID.randomUUID()}/appeals")
        .bodyValue(
          NewAppeal(
            appealDate = LocalDate.parse("2024-01-01"),
            appealDetail = "Some details about the appeal.",
            decision = AppealDecision.accepted,
            decisionDetail = "Some details about the decision.",
          ),
        )
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Create new appeal returns 403 when application is not accessible to user`() {
    govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

    givenAUser { createdByUser, _ ->
      val offender = givenAnOffender(
        offenderDetailsConfigBlock = {
          withCurrentRestriction(true)
        },
      ).first
      givenAnAssessmentForApprovedPremises(
        allocatedToUser = createdByUser,
        createdByUser = createdByUser,
        crn = offender.otherIds.crn,
      ) { _, application ->
        givenAUser(roles = listOf(UserRole.CAS1_APPEALS_MANAGER)) { _, jwt ->
          webTestClient.post()
            .uri("/applications/${application.id}/appeals")
            .bodyValue(
              NewAppeal(
                appealDate = LocalDate.parse("2024-01-01"),
                appealDetail = "Some details about the appeal.",
                decision = AppealDecision.accepted,
                decisionDetail = "Some details about the decision.",
              ),
            )
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }
  }

  @Test
  fun `Create new appeal returns 403 when user does not have CAS1_APPEALS_MANAGER role`() {
    givenAUser { userEntity, jwt ->
      givenAnAssessmentForApprovedPremises(userEntity, userEntity) { _, application ->
        webTestClient.post()
          .uri("/applications/${application.id}/appeals")
          .bodyValue(
            NewAppeal(
              appealDate = LocalDate.parse("2024-01-01"),
              appealDetail = "Some details about the appeal.",
              decision = AppealDecision.accepted,
              decisionDetail = "Some details about the decision.",
            ),
          )
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }

  @Test
  fun `Create new appeal returns 400 when invalid data is provided`() {
    givenAUser(roles = listOf(UserRole.CAS1_APPEALS_MANAGER)) { userEntity, jwt ->
      givenAnAssessmentForApprovedPremises(userEntity, userEntity) { _, application ->
        webTestClient.post()
          .uri("/applications/${application.id}/appeals")
          .bodyValue(
            NewAppeal(
              appealDate = LocalDate.now().plusDays(1),
              appealDetail = "  ",
              decision = AppealDecision.rejected,
              decisionDetail = "\n",
            ),
          )
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isBadRequest
      }
    }
  }

  @Test
  fun `Create new appeal returns 409 when no assessment exists on the application`() {
    givenAUser(roles = listOf(UserRole.CAS1_APPEALS_MANAGER)) { userEntity, jwt ->
      givenAnApplication(userEntity) { application ->
        webTestClient.post()
          .uri("/applications/${application.id}/appeals")
          .bodyValue(
            NewAppeal(
              appealDate = LocalDate.parse("2024-01-01"),
              appealDetail = "Some details about the appeal.",
              decision = AppealDecision.accepted,
              decisionDetail = "Some details about the decision.",
            ),
          )
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isEqualTo(HttpStatus.CONFLICT)
          .expectBody()
          .jsonPath("$.detail").isEqualTo("An appeal cannot be created when the application does not have an assessment: ${application.id}")
      }
    }
  }

  @Test
  fun `Create new appeal returns 201 with correct body and Location header`() {
    givenAUser(roles = listOf(UserRole.CAS1_APPEALS_MANAGER)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        givenAnAssessmentForApprovedPremises(
          allocatedToUser = userEntity,
          createdByUser = userEntity,
          crn = offenderDetails.otherIds.crn,
          decision = AssessmentDecision.REJECTED,
          submittedAt = OffsetDateTime.now(),
        ) { assessment, application ->
          webTestClient.post()
            .uri("/applications/${application.id}/appeals")
            .bodyValue(
              NewAppeal(
                appealDate = LocalDate.parse("2024-01-01"),
                appealDetail = "Some details about the appeal.",
                decision = AppealDecision.rejected,
                decisionDetail = "Some details about the decision.",
              ),
            )
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isCreated()
            .expectHeader()
            .valueMatches("Location", "/applications/${application.id}/appeals/[0-9a-f-]+")
            .expectBody()
            .jsonPath("$.appealDate").isEqualTo("2024-01-01")
            .jsonPath("$.appealDetail").isEqualTo("Some details about the appeal.")
            .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)
            .jsonPath("$.applicationId").isEqualTo(application.id.toString())
            .jsonPath("$.decision").isEqualTo(AppealDecision.rejected.value)
            .jsonPath("$.decisionDetail").isEqualTo("Some details about the decision.")
            .jsonPath("$.assessmentId").isEqualTo(assessment.id.toString())
            .jsonPath("$.createdByUser.id").isEqualTo(userEntity.id.toString())
            .jsonPath("$.createdByUser.name").isEqualTo(userEntity.name)
        }
      }
    }
  }

  @Test
  fun `Create new appeal does not create a new assessment and sends the correct notification email if the appeal was rejected`() {
    givenAUser(roles = listOf(UserRole.CAS1_APPEALS_MANAGER)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        givenAnAssessmentForApprovedPremises(
          allocatedToUser = userEntity,
          createdByUser = userEntity,
          crn = offenderDetails.otherIds.crn,
          decision = AssessmentDecision.REJECTED,
          submittedAt = OffsetDateTime.now(),
        ) { assessment, application ->
          webTestClient.post()
            .uri("/applications/${application.id}/appeals")
            .bodyValue(
              NewAppeal(
                appealDate = LocalDate.parse("2024-01-01"),
                appealDetail = "Some details about the appeal.",
                decision = AppealDecision.rejected,
                decisionDetail = "Some details about the decision.",
              ),
            )
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isCreated
            .returnResult(Appeal::class.java)

          val applicationResult = webTestClient.get()
            .uri("/applications/${application.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(ApprovedPremisesApplication::class.java)

          val applicationBody = applicationResult.responseBody.blockFirst()!!

          assertThat(applicationBody.status).isEqualTo(ApprovedPremisesApplicationStatus.rejected)

          assertThat(applicationBody.assessmentId).isEqualTo(assessment.id)
          assertThat(applicationBody.assessmentDecision).isEqualTo(ApiAssessmentDecision.rejected)
          assertThat(applicationBody.assessmentDecisionDate).isEqualTo(assessment.submittedAt!!.toLocalDate())

          emailAsserter.assertEmailsRequestedCount(1)
          emailAsserter.assertEmailRequested(application.createdByUser.email!!, notifyConfig.templates.appealReject)

          val assessments = assessmentTestRepository.findAllByApplication(application)
          assertThat(assessments.size).isEqualTo(1)
          assertThat(assessments[0].id).isEqualTo(assessment.id)
        }
      }
    }
  }

  @Test
  fun `Create new appeal creates a new assessment, updates the application status and sends notification emails if the appeal was accepted`() {
    givenAUser(roles = listOf(UserRole.CAS1_APPEALS_MANAGER)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        givenAnAssessmentForApprovedPremises(
          allocatedToUser = userEntity,
          createdByUser = userEntity,
          crn = offenderDetails.otherIds.crn,
          decision = AssessmentDecision.REJECTED,
          submittedAt = OffsetDateTime.now(),
        ) { assessment, application ->
          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          webTestClient.post()
            .uri("/applications/${application.id}/appeals")
            .bodyValue(
              NewAppeal(
                appealDate = LocalDate.parse("2024-01-01"),
                appealDetail = "Some details about the appeal.",
                decision = AppealDecision.accepted,
                decisionDetail = "Some details about the decision.",
              ),
            )
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isCreated
            .returnResult(Appeal::class.java)

          val applicationResult = webTestClient.get()
            .uri("/applications/${application.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(ApprovedPremisesApplication::class.java)

          val applicationBody = applicationResult.responseBody.blockFirst()!!

          assertThat(applicationBody.status).isEqualTo(ApprovedPremisesApplicationStatus.awaitingAssesment)

          assertThat(applicationBody.assessmentId).isNotNull()
          assertThat(applicationBody.assessmentId).isNotEqualTo(assessment.id)
          assertThat(applicationBody.assessmentDecision).isNull()
          assertThat(applicationBody.assessmentDecisionDate).isNull()

          val newAssessment = approvedPremisesAssessmentRepository.findByIdOrNull(applicationBody.assessmentId!!)

          assertThat(newAssessment).isNotNull()
          assertThat(newAssessment!!.createdFromAppeal).isTrue()
          assertThat(newAssessment.allocatedToUser).isNotNull()
          assertThat(newAssessment.allocatedToUser!!.id).isEqualTo(userEntity.id)

          emailAsserter.assertEmailsRequestedCount(3)
          emailAsserter.assertEmailRequested(userEntity.email!!, notifyConfig.templates.appealSuccess)
          emailAsserter.assertEmailRequested(application.createdByUser.email!!, notifyConfig.templates.appealSuccess)

          emailAsserter.assertEmailRequested(userEntity.email!!, notifyConfig.templates.appealedAssessmentAllocated)
        }
      }
    }
  }

  @Test
  fun `Create new appeal adds an event to the application timeline`() {
    givenAUser(roles = listOf(UserRole.CAS1_APPEALS_MANAGER)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        givenAnAssessmentForApprovedPremises(
          allocatedToUser = userEntity,
          createdByUser = userEntity,
          crn = offenderDetails.otherIds.crn,
          decision = AssessmentDecision.REJECTED,
          submittedAt = OffsetDateTime.now(),
        ) { assessment, application ->
          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          webTestClient.post()
            .uri("/applications/${application.id}/appeals")
            .bodyValue(
              NewAppeal(
                appealDate = LocalDate.parse("2024-01-01"),
                appealDetail = "Some details about the appeal.",
                decision = AppealDecision.accepted,
                decisionDetail = "Some details about the decision.",
              ),
            )
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isCreated

          val appeal = appealTestRepository.findByApplicationId(application.id)!!

          val timelineResult = webTestClient.get()
            .uri("/applications/${application.id}/timeline")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(String::class.java)

          val timeline = objectMapper.readValue<List<TimelineEvent>>(timelineResult.responseBody.blockFirst()!!)

          assertThat(timeline).anyMatch {
            it.type == TimelineEventType.approvedPremisesAssessmentAppealed &&
              it.associatedUrls?.contains(
                TimelineEventAssociatedUrl(TimelineEventUrlType.assessmentAppeal, "http://frontend/applications/${application.id}/appeals/${appeal.id}"),
              ) == true
          }
        }
      }
    }
  }

  @Test
  fun `Creating multiple appeals creates the correct number of events in the application timeline`() {
    givenAUser(roles = listOf(UserRole.CAS1_APPEALS_MANAGER)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        givenAnAssessmentForApprovedPremises(
          allocatedToUser = userEntity,
          createdByUser = userEntity,
          crn = offenderDetails.otherIds.crn,
          decision = AssessmentDecision.REJECTED,
          submittedAt = OffsetDateTime.now(),
        ) { assessment, application ->
          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          webTestClient.post()
            .uri("/applications/${application.id}/appeals")
            .bodyValue(
              NewAppeal(
                appealDate = LocalDate.parse("2024-01-01"),
                appealDetail = "Some details about the appeal.",
                decision = AppealDecision.rejected,
                decisionDetail = "Some details about the decision.",
              ),
            )
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isCreated

          webTestClient.post()
            .uri("/applications/${application.id}/appeals")
            .bodyValue(
              NewAppeal(
                appealDate = LocalDate.parse("2024-01-01"),
                appealDetail = "Some details about the appeal.",
                decision = AppealDecision.rejected,
                decisionDetail = "Some details about the decision.",
              ),
            )
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isCreated

          val timelineResult = webTestClient.get()
            .uri("/applications/${application.id}/timeline")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(String::class.java)

          val timeline = objectMapper.readValue<List<TimelineEvent>>(timelineResult.responseBody.blockFirst()!!)

          assertThat(timeline.size).isEqualTo(2)
        }
      }
    }
  }
}
