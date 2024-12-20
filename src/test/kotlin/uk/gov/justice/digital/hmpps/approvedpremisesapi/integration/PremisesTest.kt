package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DateCapacity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ExtendedPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewRoom
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateRoom
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulStaffMembersCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMembersPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RoomTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.StaffMemberTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

class PremisesTest {
  @Nested
  inner class CreatePremises : InitialiseDatabasePerClassTestBase() {
    private lateinit var user: UserEntity
    private lateinit var probationDeliveryUnit: ProbationDeliveryUnitEntity
    private lateinit var jwt: String

    @BeforeAll
    fun setup() {
      val userArgs = givenAUser()

      user = userArgs.first
      jwt = userArgs.second
      probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(user.probationRegion)
      }
    }

    @Test
    fun `Create new premises returns 201`() {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            name = "test-premises",
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB123CD",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.pending,
            probationDeliveryUnitId = probationDeliveryUnit.id,
          ),
        )
        .exchange()
        .expectStatus()
        .isCreated
    }

    @Test
    fun `When a new premises is created then all field data is persisted`() {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB123CD",
            notes = "some arbitrary notes",
            name = "new premises",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.pending,
            probationDeliveryUnitId = probationDeliveryUnit.id,
            turnaroundWorkingDayCount = 5,
          ),
        )
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody()
        .jsonPath("addressLine1").isEqualTo("1 somewhere")
        .jsonPath("addressLine2").isEqualTo("Some district")
        .jsonPath("town").isEqualTo("Somewhere")
        .jsonPath("postcode").isEqualTo("AB123CD")
        .jsonPath("service").isEqualTo(ServiceName.temporaryAccommodation.value)
        .jsonPath("notes").isEqualTo("some arbitrary notes")
        .jsonPath("name").isEqualTo("new premises")
        .jsonPath("localAuthorityArea.id").isEqualTo("a5f52443-6b55-498c-a697-7c6fad70cc3f")
        .jsonPath("probationRegion.id").isEqualTo(user.probationRegion.id.toString())
        .jsonPath("status").isEqualTo("pending")
        .jsonPath("pdu").isEqualTo(probationDeliveryUnit.name)
        .jsonPath("probationDeliveryUnit.id").isEqualTo(probationDeliveryUnit.id.toString())
        .jsonPath("probationDeliveryUnit.name").isEqualTo(probationDeliveryUnit.name)
        .jsonPath("turnaroundWorkingDayCount").isEqualTo(5)
    }

    @Test
    fun `When a new Temporary Accommodation premises is created with a legacy PDU name then all field data is persisted`() {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB123CD",
            notes = "some arbitrary notes",
            name = "legacy pdu name",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.pending,
            pdu = probationDeliveryUnit.name,
          ),
        )
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody()
        .jsonPath("addressLine1").isEqualTo("1 somewhere")
        .jsonPath("addressLine2").isEqualTo("Some district")
        .jsonPath("town").isEqualTo("Somewhere")
        .jsonPath("postcode").isEqualTo("AB123CD")
        .jsonPath("service").isEqualTo(ServiceName.temporaryAccommodation.value)
        .jsonPath("notes").isEqualTo("some arbitrary notes")
        .jsonPath("name").isEqualTo("legacy pdu name")
        .jsonPath("localAuthorityArea.id").isEqualTo("a5f52443-6b55-498c-a697-7c6fad70cc3f")
        .jsonPath("probationRegion.id").isEqualTo(user.probationRegion.id.toString())
        .jsonPath("status").isEqualTo("pending")
        .jsonPath("pdu").isEqualTo(probationDeliveryUnit.name)
        .jsonPath("probationDeliveryUnit.id").isEqualTo(probationDeliveryUnit.id.toString())
        .jsonPath("probationDeliveryUnit.name").isEqualTo(probationDeliveryUnit.name)
    }

    @Test
    fun `Trying to create a new premises without a name returns 400`() {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            name = "",
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB123CD",
            notes = "some arbitrary notes",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.active,
          ),
        )
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody()
        .jsonPath("title").isEqualTo("Bad Request")
        .jsonPath("invalid-params[0].errorType").isEqualTo("empty")
    }

    @Test
    fun `When a new Temporary Accommodation premises is created with no turnaround working day count then it defaults to 2`() {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB123CD",
            notes = "some arbitrary notes",
            name = "premises with no turnaround working day count",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.pending,
            probationDeliveryUnitId = probationDeliveryUnit.id,
          ),
        )
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody()
        .jsonPath("addressLine1").isEqualTo("1 somewhere")
        .jsonPath("addressLine2").isEqualTo("Some district")
        .jsonPath("town").isEqualTo("Somewhere")
        .jsonPath("postcode").isEqualTo("AB123CD")
        .jsonPath("service").isEqualTo(ServiceName.temporaryAccommodation.value)
        .jsonPath("notes").isEqualTo("some arbitrary notes")
        .jsonPath("name").isEqualTo("premises with no turnaround working day count")
        .jsonPath("localAuthorityArea.id").isEqualTo("a5f52443-6b55-498c-a697-7c6fad70cc3f")
        .jsonPath("probationRegion.id").isEqualTo(user.probationRegion.id.toString())
        .jsonPath("status").isEqualTo("pending")
        .jsonPath("pdu").isEqualTo(probationDeliveryUnit.name)
        .jsonPath("probationDeliveryUnit.id").isEqualTo(probationDeliveryUnit.id.toString())
        .jsonPath("probationDeliveryUnit.name").isEqualTo(probationDeliveryUnit.name)
        .jsonPath("turnaroundWorkingDayCount").isEqualTo(2)
    }

    @ParameterizedTest(name = "Trying to create a new Temporary Accommodation premises with turnaround working day count = {0} returns 400 and errorType = {1}")
    @CsvSource(
      "-1,isNotAPositiveInteger",
      "-4,isNotAPositiveInteger",
    )
    fun `Trying to create a new Temporary Accommodation premises with turnaround working day count less than 1 returns 400`(
      turnaroundWorkingDayCount: Int,
      expectedErrorType: String,
    ) {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB123CD",
            notes = "some arbitrary notes",
            name = "with turnaround working day count less than 1",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.pending,
            probationDeliveryUnitId = probationDeliveryUnit.id,
            turnaroundWorkingDayCount = turnaroundWorkingDayCount,
          ),
        )
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody()
        .jsonPath("title").isEqualTo("Bad Request")
        .jsonPath("invalid-params[0].propertyName").isEqualTo("$.turnaroundWorkingDayCount")
        .jsonPath("invalid-params[0].errorType").isEqualTo(expectedErrorType)
    }

    @Test
    fun `Trying to create a new premises with a non-unique name returns 400`() {
      temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { givenAProbationRegion() }
        withName("premises-name-conflict")
      }

      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            name = "premises-name-conflict",
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB123CD",
            notes = "some arbitrary notes",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.active,
          ),
        )
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody()
        .jsonPath("title").isEqualTo("Bad Request")
        .jsonPath("invalid-params[0].errorType").isEqualTo("notUnique")
    }

    @Test
    fun `When a new premises is created with no notes then it defaults to empty`() {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB123CD",
            name = "Premises with no notes",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.active,
            probationDeliveryUnitId = probationDeliveryUnit.id,
          ),
        )
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody()
        .jsonPath("notes").isEqualTo("")
    }

    @Test
    fun `Trying to create a new premises without an address returns 400`() {
      webTestClient.post()
        .uri("/premises?service=temporary-accommodation")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            name = "arbitrary_test_name",
            postcode = "AB123CD",
            addressLine1 = "",
            addressLine2 = "Some district",
            town = "Somewhere",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            notes = "some notes",
            characteristicIds = mutableListOf(),
            status = PropertyStatus.active,
          ),
        )
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody()
        .jsonPath("title").isEqualTo("Bad Request")
        .jsonPath("invalid-params[0].errorType").isEqualTo("empty")
    }

    @Test
    fun `Trying to create a new premises without a postcode returns 400`() {
      webTestClient.post()
        .uri("/premises?service=temporary-accommodation")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            name = "arbitrary_test_name",
            postcode = "",
            addressLine1 = "FIRST LINE OF THE ADDRESS",
            addressLine2 = "Some district",
            town = "Somewhere",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            notes = "some notes",
            characteristicIds = mutableListOf(),
            status = PropertyStatus.active,
          ),
        )
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody()
        .jsonPath("title").isEqualTo("Bad Request")
        .jsonPath("invalid-params[0].errorType").isEqualTo("empty")
    }

    @Test
    fun `Trying to create a new premises without a service returns 400`() {
      webTestClient.post()
        .uri("/premises?service=temporary-accommodation")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewPremises(
            name = "arbitrary_test_name",
            postcode = "AB123CD",
            addressLine1 = "FIRST LINE OF THE ADDRESS",
            addressLine2 = "Some district",
            town = "Somewhere",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = UUID.fromString("c5acff6c-d0d2-4b89-9f4d-89a15cfa3891"),
            notes = "some notes",
            characteristicIds = mutableListOf(),
            status = PropertyStatus.active,
          ),
        )
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody()
        .jsonPath("title").isEqualTo("Bad Request")
        .jsonPath("invalid-params[0].errorType").isEqualTo("onlyCas3Supported")
    }

    @Test
    fun `Trying to create a new premises with an invalid local authority area id returns 400`() {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            name = "arbitrary_test_name",
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB456CD",
            notes = "some arbitrary notes",
            // not in db
            localAuthorityAreaId = UUID.fromString("878217f0-6db5-49d8-a5a1-c40fdecd6060"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.active,
          ),
        )
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody()
        .jsonPath("title").isEqualTo("Bad Request")
        .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
    }

    @Test
    fun `Trying to create a new Approved Premises with no local authority area id returns 400`() {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .bodyValue(
          NewPremises(
            name = "arbitrary_test_name",
            addressLine1 = "1 somewhere",
            postcode = "AB456CD",
            notes = "some arbitrary notes",
            localAuthorityAreaId = null,
            probationRegionId = UUID.fromString("c5acff6c-d0d2-4b89-9f4d-89a15cfa3891"),
            characteristicIds = mutableListOf(),
            status = PropertyStatus.active,
          ),
        )
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody()
        .jsonPath("title").isEqualTo("Bad Request")
        .jsonPath("invalid-params[0].errorType").isEqualTo("empty")
    }

    @Test
    fun `Trying to create a new Temporary Accommodation premises with a probation region that's not the user's region returns 403 Forbidden`() {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            name = "arbitrary_test_name",
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB456CD",
            notes = "some arbitrary notes",
            localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
            // not in db
            probationRegionId = UUID.fromString("48f96076-e911-4419-bceb-95a3e7f417eb"),
            characteristicIds = mutableListOf(),
            status = PropertyStatus.active,
          ),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Trying to create a Temporary Accommodation Premises without a PDU returns 400`() {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB123CD",
            notes = "some arbitrary notes",
            name = "premises without a pdu",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.pending,
            pdu = null,
            probationDeliveryUnitId = null,
          ),
        )
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody()
        .jsonPath("title").isEqualTo("Bad Request")
        .jsonPath("invalid-params[0].propertyName").isEqualTo("$.probationDeliveryUnitId")
        .jsonPath("invalid-params[0].errorType").isEqualTo("empty")
    }

    @Test
    fun `Trying to create a Temporary Accommodation Premises with an invalid probation delivery unit ID returns 400`() {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB123CD",
            notes = "some arbitrary notes",
            name = "premises with an invalid probation delivery unit",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.pending,
            pdu = null,
            probationDeliveryUnitId = UUID.randomUUID(),
          ),
        )
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody()
        .jsonPath("title").isEqualTo("Bad Request")
        .jsonPath("invalid-params[0].propertyName").isEqualTo("$.probationDeliveryUnitId")
        .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
    }

    @Test
    fun `Trying to create a Temporary Accommodation Premises with an invalid PDU name returns 400`() {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB123CD",
            notes = "some arbitrary notes",
            name = "invalid PDU",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.pending,
            pdu = "Non-existent PDU",
            probationDeliveryUnitId = null,
          ),
        )
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody()
        .jsonPath("title").isEqualTo("Bad Request")
        .jsonPath("invalid-params[0].propertyName").isEqualTo("$.pdu")
        .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
    }
  }

  @Nested
  inner class UpdatePremises : IntegrationTestBase() {
    @Test
    fun `When an Approved Premises is updated then all field data is persisted`() {
      givenAUser { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersistMultiple(1) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }
        val premisesToGet = premises[0]

        val rooms = roomEntityFactory.produceAndPersistMultiple(5) {
          withYieldedPremises { premisesToGet }
        }

        val bed1 = bedEntityFactory.produceAndPersist {
          withRoom(rooms[0])
        }

        val bed2 = bedEntityFactory.produceAndPersist {
          withRoom(rooms[1])
        }

        rooms[0].beds += bed1
        rooms[1].beds += bed2

        webTestClient.put()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              // Allerdale
              localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
              // North West
              probationRegionId = UUID.fromString("a02b7727-63aa-46f2-80f1-e0b05b31903c"),
              characteristicIds = mutableListOf(),
              status = PropertyStatus.archived,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("addressLine1").isEqualTo("1 somewhere updated")
          .jsonPath("addressLine2").isEqualTo("Some other district")
          .jsonPath("town").isEqualTo("Somewhere Else")
          .jsonPath("postcode").isEqualTo("AB456CD")
          .jsonPath("notes").isEqualTo("some arbitrary notes updated")
          .jsonPath("localAuthorityArea.id").isEqualTo("d1bd139b-7b90-4aae-87aa-9f93e183a7ff")
          .jsonPath("localAuthorityArea.name").isEqualTo("Allerdale")
          .jsonPath("probationRegion.id").isEqualTo("a02b7727-63aa-46f2-80f1-e0b05b31903c")
          .jsonPath("probationRegion.name").isEqualTo("North West")
          .jsonPath("status").isEqualTo("archived")
          .jsonPath("turnaroundWorkingDayCount").doesNotExist()

        assertApprovedPremisesBedSpaceIsNotArchived(premisesToGet)
      }
    }

    @Test
    fun `When a Temporary Accommodation Premises is updated then all field data is persisted`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        }

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(user.probationRegion)
          withYieldedProbationDeliveryUnit {
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(user.probationRegion)
            }
          }
        }

        val premisesToGet = premises[0]
        val rooms = roomEntityFactory.produceAndPersistMultiple(5) {
          withYieldedPremises { premisesToGet }
        }

        val bedEntityWithEndDate = bedEntityFactory.produceAndPersist {
          withRoom(rooms[0])
          withEndDate { LocalDate.now().plusDays(10) }
        }

        val bedEntityWithoutEndDate = bedEntityFactory.produceAndPersist {
          withRoom(rooms[1])
        }

        rooms[0].beds += bedEntityWithEndDate
        rooms[1].beds += bedEntityWithoutEndDate

        webTestClient.put()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              // Allerdale
              localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
              probationRegionId = user.probationRegion.id,
              characteristicIds = mutableListOf(),
              status = PropertyStatus.archived,
              probationDeliveryUnitId = probationDeliveryUnit.id,
              turnaroundWorkingDayCount = 5,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("addressLine1").isEqualTo("1 somewhere updated")
          .jsonPath("addressLine2").isEqualTo("Some other district")
          .jsonPath("town").isEqualTo("Somewhere Else")
          .jsonPath("postcode").isEqualTo("AB456CD")
          .jsonPath("notes").isEqualTo("some arbitrary notes updated")
          .jsonPath("localAuthorityArea.id").isEqualTo("d1bd139b-7b90-4aae-87aa-9f93e183a7ff")
          .jsonPath("localAuthorityArea.name").isEqualTo("Allerdale")
          .jsonPath("probationRegion.id").isEqualTo(user.probationRegion.id.toString())
          .jsonPath("probationRegion.name").isEqualTo(user.probationRegion.name)
          .jsonPath("status").isEqualTo("archived")
          .jsonPath("pdu").isEqualTo(probationDeliveryUnit.name)
          .jsonPath("probationDeliveryUnit.id").isEqualTo(probationDeliveryUnit.id.toString())
          .jsonPath("probationDeliveryUnit.name").isEqualTo(probationDeliveryUnit.name)
          .jsonPath("turnaroundWorkingDayCount").isEqualTo(5)

        assertTemporaryAccommodationPremisesBedSpaceIsArchived(premisesToGet)
      }
    }

    @Test
    fun `When a Temporary Accommodation Premises is updated with online status then respective bedspace enddate is not updated`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        }

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(user.probationRegion)
          withYieldedProbationDeliveryUnit {
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(user.probationRegion)
            }
          }
        }

        val premisesToGet = premises[0]
        val rooms = roomEntityFactory.produceAndPersistMultiple(5) {
          withYieldedPremises { premisesToGet }
        }

        val bedEntityWithEndDate = bedEntityFactory.produceAndPersist {
          withRoom(rooms[0])
          withEndDate { LocalDate.now() }
        }

        val bedEntityWithoutEndDate = bedEntityFactory.produceAndPersist {
          withRoom(rooms[1])
        }

        rooms[0].beds += bedEntityWithEndDate
        rooms[1].beds += bedEntityWithoutEndDate

        webTestClient.put()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              // Allerdale
              localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
              probationRegionId = user.probationRegion.id,
              characteristicIds = mutableListOf(),
              status = PropertyStatus.active,
              probationDeliveryUnitId = probationDeliveryUnit.id,
              turnaroundWorkingDayCount = 5,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("addressLine1").isEqualTo("1 somewhere updated")
          .jsonPath("addressLine2").isEqualTo("Some other district")
          .jsonPath("town").isEqualTo("Somewhere Else")
          .jsonPath("postcode").isEqualTo("AB456CD")
          .jsonPath("notes").isEqualTo("some arbitrary notes updated")
          .jsonPath("localAuthorityArea.id").isEqualTo("d1bd139b-7b90-4aae-87aa-9f93e183a7ff")
          .jsonPath("localAuthorityArea.name").isEqualTo("Allerdale")
          .jsonPath("probationRegion.id").isEqualTo(user.probationRegion.id.toString())
          .jsonPath("probationRegion.name").isEqualTo(user.probationRegion.name)
          .jsonPath("status").isEqualTo("active")
          .jsonPath("pdu").isEqualTo(probationDeliveryUnit.name)
          .jsonPath("probationDeliveryUnit.id").isEqualTo(probationDeliveryUnit.id.toString())
          .jsonPath("probationDeliveryUnit.name").isEqualTo(probationDeliveryUnit.name)
          .jsonPath("turnaroundWorkingDayCount").isEqualTo(5)

        val findById = temporaryAccommodationPremisesRepository.findById(premisesToGet.id)
        assertThat(findById).isNotNull()
        assertThat(findById.get().rooms).isNotEmpty()
        val actualBedWithEndDate = findById.get().rooms.flatMap { it.beds }.firstOrNull { it.id == bedEntityWithEndDate.id }
        val actualBedWithoutEndDate = findById.get().rooms.flatMap { it.beds }.firstOrNull { it.id == bedEntityWithoutEndDate.id }
        assertThat(actualBedWithEndDate!!.endDate).isEqualTo(bedEntityWithEndDate.endDate)
        assertThat(actualBedWithoutEndDate!!.endDate).isEqualTo(bedEntityWithoutEndDate.endDate)
      }
    }

    @Test
    fun `When a Temporary Accommodation Premises is updated with a legacy PDU name then all field data is persisted`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        }

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(user.probationRegion)
          withYieldedProbationDeliveryUnit {
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(user.probationRegion)
            }
          }
        }

        val premisesToGet = premises[0]

        webTestClient.put()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              // Allerdale
              localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
              probationRegionId = user.probationRegion.id,
              characteristicIds = mutableListOf(),
              status = PropertyStatus.archived,
              pdu = probationDeliveryUnit.name,
              turnaroundWorkingDayCount = 5,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("addressLine1").isEqualTo("1 somewhere updated")
          .jsonPath("addressLine2").isEqualTo("Some other district")
          .jsonPath("town").isEqualTo("Somewhere Else")
          .jsonPath("postcode").isEqualTo("AB456CD")
          .jsonPath("notes").isEqualTo("some arbitrary notes updated")
          .jsonPath("localAuthorityArea.id").isEqualTo("d1bd139b-7b90-4aae-87aa-9f93e183a7ff")
          .jsonPath("localAuthorityArea.name").isEqualTo("Allerdale")
          .jsonPath("probationRegion.id").isEqualTo(user.probationRegion.id.toString())
          .jsonPath("probationRegion.name").isEqualTo(user.probationRegion.name)
          .jsonPath("status").isEqualTo("archived")
          .jsonPath("pdu").isEqualTo(probationDeliveryUnit.name)
          .jsonPath("probationDeliveryUnit.id").isEqualTo(probationDeliveryUnit.id.toString())
          .jsonPath("probationDeliveryUnit.name").isEqualTo(probationDeliveryUnit.name)
          .jsonPath("turnaroundWorkingDayCount").isEqualTo(5)
      }
    }

    @Test
    fun `Updating a Temporary Accommodation Premises that's not in the user's region returns 403 Forbidden`() {
      givenAUser { user, jwt ->
        val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        }

        val otherProbationRegion = givenAProbationRegion()

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(otherProbationRegion)
          withYieldedProbationDeliveryUnit {
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(otherProbationRegion)
            }
          }
        }

        val premisesToGet = premises[0]

        webTestClient.put()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              // Allerdale
              localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
              // North West
              probationRegionId = UUID.fromString("a02b7727-63aa-46f2-80f1-e0b05b31903c"),
              characteristicIds = mutableListOf(),
              status = PropertyStatus.archived,
              probationDeliveryUnitId = probationDeliveryUnit.id,
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Trying to update a premises with an invalid local authority area id returns 400`() {
      givenAUser { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersistMultiple(1) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val premisesToGet = premises[0]

        webTestClient.put()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              // not in db
              localAuthorityAreaId = UUID.fromString("878217f0-6db5-49d8-a5a1-c40fdecd6060"),
              probationRegionId = UUID.fromString("c5acff6c-d0d2-4b89-9f4d-89a15cfa3891"),
              characteristicIds = mutableListOf(),
              status = PropertyStatus.active,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
      }
    }

    @Test
    fun `Trying to update an Approved Premises with no local authority area id returns 400`() {
      givenAUser { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersistMultiple(1) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val premisesToGet = premises[0]

        webTestClient.put()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              localAuthorityAreaId = null,
              probationRegionId = UUID.fromString("c5acff6c-d0d2-4b89-9f4d-89a15cfa3891"),
              characteristicIds = mutableListOf(),
              status = PropertyStatus.active,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("empty")
      }
    }

    @Test
    fun `Trying to update a premises with an invalid probation region id returns 400`() {
      givenAUser { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersistMultiple(1) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val premisesToGet = premises[0]

        webTestClient.put()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
              // not in db
              probationRegionId = UUID.fromString("48f96076-e911-4419-bceb-95a3e7f417eb"),
              characteristicIds = mutableListOf(),
              status = PropertyStatus.active,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
      }
    }

    @Test
    fun `Trying to update a Temporary Accommodation Premises without a PDU returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val premisesToGet = premises[0]

        webTestClient.put()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              // Allerdale
              localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
              // North West
              probationRegionId = UUID.fromString("a02b7727-63aa-46f2-80f1-e0b05b31903c"),
              characteristicIds = mutableListOf(),
              status = PropertyStatus.archived,
              pdu = null,
              probationDeliveryUnitId = null,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].propertyName").isEqualTo("$.probationDeliveryUnitId")
          .jsonPath("invalid-params[0].errorType").isEqualTo("empty")
      }
    }

    @Test
    fun `Trying to update a Temporary Accommodation Premises with an invalid PDU name returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val premisesToGet = premises[0]

        webTestClient.put()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              // Allerdale
              localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
              // North West
              probationRegionId = UUID.fromString("a02b7727-63aa-46f2-80f1-e0b05b31903c"),
              characteristicIds = mutableListOf(),
              status = PropertyStatus.archived,
              pdu = "Non-existent PDU",
              probationDeliveryUnitId = null,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].propertyName").isEqualTo("$.pdu")
          .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
      }
    }

    @ParameterizedTest
    @ValueSource(ints = [-1, -5, -10])
    fun `Trying to update a Temporary Accommodation Premises with invalid turnaround working day count = {0} returns 400 and errorType is isNotAPositiveInteger`(
      turnaroundWorkingDayCount: Int,
    ) {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        }

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(user.probationRegion)
          withYieldedProbationDeliveryUnit {
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(user.probationRegion)
            }
          }
        }

        val premisesToGet = premises[0]

        webTestClient.put()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              // Allerdale
              localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
              probationRegionId = user.probationRegion.id,
              characteristicIds = mutableListOf(),
              status = PropertyStatus.archived,
              probationDeliveryUnitId = probationDeliveryUnit.id,
              turnaroundWorkingDayCount = turnaroundWorkingDayCount,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].propertyName").isEqualTo("$.turnaroundWorkingDayCount")
          .jsonPath("invalid-params[0].errorType").isEqualTo("isNotAPositiveInteger")
      }
    }

    @Test
    fun `Trying to update a Temporary Accommodation Premises with an invalid probation delivery unit ID returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val premisesToGet = premises[0]

        webTestClient.put()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              // Allerdale
              localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
              // North West
              probationRegionId = UUID.fromString("a02b7727-63aa-46f2-80f1-e0b05b31903c"),
              characteristicIds = mutableListOf(),
              status = PropertyStatus.archived,
              pdu = null,
              probationDeliveryUnitId = UUID.randomUUID(),
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].propertyName").isEqualTo("$.probationDeliveryUnitId")
          .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
      }
    }

    @Test
    fun `Trying to update the name of an Approved Premises has no effect`() {
      givenAUser { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
          withName("old-premises-name")
        }

        webTestClient.put()
          .uri("/premises/${premises.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              // Allerdale
              localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
              // North West
              probationRegionId = UUID.fromString("a02b7727-63aa-46f2-80f1-e0b05b31903c"),
              characteristicIds = mutableListOf(),
              status = PropertyStatus.archived,
              name = "new-premises-name",
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("addressLine1").isEqualTo("1 somewhere updated")
          .jsonPath("addressLine2").isEqualTo("Some other district")
          .jsonPath("town").isEqualTo("Somewhere Else")
          .jsonPath("postcode").isEqualTo("AB456CD")
          .jsonPath("notes").isEqualTo("some arbitrary notes updated")
          .jsonPath("localAuthorityArea.id").isEqualTo("d1bd139b-7b90-4aae-87aa-9f93e183a7ff")
          .jsonPath("localAuthorityArea.name").isEqualTo("Allerdale")
          .jsonPath("probationRegion.id").isEqualTo("a02b7727-63aa-46f2-80f1-e0b05b31903c")
          .jsonPath("probationRegion.name").isEqualTo("North West")
          .jsonPath("status").isEqualTo("archived")
          .jsonPath("turnaroundWorkingDayCount").doesNotExist()
          .jsonPath("name").isEqualTo("old-premises-name")
      }
    }

    @Test
    fun `Updating a Temporary Accommodation premises does not change the name when it's not provided`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        }

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(user.probationRegion)
          withYieldedProbationDeliveryUnit {
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(user.probationRegion)
            }
          }
          withName("old-premises-name")
        }

        val premisesToGet = premises[0]

        webTestClient.put()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              // Allerdale
              localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
              probationRegionId = user.probationRegion.id,
              characteristicIds = mutableListOf(),
              status = PropertyStatus.archived,
              probationDeliveryUnitId = probationDeliveryUnit.id,
              turnaroundWorkingDayCount = 5,
              name = null,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("addressLine1").isEqualTo("1 somewhere updated")
          .jsonPath("addressLine2").isEqualTo("Some other district")
          .jsonPath("town").isEqualTo("Somewhere Else")
          .jsonPath("postcode").isEqualTo("AB456CD")
          .jsonPath("notes").isEqualTo("some arbitrary notes updated")
          .jsonPath("localAuthorityArea.id").isEqualTo("d1bd139b-7b90-4aae-87aa-9f93e183a7ff")
          .jsonPath("localAuthorityArea.name").isEqualTo("Allerdale")
          .jsonPath("probationRegion.id").isEqualTo(user.probationRegion.id.toString())
          .jsonPath("probationRegion.name").isEqualTo(user.probationRegion.name)
          .jsonPath("status").isEqualTo("archived")
          .jsonPath("pdu").isEqualTo(probationDeliveryUnit.name)
          .jsonPath("probationDeliveryUnit.id").isEqualTo(probationDeliveryUnit.id.toString())
          .jsonPath("probationDeliveryUnit.name").isEqualTo(probationDeliveryUnit.name)
          .jsonPath("turnaroundWorkingDayCount").isEqualTo(5)
          .jsonPath("name").isEqualTo("old-premises-name")
      }
    }

    @Test
    fun `Updating a Temporary Accommodation premises changes the name when it's provided`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        }

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(user.probationRegion)
          withYieldedProbationDeliveryUnit {
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(user.probationRegion)
            }
          }
          withName("old-premises-name")
        }

        val premisesToGet = premises[0]

        webTestClient.put()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              // Allerdale
              localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
              probationRegionId = user.probationRegion.id,
              characteristicIds = mutableListOf(),
              status = PropertyStatus.archived,
              probationDeliveryUnitId = probationDeliveryUnit.id,
              turnaroundWorkingDayCount = 5,
              name = "new-premises-name",
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("addressLine1").isEqualTo("1 somewhere updated")
          .jsonPath("addressLine2").isEqualTo("Some other district")
          .jsonPath("town").isEqualTo("Somewhere Else")
          .jsonPath("postcode").isEqualTo("AB456CD")
          .jsonPath("notes").isEqualTo("some arbitrary notes updated")
          .jsonPath("localAuthorityArea.id").isEqualTo("d1bd139b-7b90-4aae-87aa-9f93e183a7ff")
          .jsonPath("localAuthorityArea.name").isEqualTo("Allerdale")
          .jsonPath("probationRegion.id").isEqualTo(user.probationRegion.id.toString())
          .jsonPath("probationRegion.name").isEqualTo(user.probationRegion.name)
          .jsonPath("status").isEqualTo("archived")
          .jsonPath("pdu").isEqualTo(probationDeliveryUnit.name)
          .jsonPath("probationDeliveryUnit.id").isEqualTo(probationDeliveryUnit.id.toString())
          .jsonPath("probationDeliveryUnit.name").isEqualTo(probationDeliveryUnit.name)
          .jsonPath("turnaroundWorkingDayCount").isEqualTo(5)
          .jsonPath("name").isEqualTo("new-premises-name")
      }
    }

    private fun assertApprovedPremisesBedSpaceIsNotArchived(premisesToGet: ApprovedPremisesEntity) {
      val approvedPremises = approvedPremisesRepository.findById(premisesToGet.id)
      assertThat(approvedPremises).isNotNull()
      assertThat(approvedPremises.get().rooms).isNotEmpty()
      approvedPremises.get().rooms.forEach { room ->
        room.beds.forEach { bed ->
          assertThat(bed.endDate).isNull()
        }
      }
    }

    private fun assertTemporaryAccommodationPremisesBedSpaceIsArchived(premisesToGet: TemporaryAccommodationPremisesEntity) {
      val temporaryAccommodationPremise = temporaryAccommodationPremisesRepository.findById(premisesToGet.id)
      assertThat(temporaryAccommodationPremise).isNotNull()
      assertThat(temporaryAccommodationPremise.get().rooms).isNotEmpty()
      temporaryAccommodationPremise.get().rooms.forEach { room ->
        room.beds.forEach { bed ->
          assertThat(bed.endDate).isEqualTo(LocalDate.now())
        }
      }
    }
  }

  @Nested
  inner class GetAllPremises : InitialiseDatabasePerClassTestBase() {
    @Autowired
    lateinit var premisesTransformer: PremisesTransformer

    private lateinit var user: UserEntity
    private lateinit var region: ProbationRegionEntity
    private lateinit var jwt: String
    private lateinit var cas1Premises: MutableMap<ProbationRegionEntity, List<ApprovedPremisesEntity>>
    private lateinit var cas3Premises: MutableMap<ProbationRegionEntity, List<TemporaryAccommodationPremisesEntity>>

    @BeforeAll
    fun setup() {
      val userArgs = givenAUser()

      user = userArgs.first
      jwt = userArgs.second
      region = user.probationRegion

      cas1Premises = mutableMapOf()
      cas3Premises = mutableMapOf()

      cas3Premises[region] = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(5) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { region }
        withService("CAS3")
        withYieldedProbationDeliveryUnit {
          probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(region)
          }
        }
      }.onEach {
        addRoomsAndBeds(it, roomCount = 2, bedsPerRoom = 10)
        addRoomsAndBeds(it, roomCount = 1, bedsPerRoom = 1, isActive = false)
      }

      cas1Premises[region] = approvedPremisesEntityFactory.produceAndPersistMultiple(5) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { region }
        withService("CAS1")
      }.onEach {
        addRoomsAndBeds(it, roomCount = 2, 10)
        addRoomsAndBeds(it, roomCount = 1, bedsPerRoom = 1, isActive = false)
      }

      // Add some extra premises in both services for other regions that shouldn't be returned
      val otherRegion = givenAProbationRegion()

      cas3Premises[otherRegion] = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(5) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { otherRegion }
        withService("CAS3")
        withYieldedProbationDeliveryUnit {
          probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(otherRegion)
          }
        }
      }.onEach {
        addRoomsAndBeds(it, roomCount = 2, 10)
        addRoomsAndBeds(it, roomCount = 1, bedsPerRoom = 1, isActive = false)
      }

      cas1Premises[otherRegion] = approvedPremisesEntityFactory.produceAndPersistMultiple(5) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { otherRegion }
        withService("CAS1")
      }.onEach {
        addRoomsAndBeds(it, roomCount = 1, 20)
        addRoomsAndBeds(it, roomCount = 1, bedsPerRoom = 1, isActive = false)
      }
    }

    @Test
    fun `Get all Premises returns OK with correct body`() {
      val premises = mutableListOf<PremisesEntity>()
      premises.addAll(cas1Premises.values.flatten())
      premises.addAll(cas3Premises.values.flatten())

      val expectedJson = objectMapper.writeValueAsString(
        premises.map {
          premisesTransformer.transformJpaToApi(it, 20, 20)
        },
      )

      webTestClient.get()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(expectedJson)
    }

    @Test
    fun `Get Premises for CAS1 returns OK with correct body`() {
      val expectedJson = objectMapper.writeValueAsString(
        cas1Premises.values.flatten().map {
          premisesTransformer.transformJpaToApi(it, 20, 20)
        },
      )

      webTestClient.get()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", "approved-premises")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(expectedJson)
    }

    @Test
    fun `Get Premises for all regions in CAS3 returns 403 Forbidden`() {
      webTestClient.get()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", "temporary-accommodation")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Get Premises in CAS3 for a region that's not the user's region returns 403 Forbidden`() {
      webTestClient.get()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", "temporary-accommodation")
        .header("X-User-Region", UUID.randomUUID().toString())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Get Premises for a single region returns OK with correct body`() {
      val premises = mutableListOf<PremisesEntity>()
      premises.addAll(cas1Premises[region]!!.toList())
      premises.addAll(cas3Premises[region]!!.toList())

      val expectedJson = objectMapper.writeValueAsString(
        premises.map {
          premisesTransformer.transformJpaToApi(it, 20, 20)
        },
      )

      webTestClient.get()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-User-Region", "${region.id}")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(expectedJson)
    }

    @Test
    fun `Get Premises for a single region and particular service returns OK with correct body`() {
      val expectedJson = objectMapper.writeValueAsString(
        cas3Premises[region]!!.map {
          premisesTransformer.transformJpaToApi(it, 20, 20)
        },
      )

      webTestClient.get()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", "temporary-accommodation")
        .header("X-User-Region", "${user.probationRegion.id}")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(expectedJson)
    }
  }

  @Nested
  inner class GetPremisesById : IntegrationTestBase() {
    @Autowired
    lateinit var premisesTransformer: PremisesTransformer

    @Test
    fun `Get Premises by ID returns OK with correct body`() {
      givenAUser { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersistMultiple(5) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }.onEach {
          addRoomsAndBeds(it, roomCount = 4, 5)
          addRoomsAndBeds(it, roomCount = 1, bedsPerRoom = 1, isActive = false)
        }

        val premisesToGet = premises[2]
        val expectedJson = objectMapper.writeValueAsString(premisesTransformer.transformJpaToApi(premises[2], 20, 20))

        webTestClient.get()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }

    @Test
    fun `Get Premises by ID returns OK with correct body when capacity is used`() {
      givenAUser { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersistMultiple(5) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }.onEach {
          addRoomsAndBeds(it, roomCount = 4, 5)
          addRoomsAndBeds(it, roomCount = 1, bedsPerRoom = 1, isActive = false)
        }

        val keyWorker = ContextStaffMemberFactory().produce()
        premises.forEach {
          apDeliusContextMockSuccessfulStaffMembersCall(keyWorker, it.qCode)
        }

        bookingEntityFactory.produceAndPersist {
          withPremises(premises[2])
          withArrivalDate(LocalDate.now().minusDays(2))
          withDepartureDate(LocalDate.now().plusDays(4))
          withStaffKeyWorkerCode(keyWorker.code)
        }

        val premisesToGet = premises[2]
        val expectedJson = objectMapper.writeValueAsString(premisesTransformer.transformJpaToApi(premises[2], 20, 19))

        webTestClient.get()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }

    @Test
    fun `Get Premises by ID returns Not Found with correct body`() {
      val idToRequest = UUID.randomUUID().toString()

      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.get()
        .uri("/premises/$idToRequest")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectHeader().contentType("application/problem+json")
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("title").isEqualTo("Not Found")
        .jsonPath("status").isEqualTo(404)
        .jsonPath("detail").isEqualTo("No Premises with an ID of $idToRequest could be found")
    }

    @Test
    fun `Get Temporary Accommodation Premises by ID for a premises not in the user's region returns 403 Forbidden`() {
      givenAUser { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(5) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val premisesToGet = premises[2]

        webTestClient.get()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `The total bedspaces on a Temporary Accommodation Premises is equal to the sum of the bedspaces in all Rooms attached to the Premises`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }.also {
          addRoomsAndBeds(it, roomCount = 2, bedsPerRoom = 5)
          addRoomsAndBeds(it, roomCount = 1, bedsPerRoom = 1, isActive = false)
        }

        webTestClient.get()
          .uri("/premises/${premises.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.bedCount").isEqualTo(10)
      }
    }
  }

  @Nested
  inner class GetPremisesStaff : IntegrationTestBase() {
    @Autowired
    lateinit var staffMemberTransformer: StaffMemberTransformer

    @Test
    fun `Get Premises Staff without JWT returns 401`() {
      webTestClient.get()
        .uri("/premises/e0f03aa2-1468-441c-aa98-0b98d86b67f9/staff")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = [ "CAS1_FUTURE_MANAGER", "CAS1_MATCHER" ])
    fun `Get premises staff where delius team cannot be found returns 404`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        val qCode = "NON_EXISTENT_TEAM_QCODE"

        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
          withQCode(qCode)
        }

        wiremockServer.stubFor(
          WireMock.get(urlEqualTo("/secure/teams/$qCode/staff"))
            .willReturn(
              WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(404),
            ),
        )

        webTestClient.get()
          .uri("/premises/${premises.id}/staff")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("$.detail").isEqualTo("No Team with an ID of NON_EXISTENT_TEAM_QCODE could be found")
      }
    }

    @Test
    fun `Get Premises Staff for Temporary Accommodation Premises returns 501`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(user.probationRegion)
        }

        webTestClient.get()
          .uri("/premises/${premises.id}/staff")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isEqualTo(HttpStatus.NOT_IMPLEMENTED)
      }
    }

    @Test
    fun `Get Premises Staff for Temporary Accommodation Premises not in the user's region returns 403 Forbidden`() {
      givenAUser { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        webTestClient.get()
          .uri("/premises/${premises.id}/staff")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = [ "CAS1_FUTURE_MANAGER", "CAS1_MATCHER" ])
    fun `Get Approved Premises Staff for Approved Premises returns 200 with correct body when user has one of roles FUTURE_MANAGER, MATCHER`(role: UserRole) {
      givenAUser(roles = listOf(role)) { userEntity, jwt ->
        val qCode = "FOUND"

        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
          withQCode(qCode)
        }

        val staffMembers = listOf(
          ContextStaffMemberFactory().produce(),
          ContextStaffMemberFactory().produce(),
          ContextStaffMemberFactory().produce(),
          ContextStaffMemberFactory().produce(),
          ContextStaffMemberFactory().produce(),
        )

        wiremockServer.stubFor(
          WireMock.get(urlEqualTo("/approved-premises/$qCode/staff"))
            .willReturn(
              WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
                .withBody(
                  objectMapper.writeValueAsString(
                    StaffMembersPage(
                      content = staffMembers,
                    ),
                  ),
                ),
            ),
        )

        webTestClient.get()
          .uri("/premises/${premises.id}/staff")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              staffMembers.map(staffMemberTransformer::transformDomainToApi),
            ),
          )
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = [ "CAS1_FUTURE_MANAGER", "CAS1_MATCHER" ])
    @Disabled
    fun `Get Approved Premises Staff caches response when user has one of roles FUTURE_MANAGER, MATCHER`(role: UserRole) {
      givenAUser(roles = listOf(role)) { userEntity, jwt ->
        val qCode = "FOUND"

        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
          withQCode(qCode)
        }

        val staffMembers = listOf(
          ContextStaffMemberFactory().produce(),
          ContextStaffMemberFactory().produce(),
          ContextStaffMemberFactory().produce(),
          ContextStaffMemberFactory().produce(),
          ContextStaffMemberFactory().produce(),
        )

        wiremockServer.stubFor(
          WireMock.get(urlEqualTo("/approved-premises/$qCode/staff"))
            .willReturn(
              WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
                .withBody(
                  objectMapper.writeValueAsString(
                    StaffMembersPage(
                      content = staffMembers,
                    ),
                  ),
                ),
            ),
        )

        repeat(2) {
          webTestClient.get()
            .uri("/premises/${premises.id}/staff")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                staffMembers.map(staffMemberTransformer::transformDomainToApi),
              ),
            )
        }

        wiremockServer.verify(exactly(1), getRequestedFor(urlEqualTo("/approved-premises/$qCode/staff")))
      }
    }
  }

  @Nested
  inner class GetPremisesSummary : InitialiseDatabasePerClassTestBase() {
    @Autowired
    lateinit var bookingTransformer: BookingTransformer

    private lateinit var premises: ApprovedPremisesEntity
    private lateinit var lostBeds: List<LostBedsEntity>
    private lateinit var rooms: List<RoomEntity>
    private lateinit var bookings: List<BookingEntity>

    private var totalBeds = 20
    private var startDate = LocalDate.now()

    @BeforeAll
    fun setup() {
      premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { givenAProbationRegion() }
      }

      rooms = addRoomsAndBeds(premises, roomCount = 4, 5)
      addRoomsAndBeds(premises, roomCount = 1, bedsPerRoom = 1, isActive = false)

      val pendingBookingEntity = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withArrivalDate(startDate.plusDays(1))
        withDepartureDate(startDate.plusDays(3))
        withStaffKeyWorkerCode(null)
      }

      lostBeds = mutableListOf(
        lostBedsEntityFactory.produceAndPersist {
          withPremises(premises)
          withStartDate(startDate.plusDays(1))
          withEndDate(startDate.plusDays(2))
          withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
          withBed(rooms[0].beds[0])
        },
        lostBedsEntityFactory.produceAndPersist {
          withPremises(premises)
          withStartDate(startDate.plusDays(1))
          withEndDate(startDate.plusDays(2))
          withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
          withBed(rooms[1].beds[0])
        },
      )

      val cancelledLostBed = lostBedsEntityFactory.produceAndPersist {
        withPremises(premises)
        withStartDate(startDate.plusDays(1))
        withEndDate(startDate.plusDays(2))
        withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
        withBed(rooms[2].beds[0])
      }

      cancelledLostBed.cancellation = lostBedCancellationEntityFactory.produceAndPersist {
        withLostBed(cancelledLostBed)
      }

      lostBeds += cancelledLostBed

      val arrivedBookingEntity = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withArrivalDate(startDate)
        withDepartureDate(startDate.plusDays(2))
        withStaffKeyWorkerCode(null)
        withBed(rooms.first().beds.first())
      }

      arrivedBookingEntity.arrivals = mutableListOf(
        arrivalEntityFactory.produceAndPersist {
          withBooking(arrivedBookingEntity)
        },
      )

      val nonArrivedBookingEntity = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withArrivalDate(startDate.plusDays(3))
        withDepartureDate(startDate.plusDays(5))
        withStaffKeyWorkerCode(null)
        withBed(rooms.first().beds.first())
      }

      nonArrivedBookingEntity.nonArrival = nonArrivalEntityFactory.produceAndPersist {
        withBooking(nonArrivedBookingEntity)
        withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
      }

      val cancelledBookingEntity = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withArrivalDate(startDate.plusDays(4))
        withDepartureDate(startDate.plusDays(6))
        withStaffKeyWorkerCode(null)
        withBed(rooms.first().beds.first())
      }

      cancelledBookingEntity.cancellations = mutableListOf(
        cancellationEntityFactory.produceAndPersist {
          withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
          withBooking(cancelledBookingEntity)
        },
      )

      bookings = listOf(
        pendingBookingEntity,
        arrivedBookingEntity,
        nonArrivedBookingEntity,
        cancelledBookingEntity,
      )
    }

    @Test
    fun `Get Premises Summary without JWT returns 401`() {
      webTestClient.get()
        .uri("/premises/e0f03aa2-1468-441c-aa98-0b98d86b67f9/summary")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = [ "CAS1_FUTURE_MANAGER" ])
    fun `Get Premises Summary by ID that does not exist returns 404`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        val id = UUID.randomUUID()
        webTestClient.get()
          .uri("/premises/$id/summary")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_FUTURE_MANAGER", "CAS1_MATCHER", "CAS1_WORKFLOW_MANAGER"])
    fun `Get Premises Summary by ID returns OK with correct body`(role: UserRole) {
      givenAUser(roles = listOf(role)) { user, jwt ->
        bookings.forEach {
          apDeliusContextAddCaseSummaryToBulkResponse(
            CaseSummaryFactory()
              .withCrn(it.crn)
              .produce(),
          )
          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(it.crn)
                .produce(),
            ),
            user.deliusUsername,
          )
        }

        webTestClient.get()
          .uri("/premises/${premises.id}/summary")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.id").isEqualTo("${premises.id}")
          .jsonPath("$.name").isEqualTo(premises.name)
          .jsonPath("$.apCode").isEqualTo(premises.apCode)
          .jsonPath("$.postcode").isEqualTo(premises.postcode)
          .jsonPath("$.bedCount").isEqualTo("20")
          .jsonPath("$.availableBedsForToday").isEqualTo("19")
          .jsonPath("$.bookings").isArray
          .jsonPath("$.bookings[0].id").isEqualTo(bookings[0].id.toString())
          .jsonPath("$.bookings[0].arrivalDate").isEqualTo(bookings[0].arrivalDate.toString())
          .jsonPath("$.bookings[0].departureDate").isEqualTo(bookings[0].departureDate.toString())
          .jsonPath("$.bookings[0].person.crn").isEqualTo(bookings[0].crn)
          .jsonPath("$.bookings[0].bed").isEmpty()
          .jsonPath("$.bookings[0].status").isEqualTo(BookingStatus.awaitingMinusArrival.value)
          .jsonPath("$.bookings[1].id").isEqualTo(bookings[1].id.toString())
          .jsonPath("$.bookings[1].arrivalDate").isEqualTo(bookings[1].arrivalDate.toString())
          .jsonPath("$.bookings[1].departureDate").isEqualTo(bookings[1].departureDate.toString())
          .jsonPath("$.bookings[1].person.crn").isEqualTo(bookings[1].crn)
          .jsonPath("$.bookings[1].bed.id").isEqualTo(bookings[1].bed!!.id.toString())
          .jsonPath("$.bookings[1].status").isEqualTo(BookingStatus.arrived.value)
          .jsonPath("$.bookings[2].id").isEqualTo(bookings[2].id.toString())
          .jsonPath("$.bookings[2].arrivalDate").isEqualTo(bookings[2].arrivalDate.toString())
          .jsonPath("$.bookings[2].departureDate").isEqualTo(bookings[2].departureDate.toString())
          .jsonPath("$.bookings[2].person.crn").isEqualTo(bookings[2].crn)
          .jsonPath("$.bookings[2].bed.id").isEqualTo(bookings[2].bed!!.id.toString())
          .jsonPath("$.bookings[2].status").isEqualTo(BookingStatus.notMinusArrived.value)
          .jsonPath("$.bookings[3].id").isEqualTo(bookings[3].id.toString())
          .jsonPath("$.bookings[3].arrivalDate").isEqualTo(bookings[3].arrivalDate.toString())
          .jsonPath("$.bookings[3].departureDate").isEqualTo(bookings[3].departureDate.toString())
          .jsonPath("$.bookings[3].person.crn").isEqualTo(bookings[3].crn)
          .jsonPath("$.bookings[3].bed.id").isEqualTo(bookings[3].bed!!.id.toString())
          .jsonPath("$.bookings[3].status").isEqualTo(BookingStatus.cancelled.value)
          .jsonPath("$.dateCapacities").isArray
          .jsonPath("$.dateCapacities[0]").isNotEmpty
      }
    }

    @Test
    fun `Get Premises Summary by ID returns the correct capacity data`() {
      givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        bookings.forEach {
          apDeliusContextAddCaseSummaryToBulkResponse(
            CaseSummaryFactory()
              .withCrn(it.crn)
              .produce(),
          )
          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(it.crn)
                .produce(),
            ),
            user.deliusUsername,
          )
        }

        val rawResponseBody = webTestClient.get()
          .uri("/premises/${premises.id}/summary")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk()
          .returnResult<String>()
          .responseBody
          .blockFirst()

        val responseBody =
          objectMapper.readValue(
            rawResponseBody,
            object : TypeReference<ExtendedPremisesSummary>() {},
          )

        val getTotalBedsForDate = { date: LocalDate ->
          val lostBedsForToday = lostBeds.filter { it.startDate <= date && it.endDate > date && it.cancellation == null }
          val bookingsForToday = bookings
            .filter { it.cancellation == null && it.nonArrival == null }
            .filter { it.arrivalDate <= date && it.departureDate > date }
          (totalBeds - bookingsForToday.count()) - lostBedsForToday.count()
        }

        assertThat(responseBody.dateCapacities?.get(0)).isEqualTo(
          DateCapacity(date = startDate, getTotalBedsForDate(startDate)),
        )
        assertThat(responseBody.dateCapacities?.get(1)).isEqualTo(
          DateCapacity(date = startDate.plusDays(1), getTotalBedsForDate(startDate.plusDays(1))),
        )
        assertThat(responseBody.dateCapacities?.get(2)).isEqualTo(
          DateCapacity(date = startDate.plusDays(2), getTotalBedsForDate(startDate.plusDays(2))),
        )
        assertThat(responseBody.dateCapacities?.get(3)).isEqualTo(
          DateCapacity(date = startDate.plusDays(3), getTotalBedsForDate(startDate.plusDays(3))),
        )
        assertThat(responseBody.dateCapacities?.get(4)).isEqualTo(
          DateCapacity(date = startDate.plusDays(4), getTotalBedsForDate(startDate.plusDays(4))),
        )
        assertThat(responseBody.dateCapacities?.get(5)).isEqualTo(
          DateCapacity(date = startDate.plusDays(5), getTotalBedsForDate(startDate.plusDays(5))),
        )
      }
    }
  }

  @Nested
  inner class GetRoomsForPremises : IntegrationTestBase() {
    @Autowired
    lateinit var roomTransformer: RoomTransformer

    @Test
    fun `Get all Rooms for Premises returns OK with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }
        val rooms = roomEntityFactory.produceAndPersistMultiple(5) {
          withYieldedPremises { premises }
        }

        val expectedJson = objectMapper.writeValueAsString(
          rooms.map {
            roomTransformer.transformJpaToApi(it)
          },
        )

        webTestClient.get()
          .uri("/premises/${premises.id}/rooms")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }

    @Test
    fun `Get all Rooms for a Temporary Accommodation Premises that's not in the user's region returns 403 Forbidden`() {
      givenAUser { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }
        val rooms = roomEntityFactory.produceAndPersistMultiple(5) {
          withYieldedPremises { premises }
        }

        val expectedJson = objectMapper.writeValueAsString(
          rooms.map {
            roomTransformer.transformJpaToApi(it)
          },
        )

        webTestClient.get()
          .uri("/premises/${premises.id}/rooms")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get all Rooms for Premises returns OK with correct body with end-date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }
        val rooms = roomEntityFactory.produceAndPersistMultiple(5) {
          withYieldedPremises { premises }
        }

        val bedEntityWithEndDate = bedEntityFactory.produceAndPersist {
          withRoom(rooms[0])
          withEndDate { LocalDate.now() }
        }

        val bedEntityWithoutEndDate = bedEntityFactory.produceAndPersist {
          withRoom(rooms[1])
        }

        rooms[0].beds += bedEntityWithEndDate
        rooms[1].beds += bedEntityWithoutEndDate

        val expectedJson = objectMapper.writeValueAsString(
          rooms.map {
            roomTransformer.transformJpaToApi(it)
          },
        )

        webTestClient.get()
          .uri("/premises/${premises.id}/rooms")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }
  }

  @Nested
  inner class CreateRoomForPremises : IntegrationTestBase() {
    @Test
    fun `Create new Room for Premises returns 201 Created with correct body when given valid data`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        webTestClient.post()
          .uri("/premises/${premises.id}/rooms")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewRoom(
              notes = "test notes",
              name = "test-room",
              characteristicIds = characteristicIds,
            ),
          )
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath("name").isEqualTo("test-room")
          .jsonPath("notes").isEqualTo("test notes")
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { "temporary-accommodation" })
          .jsonPath("characteristics[*].name").isEqualTo(MutableList(5) { "Floor level access" })
      }
    }

    @Test
    fun `When a new room is created with no notes then it defaults to empty`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        webTestClient.post()
          .uri("/premises/${premises.id}/rooms")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewRoom(
              notes = null,
              name = "test-room",
              characteristicIds = mutableListOf(),
            ),
          )
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath("notes").isEqualTo("")
      }
    }

    @Test
    fun `Create new Room with end date for temporary accommodation Premises returns 201 Created with correct body when given valid data`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        webTestClient.post()
          .uri("/premises/${premises.id}/rooms")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewRoom(
              notes = "test notes",
              name = "test-room",
              characteristicIds = characteristicIds,
              bedEndDate = LocalDate.now(),
            ),
          )
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath("name").isEqualTo("test-room")
          .jsonPath("notes").isEqualTo("test notes")
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { "temporary-accommodation" })
          .jsonPath("characteristics[*].name").isEqualTo(MutableList(5) { "Floor level access" })
          .jsonPath("beds[*].bedEndDate").isEqualTo(LocalDate.now().toString())
      }
    }

    @Test
    fun `Trying to create a room without a name returns 400`() {
      givenAUser { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        webTestClient.post()
          .uri("/premises/${premises.id}/rooms")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewRoom(
              notes = "test notes",
              name = "",
              characteristicIds = mutableListOf(),
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("empty")
      }
    }

    @Test
    fun `Trying to create a room with an unknown characteristic returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        webTestClient.post()
          .uri("/premises/${premises.id}/rooms")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewRoom(
              notes = "test notes",
              name = "test-room",
              characteristicIds = mutableListOf(UUID.randomUUID()),
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
      }
    }

    @Test
    fun `Trying to create a room with a characteristic of the wrong service scope returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val characteristicId = characteristicEntityFactory.produceAndPersist {
          withModelScope("room")
          withServiceScope("approved-premises")
        }.id

        webTestClient.post()
          .uri("/premises/${premises.id}/rooms")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewRoom(
              notes = "test notes",
              name = "test-room",
              characteristicIds = mutableListOf(characteristicId),
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("incorrectCharacteristicServiceScope")
      }
    }

    @Test
    fun `Trying to create a room with a characteristic of the wrong model scope returns 400`() {
      givenAUser { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val characteristicId = characteristicEntityFactory.produceAndPersist {
          withModelScope("premises")
          withServiceScope("approved-premises")
        }.id

        webTestClient.post()
          .uri("/premises/${premises.id}/rooms")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewRoom(
              notes = "test notes",
              name = "test-room",
              characteristicIds = mutableListOf(characteristicId),
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("incorrectCharacteristicModelScope")
      }
    }

    @Test
    fun `Create new Room for Temporary Accommodation Premises that's not in the user's region returns 403 Forbidden`() {
      givenAUser { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        webTestClient.post()
          .uri("/premises/${premises.id}/rooms")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewRoom(
              notes = "test notes",
              name = "test-room",
              characteristicIds = characteristicIds,
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }

  @Nested
  inner class UpdateRoom : IntegrationTestBase() {
    @Test
    fun `Updating a Room returns OK with correct body when given valid data`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("test-room")
        }

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("name").isEqualTo("test-room")
          .jsonPath("notes").isEqualTo("test notes")
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { "temporary-accommodation" })
          .jsonPath("characteristics[*].name").isEqualTo(MutableList(5) { "Floor level access" })
      }
    }

    @Test
    fun `When a room is updated with no notes then it defaults to empty`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("test-room")
        }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = null,
              characteristicIds = mutableListOf(),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("notes").isEqualTo("")
      }
    }

    @Test
    fun `Trying to update a room that does not exist returns 404`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val id = UUID.randomUUID()

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/$id")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = mutableListOf(UUID.randomUUID()),
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Not Found")
          .jsonPath("status").isEqualTo(404)
          .jsonPath("detail").isEqualTo("No Room with an ID of $id could be found")
      }
    }

    @Test
    fun `Trying to update a room with an unknown characteristic returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("test-room")
        }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = mutableListOf(UUID.randomUUID()),
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
      }
    }

    @Test
    fun `Trying to update a room with a characteristic of the wrong service scope returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("test-room")
        }

        val characteristicId = characteristicEntityFactory.produceAndPersist {
          withModelScope("room")
          withServiceScope("approved-premises")
        }.id

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = mutableListOf(characteristicId),
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("incorrectCharacteristicServiceScope")
      }
    }

    @Test
    fun `Trying to update a room with a characteristic of the wrong model scope returns 400`() {
      givenAUser { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("test-room")
        }

        val characteristicId = characteristicEntityFactory.produceAndPersist {
          withModelScope("premises")
          withServiceScope("approved-premises")
        }.id

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = mutableListOf(characteristicId),
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("incorrectCharacteristicModelScope")
      }
    }

    @Test
    fun `Updating a Room on a Temporary Accommodation premises that's not in the user's region returns 403 Forbidden`() {
      givenAUser { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("test-room")
        }

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Trying to update the name of an Approved Premises Room has no effect`() {
      givenAUser { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("approved-premises")
          withName("Floor level access")
        }.map { it.id }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("name").isEqualTo("old-room-name")
          .jsonPath("notes").isEqualTo("test notes")
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { "approved-premises" })
          .jsonPath("characteristics[*].name").isEqualTo(MutableList(5) { "Floor level access" })
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room does not change the name when it's not provided`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = null,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("name").isEqualTo("old-room-name")
          .jsonPath("notes").isEqualTo("test notes")
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { "temporary-accommodation" })
          .jsonPath("characteristics[*].name").isEqualTo(MutableList(5) { "Floor level access" })
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room changes the name when it's provided`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("name").isEqualTo("new-room-name")
          .jsonPath("notes").isEqualTo("test notes")
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { "temporary-accommodation" })
          .jsonPath("characteristics[*].name").isEqualTo(MutableList(5) { "Floor level access" })
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room with bedpace end-date when it's provided and no booking exists for the room`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }
        room.beds.add(bed)

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
              bedEndDate = LocalDate.now(),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("name").isEqualTo("new-room-name")
          .jsonPath("notes").isEqualTo("test notes")
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { "temporary-accommodation" })
          .jsonPath("characteristics[*].name").isEqualTo(MutableList(5) { "Floor level access" })
          .jsonPath("beds[*].bedEndDate").isEqualTo(LocalDate.now().toString())
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room with bedspace end-date throw conflict error when active booking's arrival and departure date overlap with bedspace end-date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val bedEndDate = LocalDate.now()
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }
        room.beds.add(bed)

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        val bookingEntity = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withArrivalDate(bedEndDate.minusDays(1))
          withDepartureDate(bedEndDate.plusDays(2))
          withBed(bed)
        }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
              bedEndDate = bedEndDate,
            ),
          )
          .exchange()
          .expectStatus()
          .isEqualTo(HttpStatus.CONFLICT)
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail").isEqualTo("Conflict booking exists for the room with end date $bedEndDate: ${bookingEntity.id}")
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room with bedspace end-date throw conflict error when active booking starts in the future date compare to bedpace end-date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val bedEndDate = LocalDate.now()
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }
        room.beds.add(bed)

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        val bookingEntity = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withArrivalDate(bedEndDate.plusDays(1))
          withDepartureDate(bedEndDate.plusDays(2))
          withBed(bed)
        }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
              bedEndDate = bedEndDate,
            ),
          )
          .exchange()
          .expectStatus()
          .isEqualTo(HttpStatus.CONFLICT)
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail").isEqualTo("Conflict booking exists for the room with end date $bedEndDate: ${bookingEntity.id}")
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room with bedspace end-date throw conflict error when active booking exists with departure date is equal to bedspace end-date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val bedEndDate = LocalDate.now()
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }
        room.beds.add(bed)

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        val bookingEntity = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withArrivalDate(bedEndDate.minusDays(1))
          withDepartureDate(bedEndDate)
          withBed(bed)
        }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
              bedEndDate = bedEndDate,
            ),
          )
          .exchange()
          .expectStatus()
          .isEqualTo(HttpStatus.CONFLICT)
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail").isEqualTo("Conflict booking exists for the room with end date $bedEndDate: ${bookingEntity.id}")
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room with bedspace end-date throw conflict error when active booking exists with arrival date is equal to bedspace end-date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val bedEndDate = LocalDate.now()
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }
        room.beds.add(bed)

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        val bookingEntity = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withArrivalDate(bedEndDate)
          withDepartureDate(bedEndDate.plusDays(1))
          withBed(bed)
        }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
              bedEndDate = bedEndDate,
            ),
          )
          .exchange()
          .expectStatus()
          .isEqualTo(HttpStatus.CONFLICT)
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail").isEqualTo("Conflict booking exists for the room with end date $bedEndDate: ${bookingEntity.id}")
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room with bedspace end-date is successful when booking exists but ended before bedspace end-date `() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val bedEndDate = LocalDate.now()
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }
        room.beds.add(bed)

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withArrivalDate(bedEndDate.minusDays(3))
          withDepartureDate(bedEndDate.minusDays(1))
          withBed(bed)
        }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
              bedEndDate = bedEndDate,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("name").isEqualTo("new-room-name")
          .jsonPath("notes").isEqualTo("test notes")
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { "temporary-accommodation" })
          .jsonPath("characteristics[*].name").isEqualTo(MutableList(5) { "Floor level access" })
          .jsonPath("beds[*].bedEndDate").isEqualTo(LocalDate.now().toString())
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room with bedspace end-date is successful when active booking exists for different room`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val bedEndDate = LocalDate.now()
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }
        room.beds.add(bed)

        val anotherRoom = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }
        val anotherBed = bedEntityFactory.produceAndPersist {
          withRoom(anotherRoom)
        }
        anotherRoom.beds.add(anotherBed)

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withArrivalDate(bedEndDate.minusDays(1))
          withDepartureDate(bedEndDate.plusDays(2))
          withBed(anotherBed)
        }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
              bedEndDate = bedEndDate,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("name").isEqualTo("new-room-name")
          .jsonPath("notes").isEqualTo("test notes")
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { "temporary-accommodation" })
          .jsonPath("characteristics[*].name").isEqualTo(MutableList(5) { "Floor level access" })
          .jsonPath("beds[*].bedEndDate").isEqualTo(LocalDate.now().toString())
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room with bedspace end-date is successful when booking exists for given bed but its been cancelled`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val bedEndDate = LocalDate.now()
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }
        room.beds.add(bed)

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        val bookingEntity = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withArrivalDate(bedEndDate.minusDays(1))
          withDepartureDate(bedEndDate.plusDays(2))
          withBed(bed)
        }

        val cancellationEntity = cancellationEntityFactory.produceAndPersist {
          withBooking(bookingEntity)
          withReason(cancellationReasonEntityFactory.produceAndPersist())
        }
        bookingEntity.cancellations += cancellationEntity

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
              bedEndDate = bedEndDate,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("name").isEqualTo("new-room-name")
          .jsonPath("notes").isEqualTo("test notes")
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { "temporary-accommodation" })
          .jsonPath("characteristics[*].name").isEqualTo(MutableList(5) { "Floor level access" })
          .jsonPath("beds[*].bedEndDate").isEqualTo(LocalDate.now().toString())
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room with bedspace end-date is successful when booking exists but non-arrival`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val bedEndDate = LocalDate.now()
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }
        room.beds.add(bed)

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        val bookingEntity = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withArrivalDate(bedEndDate.minusDays(1))
          withDepartureDate(bedEndDate.plusDays(2))
          withBed(bed)
        }

        val nonArrivalEntity = nonArrivalEntityFactory.produceAndPersist {
          withReason(nonArrivalReasonEntityFactory.produceAndPersist())
          withBooking(bookingEntity)
        }
        bookingEntity.nonArrival = nonArrivalEntity

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
              bedEndDate = bedEndDate,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("name").isEqualTo("new-room-name")
          .jsonPath("notes").isEqualTo("test notes")
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { "temporary-accommodation" })
          .jsonPath("characteristics[*].name").isEqualTo(MutableList(5) { "Floor level access" })
          .jsonPath("beds[*].bedEndDate").isEqualTo(LocalDate.now().toString())
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room with bedspace is not successful when end-date is already exists for the given bed`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
          withEndDate { LocalDate.now().plusDays(1) }
        }
        room.beds.add(bed)

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
              bedEndDate = LocalDate.now(),
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("bedEndDateCantBeModified")
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room with bedpace end-date is before bed created date is throws bad request error`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val bedEndDate = LocalDate.now()
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
          withCreatedAt { bedEndDate.plusDays(5).toLocalDateTime() }
        }
        bed.apply { createdAt = bedEndDate.plusDays(1).toLocalDateTime() }
        bedRepository.save(bed)
        room.beds.add(bed)

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
              bedEndDate = bedEndDate,
            ),
          )
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.detail")
          .isEqualTo("Bedspace end date cannot be prior to the Bedspace creation date: ${bed.createdAt!!.toLocalDate()}")
      }
    }
  }

  @Nested
  inner class GetRoom : IntegrationTestBase() {
    @Autowired
    lateinit var roomTransformer: RoomTransformer

    @Test
    fun `Get Room by ID returns OK with correct body`() {
      givenAUser { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("test-room")
          withNotes("test notes")
        }

        val expectedJson = objectMapper.writeValueAsString(roomTransformer.transformJpaToApi(room))

        webTestClient.get()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }

    @Test
    fun `Get Room by ID returns Not Found with correct body when Premises does not exist`() {
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { givenAProbationRegion() }
      }

      val room = roomEntityFactory.produceAndPersist {
        withYieldedPremises { premises }
        withName("test-room")
        withNotes("test notes")
      }

      val premisesIdToRequest = UUID.randomUUID().toString()

      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.get()
        .uri("/premises/$premisesIdToRequest/rooms/${room.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectHeader().contentType("application/problem+json")
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("title").isEqualTo("Not Found")
        .jsonPath("status").isEqualTo(404)
        .jsonPath("detail").isEqualTo("No Premises with an ID of $premisesIdToRequest could be found")
    }

    @Test
    fun `Get Room by ID returns Not Found with correct body when Room does not exist`() {
      givenAUser { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val roomIdToRequest = UUID.randomUUID().toString()

        webTestClient.get()
          .uri("/premises/${premises.id}/rooms/$roomIdToRequest")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectHeader().contentType("application/problem+json")
          .expectStatus()
          .isNotFound
          .expectBody()
          .jsonPath("title").isEqualTo("Not Found")
          .jsonPath("status").isEqualTo(404)
          .jsonPath("detail").isEqualTo("No Room with an ID of $roomIdToRequest could be found")
      }
    }

    @Test
    fun `Get Room by ID for a room on a Temporary Accommodation premises that's not in the user's region returns 403 Forbidden`() {
      givenAUser { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("test-room")
          withNotes("test notes")
        }

        webTestClient.get()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get Room by ID for temporary accommodation returns OK with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("test-room")
          withNotes("test notes")
        }

        val expectedJson = objectMapper.writeValueAsString(roomTransformer.transformJpaToApi(room))

        webTestClient.get()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }

    @Test
    fun `Get Room by ID for temporary accommodation returns OK with correct body with end date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("test-room")
          withNotes("test notes")
        }
        val bedEntity = bedEntityFactory.produceAndPersist {
          withRoom(room)
          withEndDate { LocalDate.now() }
        }

        val expectedJson = objectMapper.writeValueAsString(roomTransformer.transformJpaToApi(bedEntity.room))

        webTestClient.get()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }
  }
}

fun IntegrationTestBase.addRoomsAndBeds(premises: PremisesEntity, roomCount: Int, bedsPerRoom: Int, isActive: Boolean = true): List<RoomEntity> {
  return roomEntityFactory.produceAndPersistMultiple(roomCount) {
    withYieldedPremises { premises }
  }.onEach {
    bedEntityFactory.produceAndPersistMultiple(bedsPerRoom) {
      withYieldedRoom { it }
      if (!isActive) {
        withEndDate { LocalDate.now().minusDays(Random.nextLong(1, 10)) }
      }
    }
  }.map {
    roomTestRepository.getReferenceById(it.id)
  }
}
