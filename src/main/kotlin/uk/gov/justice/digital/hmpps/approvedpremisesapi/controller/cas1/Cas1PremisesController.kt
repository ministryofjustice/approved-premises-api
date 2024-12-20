package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.PremisesCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseCapacity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseDaySummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesBasicSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingDaySummarySortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PremiseCapacitySummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.time.LocalDate
import java.util.UUID

@Service
class Cas1PremisesController(
  val userAccessService: UserAccessService,
  val cas1PremisesService: Cas1PremisesService,
  val cas1PremisesTransformer: Cas1PremisesTransformer,
  val cas1PremiseCapacityTransformer: Cas1PremiseCapacitySummaryTransformer,
) : PremisesCas1Delegate {

  override fun getPremisesById(premisesId: UUID): ResponseEntity<Cas1PremisesSummary> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_PREMISES_VIEW)

    return ResponseEntity
      .ok()
      .body(
        cas1PremisesTransformer.toPremiseSummary(
          extractEntityFromCasResult(cas1PremisesService.getPremisesSummary(premisesId)),
        ),
      )
  }

  override fun getPremisesSummaries(
    gender: Cas1ApprovedPremisesGender?,
    apAreaId: UUID?,
  ): ResponseEntity<List<Cas1PremisesBasicSummary>> {
    return ResponseEntity
      .ok()
      .body(
        cas1PremisesService.getPremises(
          gender = when (gender) {
            Cas1ApprovedPremisesGender.man -> ApprovedPremisesGender.MAN
            Cas1ApprovedPremisesGender.woman -> ApprovedPremisesGender.WOMAN
            null -> null
          },
          apAreaId,
        ).map {
          cas1PremisesTransformer.toPremiseBasicSummary(it)
        },
      )
  }

  override fun getCapacity(
    premisesId: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    excludeSpaceBookingId: UUID?,
  ): ResponseEntity<Cas1PremiseCapacity> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_PREMISES_VIEW)

    val premiseSummaryInfo = cas1PremisesService.getPremisesSummary(premisesId)
    val premiseCapacity = cas1PremisesService.getPremiseCapacity(
      premisesId = premisesId,
      startDate = startDate,
      endDate = endDate,
      excludeSpaceBookingId = excludeSpaceBookingId,
    )

    return ResponseEntity.ok().body(
      cas1PremiseCapacityTransformer.toCas1PremiseCapacitySummary(
        premiseSummaryInfo = extractEntityFromCasResult(premiseSummaryInfo),
        premiseCapacity = extractEntityFromCasResult(premiseCapacity),
      ),
    )
  }

  override fun getDaySummary(
    premisesId: UUID,
    date: LocalDate,
    bookingsCriteriaFilter: List<Cas1SpaceBookingCharacteristic>?,
    bookingsSortDirection: SortDirection?,
    bookingsSortBy: Cas1SpaceBookingDaySummarySortField?,
  ): ResponseEntity<Cas1PremiseDaySummary> {
    return super.getDaySummary(premisesId, date, bookingsCriteriaFilter, bookingsSortDirection, bookingsSortBy)
  }
}
