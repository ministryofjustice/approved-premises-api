package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationStatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatusDetail

@Component("Cas2ApplicationStatusTransformer")
class ApplicationStatusTransformer {
  fun transformModelToApi(status: Cas2PersistedApplicationStatus): Cas2ApplicationStatus {
    return Cas2ApplicationStatus(
      id = status.id,
      name = status.name,
      label = status.label,
      description = status.description,
      statusDetails = status.statusDetails?.map { statusDetail -> transformStatusDetailModelToApi(statusDetail) },
    )
  }

  fun transformStatusDetailModelToApi(statusDetail: Cas2PersistedApplicationStatusDetail): Cas2ApplicationStatusDetail {
    return Cas2ApplicationStatusDetail(
      id = statusDetail.id,
      statusId = statusDetail.statusId,
      name = statusDetail.name,
      label = statusDetail.label,
      description = statusDetail.description,
    )
  }
}
