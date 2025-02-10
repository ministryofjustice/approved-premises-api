package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecisionEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatePlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementApplication
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link PlacementApplicationsApiController}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
interface PlacementApplicationsApiDelegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see PlacementApplicationsApi#placementApplicationsIdDecisionPost
     */
    fun placementApplicationsIdDecisionPost(id: java.util.UUID,
        placementApplicationDecisionEnvelope: PlacementApplicationDecisionEnvelope): ResponseEntity<PlacementApplication> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"schemaVersion\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"assessmentCompletedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"outdatedSchema\" : true,  \"type\" : \"Initial\",  \"applicationCompletedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"placementDates\" : [ {    \"duration\" : 0,    \"expectedArrival\" : \"2000-01-23\"  }, {    \"duration\" : 0,    \"expectedArrival\" : \"2000-01-23\"  } ],  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"isWithdrawn\" : true,  \"withdrawalReason\" : \"DuplicatePlacementRequest\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"canBeWithdrawn\" : true,  \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}")
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
     * @see PlacementApplicationsApi#placementApplicationsIdGet
     */
    fun placementApplicationsIdGet(id: java.util.UUID): ResponseEntity<PlacementApplication> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"schemaVersion\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"assessmentCompletedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"outdatedSchema\" : true,  \"type\" : \"Initial\",  \"applicationCompletedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"placementDates\" : [ {    \"duration\" : 0,    \"expectedArrival\" : \"2000-01-23\"  }, {    \"duration\" : 0,    \"expectedArrival\" : \"2000-01-23\"  } ],  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"isWithdrawn\" : true,  \"withdrawalReason\" : \"DuplicatePlacementRequest\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"canBeWithdrawn\" : true,  \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}")
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
     * @see PlacementApplicationsApi#placementApplicationsIdPut
     */
    fun placementApplicationsIdPut(id: java.util.UUID,
        updatePlacementApplication: UpdatePlacementApplication): ResponseEntity<PlacementApplication> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"schemaVersion\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"assessmentCompletedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"outdatedSchema\" : true,  \"type\" : \"Initial\",  \"applicationCompletedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"placementDates\" : [ {    \"duration\" : 0,    \"expectedArrival\" : \"2000-01-23\"  }, {    \"duration\" : 0,    \"expectedArrival\" : \"2000-01-23\"  } ],  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"isWithdrawn\" : true,  \"withdrawalReason\" : \"DuplicatePlacementRequest\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"canBeWithdrawn\" : true,  \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}")
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
     * @see PlacementApplicationsApi#placementApplicationsIdSubmissionPost
     */
    fun placementApplicationsIdSubmissionPost(id: java.util.UUID,
        submitPlacementApplication: SubmitPlacementApplication): ResponseEntity<List<PlacementApplication>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"schemaVersion\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"assessmentCompletedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"outdatedSchema\" : true,  \"type\" : \"Initial\",  \"applicationCompletedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"placementDates\" : [ {    \"duration\" : 0,    \"expectedArrival\" : \"2000-01-23\"  }, {    \"duration\" : 0,    \"expectedArrival\" : \"2000-01-23\"  } ],  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"isWithdrawn\" : true,  \"withdrawalReason\" : \"DuplicatePlacementRequest\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"canBeWithdrawn\" : true,  \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}, {  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"schemaVersion\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"assessmentCompletedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"outdatedSchema\" : true,  \"type\" : \"Initial\",  \"applicationCompletedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"placementDates\" : [ {    \"duration\" : 0,    \"expectedArrival\" : \"2000-01-23\"  }, {    \"duration\" : 0,    \"expectedArrival\" : \"2000-01-23\"  } ],  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"isWithdrawn\" : true,  \"withdrawalReason\" : \"DuplicatePlacementRequest\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"canBeWithdrawn\" : true,  \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"} ]")
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
     * @see PlacementApplicationsApi#placementApplicationsIdWithdrawPost
     */
    fun placementApplicationsIdWithdrawPost(id: java.util.UUID,
        withdrawPlacementApplication: WithdrawPlacementApplication?): ResponseEntity<PlacementApplication> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"schemaVersion\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"assessmentCompletedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"outdatedSchema\" : true,  \"type\" : \"Initial\",  \"applicationCompletedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"placementDates\" : [ {    \"duration\" : 0,    \"expectedArrival\" : \"2000-01-23\"  }, {    \"duration\" : 0,    \"expectedArrival\" : \"2000-01-23\"  } ],  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"isWithdrawn\" : true,  \"withdrawalReason\" : \"DuplicatePlacementRequest\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"canBeWithdrawn\" : true,  \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}")
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
     * @see PlacementApplicationsApi#placementApplicationsPost
     */
    fun placementApplicationsPost(newPlacementApplication: NewPlacementApplication): ResponseEntity<PlacementApplication> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"schemaVersion\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"assessmentCompletedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"outdatedSchema\" : true,  \"type\" : \"Initial\",  \"applicationCompletedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"placementDates\" : [ {    \"duration\" : 0,    \"expectedArrival\" : \"2000-01-23\"  }, {    \"duration\" : 0,    \"expectedArrival\" : \"2000-01-23\"  } ],  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"isWithdrawn\" : true,  \"withdrawalReason\" : \"DuplicatePlacementRequest\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"canBeWithdrawn\" : true,  \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}")
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
