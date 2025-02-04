package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ManagePomCasesClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PomAllocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PomDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prison
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PrisonerLocationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PrisonerLocationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.HmppsDomainEventPersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonIdentifier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.PomAllocationService
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

class PomAllocationServiceTest {

  private val prisonerLocationRepositoryMock = mockk<PrisonerLocationRepository>()
  private val managePomCasesClientMock = mockk<ManagePomCasesClient>()
  private val pomAllocationService = PomAllocationService(
    prisonerLocationRepositoryMock,
    managePomCasesClientMock,
  )

  @Test
  fun `Handle Pom Allocation successfully`() {
    val occuredAt = ZonedDateTime.now()
    every { managePomCasesClientMock.getPomAllocation(any()) } returns
      PomAllocation(
        manager = PomDetail("tom", "test", "tom.test@test.com"),
        prison = Prison("LON"),
      )

    every { prisonerLocationRepositoryMock.updateEndDateOfLatest(any(), any()) } returns Unit

    every { prisonerLocationRepositoryMock.save(any()) } returns
      PrisonerLocationEntity(
        id = UUID.randomUUID(),
        nomsNumber = "A0123BY",
        prisonCode = "LON",
        pomId = "A0123BY",
        startDate = occuredAt.toInstant().atOffset(ZoneOffset.UTC),
        endDate = null,
      )

    val message = HmppsDomainEvent(
      eventType = "test",
      version = 0,
      detailUrl = "http://localhost:8080/api/pom-allocation/A0123BY/3",
      occurredAt = occuredAt,
      description = null,
      additionalInformation = mapOf("prisonId" to "123", "staffCode" to "A0123BY"),
      personReference = HmppsDomainEventPersonReference(
        listOf(PersonIdentifier(type = "NOMS", value = "A0123BY")),
      ),
    )

    pomAllocationService.handlePomAllocationChangedMessage(message)

    verify(exactly = 1) { prisonerLocationRepositoryMock.updateEndDateOfLatest(any(), any()) }
    verify(exactly = 1) { prisonerLocationRepositoryMock.save(any()) }
  }
}