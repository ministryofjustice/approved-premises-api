package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.ReferenceDataCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1CruManagementAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedReasonTransformer

@Service
class Cas1ReferenceDataController(
  private val cas1OutOfServiceBedReasonTransformer: Cas1OutOfServiceBedReasonTransformer,
  private val cas1OutOfServiceBedReasonRepository: Cas1OutOfServiceBedReasonRepository,
  private val cas1CruManagementAreaTransformer: Cas1CruManagementAreaTransformer,
  private val cas1CruManagementAreaRepository: Cas1CruManagementAreaRepository,
) : ReferenceDataCas1Delegate {

  override fun getOutOfServiceBedReasons(): ResponseEntity<List<Cas1OutOfServiceBedReason>> {
    return ResponseEntity.ok(
      cas1OutOfServiceBedReasonRepository.findActive().map {
          reason ->
        cas1OutOfServiceBedReasonTransformer.transformJpaToApi(reason)
      },
    )
  }

  override fun getCruManagementAreas(): ResponseEntity<List<Cas1CruManagementArea>> {
    return ResponseEntity.ok(
      cas1CruManagementAreaRepository.findAll()
        .map { cas1CruManagementAreaTransformer.transformJpaToApi(it) },
    )
  }
}
