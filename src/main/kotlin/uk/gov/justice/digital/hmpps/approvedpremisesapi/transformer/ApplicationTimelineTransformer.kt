package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventAssociatedUrl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventUrlType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TriggerSourceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.domainevents.DomainEventDescriber
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEventSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate

@Component
class ApplicationTimelineTransformer(
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.assessment}") private val assessmentUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.booking}") private val bookingUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.cas1.space-booking}") private val cas1SpaceBookingUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.application-appeal}") private val appealUrlTemplate: UrlTemplate,
  private val domainEventDescriber: DomainEventDescriber,
  private val userTransformer: UserTransformer,
) {

  fun transformDomainEventSummaryToTimelineEvent(domainEventSummary: DomainEventSummary): TimelineEvent {
    val associatedUrls = generateUrlsForTimelineEventType(domainEventSummary)

    return TimelineEvent(
      id = domainEventSummary.id,
      type = domainEventSummary.type.timelineEventType ?: throw IllegalArgumentException("Cannot map ${domainEventSummary.type}, only CAS1 is currently supported"),
      occurredAt = domainEventSummary.occurredAt.toInstant(),
      associatedUrls = associatedUrls,
      content = domainEventDescriber.getDescription(domainEventSummary),
      createdBy = domainEventSummary.triggeredByUser?.let { userTransformer.transformJpaToApi(it, ServiceName.approvedPremises) },
      triggerSource = when (domainEventSummary.triggerSource) {
        uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TriggerSourceType.USER -> TriggerSourceType.user
        uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TriggerSourceType.SYSTEM -> TriggerSourceType.system
        null -> null
      },
    )
  }

  private fun appealUrlOrNull(domainEventSummary: DomainEventSummary): TimelineEventAssociatedUrl? {
    return if (domainEventSummary.type == DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED && domainEventSummary.appealId !== null) {
      TimelineEventAssociatedUrl(
        TimelineEventUrlType.assessmentAppeal,
        appealUrlTemplate.resolve(
          mapOf(
            "applicationId" to domainEventSummary.applicationId.toString(),
            "appealId" to domainEventSummary.appealId.toString(),
          ),
        ),
      )
    } else {
      null
    }
  }

  private fun applicationUrlOrNull(domainEventSummary: DomainEventSummary) = domainEventSummary.applicationId?.let {
    TimelineEventAssociatedUrl(
      TimelineEventUrlType.application,
      applicationUrlTemplate.resolve(mapOf("id" to domainEventSummary.applicationId.toString())),
    )
  }

  private fun assessmentUrlOrNull(domainEventSummary: DomainEventSummary) = domainEventSummary.assessmentId?.let {
    TimelineEventAssociatedUrl(
      TimelineEventUrlType.assessment,
      assessmentUrlTemplate.resolve(mapOf("id" to domainEventSummary.assessmentId.toString())),
    )
  }

  private fun bookingUrlOrNull(domainEventSummary: DomainEventSummary) = domainEventSummary.bookingId?.let {
    domainEventSummary.premisesId?.let {
      TimelineEventAssociatedUrl(
        TimelineEventUrlType.booking,
        bookingUrlTemplate.resolve(
          mapOf(
            "premisesId" to domainEventSummary.premisesId.toString(),
            "bookingId" to domainEventSummary.bookingId.toString(),
          ),
        ),
      )
    }
  }

  private fun cas1SpaceBookingUrlOrNull(domainEventSummary: DomainEventSummary) = domainEventSummary.cas1SpaceBookingId?.let {
    domainEventSummary.premisesId?.let {
      TimelineEventAssociatedUrl(
        TimelineEventUrlType.cas1SpaceBooking,
        cas1SpaceBookingUrlTemplate.resolve(
          mapOf(
            "premisesId" to domainEventSummary.premisesId.toString(),
            "bookingId" to domainEventSummary.cas1SpaceBookingId.toString(),
          ),
        ),
      )
    }
  }

  fun generateUrlsForTimelineEventType(domainEventSummary: DomainEventSummary): List<TimelineEventAssociatedUrl> {
    return when (domainEventSummary.type) {
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED -> listOfNotNull(
        appealUrlOrNull(domainEventSummary),
      )
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED,
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED,
      -> listOfNotNull(
        applicationUrlOrNull(domainEventSummary),
      )
      else -> listOfNotNull(
        applicationUrlOrNull(domainEventSummary),
        assessmentUrlOrNull(domainEventSummary),
        bookingUrlOrNull(domainEventSummary),
        cas1SpaceBookingUrlOrNull(domainEventSummary),
      )
    }
  }
}
