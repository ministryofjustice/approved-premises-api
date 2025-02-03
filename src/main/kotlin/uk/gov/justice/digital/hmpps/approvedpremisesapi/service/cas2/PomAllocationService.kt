package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PrisonerLocationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PrisonerLocationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.HmppsDomainEvent
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@ConditionalOnProperty(prefix = "feature-flags", name = ["cas2-sqs-listener-enabled"], havingValue = "true")
@Service
class PomAllocationService(
  private val prisonerLocationRepository: PrisonerLocationRepository,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun handlePomAllocationChangedMessage(message: HmppsDomainEvent) {
    val nomsNumber = message.personReference.findNomsNumber().toString()
    val startDate = message.occurredAt.toInstant().atOffset(ZoneOffset.UTC)

    updateLatestPrisonerLocationEndDate(nomsNumber, startDate)

    prisonerLocationRepository.save(
      PrisonerLocationEntity(
        id = UUID.randomUUID(),
        nomsNumber = nomsNumber,
        prisonCode = message.prisonId.toString(),
        pomId = message.staffCode,
        startDate = startDate,
        endDate = null,
      ),
    )
  }

  private fun updateLatestPrisonerLocationEndDate(nomsNumber: String, endDate: OffsetDateTime) {
    val prevPrisonerLocations = prisonerLocationRepository.findLatestByNomsNumber(nomsNumber)

    if (prevPrisonerLocations.isEmpty()) {
      log.error("No prisoner location for noms number: $nomsNumber")
    }

    prisonerLocationRepository.updateEndDate(prevPrisonerLocations[0].id, endDate)
  }
}
