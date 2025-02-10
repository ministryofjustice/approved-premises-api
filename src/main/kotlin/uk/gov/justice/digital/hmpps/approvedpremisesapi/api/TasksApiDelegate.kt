package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AllocatedFilter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskWrapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link TasksApiController}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
interface TasksApiDelegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see TasksApi#tasksGet
     */
    fun tasksGet(type: TaskType?,
        types: kotlin.collections.List<TaskType>?,
        page: kotlin.Int?,
        perPage: kotlin.Int?,
        sortBy: TaskSortField?,
        sortDirection: SortDirection?,
        allocatedFilter: AllocatedFilter?,
        apAreaId: java.util.UUID?,
        cruManagementAreaId: java.util.UUID?,
        allocatedToUserId: java.util.UUID?,
        requiredQualification: UserQualification?,
        crnOrName: kotlin.String?,
        isCompleted: kotlin.Boolean?): ResponseEntity<List<Task>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"outcomeRecordedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"dueDate\" : \"2000-01-23\",  \"probationDeliveryUnit\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"personName\" : \"personName\",  \"dueAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"allocatedToStaffMember\" : {    \"telephoneNumber\" : \"telephoneNumber\",    \"roles\" : [ \"assessor\", \"assessor\" ],    \"probationDeliveryUnit\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"cruManagementArea\" : \"\",    \"cruManagementAreaDefault\" : \"\",    \"isActive\" : true,    \"version\" : 0,    \"cruManagementAreaOverride\" : \"\",    \"qualifications\" : [ \"pipe\", \"pipe\" ],    \"apArea\" : {      \"identifier\" : \"LON\",      \"name\" : \"Yorkshire & The Humber\",      \"id\" : \"cd1c2d43-0b0b-4438-b0e3-d4424e61fb6a\"    },    \"service\" : \"service\",    \"permissions\" : [ \"cas1_adhoc_booking_create\", \"cas1_adhoc_booking_create\" ],    \"deliusUsername\" : \"deliusUsername\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"region\" : {      \"name\" : \"NPS North East Central Referrals\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"    },    \"email\" : \"email\"  },  \"taskType\" : \"Assessment\",  \"apArea\" : {    \"identifier\" : \"LON\",    \"name\" : \"Yorkshire & The Humber\",    \"id\" : \"cd1c2d43-0b0b-4438-b0e3-d4424e61fb6a\"  },  \"personSummary\" : {    \"personType\" : \"FullPersonSummary\",    \"crn\" : \"crn\"  },  \"id\" : \"6abb5fa3-e93f-4445-887b-30d081688f44\",  \"applicationId\" : \"6abb5fa3-e93f-4445-887b-30d081688f44\",  \"crn\" : \"crn\",  \"status\" : \"not_started\"}, {  \"outcomeRecordedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"dueDate\" : \"2000-01-23\",  \"probationDeliveryUnit\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"personName\" : \"personName\",  \"dueAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"allocatedToStaffMember\" : {    \"telephoneNumber\" : \"telephoneNumber\",    \"roles\" : [ \"assessor\", \"assessor\" ],    \"probationDeliveryUnit\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"cruManagementArea\" : \"\",    \"cruManagementAreaDefault\" : \"\",    \"isActive\" : true,    \"version\" : 0,    \"cruManagementAreaOverride\" : \"\",    \"qualifications\" : [ \"pipe\", \"pipe\" ],    \"apArea\" : {      \"identifier\" : \"LON\",      \"name\" : \"Yorkshire & The Humber\",      \"id\" : \"cd1c2d43-0b0b-4438-b0e3-d4424e61fb6a\"    },    \"service\" : \"service\",    \"permissions\" : [ \"cas1_adhoc_booking_create\", \"cas1_adhoc_booking_create\" ],    \"deliusUsername\" : \"deliusUsername\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"region\" : {      \"name\" : \"NPS North East Central Referrals\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"    },    \"email\" : \"email\"  },  \"taskType\" : \"Assessment\",  \"apArea\" : {    \"identifier\" : \"LON\",    \"name\" : \"Yorkshire & The Humber\",    \"id\" : \"cd1c2d43-0b0b-4438-b0e3-d4424e61fb6a\"  },  \"personSummary\" : {    \"personType\" : \"FullPersonSummary\",    \"crn\" : \"crn\"  },  \"id\" : \"6abb5fa3-e93f-4445-887b-30d081688f44\",  \"applicationId\" : \"6abb5fa3-e93f-4445-887b-30d081688f44\",  \"crn\" : \"crn\",  \"status\" : \"not_started\"} ]")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see TasksApi#tasksTaskTypeIdAllocationsDelete
     */
    fun tasksTaskTypeIdAllocationsDelete(id: java.util.UUID,
        taskType: kotlin.String): ResponseEntity<Unit> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/problem+json"))) {
                    ApiUtil.setExampleResponse(request, "application/problem+json", "Custom MIME type example not yet supported: application/problem+json")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see TasksApi#tasksTaskTypeIdAllocationsPost
     */
    fun tasksTaskTypeIdAllocationsPost(id: java.util.UUID,
        taskType: kotlin.String,
        xServiceName: ServiceName,
        body: NewReallocation?): ResponseEntity<Reallocation> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"taskType\" : \"Assessment\",  \"user\" : {    \"telephoneNumber\" : \"telephoneNumber\",    \"roles\" : [ \"assessor\", \"assessor\" ],    \"probationDeliveryUnit\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"cruManagementArea\" : \"\",    \"cruManagementAreaDefault\" : \"\",    \"isActive\" : true,    \"version\" : 0,    \"cruManagementAreaOverride\" : \"\",    \"qualifications\" : [ \"pipe\", \"pipe\" ],    \"apArea\" : {      \"identifier\" : \"LON\",      \"name\" : \"Yorkshire & The Humber\",      \"id\" : \"cd1c2d43-0b0b-4438-b0e3-d4424e61fb6a\"    },    \"service\" : \"service\",    \"permissions\" : [ \"cas1_adhoc_booking_create\", \"cas1_adhoc_booking_create\" ],    \"deliusUsername\" : \"deliusUsername\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"region\" : {      \"name\" : \"NPS North East Central Referrals\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"    },    \"email\" : \"email\"  }}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/problem+json"))) {
                    ApiUtil.setExampleResponse(request, "application/problem+json", "Custom MIME type example not yet supported: application/problem+json")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see TasksApi#tasksTaskTypeIdGet
     */
    fun tasksTaskTypeIdGet(id: java.util.UUID,
        taskType: kotlin.String): ResponseEntity<TaskWrapper> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"task\" : {    \"outcomeRecordedAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"dueDate\" : \"2000-01-23\",    \"probationDeliveryUnit\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"personName\" : \"personName\",    \"dueAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"allocatedToStaffMember\" : {      \"telephoneNumber\" : \"telephoneNumber\",      \"roles\" : [ \"assessor\", \"assessor\" ],      \"probationDeliveryUnit\" : {        \"name\" : \"name\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"      },      \"cruManagementArea\" : \"\",      \"cruManagementAreaDefault\" : \"\",      \"isActive\" : true,      \"version\" : 0,      \"cruManagementAreaOverride\" : \"\",      \"qualifications\" : [ \"pipe\", \"pipe\" ],      \"apArea\" : {        \"identifier\" : \"LON\",        \"name\" : \"Yorkshire & The Humber\",        \"id\" : \"cd1c2d43-0b0b-4438-b0e3-d4424e61fb6a\"      },      \"service\" : \"service\",      \"permissions\" : [ \"cas1_adhoc_booking_create\", \"cas1_adhoc_booking_create\" ],      \"deliusUsername\" : \"deliusUsername\",      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"region\" : {        \"name\" : \"NPS North East Central Referrals\",        \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"      },      \"email\" : \"email\"    },    \"taskType\" : \"Assessment\",    \"apArea\" : {      \"identifier\" : \"LON\",      \"name\" : \"Yorkshire & The Humber\",      \"id\" : \"cd1c2d43-0b0b-4438-b0e3-d4424e61fb6a\"    },    \"personSummary\" : {      \"personType\" : \"FullPersonSummary\",      \"crn\" : \"crn\"    },    \"id\" : \"6abb5fa3-e93f-4445-887b-30d081688f44\",    \"applicationId\" : \"6abb5fa3-e93f-4445-887b-30d081688f44\",    \"crn\" : \"crn\",    \"status\" : \"not_started\"  },  \"users\" : [ {    \"telephoneNumber\" : \"telephoneNumber\",    \"numTasksPending\" : 0,    \"roles\" : [ \"assessor\", \"assessor\" ],    \"probationDeliveryUnit\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"cruManagementArea\" : \"\",    \"isActive\" : true,    \"qualifications\" : [ \"pipe\", \"pipe\" ],    \"apArea\" : \"\",    \"numTasksCompleted7Days\" : 6,    \"service\" : \"service\",    \"deliusUsername\" : \"deliusUsername\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"region\" : {      \"name\" : \"NPS North East Central Referrals\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"    },    \"email\" : \"email\",    \"numTasksCompleted30Days\" : 1  }, {    \"telephoneNumber\" : \"telephoneNumber\",    \"numTasksPending\" : 0,    \"roles\" : [ \"assessor\", \"assessor\" ],    \"probationDeliveryUnit\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"cruManagementArea\" : \"\",    \"isActive\" : true,    \"qualifications\" : [ \"pipe\", \"pipe\" ],    \"apArea\" : \"\",    \"numTasksCompleted7Days\" : 6,    \"service\" : \"service\",    \"deliusUsername\" : \"deliusUsername\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"region\" : {      \"name\" : \"NPS North East Central Referrals\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"    },    \"email\" : \"email\",    \"numTasksCompleted30Days\" : 1  } ]}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }

}
