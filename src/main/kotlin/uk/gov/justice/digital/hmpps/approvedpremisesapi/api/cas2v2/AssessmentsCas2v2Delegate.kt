package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2v2

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2AssessmentStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link AssessmentsCas2v2Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
interface AssessmentsCas2v2Delegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see AssessmentsCas2v2#assessmentsAssessmentIdGet
     */
    fun assessmentsAssessmentIdGet(assessmentId: java.util.UUID): ResponseEntity<Cas2v2Assessment> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"assessorName\" : \"assessorName\",  \"nacroReferralId\" : \"nacroReferralId\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"statusUpdates\" : [ {    \"updatedBy\" : {      \"authSource\" : \"nomis\",      \"name\" : \"Roger Smith\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true,      \"email\" : \"Roger.Smith@justice.gov.uk\",      \"username\" : \"SMITHR_GEN\"    },    \"statusUpdateDetails\" : [ {      \"name\" : \"moreInfoRequested\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"label\" : \"More information requested\"    }, {      \"name\" : \"moreInfoRequested\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"label\" : \"More information requested\"    } ],    \"name\" : \"moreInfoRequested\",    \"description\" : \"More information about the application has been requested from the HMPPS user.\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"label\" : \"More information requested\",    \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"  }, {    \"updatedBy\" : {      \"authSource\" : \"nomis\",      \"name\" : \"Roger Smith\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true,      \"email\" : \"Roger.Smith@justice.gov.uk\",      \"username\" : \"SMITHR_GEN\"    },    \"statusUpdateDetails\" : [ {      \"name\" : \"moreInfoRequested\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"label\" : \"More information requested\"    }, {      \"name\" : \"moreInfoRequested\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"label\" : \"More information requested\"    } ],    \"name\" : \"moreInfoRequested\",    \"description\" : \"More information about the application has been requested from the HMPPS user.\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"label\" : \"More information requested\",    \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"  } ]}")
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
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see AssessmentsCas2v2#assessmentsAssessmentIdNotesPost
     */
    fun assessmentsAssessmentIdNotesPost(assessmentId: java.util.UUID,
        body: NewCas2ApplicationNote): ResponseEntity<Cas2v2ApplicationNote> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"name\" : \"Roger Smith\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"body\" : \"body\",  \"email\" : \"roger@example.com\"}")
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
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see AssessmentsCas2v2#assessmentsAssessmentIdPut
     */
    fun assessmentsAssessmentIdPut(assessmentId: java.util.UUID,
        updateCas2Assessment: UpdateCas2Assessment): ResponseEntity<Cas2v2Assessment> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"assessorName\" : \"assessorName\",  \"nacroReferralId\" : \"nacroReferralId\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"statusUpdates\" : [ {    \"updatedBy\" : {      \"authSource\" : \"nomis\",      \"name\" : \"Roger Smith\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true,      \"email\" : \"Roger.Smith@justice.gov.uk\",      \"username\" : \"SMITHR_GEN\"    },    \"statusUpdateDetails\" : [ {      \"name\" : \"moreInfoRequested\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"label\" : \"More information requested\"    }, {      \"name\" : \"moreInfoRequested\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"label\" : \"More information requested\"    } ],    \"name\" : \"moreInfoRequested\",    \"description\" : \"More information about the application has been requested from the HMPPS user.\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"label\" : \"More information requested\",    \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"  }, {    \"updatedBy\" : {      \"authSource\" : \"nomis\",      \"name\" : \"Roger Smith\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true,      \"email\" : \"Roger.Smith@justice.gov.uk\",      \"username\" : \"SMITHR_GEN\"    },    \"statusUpdateDetails\" : [ {      \"name\" : \"moreInfoRequested\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"label\" : \"More information requested\"    }, {      \"name\" : \"moreInfoRequested\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"label\" : \"More information requested\"    } ],    \"name\" : \"moreInfoRequested\",    \"description\" : \"More information about the application has been requested from the HMPPS user.\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"label\" : \"More information requested\",    \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"  } ]}")
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
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see AssessmentsCas2v2#assessmentsAssessmentIdStatusUpdatesPost
     */
    fun assessmentsAssessmentIdStatusUpdatesPost(assessmentId: java.util.UUID,
        cas2v2AssessmentStatusUpdate: Cas2v2AssessmentStatusUpdate): ResponseEntity<Unit> {
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

}
