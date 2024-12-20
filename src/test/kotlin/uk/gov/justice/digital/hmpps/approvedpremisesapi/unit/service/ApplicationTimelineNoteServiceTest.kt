package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApplicationTimelineNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelineNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelineNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationTimelineNoteService
import java.util.UUID

@ExtendWith(MockKExtension::class)
class ApplicationTimelineNoteServiceTest {
  @MockK
  private lateinit var applicationTimelineNoteRepository: ApplicationTimelineNoteRepository

  @InjectMockKs
  private lateinit var service: ApplicationTimelineNoteService

  @Nested
  inner class SaveApplicationTimelineNote {

    @Test
    fun `Add Note To Application`() {
      val applicationId = UUID.randomUUID()
      val note = "the note"
      val user = UserEntityFactory().withDefaults().produce()
      val spaceBookingId = UUID.randomUUID()

      every { applicationTimelineNoteRepository.save(any()) } returns ApplicationTimelineNoteEntityFactory().produce()

      service.saveApplicationTimelineNote(
        applicationId,
        note,
        user,
        spaceBookingId,
      )

      val savedTimelineNoteCaptor = slot<ApplicationTimelineNoteEntity>()
      verify(exactly = 1) {
        applicationTimelineNoteRepository.save(capture(savedTimelineNoteCaptor))
      }

      val savedTimelineNote = savedTimelineNoteCaptor.captured
      assertThat(savedTimelineNote.applicationId).isEqualTo(applicationId)
      assertThat(savedTimelineNote.body).isEqualTo("the note")
      assertThat(savedTimelineNote.createdBy).isEqualTo(user)
      assertThat(savedTimelineNote.cas1SpaceBookingId).isEqualTo(spaceBookingId)
    }
  }
}
