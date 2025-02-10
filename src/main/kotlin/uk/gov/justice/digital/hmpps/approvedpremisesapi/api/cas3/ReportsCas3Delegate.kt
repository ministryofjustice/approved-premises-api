package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3ReportType
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link ReportsCas3Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
interface ReportsCas3Delegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see ReportsCas3#reportsReportNameGet
     */
    fun reportsReportNameGet(reportName: Cas3ReportType,
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate,
        probationRegionId: java.util.UUID?): ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }

}
