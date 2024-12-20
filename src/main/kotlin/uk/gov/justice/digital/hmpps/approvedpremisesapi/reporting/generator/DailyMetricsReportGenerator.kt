package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ApprovedPremisesApplicationMetricsSummaryDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.DailyMetricReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ReportService
import java.time.LocalDate

class DailyMetricsReportGenerator(
  private val domainEvents: List<DomainEventEntity>,
  private val applications: List<ApprovedPremisesApplicationMetricsSummaryDto>,
  private val domainEventService: Cas1DomainEventService,
) : ReportGenerator<LocalDate, DailyMetricReportRow, Cas1ReportService.MonthSpecificReportParams>(DailyMetricReportRow::class) {
  override fun filter(properties: Cas1ReportService.MonthSpecificReportParams): (LocalDate) -> Boolean = {
    true
  }

  override val convert: LocalDate.(properties: Cas1ReportService.MonthSpecificReportParams) -> List<DailyMetricReportRow> = {
    val applicationsCreatedToday = applications.filter {
        application ->
      application.createdAt == this
    }

    val domainEventsToday = domainEvents.filter {
        domainEventEntity ->
      domainEventEntity.occurredAt.toLocalDate() == this
    }

    val applicationsSubmittedToday = domainEventsToday.filter {
        domainEventEntity ->
      domainEventEntity.type == DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED
    }.map { domainEventService.toDomainEvent(it, ApplicationSubmittedEnvelope::class) }

    val assessmentsCompletedToday = domainEventsToday.filter {
        domainEventEntity ->
      domainEventEntity.type == DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED
    }.map { domainEventService.toDomainEvent(it, ApplicationAssessedEnvelope::class) }

    val bookingsMadeToday = domainEventsToday.filter {
        domainEventEntity ->
      domainEventEntity.type == DomainEventType.APPROVED_PREMISES_BOOKING_MADE
    }.map { domainEventService.toDomainEvent(it, BookingMadeEnvelope::class) }

    listOf(
      DailyMetricReportRow(
        date = this,
        applicationsStarted = applicationsCreatedToday.size,
        uniqueUsersStartingApplications = applicationsCreatedToday.groupBy { application -> application.createdByUserId }.size,
        applicationsSubmitted = applicationsSubmittedToday.size,
        uniqueUsersSubmittingApplications = applicationsSubmittedToday.groupBy { domainEvent -> domainEvent.data.eventDetails.submittedBy.staffMember.staffCode }.size,
        assessmentsCompleted = assessmentsCompletedToday.size,
        uniqueUsersCompletingAssessments = assessmentsCompletedToday.groupBy { domainEvent -> domainEvent.data.eventDetails.assessedBy.staffMember!!.staffCode }.size,
        bookingsMade = bookingsMadeToday.size,
        uniqueUsersMakingBookings = bookingsMadeToday.groupBy { domainEvent -> domainEvent.data.eventDetails.bookedBy }.size,
      ),
    )
  }
}
