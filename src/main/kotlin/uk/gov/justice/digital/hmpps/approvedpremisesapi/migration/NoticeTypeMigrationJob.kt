package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import java.util.UUID

@Repository
interface NoticeTypeMigrationJobApplicationRepository : JpaRepository<ApplicationEntity, UUID> {
  @Modifying
  @Query(
    "UPDATE ApprovedPremisesApplicationEntity ap set " +
      "ap.noticeType = 'emergency' " +
      "where ap.isEmergencyApplication = true",
  )
  fun updateEmergencyApplicationNoticeType()

  @Modifying
  @Query("UPDATE ApprovedPremisesApplicationEntity ap set ap.noticeType = :noticeType where ap.id = :id")
  fun updateApplicationNoticeType(id: UUID, noticeType: Cas1ApplicationTimelinessCategory)

  @Query(
    "SELECT ap FROM ApprovedPremisesApplicationEntity ap where " +
      "ap.noticeType IS null AND ap.isEmergencyApplication = false ",
  )
  fun getApplicationsThatRequireNoticeTypeUpdating(pageable: Pageable): Slice<ApprovedPremisesApplicationEntity>
}

@Component
class NoticeTypeMigrationJob(
  private val applicationRepository: NoticeTypeMigrationJobApplicationRepository,
  private val entityManager: EntityManager,
) : MigrationJob() {
  private val log = LoggerFactory.getLogger(this::class.java)
  override val shouldRunInTransaction = true

  override fun process(pageSize: Int) {
    log.info("Updating emergency applications")
    applicationRepository.updateEmergencyApplicationNoticeType()

    var page = 1
    var hasNext = true
    var slice: Slice<ApprovedPremisesApplicationEntity>

    while (hasNext) {
      log.info("Getting page $page")
      slice = applicationRepository.getApplicationsThatRequireNoticeTypeUpdating(PageRequest.of(0, pageSize))

      slice.content.forEach {
        entityManager.detach(it)
        if (it.isShortNoticeApplication() == true) {
          applicationRepository.updateApplicationNoticeType(it.id, Cas1ApplicationTimelinessCategory.shortNotice)
        } else {
          applicationRepository.updateApplicationNoticeType(it.id, Cas1ApplicationTimelinessCategory.standard)
        }
      }

      entityManager.clear()
      hasNext = slice.hasNext()
      page += 1
    }
  }
}
