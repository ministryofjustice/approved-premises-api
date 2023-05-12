package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.ReportsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUsageReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUtilisationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ReportService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import java.io.ByteArrayOutputStream
import java.util.UUID

@Service
class ReportsController(
  private val reportService: ReportService,
  private val userAccessService: UserAccessService,
) : ReportsApiDelegate {

  override fun reportsBookingsGet(xServiceName: ServiceName, year: Int, month: Int, probationRegionId: UUID?): ResponseEntity<Resource> {
    when {
      probationRegionId == null && !userAccessService.currentUserHasAllRegionsAccess() -> throw ForbiddenProblem()
      probationRegionId != null && !userAccessService.currentUserCanAccessRegion(probationRegionId) -> throw ForbiddenProblem()
    }

    if (month < 1 || month > 12) {
      throw BadRequestProblem(errorDetail = "month must be between 1 and 12")
    }

    val properties = BookingsReportProperties(xServiceName, probationRegionId, year, month)
    val outputStream = ByteArrayOutputStream()

    reportService.createBookingsReport(properties, outputStream)

    return ResponseEntity.ok(InputStreamResource(outputStream.toByteArray().inputStream()))
  }

  override fun reportsBedUsageGet(xServiceName: ServiceName, year: Int, month: Int, probationRegionId: UUID?): ResponseEntity<Resource> {
    validateParameters(probationRegionId, month)

    val properties = BedUsageReportProperties(xServiceName, probationRegionId, year, month)
    val outputStream = ByteArrayOutputStream()

    reportService.createBedUsageReport(properties, outputStream)

    return ResponseEntity.ok(InputStreamResource(outputStream.toByteArray().inputStream()))
  }

  override fun reportsBedUtilisationGet(xServiceName: ServiceName, year: Int, month: Int, probationRegionId: UUID?): ResponseEntity<Resource> {
    validateParameters(probationRegionId, month)

    val properties = BedUtilisationReportProperties(xServiceName, probationRegionId, year, month)
    val outputStream = ByteArrayOutputStream()

    reportService.createBedUtilisationReport(properties, outputStream)

    return ResponseEntity.ok(InputStreamResource(outputStream.toByteArray().inputStream()))
  }

  private fun validateParameters(probationRegionId: UUID?, month: Int) {
    when {
      probationRegionId == null && !userAccessService.currentUserHasAllRegionsAccess() -> throw ForbiddenProblem()
      probationRegionId != null && !userAccessService.currentUserCanAccessRegion(probationRegionId) -> throw ForbiddenProblem()
    }

    if (month < 1 || month > 12) {
      throw BadRequestProblem(errorDetail = "month must be between 1 and 12")
    }
  }
}
