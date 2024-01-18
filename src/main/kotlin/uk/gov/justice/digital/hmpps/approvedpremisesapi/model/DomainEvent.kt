package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import java.time.Instant
import java.util.UUID

data class DomainEvent<T> (
  val id: UUID,
  val applicationId: UUID? = null,
  val assessmentId: UUID? = null,
  val bookingId: UUID? = null,
  val crn: String,
  val occurredAt: Instant,
  val data: T,
)
