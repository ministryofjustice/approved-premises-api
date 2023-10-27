package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BookingsReportData

data class BookingsReportDataAndPersonInfo(
  val bookingsReportData: BookingsReportData,
  val personInfoResult: PersonInfoResult,
)
