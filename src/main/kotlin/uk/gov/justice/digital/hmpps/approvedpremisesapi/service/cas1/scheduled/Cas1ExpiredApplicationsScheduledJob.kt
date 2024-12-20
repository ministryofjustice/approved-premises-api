package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.scheduled

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationExpired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationExpiredEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TriggerSourceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import java.time.Instant
import java.util.UUID

@Service
class Cas1ExpiredApplicationsScheduledJob(
  private val domainEventService: Cas1DomainEventService,
  private val applicationRepository: ApplicationRepository,
  private val transactionTemplate: TransactionTemplate,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Scheduled(cron = "0 0 2 * * ?")
  @SchedulerLock(name = "cas1_expire_applications", lockAtMostFor = "5m", lockAtLeastFor = "1m")
  fun expireApplications() {
    val applicationIds = applicationRepository.findAllExpiredApplications()

    log.info("There are ${applicationIds.size} applications to update.")

    applicationIds.forEach { applicationId ->
      log.info("Updating application $applicationId.")

      transactionTemplate.executeWithoutResult {
        val application = applicationRepository.findByIdOrNull(applicationId) as ApprovedPremisesApplicationEntity
        val previousStatus = application.status.name
        application.status = ApprovedPremisesApplicationStatus.EXPIRED
        applicationRepository.save(application)

        log.info("Status changed from $previousStatus to ${application.status} for application $applicationId.")

        val applicationExpired = ApplicationExpired(
          applicationId,
          previousStatus,
          (applicationRepository.findByIdOrNull(applicationId) as ApprovedPremisesApplicationEntity).status.name,
        )

        val domainEventId = UUID.randomUUID()
        val eventOccurredAt = Instant.now()
        domainEventService.saveApplicationExpiredEvent(
          DomainEvent(
            id = domainEventId,
            applicationId = application.id,
            crn = application.crn,
            nomsNumber = application.nomsNumber,
            occurredAt = eventOccurredAt,
            data = ApplicationExpiredEnvelope(
              id = domainEventId,
              timestamp = eventOccurredAt,
              eventType = EventType.applicationExpired,
              eventDetails = applicationExpired,
            ),
          ),
          triggerSource = TriggerSourceType.SYSTEM,
          emit = false,
        )
        log.info("Domain event id $domainEventId emitted for application $applicationId.")
      }
    }
  }
}
