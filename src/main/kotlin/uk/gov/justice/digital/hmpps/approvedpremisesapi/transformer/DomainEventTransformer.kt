package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.WithdrawnBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffDetail

@Component
class DomainEventTransformer(private val apDeliusContextApiClient: ApDeliusContextApiClient) {
  fun toWithdrawnBy(user: UserEntity) =
    when (val result = apDeliusContextApiClient.getStaffDetail(user.deliusUsername)) {
      is ClientResult.Success -> toWithdrawnBy(result.body)
      is ClientResult.Failure -> result.throwException()
    }

  fun toWithdrawnBy(staffDetails: StaffDetail): WithdrawnBy {
    val staffMember = staffDetails.toStaffMember()
    val probationArea = toProbationArea(staffDetails)
    return WithdrawnBy(staffMember, probationArea)
  }

  fun toProbationArea(staffDetails: StaffDetail): ProbationArea {
    return ProbationArea(
      code = staffDetails.probationArea.code,
      name = staffDetails.probationArea.description,
    )
  }

  fun toStaffMember(user: UserEntity) =
    when (val result = apDeliusContextApiClient.getStaffDetail(user.deliusUsername)) {
      is ClientResult.Success -> result.body.toStaffMember()
      is ClientResult.Failure -> result.throwException()
    }
}
