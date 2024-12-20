package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationTimelineNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredBySeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.javaConstantNameToSentence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiFormat
import java.util.UUID

@Component
class Cas1WithdrawPlacementRequestSeedJob(
  private val placementRequestService: PlacementRequestService,
  private val applicationTimelineNoteService: ApplicationTimelineNoteService,
) : SeedJob<Cas1WithdrawPlacementRequestSeedSeedCsvRow>(
  requiredHeaders = setOf(
    "placement_request_id",
    "withdrawal_reason",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas1WithdrawPlacementRequestSeedSeedCsvRow(
    placementRequestId = UUID.fromString(columns["placement_request_id"]!!.trim()),
    withdrawalReason = PlacementRequestWithdrawalReason.valueOf(columns["withdrawal_reason"]!!.trim()),
  )

  override fun processRow(row: Cas1WithdrawPlacementRequestSeedSeedCsvRow) {
    val id = row.placementRequestId
    val withdrawalReason = row.withdrawalReason

    val placementRequest = placementRequestService.getPlacementRequestOrNull(id)!!

    if (placementRequest.placementApplication == null) {
      error("Withdraw placement request seed job should only be used for placement_requests linked to a placement_application")
    }

    val result = placementRequestService.withdrawPlacementRequest(
      placementRequestId = id,
      userProvidedReason = row.withdrawalReason,
      withdrawalContext = WithdrawalContext(
        withdrawalTriggeredBy = WithdrawalTriggeredBySeedJob,
        triggeringEntityType = WithdrawableEntityType.PlacementRequest,
        triggeringEntityId = row.placementRequestId,
      ),
    )

    extractEntityFromCasResult(result).placementRequest

    val reasonDescription = withdrawalReason.name.javaConstantNameToSentence()
    applicationTimelineNoteService.saveApplicationTimelineNote(
      applicationId = placementRequest.application.id,
      note = "The Match Request for arrival date ${placementRequest.expectedArrival.toUiFormat()} has " +
        "been withdrawn by Application Support as the CRU has indicated that it is no longer required. " +
        "Withdrawal reason is '$reasonDescription'",
      user = null,
    )

    log.info("Have withdrawn placement request $id")
  }
}

data class Cas1WithdrawPlacementRequestSeedSeedCsvRow(
  val placementRequestId: UUID,
  val withdrawalReason: PlacementRequestWithdrawalReason,
)
