package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas3

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonDepartedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger

@Component
class Cas3UpdateDomainEventTypeForPersonDepartureUpdatedJob(
  private val domainEventRepository: DomainEventRepository,
  private val objectMapper: ObjectMapper,
  private val migrationLogger: MigrationLogger,
) : MigrationJob() {
  override val shouldRunInTransaction = true

  @SuppressWarnings("MagicNumber", "TooGenericExceptionCaught", "NestedBlockDepth")
  override fun process(pageSize: Int) {
    val domainEvents = domainEventRepository.findByType(DomainEventType.CAS3_PERSON_DEPARTURE_UPDATED)
    domainEvents.forEach {
      migrationLogger.info("Updating person departure updated domain event. Event id ${it.id}")
      try {
        val domainEvent = domainEventRepository.findById(it.id)
        val domainEventData = domainEvent.get().data
        if (!domainEventData.contains("accommodation.cas3.person.departed.updated")) {
          val departureEvent = objectMapper.readValue<CAS3PersonDepartedEvent>(domainEventData)
          if (departureEvent.eventType == EventType.personDeparted) {
            val updatedDepartureEvent = departureEvent.copy(
              eventType = EventType.personDepartureUpdated,
            )
            domainEventRepository.updateData(it.id, objectMapper.writeValueAsString(updatedDepartureEvent))
          }
        }
      } catch (exception: Exception) {
        migrationLogger.error("Unable to update person departure updated domain event ${it.id}", exception)
      }
    }
  }
}
