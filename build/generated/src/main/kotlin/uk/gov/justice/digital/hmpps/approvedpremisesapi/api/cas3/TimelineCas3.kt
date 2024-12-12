/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.7.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
*/
package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryNote
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

interface TimelineCas3 {

    fun getDelegate(): TimelineCas3Delegate = object: TimelineCas3Delegate {}

    @Operation(
        tags = ["CAS3",],
        summary = "Returns the timeline entries for a given Assessment.",
        operationId = "getTimelineEntries",
        description = """""",
        responses = [
            ApiResponse(responseCode = "200", description = "Successfully retrieved the timeline events", content = [Content(array = ArraySchema(schema = Schema(implementation = ReferralHistoryNote::class)))])
        ]
    )
    @RequestMapping(
            method = [RequestMethod.GET],
            value = ["/timeline/{assessmentId}"],
            produces = ["application/json"]
    )
    fun getTimelineEntries(@Parameter(description = "", required = true) @PathVariable("assessmentId") assessmentId: java.util.UUID): ResponseEntity<List<ReferralHistoryNote>> {
        return getDelegate().getTimelineEntries(assessmentId)
    }
}
