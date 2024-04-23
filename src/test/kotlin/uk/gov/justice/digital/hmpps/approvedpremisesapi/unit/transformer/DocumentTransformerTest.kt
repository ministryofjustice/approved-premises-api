package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Document
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DocumentLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DocumentFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.GroupedDocumentsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DocumentTransformer
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class DocumentTransformerTest {
  private val documentTransformer = DocumentTransformer()

  val groupedDocuments = GroupedDocumentsFactory()
    .withOffenderLevelDocument(
      DocumentFactory()
        .withId(UUID.fromString("b0df5ec4-5685-4b02-8a95-91b6da80156f").toString())
        .withDocumentName("offender_level_doc.pdf")
        .withTypeCode("TYPE-1")
        .withTypeDescription("Type 1 Description")
        .withCreatedAt(LocalDateTime.parse("2022-12-07T11:40:00"))
        .withExtendedDescription("Extended Description 1")
        .produce(),
    )
    .withOffenderLevelDocument(
      DocumentFactory()
        .withId(null)
        .produce(),
    )
    .withConvictionLevelDocument(
      "12345",
      DocumentFactory()
        .withId(UUID.fromString("457af8a5-82b1-449a-ad03-032b39435865").toString())
        .withDocumentName("conviction_level_doc.pdf")
        .withoutAuthor()
        .withTypeCode("TYPE-2")
        .withTypeDescription("Type 2 Description")
        .withCreatedAt(LocalDateTime.parse("2022-12-07T10:40:00"))
        .withExtendedDescription("Extended Description 2")
        .produce(),
    )
    .withConvictionLevelDocument(
      "12345",
      DocumentFactory()
        .withId(null)
        .produce(),
    )
    .withConvictionLevelDocument(
      "6789",
      DocumentFactory()
        .withId(UUID.fromString("e20589b3-7f83-4502-a0df-c8dd645f3f44").toString())
        .withDocumentName("conviction_level_doc_2.pdf")
        .withTypeCode("TYPE-2")
        .withTypeDescription("Type 2 Description")
        .withCreatedAt(LocalDateTime.parse("2022-12-07T10:40:00"))
        .withExtendedDescription("Extended Description 2")
        .produce(),
    )
    .produce()

  @Test
  fun `transformToApi with conviction id specified filters out irrelevant documents`() {
    val result = documentTransformer.transformToApi(groupedDocuments, 12345)

    assertThat(result).containsExactlyInAnyOrder(
      Document(
        id = UUID.fromString("b0df5ec4-5685-4b02-8a95-91b6da80156f").toString(),
        level = DocumentLevel.offender,
        fileName = "offender_level_doc.pdf",
        createdAt = Instant.parse("2022-12-07T11:40:00Z"),
        typeCode = "TYPE-1",
        typeDescription = "Type 1 Description",
        description = "Extended Description 1",
      ),
      Document(
        id = UUID.fromString("457af8a5-82b1-449a-ad03-032b39435865").toString(),
        level = DocumentLevel.conviction,
        fileName = "conviction_level_doc.pdf",
        createdAt = Instant.parse("2022-12-07T10:40:00Z"),
        typeCode = "TYPE-2",
        typeDescription = "Type 2 Description",
        description = "Extended Description 2",
      ),
    )
  }

  @Test
  fun `transformToApi with conviction id not specified doesn't filter outdocuments`() {
    val result = documentTransformer.transformToApi(groupedDocuments, convictionId = null)

    assertThat(result).containsExactlyInAnyOrder(
      Document(
        id = UUID.fromString("b0df5ec4-5685-4b02-8a95-91b6da80156f").toString(),
        level = DocumentLevel.offender,
        fileName = "offender_level_doc.pdf",
        createdAt = Instant.parse("2022-12-07T11:40:00Z"),
        typeCode = "TYPE-1",
        typeDescription = "Type 1 Description",
        description = "Extended Description 1",
      ),
      Document(
        id = UUID.fromString("457af8a5-82b1-449a-ad03-032b39435865").toString(),
        level = DocumentLevel.conviction,
        fileName = "conviction_level_doc.pdf",
        createdAt = Instant.parse("2022-12-07T10:40:00Z"),
        typeCode = "TYPE-2",
        typeDescription = "Type 2 Description",
        description = "Extended Description 2",
      ),
      Document(
        id = UUID.fromString("e20589b3-7f83-4502-a0df-c8dd645f3f44").toString(),
        level = DocumentLevel.conviction,
        fileName = "conviction_level_doc_2.pdf",
        createdAt = Instant.parse("2022-12-07T10:40:00Z"),
        typeCode = "TYPE-2",
        typeDescription = "Type 2 Description",
        description = "Extended Description 2",
      ),
    )
  }
}
/*
DocumentFactory()
        .withId(UUID.fromString("e20589b3-7f83-4502-a0df-c8dd645f3f44").toString())
        .withDocumentName("conviction_level_doc_2.pdf")
        .withTypeCode("TYPE-2")
        .withTypeDescription("Type 2 Description")
        .withCreatedAt(LocalDateTime.parse("2022-12-07T10:40:00"))
        .withExtendedDescription("Extended Description 2")
        .produce(),
 */
