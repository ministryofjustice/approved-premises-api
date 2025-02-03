package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearMocks
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationTimelineNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FlagsEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MappaEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OfflineApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RestrictedPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskEnvelopeStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RoshRisksEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UnknownPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplicationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.toHttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedsDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TeamFactoryDeliusContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulCaseDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulTeamsManagingCaseCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockUserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockSuccessfulNeedsDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ManagingTeamsResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationStatus as ApiApprovedPremisesApplicationStatus

class ApplicationTest : IntegrationTestBase() {
  @Autowired
  lateinit var assessmentTransformer: AssessmentTransformer

  @SpykBean
  lateinit var realApplicationTeamCodeRepository: ApplicationTeamCodeRepository

  @SpykBean
  lateinit var realApplicationRepository: ApplicationRepository

  @AfterEach
  fun afterEach() {
    // SpringMockK does not correctly clear mocks for @SpyKBeans that are also a @Repository, causing mocked behaviour
    // in one test to show up in another (see https://github.com/Ninja-Squad/springmockk/issues/85)
    // Manually clearing after each test seems to fix this.
    clearMocks(realApplicationTeamCodeRepository)
    clearMocks(realApplicationRepository)
  }

  @Nested
  inner class GetApplications {

    @Test
    fun `Get all applications without JWT returns 401`() {
      webTestClient.get()
        .uri("/applications")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get all applications returns 200 - when user has no roles returns applications managed by their teams`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(teams = listOf(TeamFactoryDeliusContext.team(code = "TEAM1"))),
      ) { userEntity, jwt ->
        givenAUser { otherUser, _ ->
          givenAnOffender { offenderDetails, _ ->
            givenAnOffender { otherOffenderDetails, _ ->
              approvedPremisesApplicationJsonSchemaRepository.deleteAll()

              val newestJsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
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

              val olderJsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
                withAddedAt(OffsetDateTime.parse("2022-09-21T09:45:00+01:00"))
                withSchema(
                  """
              {
                "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
                "${"\$id"}": "https://example.com/product.schema.json",
                "title": "Thing",
                "description": "A thing",
                "type": "object",
                "properties": { }
              }
            """,
                )
              }

              val upToDateApplicationEntityManagedByTeam = approvedPremisesApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(newestJsonSchema)
                withCrn(offenderDetails.otherIds.crn)
                withCreatedByUser(userEntity)
                withData(
                  """
                {
                   "thingId": 123
                }
              """,
                )
              }

              upToDateApplicationEntityManagedByTeam.teamCodes += applicationTeamCodeRepository.save(
                ApplicationTeamCodeEntity(
                  id = UUID.randomUUID(),
                  application = upToDateApplicationEntityManagedByTeam,
                  teamCode = "TEAM1",
                ),
              )

              val outdatedApplicationEntityManagedByTeam = approvedPremisesApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(olderJsonSchema)
                withCreatedByUser(userEntity)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
              }

              outdatedApplicationEntityManagedByTeam.teamCodes += applicationTeamCodeRepository.save(
                ApplicationTeamCodeEntity(
                  id = UUID.randomUUID(),
                  application = outdatedApplicationEntityManagedByTeam,
                  teamCode = "TEAM1",
                ),
              )

              val outdatedApplicationEntityNotManagedByTeam =
                approvedPremisesApplicationEntityFactory.produceAndPersist {
                  withApplicationSchema(olderJsonSchema)
                  withCreatedByUser(otherUser)
                  withCrn(otherOffenderDetails.otherIds.crn)
                  withData("{}")
                }

              apDeliusContextAddResponseToUserAccessCall(
                listOf(
                  CaseAccessFactory()
                    .withCrn(offenderDetails.otherIds.crn)
                    .produce(),
                ),
                userEntity.deliusUsername,
              )

              val responseBody = webTestClient.get()
                .uri("/applications")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .bodyAsListOfObjects<ApprovedPremisesApplicationSummary>()

              assertThat(responseBody).anyMatch {
                outdatedApplicationEntityManagedByTeam.id == it.id &&
                  outdatedApplicationEntityManagedByTeam.crn == it.person.crn &&
                  outdatedApplicationEntityManagedByTeam.createdAt.toInstant() == it.createdAt &&
                  outdatedApplicationEntityManagedByTeam.createdByUser.id == it.createdByUserId &&
                  outdatedApplicationEntityManagedByTeam.submittedAt?.toInstant() == it.submittedAt
              }

              assertThat(responseBody).anyMatch {
                upToDateApplicationEntityManagedByTeam.id == it.id &&
                  upToDateApplicationEntityManagedByTeam.crn == it.person.crn &&
                  upToDateApplicationEntityManagedByTeam.createdAt.toInstant() == it.createdAt &&
                  upToDateApplicationEntityManagedByTeam.createdByUser.id == it.createdByUserId &&
                  upToDateApplicationEntityManagedByTeam.submittedAt?.toInstant() == it.submittedAt
              }

              assertThat(responseBody).noneMatch {
                outdatedApplicationEntityNotManagedByTeam.id == it.id
              }
            }
          }
        }
      }
    }

    private fun createTempApplicationEntity(
      applicationSchema: TemporaryAccommodationApplicationJsonSchemaEntity,
      user: UserEntity,
      offenderDetails: OffenderDetailSummary,
      probationRegion: ProbationRegionEntity,
      submittedAt: OffsetDateTime?,
    ): TemporaryAccommodationApplicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(applicationSchema)
      withCreatedByUser(user)
      withSubmittedAt(submittedAt)
      withCrn(offenderDetails.otherIds.crn)
      withData("{}")
      withProbationRegion(probationRegion)
    }

    @ParameterizedTest
    @EnumSource(UserRole::class, names = ["CAS3_REFERRER", "CAS3_ASSESSOR"])
    fun `Get all applications returns 200 for TA - returns all applications for user`(userRole: UserRole) {
      givenAProbationRegion { probationRegion ->
        givenAUser(roles = listOf(userRole), probationRegion = probationRegion) { otherUser, _ ->
          givenAUser(
            roles = listOf(UserRole.CAS3_REFERRER),
            probationRegion = probationRegion,
          ) { referrerUser, jwt ->
            givenAnOffender { offenderDetails, _ ->
              temporaryAccommodationApplicationJsonSchemaRepository.deleteAll()

              val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
                withAddedAt(OffsetDateTime.now())
                withId(UUID.randomUUID())
              }

              val application =
                createTempApplicationEntity(applicationSchema, referrerUser, offenderDetails, probationRegion, null)

              val anotherUsersApplication =
                createTempApplicationEntity(applicationSchema, otherUser, offenderDetails, probationRegion, null)

              apDeliusContextAddResponseToUserAccessCall(
                listOf(
                  CaseAccessFactory()
                    .withCrn(offenderDetails.otherIds.crn)
                    .produce(),
                ),
                referrerUser.deliusUsername,
              )

              val responseBody = webTestClient.get()
                .uri("/applications")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
                .exchange()
                .expectStatus()
                .isOk
                .bodyAsListOfObjects<TemporaryAccommodationApplicationSummary>()

              assertThat(responseBody).anyMatch {
                application.id == it.id &&
                  application.crn == it.person.crn &&
                  application.createdAt.toInstant() == it.createdAt &&
                  application.createdByUser.id == it.createdByUserId &&
                  application.submittedAt?.toInstant() == it.submittedAt
              }

              assertThat(responseBody).noneMatch {
                anotherUsersApplication.id == it.id
              }
            }
          }
        }
      }
    }

    @Test
    fun `Get all applications returns limited information when a person cannot be found`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(teams = listOf(TeamFactoryDeliusContext.team(code = "TEAM1"))),
      ) { userEntity, jwt ->
        val crn = "X1234"

        val application = produceAndPersistBasicApplication(crn, userEntity, "TEAM1")

        apDeliusContextMockUserAccess(
          CaseAccessFactory()
            .withCrn(crn)
            .produce(),
          userEntity.deliusUsername,
        )

        webTestClient.get()
          .uri("/applications")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              listOf(
                ApprovedPremisesApplicationSummary(
                  createdByUserId = userEntity.id,
                  status = ApiApprovedPremisesApplicationStatus.started,
                  type = "CAS1",
                  id = application.id,
                  person = UnknownPerson(
                    crn = crn,
                    type = PersonType.unknownPerson,
                  ),
                  createdAt = application.createdAt.toInstant(),
                  isWomensApplication = null,
                  isPipeApplication = false,
                  isEmergencyApplication = null,
                  isEsapApplication = null,
                  arrivalDate = null,
                  risks = PersonRisks(
                    crn = crn,
                    roshRisks = RoshRisksEnvelope(RiskEnvelopeStatus.notFound),
                    tier = RiskTierEnvelope(RiskEnvelopeStatus.notFound),
                    flags = FlagsEnvelope(RiskEnvelopeStatus.notFound),
                    mappa = MappaEnvelope(RiskEnvelopeStatus.notFound),
                  ),
                  submittedAt = null,
                  isWithdrawn = false,
                  hasRequestsForPlacement = false,
                ),
              ),
            ),
          )
      }
    }

    @Test
    fun `Get all applications returns successfully when a person has no NOMS number`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(teams = listOf(TeamFactoryDeliusContext.team(code = "TEAM1"))),
      ) { userEntity, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = { withoutNomsNumber() },
        ) { offenderDetails, _ ->
          val application = produceAndPersistBasicApplication(offenderDetails.otherIds.crn, userEntity, "TEAM1")

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            userEntity.deliusUsername,
          )

          val responseBody = webTestClient.get()
            .uri("/applications")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .bodyAsListOfObjects<ApprovedPremisesApplicationSummary>()

          assertThat(responseBody).matches {
            val person = it[0].person as FullPerson

            application.id == it[0].id &&
              application.crn == person.crn &&
              person.nomsNumber == null &&
              person.status == PersonStatus.unknown &&
              person.prisonName == null
          }
        }
      }
    }

    @Test
    fun `Get all applications returns successfully when the person cannot be fetched from the prisons API`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(teams = listOf(TeamFactoryDeliusContext.team(code = "TEAM1"))),
      ) { userEntity, jwt ->
        val crn = "X1234"

        givenAnOffender(
          offenderDetailsConfigBlock = {
            withCrn(crn)
            withNomsNumber("ABC123")
          },
        ) { _, _ ->
          val application = produceAndPersistBasicApplication(crn, userEntity, "TEAM1")

          val responseBody = webTestClient.get()
            .uri("/applications")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .bodyAsListOfObjects<ApprovedPremisesApplicationSummary>()

          assertThat(responseBody).matches {
            val person = it[0].person as FullPerson

            application.id == it[0].id &&
              application.crn == person.crn &&
              person.nomsNumber == null &&
              person.status == PersonStatus.unknown &&
              person.prisonName == null
          }
        }
      }
    }

    @Test
    fun `Get single application without JWT returns 401`() {
      webTestClient.get()
        .uri("/applications/9b785e59-b85c-4be0-b271-d9ac287684b6")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  @Nested
  inner class Cas1GetApplication {

    @Test
    fun `Get single non LAO application returns 200 with correct body`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val newestJsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          val applicationEntity = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(newestJsonSchema)
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(userEntity)
            withData("""{"thingId":123}""")
          }

          val rawResponseBody = webTestClient.get()
            .uri("/applications/${applicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody = objectMapper.readValue(rawResponseBody, ApprovedPremisesApplication::class.java)

          assertThat(responseBody.id).isEqualTo(applicationEntity.id)
          assertThat(responseBody.person.crn).isEqualTo(applicationEntity.crn)
          assertThat(responseBody.createdAt).isEqualTo(applicationEntity.createdAt.toInstant())
          assertThat(responseBody.createdByUserId).isEqualTo(applicationEntity.createdByUser.id)
          assertThat(responseBody.submittedAt).isEqualTo(applicationEntity.submittedAt?.toInstant())
          assertThat(serializableToJsonNode(responseBody.data)).isEqualTo(serializableToJsonNode(applicationEntity.data))
          assertThat(responseBody.schemaVersion).isEqualTo(applicationEntity.schemaVersion.id)
        }
      }
    }

    @Test
    fun `Get single LAO application for creator, LAO Access, no LAO Qualification returns 200`() {
      givenAUser { applicationCreator, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = {
            withCurrentRestriction(true)
          },
        ) { offenderDetails, _ ->
          val applicationEntity = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(
              approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
            )
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(applicationCreator)
            withData(
              """
          {
             "thingId": 123
          }
          """,
            )
          }

          apDeliusContextMockUserAccess(
            caseAccess = CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .withUserExcluded(false)
              .withUserRestricted(false)
              .produce(),
            username = applicationCreator.deliusUsername,
          )

          val rawResponseBody = webTestClient.get()
            .uri("/applications/${applicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody = objectMapper.readValue(rawResponseBody, ApprovedPremisesApplication::class.java)

          assertThat(responseBody.person).isInstanceOf(FullPerson::class.java)
        }
      }
    }

    @Test
    fun `Get single LAO application for creator, no LAO Access, has LAO Qualification returns 200`() {
      givenAUser(qualifications = listOf(UserQualification.LAO)) { applicationCreator, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = {
            withCurrentRestriction(true)
          },
        ) { offenderDetails, _ ->
          val applicationEntity = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(
              approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
            )
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(applicationCreator)
            withData(
              """
          {
             "thingId": 123
          }
          """,
            )
          }

          val rawResponseBody = webTestClient.get()
            .uri("/applications/${applicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody = objectMapper.readValue(rawResponseBody, ApprovedPremisesApplication::class.java)

          assertThat(responseBody.person).isInstanceOf(FullPerson::class.java)
        }
      }
    }

    @Test
    fun `Get single LAO application for non creator, no LAO Access, no LAO Qualification returns 403`() {
      givenAUser { applicationCreator, _ ->
        givenAUser { _, otherUserJwt ->
          givenAnOffender(
            offenderDetailsConfigBlock = {
              withCurrentRestriction(true)
            },
          ) { offenderDetails, _ ->
            val applicationEntity = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(
                approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
              )
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(applicationCreator)
              withData(
                """
          {
             "thingId": 123
          }
          """,
              )
            }

            webTestClient.get()
              .uri("/applications/${applicationEntity.id}")
              .header("Authorization", "Bearer $otherUserJwt")
              .exchange()
              .expectStatus()
              .isForbidden
          }
        }
      }
    }

    @Test
    fun `Get single LAO application for non creator, no LAO Access, has LAO Qualification returns 200`() {
      givenAUser { applicationCreator, _ ->
        givenAUser(qualifications = listOf(UserQualification.LAO)) { _, otherUserJwt ->
          givenAnOffender(
            offenderDetailsConfigBlock = {
              withCurrentRestriction(true)
            },
          ) { offenderDetails, _ ->
            val applicationEntity = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(
                approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
              )
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(applicationCreator)
              withData(
                """
          {
             "thingId": 123
          }
          """,
              )
            }

            val rawResponseBody = webTestClient.get()
              .uri("/applications/${applicationEntity.id}")
              .header("Authorization", "Bearer $otherUserJwt")
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody = objectMapper.readValue(rawResponseBody, ApprovedPremisesApplication::class.java)

            assertThat(responseBody.person).isInstanceOf(FullPerson::class.java)
          }
        }
      }
    }

    @Test
    fun `Get single non LAO application for non creator returns 200`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(teams = listOf(TeamFactoryDeliusContext.team(code = "TEAM2"))),
      ) { _, jwt ->
        givenAUser { otherUser, _ ->
          val crn = "X1234"
          givenAnOffender(offenderDetailsConfigBlock = { withCrn(crn) }) { offenderDetails, _ ->
            val application = produceAndPersistBasicApplication(crn, otherUser, "TEAM1")

            val rawResponseBody = webTestClient.get()
              .uri("/applications/${application.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody = objectMapper.readValue(rawResponseBody, ApprovedPremisesApplication::class.java)

            assertThat(responseBody.person).isInstanceOf(FullPerson::class.java)
          }
        }
      }
    }

    @Test
    fun `Get single non LAO application returns 200 when a person has no NOMS number`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(teams = listOf(TeamFactoryDeliusContext.team(code = "TEAM1"))),
      ) { userEntity, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = { withoutNomsNumber() },
        ) { offenderDetails, _ ->
          val application = produceAndPersistBasicApplication(offenderDetails.otherIds.crn, userEntity, "TEAM1")

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            userEntity.deliusUsername,
          )

          val rawResponseBody = webTestClient.get()
            .uri("/applications/${application.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody = objectMapper.readValue(rawResponseBody, ApprovedPremisesApplication::class.java)

          assertThat(responseBody.person is FullPerson).isTrue
          assertThat(responseBody).matches {
            val person = it.person as FullPerson

            application.id == it.id &&
              application.crn == person.crn &&
              person.nomsNumber == null &&
              person.status == PersonStatus.unknown &&
              person.prisonName == null
          }
        }
      }
    }

    @Test
    fun `Get single non LAO application returns 200 when the person cannot be fetched from the prisons API`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(teams = listOf(TeamFactoryDeliusContext.team(code = "TEAM1"))),
      ) { userEntity, jwt ->
        val crn = "X1234"

        givenAnOffender(
          offenderDetailsConfigBlock = {
            withCrn(crn)
            withNomsNumber("ABC123")
          },
        ) { _, _ ->
          val application = produceAndPersistBasicApplication(crn, userEntity, "TEAM1")

          val rawResponseBody = webTestClient.get()
            .uri("/applications/${application.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody = objectMapper.readValue(rawResponseBody, ApprovedPremisesApplication::class.java)

          assertThat(responseBody.person is FullPerson).isTrue

          assertThat(responseBody).matches {
            val person = it.person as FullPerson

            application.id == it.id &&
              application.crn == person.crn &&
              person.nomsNumber == null &&
              person.status == PersonStatus.unknown &&
              person.prisonName == null
          }
        }
      }
    }

    @Test
    fun `Get single online application returns 200 non-upgradable outdated application marked as such`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          approvedPremisesApplicationJsonSchemaRepository.deleteAll()

          approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
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

          val olderJsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.parse("2022-09-21T09:45:00+01:00"))
            withSchema(
              """
        {
          "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
          "${"\$id"}": "https://example.com/product.schema.json",
          "title": "Thing",
          "description": "A thing",
          "type": "object",
          "properties": { }
        }
        """,
            )
          }

          val nonUpgradableApplicationEntity = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(olderJsonSchema)
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(userEntity)
            withData("{}")
          }

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            userEntity.deliusUsername,
          )

          val rawResponseBody = webTestClient.get()
            .uri("/applications/${nonUpgradableApplicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody = objectMapper.readValue(rawResponseBody, ApprovedPremisesApplication::class.java)

          assertThat(responseBody).matches {
            nonUpgradableApplicationEntity.id == it.id &&
              nonUpgradableApplicationEntity.crn == it.person.crn &&
              nonUpgradableApplicationEntity.createdAt.toInstant() == it.createdAt &&
              nonUpgradableApplicationEntity.createdByUser.id == it.createdByUserId &&
              nonUpgradableApplicationEntity.submittedAt?.toInstant() == it.submittedAt &&
              serializableToJsonNode(nonUpgradableApplicationEntity.data) == serializableToJsonNode(it.data) &&
              olderJsonSchema.id == it.schemaVersion &&
              it.outdatedSchema
          }
        }
      }
    }
  }

  @Nested
  inner class Cas3GetApplication {

    @Test
    fun `Get single application returns 200 with correct body when requesting user created application`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          temporaryAccommodationApplicationJsonSchemaRepository.deleteAll()

          val newestJsonSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
            withSchema("{}")
          }

          val applicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(newestJsonSchema)
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(userEntity)
            withProbationRegion(userEntity.probationRegion)
            withData(
              """
            {
               "thingId": 123
            }
            """,
            )
          }

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            userEntity.deliusUsername,
          )

          val rawResponseBody = webTestClient.get()
            .uri("/applications/${applicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody = objectMapper.readValue(rawResponseBody, TemporaryAccommodationApplication::class.java)

          assertThat(responseBody).matches {
            applicationEntity.id == it.id &&
              applicationEntity.crn == it.person.crn &&
              applicationEntity.createdAt.toInstant() == it.createdAt &&
              applicationEntity.createdByUser.id == it.createdByUserId &&
              applicationEntity.submittedAt?.toInstant() == it.submittedAt &&
              serializableToJsonNode(applicationEntity.data) == serializableToJsonNode(it.data) &&
              newestJsonSchema.id == it.schemaVersion &&
              !it.outdatedSchema
          }
        }
      }
    }

    @Test
    fun `Get single application returns 200 with correct body when a user with the CAS3_ASSESSOR role requests a submitted application in their region`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAUser(probationRegion = userEntity.probationRegion) { createdByUser, _ ->
          givenAnOffender { offenderDetails, _ ->
            temporaryAccommodationApplicationJsonSchemaRepository.deleteAll()

            val newestJsonSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
              withSchema("{}")
            }

            val applicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(newestJsonSchema)
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(createdByUser)
              withProbationRegion(createdByUser.probationRegion)
              withSubmittedAt(OffsetDateTime.parse("2023-06-01T12:34:56.789+01:00"))
              withData(
                """
              {
                 "thingId": 123
              }
              """,
              )
            }

            apDeliusContextAddResponseToUserAccessCall(
              listOf(
                CaseAccessFactory()
                  .withCrn(offenderDetails.otherIds.crn)
                  .produce(),
              ),
              userEntity.deliusUsername,
            )

            val rawResponseBody = webTestClient.get()
              .uri("/applications/${applicationEntity.id}")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody = objectMapper.readValue(rawResponseBody, TemporaryAccommodationApplication::class.java)

            assertThat(responseBody).matches {
              applicationEntity.id == it.id &&
                applicationEntity.crn == it.person.crn &&
                applicationEntity.createdAt.toInstant() == it.createdAt &&
                applicationEntity.createdByUser.id == it.createdByUserId &&
                applicationEntity.submittedAt?.toInstant() == it.submittedAt &&
                serializableToJsonNode(applicationEntity.data) == serializableToJsonNode(it.data) &&
                newestJsonSchema.id == it.schemaVersion &&
                !it.outdatedSchema
            }
          }
        }
      }
    }

    @Test
    fun `Get single LAO application for application creator with LAO access returns 200`() {
      givenAUser { createdByUser, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = {
            withCurrentRestriction(true)
          },
        ) { offenderDetails, _ ->
          temporaryAccommodationApplicationJsonSchemaRepository.deleteAll()

          val newestJsonSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
            withSchema("{}")
          }

          val applicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(newestJsonSchema)
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(createdByUser)
            withProbationRegion(createdByUser.probationRegion)
            withSubmittedAt(OffsetDateTime.parse("2023-06-01T12:34:56.789+01:00"))
            withData(
              """
            {
               "thingId": 123
            }
            """,
            )
          }

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            createdByUser.deliusUsername,
          )

          val rawResponseBody = webTestClient.get()
            .uri("/applications/${applicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody = objectMapper.readValue(rawResponseBody, TemporaryAccommodationApplication::class.java)

          assertThat(responseBody.person).isInstanceOf(FullPerson::class.java)
        }
      }
    }

    @Test
    fun `Get single LAO application for user who is not creator but has LAO Qualification returns RestrictedPerson`() {
      givenAUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        qualifications = listOf(UserQualification.LAO),
      ) { otherUser, otherUserJwt ->
        givenAUser(probationRegion = otherUser.probationRegion) { createdByUser, _ ->
          givenAnOffender(
            offenderDetailsConfigBlock = {
              withCurrentRestriction(true)
            },
          ) { offenderDetails, _ ->
            temporaryAccommodationApplicationJsonSchemaRepository.deleteAll()

            val newestJsonSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
              withSchema("{}")
            }

            val applicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(newestJsonSchema)
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(createdByUser)
              withProbationRegion(createdByUser.probationRegion)
              withSubmittedAt(OffsetDateTime.parse("2023-06-01T12:34:56.789+01:00"))
              withData(
                """
              {
                 "thingId": 123
              }
              """,
              )
            }

            val result = webTestClient.get()
              .uri("/applications/${applicationEntity.id}")
              .header("Authorization", "Bearer $otherUserJwt")
              .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectBody(TemporaryAccommodationApplication::class.java)
              .returnResult()
              .responseBody

            assertThat(result!!.person.type).isEqualTo(PersonType.restrictedPerson)
          }
        }
      }
    }

    @Test
    fun `Get single application returns 403 Forbidden when a user with the CAS3_ASSESSOR role requests an application not in their region`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAUser { createdByUser, _ ->
          givenAnOffender { offenderDetails, _ ->
            temporaryAccommodationApplicationJsonSchemaRepository.deleteAll()

            val newestJsonSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
              withSchema("{}")
            }

            val applicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(newestJsonSchema)
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(createdByUser)
              withProbationRegion(createdByUser.probationRegion)
              withSubmittedAt(OffsetDateTime.now())
              withData(
                """
              {
                 "thingId": 123
              }
              """,
              )
            }

            apDeliusContextAddResponseToUserAccessCall(
              listOf(
                CaseAccessFactory()
                  .withCrn(offenderDetails.otherIds.crn)
                  .produce(),
              ),
              userEntity.deliusUsername,
            )

            webTestClient.get()
              .uri("/applications/${applicationEntity.id}")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
              .exchange()
              .expectStatus()
              .isForbidden
          }
        }
      }
    }

    @Test
    fun `Get single application returns 403 Forbidden when a user without the CAS3_ASSESSOR role requests an application not created by them`() {
      givenAUser { userEntity, jwt ->
        givenAUser(probationRegion = userEntity.probationRegion) { createdByUser, _ ->
          givenAnOffender { offenderDetails, _ ->
            temporaryAccommodationApplicationJsonSchemaRepository.deleteAll()

            val newestJsonSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
              withSchema("{}")
            }

            val applicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(newestJsonSchema)
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(createdByUser)
              withProbationRegion(createdByUser.probationRegion)
              withSubmittedAt(OffsetDateTime.now())
              withData(
                """
              {
                 "thingId": 123
              }
              """,
              )
            }

            apDeliusContextAddResponseToUserAccessCall(
              listOf(
                CaseAccessFactory()
                  .withCrn(offenderDetails.otherIds.crn)
                  .produce(),
              ),
              userEntity.deliusUsername,
            )

            webTestClient.get()
              .uri("/applications/${applicationEntity.id}")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
              .exchange()
              .expectStatus()
              .isForbidden
          }
        }
      }
    }

    @Test
    fun `Get single CAS3 application returns 404 Not Found when the application was deleted`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          temporaryAccommodationApplicationJsonSchemaRepository.deleteAll()

          val newestJsonSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.parse("2024-12-11T13:21:00+01:00"))
            withSchema("{}")
          }

          val applicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(newestJsonSchema)
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(userEntity)
            withProbationRegion(userEntity.probationRegion)
            withDeletedAt(OffsetDateTime.now().minusDays(15))
            withData(
              """
            {
               "thingId": 123
            }
            """,
            )
          }

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            userEntity.deliusUsername,
          )

          webTestClient.get()
            .uri("/applications/${applicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isNotFound
        }
      }
    }
  }

  @Test
  fun `Get single offline application returns 200 with correct body`() {
    givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val offlineApplicationEntity = offlineApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
        }

        apDeliusContextAddResponseToUserAccessCall(
          listOf(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .produce(),
          ),
          userEntity.deliusUsername,
        )

        val rawResponseBody = webTestClient.get()
          .uri("/applications/${offlineApplicationEntity.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .returnResult<String>()
          .responseBody
          .blockFirst()

        val responseBody = objectMapper.readValue(rawResponseBody, OfflineApplication::class.java)

        assertThat(responseBody).matches {
          offlineApplicationEntity.id == it.id &&
            offlineApplicationEntity.crn == it.person.crn &&
            offlineApplicationEntity.createdAt.toInstant() == it.createdAt
        }
      }
    }
  }

  @Test
  fun `GET submitted CAS3 application includes assessmentId in the response`() {
    givenAUser { submittingUser, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")
        val offenderName = "${offenderDetails.firstName} ${offenderDetails.surname}"

        val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
          withAddedAt(OffsetDateTime.now())
          withId(UUID.randomUUID())
          withSchema(schemaText())
        }

        temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withId(applicationId)
          withApplicationSchema(applicationSchema)
          withCreatedByUser(submittingUser)
          withProbationRegion(submittingUser.probationRegion)
          withName(offenderName)
          withData("{}")
        }

        webTestClient.post()
          .uri("/applications/$applicationId/submission")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            SubmitTemporaryAccommodationApplication(
              translatedDocument = {},
              type = "CAS3",
              arrivalDate = LocalDate.now(),
              summaryData = object {
                val num = 50
                val text = "Hello world!"
              },
            ),
          )
          .exchange()
          .expectStatus()
          .isOk

        val result = webTestClient.get()
          .uri("/applications/$applicationId")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody(TemporaryAccommodationApplication::class.java)
          .returnResult()
          .responseBody

        assertThat(result!!.assessmentId).isNotNull()
      }
    }
  }

  @Nested
  inner class Cas1CreateApplication {

    @Test
    fun `Create new application without JWT returns 401`() {
      webTestClient.post()
        .uri("/applications")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Create new application returns 404 when a person cannot be found`() {
      givenAUser { _, jwt ->
        val crn = "X1234"

        approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withAddedAt(OffsetDateTime.now())
          withId(UUID.randomUUID())
        }

        webTestClient.post()
          .uri("/applications")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewApplication(
              crn = crn,
            ),
          )
          .exchange()
          .expectStatus()
          .isNotFound
          .expectBody()
          .jsonPath("$.detail").isEqualTo("No Offender with an ID of $crn could be found")
      }
    }

    @Test
    fun `Create new application returns 500 and does not create Application without team codes when write to team code table fails`() {
      givenAUser { _, jwt ->
        givenAnOffender { offenderDetails, _ ->

          apDeliusContextMockSuccessfulTeamsManagingCaseCall(
            offenderDetails.otherIds.crn,
            ManagingTeamsResponse(
              teamCodes = listOf(offenderDetails.otherIds.crn),
            ),
          )

          every { realApplicationTeamCodeRepository.save(any()) } throws RuntimeException("Database Error")

          webTestClient.post()
            .uri("/applications")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewApplication(
                crn = offenderDetails.otherIds.crn,
                convictionId = 123,
                deliusEventNumber = "1",
                offenceId = "789",
              ),
            )
            .exchange()
            .expectStatus()
            .is5xxServerError

          assertThat(approvedPremisesApplicationRepository.findAll().none { it.crn == offenderDetails.otherIds.crn })
        }
      }
    }

    @Test
    fun `Create new application returns 201 with correct body and Location header`() {
      givenAUser { _, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.now())
            withId(UUID.randomUUID())
          }

          apDeliusContextMockSuccessfulTeamsManagingCaseCall(
            offenderDetails.otherIds.crn,
            ManagingTeamsResponse(
              teamCodes = listOf("TEAM1"),
            ),
          )

          apOASysContextMockSuccessfulNeedsDetailsCall(
            offenderDetails.otherIds.crn,
            NeedsDetailsFactory().produce(),
          )

          val result = webTestClient.post()
            .uri("/applications")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewApplication(
                crn = offenderDetails.otherIds.crn,
                convictionId = 123,
                deliusEventNumber = "1",
                offenceId = "789",
              ),
            )
            .exchange()
            .expectStatus()
            .isCreated
            .returnResult(ApprovedPremisesApplication::class.java)

          assertThat(result.responseHeaders["Location"]).anyMatch {
            it.matches(Regex("/applications/.+"))
          }

          assertThat(result.responseBody.blockFirst()).matches {
            it.person.crn == offenderDetails.otherIds.crn &&
              it.schemaVersion == applicationSchema.id
          }
        }
      }
    }

    @Test
    fun `Create new application without risks returns 201 with correct body and Location header`() {
      givenAUser { _, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.now())
            withId(UUID.randomUUID())
          }

          apDeliusContextMockSuccessfulTeamsManagingCaseCall(
            offenderDetails.otherIds.crn,
            ManagingTeamsResponse(
              teamCodes = listOf("TEAM1"),
            ),
          )

          apOASysContextMockSuccessfulNeedsDetailsCall(
            offenderDetails.otherIds.crn,
            NeedsDetailsFactory().produce(),
          )

          val result = webTestClient.post()
            .uri("/applications?createWithRisks=false")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewApplication(
                crn = offenderDetails.otherIds.crn,
                convictionId = 123,
                deliusEventNumber = "1",
                offenceId = "789",
              ),
            )
            .exchange()
            .expectStatus()
            .isCreated
            .returnResult(ApprovedPremisesApplication::class.java)

          assertThat(result.responseHeaders["Location"]).anyMatch {
            it.matches(Regex("/applications/.+"))
          }

          assertThat(result.responseBody.blockFirst()).matches {
            it.person.crn == offenderDetails.otherIds.crn &&
              it.schemaVersion == applicationSchema.id
          }
        }
      }
    }

    @Test
    fun `Create new application returns successfully when a person has no NOMS number`() {
      givenAUser { _, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = { withoutNomsNumber() },
        ) { offenderDetails, _ ->
          apDeliusContextMockSuccessfulTeamsManagingCaseCall(
            offenderDetails.otherIds.crn,
            ManagingTeamsResponse(
              teamCodes = listOf(offenderDetails.otherIds.crn),
            ),
          )

          approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.now())
            withId(UUID.randomUUID())
          }

          val result = webTestClient.post()
            .uri("/applications")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewApplication(
                crn = offenderDetails.otherIds.crn,
                convictionId = 123,
                deliusEventNumber = "1",
                offenceId = "789",
              ),
            )
            .exchange()
            .expectStatus()
            .isCreated
            .returnResult(ApprovedPremisesApplication::class.java)

          assertThat(result.responseHeaders["Location"]).anyMatch {
            it.matches(Regex("/applications/.+"))
          }

          assertThat(result.responseBody.blockFirst()).matches {
            it.person.crn == offenderDetails.otherIds.crn
          }
        }
      }
    }

    @Test
    fun `Create new application returns successfully when the person cannot be fetched from the prisons API`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = {
            withNomsNumber("ABC123")
          },
        ) { offenderDetails, _ ->
          apDeliusContextMockSuccessfulTeamsManagingCaseCall(
            offenderDetails.otherIds.crn,
            ManagingTeamsResponse(
              teamCodes = listOf(offenderDetails.otherIds.crn),
            ),
          )

          val result = webTestClient.post()
            .uri("/applications")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewApplication(
                crn = offenderDetails.otherIds.crn,
                convictionId = 123,
                deliusEventNumber = "1",
                offenceId = "789",
              ),
            )
            .exchange()
            .expectStatus()
            .isCreated
            .returnResult(ApprovedPremisesApplication::class.java)

          assertThat(result.responseHeaders["Location"]).anyMatch {
            it.matches(Regex("/applications/.+"))
          }

          assertThat(result.responseBody.blockFirst()).matches {
            it.person.crn == offenderDetails.otherIds.crn
          }
        }
      }
    }
  }

  @Nested
  inner class Cas3CreateApplication {

    @Test
    fun `Create new application for Temporary Accommodation returns 403 when user isn't  CAS3_REFERRER role`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        givenAnOffender { offenderDetails, _ ->
          webTestClient.post()
            .uri("/applications")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .bodyValue(
              NewApplication(
                crn = offenderDetails.otherIds.crn,
                convictionId = 123,
                deliusEventNumber = "1",
                offenceId = "789",
              ),
            )
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }

    @Test
    fun `Create new application for Temporary Accommodation returns 201 with correct body and Location header`() {
      givenAUser(roles = listOf(UserRole.CAS3_REFERRER)) { _, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.now())
            withId(UUID.randomUUID())
          }

          val offenceId = "789"

          val result = webTestClient.post()
            .uri("/applications")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .bodyValue(
              NewApplication(
                crn = offenderDetails.otherIds.crn,
                convictionId = 123,
                deliusEventNumber = "1",
                offenceId = offenceId,
              ),
            )
            .exchange()
            .expectStatus()
            .isCreated
            .returnResult(TemporaryAccommodationApplication::class.java)

          assertThat(result.responseHeaders["Location"]).anyMatch {
            it.matches(Regex("/applications/.+"))
          }

          assertThat(result.responseBody.blockFirst()).matches {
            it.person.crn == offenderDetails.otherIds.crn &&
              it.schemaVersion == applicationSchema.id &&
              it.offenceId == offenceId
          }
        }
      }
    }

    @Test
    fun `Should get 403 forbidden error when create new application for Temporary Accommodation with user CAS3_REPORTER`() {
      givenAUser(roles = listOf(UserRole.CAS3_REPORTER)) { _, jwt ->
        givenAnOffender { offenderDetails, _ ->

          webTestClient.post()
            .uri("/applications")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .bodyValue(
              NewApplication(
                crn = offenderDetails.otherIds.crn,
                convictionId = 123,
                deliusEventNumber = "1",
                offenceId = "789",
              ),
            )
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }

    @Test
    fun `Create new application for Temporary Accommodation returns successfully when a person has no NOMS number`() {
      givenAUser(roles = listOf(UserRole.CAS3_REFERRER)) { _, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = { withoutNomsNumber() },
        ) { offenderDetails, _ ->
          val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.now())
            withId(UUID.randomUUID())
          }

          val offenceId = "789"

          val result = webTestClient.post()
            .uri("/applications")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .bodyValue(
              NewApplication(
                crn = offenderDetails.otherIds.crn,
                convictionId = 123,
                deliusEventNumber = "1",
                offenceId = offenceId,
              ),
            )
            .exchange()
            .expectStatus()
            .isCreated
            .returnResult(TemporaryAccommodationApplication::class.java)

          assertThat(result.responseHeaders["Location"]).anyMatch {
            it.matches(Regex("/applications/.+"))
          }

          assertThat(result.responseBody.blockFirst()).matches {
            it.person.crn == offenderDetails.otherIds.crn &&
              it.schemaVersion == applicationSchema.id &&
              it.offenceId == offenceId
          }
        }
      }
    }

    @Test
    fun `Create new application for Temporary Accommodation returns 201 with correct body and store prison-name in DB`() {
      givenAUser(roles = listOf(UserRole.CAS3_REFERRER)) { _, jwt ->
        val agencyName = "HMP Bristol"
        givenAnOffender(
          offenderDetailsConfigBlock = {
            withCrn("CRN")
            withDateOfBirth(LocalDate.parse("1985-05-05"))
            withNomsNumber("NOMS321")
            withFirstName("James")
            withLastName("Someone")
            withGender("Male")
            withEthnicity("White British")
            withNationality("English")
            withReligionOrBelief("Judaism")
            withGenderIdentity("Prefer to self-describe")
            withSelfDescribedGenderIdentity("This is a self described identity")
          },
          inmateDetailsConfigBlock = {
            withOffenderNo("NOMS321")
            withCustodyStatus(InmateStatus.IN)
            withAssignedLivingUnit(
              AssignedLivingUnit(
                agencyId = "BRI",
                locationId = 5,
                description = "B-2F-004",
                agencyName = agencyName,
              ),
            )
          },
        ) { offenderDetails, _ ->
          val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.now())
            withId(UUID.randomUUID())
          }

          val offenceId = "789"

          val result = webTestClient.post()
            .uri("/applications")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .bodyValue(
              NewApplication(
                crn = offenderDetails.otherIds.crn,
                convictionId = 123,
                deliusEventNumber = "1",
                offenceId = offenceId,
              ),
            )
            .exchange()
            .expectStatus()
            .isCreated
            .returnResult(TemporaryAccommodationApplication::class.java)

          assertThat(result.responseHeaders["Location"]).anyMatch {
            it.matches(Regex("/applications/.+"))
          }

          val blockFirst = result.responseBody.blockFirst()
          assertThat(blockFirst).matches {
            it.person.crn == offenderDetails.otherIds.crn &&
              it.schemaVersion == applicationSchema.id &&
              it.offenceId == offenceId
          }

          val accommodationApplicationEntity =
            temporaryAccommodationApplicationRepository.findByIdOrNull(blockFirst.id)
          assertThat(accommodationApplicationEntity!!.prisonNameOnCreation).isNotNull()
          assertThat(accommodationApplicationEntity!!.prisonNameOnCreation).isEqualTo(agencyName)
        }
      }
    }
  }

  @Nested
  inner class Cas1UpdateApplicationCas {
    @Test
    fun `Update existing AP application returns 200 with correct body`() {
      givenAUser { submittingUser, jwt ->
        givenAUser(
          roles = listOf(UserRole.CAS1_ASSESSOR),
          qualifications = listOf(UserQualification.PIPE),
        ) { _, _ ->
          givenAnOffender { offenderDetails, _ ->
            val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

            val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.now())
              withId(UUID.randomUUID())
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

            approvedPremisesApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withId(applicationId)
              withApplicationSchema(applicationSchema)
              withCreatedByUser(submittingUser)
            }

            val resultBody = webTestClient.put()
              .uri("/applications/$applicationId")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(
                UpdateApprovedPremisesApplication(
                  data = mapOf("thingId" to 123),
                  isWomensApplication = false,
                  isPipeApplication = true,
                  type = UpdateApplicationType.CAS1,
                ),
              )
              .exchange()
              .expectStatus()
              .isOk
              .returnResult(String::class.java)
              .responseBody
              .blockFirst()

            val result = objectMapper.readValue(resultBody, Application::class.java)

            assertThat(result.person.crn).isEqualTo(offenderDetails.otherIds.crn)
          }
        }
      }
    }
  }

  @Nested
  inner class Cas3UpdateApplicationCas {
    @Test
    fun `Update existing temporary accommodation application which was deleted returns 400`() {
      givenAUser { userEntity, jwt ->
        givenAUser(
          roles = listOf(UserRole.CAS3_REFERRER),
        ) { _, _ ->
          givenAnOffender { offenderDetails, _ ->
            temporaryAccommodationApplicationJsonSchemaRepository.deleteAll()

            val newestJsonSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.parse("2024-12-11T13:21:00+01:00"))
              withSchema("{}")
            }

            val applicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(newestJsonSchema)
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(userEntity)
              withProbationRegion(userEntity.probationRegion)
              withDeletedAt(OffsetDateTime.now().minusDays(26))
              withData(
                """
            {
               "thingId": 123
            }
            """,
              )
            }
            val resultBody = webTestClient.put()
              .uri("/applications/${applicationEntity.id}")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(
                UpdateTemporaryAccommodationApplication(
                  data = mapOf("thingId" to 345),
                  type = UpdateApplicationType.CAS3,
                ),
              )
              .exchange()
              .expectStatus()
              .isBadRequest
              .expectBody()
              .jsonPath("$.status").isEqualTo("400")
              .jsonPath("$.detail").isEqualTo("This application has already been deleted")
          }
        }
      }
    }
  }

  @Nested
  inner class Cas1SubmitApplication {

    @Test
    fun `Submit standard application does not auto allocate the assessment, sends emails and raises domain events`() {
      val (submittingUser, jwt) = givenAUser(
        probationRegion = givenAProbationRegion(
          apArea = givenAnApArea(
            defaultCruManagementArea = givenACas1CruManagementArea(assessmentAutoAllocationUsername = "DEFAULT_LONDON_ASSESSOR"),
          ),
        ),
      )

      givenAUser(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "DEFAULT_LONDON_ASSESSOR"),
      )

      val (offenderDetails, _) = givenAnOffender()

      val applicationId = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCrn(offenderDetails.otherIds.crn)
        withApplicationSchema(
          approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withDefaults()
          },
        )
        withCreatedByUser(submittingUser)
      }.id

      apDeliusContextMockSuccessfulCaseDetailCall(
        offenderDetails.otherIds.crn,
        CaseDetailFactory().produce(),
      )

      govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

      webTestClient.post()
        .uri("/applications/$applicationId/submission")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          SubmitApprovedPremisesApplication(
            noticeType = Cas1ApplicationTimelinessCategory.standard,
            apType = ApType.normal,
            translatedDocument = {},
            isWomensApplication = false,
            targetLocation = "SW1A 1AA",
            releaseType = ReleaseTypeOption.licence,
            sentenceType = SentenceTypeOption.nonStatutory,
            type = "CAS1",
            applicantUserDetails = Cas1ApplicationUserDetails(
              "applicantName",
              "applicantEmail",
              "applicationTelephone",
            ),
            caseManagerIsNotApplicant = false,
            caseManagerUserDetails = Cas1ApplicationUserDetails(
              "cmName",
              "cmEmail",
              "cmTelephone",
            ),
            reasonForShortNotice = "reasonForShort",
            reasonForShortNoticeOther = "reasonForShortOther",
            licenseExpiryDate = LocalDate.of(2026, 12, 1),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      val persistedApplication = approvedPremisesApplicationRepository.findByIdOrNull(applicationId)!!

      assertThat(persistedApplication.noticeType).isEqualTo(Cas1ApplicationTimelinessCategory.standard)
      assertThat(persistedApplication.isWomensApplication).isFalse()
      assertThat(persistedApplication.isPipeApplication).isFalse
      assertThat(persistedApplication.targetLocation).isEqualTo("SW1A 1AA")
      assertThat(persistedApplication.sentenceType).isEqualTo(SentenceTypeOption.nonStatutory.toString())
      assertThat(persistedApplication.apArea?.id).isEqualTo(submittingUser.apArea!!.id)
      assertThat(persistedApplication.licenceExpiryDate).isEqualTo(LocalDate.of(2026, 12, 1))

      val createdAssessment =
        approvedPremisesAssessmentRepository.findAll().first { it.application.id == applicationId }
      assertThat(createdAssessment.allocatedToUser).isNull()
      assertThat(createdAssessment.createdFromAppeal).isFalse()

      domainEventAsserter.assertDomainEventStoreCount(applicationId, 1)
      val persistedApplicationSubmittedEvent = domainEventAsserter.assertDomainEventOfTypeStored(
        applicationId,
        DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED,
      )

      assertThat(persistedApplicationSubmittedEvent.crn).isEqualTo(offenderDetails.otherIds.crn)

      val emittedMessage = domainEventAsserter.blockForEmittedDomainEvent(DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)

      val emittedMessageDescription = "An application has been submitted for an Approved Premises placement"
      assertThat(emittedMessage.description).isEqualTo(emittedMessageDescription)
      assertThat(emittedMessage.detailUrl).matches("http://api/events/application-submitted/${persistedApplicationSubmittedEvent.id}")
      assertThat(emittedMessage.additionalInformation.applicationId).isEqualTo(applicationId)
      assertThat(emittedMessage.personReference.identifiers).containsExactlyInAnyOrder(
        SnsEventPersonReference("CRN", offenderDetails.otherIds.crn),
        SnsEventPersonReference("NOMS", offenderDetails.otherIds.nomsNumber!!),
      )

      emailAsserter.assertEmailsRequestedCount(1)
      emailAsserter.assertEmailRequested(
        toEmailAddress = submittingUser.email!!,
        templateId = notifyConfig.templates.applicationSubmitted,
        replyToEmailId = persistedApplication.cruManagementArea!!.notifyReplyToEmailId,
      )
    }

    @Test
    fun `Submit womens application does not auto allocate the assessment, sends emails and raises domain events`() {
      val (submittingUser, jwt) = givenAUser(
        probationRegion = givenAProbationRegion(
          apArea = givenAnApArea(
            defaultCruManagementArea = givenACas1CruManagementArea(assessmentAutoAllocationUsername = "DEFAULT_LONDON_ASSESSOR"),
          ),
        ),
      )

      givenAUser(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "DEFAULT_LONDON_ASSESSOR"),
      )

      val (offenderDetails, _) = givenAnOffender()

      val applicationId = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCrn(offenderDetails.otherIds.crn)
        withApplicationSchema(
          approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withDefaults()
          },
        )
        withCreatedByUser(submittingUser)
      }.id

      apDeliusContextMockSuccessfulCaseDetailCall(
        offenderDetails.otherIds.crn,
        CaseDetailFactory().produce(),
      )

      govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

      webTestClient.post()
        .uri("/applications/$applicationId/submission")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          SubmitApprovedPremisesApplication(
            noticeType = Cas1ApplicationTimelinessCategory.standard,
            apType = ApType.normal,
            translatedDocument = {},
            isWomensApplication = true,
            targetLocation = "SW1A 1AA",
            releaseType = ReleaseTypeOption.licence,
            sentenceType = SentenceTypeOption.nonStatutory,
            type = "CAS1",
            applicantUserDetails = Cas1ApplicationUserDetails(
              "applicantName",
              "applicantEmail",
              "applicationTelephone",
            ),
            caseManagerIsNotApplicant = false,
            caseManagerUserDetails = Cas1ApplicationUserDetails(
              "cmName",
              "cmEmail",
              "cmTelephone",
            ),
            reasonForShortNotice = "reasonForShort",
            reasonForShortNoticeOther = "reasonForShortOther",
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      val persistedApplication = approvedPremisesApplicationRepository.findByIdOrNull(applicationId)!!

      assertThat(persistedApplication.noticeType).isEqualTo(Cas1ApplicationTimelinessCategory.standard)
      assertThat(persistedApplication.isWomensApplication).isTrue()
      assertThat(persistedApplication.isPipeApplication).isFalse
      assertThat(persistedApplication.targetLocation).isEqualTo("SW1A 1AA")
      assertThat(persistedApplication.sentenceType).isEqualTo(SentenceTypeOption.nonStatutory.toString())
      assertThat(persistedApplication.apArea?.id).isEqualTo(submittingUser.apArea!!.id)
      assertThat(persistedApplication.cruManagementArea!!.id).isEqualTo(Cas1CruManagementAreaEntity.WOMENS_ESTATE_ID)

      val createdAssessment =
        approvedPremisesAssessmentRepository.findAll().first { it.application.id == applicationId }
      assertThat(createdAssessment.allocatedToUser).isNull()
      assertThat(createdAssessment.createdFromAppeal).isFalse()

      domainEventAsserter.assertDomainEventStoreCount(applicationId, 1)
      val persistedApplicationSubmittedEvent = domainEventAsserter.assertDomainEventOfTypeStored(
        applicationId,
        DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED,
      )

      assertThat(persistedApplicationSubmittedEvent.crn).isEqualTo(offenderDetails.otherIds.crn)

      val emittedMessage = domainEventAsserter.blockForEmittedDomainEvent(DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)

      val emittedMessageDescription = "An application has been submitted for an Approved Premises placement"
      assertThat(emittedMessage.description).isEqualTo(emittedMessageDescription)
      assertThat(emittedMessage.detailUrl).matches("http://api/events/application-submitted/${persistedApplicationSubmittedEvent.id}")
      assertThat(emittedMessage.additionalInformation.applicationId).isEqualTo(applicationId)
      assertThat(emittedMessage.personReference.identifiers).containsExactlyInAnyOrder(
        SnsEventPersonReference("CRN", offenderDetails.otherIds.crn),
        SnsEventPersonReference("NOMS", offenderDetails.otherIds.nomsNumber!!),
      )

      emailAsserter.assertEmailsRequestedCount(1)
      emailAsserter.assertEmailRequested(
        toEmailAddress = submittingUser.email!!,
        templateId = notifyConfig.templates.applicationSubmitted,
        replyToEmailId = persistedApplication.cruManagementArea!!.notifyReplyToEmailId,
      )
    }

    @Test
    fun `Submit emergency application auto allocates the assessment, sends emails and raises domain events`() {
      val (submittingUser, jwt) = givenAUser(
        probationRegion = givenAProbationRegion(
          apArea = givenAnApArea(
            defaultCruManagementArea = givenACas1CruManagementArea(assessmentAutoAllocationUsername = "DEFAULT_LONDON_ASSESSOR"),
          ),
        ),
      )

      val (assessorUser, _) = givenAUser(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "DEFAULT_LONDON_ASSESSOR"),
      )

      val (offenderDetails, _) = givenAnOffender()

      val applicationId = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCrn(offenderDetails.otherIds.crn)
        withApplicationSchema(
          approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withDefaults()
          },
        )
        withCreatedByUser(submittingUser)
      }.id

      apDeliusContextMockSuccessfulCaseDetailCall(
        offenderDetails.otherIds.crn,
        CaseDetailFactory().produce(),
      )

      govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

      webTestClient.post()
        .uri("/applications/$applicationId/submission")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          SubmitApprovedPremisesApplication(
            noticeType = Cas1ApplicationTimelinessCategory.emergency,
            apType = ApType.esap,
            translatedDocument = {},
            isWomensApplication = false,
            targetLocation = "SW1A 1AA",
            releaseType = ReleaseTypeOption.licence,
            sentenceType = SentenceTypeOption.nonStatutory,
            type = "CAS1",
            applicantUserDetails = Cas1ApplicationUserDetails(
              "applicantName",
              "applicantEmail",
              "applicationTelephone",
            ),
            caseManagerIsNotApplicant = false,
            caseManagerUserDetails = Cas1ApplicationUserDetails(
              "cmName",
              "cmEmail",
              "cmTelephone",
            ),
            reasonForShortNotice = "reasonForShort",
            reasonForShortNoticeOther = "reasonForShortOther",
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      val persistedApplication = approvedPremisesApplicationRepository.findByIdOrNull(applicationId)!!

      assertThat(persistedApplication.noticeType).isEqualTo(Cas1ApplicationTimelinessCategory.emergency)

      val createdAssessment =
        approvedPremisesAssessmentRepository.findAll().first { it.application.id == applicationId }
      assertThat(createdAssessment.allocatedToUser!!.id).isEqualTo(assessorUser.id)
      assertThat(createdAssessment.createdFromAppeal).isFalse()

      domainEventAsserter.assertDomainEventStoreCount(applicationId, 2)
      val persistedApplicationSubmittedEvent = domainEventAsserter.assertDomainEventOfTypeStored(
        applicationId,
        DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED,
      )
      val persistedAssessmentAllocatedEvent = domainEventAsserter.assertDomainEventOfTypeStored(
        applicationId,
        DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED,
      )

      assertThat(persistedApplicationSubmittedEvent).isNotNull
      assertThat(persistedApplicationSubmittedEvent.crn).isEqualTo(offenderDetails.otherIds.crn)

      assertThat(persistedAssessmentAllocatedEvent).isNotNull
      assertThat(persistedAssessmentAllocatedEvent.crn).isEqualTo(offenderDetails.otherIds.crn)

      domainEventAsserter.blockForEmittedDomainEvent(DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED)

      emailAsserter.assertEmailsRequestedCount(2)
      emailAsserter.assertEmailRequested(
        toEmailAddress = createdAssessment.allocatedToUser!!.email!!,
        templateId = notifyConfig.templates.assessmentAllocated,
        replyToEmailId = persistedApplication.cruManagementArea!!.notifyReplyToEmailId,
      )
      emailAsserter.assertEmailRequested(
        toEmailAddress = submittingUser.email!!,
        templateId = notifyConfig.templates.applicationSubmitted,
        replyToEmailId = persistedApplication.cruManagementArea!!.notifyReplyToEmailId,
      )
    }

    @Test
    fun `Submit short notice application auto allocates the assessment according to overridden ap area, sends emails and raises domain events`() {
      val (submittingUser, jwt) = givenAUser()

      val (assessorUser, _) = givenAUser(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "DEFAULT_WALES_ASSESSOR"),
      )

      val (offenderDetails, _) = givenAnOffender()

      val applicationId = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCrn(offenderDetails.otherIds.crn)
        withApplicationSchema(
          approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withDefaults()
          },
        )
        withCreatedByUser(submittingUser)
      }.id

      apDeliusContextMockSuccessfulCaseDetailCall(
        offenderDetails.otherIds.crn,
        CaseDetailFactory().produce(),
      )

      govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

      val overriddenApArea = givenAnApArea(
        name = "wales",
        defaultCruManagementArea = givenACas1CruManagementArea(
          assessmentAutoAllocationUsername = "DEFAULT_WALES_ASSESSOR",
        ),
      )

      webTestClient.post()
        .uri("/applications/$applicationId/submission")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          SubmitApprovedPremisesApplication(
            noticeType = Cas1ApplicationTimelinessCategory.shortNotice,
            apType = ApType.pipe,
            translatedDocument = {},
            isWomensApplication = false,
            targetLocation = "SW1A 1AA",
            releaseType = ReleaseTypeOption.licence,
            sentenceType = SentenceTypeOption.nonStatutory,
            type = "CAS1",
            apAreaId = overriddenApArea.id,
            applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicationPhone"),
            caseManagerIsNotApplicant = false,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      val persistedApplication = approvedPremisesApplicationRepository.findByIdOrNull(applicationId)!!

      assertThat(persistedApplication.noticeType).isEqualTo(Cas1ApplicationTimelinessCategory.shortNotice)
      assertThat(persistedApplication.apArea?.id).isEqualTo(overriddenApArea.id)
      assertThat(persistedApplication.cruManagementArea?.id).isEqualTo(overriddenApArea.defaultCruManagementArea.id)

      val createdAssessment =
        approvedPremisesAssessmentRepository.findAll().first { it.application.id == applicationId }
      assertThat(createdAssessment.allocatedToUser).isNotNull()
      assertThat(createdAssessment.allocatedToUser!!.id).isEqualTo(assessorUser.id)

      domainEventAsserter.assertDomainEventStoreCount(applicationId, 2)
      domainEventAsserter.assertDomainEventOfTypeStored(
        applicationId,
        DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED,
      )
      domainEventAsserter.assertDomainEventOfTypeStored(
        applicationId,
        DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED,
      )

      emailAsserter.assertEmailsRequestedCount(2)
      emailAsserter.assertEmailRequested(
        toEmailAddress = createdAssessment.allocatedToUser!!.email!!,
        templateId = notifyConfig.templates.assessmentAllocated,
        replyToEmailId = overriddenApArea.defaultCruManagementArea.notifyReplyToEmailId,
      )
      emailAsserter.assertEmailRequested(
        toEmailAddress = submittingUser.email!!,
        templateId = notifyConfig.templates.applicationSubmitted,
        replyToEmailId = persistedApplication.cruManagementArea!!.notifyReplyToEmailId,
      )
    }

    @Test
    fun `Submit esap application auto allocates the assessment according to esap assessor configuration, sends emails and raises domain events`() {
      val (submittingUser, jwt) = givenAUser()

      val (esapAssessorUser, _) = givenAUser(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "ESAP_ASSESSOR"),
      )

      val (offenderDetails, _) = givenAnOffender()

      val applicationId = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCrn(offenderDetails.otherIds.crn)
        withApplicationSchema(
          approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withDefaults()
          },
        )
        withCreatedByUser(submittingUser)
      }.id

      apDeliusContextMockSuccessfulCaseDetailCall(
        offenderDetails.otherIds.crn,
        CaseDetailFactory().produce(),
      )

      govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

      val overriddenApArea = givenAnApArea()

      webTestClient.post()
        .uri("/applications/$applicationId/submission")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          SubmitApprovedPremisesApplication(
            noticeType = Cas1ApplicationTimelinessCategory.standard,
            apType = ApType.esap,
            translatedDocument = {},
            isWomensApplication = false,
            targetLocation = "SW1A 1AA",
            releaseType = ReleaseTypeOption.licence,
            sentenceType = SentenceTypeOption.nonStatutory,
            type = "CAS1",
            apAreaId = overriddenApArea.id,
            applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicationPhone"),
            caseManagerIsNotApplicant = false,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      val persistedApplication = approvedPremisesApplicationRepository.findByIdOrNull(applicationId)!!

      assertThat(persistedApplication.noticeType).isEqualTo(Cas1ApplicationTimelinessCategory.standard)
      assertThat(persistedApplication.apArea?.id).isEqualTo(overriddenApArea.id)

      val createdAssessment =
        approvedPremisesAssessmentRepository.findAll().first { it.application.id == applicationId }
      assertThat(createdAssessment.allocatedToUser!!.id).isEqualTo(esapAssessorUser.id)

      domainEventAsserter.assertDomainEventStoreCount(applicationId, 2)
      domainEventAsserter.assertDomainEventOfTypeStored(
        applicationId,
        DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED,
      )
      domainEventAsserter.assertDomainEventOfTypeStored(
        applicationId,
        DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED,
      )

      emailAsserter.assertEmailsRequestedCount(2)
      emailAsserter.assertEmailRequested(
        toEmailAddress = createdAssessment.allocatedToUser!!.email!!,
        templateId = notifyConfig.templates.assessmentAllocated,
        replyToEmailId = persistedApplication.cruManagementArea!!.notifyReplyToEmailId,
      )
      emailAsserter.assertEmailRequested(
        toEmailAddress = submittingUser.email!!,
        templateId = notifyConfig.templates.applicationSubmitted,
        replyToEmailId = persistedApplication.cruManagementArea!!.notifyReplyToEmailId,
      )
    }

    @Test
    fun `When several concurrent submit application requests occur, only one is successful, all others return 400 without persisting domain events`() {
      givenAUser { submittingUser, jwt ->
        givenAUser(
          roles = listOf(UserRole.CAS1_ASSESSOR),
        ) { _, _ ->
          givenAnOffender { offenderDetails, _ ->
            val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
              withDefaults()
            }

            val applicationId = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withApplicationSchema(applicationSchema)
              withCreatedByUser(submittingUser)
              withData(
                """
              {
                 "isWomensApplication": true,
                 "isPipeApplication": true
              }
            """,
              )
            }.id

            every { realApplicationRepository.save(any()) } answers {
              Thread.sleep(1000)
              it.invocation.args[0] as ApplicationEntity
            }

            apDeliusContextMockSuccessfulCaseDetailCall(
              offenderDetails.otherIds.crn,
              CaseDetailFactory().produce(),
            )

            val responseStatuses = mutableListOf<HttpStatus>()

            govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

            (1..10).map {
              val thread = Thread {
                webTestClient.post()
                  .uri("/applications/$applicationId/submission")
                  .header("Authorization", "Bearer $jwt")
                  .bodyValue(
                    SubmitApprovedPremisesApplication(
                      translatedDocument = {},
                      isPipeApplication = true,
                      isWomensApplication = true,
                      isEmergencyApplication = true,
                      isEsapApplication = true,
                      targetLocation = "SW1A 1AA",
                      releaseType = ReleaseTypeOption.licence,
                      sentenceType = SentenceTypeOption.nonStatutory,
                      type = "CAS1",
                      applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicationPhone"),
                      caseManagerIsNotApplicant = false,
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

            val persistedDomainEvents = domainEventRepository.findAll().filter { it.applicationId == applicationId }

            val persistedApplicationSubmittedEvents = persistedDomainEvents.filter { it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED }

            assertThat(persistedApplicationSubmittedEvents).singleElement()
            assertThat(responseStatuses.count { it.value() == 200 }).isEqualTo(1)
            assertThat(responseStatuses.count { it.value() == 400 }).isEqualTo(9)
          }
        }
      }
    }
  }

  @Nested
  inner class Cas3SubmitApplication {

    @Test
    fun `Submit Temporary Accommodation application returns 200`() {
      givenAUser { submittingUser, jwt ->
        givenAUser { _, _ ->
          givenAnOffender { offenderDetails, _ ->
            val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")
            val offenderName = "${offenderDetails.firstName} ${offenderDetails.surname}"

            val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.now())
              withId(UUID.randomUUID())
              withSchema(schemaText())
            }

            temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withId(applicationId)
              withApplicationSchema(applicationSchema)
              withCreatedByUser(submittingUser)
              withProbationRegion(submittingUser.probationRegion)
              withName(offenderName)
              withData(
                """
                {}
              """,
              )
            }

            webTestClient.post()
              .uri("/applications/$applicationId/submission")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
              .bodyValue(
                SubmitTemporaryAccommodationApplication(
                  translatedDocument = {},
                  type = "CAS3",
                  arrivalDate = LocalDate.now(),
                  summaryData = object {
                    val num = 50
                    val text = "Hello world!"
                  },
                ),
              )
              .exchange()
              .expectStatus()
              .isOk

            val persistedApplication = temporaryAccommodationApplicationRepository.findByIdOrNull(applicationId)!!
            val persistedAssessment = persistedApplication.getLatestAssessment() as TemporaryAccommodationAssessmentEntity

            assertThat(persistedAssessment.summaryData).isEqualTo("{\"num\":50,\"text\":\"Hello world!\"}")
            assertThat(persistedApplication.name).isEqualTo(offenderName)
          }
        }
      }
    }

    @Test
    fun `Submit Temporary Accommodation application returns 200 with optional elements in the request`() {
      givenAUser { submittingUser, jwt ->
        givenAUser { _, _ ->
          givenAnOffender { offenderDetails, _ ->
            val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

            val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.now())
              withId(UUID.randomUUID())
              withSchema(schemaText())
            }

            temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withId(applicationId)
              withApplicationSchema(applicationSchema)
              withCreatedByUser(submittingUser)
              withProbationRegion(submittingUser.probationRegion)
              withPdu("Probation Delivery Unit Test")
              withHasHistoryOfSexualOffence(true)
              withIsConcerningSexualBehaviour(true)
              withIsConcerningArsonBehaviour(true)
              withData(
                """
              {}
            """,
              )
            }

            val pdu = probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(
                probationRegionEntityFactory.produceAndPersist {
                  withId(submittingUser.probationRegion.id)
                },
              )
            }

            webTestClient.post()
              .uri("/applications/$applicationId/submission")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
              .bodyValue(
                SubmitTemporaryAccommodationApplication(
                  translatedDocument = {},
                  type = "CAS3",
                  arrivalDate = LocalDate.now(),
                  summaryData = object {
                    val num = 50
                    val text = "Hello world!"
                  },
                  personReleaseDate = LocalDate.now(),
                  pdu = "Probation Delivery Unit Test",
                  isHistoryOfSexualOffence = true,
                  isConcerningSexualBehaviour = true,
                  isConcerningArsonBehaviour = true,
                  dutyToReferOutcome = "Accepted – Prevention/ Relief Duty",
                  prisonReleaseTypes = listOf(
                    "Parole",
                    "CRD licence",
                  ),
                  probationDeliveryUnitId = pdu.id,
                ),
              )
              .exchange()
              .expectStatus()
              .isOk

            val persistedApplication = temporaryAccommodationApplicationRepository.findByIdOrNull(applicationId)!!
            val persistedAssessment =
              persistedApplication.getLatestAssessment() as TemporaryAccommodationAssessmentEntity

            assertThat(persistedAssessment.summaryData).isEqualTo("{\"num\":50,\"text\":\"Hello world!\"}")
            assertThat(persistedApplication.personReleaseDate).isEqualTo(LocalDate.now())
            assertThat(persistedApplication.dutyToReferOutcome).isEqualTo("Accepted – Prevention/ Relief Duty")
            assertThat(persistedApplication.prisonReleaseTypes).isEqualTo("Parole,CRD licence")
            assertThat(persistedApplication.probationDeliveryUnit!!.id).isEqualTo(pdu.id)
            assertThat(persistedApplication.probationDeliveryUnit!!.name).isEqualTo(pdu.name)
          }
        }
      }
    }

    @Test
    fun `Submit Temporary Accommodation application returns 400 when the application was deleted`() {
      givenAUser { submittingUser, jwt ->
        givenAUser { _, _ ->
          givenAnOffender { offenderDetails, _ ->
            val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")
            val offenderName = "${offenderDetails.firstName} ${offenderDetails.surname}"

            val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.now())
              withId(UUID.randomUUID())
              withSchema(schemaText())
            }

            temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withId(applicationId)
              withApplicationSchema(applicationSchema)
              withCreatedByUser(submittingUser)
              withProbationRegion(submittingUser.probationRegion)
              withName(offenderName)
              withDeletedAt(OffsetDateTime.now().minusDays(32))
              withData(
                """
                {}
              """,
              )
            }

            webTestClient.post()
              .uri("/applications/$applicationId/submission")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
              .bodyValue(
                SubmitTemporaryAccommodationApplication(
                  translatedDocument = {},
                  type = "CAS3",
                  arrivalDate = LocalDate.now(),
                  summaryData = object {
                    val num = 50
                    val text = "Hello world!"
                  },
                ),
              )
              .exchange()
              .expectStatus()
              .isBadRequest
              .expectBody()
              .jsonPath("$.status").isEqualTo("400")
              .jsonPath("$.detail").isEqualTo("This application has already been deleted")
          }
        }
      }
    }
  }

  private fun schemaText(): String = """
              {
                "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
                "${"\$id"}": "https://example.com/product.schema.json",
                "title": "Thing",
                "description": "A thing",
                "type": "object",
                "properties": {},
                "required": []
              }
            """

  @Nested
  inner class GetAssessmentForApplication {
    @Test
    fun `Get assessment for application returns an application's assessment when the requesting user is the allocated user`() {
      givenAUser { applicant, _ ->
        givenAUser { user, jwt ->
          givenAnOffender { offenderDetails, inmateDetails ->
            val (application, assessment) = produceAndPersistApplicationAndAssessment(applicant, user, offenderDetails)

            webTestClient.get()
              .uri("/applications/${application.id}/assessment")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  assessmentTransformer.transformJpaToApi(
                    assessment,
                    PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                  ),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `Get assessment for application returns an application's assessment when the requesting user is a workflow manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
        givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR)) { applicant, _ ->
          givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR)) { assignee, _ ->
            givenAnOffender { offenderDetails, inmateDetails ->
              val (application, assessment) = produceAndPersistApplicationAndAssessment(
                applicant,
                assignee,
                offenderDetails,
              )

              webTestClient.get()
                .uri("/applications/${application.id}/assessment")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    assessmentTransformer.transformJpaToApi(
                      assessment,
                      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                    ),
                  ),
                )
            }
          }
        }
      }
    }

    @Test
    fun `Get assessment for an application returns 403 if the user does not have permission`() {
      givenAUser { applicant, _ ->
        givenAUser { assignee, _ ->
          givenAUser { _, jwt ->
            givenAnOffender { offenderDetails, _ ->

              val (application, _) = produceAndPersistApplicationAndAssessment(applicant, assignee, offenderDetails)

              webTestClient.get()
                .uri("/applications/${application.id}/assessment")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isForbidden
            }
          }
        }
      }
    }
  }

  @Nested
  inner class PostTimelineNotesForApplication {
    @Test
    fun `post ApplicationTimelineNote without JWT returns 401`() {
      webTestClient.post()
        .uri("/applications/${UUID.randomUUID()}/notes")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `post ApplicationTimelineNote with JWT returns 200`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        val applicationId = UUID.randomUUID()

        val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        approvedPremisesApplicationEntityFactory.produceAndPersist {
          withId(applicationId)
          withCreatedByUser(user)
          withApplicationSchema(applicationSchema)
        }
        val body = ApplicationTimelineNote(note = "some note")
        webTestClient.post()
          .uri("/applications/$applicationId/notes")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            body,
          )
          .exchange()
          .expectStatus()
          .isOk

        val savedNote =
          applicationTimelineNoteRepository.findApplicationTimelineNoteEntitiesByApplicationIdAndDeletedAtIsNull(applicationId)
        savedNote.map {
          assertThat(it.body).isEqualTo("some note")
          assertThat(it.applicationId).isEqualTo(applicationId)
          assertThat(it.createdBy!!.id).isEqualTo(user.id)
        }
      }
    }
  }

  /**
   * Note - Withdrawal cascading is tested in [WithdrawalTest]
   */
  @Nested
  inner class WithdrawApplication {
    @Test
    fun `Withdraw Application without JWT returns 401`() {
      webTestClient.post()
        .uri("/applications/${UUID.randomUUID()}/withdrawal")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Withdraw Application 200 withdraws application and sends an email to application creator`() {
      givenAUser { user, jwt ->
        val application = produceAndPersistBasicApplication(
          crn = "ABC123",
          userEntity = user,
          managingTeamCode = "TEAM",
          submittedAt = OffsetDateTime.now(),
        )

        val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
          withAllocatedToUser(user)
          withAssessmentSchema(assessmentSchema)
          withSubmittedAt(OffsetDateTime.now())
        }

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

        val updatedApplication = approvedPremisesApplicationRepository.findByIdOrNull(application.id)!!
        assertThat(updatedApplication.isWithdrawn).isTrue

        val updatedAssessment = approvedPremisesAssessmentRepository.findByIdOrNull(assessment.id)!!
        assertThat(updatedAssessment.isWithdrawn).isTrue

        emailAsserter.assertEmailsRequestedCount(1)
        emailAsserter.assertEmailRequested(
          user.email!!,
          notifyConfig.templates.applicationWithdrawnV2,
        )
      }
    }

    @Test
    fun `Withdraw Application 200 withdraws application and sends an email to assessor if assessment is pending`() {
      givenAUser { applicant, jwt ->
        givenAUser { assessor, _ ->
          val application = produceAndPersistBasicApplication(
            crn = "ABC123",
            userEntity = applicant,
            managingTeamCode = "TEAM",
            submittedAt = OffsetDateTime.now(),
          )

          val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          approvedPremisesAssessmentEntityFactory.produceAndPersist {
            withApplication(application)
            withAllocatedToUser(assessor)
            withAssessmentSchema(assessmentSchema)
            withSubmittedAt(null)
          }

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

          emailAsserter.assertEmailsRequestedCount(2)
          emailAsserter.assertEmailRequested(
            applicant.email!!,
            notifyConfig.templates.applicationWithdrawnV2,
          )
          emailAsserter.assertEmailRequested(
            assessor.email!!,
            notifyConfig.templates.assessmentWithdrawnV2,
          )
        }
      }
    }
  }

  @Nested
  inner class GetApplicationsAll {
    @Test
    fun `Get applications all without JWT returns 401`() {
      webTestClient.get()
        .uri("/applications/all")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get applications all not approved premises service returns 403 forbidden`() {
      givenAUser { _, jwt ->
        webTestClient.get()
          .uri("/applications/all")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get applications all returns 200 and correct body`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          approvedPremisesApplicationJsonSchemaRepository.deleteAll()
          val applicationId1 = UUID.randomUUID()
          val applicationId2 = UUID.randomUUID()

          val newestJsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(newestJsonSchema)
            withId(applicationId1)
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(userEntity)
            withCreatedAt(OffsetDateTime.parse("2022-09-24T15:00:00+01:00"))
            withSubmittedAt(OffsetDateTime.parse("2022-09-25T16:00:00+01:00"))
            withData(
              """
          {
             "thingId": 123
          }
          """,
            )
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(newestJsonSchema)
            withId(applicationId2)
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(userEntity)
            withCreatedAt(OffsetDateTime.parse("2022-10-24T16:00:00+01:00"))
            withSubmittedAt(OffsetDateTime.parse("2022-10-25T17:00:00+01:00"))
            withData(
              """
          {
             "thingId": 123
          }
          """,
            )
          }

          val rawResponseBody = webTestClient.get()
            .uri("/applications/all")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody =
            objectMapper.readValue(
              rawResponseBody,
              object : TypeReference<List<ApplicationSummary>>() {},
            )

          assertThat(responseBody.count()).isEqualTo(2)
        }
      }
    }

    @Test
    fun `Get applications all LAO without qualification returns 200 and restricted person`() {
      givenAUser { userEntity, jwt ->

        val (offenderDetails, _) = givenAnOffender(
          offenderDetailsConfigBlock = {
            withCurrentRestriction(true)
          },
        )

        approvedPremisesApplicationEntityFactory.produceAndPersist {
          withApplicationSchema(
            approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
          )
          withId(UUID.randomUUID())
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(userEntity)
          withCreatedAt(OffsetDateTime.parse("2022-09-24T15:00:00+01:00"))
          withSubmittedAt(OffsetDateTime.parse("2022-09-25T16:00:00+01:00"))
          withData(
            """
        {
           "thingId": 123
        }
        """,
          )
        }

        val response = webTestClient.get()
          .uri("/applications/all")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<ApplicationSummary>()

        assertThat(response).hasSize(1)
        assertThat(response[0].person).isInstanceOf(RestrictedPerson::class.java)
      }
    }

    @Test
    fun `Get applications all LAO with LAO qualification returns 200 and full person`() {
      givenAUser(qualifications = listOf(UserQualification.LAO)) { userEntity, jwt ->
        val (offenderDetails, _) = givenAnOffender(
          offenderDetailsConfigBlock = {
            withCurrentRestriction(true)
          },
        )

        approvedPremisesApplicationEntityFactory.produceAndPersist {
          withApplicationSchema(
            approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
          )
          withId(UUID.randomUUID())
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(userEntity)
          withCreatedAt(OffsetDateTime.parse("2022-09-24T15:00:00+01:00"))
          withSubmittedAt(OffsetDateTime.parse("2022-09-25T16:00:00+01:00"))
          withData(
            """
        {
           "thingId": 123
        }
        """,
          )
        }

        val response = webTestClient.get()
          .uri("/applications/all")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<ApplicationSummary>()

        assertThat(response).hasSize(1)
        assertThat(response[0].person).isInstanceOf(FullPerson::class.java)
      }
    }

    @Test
    fun `Get applications all returns twelve items if no page parameter passed`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          createTwelveApplications(offenderDetails.otherIds.crn, userEntity)

          val responseBody = webTestClient.get()
            .uri("/applications/all")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .bodyAsListOfObjects<ApplicationSummary>()

          assertThat(responseBody.count()).isEqualTo(12)
        }
      }
    }

    @Test
    fun `Get applications all returns ten items if page parameter passed is one`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          createTwelveApplications(offenderDetails.otherIds.crn, userEntity)

          val responseBody = webTestClient.get()
            .uri("/applications/all?page=1&sortDirection=asc")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 12)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .bodyAsListOfObjects<ApplicationSummary>()

          assertThat(responseBody.count()).isEqualTo(10)
        }
      }
    }

    @Test
    fun `Get applications all returns twelve items if crn parameter passed and no page`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val crn = offenderDetails.otherIds.crn
          createTwelveApplications(crn, userEntity)

          val responseBody = webTestClient.get()
            .uri("/applications/all?crnOrName=$crn")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .bodyAsListOfObjects<ApplicationSummary>()

          assertThat(responseBody.count()).isEqualTo(12)
        }
      }
    }

    @Test
    fun `Get applications all returns twelve items if crn parameter passed and two crns in db no page`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          givenAnOffender { offenderDetails2, _ ->

            val crn1 = offenderDetails.otherIds.crn
            createTwelveApplications(crn1, userEntity)

            val crn2 = offenderDetails2.otherIds.crn
            createTwelveApplications(crn2, userEntity)

            val responseBody = webTestClient.get()
              .uri("/applications/all?crnOrName=$crn2")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .bodyAsListOfObjects<ApplicationSummary>()

            assertThat(responseBody.count()).isEqualTo(12)
          }
        }
      }
    }

    @Test
    fun `Get applications all returns two items if crn parameter passed and two crns in db and page one`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          givenAnOffender { offenderDetails2, _ ->

            val crn1 = offenderDetails.otherIds.crn
            createTwelveApplications(crn1, userEntity)

            val crn2 = offenderDetails2.otherIds.crn
            createTwelveApplications(crn2, userEntity)

            val responseBody = webTestClient.get()
              .uri("/applications/all?page=2&sortDirection=desc&crnOrName=$crn2")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", 12)
              .expectHeader().valueEquals("X-Pagination-PageSize", 10)
              .bodyAsListOfObjects<ApplicationSummary>()

            assertThat(responseBody.count()).isEqualTo(2)
          }
        }
      }
    }

    @Test
    fun `Get applications all returns 200 correct body and page one and sorted by createdAt desc`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val crn = offenderDetails.otherIds.crn
          val date1 = OffsetDateTime.parse("2022-08-24T15:00:00+01:00")
          val date2 = OffsetDateTime.parse("2022-09-24T15:00:00+01:00")
          val date3 = OffsetDateTime.parse("2022-12-24T15:00:00+01:00")
          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }
          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withCreatedAt(date1)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withCreatedAt(date2)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withCreatedAt(date3)
          }

          val responseBody = webTestClient.get()
            .uri("/applications/all?page=1&sortDirection=desc&sortBy=createdAt")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 3)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .bodyAsListOfObjects<ApplicationSummary>()

          assertThat(responseBody.count()).isEqualTo(3)
          assertThat(responseBody[0].createdAt).isEqualTo(date3.toInstant())
          assertThat(responseBody[1].createdAt).isEqualTo(date2.toInstant())
          assertThat(responseBody[2].createdAt).isEqualTo(date1.toInstant())
        }
      }
    }

    @Test
    fun `Get applications all returns 200 correct body and page one and sorted by createdAt asc`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val crn = offenderDetails.otherIds.crn
          val date1 = OffsetDateTime.parse("2022-08-24T15:00:00+01:00")
          val date2 = OffsetDateTime.parse("2022-09-24T15:00:00+01:00")
          val date3 = OffsetDateTime.parse("2022-12-24T15:00:00+01:00")
          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }
          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withCreatedAt(date1)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withCreatedAt(date2)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withCreatedAt(date3)
          }

          val responseBody = webTestClient.get()
            .uri("/applications/all?page=1&sortDirection=asc&sortBy=createdAt")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 3)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .bodyAsListOfObjects<ApplicationSummary>()

          assertThat(responseBody.count()).isEqualTo(3)
          assertThat(responseBody[0].createdAt).isEqualTo(date1.toInstant())
          assertThat(responseBody[1].createdAt).isEqualTo(date2.toInstant())
          assertThat(responseBody[2].createdAt).isEqualTo(date3.toInstant())
        }
      }
    }

    @Test
    fun `Get applications all returns 200 correct body and page one and sorted by arrivalDate desc`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val crn = offenderDetails.otherIds.crn
          val date1 = OffsetDateTime.parse("2022-08-24T15:00:00+01:00")
          val date2 = OffsetDateTime.parse("2022-09-24T15:00:00+01:00")
          val date3 = OffsetDateTime.parse("2022-12-24T15:00:00+01:00")
          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }
          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date1)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date2)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date3)
          }

          val responseBody = webTestClient.get()
            .uri("/applications/all?page=1&sortDirection=desc&sortBy=arrivalDate")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 3)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .bodyAsListOfObjects<ApprovedPremisesApplicationSummary>()

          assertThat(responseBody.count()).isEqualTo(3)
          assertThat(responseBody[0].arrivalDate).isEqualTo(date3.toInstant())
          assertThat(responseBody[1].arrivalDate).isEqualTo(date2.toInstant())
          assertThat(responseBody[2].arrivalDate).isEqualTo(date1.toInstant())
        }
      }
    }

    @Test
    fun `Get applications all returns 200 correct body and page one and sorted by arrivalDate asc`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val crn = offenderDetails.otherIds.crn
          val date1 = OffsetDateTime.parse("2022-08-24T15:00:00+01:00")
          val date2 = OffsetDateTime.parse("2022-09-24T15:00:00+01:00")
          val date3 = OffsetDateTime.parse("2022-12-24T15:00:00+01:00")
          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }
          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date1)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date2)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date3)
          }

          val responseBody = webTestClient.get()
            .uri("/applications/all?page=1&sortDirection=asc&sortBy=arrivalDate")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 3)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .bodyAsListOfObjects<ApprovedPremisesApplicationSummary>()

          assertThat(responseBody.count()).isEqualTo(3)
          assertThat(responseBody[0].arrivalDate).isEqualTo(date1.toInstant())
          assertThat(responseBody[1].arrivalDate).isEqualTo(date2.toInstant())
          assertThat(responseBody[2].arrivalDate).isEqualTo(date3.toInstant())
        }
      }
    }

    @Test
    fun `Get applications all returns 200 correct body and page one and sorted by tier asc`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val crn = offenderDetails.otherIds.crn
          val date1 = OffsetDateTime.parse("2022-08-24T15:00:00+01:00")
          val date2 = OffsetDateTime.parse("2022-09-24T15:00:00+01:00")
          val date3 = OffsetDateTime.parse("2022-12-24T15:00:00+01:00")
          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          val risk1 = PersonRisksFactory()
            .withTier(
              RiskWithStatus(
                RiskTier(
                  level = "M1",
                  lastUpdated = LocalDate.parse("2023-06-26"),
                ),
              ),
            )
            .produce()

          val risk2 = PersonRisksFactory()
            .withTier(
              RiskWithStatus(
                RiskTier(
                  level = "M2",
                  lastUpdated = LocalDate.parse("2023-06-26"),
                ),
              ),
            )
            .produce()

          val risk3 = PersonRisksFactory()
            .withTier(
              RiskWithStatus(
                RiskTier(
                  level = "M3",
                  lastUpdated = LocalDate.parse("2023-06-26"),
                ),
              ),
            )
            .produce()

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date1)
            withRiskRatings(risk1)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date2)
            withRiskRatings(risk2)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date3)
            withRiskRatings(risk3)
          }

          val responseBody = webTestClient.get()
            .uri("/applications/all?page=1&sortDirection=asc&sortBy=tier")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 3)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .bodyAsListOfObjects<ApprovedPremisesApplicationSummary>()

          assertThat(responseBody.count()).isEqualTo(3)
          assertThat(responseBody[0].tier).isEqualTo("M1")
          assertThat(responseBody[1].tier).isEqualTo("M2")
          assertThat(responseBody[2].tier).isEqualTo("M3")
        }
      }
    }

    @Test
    fun `Get applications all returns 200 correct body and page one and sorted by tier desc`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val crn = offenderDetails.otherIds.crn
          val date1 = OffsetDateTime.parse("2022-08-24T15:00:00+01:00")
          val date2 = OffsetDateTime.parse("2022-09-24T15:00:00+01:00")
          val date3 = OffsetDateTime.parse("2022-12-24T15:00:00+01:00")
          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          val risk1 = PersonRisksFactory()
            .withTier(
              RiskWithStatus(
                RiskTier(
                  level = "M1",
                  lastUpdated = LocalDate.parse("2023-06-26"),
                ),
              ),
            )
            .produce()

          val risk2 = PersonRisksFactory()
            .withTier(
              RiskWithStatus(
                RiskTier(
                  level = "M2",
                  lastUpdated = LocalDate.parse("2023-06-26"),
                ),
              ),
            )
            .produce()

          val risk3 = PersonRisksFactory()
            .withTier(
              RiskWithStatus(
                RiskTier(
                  level = "M3",
                  lastUpdated = LocalDate.parse("2023-06-26"),
                ),
              ),
            )
            .produce()

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date1)
            withRiskRatings(risk1)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date2)
            withRiskRatings(risk2)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date3)
            withRiskRatings(risk3)
          }

          val responseBody = webTestClient.get()
            .uri("/applications/all?page=1&sortDirection=desc&sortBy=tier")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 3)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .bodyAsListOfObjects<ApprovedPremisesApplicationSummary>()

          assertThat(responseBody.count()).isEqualTo(3)
          assertThat(responseBody[0].tier).isEqualTo("M3")
          assertThat(responseBody[1].tier).isEqualTo("M2")
          assertThat(responseBody[2].tier).isEqualTo("M1")
        }
      }
    }

    @Test
    fun `Get applications all returns 200 correct body and page two and query by status`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val crn = offenderDetails.otherIds.crn
          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          approvedPremisesApplicationEntityFactory.produceAndPersistMultiple(12) {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withStatus(ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersistMultiple(12) {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
          }

          val responseBody = webTestClient.get()
            .uri("/applications/all?page=2&sortDirection=desc&status=assesmentInProgress")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 12)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .bodyAsListOfObjects<ApprovedPremisesApplicationSummary>()

          assertThat(responseBody.count()).isEqualTo(2)
          assertThat(responseBody[0].status).isEqualTo(ApiApprovedPremisesApplicationStatus.assesmentInProgress)
        }
      }
    }

    @Test
    fun `Get applications all returns 200 correct body for a given name`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          givenAnOffender { offenderDetails2, _ ->

            val crn1 = offenderDetails.otherIds.crn

            val crn2 = offenderDetails2.otherIds.crn

            val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            }

            approvedPremisesApplicationEntityFactory.produceAndPersistMultiple(12) {
              withApplicationSchema(applicationSchema)
              withName("Gareth")
              withCrn(crn1)
              withCreatedByUser(userEntity)
              withStatus(ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS)
            }

            approvedPremisesApplicationEntityFactory.produceAndPersistMultiple(12) {
              withApplicationSchema(applicationSchema)
              withName("Stu")
              withCrn(crn2)
              withCreatedByUser(userEntity)
              withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
            }

            val responseBody = webTestClient.get()
              .uri("/applications/all?page=1&sortDirection=desc&crnOrName=Gareth")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", 12)
              .expectHeader().valueEquals("X-Pagination-PageSize", 10)
              .bodyAsListOfObjects<ApprovedPremisesApplicationSummary>()

            assertThat(responseBody.count()).isEqualTo(10)
          }
        }
      }
    }

    private fun createTwelveApplications(crn: String, user: UserEntity) {
      val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
        withPermissiveSchema()
      }

      approvedPremisesApplicationEntityFactory.produceAndPersistMultiple(12) {
        withApplicationSchema(applicationSchema)
        withCrn(crn)
        withCreatedByUser(user)
        withData(
          """
          {
             "thingId": 123
          }
          """,
        )
      }
    }
  }

  private fun serializableToJsonNode(serializable: Any?): JsonNode {
    if (serializable == null) return NullNode.instance
    if (serializable is String) return objectMapper.readTree(serializable)

    return objectMapper.readTree(objectMapper.writeValueAsString(serializable))
  }

  private fun produceAndPersistBasicApplication(
    crn: String,
    userEntity: UserEntity,
    managingTeamCode: String,
    submittedAt: OffsetDateTime? = null,
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
        withSubmittedAt(submittedAt)
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

  private fun produceAndPersistApplicationAndAssessment(
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
    }

    assessment.schemaUpToDate = true
    application.assessments.add(assessment)

    return Pair(application, assessment)
  }
}
