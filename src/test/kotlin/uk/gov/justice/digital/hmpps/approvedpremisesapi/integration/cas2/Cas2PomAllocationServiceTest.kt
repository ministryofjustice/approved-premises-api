package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import com.ninjasquad.springmockk.SpykBean
import io.mockk.verify
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.HmppsDomainEventPersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.PomAllocationService
import java.time.ZonedDateTime

class Cas2PomAllocationServiceTest : IntegrationTestBase() {

  @SpykBean
  private lateinit var pomAllocationService: PomAllocationService

  @Test
  fun `Handle Pom Allocation successfully`() {
    val message = HmppsDomainEvent(
      eventType = "test", version = 0, detailUrl = null, occurredAt = ZonedDateTime.now(), description = null, mapOf(),
      HmppsDomainEventPersonReference(
        listOf(),
      ),
    )
    pomAllocationService.handlePomAllocationChangedMessage(message)

    verify(exactly = 1) { prisonerLocationRepository.findLatestByNomsNumber(any()) }
    verify(exactly = 1) { prisonerLocationRepository.updateEndDate(any(), any()) }
    verify(exactly = 1) { prisonerLocationRepository.save(any()) }
  }
}