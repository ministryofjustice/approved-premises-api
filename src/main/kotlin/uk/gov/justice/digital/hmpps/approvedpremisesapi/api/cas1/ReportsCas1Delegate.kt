package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link ReportsCas1Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
interface ReportsCas1Delegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see ReportsCas1#reportsReportNameGet
     */
    fun reportsReportNameGet(xServiceName: ServiceName,
        reportName: Cas1ReportName,
        year: kotlin.Int,
        month: kotlin.Int,
        includePii: kotlin.Boolean?): ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }

}
