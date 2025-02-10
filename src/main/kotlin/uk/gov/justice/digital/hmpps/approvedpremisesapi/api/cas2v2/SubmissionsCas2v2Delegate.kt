package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2v2

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2SubmittedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2SubmittedApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitCas2v2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link SubmissionsCas2v2Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
interface SubmissionsCas2v2Delegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see SubmissionsCas2v2#submissionsApplicationIdGet
     */
    fun submissionsApplicationIdGet(applicationId: java.util.UUID): ResponseEntity<Cas2v2SubmittedApplication> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"submittedBy\" : {    \"authSource\" : \"nomis\",    \"name\" : \"Roger Smith\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true,    \"email\" : \"Roger.Smith@justice.gov.uk\",    \"username\" : \"SMITHR_GEN\"  },  \"schemaVersion\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"telephoneNumber\" : \"telephoneNumber\",  \"bailHearingDate\" : \"2000-01-23\",  \"document\" : \"{}\",  \"outdatedSchema\" : true,  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"timelineEvents\" : [ {    \"createdByName\" : \"createdByName\",    \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"label\" : \"label\",    \"type\" : \"approved_premises_application_submitted\",    \"body\" : \"body\"  }, {    \"createdByName\" : \"createdByName\",    \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"label\" : \"label\",    \"type\" : \"approved_premises_application_submitted\",    \"body\" : \"body\"  } ],  \"assessment\" : {    \"assessorName\" : \"assessorName\",    \"nacroReferralId\" : \"nacroReferralId\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"statusUpdates\" : [ {      \"updatedBy\" : {        \"authSource\" : \"nomis\",        \"name\" : \"Roger Smith\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"isActive\" : true,        \"email\" : \"Roger.Smith@justice.gov.uk\",        \"username\" : \"SMITHR_GEN\"      },      \"statusUpdateDetails\" : [ {        \"name\" : \"moreInfoRequested\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"label\" : \"More information requested\"      }, {        \"name\" : \"moreInfoRequested\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"label\" : \"More information requested\"      } ],      \"name\" : \"moreInfoRequested\",      \"description\" : \"More information about the application has been requested from the HMPPS user.\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"label\" : \"More information requested\",      \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"    }, {      \"updatedBy\" : {        \"authSource\" : \"nomis\",        \"name\" : \"Roger Smith\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"isActive\" : true,        \"email\" : \"Roger.Smith@justice.gov.uk\",        \"username\" : \"SMITHR_GEN\"      },      \"statusUpdateDetails\" : [ {        \"name\" : \"moreInfoRequested\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"label\" : \"More information requested\"      }, {        \"name\" : \"moreInfoRequested\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"label\" : \"More information requested\"      } ],      \"name\" : \"moreInfoRequested\",      \"description\" : \"More information about the application has been requested from the HMPPS user.\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"label\" : \"More information requested\",      \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"    } ]  },  \"applicationOrigin\" : \"homeDetentionCurfew\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\"}")
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
     * @see SubmissionsCas2v2#submissionsGet
     */
    fun submissionsGet(page: kotlin.Int?): ResponseEntity<List<Cas2v2SubmittedApplicationSummary>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"personName\" : \"personName\",  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"applicationOrigin\" : \"homeDetentionCurfew\",  \"nomsNumber\" : \"nomsNumber\",  \"bailHearingDate\" : \"2000-01-23\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"crn\" : \"crn\"}, {  \"personName\" : \"personName\",  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"applicationOrigin\" : \"homeDetentionCurfew\",  \"nomsNumber\" : \"nomsNumber\",  \"bailHearingDate\" : \"2000-01-23\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"crn\" : \"crn\"} ]")
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
     * @see SubmissionsCas2v2#submissionsPost
     */
    fun submissionsPost(submitCas2v2Application: SubmitCas2v2Application): ResponseEntity<Unit> {
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
