package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas2

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob

@Component
class Cas2NoteMigrationJob(
  private val noteRepository: Cas2ApplicationNoteRepository,
  private val transactionTemplate: TransactionTemplate,
) : MigrationJob() {
  private val log = LoggerFactory.getLogger(this::class.java)
  override val shouldRunInTransaction = true

  override fun process(pageSize: Int) {
    log.info("Starting CAS2 note migration process...")

    var hasNext = true
    var slice: Slice<Cas2ApplicationNoteEntity>
    var page = 1
    var notesWithoutAssessment = noteRepository.countAllNotesWithoutAssessment()
    val notesWithAssessment = noteRepository.countAllNotesWithAssessment()

    while (hasNext) {
      log.info("Getting page $page for max page size $pageSize")
      slice = noteRepository.findAllNotesWithoutAssessment(PageRequest.of(0, pageSize))
      slice.content.forEach { note ->
        transactionTemplate.executeWithoutResult {
          log.info("Adding assessment to note ${note.id}")
          note.assessment = note.application.assessment
          noteRepository.save(
            note,
          )
        }
      }
      hasNext = slice.hasNext()
      page += 1
    }
    log.info("Before the migration:")
    log.info("notes without assessments: $notesWithoutAssessment")
    log.info("notes with assessments: $notesWithAssessment")
    log.info("After the migration:")
    log.info("notes without assessments: ${noteRepository.countAllNotesWithoutAssessment()}")
    log.info("notes with assessments: ${noteRepository.countAllNotesWithAssessment()}")
    log.info("CAS2 note migration process complete!")
  }
}
