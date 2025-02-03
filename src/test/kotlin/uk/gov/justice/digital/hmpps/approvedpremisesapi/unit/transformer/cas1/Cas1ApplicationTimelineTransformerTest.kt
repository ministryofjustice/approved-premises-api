package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1BookingChangedContentPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventAssociatedUrl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventUrlType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.domainevents.DomainEventDescriber
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TriggerSourceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEventSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1ApplicationTimelineTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas1ApplicationTimelineTransformerTest {
  private val mockDomainEventDescriber = mockk<DomainEventDescriber>()
  private val mockUserTransformer = mockk<UserTransformer>()

  private val applicationTimelineTransformer = Cas1ApplicationTimelineTransformer(
    applicationUrlTemplate = UrlTemplate("http://somehost:3000/applications/#id"),
    assessmentUrlTemplate = UrlTemplate("http://somehost:3000/assessments/#id"),
    bookingUrlTemplate = UrlTemplate("http://somehost:3000/premises/#premisesId/bookings/#bookingId"),
    cas1SpaceBookingUrlTemplate = UrlTemplate("http://somehost:3000/manage/premises/#premisesId/bookings/#bookingId"),
    appealUrlTemplate = UrlTemplate("http://somehost:3000/applications/#applicationId/appeals/#appealId"),
    mockDomainEventDescriber,
    mockUserTransformer,
  )

  data class DomainEventSummaryImpl(
    override val id: String,
    override val type: DomainEventType,
    override val occurredAt: OffsetDateTime,
    override val applicationId: UUID?,
    override val assessmentId: UUID?,
    override val bookingId: UUID?,
    override val premisesId: UUID?,
    override val appealId: UUID?,
    override val cas1SpaceBookingId: UUID?,
    override val triggerSource: TriggerSourceType?,
    override val triggeredByUser: UserEntity?,
  ) : DomainEventSummary

  @ParameterizedTest
  @EnumSource(DomainEventType::class, names = ["APPROVED_PREMISES_.+"], mode = EnumSource.Mode.MATCH_ANY)
  fun `transformDomainEventSummaryToTimelineEvent transforms domain event correctly`(domainEventType: DomainEventType) {
    val userJpa = UserEntityFactory().withDefaultProbationRegion().produce()

    val domainEvent = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = domainEventType,
      occurredAt = OffsetDateTime.now(),
      bookingId = null,
      applicationId = null,
      assessmentId = null,
      premisesId = null,
      appealId = null,
      cas1SpaceBookingId = null,
      triggerSource = null,
      triggeredByUser = userJpa,
    )

    val userApi = mockk<ApprovedPremisesUser>()
    every { mockUserTransformer.transformJpaToApi(userJpa, ServiceName.approvedPremises) } returns userApi
    every { mockDomainEventDescriber.getContentPayload(domainEvent) } returns Pair("Some event", null)

    val result = applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)

    assertThat(result.id).isEqualTo(domainEvent.id)
    assertThat(result.type).isEqualTo(domainEventType.cas1TimelineEventType)
    assertThat(result.occurredAt).isEqualTo(domainEvent.occurredAt.toInstant())
    assertThat(result.associatedUrls).isEmpty()
    assertThat(result.content).isEqualTo("Some event")
    assertThat(result.createdBy).isEqualTo(userApi)
    assertThat(result.triggerSource).isEqualTo(null)
  }

  @Test
  fun `transformDomainEventSummaryToTimelineEvent throws error if given CAS2 domain event type`() {
    val userJpa = UserEntityFactory().withDefaultProbationRegion().produce()
    val domainEvent = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = DomainEventType.CAS2_APPLICATION_SUBMITTED,
      occurredAt = OffsetDateTime.now(),
      bookingId = null,
      applicationId = null,
      assessmentId = null,
      premisesId = null,
      appealId = null,
      cas1SpaceBookingId = null,
      triggerSource = null,
      triggeredByUser = userJpa,
    )
    every { mockDomainEventDescriber.getContentPayload(domainEvent) } returns Pair("Some event", null)

    val exception = assertThrows<RuntimeException> {
      applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)
    }
    Assertions.assertThat(exception.message).isEqualTo("Cannot map CAS2_APPLICATION_SUBMITTED, only CAS1 is currently supported")
  }

  @Test
  fun `transformDomainEventSummaryToTimelineEvent adds applicationUrl if application id defined`() {
    val applicationId = UUID.randomUUID()
    val domainEvent = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED,
      occurredAt = OffsetDateTime.now(),
      bookingId = null,
      applicationId = applicationId,
      assessmentId = null,
      premisesId = null,
      appealId = null,
      cas1SpaceBookingId = null,
      triggerSource = null,
      triggeredByUser = null,
    )

    every { mockDomainEventDescriber.getContentPayload(domainEvent) } returns Pair("Some event", null)

    assertThat(applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)).isEqualTo(
      Cas1TimelineEvent(
        id = domainEvent.id,
        type = Cas1TimelineEventType.applicationSubmitted,
        occurredAt = domainEvent.occurredAt.toInstant(),
        associatedUrls = listOf(
          Cas1TimelineEventAssociatedUrl(Cas1TimelineEventUrlType.application, "http://somehost:3000/applications/$applicationId"),
        ),
        content = "Some event",
      ),
    )
  }

  @ParameterizedTest
  @EnumSource(DomainEventType::class, names = ["APPROVED_PREMISES_.+"], mode = EnumSource.Mode.MATCH_ANY)
  fun `transformDomainEventSummaryToTimelineEvent adds appealUrl for appeal events`(domainEventType: DomainEventType) {
    val applicationId = UUID.randomUUID()
    val appealId = UUID.randomUUID()
    val domainEvent = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = domainEventType,
      occurredAt = OffsetDateTime.now(),
      bookingId = null,
      applicationId = applicationId,
      assessmentId = null,
      premisesId = null,
      appealId = appealId,
      cas1SpaceBookingId = null,
      triggerSource = null,
      triggeredByUser = null,
    )

    every { mockDomainEventDescriber.getContentPayload(domainEvent) } returns Pair("Some event", null)

    assertThat(applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)).isEqualTo(
      Cas1TimelineEvent(
        id = domainEvent.id,
        type = domainEventType.cas1TimelineEventType!!,
        occurredAt = domainEvent.occurredAt.toInstant(),
        associatedUrls = if (domainEventType == DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED) {
          listOf(
            Cas1TimelineEventAssociatedUrl(Cas1TimelineEventUrlType.assessmentAppeal, "http://somehost:3000/applications/$applicationId/appeals/$appealId"),
          )
        } else {
          listOf(
            Cas1TimelineEventAssociatedUrl(Cas1TimelineEventUrlType.application, "http://somehost:3000/applications/$applicationId"),
          )
        },
        content = "Some event",
      ),
    )
  }

  @Test
  fun `transformDomainEventSummaryToTimelineEvent adds bookingUrl if booking id defined`() {
    val bookingId = UUID.randomUUID()
    val premisesId = UUID.randomUUID()
    val domainEvent = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = DomainEventType.APPROVED_PREMISES_BOOKING_MADE,
      occurredAt = OffsetDateTime.now(),
      bookingId = bookingId,
      applicationId = null,
      assessmentId = null,
      premisesId = premisesId,
      appealId = null,
      cas1SpaceBookingId = null,
      triggerSource = null,
      triggeredByUser = null,
    )

    every { mockDomainEventDescriber.getContentPayload(domainEvent) } returns Pair("Some event", null)

    assertThat(applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)).isEqualTo(
      Cas1TimelineEvent(
        id = domainEvent.id,
        type = Cas1TimelineEventType.bookingMade,
        occurredAt = domainEvent.occurredAt.toInstant(),
        associatedUrls = listOf(
          Cas1TimelineEventAssociatedUrl(Cas1TimelineEventUrlType.booking, "http://somehost:3000/premises/$premisesId/bookings/$bookingId"),
        ),
        content = "Some event",
      ),
    )
  }

  @Test
  fun `transformDomainEventSummaryToTimelineEvent adds cas1SpaceBookingUrl if cas1 space booking id defined`() {
    val spaceBookingId = UUID.randomUUID()
    val premisesId = UUID.randomUUID()
    val domainEvent = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = DomainEventType.APPROVED_PREMISES_BOOKING_MADE,
      occurredAt = OffsetDateTime.now(),
      bookingId = null,
      applicationId = null,
      assessmentId = null,
      premisesId = premisesId,
      appealId = null,
      cas1SpaceBookingId = spaceBookingId,
      triggerSource = null,
      triggeredByUser = null,
    )

    every { mockDomainEventDescriber.getContentPayload(domainEvent) } returns Pair("Some event", null)

    assertThat(applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)).isEqualTo(
      Cas1TimelineEvent(
        id = domainEvent.id,
        type = Cas1TimelineEventType.bookingMade,
        occurredAt = domainEvent.occurredAt.toInstant(),
        associatedUrls = listOf(
          Cas1TimelineEventAssociatedUrl(Cas1TimelineEventUrlType.spaceBooking, "http://somehost:3000/manage/premises/$premisesId/bookings/$spaceBookingId"),
        ),
        content = "Some event",
      ),
    )
  }

  @Test
  fun `transformDomainEventSummaryToTimelineEvent adds assessmentUrl if assessment id defined`() {
    val assessmentId = UUID.randomUUID()
    val domainEvent = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED,
      occurredAt = OffsetDateTime.now(),
      bookingId = null,
      applicationId = null,
      assessmentId = assessmentId,
      premisesId = null,
      appealId = null,
      cas1SpaceBookingId = null,
      triggerSource = null,
      triggeredByUser = null,
    )

    every { mockDomainEventDescriber.getContentPayload(domainEvent) } returns Pair("Some event", null)

    assertThat(applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)).isEqualTo(
      Cas1TimelineEvent(
        id = domainEvent.id,
        type = Cas1TimelineEventType.applicationAssessed,
        occurredAt = domainEvent.occurredAt.toInstant(),
        associatedUrls = listOf(
          Cas1TimelineEventAssociatedUrl(Cas1TimelineEventUrlType.assessment, "http://somehost:3000/assessments/$assessmentId"),
        ),
        content = "Some event",
      ),
    )
  }

  @Test
  fun `transformDomainEventSummaryToTimelineEvent does not include assessment URL for info requests`() {
    val assessmentId = UUID.randomUUID()
    val applicationId = UUID.randomUUID()

    val domainEvent = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED,
      occurredAt = OffsetDateTime.now(),
      bookingId = null,
      applicationId = applicationId,
      assessmentId = assessmentId,
      premisesId = null,
      appealId = null,
      cas1SpaceBookingId = null,
      triggerSource = null,
      triggeredByUser = null,
    )

    every { mockDomainEventDescriber.getContentPayload(domainEvent) } returns Pair("Some event", null)

    assertThat(applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)).isEqualTo(
      Cas1TimelineEvent(
        id = domainEvent.id,
        type = Cas1TimelineEventType.informationRequest,
        occurredAt = domainEvent.occurredAt.toInstant(),
        associatedUrls = listOf(
          Cas1TimelineEventAssociatedUrl(Cas1TimelineEventUrlType.application, "http://somehost:3000/applications/$applicationId"),
        ),
        content = "Some event",
      ),
    )
  }

  @Test
  fun `transformDomainEventSummaryToTimelineEvent does not include assessment URL for ASSESSMENT_ALLOCATED`() {
    val assessmentId = UUID.randomUUID()
    val applicationId = UUID.randomUUID()

    val domainEvent = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED,
      occurredAt = OffsetDateTime.now(),
      bookingId = null,
      applicationId = applicationId,
      assessmentId = assessmentId,
      premisesId = null,
      appealId = null,
      cas1SpaceBookingId = null,
      triggerSource = null,
      triggeredByUser = null,
    )

    every { mockDomainEventDescriber.getContentPayload(domainEvent) } returns Pair("Some event", null)

    assertThat(applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent).associatedUrls)
      .containsOnly(
        Cas1TimelineEventAssociatedUrl(Cas1TimelineEventUrlType.application, "http://somehost:3000/applications/$applicationId"),
      )

    assertThat(applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)).isEqualTo(
      Cas1TimelineEvent(
        id = domainEvent.id,
        type = Cas1TimelineEventType.assessmentAllocated,
        occurredAt = domainEvent.occurredAt.toInstant(),
        associatedUrls = listOf(
          Cas1TimelineEventAssociatedUrl(Cas1TimelineEventUrlType.application, "http://somehost:3000/applications/$applicationId"),
        ),
        content = "Some event",
      ),
    )
  }

  @Test
  fun `transformDomainEventSummaryToTimelineEvent adds all possible url types`() {
    val applicationId = UUID.randomUUID()
    val assessmentId = UUID.randomUUID()
    val bookingId = UUID.randomUUID()
    val premisesId = UUID.randomUUID()
    val appealId = UUID.randomUUID()
    val domainEvent = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = DomainEventType.APPROVED_PREMISES_BOOKING_MADE,
      occurredAt = OffsetDateTime.now(),
      bookingId = bookingId,
      applicationId = applicationId,
      assessmentId = assessmentId,
      premisesId = premisesId,
      appealId = appealId,
      cas1SpaceBookingId = null,
      triggerSource = null,
      triggeredByUser = null,
    )

    every { mockDomainEventDescriber.getContentPayload(domainEvent) } returns Pair("Some event", null)

    assertThat(applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)).isEqualTo(
      Cas1TimelineEvent(
        id = domainEvent.id,
        type = Cas1TimelineEventType.bookingMade,
        occurredAt = domainEvent.occurredAt.toInstant(),
        associatedUrls = listOf(
          Cas1TimelineEventAssociatedUrl(Cas1TimelineEventUrlType.application, "http://somehost:3000/applications/$applicationId"),
          Cas1TimelineEventAssociatedUrl(Cas1TimelineEventUrlType.assessment, "http://somehost:3000/assessments/$assessmentId"),
          Cas1TimelineEventAssociatedUrl(Cas1TimelineEventUrlType.booking, "http://somehost:3000/premises/$premisesId/bookings/$bookingId"),
        ),
        content = "Some event",
      ),
    )
  }

  @ParameterizedTest
  @EnumSource(TriggerSourceType::class)
  fun `transformDomainEventSummaryToTimelineEvent correctly maps triggerSource`(triggerSource: TriggerSourceType) {
    val assessmentId = UUID.randomUUID()
    val domainEvent = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED,
      occurredAt = OffsetDateTime.now(),
      bookingId = null,
      applicationId = null,
      assessmentId = assessmentId,
      premisesId = null,
      appealId = null,
      cas1SpaceBookingId = null,
      triggerSource = triggerSource,
      triggeredByUser = null,
    )

    every { mockDomainEventDescriber.getContentPayload(domainEvent) } returns Pair("Some event", null)

    assertThat(
      applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)
        .triggerSource?.name.equals(triggerSource.name, true),
    )
  }

  @Test
  fun `transformDomainEventSummaryToTimelineEvent transforms domain event with booking changed payload`() {
    val userJpa = UserEntityFactory().withDefaultProbationRegion().produce()
    val premisesId = UUID.randomUUID()
    val domainEventType = DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED

    val domainEvent = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = domainEventType,
      occurredAt = OffsetDateTime.now(),
      bookingId = null,
      applicationId = null,
      assessmentId = null,
      premisesId = premisesId,
      appealId = null,
      cas1SpaceBookingId = null,
      triggerSource = null,
      triggeredByUser = userJpa,
    )

    val userApi = mockk<ApprovedPremisesUser>()
    val payload = Cas1BookingChangedContentPayload(
      expectedArrival = LocalDate.now(),
      expectedDeparture = LocalDate.now(),
      type = domainEventType.cas1TimelineEventType!!,
      premises = NamedId(premisesId, "name"),
      schemaVersion = 2,
      characteristics = listOf(Cas1SpaceCharacteristic.isArsonSuitable),
      previousCharacteristics = listOf(Cas1SpaceCharacteristic.isGroundFloor),
    )

    every { mockUserTransformer.transformJpaToApi(userJpa, ServiceName.approvedPremises) } returns userApi
    every { mockDomainEventDescriber.getContentPayload(domainEvent) } returns Pair("Some event", payload)

    val result = applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(domainEvent)

    assertThat(result.id).isEqualTo(domainEvent.id)
    assertThat(result.type).isEqualTo(domainEventType.cas1TimelineEventType)
    assertThat(result.occurredAt).isEqualTo(domainEvent.occurredAt.toInstant())
    assertThat(result.associatedUrls).isEmpty()
    assertThat(result.content).isEqualTo("Some event")
    assertThat(result.createdBy).isEqualTo(userApi)
    assertThat(result.triggerSource).isEqualTo(null)
    assertThat(result.payload).isInstanceOf(Cas1BookingChangedContentPayload::class.java)
    assertThat(payload.premises.id).isEqualTo(premisesId)
    assertThat(payload.premises.name).isEqualTo("name")
    assertThat(payload.type).isEqualTo(domainEventType.cas1TimelineEventType)
    assertThat(payload.schemaVersion).isEqualTo(2)
    assertThat(payload.expectedArrival).isEqualTo(payload.expectedArrival)
    assertThat(payload.expectedDeparture).isEqualTo(payload.expectedDeparture)
    assertThat(payload.characteristics).isEqualTo(payload.characteristics)
    assertThat(payload.previousCharacteristics).isEqualTo(payload.previousCharacteristics)
  }
}
