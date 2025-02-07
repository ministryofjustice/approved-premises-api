/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.11.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generated.cas1

import io.swagger.v3.oas.annotations.*
import io.swagger.v3.oas.annotations.media.*
import io.swagger.v3.oas.annotations.responses.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import kotlin.collections.List

@RestController
interface ApplicationsCas1 {

  fun getDelegate(): ApplicationsCas1Delegate = object : ApplicationsCas1Delegate {}

  @Operation(
    tags = ["Applications"],
    summary = "Returns domain event summary",
    operationId = "getApplicationTimeLine",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "successful operation",
        content = [Content(array = ArraySchema(schema = Schema(implementation = Cas1TimelineEvent::class)))],
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
        responseCode = "404",
        description = "invalid CRN",
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
    value = ["/applications/{applicationId}/timeline"],
    produces = ["application/json"],
  )
  fun getApplicationTimeLine(
    @Parameter(
      description = "ID of the application",
      required = true,
    ) @PathVariable("applicationId") applicationId: java.util.UUID,
  ): ResponseEntity<List<Cas1TimelineEvent>> {
    return getDelegate().getApplicationTimeLine(applicationId)
  }
}
