package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockCaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulCaseDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockUserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonAPIMockNotFoundInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonAPIMockServerErrorInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonAPIMockSuccessfulInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseDetail

fun IntegrationTestBase.givenAnOffender(
  offenderDetailsConfigBlock: (OffenderDetailsSummaryFactory.() -> Unit)? = null,
  inmateDetailsConfigBlock: (InmateDetailFactory.() -> Unit)? = null,
  mockServerErrorForPrisonApi: Boolean = false,
  mockNotFoundErrorForPrisonApi: Boolean = false,
): Pair<OffenderDetailSummary, InmateDetail> {
  val inmateDetailsFactory = InmateDetailFactory()
  if (inmateDetailsConfigBlock != null) {
    inmateDetailsConfigBlock(inmateDetailsFactory)
  }

  val inmateDetails = inmateDetailsFactory.produce()

  val offenderDetailsFactory = OffenderDetailsSummaryFactory()
    .withNomsNumber(inmateDetails.offenderNo)

  if (offenderDetailsConfigBlock != null) {
    offenderDetailsConfigBlock(offenderDetailsFactory)
  }

  val offenderDetails = offenderDetailsFactory.produce()

  val caseDetail = offenderDetails.asCaseDetail()

  apDeliusContextMockSuccessfulCaseDetailCall(offenderDetails.otherIds.crn, caseDetail)

  apDeliusContextMockCaseSummary(caseDetail.case)
  apDeliusContextMockUserAccess(
    CaseAccessFactory()
      .withCrn(offenderDetails.otherIds.crn)
      .withUserExcluded(offenderDetails.currentExclusion)
      .withUserRestricted(offenderDetails.currentRestriction)
      .produce(),
  )

  when (mockServerErrorForPrisonApi) {
    true -> prisonAPIMockServerErrorInmateDetailsCall(inmateDetails.offenderNo)
    false -> prisonAPIMockSuccessfulInmateDetailsCall(inmateDetails)
  }

  when (mockNotFoundErrorForPrisonApi) {
    true -> prisonAPIMockNotFoundInmateDetailsCall(inmateDetails.offenderNo)
    false -> {}
  }

  loadPreemptiveCacheForInmateDetails(inmateDetails.offenderNo)

  return Pair(offenderDetails, inmateDetails)
}

@SuppressWarnings("LongParameterList")
fun IntegrationTestBase.givenAnOffender(
  offenderDetailsConfigBlock: (OffenderDetailsSummaryFactory.() -> Unit)? = null,
  inmateDetailsConfigBlock: (InmateDetailFactory.() -> Unit)? = null,
  mockServerErrorForPrisonApi: Boolean = false,
  mockNotFoundErrorForPrisonApi: Boolean = false,
  block: ((offenderDetails: OffenderDetailSummary, inmateDetails: InmateDetail) -> Unit)? = null,
): Pair<OffenderDetailSummary, InmateDetail> {
  val (offenderDetails, inmateDetails) = givenAnOffender(
    offenderDetailsConfigBlock,
    inmateDetailsConfigBlock,
    mockServerErrorForPrisonApi,
    mockNotFoundErrorForPrisonApi,
  )

  block?.invoke(offenderDetails, inmateDetails)

  return Pair(offenderDetails, inmateDetails)
}

fun IntegrationTestBase.givenSomeOffenders(
  offenderDetailsConfigBlock: (OffenderDetailsSummaryFactory.() -> Unit)? = null,
  inmateDetailsConfigBlock: (InmateDetailFactory.() -> Unit)? = null,
  block: (offenderSequence: Sequence<Pair<OffenderDetailSummary, InmateDetail>>) -> Unit,
) {
  val offenderSequence = generateSequence {
    val inmateDetailsFactory = InmateDetailFactory()
    if (inmateDetailsConfigBlock != null) {
      inmateDetailsConfigBlock(inmateDetailsFactory)
    }

    val inmateDetails = inmateDetailsFactory.produce()

    val offenderDetailsFactory = OffenderDetailsSummaryFactory()
      .withNomsNumber(inmateDetails.offenderNo)

    if (offenderDetailsConfigBlock != null) {
      offenderDetailsConfigBlock(offenderDetailsFactory)
    }

    val offenderDetails = offenderDetailsFactory.produce()
    val caseDetail = offenderDetails.asCaseDetail()

    apDeliusContextMockSuccessfulCaseDetailCall(offenderDetails.otherIds.crn, caseDetail)

    apDeliusContextMockCaseSummary(caseDetail.case)
    apDeliusContextMockUserAccess(
      CaseAccessFactory()
        .withCrn(offenderDetails.otherIds.crn)
        .withUserExcluded(offenderDetails.currentExclusion)
        .withUserRestricted(offenderDetails.currentRestriction)
        .produce(),
    )

    prisonAPIMockSuccessfulInmateDetailsCall(inmateDetails)

    loadPreemptiveCacheForInmateDetails(inmateDetails.offenderNo)

    Pair(offenderDetails, inmateDetails)
  }

  block(offenderSequence)
}
