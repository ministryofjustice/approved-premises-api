package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import java.util.UUID

@Component
class Cas1UpdateApplicationLicenceExpiryDateJob(
  private val updateLicenceExpiryDateRepository: UpdateLicenceExpiryDateRepository,
  private val migrationLogger: MigrationLogger,
  override val shouldRunInTransaction: Boolean = true,
) : MigrationJob() {

  override fun process(pageSize: Int) = runLicenceExpiryDateUpdateProcess()

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun runLicenceExpiryDateUpdateProcess() {
    try {
      updateLicenceExpiryDateRepository.updateLicenceExpiryDate()
    } catch (exception: Exception) {
      migrationLogger.error("Unable to run update licence expiry date job", exception)
    }
  }
}

@Repository
interface UpdateLicenceExpiryDateRepository : JpaRepository<ApplicationEntity, UUID> {

  companion object {
    private const val QUERY_UPDATE_LICENCE_EXPIRY_DATE = """
      UPDATE approved_premises_applications AS target
      SET licence_expiry_date = TO_DATE(source.licenceExpiryDate, 'YYYY-MM-DD')
      FROM (
       SELECT
        application.id AS id,
        application.data -> 'basic-information' -> 'relevant-dates' -> 'selectedDates' ->> 'licenceExpiryDate' AS licenceExpiryDate
      FROM approved_premises_applications AS apa
      LEFT JOIN applications AS application ON application.id = apa.id
      ) AS source
      WHERE target.id = source.id AND target.licence_expiry_date IS NULL
    """
  }

  @Query(QUERY_UPDATE_LICENCE_EXPIRY_DATE, nativeQuery = true)
  @Modifying
  fun updateLicenceExpiryDate()
}
