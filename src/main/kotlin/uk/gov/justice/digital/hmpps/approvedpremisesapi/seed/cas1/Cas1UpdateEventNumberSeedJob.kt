package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedColumns
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationTimelineNoteService
import java.util.UUID

/**
 * This seed job can be used to update the event number and associated offence id/conviction id for a given
 * application. It will also update the event number in any Application Assessed, Application Submitted or
 * Booking Made domain events (these are the only domain events for which probation-integration needs a valid
 * deliusEventNumber value)
 *
 * This job has been created to allow us to recover from situations where the event number associated with
 * an application do not exist in delius (i.e. they have been deleted in delius). In this case domain event
 * processing in probation-integration will fail. Once the event number has been updated for an application
 * and its associated domain event's, those domain events can be replayed using the Cas1DomainEventReplaySeedJob,
 * and they should then be processed by probation-integration without issue.
 *
 * Because the seed job reads the complete domain event json into objects, updates the event number and then
 * rewrites the json from these objects, care needs to be taken to ensure that there is no data loss caused by
 * changes to the Domain Event's schema since the JSON was originally written to the Database (although historically,
 * the Domain Event JSON Schema is very stable)
 */
@Component
class Cas1UpdateEventNumberSeedJob(
  private val applicationTimelineNoteService: ApplicationTimelineNoteService,
  private val applicationRepository: ApplicationRepository,
  private val domainEventRepository: DomainEventRepository,
  private val objectMapper: ObjectMapper,
  private val spaceBookingRepository: Cas1SpaceBookingRepository,
) : SeedJob<Cas1UpdateEventNumberSeedJobCsvRow>(
  requiredHeaders = setOf(
    "application_id",
    "event_number",
    "offence_id",
    "conviction_id",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>): Cas1UpdateEventNumberSeedJobCsvRow {
    val seedColumns = SeedColumns(columns)
    return Cas1UpdateEventNumberSeedJobCsvRow(
      applicationId = seedColumns.getUuidOrNull("application_id")!!,
      eventNumber = seedColumns.getIntOrNull("event_number")!!,
      offenceId = seedColumns.getStringOrNull("offence_id")!!,
      convictionId = seedColumns.getLongOrNull("conviction_id")!!,
    )
  }

  override fun processRow(row: Cas1UpdateEventNumberSeedJobCsvRow) {
    val applicationId = row.applicationId
    val updatedEventNumber = row.eventNumber
    val updatedOffenceId = row.offenceId
    val updatedConvictionId = row.convictionId

    log.info("Updating event number for $applicationId to $updatedEventNumber with offence $updatedOffenceId and conviction $updatedConvictionId")

    val application = applicationRepository.findByIdOrNull(applicationId)
      ?: error("Application with identifier '$applicationId' does not exist")

    if (application !is ApprovedPremisesApplicationEntity) {
      error("Application should be of type ApprovedPremisesApplicationEntity")
    }

    val previousEventNumber = application.eventNumber
    val previousOffenceId = application.offenceId
    val previousConvictionId = application.convictionId

    log.info("Application previously used event number $previousEventNumber, offence $previousOffenceId and conviction $previousConvictionId")

    applicationRepository.updateEventNumber(applicationId, updatedEventNumber.toString(), updatedOffenceId, updatedConvictionId)

    updateDomainEvents(applicationId, updatedEventNumber)

    applicationTimelineNoteService.saveApplicationTimelineNote(
      applicationId = row.applicationId,
      note = "Application Support have updated the application to use event number '$updatedEventNumber'. Previous event number was '$previousEventNumber'",
      user = null,
    )

    spaceBookingRepository.updateEventNumber(applicationId, updatedEventNumber.toString())
  }

  private fun updateDomainEvents(applicationId: UUID, updatedEventNumber: Int) {
    val domainEvents = domainEventRepository.findByApplicationId(applicationId)

    domainEvents.filter {
      it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED
    }.forEach {
      log.info("Updating domain event ${it.id} of type APPROVED_PREMISES_APPLICATION_SUBMITTED")
      val envelope = objectMapper.readValue(it.data, ApplicationSubmittedEnvelope::class.java)
      val updatedEnvelope = envelope.copy(
        eventDetails = envelope.eventDetails.copy(deliusEventNumber = updatedEventNumber.toString()),
      )
      domainEventRepository.updateData(it.id, objectMapper.writeValueAsString(updatedEnvelope))
    }

    domainEvents.filter {
      it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED
    }.forEach {
      log.info("Updating domain event ${it.id} of type APPROVED_PREMISES_APPLICATION_ASSESSED")
      val envelope = objectMapper.readValue(it.data, ApplicationAssessedEnvelope::class.java)
      val updatedEnvelope = envelope.copy(
        eventDetails = envelope.eventDetails.copy(deliusEventNumber = updatedEventNumber.toString()),
      )
      domainEventRepository.updateData(it.id, objectMapper.writeValueAsString(updatedEnvelope))
    }

    domainEvents.filter {
      it.type == DomainEventType.APPROVED_PREMISES_BOOKING_MADE
    }.forEach {
      log.info("Updating domain event ${it.id} of type APPROVED_PREMISES_BOOKING_MADE")
      val envelope = objectMapper.readValue(it.data, BookingMadeEnvelope::class.java)
      val updatedEnvelope = envelope.copy(
        eventDetails = envelope.eventDetails.copy(deliusEventNumber = updatedEventNumber.toString()),
      )
      domainEventRepository.updateData(it.id, objectMapper.writeValueAsString(updatedEnvelope))
    }
  }
}

data class Cas1UpdateEventNumberSeedJobCsvRow(
  val applicationId: UUID,
  val eventNumber: Int,
  val offenceId: String,
  val convictionId: Long,
)
