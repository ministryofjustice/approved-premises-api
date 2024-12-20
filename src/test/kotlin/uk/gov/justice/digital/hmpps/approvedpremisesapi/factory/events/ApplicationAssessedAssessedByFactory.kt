package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedAssessedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cru
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember

class ApplicationAssessedAssessedByFactory : Factory<ApplicationAssessedAssessedBy> {
  private var staffMember: Yielded<StaffMember> = { StaffMemberFactory().produce() }
  private var probationArea: Yielded<ProbationArea> = { ProbationAreaFactory().produce() }
  private var cru: Yielded<Cru> = { CruFactory().produce() }

  fun withStaffMember(staffMember: StaffMember) = apply {
    this.staffMember = { staffMember }
  }

  fun withProbationArea(probationArea: ProbationArea) = apply {
    this.probationArea = { probationArea }
  }

  fun withCru(cru: Cru) = apply {
    this.cru = { cru }
  }

  override fun produce() = ApplicationAssessedAssessedBy(
    staffMember = this.staffMember(),
    probationArea = this.probationArea(),
    cru = this.cru(),
  )
}
