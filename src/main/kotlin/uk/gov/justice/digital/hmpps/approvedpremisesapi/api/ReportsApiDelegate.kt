package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link ReportsApiController}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
interface ReportsApiDelegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see ReportsApi#reportsBedUsageGet
     */
    fun reportsBedUsageGet(xServiceName: ServiceName,
        year: kotlin.Int,
        month: kotlin.Int,
        probationRegionId: java.util.UUID?): ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see ReportsApi#reportsBedUtilisationGet
     */
    fun reportsBedUtilisationGet(xServiceName: ServiceName,
        year: kotlin.Int,
        month: kotlin.Int,
        probationRegionId: java.util.UUID?): ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see ReportsApi#reportsBookingsGet
     */
    fun reportsBookingsGet(xServiceName: ServiceName,
        year: kotlin.Int,
        month: kotlin.Int,
        probationRegionId: java.util.UUID?): ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }

}
