package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2bail

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailApplicationStatusUpdatesReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailSubmittedApplicationReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailUnsubmittedApplicationsReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.cas2.ApplicationStatusUpdatesReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.cas2.SubmittedApplicationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.cas2.UnsubmittedApplicationsReportRow
import java.io.OutputStream

@Service
class Cas2BailReportsService(
  private val cas2BailSubmittedApplicationReportRepository: Cas2BailSubmittedApplicationReportRepository,
  private val cas2BailApplicationStatusUpdatesReportRepository: Cas2BailApplicationStatusUpdatesReportRepository,
  private val cas2BailUnsubmittedApplicationsReportRepository: Cas2BailUnsubmittedApplicationsReportRepository,
) {

  fun createSubmittedApplicationsReport(outputStream: OutputStream) {
    val reportData = cas2BailSubmittedApplicationReportRepository.generateSubmittedApplicationReportRows().map { row ->
      SubmittedApplicationReportRow(
        eventId = row.getId(),
        applicationId = row.getApplicationId(),
        personCrn = row.getPersonCrn(),
        personNoms = row.getPersonNoms(),
        referringPrisonCode = row.getReferringPrisonCode(),
        preferredAreas = row.getPreferredAreas(),
        hdcEligibilityDate = row.getHdcEligibilityDate(),
        conditionalReleaseDate = row.getConditionalReleaseDate(),
        submittedBy = row.getSubmittedBy(),
        submittedAt = row.getSubmittedAt(),
        startedAt = row.getStartedAt(),
      )
    }

    reportData.toDataFrame()
      .writeExcel(
        outputStream = outputStream,
        factory = WorkbookFactory.create(true),
      )
  }

  fun createApplicationStatusUpdatesReport(outputStream: OutputStream) {
    val reportData = cas2BailApplicationStatusUpdatesReportRepository.generateApplicationStatusUpdatesReportRows().map { row ->
      ApplicationStatusUpdatesReportRow(
        eventId = row.getId(),
        applicationId = row.getApplicationId(),
        personCrn = row.getPersonCrn(),
        personNoms = row.getPersonNoms(),
        newStatus = row.getNewStatus(),
        updatedBy = row.getUpdatedBy(),
        updatedAt = row.getUpdatedAt(),
        statusDetails = row.getStatusDetails(),
      )
    }

    reportData.toDataFrame()
      .writeExcel(
        outputStream = outputStream,
        factory = WorkbookFactory.create(true),
      )
  }

  fun createUnsubmittedApplicationsReport(outputStream: OutputStream) {
    val reportData = cas2BailUnsubmittedApplicationsReportRepository.generateUnsubmittedApplicationsReportRows().map { row ->
      UnsubmittedApplicationsReportRow(
        applicationId = row.getApplicationId(),
        personCrn = row.getPersonCrn(),
        personNoms = row.getPersonNoms(),
        startedBy = row.getStartedBy(),
        startedAt = row.getStartedAt(),
      )
    }

    reportData.toDataFrame()
      .writeExcel(
        outputStream = outputStream,
        factory = WorkbookFactory.create(true),
      )
  }
}
