package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2v2

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2v2.ReportsCas2v2Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generateXlsxStreamingResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2.Cas2v2ReportsService

@Service("Cas2v2ReportsController")
class Cas2v2ReportsController(private val cas2v2ReportService: Cas2v2ReportsService) : ReportsCas2v2Delegate {

  override fun reportsReportNameGet(reportName: Cas2ReportName): ResponseEntity<StreamingResponseBody> {
    return when (reportName) {
      Cas2ReportName.submittedMinusApplications -> generateXlsxStreamingResponse {
          outputStream ->
        cas2v2ReportService.createSubmittedApplicationsReport(outputStream)
      }
      Cas2ReportName.applicationMinusStatusMinusUpdates -> generateXlsxStreamingResponse {
          outputStream ->
        cas2v2ReportService.createApplicationStatusUpdatesReport(outputStream)
      }
      Cas2ReportName.unsubmittedMinusApplications -> generateXlsxStreamingResponse {
          outputStream ->
        cas2v2ReportService.createUnsubmittedApplicationsReport(outputStream)
      }
    }
  }
}
