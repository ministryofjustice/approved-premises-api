package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2v2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2v2ApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2.Cas2v2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2.Cas2v2UserAccessService
import java.time.OffsetDateTime

class Cas2v2UserAccessServiceTest {

  @Nested
  inner class UserCanViewApplication {

    private val cas2v2UserAccessService = Cas2v2UserAccessService()
    val newestJsonSchema = Cas2v2ApplicationJsonSchemaEntityFactory()
      .withSchema("{}")
      .produce()

    @Nested
    inner class WhenApplicationCreatedByUser {
      private val user = NomisUserEntityFactory()
        .produce()

      @Test
      fun `returns true`() {
        val application = Cas2v2ApplicationEntityFactory()
          .withApplicationSchema(newestJsonSchema)
          .withCreatedByUser(user)
          .produce()

        assertThat(cas2v2UserAccessService.userCanViewCas2v2Application(user, application)).isTrue
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
          val cas2v2Application = Cas2v2ApplicationEntityFactory()
            .withApplicationSchema(newestJsonSchema)
            .withCreatedByUser(anotherUser)
            .produce()

          assertThat(cas2v2UserAccessService.userCanViewCas2v2Application(user, cas2v2Application)).isFalse
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
          val cas2v2Application = Cas2v2ApplicationEntityFactory()
            .withApplicationSchema(newestJsonSchema)
            .withCreatedByUser(anotherUser)
            .withSubmittedAt(OffsetDateTime.now())
            .withReferringPrisonCode("different-prison")
            .produce()

          assertThat(cas2v2UserAccessService.userCanViewCas2v2Application(user, cas2v2Application)).isFalse
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
            val cas2v2Application = Cas2v2ApplicationEntityFactory()
              .withApplicationSchema(newestJsonSchema)
              .withCreatedByUser(anotherUserWithNoPrison)
              .withSubmittedAt(OffsetDateTime.now())
              .produce()

            assertThat(cas2v2UserAccessService.userCanViewCas2v2Application(userWithNoPrison, cas2v2Application)).isFalse
          }
        }
      }

      @Nested
      inner class WhenCas2v2ApplicationMadeForSamePrison {
        private val user = NomisUserEntityFactory()
          .withActiveCaseloadId("my-prison")
          .produce()
        private val anotherUser = NomisUserEntityFactory()
          .withActiveCaseloadId("my-prison")
          .produce()

        @Test
        fun `returns true if the user created the application`() {
          val cas2v2Application = Cas2v2ApplicationEntityFactory()
            .withApplicationSchema(newestJsonSchema)
            .withCreatedByUser(user)
            .withSubmittedAt(OffsetDateTime.now())
            .withReferringPrisonCode("my-prison")
            .produce()

          assertThat(cas2v2UserAccessService.userCanViewCas2v2Application(user, cas2v2Application)).isTrue
        }

        @Test
        fun `returns true when user NOT creator`() {
          val newestJsonSchema = Cas2ApplicationJsonSchemaEntityFactory()
            .withSchema("{}")
            .produce()

          val cas2v2Application = Cas2v2ApplicationEntityFactory()
            .withApplicationSchema(newestJsonSchema)
            .withCreatedByUser(anotherUser)
            .withSubmittedAt(OffsetDateTime.now())
            .withReferringPrisonCode("my-prison")
            .produce()

          assertThat(cas2v2UserAccessService.userCanViewCas2v2Application(user, cas2v2Application)).isTrue
        }
      }
    }
  }
}
