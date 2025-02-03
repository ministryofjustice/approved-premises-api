package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.ApplicationsCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEvent
import java.util.UUID

@Service
class Cas1ApplicationsController(
  private val cas1TimelineService: Cas1TimelineService,
) : ApplicationsCas1Delegate {

  override fun getApplicationTimeLine(
    applicationId: UUID,
  ): ResponseEntity<List<Cas1TimelineEvent>> {
    val cas1timelineEvents = cas1TimelineService.getApplicationTimelineEvents(applicationId)
    return ResponseEntity.ok(cas1timelineEvents)
  }
}
