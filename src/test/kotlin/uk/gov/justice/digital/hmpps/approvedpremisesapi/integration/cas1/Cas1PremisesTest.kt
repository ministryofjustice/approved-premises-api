package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseCapacity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesBasicSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOutOfServiceBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_ASSESSOR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_FUTURE_MANAGER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import java.time.LocalDate
import java.util.UUID

class Cas1PremisesTest : IntegrationTestBase() {

  @Nested
  inner class GetPremisesSummary : InitialiseDatabasePerClassTestBase() {

    lateinit var premises: ApprovedPremisesEntity

    @BeforeAll
    fun setupTestData() {
      val region = givenAProbationRegion(
        apArea = givenAnApArea(name = "The ap area name"),
      )

      premises = approvedPremisesEntityFactory.produceAndPersist {
        withName("the premises name")
        withApCode("the ap code")
        withPostcode("the postcode")
        withYieldedProbationRegion { region }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withManagerDetails("manager details")
      }
    }

    @Test
    fun `Returns 403 Forbidden if user does not have correct role`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_ASSESSOR))

      webTestClient.get()
        .uri("/cas1/premises/${premises.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Returns 404 if premise doesn't exist`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      webTestClient.get()
        .uri("/cas1/premises/${UUID.randomUUID()}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @SuppressWarnings("UnusedPrivateProperty")
    @Test
    fun `Returns premises summary`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val beds = bedEntityFactory.produceAndPersistMultiple(5) {
        withYieldedRoom {
          roomEntityFactory.produceAndPersist {
            withYieldedPremises { premises }
          }
        }
      }

      val currentOutOfServiceBedCancelled = givenAnOutOfServiceBed(
        bed = beds[0],
        startDate = LocalDate.now().minusDays(1),
        endDate = LocalDate.now().plusDays(4),
        cancelled = true,
      )

      val futureOutOfServiceBed = givenAnOutOfServiceBed(
        bed = beds[0],
        startDate = LocalDate.now().plusDays(2),
        endDate = LocalDate.now().plusDays(4),
      )

      val currentOutOfServiceBed = givenAnOutOfServiceBed(
        bed = beds[0],
        startDate = LocalDate.now().minusDays(2),
        endDate = LocalDate.now().plusDays(2),
      )

      val summary = webTestClient.get()
        .uri("/cas1/premises/${premises.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1PremisesSummary::class.java).responseBody.blockFirst()!!

      assertThat(summary.id).isEqualTo(premises.id)
      assertThat(summary.name).isEqualTo("the premises name")
      assertThat(summary.apCode).isEqualTo("the ap code")
      assertThat(summary.postcode).isEqualTo("the postcode")
      assertThat(summary.bedCount).isEqualTo(5)
      assertThat(summary.outOfServiceBeds).isEqualTo(1)
      assertThat(summary.availableBeds).isEqualTo(4)
      assertThat(summary.apArea.name).isEqualTo("The ap area name")
      assertThat(summary.managerDetails).isEqualTo("manager details")
    }
  }

  @Nested
  inner class GetPremisesSummaries : InitialiseDatabasePerClassTestBase() {

    lateinit var premises1ManInArea1: ApprovedPremisesEntity
    lateinit var premises2WomanInArea2: ApprovedPremisesEntity
    lateinit var premises3ManInArea2: ApprovedPremisesEntity

    lateinit var apArea1: ApAreaEntity
    lateinit var apArea2: ApAreaEntity

    @BeforeAll
    fun setupTestData() {
      apArea1 = givenAnApArea(name = "the ap area name 1")
      apArea2 = givenAnApArea(name = "the ap area name 2")

      val region1 = givenAProbationRegion(
        apArea = apArea1,
      )

      val region2 = givenAProbationRegion(
        apArea = apArea2,
      )

      premises1ManInArea1 = approvedPremisesEntityFactory.produceAndPersist {
        withName("the premises name 1")
        withGender(ApprovedPremisesGender.MAN)
        withYieldedProbationRegion { region1 }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withSupportsSpaceBookings(false)
      }

      premises2WomanInArea2 = approvedPremisesEntityFactory.produceAndPersist {
        withName("the premises name 2")
        withGender(ApprovedPremisesGender.WOMAN)
        withYieldedProbationRegion { region2 }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withSupportsSpaceBookings(false)
      }

      premises3ManInArea2 = approvedPremisesEntityFactory.produceAndPersist {
        withName("the premises name 3")
        withGender(ApprovedPremisesGender.MAN)
        withYieldedProbationRegion { region2 }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withSupportsSpaceBookings(true)
      }
    }

    @SuppressWarnings("CyclomaticComplexMethod")
    @Test
    fun `Returns premises summaries with no filters applied`() {
      val (_, jwt) = givenAUser()

      val summaries = webTestClient.get()
        .uri("/cas1/premises/summary")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1PremisesBasicSummary>()

      assertThat(summaries).hasSize(3)

      assertThat(summaries)
        .anyMatch {
          it.id == premises1ManInArea1.id &&
            it.name == "the premises name 1" &&
            it.apCode == premises1ManInArea1.apCode &&
            it.apArea.name == "the ap area name 1" &&
            it.bedCount == 0 &&
            it.supportsSpaceBookings == false
        }
        .anyMatch {
          it.id == premises2WomanInArea2.id &&
            it.name == "the premises name 2" &&
            it.apCode == premises2WomanInArea2.apCode &&
            it.apArea.name == "the ap area name 2" &&
            it.bedCount == 0 &&
            it.supportsSpaceBookings == false
        }
        .anyMatch {
          it.id == premises3ManInArea2.id &&
            it.name == "the premises name 3" &&
            it.apCode == premises3ManInArea2.apCode &&
            it.apArea.name == "the ap area name 2" &&
            it.bedCount == 0 &&
            it.supportsSpaceBookings == true
        }
    }

    @Test
    fun `Returns premises summaries where gender is man`() {
      val (_, jwt) = givenAUser()

      val summaries = webTestClient.get()
        .uri("/cas1/premises/summary?gender=man")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1PremisesBasicSummary>()

      assertThat(summaries).hasSize(2)

      assertThat(summaries)
        .anyMatch {
          it.id == premises1ManInArea1.id &&
            it.name == "the premises name 1" &&
            it.apCode == premises1ManInArea1.apCode &&
            it.apArea.name == "the ap area name 1" &&
            it.bedCount == 0
        }
        .anyMatch {
          it.id == premises3ManInArea2.id &&
            it.name == "the premises name 3" &&
            it.apCode == premises3ManInArea2.apCode &&
            it.apArea.name == "the ap area name 2" &&
            it.bedCount == 0
        }
    }

    @Test
    fun `Returns premises summaries where gender is woman`() {
      val (_, jwt) = givenAUser()

      val summaries = webTestClient.get()
        .uri("/cas1/premises/summary?gender=woman")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1PremisesBasicSummary>()

      assertThat(summaries).hasSize(1)

      assertThat(summaries[0].id).isEqualTo(premises2WomanInArea2.id)
      assertThat(summaries[0].name).isEqualTo("the premises name 2")
      assertThat(summaries[0].apCode).isEqualTo(premises2WomanInArea2.apCode)
      assertThat(summaries[0].apArea.name).isEqualTo("the ap area name 2")
      assertThat(summaries[0].bedCount).isEqualTo(0)
    }

    @Test
    fun `Returns premises summaries for specified ap area`() {
      val (_, jwt) = givenAUser()

      val summaries = webTestClient.get()
        .uri("/cas1/premises/summary?apAreaId=${apArea1.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1PremisesBasicSummary>()

      assertThat(summaries).hasSize(1)

      assertThat(summaries[0].id).isEqualTo(premises1ManInArea1.id)
      assertThat(summaries[0].name).isEqualTo("the premises name 1")
      assertThat(summaries[0].apCode).isEqualTo(premises1ManInArea1.apCode)
      assertThat(summaries[0].apArea.name).isEqualTo("the ap area name 1")
      assertThat(summaries[0].bedCount).isEqualTo(0)
    }

    @Test
    fun `Returns premises summaries for specified gender and ap area`() {
      val (_, jwt) = givenAUser()

      val summaries = webTestClient.get()
        .uri("/cas1/premises/summary?gender=man&apAreaId=${apArea2.id}&")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1PremisesBasicSummary>()

      assertThat(summaries).hasSize(1)

      assertThat(summaries[0].id).isEqualTo(premises3ManInArea2.id)
      assertThat(summaries[0].name).isEqualTo("the premises name 3")
      assertThat(summaries[0].apCode).isEqualTo(premises3ManInArea2.apCode)
      assertThat(summaries[0].apArea.name).isEqualTo("the ap area name 2")
      assertThat(summaries[0].bedCount).isEqualTo(0)
    }

    @Test
    fun `Returns correct bed count for premise summaries`() {
      val (_, jwt) = givenAUser()

      val premises1ManRoom = roomEntityFactory.produceAndPersist {
        withYieldedPremises { premises1ManInArea1 }
      }
      val premises2WomanRoom = roomEntityFactory.produceAndPersist {
        withYieldedPremises { premises2WomanInArea2 }
      }
      val premises3ManRoom = roomEntityFactory.produceAndPersist {
        withYieldedPremises { premises3ManInArea2 }
      }

      val premises1ManLiveBeds = listOf(
        bedEntityFactory.produceAndPersistMultiple(5) {
          withYieldedRoom { premises1ManRoom }
        },
        // Beds scheduled for removal in the future
        bedEntityFactory.produceAndPersistMultiple(2) {
          withYieldedRoom { premises1ManRoom }
          withEndDate { LocalDate.now().plusDays(5) }
        },
      ).flatten()
      // Removed beds
      bedEntityFactory.produceAndPersistMultiple(2) {
        withYieldedRoom { premises1ManRoom }
        withEndDate { LocalDate.now().minusDays(5) }
      }
      // Beds scheduled for removal today
      bedEntityFactory.produceAndPersistMultiple(3) {
        withYieldedRoom { premises1ManRoom }
        withEndDate { LocalDate.now() }
      }

      val premises2WomanLiveBeds = listOf(
        bedEntityFactory.produceAndPersistMultiple(2) {
          withYieldedRoom { premises2WomanRoom }
        },
        // Beds scheduled for removal in the future
        bedEntityFactory.produceAndPersistMultiple(2) {
          withYieldedRoom { premises2WomanRoom }
          withEndDate { LocalDate.now().plusDays(20) }
        },
      ).flatten()

      bedEntityFactory.produceAndPersistMultiple(20) {
        withYieldedRoom { premises3ManRoom }
        withEndDate { LocalDate.now().minusDays(5) }
      }

      val summaries = webTestClient.get()
        .uri("/cas1/premises/summary")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1PremisesBasicSummary>()

      assertThat(summaries).hasSize(3)

      assertThat(summaries)
        .anyMatch {
          it.id == premises1ManInArea1.id &&
            it.bedCount == premises1ManLiveBeds.count()
        }
        .anyMatch {
          it.id == premises2WomanInArea2.id &&
            it.bedCount == premises2WomanLiveBeds.count()
        }
        .anyMatch {
          it.id == premises3ManInArea2.id &&
            it.bedCount == 0
        }
    }
  }

  @Nested
  inner class GetCapacity : InitialiseDatabasePerClassTestBase() {
    lateinit var premises: ApprovedPremisesEntity

    @BeforeAll
    fun setupTestData() {
      val region = givenAProbationRegion(
        apArea = givenAnApArea(name = "The ap area name"),
      )

      premises = approvedPremisesEntityFactory.produceAndPersist {
        withName("the premises name")
        withApCode("the ap code")
        withPostcode("the postcode")
        withYieldedProbationRegion { region }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withManagerDetails("manager details")
      }
    }

    @Test
    fun `Returns 403 Forbidden if user does not have correct role`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_ASSESSOR))

      webTestClient.get()
        .uri("/cas1/premises/${premises.id}/capacity?startDate=2020-01-01&endDate=2020-01-02")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun success() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val result = webTestClient.get()
        .uri("/cas1/premises/${premises.id}/capacity?startDate=2020-01-01&endDate=2020-01-02")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1PremiseCapacity::class.java).responseBody.blockFirst()!!

      assertThat(result.premise.name).isEqualTo("the premises name")
      assertThat(result.capacity).hasSize(2)
      assertThat(result.premise.managerDetails).isEqualTo("manager details")

      assertThat(result.capacity[0].date).isEqualTo(LocalDate.of(2020, 1, 1))
      assertThat(result.capacity[1].date).isEqualTo(LocalDate.of(2020, 1, 2))
    }
  }
}
