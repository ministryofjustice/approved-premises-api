package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.OutOfServiceBedsCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewOutOfServiceBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewOutOfServiceBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Temporality
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas1OutOfServiceBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedCancellationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.time.LocalDate
import java.util.UUID

@Service
class Cas1OutOfServiceBedsController(
  private val userAccessService: UserAccessService,
  private val premisesService: PremisesService,
  private val outOfServiceBedService: Cas1OutOfServiceBedService,
  private val outOfServiceBedTransformer: Cas1OutOfServiceBedTransformer,
  private val outOfServiceBedCancellationTransformer: Cas1OutOfServiceBedCancellationTransformer,
) : OutOfServiceBedsCas1Delegate {
  override fun getOutOfServiceBeds(
    temporality: List<Temporality>?,
    premisesId: UUID?,
    apAreaId: UUID?,
    sortDirection: SortDirection?,
    sortBy: Cas1OutOfServiceBedSortField?,
    page: Int?,
    perPage: Int?,
  ): ResponseEntity<List<Cas1OutOfServiceBed>> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_VIEW_OUT_OF_SERVICE_BEDS)

    val (outOfServiceBeds, pageMetadata) = outOfServiceBedService.getOutOfServiceBeds(
      temporality?.toSet() ?: setOf(Temporality.current, Temporality.future),
      premisesId,
      apAreaId,
      PageCriteria(
        sortBy ?: Cas1OutOfServiceBedSortField.startDate,
        sortDirection ?: SortDirection.asc,
        page,
        perPage,
      ),
    )

    return ResponseEntity
      .ok()
      .headers(pageMetadata?.toHeaders())
      .body(outOfServiceBeds.map(outOfServiceBedTransformer::transformJpaToApi))
  }

  override fun getOutOfServiceBedsForPremises(premisesId: UUID): ResponseEntity<List<Cas1OutOfServiceBed>> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_VIEW_OUT_OF_SERVICE_BEDS)

    tryGetApprovedPremises(premisesId)

    val outOfServiceBeds = outOfServiceBedService.getActiveOutOfServiceBedsForPremisesId(premisesId)

    return ResponseEntity.ok(outOfServiceBeds.map(outOfServiceBedTransformer::transformJpaToApi))
  }

  override fun cancelOutOfServiceBed(
    premisesId: UUID,
    outOfServiceBedId: UUID,
    body: Cas1NewOutOfServiceBedCancellation,
  ): ResponseEntity<Cas1OutOfServiceBedCancellation> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_OUT_OF_SERVICE_BED_CREATE)

    val premises = tryGetApprovedPremises(premisesId)

    val outOfServiceBed = premises
      .outOfServiceBeds
      .firstOrNull { it.id == outOfServiceBedId }
      ?: throw NotFoundProblem(outOfServiceBedId, "OutOfServiceBed")

    val cancelOutOfServiceBedResult = outOfServiceBedService.cancelOutOfServiceBed(
      outOfServiceBed = outOfServiceBed,
      notes = body.notes,
    )

    return ResponseEntity.ok(
      outOfServiceBedCancellationTransformer.transformJpaToApi(
        extractEntityFromCasResult(cancelOutOfServiceBedResult),
      ),
    )
  }

  override fun getOutOfServiceBed(
    premisesId: UUID,
    outOfServiceBedId: UUID,
  ): ResponseEntity<Cas1OutOfServiceBed> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_VIEW_OUT_OF_SERVICE_BEDS)

    val premises = tryGetApprovedPremises(premisesId)

    val outOfServiceBed = premises.outOfServiceBeds.firstOrNull { it.id == outOfServiceBedId }
      ?: throw NotFoundProblem(outOfServiceBedId, "OutOfServiceBed")

    return ResponseEntity.ok(outOfServiceBedTransformer.transformJpaToApi(outOfServiceBed))
  }

  override fun updateOutOfServiceBed(
    premisesId: UUID,
    outOfServiceBedId: UUID,
    body: UpdateCas1OutOfServiceBed,
  ): ResponseEntity<Cas1OutOfServiceBed> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_OUT_OF_SERVICE_BED_CREATE)

    val premises = tryGetApprovedPremises(premisesId)
    val outOfServiceBed = premises.outOfServiceBeds.firstOrNull { it.id == outOfServiceBedId } ?: throw NotFoundProblem(outOfServiceBedId, "OutOfServiceBed")

    throwIfOutOfServiceBedDatesConflict(body.startDate, body.endDate, outOfServiceBedId, outOfServiceBed.bed.id)

    val updateOutOfServiceBedResult = outOfServiceBedService.updateOutOfServiceBed(
      outOfServiceBedId,
      body.startDate,
      body.endDate,
      body.reason,
      body.referenceNumber,
      body.notes,
    )

    return ResponseEntity.ok(
      outOfServiceBedTransformer.transformJpaToApi(
        extractEntityFromCasResult(updateOutOfServiceBedResult),
      ),
    )
  }

  override fun createOutOfServiceBed(
    premisesId: UUID,
    body: Cas1NewOutOfServiceBed,
  ): ResponseEntity<Cas1OutOfServiceBed> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_OUT_OF_SERVICE_BED_CREATE)

    val premises = tryGetApprovedPremises(premisesId)

    throwIfOutOfServiceBedDatesConflict(body.startDate, body.endDate, null, body.bedId)

    val result = outOfServiceBedService.createOutOfServiceBed(
      premises = premises,
      startDate = body.startDate,
      endDate = body.endDate,
      reasonId = body.reason,
      referenceNumber = body.referenceNumber,
      notes = body.notes,
      bedId = body.bedId,
    )

    val outOfServiceBed = extractEntityFromCasResult(result)

    return ResponseEntity.ok(outOfServiceBedTransformer.transformJpaToApi(outOfServiceBed))
  }

  private fun tryGetApprovedPremises(premisesId: UUID): ApprovedPremisesEntity =
    premisesService.getPremises(premisesId) as? ApprovedPremisesEntity ?: throw NotFoundProblem(premisesId, "Premises")

  private val ApprovedPremisesEntity.outOfServiceBeds: List<Cas1OutOfServiceBedEntity>
    get() = outOfServiceBedService.getActiveOutOfServiceBedsForPremisesId(this.id)

  private fun throwIfOutOfServiceBedDatesConflict(
    startDate: LocalDate,
    endDate: LocalDate,
    thisEntityId: UUID?,
    bedId: UUID,
  ) {
    outOfServiceBedService.getOutOfServiceBedWithConflictingDates(startDate, endDate, thisEntityId, bedId)?.let {
      throw ConflictProblem(it.id, "An out-of-service bed already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates")
    }
  }
}
