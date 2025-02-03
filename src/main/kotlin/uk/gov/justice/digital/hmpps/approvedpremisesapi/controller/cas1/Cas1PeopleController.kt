package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.PeopleCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PersonalTimeline
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LimitedAccessStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.BoxedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1ApplicationTimelineModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PersonalTimelineTransformer

@Service
class Cas1PeopleController(
  private val userService: UserService,
  private val offenderService: OffenderService,
  private val applicationService: ApplicationService,
  private val cas1PersonalTimelineTransformer: Cas1PersonalTimelineTransformer,
  private val cas1TimelineService: Cas1TimelineService,
) : PeopleCas1Delegate {

  override fun getPeopleApplicationsTimeline(crn: String): ResponseEntity<Cas1PersonalTimeline> {
    val user = userService.getUserForRequest()
    val personInfo = offenderService.getPersonInfoResult(crn, user.cas1LimitedAccessStrategy())
    return ResponseEntity.ok(transformPersonInfo(personInfo, crn))
  }

  private fun transformPersonInfo(personInfoResult: PersonInfoResult, crn: String): Cas1PersonalTimeline = when (personInfoResult) {
    is PersonInfoResult.NotFound -> throw NotFoundProblem(crn, "Offender")
    is PersonInfoResult.Unknown -> throw personInfoResult.throwable ?: RuntimeException("Could not retrieve person info for CRN: $crn")
    is PersonInfoResult.Success.Full -> buildPersonInfoWithTimeline(personInfoResult, crn)
    is PersonInfoResult.Success.Restricted -> buildPersonInfoWithoutTimeline(personInfoResult)
  }

  private fun buildPersonInfoWithoutTimeline(personInfo: PersonInfoResult.Success.Restricted): Cas1PersonalTimeline =
    cas1PersonalTimelineTransformer.transformApplicationTimelineModels(personInfo, emptyList())

  private fun buildPersonInfoWithTimeline(personInfo: PersonInfoResult.Success.Full, crn: String): Cas1PersonalTimeline {
    val regularApplications = getRegularApplications(crn)
    val offlineApplications = getOfflineApplications(crn)
    val combinedApplications = regularApplications + offlineApplications

    val applicationTimelineModels = combinedApplications.map { application ->
      val applicationId = application.map(
        ApprovedPremisesApplicationEntity::id,
        OfflineApplicationEntity::id,
      )
      val cas1timelineEvents = cas1TimelineService.getApplicationTimelineEvents(applicationId)
      Cas1ApplicationTimelineModel(application, cas1timelineEvents)
    }

    return cas1PersonalTimelineTransformer.transformApplicationTimelineModels(personInfo, applicationTimelineModels)
  }

  private fun getRegularApplications(crn: String) = applicationService
    .getApplicationsForCrn(crn, ServiceName.approvedPremises)
    .map { BoxedApplication.of(it as ApprovedPremisesApplicationEntity) }

  private fun getOfflineApplications(crn: String) = applicationService
    .getOfflineApplicationsForCrn(crn, ServiceName.approvedPremises)
    .map { BoxedApplication.of(it) }
}
