package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.TasksApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AllocatedFilter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskWrapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.convert.EnumConverterFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TaskEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.TypedTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.TaskService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.TaskTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.kebabCaseToPascalCase
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification as ApiUserQualification

@Service
class TasksController(
  private val userService: UserService,
  private val assessmentService: AssessmentService,
  private val taskTransformer: TaskTransformer,
  private val offenderService: OffenderService,
  private val placementApplicationService: PlacementApplicationService,
  private val enumConverterFactory: EnumConverterFactory,
  private val userTransformer: UserTransformer,
  private val taskService: TaskService,
) : TasksApiDelegate {

  override fun tasksGet(
    type: TaskType?,
    types: List<TaskType>?,
    page: Int?,
    perPage: Int?,
    sortBy: TaskSortField?,
    sortDirection: SortDirection?,
    allocatedFilter: AllocatedFilter?,
    apAreaId: UUID?,
    cruManagementAreaId: UUID?,
    allocatedToUserId: UUID?,
    requiredQualification: ApiUserQualification?,
    crnOrName: String?,
    isCompleted: Boolean?,
  ): ResponseEntity<List<Task>> {
    val user = userService.getUserForRequest()

    if (!user.hasAnyRole(UserRole.CAS1_WORKFLOW_MANAGER, UserRole.CAS1_MATCHER, UserRole.CAS1_ASSESSOR)) {
      throw ForbiddenProblem()
    }

    val taskEntityTypes = if (types != null) {
      types.map { toTaskEntityType(it) }
    } else if (type != null) {
      listOf(toTaskEntityType(type))
    } else {
      TaskEntityType.entries
    }

    val (typedTasks, metadata) = taskService.getAll(
      TaskService.TaskFilterCriteria(
        allocatedFilter = allocatedFilter,
        apAreaId = apAreaId,
        cruManagementAreaId = cruManagementAreaId,
        types = taskEntityTypes,
        allocatedToUserId = allocatedToUserId,
        requiredQualification = requiredQualification,
        crnOrName = crnOrName,
        isCompleted = isCompleted ?: false,
      ),
      PageCriteria(
        sortBy = sortBy ?: TaskSortField.createdAt,
        perPage = perPage,
        sortDirection = sortDirection ?: SortDirection.asc,
        page = page,
      ),
    )

    val offenderSummaries = getOffenderSummariesForCrns(typedTasks.map { it.crn }, user)
    val tasks = typedTasks.map {
      when (it) {
        is TypedTask.Assessment -> getAssessmentTask(it.entity, offenderSummaries)
        is TypedTask.PlacementApplication -> getPlacementApplicationTask(it.entity, offenderSummaries)
      }
    }

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(
      tasks,
    )
  }

  private fun toTaskEntityType(taskType: TaskType) = when (taskType) {
    TaskType.assessment -> TaskEntityType.ASSESSMENT
    TaskType.placementApplication -> TaskEntityType.PLACEMENT_APPLICATION
  }

  override fun tasksTaskTypeIdGet(id: UUID, taskType: String): ResponseEntity<TaskWrapper> {
    val user = userService.getUserForRequest()

    val taskInfo = when (toTaskType(taskType)) {
      TaskType.assessment -> {
        val assessment = extractEntityFromCasResult(
          assessmentService.getAssessmentAndValidate(user, id),
        ) as ApprovedPremisesAssessmentEntity
        val offenderSummaries = getOffenderSummariesForCrns(listOf(assessment.application.crn), user)

        val requiredPermission = if (assessment.createdFromAppeal) {
          UserPermission.CAS1_ASSESS_APPEALED_APPLICATION
        } else {
          UserPermission.CAS1_ASSESS_APPLICATION
        }

        TaskInfo(
          transformedTask = getAssessmentTask(assessment, offenderSummaries),
          crn = assessment.application.crn,
          requiredQualifications = assessment.application.getRequiredQualifications(),
          requiredPermission = requiredPermission,
        )
      }

      TaskType.placementApplication -> {
        val placementApplication = extractEntityFromCasResult(
          placementApplicationService.getApplication(id),
        )
        val offenderSummaries = getOffenderSummariesForCrns(listOf(placementApplication.application.crn), user)

        TaskInfo(
          transformedTask = getPlacementApplicationTask(placementApplication, offenderSummaries),
          crn = placementApplication.application.crn,
          requiredQualifications = placementApplication.application.getRequiredQualifications(),
          requiredPermission = UserPermission.CAS1_ASSESS_PLACEMENT_APPLICATION,
        )
      }
    }

    val users = userService.getAllocatableUsersForAllocationType(
      taskInfo.crn,
      taskInfo.requiredQualifications,
      taskInfo.requiredPermission,
    )

    val workload = taskService.getUserWorkloads(users.map { it.id })
    val transformedAllocatableUsers = users.map {
      userTransformer.transformJpaToAPIUserWithWorkload(it, workload[it.id]!!)
    }

    return ResponseEntity.ok(
      TaskWrapper(
        task = taskInfo.transformedTask,
        users = transformedAllocatableUsers,
      ),
    )
  }

  private data class TaskInfo(
    val transformedTask: Task,
    val crn: String,
    val requiredQualifications: List<UserQualification>,
    val requiredPermission: UserPermission,
  )

  @Transactional
  override fun tasksTaskTypeIdAllocationsPost(
    id: UUID,
    taskType: String,
    xServiceName: ServiceName,
    body: NewReallocation?,
  ): ResponseEntity<Reallocation> {
    val user = userService.getUserForRequest()

    val type = toTaskType(taskType)

    val userId = when {
      xServiceName == ServiceName.temporaryAccommodation -> user.id
      else -> {
        body?.userId ?: throw BadRequestProblem(invalidParams = ValidationErrors(mutableMapOf("$.userId" to "empty")))
      }
    }

    val reallocatedTask = extractEntityFromCasResult(
      taskService.reallocateTask(
        requestUser = user,
        taskType = type,
        userToAllocateToId = userId,
        taskId = id,
      ),
    )

    return ResponseEntity(reallocatedTask, HttpStatus.CREATED)
  }

  @Transactional
  override fun tasksTaskTypeIdAllocationsDelete(id: UUID, taskType: String): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    val type = toTaskType(taskType)

    ensureEntityFromCasResultIsSuccess(
      taskService.deallocateTask(user, type, id),
    )

    return ResponseEntity(Unit, HttpStatus.NO_CONTENT)
  }

  private fun getAssessmentTask(
    assessment: AssessmentEntity,
    offenderSummaries: List<PersonSummaryInfoResult>,
  ): AssessmentTask {
    return taskTransformer.transformAssessmentToTask(
      assessment = assessment,
      offenderSummaries = offenderSummaries,
    )
  }

  private fun getPlacementApplicationTask(
    placementApplication: PlacementApplicationEntity,
    offenderSummaries: List<PersonSummaryInfoResult>,
  ): PlacementApplicationTask {
    return taskTransformer.transformPlacementApplicationToTask(
      placementApplication = placementApplication,
      offenderSummaries = offenderSummaries,
    )
  }

  private fun getOffenderSummariesForCrns(crns: List<String>, user: UserEntity): List<PersonSummaryInfoResult> {
    return offenderService.getOffenderSummariesByCrns(
      crns.toSet(),
      user.deliusUsername,
      user.hasQualification(UserQualification.LAO),
    )
  }

  private fun toTaskType(type: String) = enumConverterFactory.getConverter(TaskType::class.java).convert(
    type.kebabCaseToPascalCase(),
  ) ?: throw NotFoundProblem(type, "TaskType")
}
