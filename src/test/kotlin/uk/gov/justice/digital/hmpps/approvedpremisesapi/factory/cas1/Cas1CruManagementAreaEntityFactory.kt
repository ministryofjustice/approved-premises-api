package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class Cas1CruManagementAreaEntityFactory : Factory<Cas1CruManagementAreaEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var emailAddress: Yielded<String?> = { randomStringUpperCase(10) }
  private var notifyReplyToEmailId: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var assessmentAutoAllocationUsername: Yielded<String?> = { null }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withEmailAddress(emailAddress: String?) = apply {
    this.emailAddress = { emailAddress }
  }

  fun withNotifyReplyToEmailId(notifyReplyToEmailId: String) = apply {
    this.notifyReplyToEmailId = { notifyReplyToEmailId }
  }

  fun withAssessmentAutoAllocationUsername(assessmentAutoAllocationUsername: String?) = apply {
    this.assessmentAutoAllocationUsername = { assessmentAutoAllocationUsername }
  }

  override fun produce() = Cas1CruManagementAreaEntity(
    id = this.id(),
    name = this.name(),
    emailAddress = this.emailAddress(),
    notifyReplyToEmailId = this.notifyReplyToEmailId(),
    assessmentAutoAllocationUsername = this.assessmentAutoAllocationUsername(),
  )
}
