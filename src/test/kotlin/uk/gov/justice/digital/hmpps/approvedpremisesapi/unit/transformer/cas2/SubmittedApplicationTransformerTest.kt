package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NomisUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NomisUserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.StatusUpdateTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.SubmittedApplicationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.sql.Timestamp
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

class SubmittedApplicationTransformerTest {
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockNomisUserTransformer = mockk<NomisUserTransformer>()
  private val mockStatusUpdateTransformer = mockk<StatusUpdateTransformer>()

  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val applicationTransformer = SubmittedApplicationTransformer(
    objectMapper,
    mockPersonTransformer,
    mockNomisUserTransformer,
    mockStatusUpdateTransformer,
  )

  private val user = NomisUserEntityFactory().produce()

  private val cas2ApplicationFactory = Cas2ApplicationEntityFactory().withCreatedByUser(user)

  private val mockStatusUpdateEntity = mockk<Cas2StatusUpdateEntity>()
  private val mockStatusUpdateApi = mockk<Cas2StatusUpdate>()
  private val submittedCas2ApplicationFactory = cas2ApplicationFactory
    .withStatusUpdates(mutableListOf(mockStatusUpdateEntity, mockStatusUpdateEntity))
    .withSubmittedAt(OffsetDateTime.now())

  private val mockNomisUser = mockk<NomisUser>()

  @BeforeEach
  fun setup() {
    every { mockPersonTransformer.transformModelToPersonApi(any()) } returns mockk<Person>()
    every { mockNomisUserTransformer.transformJpaToApi(any()) } returns mockNomisUser
    every { mockStatusUpdateTransformer.transformJpaToApi(any()) } returns mockStatusUpdateApi
  }

  @Nested
  inner class TransformJpaToApi {
    @Test
    fun `transforms to API representation with NomisUser, no data and status updates`() {
      val jpaEntity = submittedCas2ApplicationFactory.produce()

      val transformation = applicationTransformer.transformJpaToApiRepresentation(jpaEntity, mockk())

      assertThat(transformation.submittedBy).isEqualTo(mockNomisUser)

      assertThat(transformation.statusUpdates).isEqualTo(
        listOf(mockStatusUpdateApi, mockStatusUpdateApi),
      )

      assertThat(transformation).hasOnlyFields(
        "createdAt",
        "document",
        "id",
        "outdatedSchema",
        "person",
        "schemaVersion",
        "statusUpdates",
        "submittedAt",
        "submittedBy",
        "telephoneNumber",
      )
    }
  }

  @Nested
  inner class TransformJpaSummaryToCas2SubmittedSummary {
    @Test
    fun `transforms submitted summary application to API summary representation `() {
      val applicationSummary = object : Cas2ApplicationSummary {
        override fun getId() = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809")
        override fun getCrn() = randomStringMultiCaseWithNumbers(6)
        override fun getCreatedByUserId() = UUID.fromString("836a9460-b177-433a-a0d9-262509092c9f")
        override fun getCreatedAt() = Timestamp(Instant.parse("2023-04-19T13:25:00+01:00").toEpochMilli())
        override fun getSubmittedAt() = Timestamp(Instant.parse("2023-04-19T13:25:30+01:00").toEpochMilli())
      }

      val transformation = applicationTransformer.transformJpaSummaryToApiRepresentation(applicationSummary, mockk())

      assertThat(transformation.id).isEqualTo(applicationSummary.getId())
    }
  }
}
