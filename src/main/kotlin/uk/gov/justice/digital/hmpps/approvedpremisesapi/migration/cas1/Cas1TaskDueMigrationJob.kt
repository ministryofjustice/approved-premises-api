package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1

import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.TaskDeadlineService

@Component
class Cas1TaskDueMigrationJob(
  private val assessmentRepository: AssessmentRepository,
  private val placementApplicationRepository: PlacementApplicationRepository,
  private val placementRequestRepository: PlacementRequestRepository,
  private val entityManager: EntityManager,
  private val taskDeadlineService: TaskDeadlineService,
) : MigrationJob() {
  private val log = LoggerFactory.getLogger(this::class.java)
  override val shouldRunInTransaction = true

  override fun process(pageSize: Int) {
    log.info("Starting Migration process...")

    this.updateAssessments(pageSize)
    this.updatePlacementApplications(pageSize)
    this.updatePlacementRequests(pageSize)
  }

  private fun updateAssessments(pageSize: Int) {
    log.info("Updating assessments....")
    var page = 1
    var hasNext = true
    var slice: Slice<ApprovedPremisesAssessmentEntity>

    while (hasNext) {
      log.info("Getting page $page")
      slice = assessmentRepository.findAllWithNullDueAt(PageRequest.of(0, pageSize))
      slice.content.forEach {
        assessmentRepository.updateDueAt(it.id, taskDeadlineService.getDeadline(it))
        entityManager.detach(it)
      }
      entityManager.clear()
      hasNext = slice.hasNext()
      page += 1
    }
  }

  private fun updatePlacementApplications(pageSize: Int) {
    log.info("Updating placement applications....")
    var page = 1
    var hasNext = true
    var slice: Slice<PlacementApplicationEntity>

    while (hasNext) {
      log.info("Getting page $page")
      slice = placementApplicationRepository.findAllWithNullDueAt(PageRequest.of(0, pageSize))
      slice.content.forEach {
        placementApplicationRepository.updateDueAt(it.id, taskDeadlineService.getDeadline(it))
        entityManager.detach(it)
      }
      entityManager.clear()
      hasNext = slice.hasNext()
      page += 1
    }
  }

  private fun updatePlacementRequests(pageSize: Int) {
    log.info("Updating placement requests....")
    var page = 1
    var hasNext = true
    var slice: Slice<PlacementRequestEntity>

    while (hasNext) {
      log.info("Getting page $page")
      slice = placementRequestRepository.findAllWithNullDueAt(PageRequest.of(0, pageSize))
      slice.content.forEach {
        placementRequestRepository.updateDueAt(it.id, taskDeadlineService.getDeadline(it))
        entityManager.detach(it)
      }
      entityManager.clear()
      hasNext = slice.hasNext()
      page += 1
    }
  }
}
