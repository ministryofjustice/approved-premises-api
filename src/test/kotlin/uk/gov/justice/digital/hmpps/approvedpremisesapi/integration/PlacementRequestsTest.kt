package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementRequestBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestDetailTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestTransformer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class PlacementRequestsTest : IntegrationTestBase() {

  @Autowired
  lateinit var placementRequestTransformer: PlacementRequestTransformer

  @Autowired
  lateinit var realPlacementRequestRepository: PlacementRequestRepository

  lateinit var probationRegion: ProbationRegionEntity

  @BeforeEach
  fun before() {
    probationRegion = givenAProbationRegion()
  }

  @Nested
  inner class AllPlacementRequests {
    @Test
    fun `Get all placement requests without JWT returns 401`() {
      webTestClient.get()
        .uri("/placement-requests")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `It returns all the placement requests for a user`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails1, inmateDetails1 ->
          givenAnOffender { offenderDetails2, _ ->
            val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            }

            val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            }

            val application1 = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails1.otherIds.crn)
              withCreatedByUser(user)
              withApplicationSchema(applicationSchema)
              withSubmittedAt(OffsetDateTime.now())
              withReleaseType("licence")
            }

            val assessment1 = approvedPremisesAssessmentEntityFactory.produceAndPersist {
              withAssessmentSchema(assessmentSchema)
              withApplication(application1)
              withSubmittedAt(OffsetDateTime.now())
              withAllocatedToUser(user)
              withDecision(AssessmentDecision.ACCEPTED)
            }

            val application2 = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails2.otherIds.crn)
              withCreatedByUser(user)
              withSubmittedAt(OffsetDateTime.now())
              withApplicationSchema(applicationSchema)
              withReleaseType("licence")
            }

            val assessment2 = approvedPremisesAssessmentEntityFactory.produceAndPersist {
              withAssessmentSchema(assessmentSchema)
              withApplication(application2)
              withSubmittedAt(OffsetDateTime.now())
              withAllocatedToUser(user)
              withDecision(AssessmentDecision.ACCEPTED)
            }

            val placementRequirements = placementRequirementsFactory.produceAndPersist {
              withApplication(application1)
              withAssessment(assessment1)
              withPostcodeDistrict(postCodeDistrictFactory.produceAndPersist())
              withDesirableCriteria(
                characteristicEntityFactory.produceAndPersistMultiple(5),
              )
              withEssentialCriteria(
                characteristicEntityFactory.produceAndPersistMultiple(3),
              )
            }

            val placementRequest = placementRequestFactory.produceAndPersist {
              withAllocatedToUser(user)
              withApplication(application1)
              withAssessment(assessment1)
              withPlacementRequirements(placementRequirements)
            }

            placementRequestFactory.produceAndPersistMultiple(2) {
              withAllocatedToUser(user)
              withReallocatedAt(OffsetDateTime.now())
              withApplication(application2)
              withAssessment(assessment2)
              withPlacementRequirements(placementRequirements)
            }

            webTestClient.get()
              .uri("/placement-requests")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    placementRequestTransformer.transformJpaToApi(
                      placementRequest,
                      PersonInfoResult.Success.Full(offenderDetails1.otherIds.crn, offenderDetails1, inmateDetails1),
                    ),
                  ),
                ),
              )
          }
        }
      }
    }
  }

  @Nested
  inner class Dashboard {
    @Test
    fun `Get dashboard without JWT returns 401`() {
      webTestClient.get()
        .uri("/placement-requests/dashboard")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_CRU_MEMBER", "CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA", "CAS1_JANITOR"], mode = EnumSource.Mode.EXCLUDE)
    fun `Get dashboard without CAS1_VIEW_CRU_DASHBOARD permission returns 401`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/placement-requests/dashboard")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `It returns the unmatched placement requests by default when the user is a manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { unmatchedOffender, unmatchedInmate ->
          givenAPlacementRequest(
            placementRequestAllocatedTo = user,
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = unmatchedOffender.otherIds.crn,
          ) { unmatchedPlacementRequest, _ ->
            webTestClient.get()
              .uri("/placement-requests/dashboard")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    placementRequestTransformer.transformJpaToApi(
                      unmatchedPlacementRequest,
                      PersonInfoResult.Success.Full(unmatchedOffender.otherIds.crn, unmatchedOffender, unmatchedInmate),
                    ),
                  ),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `It returns the unmatched placement requests and withdrawn placement requests when the user is a manager and status is not defined`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { unmatchedOffender, unmatchedInmate ->
          givenAPlacementRequest(
            placementRequestAllocatedTo = user,
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = unmatchedOffender.otherIds.crn,
          ) { unmatchedPlacementRequest, _ ->
            val withdrawnPlacementRequest = createPlacementRequest(unmatchedOffender, user, isWithdrawn = true)

            webTestClient.get()
              .uri("/placement-requests/dashboard")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    placementRequestTransformer.transformJpaToApi(
                      unmatchedPlacementRequest,
                      PersonInfoResult.Success.Full(unmatchedOffender.otherIds.crn, unmatchedOffender, unmatchedInmate),
                    ),
                    placementRequestTransformer.transformJpaToApi(
                      withdrawnPlacementRequest,
                      PersonInfoResult.Success.Full(unmatchedOffender.otherIds.crn, unmatchedOffender, unmatchedInmate),
                    ),
                  ),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `It returns the unmatched placement requests and ignores the withdrawn placement requests when the user is a manager and status is notMatched`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { unmatchedOffender, unmatchedInmate ->
          givenAPlacementRequest(
            placementRequestAllocatedTo = user,
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = unmatchedOffender.otherIds.crn,
          ) { unmatchedPlacementRequest, _ ->
            createPlacementRequest(unmatchedOffender, user, isWithdrawn = true)

            webTestClient.get()
              .uri("/placement-requests/dashboard?status=notMatched")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    placementRequestTransformer.transformJpaToApi(
                      unmatchedPlacementRequest,
                      PersonInfoResult.Success.Full(unmatchedOffender.otherIds.crn, unmatchedOffender, unmatchedInmate),
                    ),
                  ),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `It returns the matched placement requests and ignores the withdrawn placement requests when the user is a manager and status is matched`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { matchedOffender, matchedInmate ->

          fun matchPlacementRequest(placementRequest: PlacementRequestEntity) {
            val premises = approvedPremisesEntityFactory.produceAndPersist {
              withProbationRegion(probationRegion)
              withLocalAuthorityArea(
                localAuthorityEntityFactory.produceAndPersist(),
              )
            }

            val room = roomEntityFactory.produceAndPersist {
              withPremises(premises)
            }

            val bed = bedEntityFactory.produceAndPersist {
              withRoom(room)
            }

            placementRequest.booking = bookingEntityFactory.produceAndPersist {
              withPremises(premises)
              withBed(bed)
            }

            realPlacementRequestRepository.save(placementRequest)
          }

          givenAPlacementRequest(
            placementRequestAllocatedTo = user,
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = matchedOffender.otherIds.crn,
            isWithdrawn = true,
          ) { placementRequest, _ ->
            matchPlacementRequest(placementRequest)
          }

          givenAPlacementRequest(
            placementRequestAllocatedTo = user,
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = matchedOffender.otherIds.crn,
          ) { matchedPlacementRequest, _ ->
            matchPlacementRequest(matchedPlacementRequest)

            webTestClient.get()
              .uri("/placement-requests/dashboard?status=matched")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    placementRequestTransformer.transformJpaToApi(
                      matchedPlacementRequest,
                      PersonInfoResult.Success.Full(matchedOffender.otherIds.crn, matchedOffender, matchedInmate),
                    ),
                  ),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `It returns the unable to match placement requests and ignores the withdrawn placement requests when the user is a manager and status is unableToMatch`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { unableToMatchOffender, unableToMatchInmate ->

          givenAPlacementRequest(
            placementRequestAllocatedTo = user,
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = unableToMatchOffender.otherIds.crn,
            isWithdrawn = true,
          ) { unableToMatchPlacementRequest, _ ->
            unableToMatchPlacementRequest.bookingNotMades = mutableListOf(
              bookingNotMadeFactory.produceAndPersist {
                withPlacementRequest(unableToMatchPlacementRequest)
              },
            )
          }

          givenAPlacementRequest(
            placementRequestAllocatedTo = user,
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = unableToMatchOffender.otherIds.crn,
          ) { unableToMatchPlacementRequest, _ ->
            unableToMatchPlacementRequest.bookingNotMades = mutableListOf(
              bookingNotMadeFactory.produceAndPersist {
                withPlacementRequest(unableToMatchPlacementRequest)
              },
            )

            webTestClient.get()
              .uri("/placement-requests/dashboard?status=unableToMatch")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    placementRequestTransformer.transformJpaToApi(
                      unableToMatchPlacementRequest,
                      PersonInfoResult.Success.Full(
                        unableToMatchOffender.otherIds.crn,
                        unableToMatchOffender,
                        unableToMatchInmate,
                      ),
                    ),
                  ),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `It returns paginated placement requests when the user is a manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val placementRequest = createPlacementRequest(offenderDetails, user)

          webTestClient.get()
            .uri("/placement-requests/dashboard?page=1")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 1)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                listOf(
                  placementRequestTransformer.transformJpaToApi(
                    placementRequest,
                    PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                  ),
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `It searches by name via crnOrName when the user is a manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = {
            withFirstName("JOHN")
            withLastName("SMITH")
          },
        ) { offenderDetails, inmateDetails ->
          givenAnOffender { otherOffenderDetails, _ ->
            val placementRequest = createPlacementRequest(offenderDetails, user)
            createPlacementRequest(otherOffenderDetails, user)

            webTestClient.get()
              .uri("/placement-requests/dashboard?crnOrName=john")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    placementRequestTransformer.transformJpaToApi(
                      placementRequest,
                      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                    ),
                  ),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `It searches by crn via crnOrName when the user is a manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { otherOffenderDetails, _ ->
          givenAnOffender { offenderDetails, inmateDetails ->
            val placementRequest = createPlacementRequest(offenderDetails, user)
            createPlacementRequest(otherOffenderDetails, user)

            webTestClient.get()
              .uri("/placement-requests/dashboard?crnOrName=${offenderDetails.otherIds.crn}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    placementRequestTransformer.transformJpaToApi(
                      placementRequest,
                      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                    ),
                  ),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `It searches by arrivalDateStart where user is manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          createPlacementRequest(offenderDetails, user, expectedArrival = LocalDate.of(2022, 1, 1))
          val placementRequest5thJan = createPlacementRequest(offenderDetails, user, expectedArrival = LocalDate.of(2022, 1, 5))
          val placementRequest10thJan = createPlacementRequest(offenderDetails, user, expectedArrival = LocalDate.of(2022, 1, 10))

          webTestClient.get()
            .uri("/placement-requests/dashboard?arrivalDateStart=2022-01-04")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                listOf(
                  placementRequestTransformer.transformJpaToApi(
                    placementRequest5thJan,
                    PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                  ),
                  placementRequestTransformer.transformJpaToApi(
                    placementRequest10thJan,
                    PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                  ),
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `It searches by arrivalDateEnd where user is manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val placementRequest1stJan = createPlacementRequest(offenderDetails, user, expectedArrival = LocalDate.of(2022, 1, 1))
          val placementRequest5thJan = createPlacementRequest(offenderDetails, user, expectedArrival = LocalDate.of(2022, 1, 5))
          createPlacementRequest(offenderDetails, user, expectedArrival = LocalDate.of(2022, 1, 10))

          webTestClient.get()
            .uri("/placement-requests/dashboard?arrivalDateEnd=2022-01-09")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                listOf(
                  placementRequestTransformer.transformJpaToApi(
                    placementRequest1stJan,
                    PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                  ),
                  placementRequestTransformer.transformJpaToApi(
                    placementRequest5thJan,
                    PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                  ),
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `It searches by tier where user is manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          createPlacementRequest(offenderDetails, user, tier = RiskTierLevel.a0)
          val placementRequestA1 = createPlacementRequest(offenderDetails, user, tier = RiskTierLevel.a1)
          createPlacementRequest(offenderDetails, user, tier = RiskTierLevel.a2)

          webTestClient.get()
            .uri("/placement-requests/dashboard?tier=A1")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                listOf(
                  placementRequestTransformer.transformJpaToApi(
                    placementRequestA1,
                    PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                  ),
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `It searches by AP Area ID where user is manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          val apArea1 = givenAnApArea()
          val apArea2 = givenAnApArea()

          createPlacementRequest(offenderDetails, user, apArea = apArea1)
          val placementRequestA1 = createPlacementRequest(offenderDetails, user, apArea = apArea2)
          createPlacementRequest(offenderDetails, user, apArea = apArea1)

          webTestClient.get()
            .uri("/placement-requests/dashboard?apAreaId=${apArea2.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                listOf(
                  placementRequestTransformer.transformJpaToApi(
                    placementRequestA1,
                    PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                  ),
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `It searches by CRU Management Area ID where user is manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          val cruArea1 = givenACas1CruManagementArea()
          val cruArea2 = givenACas1CruManagementArea()

          createPlacementRequest(offenderDetails, user, cruManagementArea = cruArea1)
          val placementRequestA1 = createPlacementRequest(offenderDetails, user, cruManagementArea = cruArea2)
          createPlacementRequest(offenderDetails, user, cruManagementArea = cruArea1)

          webTestClient.get()
            .uri("/placement-requests/dashboard?cruManagementAreaId=${cruArea2.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                listOf(
                  placementRequestTransformer.transformJpaToApi(
                    placementRequestA1,
                    PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                  ),
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `It searches by requestType where type is standardRelease`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          createPlacementRequest(offenderDetails, user, isParole = true)
          val standardRelease = createPlacementRequest(offenderDetails, user, isParole = false)

          webTestClient.get()
            .uri("/placement-requests/dashboard?requestType=${PlacementRequestRequestType.standardRelease.name}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                listOf(
                  placementRequestTransformer.transformJpaToApi(
                    standardRelease,
                    PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                  ),
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `It searches by requestType where type is parole`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          createPlacementRequest(offenderDetails, user, isParole = false)
          val parole = createPlacementRequest(offenderDetails, user, isParole = true)

          webTestClient.get()
            .uri("/placement-requests/dashboard?requestType=${PlacementRequestRequestType.parole.name}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                listOf(
                  placementRequestTransformer.transformJpaToApi(
                    parole,
                    PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                  ),
                ),
              ),
            )
        }
      }
    }

    @Deprecated("Can be removed when apAreaId is removed from search filter in the front end.")
    @Test
    fun `It searches using multiple criteria where user is manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { offender1Details, inmate1Details ->
          givenAnOffender { offender2Details, _ ->

            val apArea1 = givenAnApArea()
            val apArea2 = givenAnApArea()

            createPlacementRequest(offender1Details, user, expectedArrival = LocalDate.of(2022, 1, 1), tier = RiskTierLevel.a2)
            createPlacementRequest(offender1Details, user, expectedArrival = LocalDate.of(2022, 1, 5), tier = RiskTierLevel.a1)
            val placementOffender1On5thJanTierA2Parole =
              createPlacementRequest(offender1Details, user, expectedArrival = LocalDate.of(2022, 1, 5), tier = RiskTierLevel.a2, isParole = true, apArea = apArea1)
            createPlacementRequest(offender1Details, user, expectedArrival = LocalDate.of(2022, 1, 5), tier = RiskTierLevel.a2, isParole = true, apArea = apArea2)
            createPlacementRequest(offender1Details, user, expectedArrival = LocalDate.of(2022, 1, 5), tier = RiskTierLevel.a2, isParole = false)
            createPlacementRequest(offender2Details, user, expectedArrival = LocalDate.of(2022, 1, 5), tier = RiskTierLevel.a2)
            createPlacementRequest(offender1Details, user, expectedArrival = LocalDate.of(2022, 1, 10), tier = RiskTierLevel.a2)

            webTestClient.get()
              .uri("/placement-requests/dashboard?arrivalDateStart=2022-01-02&arrivalDateEnd=2022-01-09&crnOrName=${offender1Details.otherIds.crn}&tier=A2&requestType=parole&apAreaId=${apArea1.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    placementRequestTransformer.transformJpaToApi(
                      placementOffender1On5thJanTierA2Parole,
                      PersonInfoResult.Success.Full(offender1Details.otherIds.crn, offender1Details, inmate1Details),
                    ),
                  ),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `It searches using multiple criteria including CRU management area id where user is manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { offender1Details, inmate1Details ->
          givenAnOffender { offender2Details, _ ->

            val cruArea1 = givenACas1CruManagementArea()
            val cruArea2 = givenACas1CruManagementArea()

            createPlacementRequest(offender1Details, user, expectedArrival = LocalDate.of(2022, 1, 1), tier = RiskTierLevel.a2)
            createPlacementRequest(offender1Details, user, expectedArrival = LocalDate.of(2022, 1, 5), tier = RiskTierLevel.a1)
            val placementOffender1On5thJanTierA2Parole =
              createPlacementRequest(offender1Details, user, expectedArrival = LocalDate.of(2022, 1, 5), tier = RiskTierLevel.a2, isParole = true, cruManagementArea = cruArea1)
            createPlacementRequest(offender1Details, user, expectedArrival = LocalDate.of(2022, 1, 5), tier = RiskTierLevel.a2, isParole = true, cruManagementArea = cruArea2)
            createPlacementRequest(offender1Details, user, expectedArrival = LocalDate.of(2022, 1, 5), tier = RiskTierLevel.a2, isParole = false)
            createPlacementRequest(offender2Details, user, expectedArrival = LocalDate.of(2022, 1, 5), tier = RiskTierLevel.a2)
            createPlacementRequest(offender1Details, user, expectedArrival = LocalDate.of(2022, 1, 10), tier = RiskTierLevel.a2)

            webTestClient.get()
              .uri(
                "/placement-requests/dashboard?arrivalDateStart=2022-01-02&arrivalDateEnd=2022-01-09&crnOrName=${offender1Details.otherIds.crn}" +
                  "&tier=A2&requestType=parole&cruManagementAreaId=${cruArea1.id}",
              )
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    placementRequestTransformer.transformJpaToApi(
                      placementOffender1On5thJanTierA2Parole,
                      PersonInfoResult.Success.Full(offender1Details.otherIds.crn, offender1Details, inmate1Details),
                    ),
                  ),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `It sorts by duration when the user is a manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val placementRequestWithOneDayDuration = createPlacementRequest(offenderDetails, user, duration = 1)
          val placementRequestWithFiveDayDuration = createPlacementRequest(offenderDetails, user, duration = 5)
          val placementRequestWithTwelveDayDuration = createPlacementRequest(offenderDetails, user, duration = 12)

          webTestClient.get()
            .uri("/placement-requests/dashboard?page=1&sortBy=duration")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequestWithOneDayDuration.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequestWithFiveDayDuration.id.toString())
            .jsonPath("$[2].id").isEqualTo(placementRequestWithTwelveDayDuration.id.toString())

          webTestClient.get()
            .uri("/placement-requests/dashboard?page=1&sortBy=duration&sortDirection=desc")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequestWithTwelveDayDuration.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequestWithFiveDayDuration.id.toString())
            .jsonPath("$[2].id").isEqualTo(placementRequestWithOneDayDuration.id.toString())
        }
      }
    }

    @Test
    fun `It sorts by expectedArrival when the user is a manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val placementRequestWithExpectedArrivalOfToday = createPlacementRequest(offenderDetails, user)
          val placementRequestWithExpectedArrivalInTwelveDays =
            createPlacementRequest(offenderDetails, user, expectedArrival = LocalDate.now().plusDays(12))
          val placementRequestWithExpectedArrivalInThirtyDays =
            createPlacementRequest(offenderDetails, user, expectedArrival = LocalDate.now().plusDays(30))

          webTestClient.get()
            .uri("/placement-requests/dashboard?page=1&sortBy=expected_arrival")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequestWithExpectedArrivalOfToday.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequestWithExpectedArrivalInTwelveDays.id.toString())
            .jsonPath("$[2].id").isEqualTo(placementRequestWithExpectedArrivalInThirtyDays.id.toString())

          webTestClient.get()
            .uri("/placement-requests/dashboard?page=1&sortBy=expected_arrival&sortDirection=desc")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequestWithExpectedArrivalInThirtyDays.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequestWithExpectedArrivalInTwelveDays.id.toString())
            .jsonPath("$[2].id").isEqualTo(placementRequestWithExpectedArrivalOfToday.id.toString())
        }
      }
    }

    @Test
    fun `It sorts by requestType when the user is a manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val placementRequest1WithStatusParole = createPlacementRequest(offenderDetails, user, isParole = true)
          val placementRequest2WithStatusStandard = createPlacementRequest(offenderDetails, user, isParole = false)

          webTestClient.get()
            .uri("/placement-requests/dashboard?page=1&sortBy=request_type&sortDirection=asc")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequest1WithStatusParole.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequest2WithStatusStandard.id.toString())

          webTestClient.get()
            .uri("/placement-requests/dashboard?page=1&sortBy=request_type&sortDirection=desc")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequest2WithStatusStandard.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequest1WithStatusParole.id.toString())
        }
      }
    }

    @Test
    fun `It sorts by personName when the user is a manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->

        val (offenderJohnSmithDetails, _) = givenAnOffender(
          offenderDetailsConfigBlock = {
            withFirstName("JOHN")
            withLastName("SMITH")
          },
        )

        val (offenderZackeryZoikesDetails, _) = givenAnOffender(
          offenderDetailsConfigBlock = {
            withFirstName("ZAKERY")
            withLastName("ZOIKES")
          },
        )

        val (offenderHarryHarrisonDetails, _) = givenAnOffender(
          offenderDetailsConfigBlock = {
            withFirstName("HARRY")
            withLastName("HARRISON")
          },
        )

        val placementRequestJohnSmith = createPlacementRequest(offenderJohnSmithDetails, user)
        val placementRequestZakeryZoikes = createPlacementRequest(offenderZackeryZoikesDetails, user)
        val placementRequestHarryHarrisonDetails = createPlacementRequest(offenderHarryHarrisonDetails, user)

        webTestClient.get()
          .uri("/placement-requests/dashboard?page=1&sortBy=person_name&sortDirection=asc")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$[0].id").isEqualTo(placementRequestHarryHarrisonDetails.id.toString())
          .jsonPath("$[1].id").isEqualTo(placementRequestJohnSmith.id.toString())
          .jsonPath("$[2].id").isEqualTo(placementRequestZakeryZoikes.id.toString())

        webTestClient.get()
          .uri("/placement-requests/dashboard?page=1&sortBy=person_name&sortDirection=desc")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$[0].id").isEqualTo(placementRequestZakeryZoikes.id.toString())
          .jsonPath("$[1].id").isEqualTo(placementRequestJohnSmith.id.toString())
          .jsonPath("$[2].id").isEqualTo(placementRequestHarryHarrisonDetails.id.toString())
      }
    }

    @Test
    fun `It sorts by personRisksTier when the user is a manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val placementRequest1TierB1 = createPlacementRequest(offenderDetails, user, tier = RiskTierLevel.b1)
          val placementRequest2TierA0 = createPlacementRequest(offenderDetails, user, tier = RiskTierLevel.a0)
          val placementRequest3TierA1 = createPlacementRequest(offenderDetails, user, tier = RiskTierLevel.a1)

          webTestClient.get()
            .uri("/placement-requests/dashboard?page=1&sortBy=person_risks_tier&sortDirection=asc")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequest2TierA0.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequest3TierA1.id.toString())
            .jsonPath("$[2].id").isEqualTo(placementRequest1TierB1.id.toString())

          webTestClient.get()
            .uri("/placement-requests/dashboard?page=1&sortBy=person_risks_tier&sortDirection=desc")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequest1TierB1.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequest3TierA1.id.toString())
            .jsonPath("$[2].id").isEqualTo(placementRequest2TierA0.id.toString())
        }
      }
    }

    @Test
    fun `It sorts by createdAt when the user is a manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val placementRequestCreatedToday = createPlacementRequest(offenderDetails, user)
          val placementRequestCreatedFiveDaysAgo =
            createPlacementRequest(offenderDetails, user, createdAt = OffsetDateTime.now().minusDays(5))
          val placementRequestCreatedThirtyDaysAgo =
            createPlacementRequest(offenderDetails, user, createdAt = OffsetDateTime.now().minusDays(30))

          webTestClient.get()
            .uri("/placement-requests/dashboard?page=1&sortBy=created_at")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequestCreatedThirtyDaysAgo.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequestCreatedFiveDaysAgo.id.toString())
            .jsonPath("$[2].id").isEqualTo(placementRequestCreatedToday.id.toString())

          webTestClient.get()
            .uri("/placement-requests/dashboard?page=1&sortBy=created_at&sortDirection=desc")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequestCreatedToday.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequestCreatedFiveDaysAgo.id.toString())
            .jsonPath("$[2].id").isEqualTo(placementRequestCreatedThirtyDaysAgo.id.toString())
        }
      }
    }

    @Test
    fun `It sorts by applicationSubmittedAt when the user is a manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val placementRequestWithApplicationCreatedToday = createPlacementRequest(offenderDetails, user)
          val placementRequestWithApplicationCreatedTwelveDaysAgo =
            createPlacementRequest(offenderDetails, user, applicationDate = OffsetDateTime.now().minusDays(12))
          val placementRequestWithApplicationCreatedThirtyDaysAgo =
            createPlacementRequest(offenderDetails, user, applicationDate = OffsetDateTime.now().minusDays(30))

          webTestClient.get()
            .uri("/placement-requests/dashboard?page=1&sortBy=application_date")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequestWithApplicationCreatedThirtyDaysAgo.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequestWithApplicationCreatedTwelveDaysAgo.id.toString())
            .jsonPath("$[2].id").isEqualTo(placementRequestWithApplicationCreatedToday.id.toString())

          webTestClient.get()
            .uri("/placement-requests/dashboard?page=1&sortBy=application_date&sortDirection=desc")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequestWithApplicationCreatedToday.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequestWithApplicationCreatedTwelveDaysAgo.id.toString())
            .jsonPath("$[2].id").isEqualTo(placementRequestWithApplicationCreatedThirtyDaysAgo.id.toString())
        }
      }
    }

    @Suppress("LongParameterList")
    private fun createPlacementRequest(
      offenderDetails: OffenderDetailSummary,
      user: UserEntity,
      duration: Int = 12,
      expectedArrival: LocalDate = LocalDate.now(),
      createdAt: OffsetDateTime = OffsetDateTime.now(),
      applicationDate: OffsetDateTime = OffsetDateTime.now(),
      isWithdrawn: Boolean = false,
      tier: RiskTierLevel = RiskTierLevel.b1,
      isParole: Boolean = false,
      apArea: ApAreaEntity? = null,
      cruManagementArea: Cas1CruManagementAreaEntity? = null,
    ): PlacementRequestEntity {
      val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
        withPermissiveSchema()
      }

      val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
        withPermissiveSchema()
      }

      val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCrn(offenderDetails.otherIds.crn)
        withCreatedByUser(user)
        withSubmittedAt(OffsetDateTime.now())
        withApplicationSchema(applicationSchema)
        withReleaseType("licence")
        withSubmittedAt(applicationDate)
        withName("${offenderDetails.firstName} ${offenderDetails.surname}")
        withRiskRatings(
          PersonRisksFactory()
            .withTier(
              RiskWithStatus(
                RiskTier(
                  level = tier.value,
                  lastUpdated = LocalDate.parse("2023-06-26"),
                ),
              ),
            ).produce(),
        )
        withApArea(apArea)
        cruManagementArea?.let {
          withCruManagementArea(it)
        }
      }

      val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
        withAssessmentSchema(assessmentSchema)
        withApplication(application)
        withSubmittedAt(OffsetDateTime.now())
        withAllocatedToUser(user)
        withDecision(AssessmentDecision.ACCEPTED)
      }

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

      return placementRequestFactory.produceAndPersist {
        withAllocatedToUser(
          userEntityFactory.produceAndPersist {
            withProbationRegion(probationRegion)
          },
        )
        withApplication(application)
        withAssessment(assessment)
        withPlacementRequirements(placementRequirements)
        withDuration(duration)
        withExpectedArrival(expectedArrival)
        withCreatedAt(createdAt)
        withIsWithdrawn(isWithdrawn)
        withIsParole(isParole)
      }
    }
  }

  @Nested
  inner class GetPlacementRequest {

    @Autowired
    lateinit var placementRequestDetailTransformer: PlacementRequestDetailTransformer

    @Test
    fun `Without a JWT returns 401`() {
      webTestClient.get()
        .uri("/placement-requests/62faf6f4-1dac-4139-9a18-09c1b2852a0f")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Not allocated to calling User without WORKFLOW_MANAGER role returns 403`() {
      givenAUser { _, jwt ->
        givenAUser { otherUser, _ ->
          givenAnOffender { offenderDetails, _ ->
            givenAnApplication(createdByUser = otherUser) {
              givenAPlacementRequest(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              ) { placementRequest, _ ->
                webTestClient.get()
                  .uri("/placement-requests/${placementRequest.id}")
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

    @Test
    fun `Allocated to calling User, offender not LAO, returns 200`() {
      givenAUser { user, jwt ->
        givenAUser { otherUser, _ ->
          givenAnOffender { offenderDetails, inmateDetails ->
            givenAPlacementRequest(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            ) { placementRequest, _ ->
              webTestClient.get()
                .uri("/placement-requests/${placementRequest.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    placementRequestDetailTransformer.transformJpaToApi(
                      placementRequest,
                      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                      listOf(),
                    ),
                  ),
                )
            }
          }
        }
      }
    }

    @Test
    fun `Allocated to calling User, offender is LAO, user doesn't have LAO access or LAO qualification, returns 403`() {
      givenAUser { user, jwt ->
        givenAUser { otherUser, _ ->
          givenAnOffender(
            offenderDetailsConfigBlock = {
              withCurrentExclusion(true)
            },
          ) { offenderDetails, _ ->
            givenAPlacementRequest(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            ) { placementRequest, _ ->
              webTestClient.get()
                .uri("/placement-requests/${placementRequest.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isForbidden
            }
          }
        }
      }
    }

    @Test
    fun `Allocated to calling User, offender is LAO, user has LAO access, returns 200`() {
      givenAUser { user, jwt ->
        givenAUser { otherUser, _ ->
          givenAnOffender(
            offenderDetailsConfigBlock = {
              withCurrentExclusion(true)
            },
          ) { offenderDetails, inmateDetails ->
            givenAPlacementRequest(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            ) { placementRequest, _ ->
              apDeliusContextAddResponseToUserAccessCall(
                listOf(
                  CaseAccessFactory()
                    .withCrn(offenderDetails.otherIds.crn)
                    .produce(),
                ),
                user.deliusUsername,
              )

              webTestClient.get()
                .uri("/placement-requests/${placementRequest.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    placementRequestDetailTransformer.transformJpaToApi(
                      placementRequest,
                      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                      listOf(),
                    ),
                  ),
                )
            }
          }
        }
      }
    }

    @Test
    fun `Allocated to calling User, offender is LAO, user doesn't have LAO access but has LAO qualification, returns 200`() {
      givenAUser(qualifications = listOf(UserQualification.LAO)) { user, jwt ->
        givenAUser { otherUser, _ ->
          givenAnOffender(
            offenderDetailsConfigBlock = {
              withCurrentExclusion(true)
            },
          ) { offenderDetails, inmateDetails ->
            givenAPlacementRequest(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            ) { placementRequest, _ ->
              webTestClient.get()
                .uri("/placement-requests/${placementRequest.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    placementRequestDetailTransformer.transformJpaToApi(
                      placementRequest,
                      PersonInfoResult.Success.Restricted(
                        offenderDetails.otherIds.crn,
                        offenderDetails.otherIds.nomsNumber,
                      ),
                      listOf(),
                    ),
                  ),
                )
            }
          }
        }
      }
    }

    @Test
    fun `Allocated to calling User, offender not LAO, returns 200 with cancellations when they exist`() {
      givenAUser { user, jwt ->
        givenAUser { otherUser, _ ->
          givenAnOffender { offenderDetails, inmateDetails ->
            givenAPlacementRequest(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            ) { placementRequest, _ ->
              val premises = approvedPremisesEntityFactory.produceAndPersist {
                withProbationRegion(probationRegion)
                withLocalAuthorityArea(
                  localAuthorityEntityFactory.produceAndPersist(),
                )
              }

              val room = roomEntityFactory.produceAndPersist {
                withPremises(premises)
              }

              val bed = bedEntityFactory.produceAndPersist {
                withRoom(room)
              }

              val booking = bookingEntityFactory.produceAndPersist {
                withPremises(premises)
                withCrn(offenderDetails.otherIds.crn)
                withBed(bed)
                withServiceName(ServiceName.approvedPremises)
                withApplication(placementRequest.application)
              }

              val cancellations = cancellationEntityFactory.produceAndPersistMultiple(2) {
                withBooking(booking)
                withReason(cancellationReasonEntityFactory.produceAndPersist())
              }

              webTestClient.get()
                .uri("/placement-requests/${placementRequest.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    placementRequestDetailTransformer.transformJpaToApi(
                      placementRequestRepository.findByIdOrNull(placementRequest.id)!!,
                      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                      cancellations,
                    ),
                  ),
                )
            }
          }
        }
      }
    }
  }

  @Nested
  inner class CreateBookingFromPlacementRequest {
    @Test
    fun `Create a Booking from PR without a JWT returns 401`() {
      webTestClient.post()
        .uri("/placement-requests/62faf6f4-1dac-4139-9a18-09c1b2852a0f/booking")
        .bodyValue(
          NewPlacementRequestBooking(
            arrivalDate = LocalDate.parse("2023-03-29"),
            departureDate = LocalDate.parse("2023-04-01"),
            bedId = UUID.fromString("d5dfd808-b8f4-4cc0-a0ac-fdce7144126e"),
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Create a Booking from a PR creates a domain event and sends booking made emails`() {
      givenAUser { user, jwt ->
        givenAUser { applicant, _ ->
          givenAnOffender { offenderDetails, _ ->
            givenAnApplication(createdByUser = applicant) {
              givenAPlacementRequest(
                placementRequestAllocatedTo = user,
                assessmentAllocatedTo = applicant,
                createdByUser = applicant,
                crn = offenderDetails.otherIds.crn,
              ) { placementRequest, _ ->
                val premises = approvedPremisesEntityFactory.produceAndPersist {
                  withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
                  withYieldedProbationRegion { probationRegion }
                }

                val room = roomEntityFactory.produceAndPersist {
                  withPremises(premises)
                }

                val bed = bedEntityFactory.produceAndPersist {
                  withRoom(room)
                }

                webTestClient.post()
                  .uri("/placement-requests/${placementRequest.id}/booking")
                  .header("Authorization", "Bearer $jwt")
                  .bodyValue(
                    NewPlacementRequestBooking(
                      arrivalDate = LocalDate.parse("2023-03-29"),
                      departureDate = LocalDate.parse("2023-04-01"),
                      bedId = bed.id,
                    ),
                  )
                  .exchange()
                  .expectStatus()
                  .isOk

                domainEventAsserter.assertDomainEventOfTypeStored(
                  placementRequest.application.id,
                  DomainEventType.APPROVED_PREMISES_BOOKING_MADE,
                )

                emailAsserter.assertEmailsRequestedCount(2)
                emailAsserter.assertEmailRequested(
                  applicant.email!!,
                  notifyConfig.templates.bookingMade,
                )
                emailAsserter.assertEmailRequested(
                  premises.emailAddress!!,
                  notifyConfig.templates.bookingMadePremises,
                )
              }
            }
          }
        }
      }
    }

    @Test
    fun `Create a Booking from a PR linked to a placement application creates a domain event and sends booking made emails`() {
      givenAUser { user, jwt ->
        givenAUser { applicant, _ ->
          givenAUser { placementApplicationCreator, _ ->
            givenAnOffender { offenderDetails, _ ->
              givenAnApplication(createdByUser = applicant) {
                givenAPlacementApplication(createdByUser = placementApplicationCreator) { placementApplication ->
                  givenAPlacementRequest(
                    placementRequestAllocatedTo = user,
                    assessmentAllocatedTo = applicant,
                    createdByUser = applicant,
                    crn = offenderDetails.otherIds.crn,
                    placementApplication = placementApplication,
                  ) { placementRequest, _ ->
                    val premises = approvedPremisesEntityFactory.produceAndPersist {
                      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
                      withYieldedProbationRegion { probationRegion }
                    }

                    val room = roomEntityFactory.produceAndPersist {
                      withPremises(premises)
                    }

                    val bed = bedEntityFactory.produceAndPersist {
                      withRoom(room)
                    }

                    webTestClient.post()
                      .uri("/placement-requests/${placementRequest.id}/booking")
                      .header("Authorization", "Bearer $jwt")
                      .bodyValue(
                        NewPlacementRequestBooking(
                          arrivalDate = LocalDate.parse("2023-03-29"),
                          departureDate = LocalDate.parse("2023-04-01"),
                          bedId = bed.id,
                        ),
                      )
                      .exchange()
                      .expectStatus()
                      .isOk

                    domainEventAsserter.assertDomainEventOfTypeStored(
                      placementRequest.application.id,
                      DomainEventType.APPROVED_PREMISES_BOOKING_MADE,
                    )

                    emailAsserter.assertEmailsRequestedCount(3)
                    emailAsserter.assertEmailRequested(
                      applicant.email!!,
                      notifyConfig.templates.bookingMade,
                    )
                    emailAsserter.assertEmailRequested(
                      placementApplicationCreator.email!!,
                      notifyConfig.templates.bookingMade,
                    )
                    emailAsserter.assertEmailRequested(
                      premises.emailAddress!!,
                      notifyConfig.templates.bookingMadePremises,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    @Test
    fun `Create a Booking from a PR allocated to another user without 'CAS1_BOOKING_CREATE' permission returns a 403`() {
      givenAUser(
        roles = UserRole.entries.filter {
          it != UserRole.CAS1_CRU_MEMBER && it != UserRole.CAS1_WORKFLOW_MANAGER && it != UserRole.CAS1_JANITOR
        },
      ) { _, jwt ->
        givenAUser { otherUser, _ ->
          givenAnOffender { offenderDetails, _ ->
            givenAnApplication(createdByUser = otherUser) {
              givenAPlacementRequest(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              ) { placementRequest, _ ->
                webTestClient.post()
                  .uri("/placement-requests/${placementRequest.id}/booking")
                  .header("Authorization", "Bearer $jwt")
                  .bodyValue(
                    NewPlacementRequestBooking(
                      arrivalDate = LocalDate.parse("2023-03-29"),
                      departureDate = LocalDate.parse("2023-04-01"),
                      bedId = UUID.fromString("d5dfd808-b8f4-4cc0-a0ac-fdce7144126e"),
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
    }

    @Test
    fun `Create a Booking from a PR for bed id returns a 200`() {
      givenAUser { user, jwt ->
        givenAUser { applicant, _ ->
          givenAnOffender { offenderDetails, _ ->
            givenAnApplication(createdByUser = applicant) {
              givenAPlacementRequest(
                placementRequestAllocatedTo = user,
                assessmentAllocatedTo = applicant,
                createdByUser = applicant,
                crn = offenderDetails.otherIds.crn,
              ) { placementRequest, _ ->
                val premises = approvedPremisesEntityFactory.produceAndPersist {
                  withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
                  withYieldedProbationRegion { probationRegion }
                }

                val room = roomEntityFactory.produceAndPersist {
                  withPremises(premises)
                }

                val bed = bedEntityFactory.produceAndPersist {
                  withRoom(room)
                }

                webTestClient.post()
                  .uri("/placement-requests/${placementRequest.id}/booking")
                  .header("Authorization", "Bearer $jwt")
                  .bodyValue(
                    NewPlacementRequestBooking(
                      arrivalDate = LocalDate.parse("2023-03-29"),
                      departureDate = LocalDate.parse("2023-04-01"),
                      bedId = bed.id,
                    ),
                  )
                  .exchange()
                  .expectStatus()
                  .isOk
              }
            }
          }
        }
      }
    }

    @Test
    fun `Create a Booking from a PR for premises id returns a 200`() {
      givenAUser { user, jwt ->
        givenAUser { otherUser, _ ->
          givenAnOffender { offenderDetails, _ ->
            givenAnApplication(createdByUser = otherUser) {
              givenAPlacementRequest(
                placementRequestAllocatedTo = user,
                assessmentAllocatedTo = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              ) { placementRequest, _ ->
                val premises = approvedPremisesEntityFactory.produceAndPersist {
                  withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
                  withYieldedProbationRegion { probationRegion }
                }

                webTestClient.post()
                  .uri("/placement-requests/${placementRequest.id}/booking")
                  .header("Authorization", "Bearer $jwt")
                  .bodyValue(
                    NewPlacementRequestBooking(
                      arrivalDate = LocalDate.parse("2023-03-29"),
                      departureDate = LocalDate.parse("2023-04-01"),
                      bedId = null,
                      premisesId = premises.id,
                    ),
                  )
                  .exchange()
                  .expectStatus()
                  .isOk
              }
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(
      value = UserRole::class,
      mode = EnumSource.Mode.INCLUDE,
      names = ["CAS1_CRU_MEMBER", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR"],
    )
    fun `Create a Booking from a PR allocated to other user and current user has the 'CAS1_BOOKING_CREATE' permission returns a 200`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAUser { otherUser, _ ->
          givenAnOffender { offenderDetails, _ ->
            givenAnApplication(createdByUser = otherUser) {
              givenAPlacementRequest(
                placementRequestAllocatedTo = user,
                assessmentAllocatedTo = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              ) { placementRequest, _ ->
                val premises = approvedPremisesEntityFactory.produceAndPersist {
                  withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
                  withYieldedProbationRegion { probationRegion }
                }

                webTestClient.post()
                  .uri("/placement-requests/${placementRequest.id}/booking")
                  .header("Authorization", "Bearer $jwt")
                  .bodyValue(
                    NewPlacementRequestBooking(
                      arrivalDate = LocalDate.parse("2023-03-29"),
                      departureDate = LocalDate.parse("2023-04-01"),
                      bedId = null,
                      premisesId = premises.id,
                    ),
                  )
                  .exchange()
                  .expectStatus()
                  .isOk
              }
            }
          }
        }
      }
    }
  }

  @Nested
  inner class CreateBookingNotMadeFromPlacementRequest {
    @Test
    fun `Create a Booking Not Made from a Placement Request without a JWT returns 401`() {
      webTestClient.post()
        .uri("/placement-requests/62faf6f4-1dac-4139-9a18-09c1b2852a0f/booking-not-made")
        .bodyValue(
          NewBookingNotMade(
            notes = "some notes",
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Create a Booking Not Made from a Placement Request returns 200 and creates a domain event`() {
      givenAUser { _, jwt ->
        givenAUser { otherUser, _ ->
          givenAnOffender { offenderDetails, _ ->
            givenAnApplication(createdByUser = otherUser) {
              givenAPlacementRequest(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              ) { placementRequest, _ ->
                webTestClient.post()
                  .uri("/placement-requests/${placementRequest.id}/booking-not-made")
                  .header("Authorization", "Bearer $jwt")
                  .bodyValue(
                    NewBookingNotMade(
                      notes = "some notes",
                    ),
                  )
                  .exchange()
                  .expectStatus()
                  .isOk
                  .expectBody()
                  .jsonPath("$.placementRequestId").isEqualTo(placementRequest.id.toString())
                  .jsonPath("$.notes").isEqualTo("some notes")

                domainEventAsserter.assertDomainEventOfTypeStored(
                  placementRequest.application.id,
                  DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE,
                )
              }
            }
          }
        }
      }
    }
  }

  /**
   * Note - Withdrawal cascading is tested in [WithdrawalTest]
   */
  @Nested
  inner class WithdrawPlacementRequest {
    @Test
    fun `Withdraw Placement Request without a JWT returns 401`() {
      webTestClient.post()
        .uri("/placement-requests/62faf6f4-1dac-4139-9a18-09c1b2852a0f/withdrawal")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Withdraw Placement Request without CAS1_WORKFLOW_MANAGER returns 403`() {
      givenAUser { creator, _ ->
        givenAUser { user, jwt ->
          givenAnOffender { offenderDetails, inmateDetails ->
            givenAnApplication(createdByUser = user) {
              givenAPlacementRequest(
                placementRequestAllocatedTo = user,
                assessmentAllocatedTo = user,
                createdByUser = creator,
                crn = offenderDetails.otherIds.crn,
              ) { placementRequest, _ ->
                webTestClient.post()
                  .uri("/placement-requests/${placementRequest.id}/withdrawal")
                  .header("Authorization", "Bearer $jwt")
                  .bodyValue(
                    WithdrawPlacementRequest(
                      reason = WithdrawPlacementRequestReason.duplicatePlacementRequest,
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
    }

    @SuppressWarnings("MaxLineLength")
    @Test
    fun `Withdraw Placement Request returns 200, sets isWithdrawn to true, raises domain event, sends email to CRU and Applicant if it represents dates included on application on submission`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          givenAPlacementRequest(
            placementRequestAllocatedTo = user,
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = offenderDetails.otherIds.crn,
            apArea = givenAnApArea(),
            cruManagementArea = givenACas1CruManagementArea(),
          ) { placementRequest, _ ->

            webTestClient.post()
              .uri("/placement-requests/${placementRequest.id}/withdrawal")
              .bodyValue(
                WithdrawPlacementRequest(
                  reason = WithdrawPlacementRequestReason.duplicatePlacementRequest,
                ),
              )
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .jsonPath("$.person.crn").isEqualTo(placementRequest.application.crn)

            val persistedPlacementRequest = placementRequestRepository.findByIdOrNull(placementRequest.id)!!
            assertThat(persistedPlacementRequest.isWithdrawn).isTrue
            assertThat(persistedPlacementRequest.withdrawalReason).isEqualTo(PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST)

            snsDomainEventListener.blockForMessage(DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN)

            emailAsserter.assertEmailsRequestedCount(2)
            emailAsserter.assertEmailRequested(
              placementRequest.application.cruManagementArea!!.emailAddress!!,
              notifyConfig.templates.matchRequestWithdrawnV2,
            )
            emailAsserter.assertEmailRequested(
              placementRequest.application.createdByUser.email!!,
              notifyConfig.templates.placementRequestWithdrawnV2,
            )
          }
        }
      }
    }
  }
}
