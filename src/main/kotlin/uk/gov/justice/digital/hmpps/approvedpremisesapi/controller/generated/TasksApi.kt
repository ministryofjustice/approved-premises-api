/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.11.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generated

import io.swagger.v3.oas.annotations.*
import io.swagger.v3.oas.annotations.enums.*
import io.swagger.v3.oas.annotations.media.*
import io.swagger.v3.oas.annotations.responses.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AllocatedFilter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskWrapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import kotlin.collections.List

@RestController
interface TasksApi {

  fun getDelegate(): TasksApiDelegate = object : TasksApiDelegate {}

  @Operation(
    tags = ["Task data"],
    summary = "List all tasks",
    operationId = "tasksGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "successfully retrieved tasks",
        content = [Content(array = ArraySchema(schema = Schema(implementation = Task::class)))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/tasks"],
    produces = ["application/json"],
  )
  fun tasksGet(
    @Parameter(
      description = "Returns tasks that match the given type. BookingAppeal is not supported. Deprecated - use 'types'",
      schema = Schema(allowableValues = ["Assessment", "PlacementApplication"]),
    ) @RequestParam(value = "type", required = false) type: TaskType?,
    @Parameter(description = "Returns tasks that match the given types. BookingAppeal is not supported") @RequestParam(
      value = "types",
      required = false,
    ) types: kotlin.collections.List<TaskType>?,
    @Parameter(description = "Page number of results to return. If not provided results will not be paged") @RequestParam(
      value = "page",
      required = false,
    ) page: kotlin.Int?,
    @Parameter(description = "Number of items to return per page (defaults to 10)") @RequestParam(
      value = "perPage",
      required = false,
    ) perPage: kotlin.Int?,
    @Parameter(
      description = "Which field to sort the results by. If not provided will sort by createdAt",
      schema = Schema(allowableValues = ["createdAt", "dueAt", "person", "allocatedTo", "completedAt", "taskType", "decision"]),
    ) @RequestParam(value = "sortBy", required = false) sortBy: TaskSortField?,
    @Parameter(
      description = "The direction to sort the results by. If not provided will sort by descending order",
      schema = Schema(allowableValues = ["asc", "desc"]),
    ) @RequestParam(value = "sortDirection", required = false) sortDirection: SortDirection?,
    @Parameter(
      description = "Allows filtering on allocated, unallocated, or both",
      schema = Schema(allowableValues = ["allocated", "unallocated"]),
    ) @RequestParam(value = "allocatedFilter", required = false) allocatedFilter: AllocatedFilter?,
    @Parameter(description = "Approved Premises Area ID to filter results by.  Deprecated, Use cruManagementAreaId instead.") @RequestParam(
      value = "apAreaId",
      required = false,
    ) apAreaId: java.util.UUID?,
    @Parameter(description = "filter by CRU management area ID") @RequestParam(
      value = "cruManagementAreaId",
      required = false,
    ) cruManagementAreaId: java.util.UUID?,
    @Parameter(description = "Only show tasks allocated to this user id") @RequestParam(
      value = "allocatedToUserId",
      required = false,
    ) allocatedToUserId: java.util.UUID?,
    @Parameter(
      description = "Only show tasks with this required qualification",
      schema = Schema(allowableValues = ["pipe", "lao", "emergency", "esap", "recovery_focused", "mental_health_specialist"]),
    ) @RequestParam(value = "requiredQualification", required = false) requiredQualification: UserQualification?,
    @Parameter(description = "search by CRN or name") @RequestParam(
      value = "crnOrName",
      required = false,
    ) crnOrName: kotlin.String?,
    @Parameter(description = "filter by if the tasks are completed (defaults to `false`)") @RequestParam(
      value = "isCompleted",
      required = false,
    ) isCompleted: kotlin.Boolean?,
  ): ResponseEntity<List<Task>> {
    return getDelegate().tasksGet(
      type,
      types,
      page,
      perPage,
      sortBy,
      sortDirection,
      allocatedFilter,
      apAreaId,
      cruManagementAreaId,
      allocatedToUserId,
      requiredQualification,
      crnOrName,
      isCompleted,
    )
  }

  @Operation(
    tags = ["Operations on applications"],
    summary = "Unallocates a task for an application",
    operationId = "tasksTaskTypeIdAllocationsDelete",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation"),
      ApiResponse(
        responseCode = "400",
        description = "invalid params",
        content = [Content(schema = Schema(implementation = ValidationError::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.DELETE],
    value = ["/tasks/{taskType}/{id}/allocations"],
    produces = ["application/problem+json", "application/json"],
  )
  fun tasksTaskTypeIdAllocationsDelete(
    @Parameter(
      description = "ID of the task",
      required = true,
    ) @PathVariable("id") id: java.util.UUID,
    @Parameter(
      description = "Task type",
      required = true,
    ) @PathVariable("taskType") taskType: kotlin.String,
  ): ResponseEntity<Unit> {
    return getDelegate().tasksTaskTypeIdAllocationsDelete(id, taskType)
  }

  @Operation(
    tags = ["Operations on applications"],
    summary = "Reallocates a task for an application",
    operationId = "tasksTaskTypeIdAllocationsPost",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "successful operation",
        content = [Content(schema = Schema(implementation = Reallocation::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "invalid params",
        content = [Content(schema = Schema(implementation = ValidationError::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/tasks/{taskType}/{id}/allocations"],
    produces = ["application/json", "application/problem+json"],
    consumes = ["application/json"],
  )
  fun tasksTaskTypeIdAllocationsPost(
    @Parameter(
      description = "ID of the task",
      required = true,
    ) @PathVariable("id") id: java.util.UUID,
    @Parameter(
      description = "Task type",
      required = true,
    ) @PathVariable("taskType") taskType: kotlin.String,
    @Parameter(
      description = "Only assessments for this service will be returned",
      `in` = ParameterIn.HEADER,
      required = true,
      schema = Schema(allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"]),
    ) @RequestHeader(
      value = "X-Service-Name",
      required = true,
    ) xServiceName: ServiceName,
    @Parameter(description = "") @RequestBody(required = false) body: NewReallocation?,
  ): ResponseEntity<Reallocation> {
    return getDelegate().tasksTaskTypeIdAllocationsPost(id, taskType, xServiceName, body)
  }

  @Operation(
    tags = ["Application data"],
    summary = "Gets a task for an application",
    operationId = "tasksTaskTypeIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "successfully retrieved task",
        content = [Content(schema = Schema(implementation = TaskWrapper::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/tasks/{taskType}/{id}"],
    produces = ["application/json"],
  )
  fun tasksTaskTypeIdGet(
    @Parameter(
      description = "ID of the task",
      required = true,
    ) @PathVariable("id") id: java.util.UUID,
    @Parameter(
      description = "Task type",
      required = true,
    ) @PathVariable("taskType") taskType: kotlin.String,
  ): ResponseEntity<TaskWrapper> {
    return getDelegate().tasksTaskTypeIdGet(id, taskType)
  }
}
