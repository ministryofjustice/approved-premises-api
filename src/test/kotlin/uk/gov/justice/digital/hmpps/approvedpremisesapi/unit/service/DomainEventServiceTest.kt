package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingChangedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DomainEventEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationSubmittedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingCancelledFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingChangedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingNotMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonDepartedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonNotArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ConfiguredDomainEventWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

class DomainEventServiceTest {
  private val domainEventRespositoryMock = mockk<DomainEventRepository>()
  private val domainEventWorkerMock = mockk<ConfiguredDomainEventWorker>()
  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val domainEventService = DomainEventService(
    objectMapper = objectMapper,
    domainEventRepository = domainEventRespositoryMock,
    domainEventWorker = domainEventWorkerMock,
    emitDomainEventsEnabled = true,
    applicationSubmittedDetailUrlTemplate = "http://api/events/application-submitted/#eventId",
    applicationAssessedDetailUrlTemplate = "http://api/events/application-assessed/#eventId",
    bookingMadeDetailUrlTemplate = "http://api/events/booking-made/#eventId",
    personArrivedDetailUrlTemplate = "http://api/events/person-arrived/#eventId",
    personNotArrivedDetailUrlTemplate = "http://api/events/person-not-arrived/#eventId",
    personDepartedDetailUrlTemplate = "http://api/events/person-departed/#eventId",
    bookingNotMadeDetailUrlTemplate = "http://api/events/booking-not-made/#eventId",
    bookingCancelledDetailUrlTemplate = "http://api/events/booking-cancelled/#eventId",
    bookingChangedDetailUrlTemplate = "http://api/events/booking-changed/#eventId",
    applicationWithdrawnDetailUrlTemplate = "http://api/events/application-withdrawn/#eventId",
  )

  @Test
  fun `getApplicationSubmittedDomainEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getApplicationSubmittedDomainEvent(id)).isNull()
  }

  @Test
  fun `getApplicationSubmittedDomainEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = ApplicationSubmittedEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.application.submitted",
      eventDetails = ApplicationSubmittedFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getApplicationSubmittedDomainEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  @Test
  fun `saveApplicationSubmittedDomainEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = ApplicationSubmittedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = "approved-premises.application.submitted",
        eventDetails = ApplicationSubmittedFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.saveApplicationSubmittedDomainEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match {
          it.eventType == "approved-premises.application.submitted" &&
            it.version == 1 &&
            it.description == "An application has been submitted for an Approved Premises placement" &&
            it.detailUrl == "http://api/events/application-submitted/$id" &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.additionalInformation.applicationId == applicationId &&
            it.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            it.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `saveApplicationSubmittedDomainEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = ApplicationSubmittedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = "approved-premises.application.submitted",
        eventDetails = ApplicationSubmittedFactory().produce(),
      ),
    )

    try {
      domainEventService.saveApplicationSubmittedDomainEvent(domainEventToSave)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        },
      )
    }

    verify(exactly = 0) {
      domainEventWorkerMock.emitEvent(any(), any())
    }
  }

  @Test
  fun `getApplicationAssessedDomainEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getApplicationAssessedDomainEvent(id)).isNull()
  }

  @Test
  fun `getApplicationAssessedDomainEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = ApplicationAssessedEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.application.assessed",
      eventDetails = ApplicationAssessedFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getApplicationAssessedDomainEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  @Test
  fun `saveApplicationAssessedDomainEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = ApplicationAssessedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = "approved-premises.application.assessed",
        eventDetails = ApplicationAssessedFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.saveApplicationAssessedDomainEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match {
          it.eventType == "approved-premises.application.assessed" &&
            it.version == 1 &&
            it.description == "An application has been assessed for an Approved Premises placement" &&
            it.detailUrl == "http://api/events/application-assessed/$id" &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.additionalInformation.applicationId == applicationId &&
            it.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            it.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `saveApplicationAssessedDomainEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = ApplicationAssessedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = "approved-premises.application.assessed",
        eventDetails = ApplicationAssessedFactory().produce(),
      ),
    )

    try {
      domainEventService.saveApplicationAssessedDomainEvent(domainEventToSave)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        },
      )
    }

    verify(exactly = 0) {
      domainEventWorkerMock.emitEvent(any(), any())
    }
  }

  @Test
  fun `getBookingMadeDomainEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getBookingMadeEvent(id)).isNull()
  }

  @Test
  fun `getBookingMadeDomainEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = BookingMadeEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.booking.made",
      eventDetails = BookingMadeFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_BOOKING_MADE)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getBookingMadeEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  @Test
  fun `saveBookingMadeDomainEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = BookingMadeEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = "approved-premises.booking.made",
        eventDetails = BookingMadeFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.saveBookingMadeDomainEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_BOOKING_MADE &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match {
          it.eventType == "approved-premises.booking.made" &&
            it.version == 1 &&
            it.description == "An Approved Premises booking has been made" &&
            it.detailUrl == "http://api/events/booking-made/$id" &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.additionalInformation.applicationId == applicationId &&
            it.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            it.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `saveBookingMadeDomainEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = BookingMadeEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = "approved-premises.booking.made",
        eventDetails = BookingMadeFactory().produce(),
      ),
    )

    try {
      domainEventService.saveBookingMadeDomainEvent(domainEventToSave)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_BOOKING_MADE &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        },
      )
    }

    verify(exactly = 0) {
      domainEventWorkerMock.emitEvent(any(), any())
    }
  }

  @Test
  fun `getBookingChangedDomainEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getBookingChangedEvent(id)).isNull()
  }

  @Test
  fun `getBookingChangedDomainEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = BookingChangedEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.booking.changed",
      eventDetails = BookingChangedFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getBookingChangedEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  @Test
  fun `saveBookingChangedDomainEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = BookingChangedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = "approved-premises.booking.changed",
        eventDetails = BookingChangedFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.saveBookingChangedEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match {
          it.eventType == "approved-premises.booking.changed" &&
            it.version == 1 &&
            it.description == "An Approved Premises Booking has been changed" &&
            it.detailUrl == "http://api/events/booking-changed/$id" &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.additionalInformation.applicationId == applicationId &&
            it.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            it.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `saveBookingChangedDomainEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = BookingChangedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = "approved-premises.booking.changed",
        eventDetails = BookingChangedFactory().produce(),
      ),
    )

    try {
      domainEventService.saveBookingChangedEvent(domainEventToSave)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        },
      )
    }

    verify(exactly = 0) {
      domainEventWorkerMock.emitEvent(any(), any())
    }
  }

  @Test
  fun `getBookingCancelledDomainEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getBookingCancelledEvent(id)).isNull()
  }

  @Test
  fun `getBookingCancelledDomainEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = BookingCancelledEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.booking.cancelled",
      eventDetails = BookingCancelledFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getBookingCancelledEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  @Test
  fun `saveBookingCancelledDomainEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = BookingCancelledEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = "approved-premises.booking.cancelled",
        eventDetails = BookingCancelledFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.saveBookingCancelledEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.bookingId == domainEventToSave.bookingId
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match {
          it.eventType == "approved-premises.booking.cancelled" &&
            it.version == 1 &&
            it.description == "An Approved Premises Booking has been cancelled" &&
            it.detailUrl == "http://api/events/booking-cancelled/$id" &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.additionalInformation.applicationId == applicationId &&
            it.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            it.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `saveBookingCancelledDomainEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = BookingCancelledEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = "approved-premises.booking.cancelled",
        eventDetails = BookingCancelledFactory().produce(),
      ),
    )

    try {
      domainEventService.saveBookingCancelledEvent(domainEventToSave)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        },
      )
    }

    verify(exactly = 0) {
      domainEventWorkerMock.emitEvent(any(), any())
    }
  }

  @Test
  fun `getPersonArrivedEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getPersonArrivedEvent(id)).isNull()
  }

  @Test
  fun `getPersonArrivedEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = PersonArrivedEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.person.arrived",
      eventDetails = PersonArrivedFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getPersonArrivedEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  @Test
  fun `savePersonArrivedEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = PersonArrivedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = "approved-premises.person.arrived",
        eventDetails = PersonArrivedFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.savePersonArrivedEvent(domainEventToSave, emit = true)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.bookingId == domainEventToSave.bookingId
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match {
          it.eventType == "approved-premises.person.arrived" &&
            it.version == 1 &&
            it.description == "Someone has arrived at an Approved Premises for their Booking" &&
            it.detailUrl == "http://api/events/person-arrived/$id" &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.additionalInformation.applicationId == applicationId &&
            it.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            it.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `savePersonArrivedEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = PersonArrivedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = "approved-premises.person.arrived",
        eventDetails = PersonArrivedFactory().produce(),
      ),
    )

    try {
      domainEventService.savePersonArrivedEvent(domainEventToSave, emit = true)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        },
      )
    }

    verify { domainEventWorkerMock wasNot Called }
  }

  @Test
  fun `savePersonArrivedEvent persists event, does not emit event to SNS if emit flag is false`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = PersonArrivedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = "approved-premises.person.arrived",
        eventDetails = PersonArrivedFactory().produce(),
      ),
    )

    domainEventService.savePersonArrivedEvent(domainEventToSave, emit = false)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.bookingId == domainEventToSave.bookingId
        },
      )
    }

    verify { domainEventWorkerMock wasNot Called }
  }

  @Test
  fun `getPersonNotArrivedEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getPersonNotArrivedEvent(id)).isNull()
  }

  @Test
  fun `getPersonNotArrivedEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = PersonNotArrivedEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.person.not-arrived",
      eventDetails = PersonNotArrivedFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getPersonNotArrivedEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  @Test
  fun `savePersonNotArrivedEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = PersonNotArrivedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = "approved-premises.person.not-arrived",
        eventDetails = PersonNotArrivedFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.savePersonNotArrivedEvent(domainEventToSave, emit = true)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.bookingId == domainEventToSave.bookingId
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match {
          it.eventType == "approved-premises.person.not-arrived" &&
            it.version == 1 &&
            it.description == "Someone has failed to arrive at an Approved Premises for their Booking" &&
            it.detailUrl == "http://api/events/person-not-arrived/$id" &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.additionalInformation.applicationId == applicationId &&
            it.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            it.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `savePersonNotArrivedEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = PersonNotArrivedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = "approved-premises.person.not-arrived",
        eventDetails = PersonNotArrivedFactory().produce(),
      ),
    )

    try {
      domainEventService.savePersonNotArrivedEvent(domainEventToSave, emit = true)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        },
      )
    }

    verify { domainEventWorkerMock wasNot Called }
  }

  @Test
  fun `savePersonNotArrivedEvent persists event, does not emit event to SNS if emit flag is false`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = PersonNotArrivedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = "approved-premises.person.not-arrived",
        eventDetails = PersonNotArrivedFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.savePersonNotArrivedEvent(domainEventToSave, emit = false)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.bookingId == domainEventToSave.bookingId
        },
      )
    }

    verify { domainEventWorkerMock wasNot Called }
  }

  @Test
  fun `getPersonDepartedEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getPersonDepartedEvent(id)).isNull()
  }

  @Test
  fun `getPersonDepartedEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = PersonDepartedEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.person.departed",
      eventDetails = PersonDepartedFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getPersonDepartedEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  @Test
  fun `savePersonDepartedEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = PersonDepartedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = "approved-premises.person.departed",
        eventDetails = PersonDepartedFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.savePersonDepartedEvent(domainEventToSave, emit = true)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.bookingId == domainEventToSave.bookingId
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match {
          it.eventType == "approved-premises.person.departed" &&
            it.version == 1 &&
            it.description == "Someone has left an Approved Premises" &&
            it.detailUrl == "http://api/events/person-departed/$id" &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.additionalInformation.applicationId == applicationId &&
            it.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            it.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `savePersonDepartedEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = PersonDepartedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = "approved-premises.person.departed",
        eventDetails = PersonDepartedFactory().produce(),
      ),
    )

    try {
      domainEventService.savePersonDepartedEvent(domainEventToSave, emit = true)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        },
      )
    }

    verify { domainEventWorkerMock wasNot Called }
  }

  @Test
  fun `savePersonDepartedEvent persists event, does not emit event to SNS if emit flag is false`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = PersonDepartedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = "approved-premises.person.departed",
        eventDetails = PersonDepartedFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.savePersonDepartedEvent(domainEventToSave, emit = false)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.bookingId == domainEventToSave.bookingId
        },
      )
    }

    verify { domainEventWorkerMock wasNot Called }
  }

  @Test
  fun `getBookingNotMadeEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getBookingMadeEvent(id)).isNull()
  }

  @Test
  fun `getBookingNotMadeEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = BookingNotMadeEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.booking.not-made",
      eventDetails = BookingNotMadeFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getBookingNotMadeEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  @Test
  fun `saveBookingNotMadeEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = BookingNotMadeEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = "approved-premises.booking.not-made",
        eventDetails = BookingNotMadeFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.saveBookingNotMadeEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match {
          it.eventType == "approved-premises.booking.not-made" &&
            it.version == 1 &&
            it.description == "It was not possible to create a Booking on this attempt" &&
            it.detailUrl == "http://api/events/booking-not-made/$id" &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.additionalInformation.applicationId == applicationId &&
            it.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            it.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `saveBookingNotMadeEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = BookingNotMadeEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = "approved-premises.booking.not-made",
        eventDetails = BookingNotMadeFactory().produce(),
      ),
    )

    try {
      domainEventService.saveBookingNotMadeEvent(domainEventToSave)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        },
      )
    }

    verify(exactly = 0) {
      domainEventWorkerMock.emitEvent(any(), any())
    }
  }

  @Test
  fun `getApplicationWithdrawnEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getApplicationWithdrawnEvent(id)).isNull()
  }

  @Test
  fun `getApplicationWithdrawnEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = ApplicationWithdrawnEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.application.withdrawn",
      eventDetails = ApplicationWithdrawnFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getApplicationWithdrawnEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  @Test
  fun `saveApplicationWithdrawnEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = ApplicationWithdrawnEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = "approved-premises.application.withdrawn",
        eventDetails = ApplicationWithdrawnFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.saveApplicationWithdrawnEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match {
          it.eventType == "approved-premises.application.withdrawn" &&
            it.version == 1 &&
            it.description == "An Approved Premises Application has been withdrawn" &&
            it.detailUrl == "http://api/events/application-withdrawn/$id" &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.additionalInformation.applicationId == applicationId &&
            it.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            it.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `saveApplicationWithdrawnEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = ApplicationWithdrawnEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = "approved-premises.application.withdrawn",
        eventDetails = ApplicationWithdrawnFactory().produce(),
      ),
    )

    try {
      domainEventService.saveApplicationWithdrawnEvent(domainEventToSave)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        },
      )
    }

    verify(exactly = 0) {
      domainEventWorkerMock.emitEvent(any(), any())
    }
  }
}
