package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.entity.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OfflineApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ApplicationFacade
import java.time.OffsetDateTime
import java.util.UUID

class Cas1ApplicationFacadeTest {

  @Nested
  inner class GetId {

    @Test
    fun `get from application if defined`() {
      val appId = UUID.randomUUID()
      val offlineAppId = UUID.randomUUID()

      val facade = Cas1ApplicationFacade(
        application = applicationFactory().withId(appId).produce(),
        offlineApplication = offlineApplicationFactory().withId(offlineAppId).produce(),
      )

      assertThat(facade.id).isEqualTo(appId)
    }

    @Test
    fun `get from offline application if application not defined`() {
      val offlineAppId = UUID.randomUUID()

      val facade = Cas1ApplicationFacade(
        application = null,
        offlineApplication = offlineApplicationFactory().withId(offlineAppId).produce(),
      )

      assertThat(facade.id).isEqualTo(offlineAppId)
    }
  }

  @Nested
  inner class GetEventNumber {

    @Test
    fun `get from application if defined`() {
      val facade = Cas1ApplicationFacade(
        application = applicationFactory().withSubmittedAt(OffsetDateTime.parse("2007-12-03T10:15:30+01:00")).produce(),
        offlineApplication = offlineApplicationFactory().withCreatedAt(OffsetDateTime.parse("2008-12-03T10:15:30+01:00")).produce(),
      )

      assertThat(facade.submittedAt).isEqualTo(OffsetDateTime.parse("2007-12-03T10:15:30+01:00"))
    }

    @Test
    fun `get from offline application if application not defined`() {
      val facade = Cas1ApplicationFacade(
        application = null,
        offlineApplication = offlineApplicationFactory().withCreatedAt(OffsetDateTime.parse("2008-12-03T10:15:30+01:00")).produce(),
      )

      assertThat(facade.submittedAt).isEqualTo(OffsetDateTime.parse("2008-12-03T10:15:30+01:00"))
    }
  }

  private fun applicationFactory() = ApprovedPremisesApplicationEntityFactory().withDefaults()
  private fun offlineApplicationFactory() = OfflineApplicationEntityFactory()
}
