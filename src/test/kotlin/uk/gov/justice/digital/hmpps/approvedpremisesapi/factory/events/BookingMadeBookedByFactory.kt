package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeBookedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cru
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember

class BookingMadeBookedByFactory : Factory<BookingMadeBookedBy> {
  private var staffMember: Yielded<StaffMember> = { StaffMemberFactory().produce() }
  private var cru: Yielded<Cru> = { CruFactory().produce() }

  fun withStaffMember(staffMember: StaffMember) = apply {
    this.staffMember = { staffMember }
  }

  fun withCru(cru: Cru) = apply {
    this.cru = { cru }
  }

  override fun produce() = BookingMadeBookedBy(
    staffMember = this.staffMember(),
    cru = this.cru(),
  )
}
