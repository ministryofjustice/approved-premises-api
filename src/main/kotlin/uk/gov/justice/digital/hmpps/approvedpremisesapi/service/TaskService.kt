package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AllocatedFilter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Task
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TaskEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TaskRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.TypedTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotAllowedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import java.util.UUID

@Service
class TaskService(
  private val assessmentService: AssessmentService,
  private val userService: UserService,
  private val userAccessService: UserAccessService,
  private val placementRequestService: PlacementRequestService,
  private val userTransformer: UserTransformer,
  private val placementApplicationService: PlacementApplicationService,
  private val taskRepository: TaskRepository,
  private val assessmentRepository: AssessmentRepository,
  private val placementApplicationRepository: PlacementApplicationRepository,
  private val placementRequestRepository: PlacementRequestRepository,
) {

  data class TaskFilterCriteria(
    val allocatedFilter: AllocatedFilter?,
    val apAreaId: UUID?,
    val cruManagementAreaId: UUID?,
    val types: List<TaskEntityType>,
    val allocatedToUserId: UUID?,
    val requiredQualification: UserQualification?,
    val crnOrName: String?,
    val isCompleted: Boolean,
  )

  fun getAll(
    filterCriteria: TaskFilterCriteria,
    pageCriteria: PageCriteria<TaskSortField>,
  ): Pair<List<TypedTask>, PaginationMetadata?> {
    val taskTypes = filterCriteria.types

    val tasksResult = getAllEntities(filterCriteria, pageCriteria, taskTypes)
    val tasks = tasksResult.content

    val assessments = if (taskTypes.contains(TaskEntityType.ASSESSMENT)) {
      val assessmentIds = tasks.idsForType(TaskEntityType.ASSESSMENT)
      assessmentRepository.findAllById(assessmentIds).map { TypedTask.Assessment(it as ApprovedPremisesAssessmentEntity) }
    } else {
      emptyList()
    }

    val placementApplications = if (taskTypes.contains(TaskEntityType.PLACEMENT_APPLICATION)) {
      val placementApplicationIds = tasks.idsForType(TaskEntityType.PLACEMENT_APPLICATION)
      placementApplicationRepository.findAllById(placementApplicationIds).map { TypedTask.PlacementApplication(it) }
    } else {
      emptyList()
    }

    val placementRequests = if (taskTypes.contains(TaskEntityType.PLACEMENT_REQUEST)) {
      val placementRequestIds = tasks.idsForType(TaskEntityType.PLACEMENT_REQUEST)
      placementRequestRepository.findAllById(placementRequestIds).map { TypedTask.PlacementRequest(it) }
    } else {
      emptyList()
    }

    val typedTasks = tasks
      .map { task ->
        val candidateList = when (task.type) {
          TaskEntityType.ASSESSMENT -> assessments
          TaskEntityType.PLACEMENT_APPLICATION -> placementApplications
          TaskEntityType.PLACEMENT_REQUEST -> placementRequests
        }

        candidateList.first { it.id == task.id }
      }

    val metadata = getMetadata(tasksResult, pageCriteria)
    return Pair(typedTasks, metadata)
  }

  private fun getAllEntities(
    filterCriteria: TaskFilterCriteria,
    pageCriteria: PageCriteria<TaskSortField>,
    taskTypes: List<TaskEntityType>,
  ): Page<Task> {
    val pageable = pageCriteria.toPageable(
      when (pageCriteria.sortBy) {
        TaskSortField.createdAt -> "created_at"
        TaskSortField.dueAt -> "due_at"
        TaskSortField.allocatedTo -> "allocated_to"
        TaskSortField.person -> "person"
        TaskSortField.completedAt -> "completed_at"
        TaskSortField.taskType -> "type"
        TaskSortField.decision -> "decision"
      },
    )

    val allocatedFilter = filterCriteria.allocatedFilter

    val isAllocated = allocatedFilter?.let { allocatedFilter == AllocatedFilter.ALLOCATED }

    val repoFunction = if (taskTypes.size == 1) {
      when (taskTypes[0]) {
        TaskEntityType.ASSESSMENT -> taskRepository::getAllAssessments
        TaskEntityType.PLACEMENT_APPLICATION -> taskRepository::getAllPlacementApplications
        TaskEntityType.PLACEMENT_REQUEST -> taskRepository::getAllPlacementRequests
      }
    } else {
      taskRepository::getAll
    }

    return repoFunction(
      isAllocated,
      filterCriteria.apAreaId,
      filterCriteria.cruManagementAreaId,
      taskTypes.map { it.name },
      filterCriteria.allocatedToUserId,
      filterCriteria.requiredQualification?.value,
      filterCriteria.crnOrName,
      filterCriteria.isCompleted,
      pageable,
    )
  }

  private fun List<Task>.idsForType(type: TaskEntityType) = this.filter { it.type == type }.map { it.id }

  @SuppressWarnings("ReturnCount", "CyclomaticComplexMethod")
  @Transactional
  fun reallocateTask(requestUser: UserEntity, taskType: TaskType, userToAllocateToId: UUID, taskId: UUID): AuthorisableActionResult<ValidatableActionResult<Reallocation>> {
    if (!userAccessService.userCanReallocateTask(requestUser)) {
      return AuthorisableActionResult.Unauthorised()
    }

    val assigneeUserResult = userService.updateUserFromDelius(userToAllocateToId, ServiceName.approvedPremises)

    val assigneeUser =
      if (assigneeUserResult is CasResult.Success &&
        assigneeUserResult.value is UserService.GetUserResponse.Success
      ) {
        assigneeUserResult.value.user
      } else {
        return AuthorisableActionResult.NotFound()
      }

    val result = when (taskType) {
      TaskType.assessment -> {
        assessmentService.reallocateAssessment(assigneeUser, taskId)
      }
      TaskType.placementRequest -> {
        placementRequestService.reallocatePlacementRequest(assigneeUser, taskId)
      }
      TaskType.placementApplication -> {
        placementApplicationService.reallocateApplication(assigneeUser, taskId)
      }
      else -> {
        throw NotAllowedProblem(detail = "The Task Type $taskType is not currently supported")
      }
    }

    val validationResult = when (result) {
      is AuthorisableActionResult.NotFound -> return AuthorisableActionResult.NotFound()
      is AuthorisableActionResult.Unauthorised -> return AuthorisableActionResult.Unauthorised()
      is AuthorisableActionResult.Success -> result.entity
    }

    return when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> AuthorisableActionResult.Success(ValidatableActionResult.GeneralValidationError(validationResult.message))
      is ValidatableActionResult.FieldValidationError -> AuthorisableActionResult.Success(ValidatableActionResult.FieldValidationError(validationResult.validationMessages))
      is ValidatableActionResult.ConflictError -> AuthorisableActionResult.Success(ValidatableActionResult.ConflictError(validationResult.conflictingEntityId, validationResult.message))
      is ValidatableActionResult.Success -> AuthorisableActionResult.Success(
        ValidatableActionResult.Success(
          entityToReallocation(validationResult.entity, taskType),
        ),
      )
    }
  }

  fun deallocateTask(
    requestUser: UserEntity,
    taskType: TaskType,
    id: UUID,
  ): AuthorisableActionResult<ValidatableActionResult<Unit>> {
    if (!userAccessService.userCanDeallocateTask(requestUser)) {
      return AuthorisableActionResult.Unauthorised()
    }

    val result = when (taskType) {
      TaskType.assessment -> assessmentService.deallocateAssessment(id)
      else -> throw NotAllowedProblem(detail = "The Task Type $taskType is not currently supported")
    }

    val validationResult = when (result) {
      is AuthorisableActionResult.NotFound -> return AuthorisableActionResult.NotFound()
      is AuthorisableActionResult.Unauthorised -> return AuthorisableActionResult.Unauthorised()
      is AuthorisableActionResult.Success -> result.entity
    }

    return when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> AuthorisableActionResult.Success(ValidatableActionResult.GeneralValidationError(validationResult.message))
      is ValidatableActionResult.FieldValidationError -> AuthorisableActionResult.Success(ValidatableActionResult.FieldValidationError(validationResult.validationMessages))
      is ValidatableActionResult.ConflictError -> AuthorisableActionResult.Success(ValidatableActionResult.ConflictError(validationResult.conflictingEntityId, validationResult.message))
      is ValidatableActionResult.Success -> AuthorisableActionResult.Success(
        ValidatableActionResult.Success(
          Unit,
        ),
      )
    }
  }

  private fun entityToReallocation(entity: Any, taskType: TaskType): Reallocation {
    val allocatedToUser = when (entity) {
      is PlacementRequestEntity -> entity.allocatedToUser
      is AssessmentEntity -> entity.allocatedToUser
      is PlacementApplicationEntity -> entity.allocatedToUser!!
      else -> throw RuntimeException("Unexpected type")
    }

    return Reallocation(
      taskType = taskType,
      user = userTransformer.transformJpaToApi(allocatedToUser!!, ServiceName.approvedPremises) as ApprovedPremisesUser,
    )
  }
}
