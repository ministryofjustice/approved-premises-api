package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.trimToNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.time.LocalDate
import java.util.UUID

@Component
class Cas1OutOfServiceBedSeedJob(
  private val cas1OutOfServiceBedService: Cas1OutOfServiceBedService,
  private val premisesService: PremisesService,
) : SeedJob<Cas1OutOfServiceBedSeedCsvRow>(
  requiredHeaders = setOf(
    "premisesId",
    "bedId",
    "startDate",
    "endDate",
    "reasonId",
    "isCancelled",
  ),
) {
  private val rowKeys = mutableMapOf<Cas1OutOfServiceBedSeedCsvRowKey, UUID>()

  override fun deserializeRow(columns: Map<String, String>) = Cas1OutOfServiceBedSeedCsvRow(
    key = Cas1OutOfServiceBedSeedCsvRowKey(
      premisesId = UUID.fromString(columns["premisesId"]!!.trim()),
      bedId = UUID.fromString(columns["bedId"]!!.trim()),
    ),
    startDate = LocalDate.parse(columns["startDate"]!!.trim()),
    endDate = LocalDate.parse(columns["endDate"]!!.trim()),
    reasonId = UUID.fromString(columns["reasonId"]!!.trim()),
    referenceNumber = columns["referenceNumber"].trimToNull(),
    notes = columns["notes"].trimToNull(),
    isCancelled = columns["isCancelled"]!!.trim().equals("true", ignoreCase = true),
    cancellationNotes = columns["cancellationNotes"].trimToNull(),
  )

  override fun processRow(row: Cas1OutOfServiceBedSeedCsvRow) {
    when (val outOfServiceBedId = rowKeys[row.key]) {
      null -> createOutOfServiceBed(row)
      else -> updateOutOfServiceBed(outOfServiceBedId, row)
    }
  }

  private fun createOutOfServiceBed(row: Cas1OutOfServiceBedSeedCsvRow) {
    val premises = premisesService.getPremises(row.key.premisesId) as ApprovedPremisesEntity?
      ?: throw SeedException("No Approved Premises with ID ${row.key.premisesId} exists.")

    val creationResult = cas1OutOfServiceBedService.createOutOfServiceBed(
      premises,
      row.startDate,
      row.endDate,
      row.reasonId,
      row.referenceNumber,
      row.notes,
      row.key.bedId,
      createdBy = null,
    )

    val outOfServiceBed = extractEntityFromCasResult(creationResult)

    if (row.isCancelled) {
      val cancellationResult = cas1OutOfServiceBedService.cancelOutOfServiceBed(outOfServiceBed, row.cancellationNotes)

      ensureEntityFromCasResultIsSuccess(cancellationResult)
    }

    rowKeys[row.key] = outOfServiceBed.id
  }

  private fun updateOutOfServiceBed(outOfServiceBedId: UUID, row: Cas1OutOfServiceBedSeedCsvRow) {
    val result = cas1OutOfServiceBedService.updateOutOfServiceBed(
      outOfServiceBedId,
      row.startDate,
      row.endDate,
      row.reasonId,
      row.referenceNumber,
      row.notes,
      createdBy = null,
    )

    ensureEntityFromCasResultIsSuccess(result)
  }
}

data class Cas1OutOfServiceBedSeedCsvRowKey(
  val premisesId: UUID,
  val bedId: UUID,
)

data class Cas1OutOfServiceBedSeedCsvRow(
  val key: Cas1OutOfServiceBedSeedCsvRowKey,
  val startDate: LocalDate,
  val endDate: LocalDate,
  val reasonId: UUID,
  val referenceNumber: String?,
  val notes: String?,
  val isCancelled: Boolean,
  val cancellationNotes: String?,
)
