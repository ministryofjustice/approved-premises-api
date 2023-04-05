package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentRejection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatedClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import java.time.LocalDate
import java.time.OffsetDateTime

class AssessmentTest : IntegrationTestBase() {
  @Autowired
  lateinit var inboundMessageListener: InboundMessageListener

  @Autowired
  lateinit var assessmentTransformer: AssessmentTransformer

  @BeforeEach
  fun clearMessages() {
    inboundMessageListener.clearMessages()
  }

  @Test
  fun `Get all assessments without JWT returns 401`() {
    webTestClient.get()
      .uri("/assessments")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get all assessments returns 200 with correct body`() {
    `Given a User` { user, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
          withAddedAt(OffsetDateTime.now())
        }

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(user)
          withApplicationSchema(applicationSchema)
        }

        val assessment = assessmentEntityFactory.produceAndPersist {
          withAllocatedToUser(user)
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
        }

        assessment.schemaUpToDate = true

        val reallocatedAssessment = assessmentEntityFactory.produceAndPersist {
          withAllocatedToUser(user)
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
          withReallocatedAt(OffsetDateTime.now())
        }

        reallocatedAssessment.schemaUpToDate = true

        webTestClient.get()
          .uri("/assessments")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              listOf(
                assessmentTransformer.transformJpaToApi(assessment, offenderDetails, inmateDetails)
              )
            )
          )
      }
    }
  }

  @Test
  fun `Get assessment by ID without JWT returns 401`() {
    webTestClient.get()
      .uri("/assessments/6966902f-9b7e-4fc7-96c4-b54ec02d16c9")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get assessment by ID returns 200 with correct body`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
          withAddedAt(OffsetDateTime.now())
        }

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(userEntity)
          withApplicationSchema(applicationSchema)
        }

        val assessment = assessmentEntityFactory.produceAndPersist {
          withAllocatedToUser(userEntity)
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
        }

        assessment.schemaUpToDate = true

        webTestClient.get()
          .uri("/assessments/${assessment.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              assessmentTransformer.transformJpaToApi(assessment, offenderDetails, inmateDetails)
            )
          )
      }
    }
  }

  @Test
  fun `Accept assessment without JWT returns 401`() {
    val placementRequest = NewPlacementRequest(
      gender = Gender.male,
      type = ApType.normal,
      expectedArrival = LocalDate.now(),
      duration = 12,
      location = "B74",
      radius = 50,
      essentialCriteria = listOf(PlacementCriteria.hasHearingLoop, PlacementCriteria.hasLift),
      desirableCriteria = listOf(PlacementCriteria.hasBrailleSignage, PlacementCriteria.acceptsSexOffenders),
      mentalHealthSupport = false
    )

    webTestClient.post()
      .uri("/assessments/6966902f-9b7e-4fc7-96c4-b54ec02d16c9/acceptance")
      .bodyValue(AssessmentAcceptance(document = "{}", requirements = placementRequest))
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Accept assessment returns 200, persists decision, creates and allocates a placement request, and emits SNS domain event message`() {
    `Given a User`(
      staffUserDetailsConfigBlock = { withProbationAreaCode("N21") }
    ) { userEntity, jwt ->
      `Given a User`(roles = listOf(UserRole.MATCHER)) { matcher1, _ ->
        `Given a User`(roles = listOf(UserRole.MATCHER)) { matcher2, _ ->
          `Given an Offender` { offenderDetails, inmateDetails ->
            val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            }

            val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
              withAddedAt(OffsetDateTime.now())
            }

            val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(userEntity)
              withApplicationSchema(applicationSchema)
            }

            val assessment = assessmentEntityFactory.produceAndPersist {
              withAllocatedToUser(userEntity)
              withApplication(application)
              withAssessmentSchema(assessmentSchema)
            }

            val postcodeDistrict = postCodeDistrictFactory.produceAndPersist()

            assessment.schemaUpToDate = true

            val essentialCriteria = listOf(PlacementCriteria.hasHearingLoop, PlacementCriteria.hasLift)
            val desirableCriteria = listOf(PlacementCriteria.hasBrailleSignage, PlacementCriteria.acceptsSexOffenders)

            val placementRequest = NewPlacementRequest(
              gender = Gender.male,
              type = ApType.normal,
              expectedArrival = LocalDate.now(),
              duration = 12,
              location = postcodeDistrict.outcode,
              radius = 50,
              essentialCriteria = essentialCriteria,
              desirableCriteria = desirableCriteria,
              mentalHealthSupport = false
            )

            webTestClient.post()
              .uri("/assessments/${assessment.id}/acceptance")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(AssessmentAcceptance(document = mapOf("document" to "value"), requirements = placementRequest))
              .exchange()
              .expectStatus()
              .isOk

            val persistedAssessment = assessmentRepository.findByIdOrNull(assessment.id)!!
            assertThat(persistedAssessment.decision).isEqualTo(AssessmentDecision.ACCEPTED)
            assertThat(persistedAssessment.document).isEqualTo("{\"document\":\"value\"}")
            assertThat(persistedAssessment.submittedAt).isNotNull

            val emittedMessage = inboundMessageListener.blockForMessage()

            assertThat(emittedMessage.eventType).isEqualTo("approved-premises.application.assessed")
            assertThat(emittedMessage.description).isEqualTo("An application has been assessed for an Approved Premises placement")
            assertThat(emittedMessage.detailUrl).matches("http://frontend/events/application-assessed/[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}")
            assertThat(emittedMessage.additionalInformation.applicationId).isEqualTo(assessment.application.id)
            assertThat(emittedMessage.personReference.identifiers).containsExactlyInAnyOrder(
              SnsEventPersonReference("CRN", offenderDetails.otherIds.crn),
              SnsEventPersonReference("NOMS", offenderDetails.otherIds.nomsNumber!!)
            )

            val persistedPlacementRequest = placementRequestRepository.findByApplication(application)

            assertThat(persistedPlacementRequest.allocatedToUser.id).isIn(listOf(matcher1.id, matcher2.id))
            assertThat(persistedPlacementRequest.application.id).isEqualTo(application.id)
            assertThat(persistedPlacementRequest.duration).isEqualTo(placementRequest.duration)
            assertThat(persistedPlacementRequest.apType).isEqualTo(placementRequest.type)
            assertThat(persistedPlacementRequest.mentalHealthSupport).isEqualTo(placementRequest.mentalHealthSupport)
            assertThat(persistedPlacementRequest.expectedArrival).isEqualTo(placementRequest.expectedArrival)
            assertThat(persistedPlacementRequest.gender).isEqualTo(placementRequest.gender)
            assertThat(persistedPlacementRequest.postcodeDistrict.outcode).isEqualTo(placementRequest.location)
            assertThat(persistedPlacementRequest.radius).isEqualTo(placementRequest.radius)

            assertThat(persistedPlacementRequest.desirableCriteria.map { it.propertyName }).containsExactlyInAnyOrderElementsOf(placementRequest.desirableCriteria.map { it.toString() })
            assertThat(persistedPlacementRequest.essentialCriteria.map { it.propertyName }).containsExactlyInAnyOrderElementsOf(placementRequest.essentialCriteria.map { it.toString() })
          }
        }
      }
    }
  }

  @Test
  fun `Accept assessment returns an error if the postcode cannot be found`() {
    `Given a User`(
      staffUserDetailsConfigBlock = { withProbationAreaCode("N21") }
    ) { userEntity, jwt ->
      `Given a User`(roles = listOf(UserRole.MATCHER)) { matcher1, _ ->
        `Given a User`(roles = listOf(UserRole.MATCHER)) { matcher2, _ ->
          `Given an Offender` { offenderDetails, inmateDetails ->
            val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            }

            val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
              withAddedAt(OffsetDateTime.now())
            }

            val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(userEntity)
              withApplicationSchema(applicationSchema)
            }

            val assessment = assessmentEntityFactory.produceAndPersist {
              withAllocatedToUser(userEntity)
              withApplication(application)
              withAssessmentSchema(assessmentSchema)
            }

            assessment.schemaUpToDate = true

            val essentialCriteria = listOf(PlacementCriteria.hasHearingLoop, PlacementCriteria.hasLift)
            val desirableCriteria = listOf(PlacementCriteria.hasBrailleSignage, PlacementCriteria.acceptsSexOffenders)

            val placementRequest = NewPlacementRequest(
              gender = Gender.male,
              type = ApType.normal,
              expectedArrival = LocalDate.now(),
              duration = 12,
              location = "SW1",
              radius = 50,
              essentialCriteria = essentialCriteria,
              desirableCriteria = desirableCriteria,
              mentalHealthSupport = false
            )

            webTestClient.post()
              .uri("/assessments/${assessment.id}/acceptance")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(AssessmentAcceptance(document = mapOf("document" to "value"), requirements = placementRequest))
              .exchange()
              .expectStatus()
              .is4xxClientError
              .expectBody()
              .jsonPath("title").isEqualTo("Bad Request")
              .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
              .jsonPath("invalid-params[0].propertyName").isEqualTo("\$.postcodeDistrict")
          }
        }
      }
    }
  }

  @Test
  fun `Reject assessment without JWT returns 401`() {
    webTestClient.post()
      .uri("/assessments/6966902f-9b7e-4fc7-96c4-b54ec02d16c9/rejection")
      .bodyValue(AssessmentRejection(document = "{}", rejectionRationale = "reasoning"))
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Reject assessment returns 200, persists decision, emits SNS domain event message`() {
    `Given a User`(
      staffUserDetailsConfigBlock = { withProbationAreaCode("N21") }
    ) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
          withAddedAt(OffsetDateTime.now())
        }

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(userEntity)
          withApplicationSchema(applicationSchema)
        }

        val assessment = assessmentEntityFactory.produceAndPersist {
          withAllocatedToUser(userEntity)
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
        }

        assessment.schemaUpToDate = true

        webTestClient.post()
          .uri("/assessments/${assessment.id}/rejection")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(AssessmentRejection(document = mapOf("document" to "value"), rejectionRationale = "reasoning"))
          .exchange()
          .expectStatus()
          .isOk

        val persistedAssessment = assessmentRepository.findByIdOrNull(assessment.id)!!
        assertThat(persistedAssessment.decision).isEqualTo(AssessmentDecision.REJECTED)
        assertThat(persistedAssessment.document).isEqualTo("{\"document\":\"value\"}")
        assertThat(persistedAssessment.submittedAt).isNotNull

        val emittedMessage = inboundMessageListener.blockForMessage()

        assertThat(emittedMessage.eventType).isEqualTo("approved-premises.application.assessed")
        assertThat(emittedMessage.description).isEqualTo("An application has been assessed for an Approved Premises placement")
        assertThat(emittedMessage.detailUrl).matches("http://frontend/events/application-assessed/[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}")
        assertThat(emittedMessage.additionalInformation.applicationId).isEqualTo(assessment.application.id)
        assertThat(emittedMessage.personReference.identifiers).containsExactlyInAnyOrder(
          SnsEventPersonReference("CRN", offenderDetails.otherIds.crn),
          SnsEventPersonReference("NOMS", offenderDetails.otherIds.nomsNumber!!)
        )
      }
    }
  }

  @Test
  fun `Create clarification note returns 200 with correct body`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(userEntity)
          withApplicationSchema(applicationSchema)
        }

        val assessment = assessmentEntityFactory.produceAndPersist {
          withAllocatedToUser(userEntity)
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
        }

        webTestClient.post()
          .uri("/assessments/${assessment.id}/notes")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewClarificationNote(
              query = "some text"
            )
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.query").isEqualTo("some text")
      }
    }
  }

  @Test
  fun `Update clarification note returns 201 with correct body`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(userEntity)
          withApplicationSchema(applicationSchema)
        }

        val assessment = assessmentEntityFactory.produceAndPersist {
          withAllocatedToUser(userEntity)
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
        }

        val clarificationNote = assessmentClarificationNoteEntityFactory.produceAndPersist {
          withAssessment(assessment)
          withCreatedBy(userEntity)
        }

        webTestClient.put()
          .uri("/assessments/${assessment.id}/notes/${clarificationNote.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatedClarificationNote(
              response = "some text",
              responseReceivedOn = LocalDate.parse("2022-03-04")
            )
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.response").isEqualTo("some text")
          .jsonPath("$.responseReceivedOn").isEqualTo("2022-03-04")
      }
    }
  }
}
