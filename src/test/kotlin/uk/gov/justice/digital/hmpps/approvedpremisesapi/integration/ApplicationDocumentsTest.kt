package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ContentDisposition
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DocumentFromDeliusApiFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulDocumentDownloadCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulDocumentsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.APDeliusDocument
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DocumentTransformer
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class ApplicationDocumentsTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var documentTransformer: DocumentTransformer

  @Test
  fun `Get application documents without JWT returns 401`() {
    webTestClient.get()
      .uri("/applications/1e63735c-64dd-4904-a808-fc11e4feded3/documents")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get application documents - returns 200`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withConvictionId(12345)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
        }

        val convictionLevelDocId = UUID.randomUUID()
        val documents = stubDocumentsFromDelius(convictionLevelDocId)

        apDeliusContextMockSuccessfulDocumentsCall(offenderDetails.otherIds.crn, documents)

        webTestClient.get()
          .uri("/applications/${application.id}/documents")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(objectMapper.writeValueAsString(documentTransformer.transformToApi(documents)))
      }
    }
  }

  @Test
  fun `Download document - returns 404 when not found in documents meta data`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withConvictionId(12345)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
        }

        val convictionLevelDocId = UUID.randomUUID()
        val documents = stubDocumentsFromDelius(convictionLevelDocId)

        apDeliusContextMockSuccessfulDocumentsCall(offenderDetails.otherIds.crn, documents)

        val notFoundDocId = UUID.randomUUID()
        webTestClient.get()
          .uri("/documents/${application.crn}/$notFoundDocId")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }

  @Test
  fun `Download document - returns 200 with correct body and headers`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withConvictionId(12345)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
        }
        val convictionLevelDocId = UUID.randomUUID()
        val documents = stubDocumentsFromDelius(convictionLevelDocId)
        val docFileContents = this::class.java.classLoader.getResourceAsStream("mock_document.txt").readAllBytes()

        apDeliusContextMockSuccessfulDocumentsCall(offenderDetails.otherIds.crn, documents)
        apDeliusContextMockSuccessfulDocumentDownloadCall(offenderDetails.otherIds.crn, convictionLevelDocId, docFileContents)

        val result = webTestClient.get()
          .uri("/documents/${application.crn}/$convictionLevelDocId")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectHeader()
          .contentDisposition(ContentDisposition.parse("attachment; filename=\"conviction_level_doc.pdf\""))
          .expectBody()
          .returnResult()

        assertThat(result.responseBody).isEqualTo(docFileContents)
      }
    }
  }

  private fun stubDocumentsFromDelius(convictionLevelDocId: UUID): List<APDeliusDocument> {
    return listOf(
      DocumentFromDeliusApiFactory()
        .withId(UUID.randomUUID().toString())
        .withDescription("Offender level doc description")
        .withLevel("LEVEL-1")
        .withEventNumber("2")
        .withFilename("offender_level_doc.pdf")
        .withTypeCode("TYPE-1")
        .withTypeDescription("Type 1 Description")
        .withDateSaved(LocalDateTime.parse("2024-03-18T06:00:00").atZone(ZoneId.systemDefault()))
        .withDateCreated(LocalDateTime.parse("2024-03-02T15:20:00").atZone(ZoneId.systemDefault()))
        .produce(),
      DocumentFromDeliusApiFactory()
        .withId(convictionLevelDocId.toString())
        .withDescription("Conviction level doc description")
        .withLevel("LEVEL-2")
        .withEventNumber("1")
        .withFilename("conviction_level_doc.pdf")
        .withTypeCode("TYPE-2")
        .withTypeDescription("Type 2 Description")
        .withDateSaved(LocalDateTime.parse("2024-10-05T13:12:00").atZone(ZoneId.systemDefault()))
        .withDateCreated(LocalDateTime.parse("2024-10-02T10:40:00").atZone(ZoneId.systemDefault()))
        .produce(),
    )
  }
}
