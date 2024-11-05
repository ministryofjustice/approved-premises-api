package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CancellationReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonSummaryDiscriminator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RestrictedPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RestrictedPersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApprovedPremisesUserFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingAtPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingRequirementsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asOffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1SpaceBookingTransformerTest {
  @MockK
  private lateinit var personTransformer: PersonTransformer

  @MockK
  private lateinit var requirementsTransformer: Cas1SpaceBookingRequirementsTransformer

  @MockK
  private lateinit var cancellationReasonTransformer: CancellationReasonTransformer

  @MockK
  private lateinit var userTransformer: UserTransformer

  @InjectMockKs
  private lateinit var transformer: Cas1SpaceBookingTransformer

  @Nested
  inner class TransformJpaToApi {

    @Test
    fun `Space booking is transformed correctly`() {
      val personInfo = PersonInfoResult.Success.Full(
        "SOMECRN",
        CaseSummaryFactory().produce().asOffenderDetailSummary(),
        null,
      )

      val expectedPerson = RestrictedPerson(
        "SOMECRN",
        PersonType.restrictedPerson,
      )

      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withRiskRatings(
          PersonRisksFactory()
            .withTier(RiskWithStatus(RiskStatus.Retrieved, RiskTier("A", LocalDate.now())))
            .produce(),
        )
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withApplication(application)
        .produce()

      val cancellationReason = CancellationReasonEntityFactory().produce()

      val criteria = listOf(
        CharacteristicEntityFactory().produce(),
        CharacteristicEntityFactory().produce(),
      )

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withPlacementRequest(placementRequest)
        .withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
        .withKeyworkerName("Mr Key Worker")
        .withKeyworkerStaffCode("K123")
        .withKeyworkerAssignedAt(LocalDateTime.parse("2007-12-03T10:15:30").toInstant(ZoneOffset.UTC))
        .withActualArrivalDateTime(Instant.parse("2009-02-05T11:25:10.00Z"))
        .withActualDepartureDateTime(Instant.parse("2012-12-25T00:00:00.00Z"))
        .withCancellationOccurredAt(LocalDate.parse("2039-12-28"))
        .withCancellationRecordedAt(Instant.parse("2023-12-29T11:25:10.00Z"))
        .withCancellationReason(cancellationReason)
        .withCancellationReasonNotes("some extra info on cancellation")
        .withCriteria(criteria)
        .withDeliusEventNumber("97")
        .produce()

      val expectedRequirements = Cas1SpaceBookingRequirements(
        apType = ApType.pipe,
        gender = Gender.female,
        essentialCharacteristics = listOf(),
        desirableCharacteristics = listOf(),
      )

      val expectedCancellationReason = CancellationReason(
        id = UUID.randomUUID(),
        name = "some reason",
        isActive = true,
        serviceScope = "the service",
      )

      val expectedUser = ApprovedPremisesUserFactory().produce()

      val otherBookings = listOf(
        Cas1SpaceBookingAtPremisesImpl(
          id = UUID.randomUUID(),
          canonicalArrivalDate = LocalDate.parse("2025-04-06"),
          canonicalDepartureDate = LocalDate.parse("2025-05-07"),
        ),
      )

      every { personTransformer.transformModelToPersonApi(personInfo) } returns expectedPerson
      every {
        requirementsTransformer.transformJpaToApi(spaceBooking = spaceBooking)
      } returns expectedRequirements
      every {
        userTransformer.transformJpaToApi(
          spaceBooking.createdBy,
          ServiceName.approvedPremises,
        )
      } returns expectedUser
      every { cancellationReasonTransformer.transformJpaToApi(cancellationReason) } returns expectedCancellationReason

      val result = transformer.transformJpaToApi(personInfo, spaceBooking, otherBookings)

      assertThat(result.id).isEqualTo(spaceBooking.id)
      assertThat(result.applicationId).isEqualTo(application.id)
      assertThat(result.assessmentId).isEqualTo(placementRequest.assessment.id)
      assertThat(result.person).isEqualTo(expectedPerson)
      assertThat(result.requirements).isEqualTo(expectedRequirements)
      assertThat(result.premises.id).isEqualTo(spaceBooking.premises.id)
      assertThat(result.premises.name).isEqualTo(spaceBooking.premises.name)
      assertThat(result.apArea.id).isEqualTo(spaceBooking.premises.probationRegion.apArea!!.id)
      assertThat(result.apArea.name).isEqualTo(spaceBooking.premises.probationRegion.apArea!!.name)
      assertThat(result.bookedBy).isEqualTo(expectedUser)
      assertThat(result.expectedArrivalDate).isEqualTo(spaceBooking.expectedArrivalDate)
      assertThat(result.expectedDepartureDate).isEqualTo(spaceBooking.expectedDepartureDate)
      assertThat(result.actualArrivalDate).isEqualTo(Instant.parse("2009-02-05T11:25:10.00Z"))
      assertThat(result.actualDepartureDate).isEqualTo(Instant.parse("2012-12-25T00:00:00.00Z"))
      assertThat(result.canonicalArrivalDate).isEqualTo(spaceBooking.expectedArrivalDate)
      assertThat(result.canonicalDepartureDate).isEqualTo(spaceBooking.expectedDepartureDate)
      assertThat(result.createdAt).isEqualTo(spaceBooking.createdAt.toInstant())
      assertThat(result.tier).isEqualTo("A")
      assertThat(result.keyWorkerAllocation!!.keyWorker.name).isEqualTo("Mr Key Worker")
      assertThat(result.keyWorkerAllocation!!.keyWorker.code).isEqualTo("K123")
      assertThat(result.keyWorkerAllocation!!.allocatedAt).isEqualTo(LocalDate.parse("2007-12-03"))
      assertThat(result.cancellation!!.occurredAt).isEqualTo(LocalDate.parse("2039-12-28"))
      assertThat(result.cancellation!!.recordedAt).isEqualTo(Instant.parse("2023-12-29T11:25:10.00Z"))
      assertThat(result.cancellation!!.reason).isEqualTo(expectedCancellationReason)
      assertThat(result.cancellation!!.reasonNotes).isEqualTo("some extra info on cancellation")
      assertThat(result.deliusEventNumber).isEqualTo("97")

      assertThat(result.otherBookingsInPremisesForCrn).hasSize(1)
      assertThat(result.otherBookingsInPremisesForCrn[0].canonicalArrivalDate).isEqualTo(LocalDate.parse("2025-04-06"))
      assertThat(result.otherBookingsInPremisesForCrn[0].canonicalDepartureDate).isEqualTo(LocalDate.parse("2025-05-07"))
      assertThat(result.requestForPlacementId).isEqualTo(placementRequest.id)
    }

    @Test
    fun `Space booking is transformed correctly via placement application`() {
      val personInfo = PersonInfoResult.Success.Full(
        "SOMECRN",
        CaseSummaryFactory().produce().asOffenderDetailSummary(),
        null,
      )

      val expectedPerson = RestrictedPerson(
        "SOMECRN",
        PersonType.restrictedPerson,
      )

      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withRiskRatings(
          PersonRisksFactory()
            .withTier(RiskWithStatus(RiskStatus.Retrieved, RiskTier("A", LocalDate.now())))
            .produce(),
        )
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withDefaults()
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withApplication(application)
        .withPlacementApplication(placementApplication)
        .produce()

      val criteria = listOf(
        CharacteristicEntityFactory().produce(),
        CharacteristicEntityFactory().produce(),
      )

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withPlacementRequest(placementRequest)
        .withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
        .withKeyworkerName("Mr Key Worker")
        .withKeyworkerStaffCode("K123")
        .withKeyworkerAssignedAt(LocalDateTime.parse("2007-12-03T10:15:30").toInstant(ZoneOffset.UTC))
        .withActualArrivalDateTime(Instant.parse("2009-02-05T11:25:10.00Z"))
        .withActualDepartureDateTime(Instant.parse("2012-12-25T00:00:00.00Z"))
        .withCancellationOccurredAt(LocalDate.parse("2039-12-28"))
        .withCancellationRecordedAt(Instant.parse("2023-12-29T11:25:10.00Z"))
        .withCancellationReasonNotes("some extra info on cancellation")
        .withCriteria(criteria)
        .produce()

      val expectedRequirements = Cas1SpaceBookingRequirements(
        apType = ApType.pipe,
        gender = Gender.female,
        essentialCharacteristics = listOf(),
        desirableCharacteristics = listOf(),
      )

      val expectedUser = ApprovedPremisesUserFactory().produce()

      val otherBookings = listOf(
        Cas1SpaceBookingAtPremisesImpl(
          id = UUID.randomUUID(),
          canonicalArrivalDate = LocalDate.parse("2025-04-06"),
          canonicalDepartureDate = LocalDate.parse("2025-05-07"),
        ),
      )

      every { personTransformer.transformModelToPersonApi(personInfo) } returns expectedPerson
      every {
        requirementsTransformer.transformJpaToApi(spaceBooking = spaceBooking)
      } returns expectedRequirements
      every {
        userTransformer.transformJpaToApi(
          spaceBooking.createdBy,
          ServiceName.approvedPremises,
        )
      } returns expectedUser

      val result = transformer.transformJpaToApi(personInfo, spaceBooking, otherBookings)

      assertThat(result.requestForPlacementId).isEqualTo(placementApplication.id)
    }
  }

  @Nested
  inner class TransformSearchResultToSummary {

    @Test
    fun `Space booking is transformed correctly with case worker`() {
      val id = UUID.randomUUID()

      val personSummaryInfo = PersonSummaryInfoResult.Success.Restricted("the crn", "the noms")
      val expectedPersonSummary = RestrictedPersonSummary(
        "the crn",
        PersonSummaryDiscriminator.restrictedPersonSummary,
      )

      every { personTransformer.personSummaryInfoToPersonSummary(personSummaryInfo) } returns expectedPersonSummary

      val result = transformer.transformSearchResultToSummary(
        Cas1SpaceBookingSearchResultImpl(
          id = id,
          crn = "the crn",
          canonicalArrivalDate = LocalDate.parse("2023-12-13"),
          canonicalDepartureDate = LocalDate.parse("2023-01-02"),
          tier = "A",
          keyWorkerStaffCode = "the staff code",
          keyWorkerAssignedAt = LocalDateTime.of(2023, 12, 12, 0, 0, 0).toInstant(ZoneOffset.UTC),
          keyWorkerName = "the keyworker name",
        ),
        personSummaryInfo,
      )

      assertThat(result.id).isEqualTo(id)
      assertThat(result.person).isEqualTo(expectedPersonSummary)
      assertThat(result.canonicalArrivalDate).isEqualTo(LocalDate.parse("2023-12-13"))
      assertThat(result.canonicalDepartureDate).isEqualTo(LocalDate.parse("2023-01-02"))
      assertThat(result.tier).isEqualTo("A")
      assertThat(result.keyWorkerAllocation!!.allocatedAt).isEqualTo(LocalDate.parse("2023-12-12"))
      assertThat(result.keyWorkerAllocation!!.keyWorker.name).isEqualTo("the keyworker name")
      assertThat(result.keyWorkerAllocation!!.keyWorker.code).isEqualTo("the staff code")
    }

    @Test
    fun `Space booking is transformed correctly without case worker`() {
      val id = UUID.randomUUID()

      val personSummaryInfo = PersonSummaryInfoResult.Success.Restricted("the crn", "the noms")
      val expectedPersonSummary = RestrictedPersonSummary(
        "the crn",
        PersonSummaryDiscriminator.restrictedPersonSummary,
      )

      every { personTransformer.personSummaryInfoToPersonSummary(personSummaryInfo) } returns expectedPersonSummary

      val result = transformer.transformSearchResultToSummary(
        Cas1SpaceBookingSearchResultImpl(
          id = id,
          crn = "the crn",
          canonicalArrivalDate = LocalDate.parse("2023-12-13"),
          canonicalDepartureDate = LocalDate.parse("2023-01-02"),
          tier = "A",
          keyWorkerStaffCode = null,
          keyWorkerAssignedAt = null,
          keyWorkerName = null,
        ),
        personSummaryInfo,
      )

      assertThat(result.id).isEqualTo(id)
      assertThat(result.keyWorkerAllocation).isNull()
    }
  }
}

data class Cas1SpaceBookingSearchResultImpl(
  override val id: UUID,
  override val crn: String,
  override val canonicalArrivalDate: LocalDate,
  override val canonicalDepartureDate: LocalDate,
  override val tier: String?,
  override val keyWorkerStaffCode: String?,
  override val keyWorkerAssignedAt: Instant?,
  override val keyWorkerName: String?,
) : Cas1SpaceBookingSearchResult

data class Cas1SpaceBookingAtPremisesImpl(
  override val id: UUID,
  override val canonicalArrivalDate: LocalDate,
  override val canonicalDepartureDate: LocalDate,
) : Cas1SpaceBookingAtPremises
