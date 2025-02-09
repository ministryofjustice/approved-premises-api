/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.11.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
*/
package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import io.swagger.v3.oas.annotations.*
import io.swagger.v3.oas.annotations.media.*
import io.swagger.v3.oas.annotations.responses.*
import org.springframework.http.ResponseEntity

import org.springframework.web.bind.annotation.*

@RestController
interface MigrationJobApi {

    fun getDelegate(): MigrationJobApiDelegate = object: MigrationJobApiDelegate {}

    @Operation(
        tags = ["default",],
        summary = "Starts a migration job (process for data migrations that can't be achieved solely via SQL migrations), can only be called from a local connection",
        operationId = "migrationJobPost",
        description = """""",
        responses = [
            ApiResponse(responseCode = "202", description = "successfully requested task"),
            ApiResponse(responseCode = "401", description = "not authenticated", content = [Content(schema = Schema(implementation = Problem::class))]),
            ApiResponse(responseCode = "403", description = "unauthorised", content = [Content(schema = Schema(implementation = Problem::class))]),
            ApiResponse(responseCode = "500", description = "unexpected error", content = [Content(schema = Schema(implementation = Problem::class))])
        ]
    )
    @RequestMapping(
            method = [RequestMethod.POST],
            value = ["/migration-job"],
            produces = ["application/json"],
            consumes = ["application/json"]
    )
    fun migrationJobPost(@Parameter(description = "", required = true) @RequestBody migrationJobRequest: MigrationJobRequest): ResponseEntity<Unit> {
        return getDelegate().migrationJobPost(migrationJobRequest)
    }
}
