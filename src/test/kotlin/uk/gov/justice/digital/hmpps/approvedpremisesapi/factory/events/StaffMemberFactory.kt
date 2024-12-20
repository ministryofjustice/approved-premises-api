package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

class StaffMemberFactory : Factory<StaffMember> {
  private var staffCode: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var forenames: Yielded<String> = { randomStringUpperCase(6) }
  private var surname: Yielded<String> = { randomStringUpperCase(6) }
  private var username: Yielded<String> = { randomStringUpperCase(8) }

  fun withStaffCode(staffCode: String) = apply {
    this.staffCode = { staffCode }
  }

  fun withForenames(forenames: String) = apply {
    this.forenames = { forenames }
  }

  fun withSurname(surname: String) = apply {
    this.surname = { surname }
  }

  fun withUsername(username: String) = apply {
    this.username = { username }
  }

  override fun produce(): StaffMember = StaffMember(
    staffCode = this.staffCode(),
    forenames = this.forenames(),
    surname = this.surname(),
    username = this.username(),
  )
}
