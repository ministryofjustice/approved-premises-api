package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.WithdrawnBy

class WithdrawnByFactory : Factory<WithdrawnBy> {
  private var withdrawnByStaffMember: Yielded<StaffMember> = { StaffMemberFactory().produce() }
  private var withdrawnByProbationArea: Yielded<ProbationArea> = { ProbationAreaFactory().produce() }

  override fun produce(): WithdrawnBy = WithdrawnBy(
    staffMember = this.withdrawnByStaffMember(),
    probationArea = this.withdrawnByProbationArea(),
  )
}
