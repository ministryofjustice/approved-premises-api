package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import org.slf4j.LoggerFactory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import java.util.UUID

@Component
class UpdateSentenceTypeAndSituationJob(
  private val applicationRepository: UpdateSentenceTypeAndSituationRepository,
) : MigrationJob() {
  private val log = LoggerFactory.getLogger(this::class.java)
  override val shouldRunInTransaction = true

  override fun process(pageSize: Int) {
    val items = applicationRepository.getIdSentenceTypeAndSituationFromAllApplicationData()

    items.forEach { item ->
      log.info("Updating application ${item.getId()}")
      applicationRepository.updateSentenceTypeAndSituation(
        item.getId(),
        item.getSentenceType(),
        item.getSituation(),
      )
    }
  }
}

@Repository
interface UpdateSentenceTypeAndSituationRepository : JpaRepository<ApplicationEntity, UUID> {
  @Query(
    """
      SELECT
        CAST(a.id AS TEXT) as id,
        a.data -> 'basic-information' -> 'sentence-type' ->> 'sentenceType' as sentenceType,
        a.data -> 'basic-information' -> 'situation' ->> 'situation' as situation
      FROM approved_premises_applications apa
      LEFT JOIN applications a ON a.id = apa.id
      WHERE a.service = 'approved-premises'
    """,
    nativeQuery = true,
  )
  fun getIdSentenceTypeAndSituationFromAllApplicationData(): List<ApplicationIdSentenceTypeAndSituation>

  @Modifying
  @Query("UPDATE ApprovedPremisesApplicationEntity ap set ap.sentenceType = :sentenceType, ap.situation = :situation WHERE ap.id = :applicationId")
  fun updateSentenceTypeAndSituation(applicationId: UUID, sentenceType: String?, situation: String?)
}

interface ApplicationIdSentenceTypeAndSituation {
  fun getId(): UUID

  fun getSentenceType(): String?

  fun getSituation(): String?
}
