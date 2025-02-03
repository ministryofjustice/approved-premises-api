package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskWrapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NameFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForTemporaryAccommodation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision.ACCEPTED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision.REJECTED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision.WITHDRAW
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision.WITHDRAWN_BY_PP
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.UserWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.TaskTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.ceil

class TasksTest {

  @Nested
  inner class GetTasksTest {

    @SuppressWarnings("LargeClass")
    @Nested
    inner class PermissionsTest : IntegrationTestBase() {
      @Autowired
      lateinit var taskTransformer: TaskTransformer

      @BeforeEach
      fun stubBankHolidaysApi() {
        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()
      }

      @Test
      fun `Get all tasks without JWT returns 401`() {
        webTestClient.get()
          .uri("/task")
          .exchange()
          .expectStatus()
          .isUnauthorized
      }

      @Test
      fun `Get all tasks without workflow manager, matcher or assessor permissions returns 403`() {
        givenAUser { _, jwt ->
          webTestClient.get()
            .uri("/tasks")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_MATCHER", "CAS1_WORKFLOW_MANAGER", "CAS1_ASSESSOR"])
      fun `Get all tasks returns 200 when have CAS1_WORKFLOW_MANAGER, CAS1_MATCHER or CAS1_ASSESSOR roles`(role: UserRole) {
        givenAUser(roles = listOf(role)) { _, jwt ->
          givenAUser { otherUser, _ ->
            givenAnOffender { offenderDetails, _ ->

              val offenderSummaries = getOffenderSummaries(offenderDetails)

              val task = givenAPlacementApplication(
                createdByUser = otherUser,
                allocatedToUser = otherUser,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )

              val expectedTasks = listOf(
                taskTransformer.transformPlacementApplicationToTask(
                  task,
                  offenderSummaries,
                ),
              )

              webTestClient.get()
                .uri("/tasks?page=1&sortBy=createdAt&sortDirection=asc")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    expectedTasks,
                  ),
                )
            }
          }
        }
      }

      @Test
      fun `Get all tasks returns 200 when no type retains original sort order`() {
        givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
          givenAUser { otherUser, _ ->
            givenAnOffender { offenderDetails, _ ->

              val offenderSummaries = getOffenderSummaries(offenderDetails)

              val (task2, _) = givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )

              val task4 = givenAPlacementApplication(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )

              val (task5) = givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )

              val expectedTasks = listOf(
                taskTransformer.transformAssessmentToTask(
                  task2,
                  offenderSummaries,
                ),
                taskTransformer.transformPlacementApplicationToTask(
                  task4,
                  offenderSummaries,
                ),
                taskTransformer.transformAssessmentToTask(
                  task5,
                  offenderSummaries,
                ),
              )

              webTestClient.get()
                .uri("/tasks?page=1&sortBy=createdAt&sortDirection=asc")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    expectedTasks,
                  ),
                )

              webTestClient.get()
                .uri("/tasks?page=1&sortBy=createdAt&sortDirection=desc")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    expectedTasks.reversed(),
                  ),
                )
            }
          }
        }
      }
    }

    @Nested
    inner class FilterByType : InitialiseDatabasePerClassTestBase() {
      private lateinit var tasks: Map<TaskType, List<Task>>
      lateinit var jwt: String

      @Autowired
      lateinit var taskTransformer: TaskTransformer

      @BeforeAll
      fun stubBankHolidaysApi() {
        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()
      }

      @BeforeAll
      fun setup() {
        givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
          givenAUser { otherUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              this.jwt = jwt

              val offenderSummaries = getOffenderSummaries(offenderDetails)

              val (task2, _) = givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )

              val task4 = givenAPlacementApplication(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )

              val (task5) = givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )

              val placementApplications = listOf(
                taskTransformer.transformPlacementApplicationToTask(
                  task4,
                  offenderSummaries,
                ),
              )

              val assessments = listOf(
                taskTransformer.transformAssessmentToTask(
                  task2,
                  offenderSummaries,
                ),
                taskTransformer.transformAssessmentToTask(
                  task5,
                  offenderSummaries,
                ),
              )

              tasks = mapOf(
                TaskType.assessment to assessments,
                TaskType.placementApplication to placementApplications,
              )
            }
          }
        }
      }

      @ParameterizedTest
      @EnumSource(value = TaskType::class, names = ["assessment", "placementApplication"])
      fun `Get all tasks filters by a single type`(taskType: TaskType) {
        val url = "/tasks?page=1&sortBy=createdAt&sortDirection=asc&types=${taskType.value}"
        val expectedTasks = tasks[taskType]!!.sortedBy { it.dueDate }

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              expectedTasks,
            ),
          )
      }

      @ParameterizedTest
      @CsvSource("assessment,placementApplication")
      fun `Get all tasks filters by multiple types`(taskType1: TaskType, taskType2: TaskType) {
        val url = "/tasks?page=1&sortBy=createdAt&sortDirection=asc&types=${taskType1.value}&types=${taskType2.value}"
        val expectedTasks = listOf(
          tasks[taskType1]!!,
          tasks[taskType2]!!,
        ).flatten().sortedBy { it.dueDate }

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              expectedTasks,
            ),
          )
      }

      @Test
      fun `Get all tasks returns all task types`() {
        val url = "/tasks?page=1&sortBy=createdAt&sortDirection=asc&types=Assessment&types=PlacementApplication"
        val expectedTasks = listOf(
          tasks[TaskType.assessment]!!,
          tasks[TaskType.placementApplication]!!,
        ).flatten().sortedBy { it.dueDate }

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              expectedTasks,
            ),
          )
      }

      @Test
      fun `Get all tasks returns all task types by default`() {
        val url = "/tasks?page=1&sortBy=createdAt&sortDirection=asc"
        val expectedTasks = listOf(
          tasks[TaskType.assessment]!!,
          tasks[TaskType.placementApplication]!!,
        ).flatten().sortedBy { it.dueDate }

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              expectedTasks,
            ),
          )
      }
    }

    @Deprecated("Superseded by FilterByCruManagementArea")
    @Nested
    inner class FilterByApArea : InitialiseDatabasePerClassTestBase() {
      private lateinit var tasks: Map<TaskType, List<Task>>

      lateinit var jwt: String
      lateinit var apArea: ApAreaEntity

      @Autowired
      lateinit var taskTransformer: TaskTransformer

      @BeforeAll
      fun setup() {
        givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
          givenAUser { otherUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              this.jwt = jwt

              apArea = givenAnApArea()
              val apArea2 = givenAnApArea()

              val offenderSummaries = getOffenderSummaries(offenderDetails)

              val (assessment) = givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
                apArea = apArea,
              )

              givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
                apArea = apArea2,
              )

              val placementApplication = givenAPlacementApplication(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
                apArea = apArea,
              )

              givenAPlacementApplication(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
                apArea = apArea2,
              )

              val assessments = listOf(
                taskTransformer.transformAssessmentToTask(
                  assessment,
                  offenderSummaries,
                ),
              )

              val placementApplications = listOf(
                taskTransformer.transformPlacementApplicationToTask(
                  placementApplication,
                  offenderSummaries,
                ),
              )

              tasks = mapOf(
                TaskType.assessment to assessments,
                TaskType.placementApplication to placementApplications,
              )
            }
          }
        }
      }

      @ParameterizedTest
      @EnumSource(value = TaskType::class, names = ["assessment", "placementApplication"])
      fun `it filters by Ap Area and task type`(taskType: TaskType) {
        val expectedTasks = tasks[taskType]
        val url = "/tasks?type=${taskType.value}&apAreaId=${apArea.id}"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              expectedTasks,
            ),
          )
      }

      @Test
      fun `it filters by all areas with no task type`() {
        val expectedTasks = listOf(
          tasks[TaskType.assessment]!!,
          tasks[TaskType.placementApplication]!!,
        ).flatten().sortedBy { it.dueDate }

        webTestClient.get()
          .uri("/tasks?apAreaId=${apArea.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              expectedTasks,
            ),
          )
      }
    }

    @Nested
    inner class FilterByCruManagementArea : InitialiseDatabasePerClassTestBase() {
      private lateinit var tasks: Map<TaskType, List<Task>>

      lateinit var jwt: String
      lateinit var cruArea: Cas1CruManagementAreaEntity

      @Autowired
      lateinit var taskTransformer: TaskTransformer

      @BeforeAll
      fun setup() {
        givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
          givenAUser { otherUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              this.jwt = jwt

              cruArea = givenACas1CruManagementArea()
              val cruArea2 = givenACas1CruManagementArea()

              val offenderSummaries = getOffenderSummaries(offenderDetails)

              val (assessment) = givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
                cruManagementArea = cruArea,
              )

              givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
                cruManagementArea = cruArea2,
              )

              val placementApplication = givenAPlacementApplication(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
                cruManagementArea = cruArea,
              )

              givenAPlacementApplication(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
                cruManagementArea = cruArea2,
              )

              val assessments = listOf(
                taskTransformer.transformAssessmentToTask(
                  assessment,
                  offenderSummaries,
                ),
              )

              val placementApplications = listOf(
                taskTransformer.transformPlacementApplicationToTask(
                  placementApplication,
                  offenderSummaries,
                ),
              )

              tasks = mapOf(
                TaskType.assessment to assessments,
                TaskType.placementApplication to placementApplications,
              )
            }
          }
        }
      }

      @ParameterizedTest
      @EnumSource(value = TaskType::class, names = ["assessment", "placementApplication"])
      fun `it filters by CRU area and task type`(taskType: TaskType) {
        val expectedTasks = tasks[taskType]
        val url = "/tasks?type=${taskType.value}&cruManagementAreaId=${cruArea.id}"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              expectedTasks,
            ),
          )
      }

      @Test
      fun `it filters by all areas with no task type`() {
        val expectedTasks = listOf(
          tasks[TaskType.assessment]!!,
          tasks[TaskType.placementApplication]!!,
        ).flatten().sortedBy { it.dueDate }

        webTestClient.get()
          .uri("/tasks?cruManagementAreaId=${cruArea.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              expectedTasks,
            ),
          )
      }
    }

    @Nested
    inner class FilterByUser : InitialiseDatabasePerClassTestBase() {
      private lateinit var tasks: Map<TaskType, List<Task>>

      lateinit var jwt: String
      lateinit var user: UserEntity

      @Autowired
      lateinit var taskTransformer: TaskTransformer

      @BeforeAll
      fun setup() {
        givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
          givenAUser { otherUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              this.jwt = jwt
              this.user = user

              val offenderSummaries = getOffenderSummaries(offenderDetails)

              val (allocatableAssessment) = givenAnAssessmentForApprovedPremises(
                allocatedToUser = user,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )

              givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )

              val allocatablePlacementApplication = givenAPlacementApplication(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )

              givenAPlacementApplication(
                createdByUser = user,
                allocatedToUser = otherUser,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )

              val assessments = listOf(
                taskTransformer.transformAssessmentToTask(
                  allocatableAssessment,
                  offenderSummaries,
                ),
              )

              val placementApplications = listOf(
                taskTransformer.transformPlacementApplicationToTask(
                  allocatablePlacementApplication,
                  offenderSummaries,
                ),
              )

              tasks = mapOf(
                TaskType.assessment to assessments,
                TaskType.placementApplication to placementApplications,
              )
            }
          }
        }
      }

      @ParameterizedTest
      @EnumSource(value = TaskType::class, names = ["assessment", "placementApplication"])
      fun `it filters by user and task type`(taskType: TaskType) {
        val expectedTasks = tasks[taskType]
        val url = "/tasks?type=${taskType.value}&allocatedToUserId=${user.id}"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              expectedTasks,
            ),
          )
      }

      @Test
      fun `it filters by user with all tasks`() {
        val expectedTasks = listOf(
          tasks[TaskType.assessment]!!,
          tasks[TaskType.placementApplication]!!,
        ).flatten().sortedBy { it.dueDate }

        val url = "/tasks?allocatedToUserId=${user.id}"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              expectedTasks,
            ),
          )
      }
    }

    @Nested
    inner class PaginationAndWithdrawalExclusion : InitialiseDatabasePerClassTestBase() {
      private val pageSize = 1
      private lateinit var counts: Map<TaskType, Map<String, Int>>

      lateinit var jwt: String

      @BeforeAll
      fun setup() {
        givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
          givenAUser { otherUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              this.jwt = jwt

              counts = mapOf(
                TaskType.assessment to mapOf(
                  "allocated" to 2,
                  "unallocated" to 3,
                  "withdrawn" to 1,
                ),
                TaskType.placementApplication to mapOf(
                  "allocated" to 3,
                  "unallocated" to 2,
                  "withdrawn" to 1,
                ),
              )

              givenAnAssessmentForTemporaryAccommodation(
                createdByUser = otherUser,
                allocatedToUser = null,
              )

              repeat(counts[TaskType.assessment]!!["allocated"]!!) {
                givenAnAssessmentForApprovedPremises(
                  allocatedToUser = otherUser,
                  createdByUser = otherUser,
                  crn = offenderDetails.otherIds.crn,
                )
              }

              repeat(counts[TaskType.assessment]!!["unallocated"]!!) {
                givenAnAssessmentForApprovedPremises(
                  null,
                  createdByUser = otherUser,
                  crn = offenderDetails.otherIds.crn,
                )
              }

              repeat(counts[TaskType.assessment]!!["withdrawn"]!!) {
                givenAnAssessmentForApprovedPremises(
                  null,
                  createdByUser = otherUser,
                  crn = offenderDetails.otherIds.crn,
                  isWithdrawn = true,
                )
              }

              repeat(counts[TaskType.placementApplication]!!["allocated"]!!) {
                givenAPlacementApplication(
                  createdByUser = user,
                  allocatedToUser = user,
                  schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                    withPermissiveSchema()
                  },
                  crn = offenderDetails.otherIds.crn,
                  submittedAt = OffsetDateTime.now(),
                )
              }

              repeat(counts[TaskType.placementApplication]!!["unallocated"]!!) {
                givenAPlacementApplication(
                  createdByUser = user,
                  schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                    withPermissiveSchema()
                  },
                  crn = offenderDetails.otherIds.crn,
                  submittedAt = OffsetDateTime.now(),
                )
              }

              repeat(counts[TaskType.placementApplication]!!["withdrawn"]!!) {
                givenAPlacementApplication(
                  createdByUser = user,
                  schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                    withPermissiveSchema()
                  },
                  crn = offenderDetails.otherIds.crn,
                  submittedAt = OffsetDateTime.now(),
                  isWithdrawn = true,
                )
              }
            }
          }
        }
      }

      @ParameterizedTest
      @CsvSource(
        "assessment,allocated,1",
        "assessment,allocated,2",
        "assessment,unallocated,1",
        "assessment,unallocated,1",
        "placementApplication,allocated,1",
        "placementApplication,allocated,2",
        "placementApplication,unallocated,1",
        "placementApplication,unallocated,2",
      )
      fun `get all tasks returns page counts when taskType and allocated filter are set`(
        taskType: TaskType,
        allocatedFilter: String,
        pageNumber: String,
      ) {
        val itemCount = counts[taskType]!![allocatedFilter]!!
        val url = "/tasks?type=${taskType.value}&perPage=$pageSize&page=$pageNumber&allocatedFilter=$allocatedFilter"

        expectCountHeaders(url, pageNumber.toInt(), itemCount)
      }

      @ParameterizedTest
      @CsvSource(
        "allocated,1",
        "allocated,2",
        "unallocated,1",
        "unallocated,1",
      )
      fun `get all tasks returns page counts for all tasks when allocated filter is set`(
        allocatedFilter: String,
        pageNumber: String,
      ) {
        val itemCount = listOf(
          counts[TaskType.assessment]!![allocatedFilter]!!,
          counts[TaskType.placementApplication]!![allocatedFilter]!!,
        ).sum()

        val url = "/tasks?&page=$pageNumber&perPage=$pageSize&allocatedFilter=$allocatedFilter"

        expectCountHeaders(url, pageNumber.toInt(), itemCount)
      }

      @ParameterizedTest
      @ValueSource(ints = [1, 2])
      fun `get all tasks returns page count when no allocated filter is set`(pageNumber: Int) {
        val itemCount = listOf(
          counts[TaskType.assessment]!!["allocated"]!!,
          counts[TaskType.assessment]!!["unallocated"]!!,
          counts[TaskType.placementApplication]!!["allocated"]!!,
          counts[TaskType.placementApplication]!!["unallocated"]!!,
        ).sum()

        expectCountHeaders("/tasks?&page=$pageNumber&perPage=$pageSize", pageNumber, itemCount)
      }

      private fun expectCountHeaders(url: String, pageNumber: Int, itemCount: Int) {
        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectHeader().valueEquals("X-Pagination-CurrentPage", pageNumber.toLong())
          .expectHeader().valueEquals("X-Pagination-TotalPages", expectedTotalPages(itemCount))
          .expectHeader().valueEquals("X-Pagination-TotalResults", itemCount.toLong())
          .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
      }

      private fun expectedTotalPages(count: Int) = ceil(count.toDouble() / pageSize).toLong()
    }

    @Nested
    inner class FilterQualification : InitialiseDatabasePerClassTestBase() {
      lateinit var jwt: String

      lateinit var tasks: Map<TaskType, Map<UserQualification, List<Task>>>

      @Autowired
      lateinit var taskTransformer: TaskTransformer

      @BeforeAll
      fun setup() {
        givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
          givenAUser { otherUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              this.jwt = jwt

              val offenderSummaries = getOffenderSummaries(offenderDetails)
              val assessmentTasks = mutableMapOf<UserQualification, List<Task>>()
              val placementApplicationTasks = mutableMapOf<UserQualification, List<Task>>()

              fun createAssessmentTask(
                requiredQualification: UserQualification?,
                noticeType: Cas1ApplicationTimelinessCategory? = Cas1ApplicationTimelinessCategory.standard,
              ): Task {
                val (assessment) = givenAnAssessmentForApprovedPremises(
                  allocatedToUser = otherUser,
                  createdByUser = otherUser,
                  crn = offenderDetails.otherIds.crn,
                  requiredQualification = requiredQualification,
                  noticeType = noticeType,
                )

                return taskTransformer.transformAssessmentToTask(
                  assessment,
                  offenderSummaries,
                )
              }

              fun createPlacementApplicationTask(
                requiredQualification: UserQualification?,
                noticeType: Cas1ApplicationTimelinessCategory? = Cas1ApplicationTimelinessCategory.standard,
              ): Task {
                val placementApplication = givenAPlacementApplication(
                  createdByUser = user,
                  allocatedToUser = user,
                  schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                    withPermissiveSchema()
                  },
                  crn = offenderDetails.otherIds.crn,
                  submittedAt = OffsetDateTime.now(),
                  requiredQualification = requiredQualification,
                  noticeType = noticeType,
                )

                return taskTransformer.transformPlacementApplicationToTask(
                  placementApplication,
                  offenderSummaries,
                )
              }

              listOf(
                UserQualification.ESAP,
                UserQualification.PIPE,
                UserQualification.RECOVERY_FOCUSED,
                UserQualification.MENTAL_HEALTH_SPECIALIST,
              ).forEach { qualification ->
                assessmentTasks[qualification] = listOf(
                  createAssessmentTask(qualification),
                )
                placementApplicationTasks[qualification] = listOf(
                  createPlacementApplicationTask(qualification),
                )
              }

              assessmentTasks[UserQualification.EMERGENCY] = listOf(
                createAssessmentTask(null, Cas1ApplicationTimelinessCategory.shortNotice),
                createAssessmentTask(null, Cas1ApplicationTimelinessCategory.emergency),
              )
              placementApplicationTasks[UserQualification.EMERGENCY] = listOf(
                createPlacementApplicationTask(null, Cas1ApplicationTimelinessCategory.shortNotice),
                createPlacementApplicationTask(null, Cas1ApplicationTimelinessCategory.emergency),
              )

              tasks = mapOf(
                TaskType.assessment to assessmentTasks,
                TaskType.placementApplication to placementApplicationTasks,
              )
            }
          }
        }
      }

      @ParameterizedTest
      @CsvSource(
        "assessment,PIPE",
        "assessment,ESAP",
        "assessment,EMERGENCY",
        "assessment,RECOVERY_FOCUSED",
        "assessment,MENTAL_HEALTH_SPECIALIST",

        "placementApplication,PIPE",
        "placementApplication,ESAP",
        "placementApplication,EMERGENCY",
        "placementApplication,RECOVERY_FOCUSED",
        "placementApplication,MENTAL_HEALTH_SPECIALIST",
      )
      fun `Get all tasks filters by task type and required qualification`(
        taskType: TaskType,
        qualification: UserQualification,
      ) {
        val url = "/tasks?type=${taskType.value}&requiredQualification=${qualification.name.lowercase()}"
        val expectedTasks = tasks[taskType]!![qualification]!!

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              expectedTasks,
            ),
          )
      }

      @ParameterizedTest
      @EnumSource(
        value = UserQualification::class,
        names = ["EMERGENCY", "ESAP", "PIPE", "RECOVERY_FOCUSED", "MENTAL_HEALTH_SPECIALIST"],
      )
      fun `Get all tasks required qualification`(qualification: UserQualification) {
        val url = "/tasks?requiredQualification=${qualification.name.lowercase()}"
        val expectedTasks = listOf(
          tasks[TaskType.assessment]!![qualification]!!,
          tasks[TaskType.placementApplication]!![qualification]!!,
        ).flatten()

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              expectedTasks,
            ),
          )
      }
    }

    @Nested
    inner class FilterByNameOrCrn : InitialiseDatabasePerClassTestBase() {
      lateinit var jwt: String
      lateinit var crn: String

      private lateinit var nameMatchTasks: Map<TaskType, Task>
      private lateinit var crnMatchTasks: Map<TaskType, Task>

      @Autowired
      lateinit var taskTransformer: TaskTransformer

      @BeforeAll
      fun setup() {
        givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
          givenAUser { otherUser, _ ->
            givenAnOffender { offenderDetails1, _ ->
              givenAnOffender { offenderDetails2, _ ->
                this.jwt = jwt
                this.crn = offenderDetails2.otherIds.crn

                val offenderSummaries1 = getOffenderSummaries(offenderDetails1)
                val offenderSummaries2 = getOffenderSummaries(offenderDetails2)
                val (assessment1, _) = givenAnAssessmentForApprovedPremises(
                  allocatedToUser = otherUser,
                  createdByUser = otherUser,
                  crn = offenderDetails1.otherIds.crn,
                  name = "SOMEONE",
                )

                val (assessment2, _) = givenAnAssessmentForApprovedPremises(
                  allocatedToUser = otherUser,
                  createdByUser = otherUser,
                  crn = offenderDetails2.otherIds.crn,
                  name = "ANOTHER",
                )

                val placementApplication1 = givenAPlacementApplication(
                  createdByUser = user,
                  allocatedToUser = user,
                  schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                    withPermissiveSchema()
                  },
                  crn = offenderDetails1.otherIds.crn,
                  name = "SOMEONE",
                  submittedAt = OffsetDateTime.now(),
                )

                val placementApplication2 = givenAPlacementApplication(
                  createdByUser = user,
                  allocatedToUser = user,
                  schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                    withPermissiveSchema()
                  },
                  crn = offenderDetails2.otherIds.crn,
                  submittedAt = OffsetDateTime.now(),
                  name = "ANOTHER",
                )

                nameMatchTasks = mapOf(
                  TaskType.assessment to taskTransformer.transformAssessmentToTask(
                    assessment1,
                    offenderSummaries1,
                  ),
                  TaskType.placementApplication to taskTransformer.transformPlacementApplicationToTask(
                    placementApplication1,
                    offenderSummaries1,
                  ),
                )

                crnMatchTasks = mapOf(
                  TaskType.assessment to taskTransformer.transformAssessmentToTask(
                    assessment2,
                    offenderSummaries2,
                  ),
                  TaskType.placementApplication to taskTransformer.transformPlacementApplicationToTask(
                    placementApplication2,
                    offenderSummaries2,
                  ),
                )
              }
            }
          }
        }
      }

      @ParameterizedTest
      @EnumSource(value = TaskType::class, names = ["assessment", "placementApplication"])
      fun `Get all tasks filters by name and task type`(taskType: TaskType) {
        val url = "/tasks?type=${taskType.value}&crnOrName=someone"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              listOf(nameMatchTasks[taskType]),
            ),
          )
      }

      @ParameterizedTest
      @EnumSource(value = TaskType::class, names = ["assessment", "placementApplication"])
      fun `Get all tasks filters by CRN and task type`(taskType: TaskType) {
        val url = "/tasks?type=${taskType.value}&crnOrName=$crn"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              listOf(crnMatchTasks[taskType]),
            ),
          )
      }

      @Test
      fun `Get all tasks filters by name without task type`() {
        val url = "/tasks?crnOrName=someone"
        val expectedTasks = listOf(
          nameMatchTasks[TaskType.assessment],
          nameMatchTasks[TaskType.placementApplication],
        )

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              expectedTasks,
            ),
          )
      }

      @Test
      fun `Get all tasks filters by CRN without task type`() {
        val url = "/tasks?crnOrName=$crn"
        val expectedTasks = listOf(
          crnMatchTasks[TaskType.assessment],
          crnMatchTasks[TaskType.placementApplication],
        )

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              expectedTasks,
            ),
          )
      }
    }

    @Nested
    inner class FilterByCompleted : InitialiseDatabasePerClassTestBase() {
      lateinit var jwt: String
      lateinit var crn: String

      private lateinit var incompleteTasks: List<Task>
      private lateinit var completeTasks: List<Task>

      @Autowired
      lateinit var taskTransformer: TaskTransformer

      @BeforeAll
      fun setup() {
        givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
          givenAUser { otherUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              this.jwt = jwt
              this.crn = offenderDetails.otherIds.crn

              val offenderSummaries = getOffenderSummaries(offenderDetails)

              val (assessment1, _) = givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
                createdAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS),
              )

              val (assessment2, _) = givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )

              val placementApplication1 = givenAPlacementApplication(
                createdByUser = otherUser,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )

              val placementApplication2 = givenAPlacementApplication(
                createdByUser = otherUser,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
                decision = ACCEPTED,
              )

              incompleteTasks = listOf(
                taskTransformer.transformAssessmentToTask(
                  assessment1,
                  offenderSummaries,
                ),
                taskTransformer.transformPlacementApplicationToTask(
                  placementApplication1,
                  offenderSummaries,
                ),
              )

              completeTasks = listOf(
                taskTransformer.transformAssessmentToTask(
                  assessment2,
                  offenderSummaries,
                ),
                taskTransformer.transformAssessmentToTask(
                  assessmentTestRepository.findAllByApplication(placementApplication1.application)[0],
                  offenderSummaries,
                ),
                taskTransformer.transformAssessmentToTask(
                  assessmentTestRepository.findAllByApplication(placementApplication2.application)[0],
                  offenderSummaries,
                ),
                taskTransformer.transformPlacementApplicationToTask(
                  placementApplication2,
                  offenderSummaries,
                ),
              )
            }
          }
        }
      }

      @Test
      fun `Get all tasks shows incomplete tasks by default`() {
        val url = "/tasks"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              incompleteTasks,
            ),
          )
      }

      @Test
      fun `Get all tasks shows allows showing completed tasks`() {
        val url = "/tasks?isCompleted=true"

        objectMapper.setDateFormat(SimpleDateFormat("yyyy-mm-dd'T'HH:mm:ss"))

        val rawResponseBody = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .returnResult<String>()
          .responseBody
          .blockFirst()

        val responseBody = objectMapper.readValue(rawResponseBody, object : TypeReference<List<Task>>() {})

        assertThat(responseBody)
          .usingRecursiveFieldByFieldElementComparator(
            RecursiveComparisonConfiguration.builder().withComparatorForType(
              { a: Instant, b: Instant ->
                a.truncatedTo(ChronoUnit.MILLIS).compareTo(b.truncatedTo(ChronoUnit.MILLIS))
              },
              Instant::class.java,
            ).build(),
          )
          .hasSameElementsAs(completeTasks)
      }
    }

    @Nested
    inner class SortByTest : InitialiseDatabasePerClassTestBase() {
      lateinit var jwt: String
      lateinit var crn: String

      private lateinit var tasks: Map<UUID, Task>
      private lateinit var assessments: Map<UUID, AssessmentEntity>
      private lateinit var placementRequests: Map<UUID, PlacementRequestEntity>
      private lateinit var placementApplications: Map<UUID, PlacementApplicationEntity>

      @Autowired
      lateinit var taskTransformer: TaskTransformer

      @BeforeAll
      fun setup() {
        givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
          givenAUser { otherUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              this.jwt = jwt
              this.crn = offenderDetails.otherIds.crn

              val (assessment1, _) = givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
                createdAt = OffsetDateTime.now().minusDays(14).randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
                submittedAt = OffsetDateTime.now().randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
                dueAt = OffsetDateTime.now().randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
              )

              val (assessment2, _) = givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
                createdAt = OffsetDateTime.now().minusDays(14).randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
                submittedAt = OffsetDateTime.now().randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
                dueAt = OffsetDateTime.now().randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
              )

              val (placementRequest1, _) = givenAPlacementRequest(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
                booking = bookingEntityFactory.produceAndPersist {
                  withPremises(
                    approvedPremisesEntityFactory.produceAndPersist {
                      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
                      withYieldedProbationRegion { givenAProbationRegion() }
                    },
                  )
                },
              )

              val (placementRequest2, _) = givenAPlacementRequest(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
                dueAt = OffsetDateTime.now().randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
                booking = bookingEntityFactory.produceAndPersist {
                  withPremises(
                    approvedPremisesEntityFactory.produceAndPersist {
                      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
                      withYieldedProbationRegion { givenAProbationRegion() }
                    },
                  )
                },
              )

              val offenderSummaries = getOffenderSummaries(offenderDetails)
              val (placementRequest3, _) = givenAPlacementRequest(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
                dueAt = OffsetDateTime.now().randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
              )

              placementRequest3.bookingNotMades = mutableListOf(
                bookingNotMadeFactory.produceAndPersist {
                  withPlacementRequest(placementRequest3)
                },
              )

              val placementApplication1 = givenAPlacementApplication(
                createdByUser = otherUser,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now().randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
                dueAt = OffsetDateTime.now().randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
                decision = REJECTED,
              )

              val placementApplication2 = givenAPlacementApplication(
                createdByUser = otherUser,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now().randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
                dueAt = OffsetDateTime.now().randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
                decision = ACCEPTED,
              )

              assessments = mapOf(
                assessment1.id to assessment1,
                assessment2.id to assessment2,
                placementRequest1.assessment.id to placementRequest1.assessment,
                placementRequest2.assessment.id to placementRequest2.assessment,
                placementRequest3.assessment.id to placementRequest3.assessment,
                placementApplication1.application.getLatestAssessment()!!.id to placementApplication1.application.getLatestAssessment()!!,
                placementApplication2.application.getLatestAssessment()!!.id to placementApplication2.application.getLatestAssessment()!!,
              )

              placementRequests = mapOf(
                placementRequest1.id to placementRequest1,
                placementRequest2.id to placementRequest2,
                placementRequest3.id to placementRequest3,
              )

              placementApplications = mapOf(
                placementApplication1.id to placementApplication1,
                placementApplication2.id to placementApplication2,
              )

              tasks = mapOf()
              tasks += assessments.mapValues {
                taskTransformer.transformAssessmentToTask(
                  it.value,
                  offenderSummaries,
                )
              }
              tasks += placementApplications.mapValues {
                taskTransformer.transformPlacementApplicationToTask(
                  it.value,
                  offenderSummaries,
                )
              }
            }
          }
        }
      }

      @Test
      fun `Get all tasks sorts by createdAt in ascending order by default`() {
        val url = "/tasks?isCompleted=true"

        println(objectMapper.writeValueAsString(tasksSortedByCreatedAt()))

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              tasksSortedByCreatedAt(),
            ),
          )
      }

      @Test
      fun `Get all tasks sorts by createdAt in ascending order`() {
        val url = "/tasks?isCompleted=true&sortBy=createdAt&sortDirection=asc"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              tasksSortedByCreatedAt(),
            ),
          )
      }

      @Test
      fun `Get all tasks sorts by createdAt in descending order`() {
        val url = "/tasks?isCompleted=true&sortBy=createdAt&sortDirection=desc"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              tasksSortedByCreatedAt(SortDirection.desc),
            ),
          )
      }

      @Test
      fun `Get all tasks sorts by dueAt in ascending order`() {
        val url = "/tasks?isCompleted=true&sortBy=dueAt&sortDirection=asc"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              tasksSortedByDueAt(),
            ),
          )
      }

      @Test
      fun `Get all tasks sorts by dueAt in descending order`() {
        val url = "/tasks?isCompleted=true&sortBy=dueAt&sortDirection=desc"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              tasksSortedByDueAt(SortDirection.desc),
            ),
          )
      }

      @Test
      fun `Get all tasks sorts by allocatedTo in ascending order`() {
        val url = "/tasks?isCompleted=true&sortBy=allocatedTo&sortDirection=asc"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              tasksSortedByAllocatedTo(),
            ),
          )
      }

      @Test
      fun `Get all tasks sorts by allocatedTo in descending order`() {
        val url = "/tasks?isCompleted=true&sortBy=allocatedTo&sortDirection=desc"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              tasksSortedByAllocatedTo(SortDirection.desc),
            ),
          )
      }

      @Test
      fun `Get all tasks sorts by person in ascending order`() {
        val url = "/tasks?isCompleted=true&sortBy=person&sortDirection=asc"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              tasksSortedByPerson(),
            ),
          )
      }

      @Test
      fun `Get all tasks sorts by person in descending order`() {
        val url = "/tasks?isCompleted=true&sortBy=person&sortDirection=desc"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              tasksSortedByPerson(SortDirection.desc),
            ),
          )
      }

      @Test
      fun `Get all tasks sorts by completedAt in ascending order`() {
        val url = "/tasks?isCompleted=true&sortBy=completedAt&sortDirection=asc"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              tasksSortedByCompletedAt(),
            ),
          )
      }

      @Test
      fun `Get all tasks sorts by completedAt in descending order`() {
        val url = "/tasks?isCompleted=true&sortBy=completedAt&sortDirection=desc"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              tasksSortedByCompletedAt(SortDirection.desc),
            ),
          )
      }

      @Test
      fun `Get all tasks sorts by taskType in ascending order`() {
        val url = "/tasks?isCompleted=true&sortBy=taskType&sortDirection=asc"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              tasksSortedByTaskType(),
            ),
          )
      }

      @Test
      fun `Get all tasks sorts by taskType in descending order`() {
        val url = "/tasks?isCompleted=true&sortBy=taskType&sortDirection=desc"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              tasksSortedByTaskType(SortDirection.desc),
            ),
          )
      }

      @Test
      fun `Get all tasks sorts by decision in ascending order`() {
        val url = "/tasks?isCompleted=true&sortBy=decision&sortDirection=asc"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              tasksSortedByDecision(),
            ),
          )
      }

      @Test
      fun `Get all tasks sorts by decision in descending order`() {
        val url = "/tasks?isCompleted=true&sortBy=decision&sortDirection=desc"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              tasksSortedByDecision(SortDirection.desc),
            ),
          )
      }

      private fun tasksSortedByCreatedAt(sortDirection: SortDirection = SortDirection.asc) =
        sortTasks(sortDirection) { id: UUID ->
          val createdAt = when (val task = tasks[id]!!) {
            is AssessmentTask -> assessments[id]!!.createdAt
            is PlacementRequestTask -> placementRequests[id]!!.createdAt
            is PlacementApplicationTask -> placementApplications[id]!!.createdAt
            else -> fail("Unexpected task type ${task::class.qualifiedName}")
          }

          createdAt.toInstant()
        }

      private fun tasksSortedByDueAt(sortDirection: SortDirection = SortDirection.asc) =
        sortTasks(sortDirection) { id: UUID ->
          tasks[id]!!.dueAt
        }

      private fun tasksSortedByPerson(sortDirection: SortDirection = SortDirection.asc) =
        sortTasks(sortDirection) { id: UUID ->
          tasks[id]!!.personName
        }

      private fun tasksSortedByAllocatedTo(sortDirection: SortDirection = SortDirection.asc) =
        sortTasks(sortDirection) { id: UUID ->
          tasks[id]!!.allocatedToStaffMember!!.name
        }

      private fun tasksSortedByCompletedAt(sortDirection: SortDirection = SortDirection.asc) =
        sortTasks(sortDirection) { id: UUID ->
          tasks[id]!!.outcomeRecordedAt
        }

      private fun tasksSortedByTaskType(sortDirection: SortDirection = SortDirection.asc) =
        sortTasks(sortDirection) { id: UUID ->
          tasks[id]!!.taskType
        }

      private fun tasksSortedByDecision(sortDirection: SortDirection = SortDirection.asc) =
        sortTasks(sortDirection) { id: UUID ->
          when (val task = tasks[id]!!) {
            is AssessmentTask -> task.outcome?.value
            is PlacementRequestTask -> task.outcome?.value
            is PlacementApplicationTask -> task.outcome?.value
            else -> fail("Unexpected task type ${task::class.qualifiedName}")
          }
        }

      private fun <T : Comparable<T>> sortTasks(sortDirection: SortDirection, sortFunc: (UUID) -> T?) =
        tasks
          .keys
          .apply {
            when (sortDirection) {
              SortDirection.asc -> sortedBy(sortFunc)
              SortDirection.desc -> sortedByDescending(sortFunc)
            }
          }
          .map { tasks[it]!! }
    }
  }

  @Nested
  inner class GetTaskTest : IntegrationTestBase() {
    @Autowired
    lateinit var taskTransformer: TaskTransformer

    @Autowired
    lateinit var userTransformer: UserTransformer

    @Test
    fun `Request without JWT returns 401`() {
      webTestClient.get()
        .uri("/tasks/assessment/f601ff2d-b1e0-4878-8731-ccfa19a2ce84")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Unknown task type for an application returns 404`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          givenAnAssessmentForApprovedPremises(
            allocatedToUser = user,
            createdByUser = user,
            crn = offenderDetails.otherIds.crn,
          ) { _, application ->
            webTestClient.get()
              .uri("/tasks/unknown-task/${application.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isNotFound
          }
        }
      }
    }

    @Test
    fun `Assessment Task UserWithWorkload only returns users with ASSESSOR role`() {
      val (creator, _) = givenAUser()
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER))
      val (assessor, _) = givenAUser(
        roles = listOf(UserRole.CAS1_ASSESSOR),
      )
      // inactive matcher
      givenAUser(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        isActive = false,
      )
      // matcher
      givenAUser(
        roles = listOf(UserRole.CAS1_MATCHER),
      )

      givenAnOffender { offenderDetails, _ ->
        givenAnAssessmentForApprovedPremises(
          allocatedToUser = null,
          createdByUser = creator,
          crn = offenderDetails.otherIds.crn,
          dueAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS),
        ) { assessment, _ ->

          webTestClient.get()
            .uri("/tasks/assessment/${assessment.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                TaskWrapper(
                  task = taskTransformer.transformAssessmentToTask(
                    assessment,
                    getOffenderSummaries(offenderDetails),
                  ),
                  users = listOf(
                    userTransformer.transformJpaToAPIUserWithWorkload(
                      assessor,
                      UserWorkload(
                        0,
                        0,
                        0,
                      ),
                    ),
                  ),
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `Placement Application Task returns 200`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
        givenAUser { user, _ ->
          givenAUser(
            roles = listOf(UserRole.CAS1_MATCHER),
          ) { allocatableUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              givenAPlacementApplication(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
              ) { placementApplication ->

                webTestClient.get()
                  .uri("/tasks/placement-application/${placementApplication.id}")
                  .header("Authorization", "Bearer $jwt")
                  .exchange()
                  .expectStatus()
                  .isOk
                  .expectBody()
                  .json(
                    objectMapper.writeValueAsString(
                      TaskWrapper(
                        task = taskTransformer.transformPlacementApplicationToTask(
                          placementApplication,
                          getOffenderSummaries(offenderDetails),
                        ),
                        users = listOf(
                          userTransformer.transformJpaToAPIUserWithWorkload(
                            allocatableUser,
                            UserWorkload(0, 0, 0),
                          ),
                        ),
                      ),
                    ),
                  )
              }
            }
          }
        }
      }
    }

    @Test
    fun `Placement Application Task UserWithWorkload only returns users with MATCHER or ASSESSOR roles`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->

        val (matcherUser1, _) = givenAUser(
          roles = listOf(UserRole.CAS1_MATCHER),
        )

        val (matcherUser2, _) = givenAUser(
          roles = listOf(UserRole.CAS1_MATCHER),
        )

        val (assessorUser, _) = givenAUser(
          roles = listOf(UserRole.CAS1_ASSESSOR),
        )

        val (assessorAndMatcherUser, _) = givenAUser(
          roles = listOf(UserRole.CAS1_ASSESSOR, UserRole.CAS1_MATCHER),
        )

        givenAnOffender { offenderDetails, _ ->
          givenAPlacementApplication(
            createdByUser = matcherUser1,
            allocatedToUser = matcherUser1,
            schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
            crn = offenderDetails.otherIds.crn,
          ) { placementApplication ->

            webTestClient.get()
              .uri("/tasks/placement-application/${placementApplication.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  TaskWrapper(
                    task = taskTransformer.transformPlacementApplicationToTask(
                      placementApplication,
                      getOffenderSummaries(offenderDetails),
                    ),
                    users = listOf(
                      userTransformer.transformJpaToAPIUserWithWorkload(
                        matcherUser1,
                        UserWorkload(1, 0, 0),
                      ),
                      userTransformer.transformJpaToAPIUserWithWorkload(
                        matcherUser2,
                        UserWorkload(0, 0, 0),
                      ),
                      userTransformer.transformJpaToAPIUserWithWorkload(
                        assessorUser,
                        UserWorkload(0, 0, 0),
                      ),
                      userTransformer.transformJpaToAPIUserWithWorkload(
                        assessorAndMatcherUser,
                        UserWorkload(0, 0, 0),
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
    fun `Assessment Task UserWithWorkload for an appealed application oly returns users with CAS1_APPEALS_MANAGER or CAS1_ASSESSOR role`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
        givenAUser(
          roles = listOf(UserRole.CAS1_REPORT_VIEWER),
        ) { user, _ ->
          givenAUser(
            roles = listOf(UserRole.CAS1_APPEALS_MANAGER),
          ) { appealsManager, _ ->
            givenAUser(
              roles = listOf(UserRole.CAS1_ASSESSOR),
            ) { assessor, _ ->
              givenAnOffender { offenderDetails, _ ->
                givenAnAssessmentForApprovedPremises(
                  allocatedToUser = user,
                  createdByUser = user,
                  crn = offenderDetails.otherIds.crn,
                  decision = AssessmentDecision.REJECTED,
                  createdFromAppeal = true,
                  dueAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                ) { assessment, _ ->
                  webTestClient.get()
                    .uri("/tasks/assessment/${assessment.id}")
                    .header("Authorization", "Bearer $jwt")
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody()
                    .json(
                      objectMapper.writeValueAsString(
                        TaskWrapper(
                          task = taskTransformer.transformAssessmentToTask(
                            assessment,
                            getOffenderSummaries(offenderDetails),
                          ),
                          users = listOf(
                            userTransformer.transformJpaToAPIUserWithWorkload(
                              appealsManager,
                              UserWorkload(
                                0,
                                0,
                                0,
                              ),
                            ),
                            userTransformer.transformJpaToAPIUserWithWorkload(
                              assessor,
                              UserWorkload(
                                0,
                                0,
                                0,
                              ),
                            ),
                          ),
                        ),
                      ),
                    )
                }
              }
            }
          }
        }
      }
    }

    @Test
    fun `Assessment Task UserWithWorkload for an appealed application returns 0 users if no users with CAS1_APPEALS_MANAGER or CAS1_ASSESSOR role`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
        givenAUser(
          roles = listOf(UserRole.CAS1_REPORT_VIEWER),
        ) { janitor, _ ->
          givenAUser(
            roles = listOf(UserRole.CAS1_MATCHER),
          ) { _, _ ->
            givenAnOffender { offenderDetails, _ ->
              givenAnAssessmentForApprovedPremises(
                allocatedToUser = null,
                createdByUser = janitor,
                crn = offenderDetails.otherIds.crn,
                decision = AssessmentDecision.REJECTED,
                createdFromAppeal = true,
                dueAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS),
              ) { assessment, _ ->
                webTestClient.get()
                  .uri("/tasks/assessment/${assessment.id}")
                  .header("Authorization", "Bearer $jwt")
                  .exchange()
                  .expectStatus()
                  .isOk
                  .expectBody()
                  .json(
                    objectMapper.writeValueAsString(
                      TaskWrapper(
                        task = taskTransformer.transformAssessmentToTask(
                          assessment,
                          getOffenderSummaries(offenderDetails),
                        ),
                        users = emptyList(),
                      ),
                    ),
                  )
              }
            }
          }
        }
      }
    }

    @Test
    fun `Assessment Task UserWithWorkload for an accepted application only returns users with ASSESSOR role`() {
      givenAUser(roles = listOf(UserRole.CAS1_APPEALS_MANAGER)) { _, jwt ->
        givenAUser(
          roles = listOf(UserRole.CAS1_REPORT_VIEWER),
        ) { user, _ ->
          givenAUser(
            roles = listOf(UserRole.CAS1_ASSESSOR),
          ) { allocatableUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              givenAnAssessmentForApprovedPremises(
                allocatedToUser = user,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
                decision = AssessmentDecision.ACCEPTED,
                dueAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS),
              ) { assessment, _ ->
                webTestClient.get()
                  .uri("/tasks/assessment/${assessment.id}")
                  .header("Authorization", "Bearer $jwt")
                  .exchange()
                  .expectStatus()
                  .isOk
                  .expectBody()
                  .json(
                    objectMapper.writeValueAsString(
                      TaskWrapper(
                        task = taskTransformer.transformAssessmentToTask(
                          assessment,
                          getOffenderSummaries(offenderDetails),
                        ),
                        users = listOf(
                          userTransformer.transformJpaToAPIUserWithWorkload(
                            allocatableUser,
                            UserWorkload(
                              0,
                              0,
                              0,
                            ),
                          ),
                        ),
                      ),
                    ),
                  )
              }
            }
          }
        }
      }
    }

    @Test
    fun `Placement Application Task UserWithWorkload ignoring inactive users`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
        givenAUser { user, _ ->
          givenAUser(
            roles = listOf(UserRole.CAS1_MATCHER),
          ) { allocatableUser, _ ->
            givenAUser(
              roles = listOf(UserRole.CAS1_MATCHER),
              isActive = false,
            ) { _, _ ->
              givenAnOffender { offenderDetails, _ ->
                givenAPlacementApplication(
                  createdByUser = user,
                  allocatedToUser = user,
                  schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                    withPermissiveSchema()
                  },
                  crn = offenderDetails.otherIds.crn,

                ) { placementApplication ->
                  val crn = offenderDetails.otherIds.crn
                  val numAssessmentsPending = 3
                  repeat(numAssessmentsPending) {
                    createAssessment(null, allocatableUser, user, crn)
                  }
                  createAssessment(null, allocatableUser, user, crn, isWithdrawn = true)

                  val numPlacementApplicationsPending = 4
                  repeat(numPlacementApplicationsPending) {
                    createPlacementApplication(null, allocatableUser, user, crn)
                  }
                  createPlacementApplication(null, allocatableUser, user, crn, decision = ACCEPTED)
                  createPlacementApplication(null, allocatableUser, user, crn, decision = REJECTED)
                  createPlacementApplication(null, allocatableUser, user, crn, decision = WITHDRAW)
                  createPlacementApplication(null, allocatableUser, user, crn, decision = WITHDRAWN_BY_PP)

                  val numPlacementRequestsPending = 2
                  repeat(numPlacementRequestsPending) {
                    createPlacementRequest(null, allocatableUser, user, crn)
                  }
                  createPlacementRequest(null, allocatableUser, user, crn, isWithdrawn = true)

                  val numAssessmentsCompletedBetween1And7DaysAgo = 4
                  repeat(numAssessmentsCompletedBetween1And7DaysAgo) {
                    val days = kotlin.random.Random.nextInt(1, 7).toLong()
                    createAssessment(OffsetDateTime.now().minusDays(days), allocatableUser, user, crn)
                  }

                  val numPlacementApplicationsCompletedBetween1And7DaysAgo = 2
                  repeat(numPlacementApplicationsCompletedBetween1And7DaysAgo) {
                    val days = kotlin.random.Random.nextInt(1, 7).toLong()
                    createPlacementApplication(OffsetDateTime.now().minusDays(days), allocatableUser, user, crn)
                  }

                  val numPlacementRequestsCompletedBetween1And7DaysAgo = 1
                  repeat(numPlacementRequestsCompletedBetween1And7DaysAgo) {
                    val days = kotlin.random.Random.nextInt(1, 7).toLong()
                    createPlacementRequest(OffsetDateTime.now().minusDays(days), allocatableUser, user, crn)
                  }

                  val numAssessmentsCompletedBetween8And30DaysAgo = 4
                  repeat(numAssessmentsCompletedBetween8And30DaysAgo) {
                    val days = kotlin.random.Random.nextInt(8, 30).toLong()
                    createAssessment(OffsetDateTime.now().minusDays(days), allocatableUser, user, crn)
                  }

                  val numPlacementApplicationsCompletedBetween8And30DaysAgo = 3
                  repeat(numPlacementApplicationsCompletedBetween8And30DaysAgo) {
                    val days = kotlin.random.Random.nextInt(8, 30).toLong()
                    createPlacementApplication(OffsetDateTime.now().minusDays(days), allocatableUser, user, crn)
                  }

                  val numPlacementRequestsCompletedBetween8And30DaysAgo = 2
                  repeat(numPlacementRequestsCompletedBetween8And30DaysAgo) {
                    val days = kotlin.random.Random.nextInt(8, 30).toLong()
                    createPlacementRequest(OffsetDateTime.now().minusDays(days), allocatableUser, user, crn)
                  }

                  val numPendingTasks = listOf(
                    numAssessmentsPending,
                    numPlacementRequestsPending,
                    numPlacementApplicationsPending,
                  ).sum()
                  val numTasksCompletedInTheLast7Days = listOf(
                    numAssessmentsCompletedBetween1And7DaysAgo,
                    numPlacementApplicationsCompletedBetween1And7DaysAgo,
                    numPlacementRequestsCompletedBetween1And7DaysAgo,
                  ).sum()
                  val numTasksCompletedInTheLast30Days = listOf(
                    numTasksCompletedInTheLast7Days,
                    numAssessmentsCompletedBetween8And30DaysAgo,
                    numPlacementApplicationsCompletedBetween8And30DaysAgo,
                    numPlacementRequestsCompletedBetween8And30DaysAgo,
                  ).sum()

                  webTestClient.get()
                    .uri("/tasks/placement-application/${placementApplication.id}")
                    .header("Authorization", "Bearer $jwt")
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody()
                    .json(
                      objectMapper.writeValueAsString(
                        TaskWrapper(
                          task = taskTransformer.transformPlacementApplicationToTask(
                            placementApplication,
                            getOffenderSummaries(offenderDetails),
                          ),
                          users = listOf(
                            userTransformer.transformJpaToAPIUserWithWorkload(
                              allocatableUser,
                              UserWorkload(
                                numPendingTasks,
                                numTasksCompletedInTheLast7Days,
                                numTasksCompletedInTheLast30Days,
                              ),
                            ),
                          ),
                        ),
                      ),
                    )
                }
              }
            }
          }
        }
      }
    }

    private fun createAssessment(
      completedAt: OffsetDateTime?,
      allocatedUser: UserEntity,
      createdByUser: UserEntity,
      crn: String,
      isWithdrawn: Boolean = false,
    ) {
      givenAnAssessmentForApprovedPremises(
        allocatedToUser = allocatedUser,
        createdByUser = createdByUser,
        crn = crn,
        decision = null,
        reallocated = false,
        submittedAt = completedAt,
        isWithdrawn = isWithdrawn,
      )
    }

    private fun createPlacementRequest(
      completedAt: OffsetDateTime?,
      allocatedUser: UserEntity,
      createdByUser: UserEntity,
      crn: String,
      isWithdrawn: Boolean = false,
    ) {
      val booking = if (completedAt != null) {
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(createdByUser.probationRegion)
        }
        bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withCreatedAt(completedAt)
        }
      } else {
        null
      }

      givenAPlacementRequest(
        placementRequestAllocatedTo = allocatedUser,
        assessmentAllocatedTo = createdByUser,
        createdByUser = createdByUser,
        crn = crn,
        booking = booking,
        isWithdrawn = isWithdrawn,
      )
    }

    private fun createPlacementApplication(
      completedAt: OffsetDateTime?,
      allocatedUser: UserEntity,
      createdByUser: UserEntity,
      crn: String,
      decision: PlacementApplicationDecision? = null,
    ) {
      givenAPlacementApplication(
        createdByUser = createdByUser,
        allocatedToUser = allocatedUser,
        schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        },
        submittedAt = completedAt,
        crn = crn,
        decision = decision,
      )
    }
  }

  @Nested
  inner class ReallocateTaskTest : IntegrationTestBase() {
    @Autowired
    lateinit var userTransformer: UserTransformer

    @BeforeEach
    fun stubBankHolidaysApi() {
      govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()
    }

    @Test
    fun `Reallocate application to different assessor without JWT returns 401`() {
      webTestClient.post()
        .uri("/tasks/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
        .bodyValue(
          NewReallocation(
            userId = UUID.randomUUID(),
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Reallocate application to different assessor without WORKFLOW_MANAGER role returns 403`() {
      givenAUser { _, jwt ->
        webTestClient.post()
          .uri("/tasks/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .bodyValue(
            NewReallocation(
              userId = UUID.randomUUID(),
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Reallocate assessment to different assessor returns 201, creates new assessment, deallocates old one, sends emails`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
        givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR)) { currentlyAllocatedUser, _ ->
          givenAUser(
            roles = listOf(UserRole.CAS1_ASSESSOR),
          ) { assigneeUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              givenAnAssessmentForApprovedPremises(
                allocatedToUser = currentlyAllocatedUser,
                createdByUser = currentlyAllocatedUser,
                crn = offenderDetails.otherIds.crn,
              ) { existingAssessment, application ->

                webTestClient.post()
                  .uri("/tasks/assessment/${existingAssessment.id}/allocations")
                  .header("Authorization", "Bearer $jwt")
                  .header("X-Service-Name", ServiceName.approvedPremises.value)
                  .bodyValue(
                    NewReallocation(
                      userId = assigneeUser.id,
                    ),
                  )
                  .exchange()
                  .expectStatus()
                  .isCreated
                  .expectBody()
                  .json(
                    objectMapper.writeValueAsString(
                      Reallocation(
                        user = userTransformer.transformJpaToApi(
                          assigneeUser,
                          ServiceName.approvedPremises,
                        ) as ApprovedPremisesUser,
                        taskType = TaskType.assessment,
                      ),
                    ),
                  )

                val assessments = approvedPremisesAssessmentRepository.findAll()

                assertThat(assessments.first { it.id == existingAssessment.id }.reallocatedAt).isNotNull
                assertThat(assessments)
                  .anyMatch { it.application.id == application.id && it.allocatedToUser!!.id == assigneeUser.id }

                emailAsserter.assertEmailsRequestedCount(2)
                emailAsserter.assertEmailRequested(currentlyAllocatedUser.email!!, notifyConfig.templates.assessmentDeallocated)
                emailAsserter.assertEmailRequested(assigneeUser.email!!, notifyConfig.templates.assessmentAllocated)
              }
            }
          }
        }
      }
    }

    @Test
    fun `Reallocate assessment to different assessor returns an error if the assessment has already been allocated`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
        givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR)) { user, _ ->
          givenAUser(
            roles = listOf(UserRole.CAS1_ASSESSOR),
          ) { assigneeUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              givenAnAssessmentForApprovedPremises(
                allocatedToUser = user,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
                reallocated = true,
              ) { existingAssessment, application ->

                webTestClient.post()
                  .uri("/tasks/assessment/${existingAssessment.id}/allocations")
                  .header("Authorization", "Bearer $jwt")
                  .header("X-Service-Name", ServiceName.approvedPremises.value)
                  .bodyValue(
                    NewReallocation(
                      userId = assigneeUser.id,
                    ),
                  )
                  .exchange()
                  .expectStatus()
                  .is4xxClientError()
                  .expectBody()
                  .jsonPath("title").isEqualTo("Conflict")
                  .jsonPath("status").isEqualTo(409)
                  .jsonPath("detail")
                  .isEqualTo("This assessment has already been reallocated: ${existingAssessment.id}")
              }
            }
          }
        }
      }
    }

    @Test
    fun `Reallocating a placement application to different assessor returns 201, creates new placement application, deallocates old one`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
        givenAUser { user, _ ->
          givenAUser(
            roles = listOf(UserRole.CAS1_MATCHER),
          ) { assigneeUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              givenAPlacementApplication(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              ) { placementApplication ->
                val placementDate = placementDateFactory.produceAndPersist {
                  withPlacementApplication(placementApplication)
                }

                webTestClient.post()
                  .uri("/tasks/placement-application/${placementApplication.id}/allocations")
                  .header("Authorization", "Bearer $jwt")
                  .header("X-Service-Name", ServiceName.approvedPremises.value)
                  .bodyValue(
                    NewReallocation(
                      userId = assigneeUser.id,
                    ),
                  )
                  .exchange()
                  .expectStatus()
                  .isCreated
                  .expectBody()
                  .json(
                    objectMapper.writeValueAsString(
                      Reallocation(
                        user = userTransformer.transformJpaToApi(
                          assigneeUser,
                          ServiceName.approvedPremises,
                        ) as ApprovedPremisesUser,
                        taskType = TaskType.placementApplication,
                      ),
                    ),
                  )

                val placementApplications = placementApplicationRepository.findAll()
                val allocatedPlacementApplication =
                  placementApplications.find { it.allocatedToUser!!.id == assigneeUser.id }

                assertThat(placementApplications.first { it.id == placementApplication.id }.reallocatedAt).isNotNull
                assertThat(allocatedPlacementApplication).isNotNull

                val placementDates = allocatedPlacementApplication!!.placementDates

                assertThat(placementDates.size).isEqualTo(1)
                assertThat(placementDates[0].expectedArrival).isEqualTo(placementDate.expectedArrival)
                assertThat(placementDates[0].duration).isEqualTo(placementDate.duration)
              }
            }
          }
        }
      }
    }

    @Test
    fun `Reallocating a Temporary Accommodation assessment does not require a request body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { originalUser, _ ->
        givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { expectedUser, jwt ->
          givenAnOffender { offenderDetails, _ ->
            givenAnAssessmentForTemporaryAccommodation(
              allocatedToUser = originalUser,
              createdByUser = originalUser,
              crn = offenderDetails.otherIds.crn,
            ) { assessment, _ ->
              webTestClient.post()
                .uri("/tasks/assessment/${assessment.id}/allocations")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
                .bodyValue(Unit)
                .exchange()
                .expectStatus()
                .isCreated

              val result = temporaryAccommodationAssessmentRepository.findAll().first { it.id == assessment.id }
              assertThat(result.allocatedToUser).isNotNull()
              assertThat(result.allocatedToUser!!.id).isEqualTo(expectedUser.id)
            }
          }
        }
      }
    }
  }

  @Nested
  inner class DeallocateTaskTest : IntegrationTestBase() {
    @Test
    fun `Deallocate assessment without JWT returns 401 Unauthorized`() {
      webTestClient.delete()
        .uri("/tasks/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Deallocate Temporary Accommodation assessment without CAS3_ASSESSOR role returns 403 Forbidden`() {
      givenAUser { _, jwt ->
        webTestClient.delete()
          .uri("/tasks/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Deallocate Approved Premises assessment returns 403 Forbidden`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          givenAUser { _, _ ->
            givenAnAssessmentForApprovedPremises(
              allocatedToUser = user,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            ) { assessment, _ ->
              webTestClient.delete()
                .uri("/tasks/assessment/${assessment.id}/allocations")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
                .exchange()
                .expectStatus()
                .isForbidden
            }
          }
        }
      }
    }

    @Test
    fun `Deallocate Temporary Accommodation assessment returns 200 and unassigns the allocated user`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          givenAnAssessmentForTemporaryAccommodation(
            allocatedToUser = user,
            createdByUser = user,
            crn = offenderDetails.otherIds.crn,
          ) { existingAssessment, _ ->

            webTestClient.delete()
              .uri("/tasks/assessment/${existingAssessment.id}/allocations")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
              .exchange()
              .expectStatus()
              .isNoContent

            val assessment =
              temporaryAccommodationAssessmentRepository.findAll().first { it.id == existingAssessment.id }

            assertThat(assessment.allocatedToUser).isNull()
            assertThat(assessment.allocatedAt).isNull()
          }
        }
      }
    }
  }

  fun getOffenderSummaries(offenderDetails: OffenderDetailSummary): List<PersonSummaryInfoResult> {
    return listOf(
      PersonSummaryInfoResult.Success.Full(
        offenderDetails.otherIds.crn,
        CaseSummaryFactory().withName(
          NameFactory()
            .withForename(offenderDetails.firstName)
            .withSurname(offenderDetails.surname)
            .produce(),
        )
          .produce(),
      ),
    )
  }
}
