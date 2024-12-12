/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.7.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
*/
package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
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

interface BedsApi {

    fun getDelegate(): BedsApiDelegate = object: BedsApiDelegate {}

    @Operation(
        tags = ["default",],
        summary = "Searches for available Beds within the given parameters",
        operationId = "bedsSearchPost",
        description = """""",
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = BedSearchResults::class))]),
            ApiResponse(responseCode = "400", description = "invalid params", content = [Content(schema = Schema(implementation = ValidationError::class))]),
            ApiResponse(responseCode = "401", description = "not authenticated", content = [Content(schema = Schema(implementation = Problem::class))]),
            ApiResponse(responseCode = "403", description = "unauthorised", content = [Content(schema = Schema(implementation = Problem::class))]),
            ApiResponse(responseCode = "500", description = "unexpected error", content = [Content(schema = Schema(implementation = Problem::class))])
        ]
    )
    @RequestMapping(
            method = [RequestMethod.POST],
            value = ["/beds/search"],
            produces = ["application/json", "application/problem+json"],
            consumes = ["application/json"]
    )
    fun bedsSearchPost(@Parameter(description = "", required = true) @RequestBody bedSearchParameters: BedSearchParameters): ResponseEntity<BedSearchResults> {
        return getDelegate().bedsSearchPost(bedSearchParameters)
    }
}
