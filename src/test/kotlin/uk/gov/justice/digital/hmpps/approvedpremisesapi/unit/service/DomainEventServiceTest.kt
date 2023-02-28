package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import com.amazonaws.services.sns.model.PublishResult
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DomainEventEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationSubmittedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonNotArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.time.OffsetDateTime
import java.util.UUID

class DomainEventServiceTest {
  private val domainEventRespositoryMock = mockk<DomainEventRepository>()
  private val hmppsQueueServieMock = mockk<HmppsQueueService>()

  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val domainEventService = DomainEventService(
    objectMapper = objectMapper,
    domainEventRepository = domainEventRespositoryMock,
    hmppsQueueService = hmppsQueueServieMock,
    emitDomainEventsEnabled = true,
    applicationSubmittedDetailUrlTemplate = "http://frontend/events/application-submitted/#eventId",
    applicationAssessedDetailUrlTemplate = "http://frontend/events/application-assessed/#eventId",
    bookingMadeDetailUrlTemplate = "http://frontend/events/booking-made/#eventId",
    personArrivedDetailUrlTemplate = "http://frontend/events/person-arrived/#eventId",
    personNotArrivedDetailUrlTemplate = "http://frontend/events/person-not-arrived/#eventId"
  )

  @Test
  fun `getApplicationSubmittedDomainEvent returns null when no event does not exist`() {
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
      timestamp = occurredAt,
      eventType = "approved-premises.application.submitted",
      eventDetails = ApplicationSubmittedFactory().produce()
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
        occurredAt = occurredAt,
        data = data
      )
    )
  }

  @Test
  fun `saveApplicationSubmittedDomainEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val mockHmppsTopic = mockk<HmppsTopic>()

    every { hmppsQueueServieMock.findByTopicId("domainevents") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = OffsetDateTime.now(),
      data = ApplicationSubmittedEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = "approved-premises.application.submitted",
        eventDetails = ApplicationSubmittedFactory().produce()
      )
    )

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    domainEventService.saveApplicationSubmittedDomainEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        }
      )
    }

    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(
        match {
          val deserializedMessage = objectMapper.readValue(it.message, SnsEvent::class.java)

          deserializedMessage.eventType == "approved-premises.application.submitted" &&
            deserializedMessage.version == 1 &&
            deserializedMessage.description == "An application has been submitted for an Approved Premises placement" &&
            deserializedMessage.detailUrl == "http://frontend/events/application-submitted/$id" &&
            deserializedMessage.occurredAt == domainEventToSave.occurredAt &&
            deserializedMessage.additionalInformation.applicationId == applicationId &&
            deserializedMessage.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            deserializedMessage.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        }
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

    val mockHmppsTopic = mockk<HmppsTopic>()

    every { hmppsQueueServieMock.findByTopicId("domain-events") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = OffsetDateTime.now(),
      data = ApplicationSubmittedEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = "approved-premises.application.submitted",
        eventDetails = ApplicationSubmittedFactory().produce()
      )
    )

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    try {
      domainEventService.saveApplicationSubmittedDomainEvent(domainEventToSave)
    } catch (_: Exception) { }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        }
      )
    }

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any())
    }
  }

  @Test
  fun `getApplicationAssessedDomainEvent returns null when no event does not exist`() {
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
      timestamp = occurredAt,
      eventType = "approved-premises.application.assessed",
      eventDetails = ApplicationAssessedFactory().produce()
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
        occurredAt = occurredAt,
        data = data
      )
    )
  }

  @Test
  fun `saveApplicationAssessedDomainEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val mockHmppsTopic = mockk<HmppsTopic>()

    every { hmppsQueueServieMock.findByTopicId("domainevents") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = OffsetDateTime.now(),
      data = ApplicationAssessedEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = "approved-premises.application.assessed",
        eventDetails = ApplicationAssessedFactory().produce()
      )
    )

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    domainEventService.saveApplicationAssessedDomainEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        }
      )
    }

    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(
        match {
          val deserializedMessage = objectMapper.readValue(it.message, SnsEvent::class.java)

          deserializedMessage.eventType == "approved-premises.application.assessed" &&
            deserializedMessage.version == 1 &&
            deserializedMessage.description == "An application has been assessed for an Approved Premises placement" &&
            deserializedMessage.detailUrl == "http://frontend/events/application-assessed/$id" &&
            deserializedMessage.occurredAt == domainEventToSave.occurredAt &&
            deserializedMessage.additionalInformation.applicationId == applicationId &&
            deserializedMessage.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            deserializedMessage.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        }
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

    val mockHmppsTopic = mockk<HmppsTopic>()

    every { hmppsQueueServieMock.findByTopicId("domain-events") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = OffsetDateTime.now(),
      data = ApplicationAssessedEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = "approved-premises.application.assessed",
        eventDetails = ApplicationAssessedFactory().produce()
      )
    )

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    try {
      domainEventService.saveApplicationAssessedDomainEvent(domainEventToSave)
    } catch (_: Exception) { }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        }
      )
    }

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any())
    }
  }

  @Test
  fun `getBookingMadeDomainEvent returns null when no event does not exist`() {
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
      timestamp = occurredAt,
      eventType = "approved-premises.booking.made",
      eventDetails = BookingMadeFactory().produce()
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
        occurredAt = occurredAt,
        data = data
      )
    )
  }

  @Test
  fun `saveBookingMadeDomainEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val mockHmppsTopic = mockk<HmppsTopic>()

    every { hmppsQueueServieMock.findByTopicId("domainevents") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = OffsetDateTime.now(),
      data = BookingMadeEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = "approved-premises.booking.made",
        eventDetails = BookingMadeFactory().produce()
      )
    )

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    domainEventService.saveBookingMadeDomainEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_BOOKING_MADE &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        }
      )
    }

    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(
        match {
          val deserializedMessage = objectMapper.readValue(it.message, SnsEvent::class.java)

          deserializedMessage.eventType == "approved-premises.booking.made" &&
            deserializedMessage.version == 1 &&
            deserializedMessage.description == "An Approved Premises booking has been made" &&
            deserializedMessage.detailUrl == "http://frontend/events/booking-made/$id" &&
            deserializedMessage.occurredAt == domainEventToSave.occurredAt &&
            deserializedMessage.additionalInformation.applicationId == applicationId &&
            deserializedMessage.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            deserializedMessage.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        }
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

    val mockHmppsTopic = mockk<HmppsTopic>()

    every { hmppsQueueServieMock.findByTopicId("domain-events") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = OffsetDateTime.now(),
      data = BookingMadeEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = "approved-premises.booking.made",
        eventDetails = BookingMadeFactory().produce()
      )
    )

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    try {
      domainEventService.saveBookingMadeDomainEvent(domainEventToSave)
    } catch (_: Exception) { }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_BOOKING_MADE &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        }
      )
    }

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any())
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
      timestamp = occurredAt,
      eventType = "approved-premises.person.arrived",
      eventDetails = PersonArrivedFactory().produce()
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
        occurredAt = occurredAt,
        data = data
      )
    )
  }

  @Test
  fun `savePersonArrivedEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val mockHmppsTopic = mockk<HmppsTopic>()

    every { hmppsQueueServieMock.findByTopicId("domainevents") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = OffsetDateTime.now(),
      data = PersonArrivedEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = "approved-premises.person.arrived",
        eventDetails = PersonArrivedFactory().produce()
      )
    )

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    domainEventService.savePersonArrivedEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        }
      )
    }

    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(
        match {
          val deserializedMessage = objectMapper.readValue(it.message, SnsEvent::class.java)

          deserializedMessage.eventType == "approved-premises.person.arrived" &&
            deserializedMessage.version == 1 &&
            deserializedMessage.description == "Someone has arrived at an Approved Premises for their Booking" &&
            deserializedMessage.detailUrl == "http://frontend/events/person-arrived/$id" &&
            deserializedMessage.occurredAt == domainEventToSave.occurredAt &&
            deserializedMessage.additionalInformation.applicationId == applicationId &&
            deserializedMessage.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            deserializedMessage.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        }
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

    val mockHmppsTopic = mockk<HmppsTopic>()

    every { hmppsQueueServieMock.findByTopicId("domain-events") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = OffsetDateTime.now(),
      data = PersonArrivedEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = "approved-premises.person.arrived",
        eventDetails = PersonArrivedFactory().produce()
      )
    )

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    try {
      domainEventService.savePersonArrivedEvent(domainEventToSave)
    } catch (_: Exception) { }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        }
      )
    }

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any())
    }
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
      timestamp = occurredAt,
      eventType = "approved-premises.person.not-arrived",
      eventDetails = PersonNotArrivedFactory().produce()
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
        occurredAt = occurredAt,
        data = data
      )
    )
  }

  @Test
  fun `savePersonNotArrivedEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val mockHmppsTopic = mockk<HmppsTopic>()

    every { hmppsQueueServieMock.findByTopicId("domainevents") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = OffsetDateTime.now(),
      data = PersonNotArrivedEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = "approved-premises.person.not-arrived",
        eventDetails = PersonNotArrivedFactory().produce()
      )
    )

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    domainEventService.savePersonNotArrivedEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        }
      )
    }

    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(
        match {
          val deserializedMessage = objectMapper.readValue(it.message, SnsEvent::class.java)

          deserializedMessage.eventType == "approved-premises.person.not-arrived" &&
            deserializedMessage.version == 1 &&
            deserializedMessage.description == "Someone has failed to arrive at an Approved Premises for their Booking" &&
            deserializedMessage.detailUrl == "http://frontend/events/person-not-arrived/$id" &&
            deserializedMessage.occurredAt == domainEventToSave.occurredAt &&
            deserializedMessage.additionalInformation.applicationId == applicationId &&
            deserializedMessage.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            deserializedMessage.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        }
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

    val mockHmppsTopic = mockk<HmppsTopic>()

    every { hmppsQueueServieMock.findByTopicId("domain-events") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = OffsetDateTime.now(),
      data = PersonNotArrivedEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = "approved-premises.person.not-arrived",
        eventDetails = PersonNotArrivedFactory().produce()
      )
    )

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    try {
      domainEventService.savePersonNotArrivedEvent(domainEventToSave)
    } catch (_: Exception) { }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        }
      )
    }

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any())
    }
  }
}
