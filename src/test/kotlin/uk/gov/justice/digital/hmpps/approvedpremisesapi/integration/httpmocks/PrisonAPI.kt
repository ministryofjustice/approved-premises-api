package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisoneralertsapi.AlertsPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AdjudicationsPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.PrisonerInPrisonSummary

fun IntegrationTestBase.prisonAPIMockSuccessfulInmateDetailsCall(inmateDetail: InmateDetail) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/api/offenders/${inmateDetail.offenderNo}",
    responseBody = inmateDetail,
  )

fun IntegrationTestBase.prisonAPIMockNotFoundInmateDetailsCall(offenderNo: String) =
  mockUnsuccessfulGetCall(
    url = "/api/offenders/$offenderNo",
    responseStatus = 404,
  )

fun IntegrationTestBase.prisonAPIMockServerErrorInmateDetailsCall(offenderNo: String) =
  mockUnsuccessfulGetCall(
    url = "/api/offenders/$offenderNo",
    responseStatus = 500,
  )

fun IntegrationTestBase.prisonerAlertsAPIMockSuccessfulAlertsCall(nomsNumber: String, alertCode: String, alertsPage: AlertsPage) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/prisoners/$nomsNumber/alerts?alertCode=$alertCode&sort=createdAt,DESC",
    responseBody = alertsPage,
  )

fun IntegrationTestBase.prisonAPIMockSuccessfulPrisonTimeLineCall(nomsNumber: String, summary: PrisonerInPrisonSummary) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/api/offenders/$nomsNumber/prison-timeline",
    responseBody = summary,
  )

fun IntegrationTestBase.prisonAPIMockNotFoundPrisonTimeLineCall(nomsNumber: String) =
  mockUnsuccessfulGetCall(
    url = "/api/offenders/$nomsNumber/prison-timeline",
    responseStatus = 404,
  )

fun IntegrationTestBase.prisonAPIMockServerErrorPrisonTimeLineCall(nomsNumber: String) =
  mockUnsuccessfulGetCall(
    url = "/api/offenders/$nomsNumber/prison-timeline",
    responseStatus = 500,
  )

fun IntegrationTestBase.prisonAPIMockSuccessfulAdjudicationsCall(nomsNumber: String, response: AdjudicationsPage) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/api/offenders/$nomsNumber/adjudications",
    responseBody = response,
  )
