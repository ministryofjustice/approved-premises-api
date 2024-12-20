package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecisionEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatePlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASubmittedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision as JpaPlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType as JpaPlacementType

class PlacementApplicationsTest : IntegrationTestBase() {

  @BeforeEach
  fun setupBankHolidays() {
    govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()
  }

  @Nested
  inner class CreatePlacementApplicationTest {
    @Test
    fun `creating a placement application JWT returns 401`() {
      webTestClient.post()
        .uri("/placement-applications")
        .bodyValue(
          NewPlacementApplication(
            applicationId = UUID.randomUUID(),
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `creating a placement application when the application does not exist returns 404`() {
      givenAUser { _, jwt ->
        webTestClient.post()
          .uri("/placement-applications")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewPlacementApplication(
              applicationId = UUID.randomUUID(),
            ),
          )
          .exchange()
          .expectStatus()
          .isNotFound()
      }
    }

    @Test
    fun `creating a placement application when the application cannot be viewed by the user returns 401`() {
      givenAUser { _, jwt ->
        givenAUser { otherUser, _ ->
          val offender = givenAnOffender(
            offenderDetailsConfigBlock = {
              withCurrentRestriction(true)
            },
          ).first
          givenASubmittedApplication(
            createdByUser = otherUser,
            crn = offender.otherIds.crn,
          ) { application ->
            webTestClient.post()
              .uri("/placement-applications")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(
                NewPlacementApplication(
                  applicationId = application.id,
                ),
              )
              .exchange()
              .expectStatus()
              .isForbidden()
          }
        }
      }
    }

    @Test
    fun `creating a placement application when the application does not have an assessment returns an error`() {
      givenAUser { user, jwt ->
        givenASubmittedApplication(createdByUser = user) { application ->
          webTestClient.post()
            .uri("/placement-applications")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewPlacementApplication(
                applicationId = application.id,
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest()
        }
      }
    }

    @Test
    fun `creating a placement application when the assessment has been rejected returns an error`() {
      givenAUser { user, jwt ->
        givenAnAssessmentForApprovedPremises(decision = AssessmentDecision.REJECTED, allocatedToUser = user, createdByUser = user) { _, application ->
          approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          webTestClient.post()
            .uri("/placement-applications")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewPlacementApplication(
                applicationId = application.id,
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest()
        }
      }
    }

    @Test
    fun `creating a placement application for an expired application returns an error`() {
      givenAUser { user, jwt ->
        givenAnAssessmentForApprovedPremises(decision = AssessmentDecision.ACCEPTED, allocatedToUser = user, createdByUser = user, submittedAt = OffsetDateTime.now()) { _, application ->
          approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          application.status = ApprovedPremisesApplicationStatus.EXPIRED
          approvedPremisesApplicationRepository.save(application)

          webTestClient.post()
            .uri("/placement-applications")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewPlacementApplication(
                applicationId = application.id,
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.detail").isEqualTo("Placement requests cannot be made for an expired application")
        }
      }
    }

    @Test
    fun `creating a placement application when the application belongs to the user returns successfully`() {
      givenAUser { user, jwt ->
        givenAnAssessmentForApprovedPremises(decision = AssessmentDecision.ACCEPTED, allocatedToUser = user, createdByUser = user, submittedAt = OffsetDateTime.now()) { _, application ->
          val schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          val rawResult = webTestClient.post()
            .uri("/placement-applications")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewPlacementApplication(
                applicationId = application.id,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val body = objectMapper.readValue(rawResult, PlacementApplication::class.java)

          assertThat(body.applicationId).isEqualTo(application.id)
          assertThat(body.applicationId).isEqualTo(application.id)
          assertThat(body.outdatedSchema).isEqualTo(false)
          assertThat(body.createdAt).isNotNull()
          assertThat(body.schemaVersion).isEqualTo(schema.id)
        }
      }
    }
  }

  @Nested
  inner class GetPlacementApplicationTest {
    @Test
    fun `getting a placement request application JWT returns 401`() {
      givenAUser { user, _ ->
        givenAPlacementApplication(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
        ) { placementApplicationEntity ->
          webTestClient.get()
            .uri("/placement-request-applications/${placementApplicationEntity.id}")
            .exchange()
            .expectStatus()
            .isUnauthorized
        }
      }
    }

    @Test
    fun `getting a nonexistent placement request application returns 404`() {
      givenAUser { _, jwt ->
        webTestClient.get()
          .uri("/placement-applications/${UUID.randomUUID()}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `getting a placement application where the Offender is LAO but user does not pass LAO check or have LAO qualification returns 403`() {
      givenAUser { user, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = {
            withCurrentExclusion(true)
          },
        ) { offenderDetails, _ ->
          givenAPlacementApplication(
            crn = offenderDetails.otherIds.crn,
            createdByUser = user,
            schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
          ) { placementApplicationEntity ->
            webTestClient.get()
              .uri("/placement-applications/${placementApplicationEntity.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isForbidden
          }
        }
      }
    }

    @Test
    fun `getting a placement application where the Offender is LAO, does not have LAO qualification but does pass LAO check or returns 200`() {
      givenAUser { user, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = {
            withCurrentExclusion(true)
          },
        ) { offenderDetails, _ ->
          givenAPlacementApplication(
            crn = offenderDetails.otherIds.crn,
            createdByUser = user,
            schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
          ) { placementApplicationEntity ->
            apDeliusContextAddResponseToUserAccessCall(
              listOf(
                CaseAccessFactory()
                  .withCrn(offenderDetails.otherIds.crn)
                  .produce(),
              ),
              user.deliusUsername,
            )

            val rawResult = webTestClient.get()
              .uri("/placement-applications/${placementApplicationEntity.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val body = objectMapper.readValue(rawResult, PlacementApplication::class.java)

            assertThat(body.id).isEqualTo(placementApplicationEntity.id)
            assertThat(body.applicationId).isEqualTo(placementApplicationEntity.application.id)
            assertThat(body.createdByUserId).isEqualTo(placementApplicationEntity.createdByUser.id)
            assertThat(body.schemaVersion).isEqualTo(placementApplicationEntity.schemaVersion.id)
            assertThat(body.createdAt).isEqualTo(placementApplicationEntity.createdAt.toInstant())
            assertThat(body.submittedAt).isNull()
          }
        }
      }
    }

    @Test
    fun `getting a placement application where the Offender is LAO, does not pass LAO check but does have LAO qualification returns 200`() {
      givenAUser(qualifications = listOf(UserQualification.LAO)) { user, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = {
            withCurrentExclusion(true)
          },
        ) { offenderDetails, _ ->
          givenAPlacementApplication(
            crn = offenderDetails.otherIds.crn,
            createdByUser = user,
            schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
          ) { placementApplicationEntity ->
            val rawResult = webTestClient.get()
              .uri("/placement-applications/${placementApplicationEntity.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val body = objectMapper.readValue(rawResult, PlacementApplication::class.java)

            assertThat(body.id).isEqualTo(placementApplicationEntity.id)
            assertThat(body.applicationId).isEqualTo(placementApplicationEntity.application.id)
            assertThat(body.createdByUserId).isEqualTo(placementApplicationEntity.createdByUser.id)
            assertThat(body.schemaVersion).isEqualTo(placementApplicationEntity.schemaVersion.id)
            assertThat(body.createdAt).isEqualTo(placementApplicationEntity.createdAt.toInstant())
            assertThat(body.submittedAt).isNull()
          }
        }
      }
    }

    @Test
    fun `getting a placement application returns the transformed object`() {
      givenAUser { user, jwt ->
        givenAPlacementApplication(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
        ) { placementApplicationEntity ->
          val rawResult = webTestClient.get()
            .uri("/placement-applications/${placementApplicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val body = objectMapper.readValue(rawResult, PlacementApplication::class.java)

          assertThat(body.id).isEqualTo(placementApplicationEntity.id)
          assertThat(body.applicationId).isEqualTo(placementApplicationEntity.application.id)
          assertThat(body.createdByUserId).isEqualTo(placementApplicationEntity.createdByUser.id)
          assertThat(body.schemaVersion).isEqualTo(placementApplicationEntity.schemaVersion.id)
          assertThat(body.createdAt).isEqualTo(placementApplicationEntity.createdAt.toInstant())
          assertThat(body.submittedAt).isNull()
        }
      }
    }
  }

  @Nested
  inner class UpdatePlacementApplicationTest {
    @Test
    fun `updating a placement request application without a JWT returns 401`() {
      webTestClient.put()
        .uri("/placement-applications/${UUID.randomUUID()}")
        .bodyValue(
          UpdatePlacementApplication(
            data = mapOf("thingId" to 123),
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `updating a submitted placement request application returns an error`() {
      givenAUser { user, jwt ->
        givenAPlacementApplication(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          submittedAt = OffsetDateTime.now(),
        ) { placementApplicationEntity ->
          webTestClient.put()
            .uri("/placement-applications/${placementApplicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              UpdatePlacementApplication(
                data = mapOf("thingId" to 123),
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
        }
      }
    }

    @Test
    fun `updating a placement request application created by a different user returns an error`() {
      givenAUser { _, jwt ->
        givenAPlacementApplication(
          createdByUser = userEntityFactory.produceAndPersist {
            withYieldedProbationRegion { givenAProbationRegion() }
          },
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          submittedAt = OffsetDateTime.now(),
        ) { placementApplicationEntity ->
          webTestClient.put()
            .uri("/placement-applications/${placementApplicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              UpdatePlacementApplication(
                data = mapOf("thingId" to 123),
              ),
            )
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }

    @Test
    fun `updating a placement request application with an outdated schema returns an error`() {
      givenAUser { user, jwt ->
        givenAPlacementApplication(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          submittedAt = OffsetDateTime.now(),
        ) { placementApplicationEntity ->
          approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          webTestClient.put()
            .uri("/placement-applications/${placementApplicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              UpdatePlacementApplication(
                data = mapOf("thingId" to 123),
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
        }
      }
    }

    @Test
    fun `updating a placement request application for an expired application returns an error`() {
      givenAUser { user, jwt ->
        givenAPlacementApplication(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
        ) { placementApplicationEntity ->

          var application = placementApplicationEntity.application
          application.status = ApprovedPremisesApplicationStatus.EXPIRED
          approvedPremisesApplicationRepository.save(application)

          webTestClient.put()
            .uri("/placement-applications/${placementApplicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              UpdatePlacementApplication(
                data = mapOf("thingId" to 123),
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.detail").isEqualTo("Placement requests cannot be made for an expired application")
        }
      }
    }

    @Test
    fun `updating an in-progress placement request application returns successfully and updates the application`() {
      givenAUser { user, jwt ->
        givenAPlacementApplication(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
        ) { placementApplicationEntity ->
          val rawResult = webTestClient.put()
            .uri("/placement-applications/${placementApplicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              UpdatePlacementApplication(
                data = mapOf("thingId" to 123),
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val body = objectMapper.readValue(rawResult, PlacementApplication::class.java)
          val expectedUpdatedPlacementApplication = placementApplicationEntity.copy(
            schemaUpToDate = true,
            data = "{\"thingId\":123}",
          )

          assertThat(body).matches {
            expectedUpdatedPlacementApplication.id == it.id &&
              expectedUpdatedPlacementApplication.application.id == it.applicationId &&
              expectedUpdatedPlacementApplication.createdByUser.id == it.createdByUserId &&
              expectedUpdatedPlacementApplication.schemaVersion.id == it.schemaVersion &&
              expectedUpdatedPlacementApplication.createdAt.toInstant() == it.createdAt &&
              serializableToJsonNode(expectedUpdatedPlacementApplication.data) == serializableToJsonNode(it.data)
          }

          val updatedPlacementApplication = placementApplicationRepository.findByIdOrNull(placementApplicationEntity.id)

          assertThat(updatedPlacementApplication!!.data).isEqualTo(expectedUpdatedPlacementApplication.data)
        }
      }
    }
  }

  @Nested
  inner class SubmitPlacementApp {
    @Test
    fun `submitting a placement request application without a JWT returns 401`() {
      webTestClient.post()
        .uri("/placement-applications/${UUID.randomUUID()}/submission")
        .bodyValue(
          SubmitPlacementApplication(
            translatedDocument = mapOf("thingId" to 123),
            placementType = PlacementType.additionalPlacement,
            placementDates = listOf(
              PlacementDates(
                expectedArrival = LocalDate.now(),
                duration = 12,
              ),
            ),
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `submitting an already submitted placement request application returns an error`() {
      givenAUser { user, jwt ->
        givenAPlacementApplication(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          submittedAt = OffsetDateTime.now(),
        ) { placementApplicationEntity ->
          webTestClient.post()
            .uri("/placement-applications/${placementApplicationEntity.id}/submission")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              SubmitPlacementApplication(
                translatedDocument = mapOf("thingId" to 123),
                placementType = PlacementType.additionalPlacement,
                placementDates = listOf(
                  PlacementDates(
                    expectedArrival = LocalDate.now(),
                    duration = 12,
                  ),
                ),
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
        }
      }
    }

    @Test
    fun `submitting a placement request for an expired application returns an error`() {
      givenAUser { user, jwt ->
        givenAPlacementApplication(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
        ) { placementApplicationEntity ->

          var application = placementApplicationEntity.application
          application.status = ApprovedPremisesApplicationStatus.EXPIRED
          approvedPremisesApplicationRepository.save(application)

          webTestClient.post()
            .uri("/placement-applications/${placementApplicationEntity.id}/submission")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              SubmitPlacementApplication(
                translatedDocument = mapOf("thingId" to 123),
                placementType = PlacementType.additionalPlacement,
                placementDates = listOf(
                  PlacementDates(
                    expectedArrival = LocalDate.now(),
                    duration = 12,
                  ),
                ),
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.detail").isEqualTo("Placement requests cannot be made for an expired application")
        }
      }
    }

    @Test
    fun `submitting a placement request application created by a different user returns an error`() {
      givenAUser { _, jwt ->
        givenAPlacementApplication(
          createdByUser = userEntityFactory.produceAndPersist {
            withYieldedProbationRegion { givenAProbationRegion() }
          },
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          submittedAt = OffsetDateTime.now(),
        ) { placementApplicationEntity ->
          webTestClient.post()
            .uri("/placement-applications/${placementApplicationEntity.id}/submission")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              SubmitPlacementApplication(
                translatedDocument = mapOf("thingId" to 123),
                placementType = PlacementType.additionalPlacement,
                placementDates = listOf(
                  PlacementDates(
                    expectedArrival = LocalDate.now(),
                    duration = 12,
                  ),
                ),
              ),
            )
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }

    @Test
    fun `submitting a placement request application with an outdated schema returns an error`() {
      givenAUser { user, jwt ->
        givenAPlacementApplication(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          submittedAt = OffsetDateTime.now(),
        ) { placementApplicationEntity ->
          approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          webTestClient.post()
            .uri("/placement-applications/${placementApplicationEntity.id}/submission")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              SubmitPlacementApplication(
                translatedDocument = mapOf("thingId" to 123),
                placementType = PlacementType.additionalPlacement,
                placementDates = listOf(
                  PlacementDates(
                    expectedArrival = LocalDate.now(),
                    duration = 12,
                  ),
                ),
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
        }
      }
    }

    @Test
    fun `submitting a placement application with a single date returns successfully, sends emails, raises domain event and updates the application`() {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER), qualifications = listOf()) { matcherUser, _ ->
        givenAUser { user, jwt ->
          givenAPlacementApplication(
            createdByUser = user,
            schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
          ) { placementApplicationEntity ->
            givenAnOffender(
              offenderDetailsConfigBlock = {
                withCrn(placementApplicationEntity.application.crn)
              },
            ) { _, _ ->
              govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

              val placementDates = listOf(
                PlacementDates(
                  expectedArrival = LocalDate.of(2025, 3, 10),
                  duration = 12,
                ),
              )
              val rawResult = webTestClient.post()
                .uri("/placement-applications/${placementApplicationEntity.id}/submission")
                .header("Authorization", "Bearer $jwt")
                .bodyValue(
                  SubmitPlacementApplication(
                    translatedDocument = mapOf("thingId" to 123),
                    placementType = PlacementType.additionalPlacement,
                    placementDates = placementDates,
                  ),
                )
                .exchange()
                .expectStatus()
                .isOk
                .returnResult<String>()
                .responseBody
                .blockFirst()

              val body = objectMapper.readValue<List<PlacementApplication>>(rawResult!!)
              assertThat(body).hasSize(1)

              val expectedUpdatedPlacementApplication = placementApplicationEntity.copy(
                schemaUpToDate = true,
                document = "{\"thingId\":123}",
              )

              assertThat(body[0]).matches {
                expectedUpdatedPlacementApplication.id == it.id &&
                  expectedUpdatedPlacementApplication.application.id == it.applicationId &&
                  expectedUpdatedPlacementApplication.createdByUser.id == it.createdByUserId &&
                  expectedUpdatedPlacementApplication.schemaVersion.id == it.schemaVersion &&
                  expectedUpdatedPlacementApplication.createdAt.toInstant() == it.createdAt &&
                  serializableToJsonNode(expectedUpdatedPlacementApplication.document) == serializableToJsonNode(it.document)
              }

              val updatedPlacementApplication =
                placementApplicationRepository.findByIdOrNull(placementApplicationEntity.id)!!

              assertThat(updatedPlacementApplication.document).isEqualTo(expectedUpdatedPlacementApplication.document)
              assertThat(updatedPlacementApplication.submittedAt).isNotNull()
              assertThat(updatedPlacementApplication.allocatedToUser).isNull()

              val createdPlacementDates = placementDateRepository.findAllByPlacementApplication(placementApplicationEntity)

              assertThat(createdPlacementDates.size).isEqualTo(1)

              assertThat(createdPlacementDates[0].placementApplication.id).isEqualTo(placementApplicationEntity.id)
              assertThat(createdPlacementDates[0].duration).isEqualTo(placementDates[0].duration)
              assertThat(createdPlacementDates[0].expectedArrival).isEqualTo(placementDates[0].expectedArrival)

              domainEventAsserter.assertDomainEventOfTypeStored(
                placementApplicationEntity.application.id,
                DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED,
              )

              val recipient = placementApplicationEntity.createdByUser.email!!
              val templates = notifyConfig.templates

              emailAsserter.assertEmailsRequestedCount(1)
              emailAsserter.assertEmailRequested(recipient, templates.placementRequestSubmittedV2, mapOf("startDate" to "2025-03-10"))
            }
          }
        }
      }
    }

    @Test
    fun `submitting a placement application with multiple dates returns successfully and produces multiple placment apps`() {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER), qualifications = listOf()) { matcherUser, _ ->
        givenAUser { user, jwt ->
          givenAPlacementApplication(
            createdByUser = user,
            schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
          ) { placementApplicationEntity ->
            givenAnOffender(
              offenderDetailsConfigBlock = {
                withCrn(placementApplicationEntity.application.crn)
              },
            ) { offenderDetails, inmateDetails ->
              govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

              val arrival1 = LocalDate.of(2024, 1, 2)
              val duration1 = 12
              val arrival2 = LocalDate.of(2024, 2, 3)
              val duration2 = 10
              val arrival3 = LocalDate.of(2024, 3, 4)
              val duration3 = 15

              val placementDates = listOf(
                PlacementDates(
                  expectedArrival = arrival1,
                  duration = duration1,
                ),
                PlacementDates(
                  expectedArrival = arrival2,
                  duration = duration2,
                ),
                PlacementDates(
                  expectedArrival = arrival3,
                  duration = duration3,
                ),
              )
              val rawResult = webTestClient.post()
                .uri("/placement-applications/${placementApplicationEntity.id}/submission")
                .header("Authorization", "Bearer $jwt")
                .bodyValue(
                  SubmitPlacementApplication(
                    translatedDocument = mapOf("thingId" to 123),
                    placementType = PlacementType.additionalPlacement,
                    placementDates = placementDates,
                  ),
                )
                .exchange()
                .expectStatus()
                .isOk
                .returnResult<String>()
                .responseBody
                .blockFirst()

              val body = objectMapper.readValue<List<PlacementApplication>>(rawResult!!)
              assertThat(body).hasSize(3)

              val createdApp1Id = body[0].id
              val updatedEntity1 = placementApplicationRepository.findByIdOrNull(createdApp1Id)!!
              assertThat(updatedEntity1.placementDates[0].expectedArrival).isEqualTo(arrival1)
              assertThat(updatedEntity1.placementDates[0].duration).isEqualTo(duration1)
              assertThat(updatedEntity1.submittedAt).isNotNull()
              assertThat(updatedEntity1.allocatedToUser).isNull()

              val createdApp2Id = body[1].id
              val updatedEntity2 = placementApplicationRepository.findByIdOrNull(createdApp2Id)!!
              assertThat(updatedEntity2.placementDates[0].expectedArrival).isEqualTo(arrival2)
              assertThat(updatedEntity2.placementDates[0].duration).isEqualTo(duration2)
              assertThat(updatedEntity2.submittedAt).isNotNull()
              assertThat(updatedEntity1.allocatedToUser).isNull()

              val createdApp3Id = body[2].id
              val updatedEntity3 = placementApplicationRepository.findByIdOrNull(createdApp3Id)!!
              assertThat(updatedEntity3.placementDates[0].expectedArrival).isEqualTo(arrival3)
              assertThat(updatedEntity3.placementDates[0].duration).isEqualTo(duration3)
              assertThat(updatedEntity3.submittedAt).isNotNull()
              assertThat(updatedEntity1.allocatedToUser).isNull()

              val recipient = placementApplicationEntity.createdByUser.email!!
              val templates = notifyConfig.templates

              domainEventAsserter.assertDomainEventsOfTypeStored(
                applicationId = placementApplicationEntity.application.id,
                eventType = DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED,
                expectedCount = 3,
              )

              emailAsserter.assertEmailsRequestedCount(3)
              emailAsserter.assertEmailRequested(recipient, templates.placementRequestSubmittedV2, mapOf("startDate" to "2024-01-02"))
              emailAsserter.assertEmailRequested(recipient, templates.placementRequestSubmittedV2, mapOf("startDate" to "2024-02-03"))
              emailAsserter.assertEmailRequested(recipient, templates.placementRequestSubmittedV2, mapOf("startDate" to "2024-03-04"))
            }
          }
        }
      }
    }
  }

  @Nested
  inner class SubmitDecisionForPlacementApplicationTest {

    @Test
    fun `submitting a placement request application decision without a JWT returns 401`() {
      webTestClient.post()
        .uri("/placement-applications/${UUID.randomUUID()}/decision")
        .bodyValue(
          PlacementApplicationDecisionEnvelope(
            decision = PlacementApplicationDecision.accepted,
            summaryOfChanges = "ChangeSummary",
            decisionSummary = "DecisionSummary",
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `submitting a placement application decision when the placement application does not exist returns 404`() {
      givenAUser { _, jwt ->
        webTestClient.post()
          .uri("/placement-applications/${UUID.randomUUID()}/decision")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            PlacementApplicationDecisionEnvelope(
              decision = PlacementApplicationDecision.accepted,
              summaryOfChanges = "ChangeSummary",
              decisionSummary = "DecisionSummary",
            ),
          )
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `submitting a placement request application decision with a decision already set returns an error`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          `Given a submitted Placement Application`(
            allocatedToUser = user,
            offenderDetails = offenderDetails,
            decision = JpaPlacementApplicationDecision.REJECTED,
          ) { placementApplicationEntity ->
            webTestClient.post()
              .uri("/placement-applications/${placementApplicationEntity.id}/decision")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(
                PlacementApplicationDecisionEnvelope(
                  decision = PlacementApplicationDecision.accepted,
                  summaryOfChanges = "ChangeSummary",
                  decisionSummary = "DecisionSummary",
                ),
              )
              .exchange()
              .expectStatus()
              .isBadRequest
          }
        }
      }
    }

    @Test
    fun `submitting a placement request application that is not assigned to me returns an error`() {
      givenAUser { _, jwt ->
        givenAUser { otherUser, _ ->
          givenAnOffender { offenderDetails, _ ->
            `Given a submitted Placement Application`(
              allocatedToUser = otherUser,
              offenderDetails = offenderDetails,
              decision = JpaPlacementApplicationDecision.REJECTED,
            ) { placementApplicationEntity ->
              webTestClient.post()
                .uri("/placement-applications/${placementApplicationEntity.id}/decision")
                .header("Authorization", "Bearer $jwt")
                .bodyValue(
                  PlacementApplicationDecisionEnvelope(
                    decision = PlacementApplicationDecision.accepted,
                    summaryOfChanges = "ChangeSummary",
                    decisionSummary = "DecisionSummary",
                  ),
                )
                .exchange()
                .expectStatus()
                .isForbidden
            }
          }
        }
      }
    }

    @Test
    fun `submitting a placement application decision when the placement requirements do not exist returns 404 and does not update the decision`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          `Given a submitted Placement Application`(allocatedToUser = user, offenderDetails = offenderDetails) { placementApplicationEntity ->
            webTestClient.post()
              .uri("/placement-applications/${placementApplicationEntity.id}/decision")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(
                PlacementApplicationDecisionEnvelope(
                  decision = PlacementApplicationDecision.accepted,
                  summaryOfChanges = "ChangeSummary",
                  decisionSummary = "DecisionSummary",
                ),
              )
              .exchange()
              .expectStatus()
              .isNotFound

            val updatedPlacementApplication =
              placementApplicationRepository.findByIdOrNull(placementApplicationEntity.id)!!

            assertThat(updatedPlacementApplication.decision).isEqualTo(null)
          }
        }
      }
    }

    @Test
    fun `submitting a placement application decision when the placement dates do not exist returns 404 and does not update the decision`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          `Given a submitted Placement Application`(allocatedToUser = user, offenderDetails = offenderDetails) { placementApplicationEntity ->
            `Given placement requirements`(placementApplicationEntity = placementApplicationEntity) { _ ->
              webTestClient.post()
                .uri("/placement-applications/${placementApplicationEntity.id}/decision")
                .header("Authorization", "Bearer $jwt")
                .bodyValue(
                  PlacementApplicationDecisionEnvelope(
                    decision = PlacementApplicationDecision.accepted,
                    summaryOfChanges = "ChangeSummary",
                    decisionSummary = "DecisionSummary",
                  ),
                )
                .exchange()
                .expectStatus()
                .isNotFound

              val updatedPlacementApplication =
                placementApplicationRepository.findByIdOrNull(placementApplicationEntity.id)!!

              assertThat(updatedPlacementApplication.decision).isEqualTo(null)
            }
          }
        }
      }
    }

    @ParameterizedTest
    @CsvSource("ROTL,false", "ADDITIONAL_PLACEMENT,false", "RELEASE_FOLLOWING_DECISION,true")
    fun `accepting a placement application decision records the decision, creates a placement request and sends an email`(placementType: JpaPlacementType, isParole: Boolean) {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER)) { matcher1, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_MATCHER)) { matcher2, _ ->
          givenAUser { user, jwt ->
            givenAnOffender { offenderDetails, _ ->
              `Given a submitted Placement Application`(allocatedToUser = user, offenderDetails = offenderDetails, placementType = placementType) { placementApplicationEntity ->
                `Given placement requirements`(placementApplicationEntity = placementApplicationEntity, createdAt = OffsetDateTime.now()) { placementRequirements ->
                  `Given placement requirements`(placementApplicationEntity = placementApplicationEntity, createdAt = OffsetDateTime.now().minusDays(4)) { _ ->
                    `Given placement dates`(placementApplicationEntity = placementApplicationEntity) { placementDates ->

                      webTestClient.post()
                        .uri("/placement-applications/${placementApplicationEntity.id}/decision")
                        .header("Authorization", "Bearer $jwt")
                        .bodyValue(
                          PlacementApplicationDecisionEnvelope(
                            decision = PlacementApplicationDecision.accepted,
                            summaryOfChanges = "ChangeSummary",
                            decisionSummary = "DecisionSummary",
                          ),
                        )
                        .exchange()
                        .expectStatus()
                        .isOk

                      val updatedPlacementApplication =
                        placementApplicationRepository.findByIdOrNull(placementApplicationEntity.id)!!

                      assertThat(updatedPlacementApplication.decision).isEqualTo(JpaPlacementApplicationDecision.ACCEPTED)
                      assertThat(updatedPlacementApplication.decisionMadeAt).isWithinTheLastMinute()

                      val createdPlacementRequests =
                        placementRequestTestRepository.findAllByApplication(placementApplicationEntity.application)

                      assertThat(createdPlacementRequests.size).isEqualTo(1)

                      val createdPlacementApplication = createdPlacementRequests[0]
                      assertThat(updatedPlacementApplication.placementDates[0].placementRequest!!.id).isEqualTo(createdPlacementApplication.id)

                      assertThat(createdPlacementApplication.allocatedToUser).isNull()
                      assertThat(createdPlacementApplication.application.id).isEqualTo(placementApplicationEntity.application.id)
                      assertThat(createdPlacementApplication.expectedArrival).isEqualTo(placementDates.expectedArrival)
                      assertThat(createdPlacementApplication.duration).isEqualTo(placementDates.duration)
                      assertThat(createdPlacementApplication.isParole).isEqualTo(isParole)
                      assertThat(createdPlacementApplication.placementRequirements.id).isEqualTo(placementRequirements.id)

                      emailAsserter.assertEmailsRequestedCount(1)
                      emailAsserter.assertEmailRequested(placementApplicationEntity.createdByUser.email!!, notifyConfig.templates.placementRequestDecisionAcceptedV2)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    @ParameterizedTest
    @CsvSource("ROTL", "ADDITIONAL_PLACEMENT", "RELEASE_FOLLOWING_DECISION")
    fun `rejecting a placement application decision records the decision and sends an email`(placementType: JpaPlacementType) {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER)) { matcher1, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_MATCHER)) { matcher2, _ ->
          givenAUser { user, jwt ->
            givenAnOffender { offenderDetails, _ ->
              `Given a submitted Placement Application`(allocatedToUser = user, offenderDetails = offenderDetails, placementType = placementType) { placementApplicationEntity ->
                `Given placement requirements`(placementApplicationEntity = placementApplicationEntity, createdAt = OffsetDateTime.now()) { placementRequirements ->
                  `Given placement requirements`(placementApplicationEntity = placementApplicationEntity, createdAt = OffsetDateTime.now().minusDays(4)) { _ ->
                    `Given placement dates`(placementApplicationEntity = placementApplicationEntity) { placementDates ->

                      webTestClient.post()
                        .uri("/placement-applications/${placementApplicationEntity.id}/decision")
                        .header("Authorization", "Bearer $jwt")
                        .bodyValue(
                          PlacementApplicationDecisionEnvelope(
                            decision = PlacementApplicationDecision.rejected,
                            summaryOfChanges = "ChangeSummary",
                            decisionSummary = "DecisionSummary",
                          ),
                        )
                        .exchange()
                        .expectStatus()
                        .isOk

                      val updatedPlacementApplication =
                        placementApplicationRepository.findByIdOrNull(placementApplicationEntity.id)!!

                      assertThat(updatedPlacementApplication.decision).isEqualTo(JpaPlacementApplicationDecision.REJECTED)
                      assertThat(updatedPlacementApplication.decisionMadeAt).isWithinTheLastMinute()

                      val createdPlacementRequests =
                        placementRequestTestRepository.findAllByApplication(placementApplicationEntity.application)
                      assertThat(createdPlacementRequests).isEmpty()

                      emailAsserter.assertEmailsRequestedCount(1)
                      emailAsserter.assertEmailRequested(placementApplicationEntity.createdByUser.email!!, notifyConfig.templates.placementRequestDecisionRejectedV2)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    private fun `Given a submitted Placement Application`(
      allocatedToUser: UserEntity,
      offenderDetails: OffenderDetailSummary,
      decision: JpaPlacementApplicationDecision? = null,
      placementType: JpaPlacementType? = JpaPlacementType.ADDITIONAL_PLACEMENT,
      block: (placementApplicationEntity: PlacementApplicationEntity) -> Unit,
    ) {
      val placementApplication = givenAPlacementApplication(
        allocatedToUser = allocatedToUser,
        createdByUser = userEntityFactory.produceAndPersist {
          withYieldedProbationRegion { givenAProbationRegion() }
        },
        schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        },
        submittedAt = OffsetDateTime.now(),
        crn = offenderDetails.otherIds.crn,
        decision = decision,
        reallocated = false,
        placementType = placementType,
      )

      block(placementApplication)
    }

    private fun `Given placement requirements`(
      placementApplicationEntity: PlacementApplicationEntity,
      createdAt: OffsetDateTime = OffsetDateTime.now(),
      block: (placementRequirements: PlacementRequirementsEntity) -> Unit,
    ) {
      val placementRequirements = placementRequirementsFactory.produceAndPersist {
        withApplication(placementApplicationEntity.application)
        withAssessment(placementApplicationEntity.application.getLatestAssessment()!! as ApprovedPremisesAssessmentEntity)
        withPostcodeDistrict(postCodeDistrictFactory.produceAndPersist())
        withDesirableCriteria(
          characteristicEntityFactory.produceAndPersistMultiple(5),
        )
        withEssentialCriteria(
          characteristicEntityFactory.produceAndPersistMultiple(3),
        )
        withCreatedAt(createdAt)
      }

      block(placementRequirements)
    }

    private fun `Given placement dates`(
      placementApplicationEntity: PlacementApplicationEntity,
      block: (placementDates: PlacementDateEntity) -> Unit,
    ) {
      block(
        placementDateFactory.produceAndPersist {
          withPlacementApplication(placementApplicationEntity)
        },
      )
    }
  }

  /**
   * Note - Withdrawal cascading is tested in [WithdrawalTest]
   */
  @Nested
  inner class WithdrawPlacementApplication {

    @Test
    fun `withdrawing a placement application JWT returns 401`() {
      webTestClient.post()
        .uri("/placement-applications/${UUID.randomUUID()}/withdraw")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `withdrawing a placement application decision when the placement application does not exist returns 404`() {
      givenAUser { _, jwt ->
        webTestClient.post()
          .uri("/placement-applications/${UUID.randomUUID()}/withdraw")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            WithdrawPlacementApplication(WithdrawPlacementRequestReason.duplicatePlacementRequest),
          )
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `withdrawing a placement application created by a different user returns an error`() {
      givenAUser { _, jwt ->
        givenAPlacementApplication(
          createdByUser = userEntityFactory.produceAndPersist {
            withYieldedProbationRegion { givenAProbationRegion() }
          },
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          submittedAt = OffsetDateTime.now(),
        ) { placementApplicationEntity ->

          webTestClient.post()
            .uri("/placement-applications/${placementApplicationEntity.id}/withdraw")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              WithdrawPlacementApplication(WithdrawPlacementRequestReason.duplicatePlacementRequest),
            )
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }

    @Test
    fun `withdrawing an in-progress placement application returns successfully and updates decision field`() {
      givenAUser { user, jwt ->
        givenAPlacementApplication(
          createdByUser = user,
          submittedAt = OffsetDateTime.now(),
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
        ) { placementApplicationEntity ->
          val rawResult = webTestClient.post()
            .uri("/placement-applications/${placementApplicationEntity.id}/withdraw")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              WithdrawPlacementApplication(WithdrawPlacementRequestReason.duplicatePlacementRequest),
            )
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val body = objectMapper.readValue(rawResult, PlacementApplication::class.java)

          assertThat(body).matches {
            placementApplicationEntity.id == it.id &&
              placementApplicationEntity.application.id == it.applicationId &&
              placementApplicationEntity.createdByUser.id == it.createdByUserId &&
              placementApplicationEntity.schemaVersion.id == it.schemaVersion &&
              placementApplicationEntity.createdAt.toInstant() == it.createdAt &&
              serializableToJsonNode(placementApplicationEntity.document) == serializableToJsonNode(it.document)
          }

          val updatedPlacementApplication =
            placementApplicationRepository.findByIdOrNull(placementApplicationEntity.id)!!

          assertThat(updatedPlacementApplication.isWithdrawn).isTrue()
        }
      }
    }

    @Test
    fun `withdrawing a submitted placement application as the applicant sends emails, raises a domain event and returns successfully`() {
      givenAUser { applicant, jwt ->

        givenAUser { assessor, _ ->
          givenAPlacementApplication(
            createdByUser = applicant,
            schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
            decision = null,
            submittedAt = OffsetDateTime.now(),
            allocatedToUser = assessor,
          ) { placementApplicationEntity ->

            val application = placementApplicationEntity.application

            val rawResult = webTestClient.post()
              .uri("/placement-applications/${placementApplicationEntity.id}/withdraw")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(
                WithdrawPlacementApplication(WithdrawPlacementRequestReason.duplicatePlacementRequest),
              )
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val body = objectMapper.readValue(rawResult, PlacementApplication::class.java)

            assertThat(body).matches {
              placementApplicationEntity.id == it.id &&
                application.id == it.applicationId &&
                placementApplicationEntity.createdByUser.id == it.createdByUserId &&
                placementApplicationEntity.schemaVersion.id == it.schemaVersion &&
                placementApplicationEntity.createdAt.toInstant() == it.createdAt &&
                serializableToJsonNode(placementApplicationEntity.document) == serializableToJsonNode(it.document)
            }

            snsDomainEventListener.blockForMessage(DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN)

            emailAsserter.assertEmailsRequestedCount(3)
            emailAsserter.assertEmailRequested(
              placementApplicationEntity.application.createdByUser.email!!,
              notifyConfig.templates.placementRequestWithdrawnV2,
            )
            emailAsserter.assertEmailRequested(
              placementApplicationEntity.createdByUser.email!!,
              notifyConfig.templates.placementRequestWithdrawnV2,
            )
            emailAsserter.assertEmailRequested(
              placementApplicationEntity.allocatedToUser!!.email!!,
              notifyConfig.templates.placementRequestWithdrawnV2,
            )
          }
        }
      }
    }

    @Test
    fun `withdrawing a submitted placement application as a workflow manager sends emails, raises a domain event and returns successfully`() {
      givenAUser { applicant, _ ->
        givenAUser { assessor, _ ->
          givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
            givenAPlacementApplication(
              createdByUser = applicant,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              decision = null,
              submittedAt = OffsetDateTime.now(),
              allocatedToUser = assessor,
            ) { placementApplicationEntity ->

              val application = placementApplicationEntity.application

              val rawResult = webTestClient.post()
                .uri("/placement-applications/${placementApplicationEntity.id}/withdraw")
                .header("Authorization", "Bearer $jwt")
                .bodyValue(
                  WithdrawPlacementApplication(WithdrawPlacementRequestReason.duplicatePlacementRequest),
                )
                .exchange()
                .expectStatus()
                .isOk
                .returnResult<String>()
                .responseBody
                .blockFirst()

              val body = objectMapper.readValue(rawResult, PlacementApplication::class.java)

              assertThat(body).matches {
                placementApplicationEntity.id == it.id &&
                  application.id == it.applicationId &&
                  placementApplicationEntity.createdByUser.id == it.createdByUserId &&
                  placementApplicationEntity.schemaVersion.id == it.schemaVersion &&
                  placementApplicationEntity.createdAt.toInstant() == it.createdAt &&
                  serializableToJsonNode(placementApplicationEntity.document) == serializableToJsonNode(it.document)
              }

              snsDomainEventListener.blockForMessage(DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN)

              emailAsserter.assertEmailsRequestedCount(3)
              emailAsserter.assertEmailRequested(
                placementApplicationEntity.application.createdByUser.email!!,
                notifyConfig.templates.placementRequestWithdrawnV2,
              )
              emailAsserter.assertEmailRequested(
                placementApplicationEntity.createdByUser.email!!,
                notifyConfig.templates.placementRequestWithdrawnV2,
              )
              emailAsserter.assertEmailRequested(
                placementApplicationEntity.allocatedToUser!!.email!!,
                notifyConfig.templates.placementRequestWithdrawnV2,
              )
            }
          }
        }
      }
    }
  }

  private fun serializableToJsonNode(serializable: Any?): JsonNode {
    if (serializable == null) return NullNode.instance
    if (serializable is String) return objectMapper.readTree(serializable)

    return objectMapper.readTree(objectMapper.writeValueAsString(serializable))
  }
}
