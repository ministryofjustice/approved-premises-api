/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.7.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
*/
package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2AssessmentStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import io.swagger.v3.oas.annotations.*
import io.swagger.v3.oas.annotations.enums.*
import io.swagger.v3.oas.annotations.media.*
import io.swagger.v3.oas.annotations.responses.*
import io.swagger.v3.oas.annotations.security.*
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.beans.factory.annotation.Autowired


import kotlin.collections.List
import kotlin.collections.Map

interface AssessmentsCas2 {

    fun getDelegate(): AssessmentsCas2Delegate = object: AssessmentsCas2Delegate {}

    @Operation(
        tags = ["Operations on submitted CAS2 applications (Assessors)",],
        summary = "Gets a single CAS2 assessment by its ID",
        operationId = "assessmentsAssessmentIdGet",
        description = """""",
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas2Assessment::class))]),
            ApiResponse(responseCode = "401", description = "not authenticated", content = [Content(schema = Schema(implementation = Problem::class))]),
            ApiResponse(responseCode = "403", description = "unauthorised", content = [Content(schema = Schema(implementation = Problem::class))]),
            ApiResponse(responseCode = "404", description = "invalid assessmentId", content = [Content(schema = Schema(implementation = Problem::class))]),
            ApiResponse(responseCode = "500", description = "unexpected error", content = [Content(schema = Schema(implementation = Problem::class))])
        ]
    )
    @RequestMapping(
            method = [RequestMethod.GET],
            value = ["/assessments/{assessmentId}"],
            produces = ["application/json"]
    )
    fun assessmentsAssessmentIdGet(@Parameter(description = "ID of the assessment", required = true) @PathVariable("assessmentId") assessmentId: java.util.UUID): ResponseEntity<Cas2Assessment> {
        return getDelegate().assessmentsAssessmentIdGet(assessmentId)
    }

    @Operation(
        tags = ["Operations on CAS2 assessments",],
        summary = "Add a note to an assessment",
        operationId = "assessmentsAssessmentIdNotesPost",
        description = """""",
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas2ApplicationNote::class))]),
            ApiResponse(responseCode = "401", description = "not authenticated", content = [Content(schema = Schema(implementation = Problem::class))]),
            ApiResponse(responseCode = "403", description = "unauthorised", content = [Content(schema = Schema(implementation = Problem::class))]),
            ApiResponse(responseCode = "404", description = "invalid assessmentId", content = [Content(schema = Schema(implementation = Problem::class))]),
            ApiResponse(responseCode = "500", description = "unexpected error", content = [Content(schema = Schema(implementation = Problem::class))])
        ]
    )
    @RequestMapping(
            method = [RequestMethod.POST],
            value = ["/assessments/{assessmentId}/notes"],
            produces = ["application/json"],
            consumes = ["application/json"]
    )
    fun assessmentsAssessmentIdNotesPost(@Parameter(description = "ID of the assessment", required = true) @PathVariable("assessmentId") assessmentId: java.util.UUID,@Parameter(description = "the note to add", required = true) @RequestBody body: NewCas2ApplicationNote): ResponseEntity<Cas2ApplicationNote> {
        return getDelegate().assessmentsAssessmentIdNotesPost(assessmentId, body)
    }

    @Operation(
        tags = ["Operations on submitted CAS2 applications (Assessors)",],
        summary = "Updates a single CAS2 assessment by its ID",
        operationId = "assessmentsAssessmentIdPut",
        description = """""",
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas2Assessment::class))]),
            ApiResponse(responseCode = "401", description = "not authenticated", content = [Content(schema = Schema(implementation = Problem::class))]),
            ApiResponse(responseCode = "403", description = "unauthorised", content = [Content(schema = Schema(implementation = Problem::class))]),
            ApiResponse(responseCode = "404", description = "invalid assessmentId", content = [Content(schema = Schema(implementation = Problem::class))]),
            ApiResponse(responseCode = "500", description = "unexpected error", content = [Content(schema = Schema(implementation = Problem::class))])
        ]
    )
    @RequestMapping(
            method = [RequestMethod.PUT],
            value = ["/assessments/{assessmentId}"],
            produces = ["application/json"],
            consumes = ["application/json"]
    )
    fun assessmentsAssessmentIdPut(@Parameter(description = "ID of the assessment", required = true) @PathVariable("assessmentId") assessmentId: java.util.UUID,@Parameter(description = "Information to update the assessment with", required = true) @RequestBody updateCas2Assessment: UpdateCas2Assessment): ResponseEntity<Cas2Assessment> {
        return getDelegate().assessmentsAssessmentIdPut(assessmentId, updateCas2Assessment)
    }

    @Operation(
        tags = ["Operations on submitted CAS2 applications (Assessors)",],
        summary = "Creates a status update on an assessment",
        operationId = "assessmentsAssessmentIdStatusUpdatesPost",
        description = """""",
        responses = [
            ApiResponse(responseCode = "200", description = "successfully created the status update"),
            ApiResponse(responseCode = "400", description = "status update has already been submitted", content = [Content(schema = Schema(implementation = ValidationError::class))]),
            ApiResponse(responseCode = "401", description = "not authenticated", content = [Content(schema = Schema(implementation = Problem::class))]),
            ApiResponse(responseCode = "403", description = "unauthorised", content = [Content(schema = Schema(implementation = Problem::class))]),
            ApiResponse(responseCode = "500", description = "unexpected error", content = [Content(schema = Schema(implementation = Problem::class))])
        ]
    )
    @RequestMapping(
            method = [RequestMethod.POST],
            value = ["/assessments/{assessmentId}/status-updates"],
            produces = ["application/problem+json", "application/json"],
            consumes = ["application/json"]
    )
    fun assessmentsAssessmentIdStatusUpdatesPost(@Parameter(description = "ID of the assessment whose status is to be updated", required = true) @PathVariable("assessmentId") assessmentId: java.util.UUID,@Parameter(description = "Information on the new status to be applied", required = true) @RequestBody cas2AssessmentStatusUpdate: Cas2AssessmentStatusUpdate): ResponseEntity<Unit> {
        return getDelegate().assessmentsAssessmentIdStatusUpdatesPost(assessmentId, cas2AssessmentStatusUpdate)
    }
}
