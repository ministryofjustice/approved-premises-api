package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2bail

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2BailApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2bail.Cas2BailApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2bail.Cas2BailUserAccessService
import java.time.OffsetDateTime

class Cas2BailUserAccessServiceTest {

  @Nested
  inner class UserCanViewApplication {

    private val cas2BailUserAccessService = Cas2BailUserAccessService()
    val newestJsonSchema = Cas2BailApplicationJsonSchemaEntityFactory()
      .withSchema("{}")
      .produce()

    @Nested
    inner class WhenApplicationCreatedByUser {
      private val user = NomisUserEntityFactory()
        .produce()

      @Test
      fun `returns true`() {
        val application = Cas2BailApplicationEntityFactory()
          .withApplicationSchema(newestJsonSchema)
          .withCreatedByUser(user)
          .produce()

        assertThat(cas2BailUserAccessService.userCanViewCas2BailApplication(user, application)).isTrue
      }
    }

    @Nested
    inner class WhenApplicationNotCreatedByUser {

      @Nested
      inner class WhenApplicationNotSubmitted {
        private val user = NomisUserEntityFactory()
          .produce()
        private val anotherUser = NomisUserEntityFactory()
          .produce()

        @Test
        fun `returns false`() {
          val cas2BailApplication = Cas2BailApplicationEntityFactory()
            .withApplicationSchema(newestJsonSchema)
            .withCreatedByUser(anotherUser)
            .produce()

          assertThat(cas2BailUserAccessService.userCanViewCas2BailApplication(user, cas2BailApplication)).isFalse
        }
      }

      @Nested
      inner class WhenApplicationMadeForDifferentPrison {
        private val user = NomisUserEntityFactory()
          .withActiveCaseloadId("my-prison")
          .produce()
        private val anotherUser = NomisUserEntityFactory()
          .withActiveCaseloadId("different-prison").produce()

        @Test
        fun `returns false`() {
          val cas2BailApplication = Cas2BailApplicationEntityFactory()
            .withApplicationSchema(newestJsonSchema)
            .withCreatedByUser(anotherUser)
            .withSubmittedAt(OffsetDateTime.now())
            .withReferringPrisonCode("different-prison")
            .produce()

          assertThat(cas2BailUserAccessService.userCanViewCas2BailApplication(user, cas2BailApplication)).isFalse
        }

        @Nested
        inner class WhenNoPrisonData {
          private val userWithNoPrison = NomisUserEntityFactory()
            .withActiveCaseloadId("my-prison")
            .produce()
          private val anotherUserWithNoPrison = NomisUserEntityFactory()
            .withActiveCaseloadId("different-prison").produce()

          @Test
          fun `returns false`() {
            val cas2BailApplication = Cas2BailApplicationEntityFactory()
              .withApplicationSchema(newestJsonSchema)
              .withCreatedByUser(anotherUserWithNoPrison)
              .withSubmittedAt(OffsetDateTime.now())
              .produce()

            assertThat(cas2BailUserAccessService.userCanViewCas2BailApplication(userWithNoPrison, cas2BailApplication)).isFalse
          }
        }
      }

      @Nested
      inner class WhenCas2BailApplicationMadeForSamePrison {
        private val user = NomisUserEntityFactory()
          .withActiveCaseloadId("my-prison")
          .produce()
        private val anotherUser = NomisUserEntityFactory()
          .withActiveCaseloadId("my-prison")
          .produce()

        @Test
        fun `returns true if the user created the application`() {
          val cas2BailApplication = Cas2BailApplicationEntityFactory()
            .withApplicationSchema(newestJsonSchema)
            .withCreatedByUser(user)
            .withSubmittedAt(OffsetDateTime.now())
            .withReferringPrisonCode("my-prison")
            .produce()

          assertThat(cas2BailUserAccessService.userCanViewCas2BailApplication(user, cas2BailApplication)).isTrue
        }

        @Test
        fun `returns true when user NOT creator`() {
          val newestJsonSchema = Cas2ApplicationJsonSchemaEntityFactory()
            .withSchema("{}")
            .produce()

          val cas2BailApplication = Cas2BailApplicationEntityFactory()
            .withApplicationSchema(newestJsonSchema)
            .withCreatedByUser(anotherUser)
            .withSubmittedAt(OffsetDateTime.now())
            .withReferringPrisonCode("my-prison")
            .produce()

          assertThat(cas2BailUserAccessService.userCanViewCas2BailApplication(user, cas2BailApplication)).isTrue
        }
      }
    }
  }
}
