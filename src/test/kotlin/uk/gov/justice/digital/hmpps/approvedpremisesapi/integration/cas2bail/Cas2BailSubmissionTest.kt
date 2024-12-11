package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2bail

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearMocks
import io.mockk.every
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2SubmittedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2SubmittedApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.toHttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ExternalUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2Admin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2Assessor
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.manageUsersMockSuccessfulExternalUsersCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2BailApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailAssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailStatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailStatusUpdateDetailRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailStatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NomisUserTransformer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas2BailSubmissionTest(
  @Value("\${url-templates.frontend.cas2.application}") private val applicationUrlTemplate: String,
  @Value("\${url-templates.frontend.cas2.submitted-application-overview}") private val submittedApplicationUrlTemplate: String,
) : IntegrationTestBase() {
  @SpykBean
  lateinit var cas2BailRealApplicationRepository: Cas2BailApplicationRepository

  @SpykBean
  lateinit var cas2BailRealAssessmentRepository: Cas2BailAssessmentRepository

  @SpykBean
  lateinit var cas2BailRealStatusUpdateRepository: Cas2BailStatusUpdateRepository

  @SpykBean
  lateinit var cas2BailRealStatusUpdateDetailRepository: Cas2BailStatusUpdateDetailRepository

  @Autowired
  lateinit var nomisUserTransformer: NomisUserTransformer

  val schema = """
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
  """

  @AfterEach
  fun afterEach() {
    // SpringMockK does not correctly clear mocks for @SpyKBeans that are also a @Repository, causing mocked behaviour
    // in one test to show up in another (see https://github.com/Ninja-Squad/springmockk/issues/85)
    // Manually clearing after each test seems to fix this.
    clearMocks(cas2BailRealApplicationRepository)
    clearMocks(cas2BailRealAssessmentRepository)
    clearMocks(cas2BailRealStatusUpdateRepository)
    clearMocks(cas2BailRealStatusUpdateDetailRepository)
  }

  @Nested
  inner class ControlsOnExternalUsers {
    @Test
    fun `submitting an application is forbidden to external users based on role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "auth",
        roles = listOf("ROLE_CAS2_ASSESSOR"),
      )

      webTestClient.post()
        .uri("/cas2bail/submissions?applicationId=de6512fc-a225-4109-bdcd-86c6307a5237")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `viewing submitted applications is forbidden to internal users based on role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_POM"),
      )

      webTestClient.get()
        .uri("/cas2bail/submissions")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `viewing a single submitted application is forbidden to internal users based on     role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_POM"),
      )

      webTestClient.get()
        .uri("/cas2bail/submissions/66911cf0-75b1-4361-84bd-501b176fd4fd")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Nested
  inner class MissingJwt {
    @Test
    fun `Get all submitted applications without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas2bail/submissions")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get single submitted application without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas2bail/submissions/9b785e59-b85c-4be0-b271-d9ac287684b6")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  @Nested
  inner class GetToIndex {
    @Test
    fun `Previously unknown Assessor has an ExternalUser record created from details retrieved from Manage-Users API `() {
      externalUserRepository.deleteAll()

      val username = "PREVIOUSLY_UNKNOWN_ASSESSOR"
      val externalUserDetails = ExternalUserDetailsFactory()
        .withUsername(username)
        .produce()

      manageUsersMockSuccessfulExternalUsersCall(
        username = username,
        externalUserDetails = externalUserDetails,
      )

      val jwt = jwtAuthHelper.createValidExternalAuthorisationCodeJwt(username)

      webTestClient.get()
        .uri("/cas2bail/submissions")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk

      Assertions.assertThat(
        externalUserRepository.findByUsername(username),
      ).isNotNull
    }

    @Test
    fun `Assessor can view ALL submitted cas2bail applications`() {
      givenACas2Assessor { _externalUserEntity, jwt ->
        givenACas2PomUser { user, _ ->
          givenAnOffender { offenderDetails, _ ->
            cas2BailApplicationJsonSchemaRepository.deleteAll()

            val cas2bailApplicationSchema =
              cas2BailApplicationJsonSchemaEntityFactory.produceAndPersist {
                withAddedAt(OffsetDateTime.now())
                withId(UUID.randomUUID())
              }

            val submittedcas2bailApplicationentitySecond = cas2BailApplicationEntityFactory
              .produceAndPersist {
                withApplicationSchema(cas2bailApplicationSchema)
                withCreatedByUser(user)
                withCrn(offenderDetails.otherIds.crn)
                withNomsNumber(offenderDetails.otherIds.nomsNumber!!)
                withSubmittedAt(OffsetDateTime.parse("2023-01-02T09:00:00+01:00"))
                withData("{}")
              }

            val submittedcas2bailApplicationentityFirst = cas2BailApplicationEntityFactory
              .produceAndPersist {
                withApplicationSchema(cas2bailApplicationSchema)
                withCreatedByUser(user)
                withCrn(offenderDetails.otherIds.crn)
                withNomsNumber(offenderDetails.otherIds.nomsNumber!!)
                withSubmittedAt(OffsetDateTime.parse("2023-01-01T09:00:00+01:00"))
                withData("{}")
              }

            val submittedcas2bailApplicationentityThird = cas2BailApplicationEntityFactory
              .produceAndPersist {
                withApplicationSchema(cas2bailApplicationSchema)
                withCreatedByUser(user)
                withCrn(offenderDetails.otherIds.crn)
                withNomsNumber(offenderDetails.otherIds.nomsNumber!!)
                withSubmittedAt(OffsetDateTime.parse("2023-01-03T09:00:00+01:00"))
                withData("{}")
              }

            val inProgressCas2bailApplicationEntity = cas2BailApplicationEntityFactory
              .produceAndPersist {
                withApplicationSchema(cas2bailApplicationSchema)
                withCreatedByUser(user)
                withCrn(offenderDetails.otherIds.crn)
                withNomsNumber(offenderDetails.otherIds.nomsNumber!!)
                withSubmittedAt(null)
                withData("{}")
              }

            val rawResponseBody = webTestClient.get()
              .uri("/cas2bail/submissions?page=1")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.cas2.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
              .expectHeader().valueEquals("X-Pagination-TotalResults", 3)
              .expectHeader().valueEquals("X-Pagination-PageSize", 10)
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody =
              objectMapper.readValue(
                rawResponseBody,
                object : TypeReference<List<Cas2SubmittedApplicationSummary>>() {},
              )

            assertApplicationResponseMatchesExpected(
              responseBody[0],
              submittedcas2bailApplicationentityFirst,
              offenderDetails,
            )

            assertApplicationResponseMatchesExpected(
              responseBody[1],
              submittedcas2bailApplicationentitySecond,
              offenderDetails,
            )

            assertApplicationResponseMatchesExpected(
              responseBody[2],
              submittedcas2bailApplicationentityThird,
              offenderDetails,
            )

            Assertions.assertThat(responseBody).noneMatch {
              inProgressCas2bailApplicationEntity.id == it.id
            }
          }
        }
      }
    }

    private fun assertApplicationResponseMatchesExpected(
      response: Cas2SubmittedApplicationSummary,
      expectedSubmittedApplication: Cas2BailApplicationEntity,
      offenderDetails: OffenderDetailSummary,
    ) {
      Assertions.assertThat(response).matches {
        expectedSubmittedApplication.id == it.id &&
          expectedSubmittedApplication.crn == it.crn &&
          expectedSubmittedApplication.nomsNumber == it.nomsNumber &&
          expectedSubmittedApplication.createdAt.toInstant() == it.createdAt &&
          expectedSubmittedApplication.createdByUser.id == it.createdByUserId &&
          expectedSubmittedApplication.submittedAt?.toInstant() == it.submittedAt
      }

      Assertions.assertThat(response.personName)
        .isEqualTo("${offenderDetails.firstName} ${offenderDetails.surname}")
    }
  }

  @Nested
  inner class GetToShow {

    private fun createInProgressApplication(
      newestJsonSchema: Cas2BailApplicationJsonSchemaEntity,
      crn: String,
      user: NomisUserEntity,
    ): Cas2BailApplicationEntity {
      val applicationEntity = cas2BailApplicationEntityFactory.produceAndPersist {
        withApplicationSchema(newestJsonSchema)
        withCrn(crn)
        withCreatedByUser(user)
        withSubmittedAt(null)
        withData(
          """
                              {
                                 "thingId": 123
                              }
                              """,
        )
      }

      return applicationEntity
    }

    @Test
    fun `Previously unknown Assessor has an ExternalUser record created from details retrieved from Manage-Users API`() {
      externalUserRepository.deleteAll()

      val username = "PREVIOUSLY_UNKNOWN_ASSESSOR"
      val externalUserDetails = ExternalUserDetailsFactory()
        .withUsername(username)
        .produce()

      manageUsersMockSuccessfulExternalUsersCall(
        username = username,
        externalUserDetails = externalUserDetails,
      )

      val jwt = jwtAuthHelper.createValidExternalAuthorisationCodeJwt(username)

      webTestClient.get()
        .uri("/cas2/submissions/fea7986d-cae6-4a7a-8420-5b31376ce787")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound

      Assertions.assertThat(
        externalUserRepository.findByUsername("PREVIOUSLY_UNKNOWN_ASSESSOR"),
      ).isNotNull
    }

    @Test
    fun `Assessor can view single submitted application`() {
      givenACas2Assessor { assessor, jwt ->
        givenACas2PomUser { user, _ ->
          givenAnOffender { offenderDetails, _ ->
            cas2BailApplicationJsonSchemaRepository.deleteAll()

            val newestJsonSchema = cas2BailApplicationJsonSchemaEntityFactory
              .produceAndPersist {
                withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
                withSchema(
                  schema,
                )
              }

            val applicationEntity = cas2BailApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(newestJsonSchema)
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(user)
              withSubmittedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
              withData(
                """
            {
               "thingId": 123
            }
            """,
              )
            }

            val assessmentEntity = cas2BailAssessmentEntityFactory.produceAndPersist {
              withApplication(applicationEntity)
              withNacroReferralId("OH123")
              withAssessorName("Assessor name")
            }

            val update1 = cas2BailStatusUpdateEntityFactory.produceAndPersist {
              withApplication(applicationEntity)
              withAssessment(assessmentEntity)
              withAssessor(assessor)
              withLabel("1st update")
            }

            val update2 = cas2BailStatusUpdateEntityFactory.produceAndPersist {
              withApplication(applicationEntity)
              withAssessment(assessmentEntity)
              withAssessor(assessor)
              withLabel("2nd update")
            }

            val update3 = cas2BailStatusUpdateEntityFactory.produceAndPersist {
              withApplication(applicationEntity)
              withAssessment(assessmentEntity)
              withAssessor(assessor)
              withStatusId(UUID.fromString("9a381bc6-22d3-41d6-804d-4e49f428c1de"))
              withLabel("3rd update")
            }

            val statusUpdateDetail = Cas2BailStatusUpdateDetailEntity(
              id = UUID.fromString("5f89ec4d-1a3e-4ec3-a48b-52959d6fcc6a"),
              statusUpdate = update3,
              statusDetailId = UUID.fromString("62645779-242d-4601-a8f8-d2cbf1d41dfa"),
              label = "Detail on 3rd update",
            )

            update1.apply { this.createdAt = OffsetDateTime.now().minusDays(20) }
            cas2BailRealStatusUpdateRepository.save(update1)

            update2.apply { this.createdAt = OffsetDateTime.now().minusDays(15) }
            cas2BailRealStatusUpdateRepository.save(update2)

            update3.apply { this.createdAt = OffsetDateTime.now().minusDays(1) }
            cas2BailRealStatusUpdateRepository.save(update3)

            statusUpdateDetail.apply { this.createdAt = OffsetDateTime.now().minusDays(1) }
            cas2BailRealStatusUpdateDetailRepository.save(statusUpdateDetail)

            val rawResponseBody = webTestClient.get()
              .uri("/cas2/submissions/${applicationEntity.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody = objectMapper.readValue(
              rawResponseBody,
              Cas2SubmittedApplication::class.java,
            )

            val applicant = nomisUserTransformer.transformJpaToApi(
              applicationEntity
                .createdByUser,
            )

            Assertions.assertThat(responseBody).matches {
              applicationEntity.id == it.id &&
                applicationEntity.crn == it.person.crn &&
                applicationEntity.createdAt.toInstant() == it.createdAt &&
                applicant == it.submittedBy &&
                applicationEntity.submittedAt?.toInstant() == it.submittedAt &&
                serializableToJsonNode(applicationEntity.document) == serializableToJsonNode(it.document) &&
                newestJsonSchema.id == it.schemaVersion && !it.outdatedSchema &&
                assessmentEntity.assessorName == it.assessment.assessorName &&
                assessmentEntity.nacroReferralId == it.assessment.nacroReferralId
            }

            Assertions.assertThat(responseBody.assessment.statusUpdates!!.map { update -> update.label })
              .isEqualTo(listOf("3rd update", "2nd update", "1st update"))

            Assertions.assertThat(responseBody.assessment.statusUpdates!!.map { update -> update.label })
              .isEqualTo(listOf("3rd update", "2nd update", "1st update"))

            Assertions.assertThat(
              responseBody.assessment.statusUpdates!!.first().statusUpdateDetails!!
                .map { detail -> detail.label },
            )
              .isEqualTo(listOf("Detail on 3rd update"))

            Assertions.assertThat(responseBody.timelineEvents.map { event -> event.label })
              .isEqualTo(listOf("3rd update", "2nd update", "1st update", "Application submitted"))
          }
        }
      }
    }

    @Test
    fun `Assessor can NOT view single in-progress application`() {
      givenACas2Assessor { _, jwt ->
        givenACas2PomUser { user, _ ->
          givenAnOffender { offenderDetails, _ ->
            cas2BailApplicationJsonSchemaRepository.deleteAll()

            val newestJsonSchema = cas2BailApplicationJsonSchemaEntityFactory
              .produceAndPersist {
                withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
                withSchema(
                  schema,
                )
              }

            val applicationEntity = createInProgressApplication(
              newestJsonSchema,
              offenderDetails.otherIds.crn,
              user,
            )

            val rawResponseBody = webTestClient.get()
              .uri("/cas2/submissions/${applicationEntity.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isNotFound
          }
        }
      }
    }

    @Nested
    inner class ControlsOnCas2Admin {
      @Test
      fun `Admin can view single submitted application`() {
        givenACas2Assessor { assessor, _ ->
          givenACas2Admin { admin, jwt ->
            givenACas2PomUser { user, _ ->
              givenAnOffender { offenderDetails, _ ->
                cas2BailApplicationJsonSchemaRepository.deleteAll()

                val newestJsonSchema = cas2BailApplicationJsonSchemaEntityFactory
                  .produceAndPersist {
                    withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
                    withSchema(
                      schema,
                    )
                  }

                val applicationEntity = cas2BailApplicationEntityFactory.produceAndPersist {
                  withApplicationSchema(newestJsonSchema)
                  withCrn(offenderDetails.otherIds.crn)
                  withCreatedByUser(user)
                  withSubmittedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
                  withData(
                    """
                    {
                        "thingId": 123
                    }
                  """,
                  )
                }

                val assessmentEntity = cas2BailAssessmentEntityFactory.produceAndPersist {
                  withApplication(applicationEntity)
                  withNacroReferralId("OH123")
                  withAssessorName("Assessor name")
                }

                val update1 = cas2BailStatusUpdateEntityFactory.produceAndPersist {
                  withApplication(applicationEntity)
                  withAssessor(assessor)
                  withAssessment(assessmentEntity)
                  withLabel("1st update")
                }

                val update2 = cas2BailStatusUpdateEntityFactory.produceAndPersist {
                  withApplication(applicationEntity)
                  withAssessment(assessmentEntity)
                  withAssessor(assessor)
                  withLabel("2nd update")
                }

                val update3 = cas2BailStatusUpdateEntityFactory.produceAndPersist {
                  withApplication(applicationEntity)
                  withAssessment(assessmentEntity)
                  withAssessor(assessor)
                  withLabel("3rd update")
                }

                update1.apply { this.createdAt = OffsetDateTime.now().minusDays(20) }
                cas2BailRealStatusUpdateRepository.save(update1)

                update2.apply { this.createdAt = OffsetDateTime.now().minusDays(15) }
                cas2BailRealStatusUpdateRepository.save(update2)

                update3.apply { this.createdAt = OffsetDateTime.now().minusDays(1) }
                cas2BailRealStatusUpdateRepository.save(update3)

                val rawResponseBody = webTestClient.get()
                  .uri("/cas2/submissions/${applicationEntity.id}")
                  .header("Authorization", "Bearer $jwt")
                  .exchange()
                  .expectStatus()
                  .isOk
                  .returnResult<String>()
                  .responseBody
                  .blockFirst()

                val responseBody = objectMapper.readValue(
                  rawResponseBody,
                  Cas2SubmittedApplication::class.java,
                )

                val applicant = nomisUserTransformer.transformJpaToApi(
                  applicationEntity
                    .createdByUser,
                )

                Assertions.assertThat(responseBody).matches {
                  applicationEntity.id == it.id &&
                    applicationEntity.crn == it.person.crn &&
                    applicationEntity.createdAt.toInstant() == it.createdAt &&
                    applicant == it.submittedBy &&
                    applicationEntity.submittedAt?.toInstant() == it.submittedAt &&
                    serializableToJsonNode(applicationEntity.document) == serializableToJsonNode(it.document) &&
                    newestJsonSchema.id == it.schemaVersion && !it.outdatedSchema
                }

                Assertions.assertThat(responseBody.assessment.statusUpdates!!.map { update -> update.label })
                  .isEqualTo(listOf("3rd update", "2nd update", "1st update"))

                Assertions.assertThat(responseBody.timelineEvents.map { event -> event.label })
                  .isEqualTo(listOf("3rd update", "2nd update", "1st update", "Application submitted"))
              }
            }
          }
        }
      }

      @Test
      fun `Admin can NOT view single in-progress application`() {
        givenACas2Admin { _, jwt ->
          givenACas2PomUser { user, _ ->
            givenAnOffender { offenderDetails, _ ->
              cas2BailApplicationJsonSchemaRepository.deleteAll()

              val newestJsonSchema = cas2BailApplicationJsonSchemaEntityFactory
                .produceAndPersist {
                  withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
                  withSchema(
                    schema,
                  )
                }

              val applicationEntity = createInProgressApplication(
                newestJsonSchema,
                offenderDetails.otherIds.crn,
                user,
              )

              val rawResponseBody = webTestClient.get()
                .uri("/cas2/submissions/${applicationEntity.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isNotFound
            }
          }
        }
      }
    }
  }

  @Nested
  inner class PostToSubmit {

    @Test
    fun `Submit Cas2 application returns 200`() {
      val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")
      val telephoneNumber = "123 456 7891"

      givenACas2PomUser { submittingUser, jwt ->
        givenAnOffender(
          inmateDetailsConfigBlock = {
            withAssignedLivingUnit(
              AssignedLivingUnit(
                agencyId = "agency_id",
                locationId = 123.toLong(),
                agencyName = "agency_name",
                description = null,
              ),
            )
          },
        ) { offenderDetails, _ ->

          val applicationSchema =
            cas2BailApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.now())
              withId(UUID.randomUUID())
              withSchema(
                schema,
              )
            }

          cas2BailApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withNomsNumber(offenderDetails.otherIds.nomsNumber.toString())
            withId(applicationId)
            withApplicationSchema(applicationSchema)
            withCreatedByUser(submittingUser)
            withData(
              """
                        {
                           "thingId": 123
                        }
               """,
            )
          }

          Assertions.assertThat(cas2BailRealAssessmentRepository.count()).isEqualTo(0)

          webTestClient.post()
            .uri("/cas2/submissions")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.cas2.value)
            .bodyValue(
              SubmitCas2Application(
                applicationId = applicationId,
                translatedDocument = {},
                preferredAreas = "Leeds | Bradford",
                hdcEligibilityDate = LocalDate.parse("2023-03-30"),
                conditionalReleaseDate = LocalDate.parse("2023-04-29"),
                telephoneNumber = telephoneNumber,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
        }

        // verify that generated 'application.submitted' domain event links to the CAS2 domain
        val expectedFrontEndUrl = applicationUrlTemplate.replace("#id", applicationId.toString())
        val persistedDomainEvent = domainEventRepository.findFirstByOrderByCreatedAtDesc()
        val domainEventFromJson = objectMapper.readValue(
          persistedDomainEvent!!.data,
          Cas2ApplicationSubmittedEvent::class.java,
        )
        Assertions.assertThat(domainEventFromJson.eventDetails.applicationUrl)
          .isEqualTo(expectedFrontEndUrl)

        val persistedAssessment = cas2BailRealAssessmentRepository.findAll().first()
        Assertions.assertThat(persistedAssessment!!.application.id).isEqualTo(applicationId)

        val expectedEmailUrl = submittedApplicationUrlTemplate.replace("#applicationId", applicationId.toString())
        emailAsserter.assertEmailsRequestedCount(1)
        emailAsserter.assertEmailRequested(
          notifyConfig.emailAddresses.cas2Assessors,
          notifyConfig.templates.cas2ApplicationSubmitted,
          personalisation = mapOf(
            "name" to submittingUser.name,
            "email" to submittingUser.email!!,
            "prisonNumber" to persistedAssessment.application.nomsNumber!!,
            "telephoneNumber" to telephoneNumber,
            "applicationUrl" to expectedEmailUrl,
          ),
          replyToEmailId = notifyConfig.emailAddresses.cas2ReplyToId,
        )
      }
    }

    @Test
    fun `When several concurrent submit application requests occur, only one is successful, all others return 400`() {
      givenACas2PomUser { submittingUser, jwt ->
        givenAnOffender(
          inmateDetailsConfigBlock = {
            withAssignedLivingUnit(
              AssignedLivingUnit(
                agencyId = "agency_id",
                locationId = 123.toLong(),
                agencyName = "agency_name",
                description = null,
              ),
            )
          },
        ) { offenderDetails, _ ->
          val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

          val applicationSchema = cas2BailApplicationJsonSchemaEntityFactory
            .produceAndPersist {
              withAddedAt(OffsetDateTime.now())
              withId(UUID.randomUUID())
              withSchema(
                schema,
              )
            }

          cas2BailApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withNomsNumber(offenderDetails.otherIds.nomsNumber.toString())
            withId(applicationId)
            withApplicationSchema(applicationSchema)
            withCreatedByUser(submittingUser)
            withData(
              """
            {
               "thingId": 123
            }
            """,
            )
          }

          every { cas2BailRealApplicationRepository.save(any()) } answers {
            Thread.sleep(1000)
            it.invocation.args[0] as Cas2BailApplicationEntity
          }

          val responseStatuses = mutableListOf<HttpStatus>()

          (1..10).map {
            val thread = Thread {
              webTestClient.post()
                .uri("/cas2/submissions")
                .header("Authorization", "Bearer $jwt")
                .bodyValue(
                  SubmitCas2Application(
                    applicationId = applicationId,
                    translatedDocument = {},
                    telephoneNumber = "123 456 7891",
                  ),
                )
                .exchange()
                .returnResult<String>()
                .consumeWith {
                  synchronized(responseStatuses) {
                    responseStatuses += it.status.toHttpStatus()
                  }
                }
            }

            thread.start()

            thread
          }.forEach(Thread::join)

          Assertions.assertThat(responseStatuses.count { it.value() == 200 }).isEqualTo(1)
          Assertions.assertThat(responseStatuses.count { it.value() == 400 }).isEqualTo(9)
        }
      }
    }

    @Test
    fun `When there's an error fetching the referred person's prison code, the application is not saved`() {
      givenACas2PomUser { submittingUser, jwt ->
        givenAnOffender(mockNotFoundErrorForPrisonApi = true) { offenderDetails, _ ->
          val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

          val applicationSchema = cas2BailApplicationJsonSchemaEntityFactory
            .produceAndPersist {
              withAddedAt(OffsetDateTime.now())
              withId(UUID.randomUUID())
              withSchema(
                schema,
              )
            }

          cas2BailApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withNomsNumber(offenderDetails.otherIds.nomsNumber!!)
            withId(applicationId)
            withApplicationSchema(applicationSchema)
            withCreatedByUser(submittingUser)
            withData(
              """
            {
               "thingId": 123
            }
            """,
            )
          }

          Assertions.assertThat(domainEventRepository.count()).isEqualTo(0)
          Assertions.assertThat(cas2BailRealAssessmentRepository.count()).isEqualTo(0)

          webTestClient.post()
            .uri("/cas2/submissions")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.cas2.value)
            .bodyValue(
              SubmitCas2Application(
                applicationId = applicationId,
                translatedDocument = {},
                preferredAreas = "Leeds | Bradford",
                hdcEligibilityDate = LocalDate.parse("2023-03-30"),
                conditionalReleaseDate = LocalDate.parse("2023-04-29"),
                telephoneNumber = "123 456 789",
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest

          Assertions.assertThat(domainEventRepository.count()).isEqualTo(0)
          Assertions.assertThat(cas2BailRealAssessmentRepository.count()).isEqualTo(0)
          Assertions.assertThat(cas2BailRealApplicationRepository.findById(applicationId).get().submittedAt).isNull()
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
