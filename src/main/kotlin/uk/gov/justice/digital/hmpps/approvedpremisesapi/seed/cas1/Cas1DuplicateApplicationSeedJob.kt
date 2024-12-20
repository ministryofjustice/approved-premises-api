package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.asApiType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationTimelineNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromValidatableActionResult
import java.util.UUID

/**
 * Creates a duplicated version of an existing application, up to the point of submission.
 *
 * There are limitations to this approach:
 *
 * 1. Depending upon the age of the source application, the JSON data stored in 'data' may not be in the format
 * expected by the latest version of the UI, and as such it may be not be possible to submit the application
 * 2. The index offence event number for the source application must still be active, otherwise delius processing of events will fail
 * 3. Unlike when a new application is created, fresh Risk data will not be retrieved from OASys.
 * It is the user's responsibility to ensure this is up-to-date
 *
 * Given this, the seed job should be used judiciously and only in situations where it is not practical/reasonable
 * for the user to recreate the application manually.
 *
 * Using this seed job against a given application should always be tested in pre-prod to ensure that all the pages
 * of the application can be traversed, and the application can be submitted.
 */
@Component
class Cas1DuplicateApplicationSeedJob(
  private val applicationService: ApplicationService,
  private val offenderService: OffenderService,
  private val applicationTimelineNoteService: ApplicationTimelineNoteService,
) : SeedJob<Cas1DuplicateApplicationSeedCsvRow>(
  requiredHeaders = setOf(
    "application_id",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas1DuplicateApplicationSeedCsvRow(
    applicationId = UUID.fromString(columns["application_id"]!!.trim()),
  )

  override fun processRow(row: Cas1DuplicateApplicationSeedCsvRow) {
    val applicationIdToDuplicate = row.applicationId

    log.info("Duplicating application $applicationIdToDuplicate")

    val sourceApplication = applicationService.getApplication(applicationIdToDuplicate)
      ?: error("Could not find application if id $applicationIdToDuplicate")

    if (sourceApplication !is ApprovedPremisesApplicationEntity) {
      error("Application $applicationIdToDuplicate is not cas 1")
    }

    val personInfo =
      when (
        val personInfoResult = offenderService.getPersonInfoResult(
          crn = sourceApplication.crn,
          deliusUsername = null,
          ignoreLaoRestrictions = true,
        )
      ) {
        is PersonInfoResult.NotFound, is PersonInfoResult.Unknown -> throw NotFoundProblem(
          personInfoResult.crn,
          "Offender",
        )

        is PersonInfoResult.Success.Restricted -> throw ForbiddenProblem()
        is PersonInfoResult.Success.Full -> personInfoResult
      }

    val createdByUser = sourceApplication.createdByUser

    val newApplicationEntity = extractEntityFromValidatableActionResult(
      applicationService.createApprovedPremisesApplication(
        offenderDetails = personInfo.offenderDetailSummary,
        user = createdByUser,
        convictionId = sourceApplication.convictionId,
        deliusEventNumber = sourceApplication.eventNumber,
        offenceId = sourceApplication.offenceId,
        createWithRisks = true,
      ),
    )

    applicationService.updateApprovedPremisesApplication(
      applicationId = newApplicationEntity.id,
      updateFields = ApplicationService.Cas1ApplicationUpdateFields(
        isWomensApplication = sourceApplication.isWomensApplication,
        isPipeApplication = null,
        isEmergencyApplication = sourceApplication.isEmergencyApplication,
        isEsapApplication = null,
        apType = sourceApplication.apType.asApiType(),
        releaseType = sourceApplication.releaseType,
        arrivalDate = sourceApplication.arrivalDate?.toLocalDate(),
        data = sourceApplication.data!!,
        isInapplicable = sourceApplication.isInapplicable,
        noticeType = sourceApplication.noticeType,
      ),
      createdByUser,
    )

    applicationTimelineNoteService.saveApplicationTimelineNote(
      applicationId = newApplicationEntity.id,
      note = "Application automatically created by Application Support by duplicating existing application ${sourceApplication.id}",
      user = null,
    )

    log.info("Have duplicated application $applicationIdToDuplicate as ${newApplicationEntity.id}")
  }
}

data class Cas1DuplicateApplicationSeedCsvRow(
  val applicationId: UUID,
)
