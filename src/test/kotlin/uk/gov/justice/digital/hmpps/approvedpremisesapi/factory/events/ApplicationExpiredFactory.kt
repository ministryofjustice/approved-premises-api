package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationExpired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.util.UUID

class ApplicationExpiredFactory : Factory<ApplicationExpired> {
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var previousStatus: Yielded<String> = { ApprovedPremisesApplicationStatus.STARTED.name }
  private var updatedStatus: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }

  override fun produce() = ApplicationExpired(
    applicationId = this.applicationId(),
    previousStatus = this.previousStatus(),
    updatedStatus = this.updatedStatus(),
  )
}
