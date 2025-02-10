package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentRejection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReferralHistoryUserNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatedClarificationNote
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link AssessmentsApiController}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
interface AssessmentsApiDelegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see AssessmentsApi#assessmentsAssessmentIdAcceptancePost
     */
    fun assessmentsAssessmentIdAcceptancePost(assessmentId: java.util.UUID,
        assessmentAcceptance: AssessmentAcceptance): ResponseEntity<Unit> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
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
     * @see AssessmentsApi#assessmentsAssessmentIdClosurePost
     */
    fun assessmentsAssessmentIdClosurePost(assessmentId: java.util.UUID): ResponseEntity<Unit> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
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
     * @see AssessmentsApi#assessmentsAssessmentIdGet
     */
    fun assessmentsAssessmentIdGet(assessmentId: java.util.UUID): ResponseEntity<Assessment> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"schemaVersion\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"decision\" : \"accepted\",  \"data\" : \"{}\",  \"service\" : \"service\",  \"rejectionRationale\" : \"rejectionRationale\",  \"outdatedSchema\" : true,  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"allocatedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"clarificationNotes\" : [ {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"response\" : \"response\",    \"query\" : \"query\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"responseReceivedOn\" : \"2000-01-23\",    \"createdByStaffMemberId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"response\" : \"response\",    \"query\" : \"query\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"responseReceivedOn\" : \"2000-01-23\",    \"createdByStaffMemberId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ]}")
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
     * @see AssessmentsApi#assessmentsAssessmentIdNotesNoteIdPut
     */
    fun assessmentsAssessmentIdNotesNoteIdPut(assessmentId: java.util.UUID,
        noteId: java.util.UUID,
        updatedClarificationNote: UpdatedClarificationNote): ResponseEntity<ClarificationNote> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"response\" : \"response\",  \"query\" : \"query\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"responseReceivedOn\" : \"2000-01-23\",  \"createdByStaffMemberId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}")
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
     * @see AssessmentsApi#assessmentsAssessmentIdNotesPost
     */
    fun assessmentsAssessmentIdNotesPost(assessmentId: java.util.UUID,
        newClarificationNote: NewClarificationNote): ResponseEntity<ClarificationNote> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"response\" : \"response\",  \"query\" : \"query\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"responseReceivedOn\" : \"2000-01-23\",  \"createdByStaffMemberId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}")
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
     * @see AssessmentsApi#assessmentsAssessmentIdPut
     */
    fun assessmentsAssessmentIdPut(assessmentId: java.util.UUID,
        updateAssessment: UpdateAssessment,
        xServiceName: ServiceName?): ResponseEntity<Assessment> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"schemaVersion\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"decision\" : \"accepted\",  \"data\" : \"{}\",  \"service\" : \"service\",  \"rejectionRationale\" : \"rejectionRationale\",  \"outdatedSchema\" : true,  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"allocatedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"clarificationNotes\" : [ {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"response\" : \"response\",    \"query\" : \"query\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"responseReceivedOn\" : \"2000-01-23\",    \"createdByStaffMemberId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"response\" : \"response\",    \"query\" : \"query\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"responseReceivedOn\" : \"2000-01-23\",    \"createdByStaffMemberId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ]}")
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
     * @see AssessmentsApi#assessmentsAssessmentIdReferralHistoryNotesPost
     */
    fun assessmentsAssessmentIdReferralHistoryNotesPost(assessmentId: java.util.UUID,
        newReferralHistoryUserNote: NewReferralHistoryUserNote): ResponseEntity<ReferralHistoryNote> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"createdByUserName\" : \"createdByUserName\",  \"messageDetails\" : {    \"isWithdrawn\" : true,    \"rejectionReason\" : \"rejectionReason\",    \"domainEvent\" : \"{}\",    \"rejectionReasonDetails\" : \"rejectionReasonDetails\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"message\" : \"message\",  \"type\" : \"type\"}")
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
     * @see AssessmentsApi#assessmentsAssessmentIdRejectionPost
     */
    fun assessmentsAssessmentIdRejectionPost(assessmentId: java.util.UUID,
        assessmentRejection: AssessmentRejection): ResponseEntity<Unit> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
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
     * @see AssessmentsApi#assessmentsGet
     */
    fun assessmentsGet(xServiceName: ServiceName,
        sortDirection: SortDirection?,
        sortBy: AssessmentSortField?,
        statuses: kotlin.collections.List<AssessmentStatus>?,
        crnOrName: kotlin.String?,
        page: kotlin.Int?,
        perPage: kotlin.Int?): ResponseEntity<List<AssessmentSummary>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"risks\" : {    \"mappa\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"tier\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"roshRisks\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"overallRisk\" : \"overallRisk\",        \"riskToChildren\" : \"riskToChildren\",        \"riskToPublic\" : \"riskToPublic\",        \"riskToKnownAdult\" : \"riskToKnownAdult\",        \"riskToStaff\" : \"riskToStaff\"      },      \"status\" : \"retrieved\"    },    \"flags\" : {      \"value\" : [ \"value\", \"value\" ]    },    \"crn\" : \"crn\"  },  \"decision\" : \"accepted\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"dateOfInfoRequest\" : \"2000-01-23T04:56:07.000+00:00\",  \"type\" : \"type\",  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"arrivalDate\" : \"2000-01-23T04:56:07.000+00:00\"}, {  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"risks\" : {    \"mappa\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"tier\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"roshRisks\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"overallRisk\" : \"overallRisk\",        \"riskToChildren\" : \"riskToChildren\",        \"riskToPublic\" : \"riskToPublic\",        \"riskToKnownAdult\" : \"riskToKnownAdult\",        \"riskToStaff\" : \"riskToStaff\"      },      \"status\" : \"retrieved\"    },    \"flags\" : {      \"value\" : [ \"value\", \"value\" ]    },    \"crn\" : \"crn\"  },  \"decision\" : \"accepted\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"dateOfInfoRequest\" : \"2000-01-23T04:56:07.000+00:00\",  \"type\" : \"type\",  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"arrivalDate\" : \"2000-01-23T04:56:07.000+00:00\"} ]")
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
