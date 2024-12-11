package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2bail

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2bail.ReportsCas2bailDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generateXlsxStreamingResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2bail.Cas2BailReportsService

@Service("Cas2BailReportsController")
class Cas2BailReportsController(private val cas2BailReportService: Cas2BailReportsService) : ReportsCas2bailDelegate {

  override fun reportsReportNameGet(reportName: Cas2ReportName): ResponseEntity<StreamingResponseBody> {
    return when (reportName) {
      Cas2ReportName.submittedMinusApplications -> generateXlsxStreamingResponse {
          outputStream ->
        cas2BailReportService.createSubmittedApplicationsReport(outputStream)
      }
      Cas2ReportName.applicationMinusStatusMinusUpdates -> generateXlsxStreamingResponse {
          outputStream ->
        cas2BailReportService.createApplicationStatusUpdatesReport(outputStream)
      }
      Cas2ReportName.unsubmittedMinusApplications -> generateXlsxStreamingResponse {
          outputStream ->
        cas2BailReportService.createUnsubmittedApplicationsReport(outputStream)
      }
    }
  }
}
