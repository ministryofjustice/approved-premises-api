package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationTimelineNoteService
import java.util.UUID

@Component
class Cas1UpdateNomsNumberSeedJob(
  private val applicationRepository: ApplicationRepository,
  private val applicationTimelineNoteService: ApplicationTimelineNoteService,
  private val bookingRepository: BookingRepository,
) : SeedJob<UpdateNomsNumberSeedRow>(
  requiredHeaders = setOf(
    "crn",
    "oldNomsNumber",
    "newNomsNumber",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = UpdateNomsNumberSeedRow(
    crn = columns["crn"]!!.trim(),
    oldNomsNumber = columns["oldNomsNumber"]!!.trim(),
    newNomsNumber = columns["newNomsNumber"]!!.trim(),
  )

  override fun processRow(row: UpdateNomsNumberSeedRow) {
    val crn = row.crn
    val oldNomsNumber = row.oldNomsNumber
    val newNomsNumber = row.newNomsNumber
    log.info("Updating NOMS number for all applications with CRN $crn and NOMS $oldNomsNumber to $newNomsNumber")

    val applications = applicationRepository.findByCrnAndNoms(crn, oldNomsNumber)

    applications.forEach { application ->
      val applicationId = UUID.fromString(application.getId())
      val previousNomsNumber = application.getNomsNumber()

      log.info("Updating NOMS number on application $applicationId from $previousNomsNumber to $newNomsNumber")
      applicationRepository.updateNomsNumber(applicationId, newNomsNumber)
      applicationTimelineNoteService.saveApplicationTimelineNote(
        applicationId = applicationId,
        note = "NOMS Number for application updated from '$previousNomsNumber' to '$newNomsNumber' by Application Support",
        user = null,
      )
    }
    log.info("Have updated ${applications.size} applications")

    log.info("Updating NOMS number for all bookings with CRN $crn and NOMS $oldNomsNumber to $newNomsNumber")
    val bookingUpdatedCount = bookingRepository.updateNomsByCrn(crn, oldNomsNumber, newNomsNumber)
    log.info("Have updated $bookingUpdatedCount bookings")
  }
}

data class UpdateNomsNumberSeedRow(
  val crn: String,
  val oldNomsNumber: String,
  val newNomsNumber: String,
)
