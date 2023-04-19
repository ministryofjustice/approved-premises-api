package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PostCodeDistrictEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostcodeDistrictRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApprovedPremisesBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CharacteristicNames
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TemporaryAccommodationBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BedSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import java.time.LocalDate
import java.util.UUID

class BedSearchServiceTest {
  private val mockBedSearchRepository = mockk<BedSearchRepository>()
  private val mockPostcodeDistrictRepository = mockk<PostcodeDistrictRepository>()
  private val mockCharacteristicService = mockk<CharacteristicService>()

  private val bedSearchService = BedSearchService(
    mockBedSearchRepository,
    mockPostcodeDistrictRepository,
    mockCharacteristicService,
  )

  @Test
  fun `findApprovedPremisesBeds returns Unauthorised when user does not have the MATCHER role`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val result = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = "AA11",
      maxDistanceMiles = 20,
      startDate = LocalDate.parse("2023-03-22"),
      durationInWeeks = 7,
      requiredCharacteristics = listOf(),
    )

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `findApprovedPremisesBeds returns FieldValidationError when premises characteristic does not exist`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.MATCHER)
          .produce()
      }

    val postcodeDistrict = PostCodeDistrictEntityFactory()
      .produce()

    every { mockPostcodeDistrictRepository.findByOutcode(postcodeDistrict.outcode) } returns postcodeDistrict

    val premisesCharacteristicPropertyName = "unknownPropertyName"

    val roomCharacteristic = CharacteristicEntityFactory()
      .withModelScope("room")
      .withServiceScope("approved-premises")
      .produce()

    every { mockCharacteristicService.getCharacteristicsByPropertyNames(listOf(premisesCharacteristicPropertyName, roomCharacteristic.propertyName!!)) } returns listOf(roomCharacteristic)

    val authorisableResult = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 20,
      startDate = LocalDate.parse("2023-03-22"),
      durationInWeeks = 1,
      requiredCharacteristics = listOf(premisesCharacteristicPropertyName, roomCharacteristic.propertyName!!),
    )

    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
    authorisableResult as AuthorisableActionResult.Success
    assertThat(authorisableResult.entity is ValidatableActionResult.FieldValidationError).isTrue
    val fieldValidationError = authorisableResult.entity as ValidatableActionResult.FieldValidationError

    assertThat(fieldValidationError.validationMessages["$.requiredCharacteristics"]).isEqualTo("$premisesCharacteristicPropertyName doesNotExist")
  }

  @Test
  fun `findApprovedPremisesBeds returns FieldValidationError when premises characteristic has invalid scope`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.MATCHER)
          .produce()
      }

    val postcodeDistrict = PostCodeDistrictEntityFactory()
      .produce()

    every { mockPostcodeDistrictRepository.findByOutcode(postcodeDistrict.outcode) } returns postcodeDistrict

    val premisesCharacteristic = CharacteristicEntityFactory()
      .withServiceScope("temporary-accommodation")
      .withModelScope("premises")
      .produce()

    val roomCharacteristic = CharacteristicEntityFactory()
      .withModelScope("room")
      .withServiceScope("approved-premises")
      .produce()

    every { mockCharacteristicService.getCharacteristicsByPropertyNames(listOf(premisesCharacteristic.propertyName!!, roomCharacteristic.propertyName!!)) } returns listOf(premisesCharacteristic, roomCharacteristic)

    val authorisableResult = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 20,
      startDate = LocalDate.parse("2023-03-22"),
      durationInWeeks = 1,
      requiredCharacteristics = listOf(premisesCharacteristic.propertyName!!, roomCharacteristic.propertyName!!),
    )

    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
    authorisableResult as AuthorisableActionResult.Success
    assertThat(authorisableResult.entity is ValidatableActionResult.FieldValidationError).isTrue
    val fieldValidationError = authorisableResult.entity as ValidatableActionResult.FieldValidationError

    assertThat(fieldValidationError.validationMessages["$.requiredCharacteristics"]).isEqualTo("${premisesCharacteristic.propertyName} scopeInvalid")
  }

  @Test
  fun `findApprovedPremisesBeds returns FieldValidationError when room characteristic does not exist`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.MATCHER)
          .produce()
      }

    val postcodeDistrict = PostCodeDistrictEntityFactory()
      .produce()

    every { mockPostcodeDistrictRepository.findByOutcode(postcodeDistrict.outcode) } returns postcodeDistrict

    val premisesCharacteristic = CharacteristicEntityFactory()
      .withServiceScope("approved-premises")
      .withModelScope("premises")
      .produce()

    val roomCharacteristicPropertyName = "unknownPropertyName"

    every { mockCharacteristicService.getCharacteristicsByPropertyNames(listOf(premisesCharacteristic.propertyName!!, roomCharacteristicPropertyName)) } returns listOf(premisesCharacteristic)

    val authorisableResult = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 20,
      startDate = LocalDate.parse("2023-03-22"),
      durationInWeeks = 1,
      requiredCharacteristics = listOf(premisesCharacteristic.propertyName!!, roomCharacteristicPropertyName),
    )

    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
    authorisableResult as AuthorisableActionResult.Success
    assertThat(authorisableResult.entity is ValidatableActionResult.FieldValidationError).isTrue
    val fieldValidationError = authorisableResult.entity as ValidatableActionResult.FieldValidationError

    assertThat(fieldValidationError.validationMessages["$.requiredCharacteristics"]).isEqualTo("$roomCharacteristicPropertyName doesNotExist")
  }

  @Test
  fun `findApprovedPremisesBeds returns FieldValidationError when room characteristic has invalid scope`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.MATCHER)
          .produce()
      }

    val postcodeDistrict = PostCodeDistrictEntityFactory()
      .produce()

    every { mockPostcodeDistrictRepository.findByOutcode(postcodeDistrict.outcode) } returns postcodeDistrict

    val premisesCharacteristic = CharacteristicEntityFactory()
      .withServiceScope("approved-premises")
      .withModelScope("premises")
      .produce()

    val roomCharacteristic = CharacteristicEntityFactory()
      .withServiceScope("temporary-accommodation")
      .withModelScope("room")
      .produce()

    every { mockCharacteristicService.getCharacteristicsByPropertyNames(listOf(premisesCharacteristic.propertyName!!, roomCharacteristic.propertyName!!)) } returns listOf(premisesCharacteristic, roomCharacteristic)

    val authorisableResult = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 20,
      startDate = LocalDate.parse("2023-03-22"),
      durationInWeeks = 1,
      requiredCharacteristics = listOf(premisesCharacteristic.propertyName!!, roomCharacteristic.propertyName!!),
    )

    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
    authorisableResult as AuthorisableActionResult.Success
    assertThat(authorisableResult.entity is ValidatableActionResult.FieldValidationError).isTrue
    val fieldValidationError = authorisableResult.entity as ValidatableActionResult.FieldValidationError

    assertThat(fieldValidationError.validationMessages["$.requiredCharacteristics"]).isEqualTo("${roomCharacteristic.propertyName} scopeInvalid")
  }

  @Test
  fun `findApprovedPremisesBeds returns FieldValidationError when postcode district does not exist`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.MATCHER)
          .produce()
      }

    val postcodeDistrict = PostCodeDistrictEntityFactory()
      .produce()

    every { mockPostcodeDistrictRepository.findByOutcode(postcodeDistrict.outcode) } returns null

    val premisesCharacteristic = CharacteristicEntityFactory()
      .withServiceScope("approved-premises")
      .withModelScope("premises")
      .produce()

    val roomCharacteristic = CharacteristicEntityFactory()
      .withServiceScope("approved-premises")
      .withModelScope("room")
      .produce()

    every { mockCharacteristicService.getCharacteristicsByPropertyNames(listOf(premisesCharacteristic.propertyName!!, roomCharacteristic.propertyName!!)) } returns listOf(premisesCharacteristic, roomCharacteristic)

    every { mockCharacteristicService.getCharacteristicByPropertyName(roomCharacteristic.propertyName!!) } returns roomCharacteristic

    val authorisableResult = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 20,
      startDate = LocalDate.parse("2023-03-22"),
      durationInWeeks = 1,
      requiredCharacteristics = listOf(premisesCharacteristic.propertyName!!, roomCharacteristic.propertyName!!),
    )

    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
    authorisableResult as AuthorisableActionResult.Success
    assertThat(authorisableResult.entity is ValidatableActionResult.FieldValidationError).isTrue
    val fieldValidationError = authorisableResult.entity as ValidatableActionResult.FieldValidationError

    assertThat(fieldValidationError.validationMessages["$.postcodeDistrictOutcode"]).isEqualTo("doesNotExist")
  }

  @Test
  fun `findApprovedPremisesBeds returns FieldValidationError when duration in days is less than 1`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.MATCHER)
          .produce()
      }

    val postcodeDistrict = PostCodeDistrictEntityFactory()
      .produce()

    every { mockPostcodeDistrictRepository.findByOutcode(postcodeDistrict.outcode) } returns postcodeDistrict

    val premisesCharacteristic = CharacteristicEntityFactory()
      .withServiceScope("approved-premises")
      .withModelScope("premises")
      .produce()

    val roomCharacteristic = CharacteristicEntityFactory()
      .withServiceScope("approved-premises")
      .withModelScope("room")
      .produce()

    every { mockCharacteristicService.getCharacteristicsByPropertyNames(listOf(premisesCharacteristic.propertyName!!, roomCharacteristic.propertyName!!)) } returns listOf(premisesCharacteristic, roomCharacteristic)

    val authorisableResult = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 20,
      startDate = LocalDate.parse("2023-03-22"),
      durationInWeeks = 0,
      requiredCharacteristics = listOf(premisesCharacteristic.propertyName!!, roomCharacteristic.propertyName!!),
    )

    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
    authorisableResult as AuthorisableActionResult.Success
    assertThat(authorisableResult.entity is ValidatableActionResult.FieldValidationError).isTrue
    val fieldValidationError = authorisableResult.entity as ValidatableActionResult.FieldValidationError

    assertThat(fieldValidationError.validationMessages["$.durationInWeeks"]).isEqualTo("mustBeAtLeast1")
  }

  @Test
  fun `findApprovedPremisesBeds returns FieldValidationError when max distance in miles is less than 1`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.MATCHER)
          .produce()
      }

    val postcodeDistrict = PostCodeDistrictEntityFactory()
      .produce()

    every { mockPostcodeDistrictRepository.findByOutcode(postcodeDistrict.outcode) } returns postcodeDistrict

    val premisesCharacteristic = CharacteristicEntityFactory()
      .withServiceScope("approved-premises")
      .withModelScope("premises")
      .produce()

    val roomCharacteristic = CharacteristicEntityFactory()
      .withServiceScope("approved-premises")
      .withModelScope("room")
      .produce()

    every { mockCharacteristicService.getCharacteristicsByPropertyNames(listOf(premisesCharacteristic.propertyName!!, roomCharacteristic.propertyName!!)) } returns listOf(premisesCharacteristic, roomCharacteristic)

    val authorisableResult = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 0,
      startDate = LocalDate.parse("2023-03-22"),
      durationInWeeks = 1,
      requiredCharacteristics = listOf(premisesCharacteristic.propertyName!!, roomCharacteristic.propertyName!!),
    )

    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
    authorisableResult as AuthorisableActionResult.Success
    assertThat(authorisableResult.entity is ValidatableActionResult.FieldValidationError).isTrue
    val fieldValidationError = authorisableResult.entity as ValidatableActionResult.FieldValidationError

    assertThat(fieldValidationError.validationMessages["$.maxDistanceMiles"]).isEqualTo("mustBeAtLeast1")
  }

  @Test
  fun `findApprovedPremisesBeds returns results from repository`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.MATCHER)
          .produce()
      }

    val postcodeDistrict = PostCodeDistrictEntityFactory()
      .produce()

    every { mockPostcodeDistrictRepository.findByOutcode(postcodeDistrict.outcode) } returns postcodeDistrict

    val premisesCharacteristic = CharacteristicEntityFactory()
      .withModelScope("premises")
      .withServiceScope("approved-premises")
      .produce()

    val roomCharacteristic = CharacteristicEntityFactory()
      .withModelScope("room")
      .withServiceScope("approved-premises")
      .produce()

    every { mockCharacteristicService.getCharacteristicsByPropertyNames(listOf(premisesCharacteristic.propertyName!!, roomCharacteristic.propertyName!!)) } returns listOf(premisesCharacteristic, roomCharacteristic)

    val repositorySearchResults = listOf(
      ApprovedPremisesBedSearchResult(
        premisesId = UUID.randomUUID(),
        premisesName = "Premises Name",
        premisesAddressLine1 = "1 Someplace",
        premisesAddressLine2 = null,
        premisesTown = null,
        premisesPostcode = "LA1111A",
        premisesCharacteristics = mutableListOf(
          CharacteristicNames(
            propertyName = "bedCharacteristicPropertyName",
            name = "Bed Characteristic Name",
          ),
        ),
        roomId = UUID.randomUUID(),
        roomName = "Room Name",
        bedId = UUID.randomUUID(),
        bedName = "Bed Name",
        roomCharacteristics = mutableListOf(
          CharacteristicNames(
            propertyName = "roomCharacteristicPropertyName",
            name = "Room Characteristic Name",
          ),
        ),
        distance = 12.3,
        premisesBedCount = 3,
      ),
    )

    every {
      mockBedSearchRepository.findApprovedPremisesBeds(
        postcodeDistrictOutcode = postcodeDistrict.outcode,
        maxDistanceMiles = 20,
        startDate = LocalDate.parse("2023-03-22"),
        durationInWeeks = 1,
        requiredPremisesCharacteristics = listOf(premisesCharacteristic.id),
        requiredRoomCharacteristics = listOf(roomCharacteristic.id),
      )
    } returns repositorySearchResults

    val authorisableResult = bedSearchService.findApprovedPremisesBeds(
      user = user,
      postcodeDistrictOutcode = postcodeDistrict.outcode,
      maxDistanceMiles = 20,
      startDate = LocalDate.parse("2023-03-22"),
      durationInWeeks = 1,
      requiredCharacteristics = listOf(premisesCharacteristic.propertyName!!, roomCharacteristic.propertyName!!),
    )

    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
    authorisableResult as AuthorisableActionResult.Success
    assertThat(authorisableResult.entity is ValidatableActionResult.Success).isTrue
    val result = (authorisableResult.entity as ValidatableActionResult.Success).entity

    assertThat(result).isEqualTo(repositorySearchResults)
  }

  @Test
  fun `findTemporaryAccommodationBeds returns FieldValidationError when duration in days is less than 1`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val authorisableResult = bedSearchService.findTemporaryAccommodationBeds(
      user = user,
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 0,
      probationDeliveryUnit = "PDU-1",
    )

    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
    authorisableResult as AuthorisableActionResult.Success
    assertThat(authorisableResult.entity is ValidatableActionResult.FieldValidationError).isTrue
    val validationError = authorisableResult.entity as ValidatableActionResult.FieldValidationError

    assertThat(validationError.validationMessages["$.durationDays"]).isEqualTo("mustBeAtLeast1")
  }

  @Test
  fun `findTemporaryAccommodationBeds returns results from repository`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val repositorySearchResults = listOf(
      TemporaryAccommodationBedSearchResult(
        premisesId = UUID.randomUUID(),
        premisesName = "Premises Name",
        premisesAddressLine1 = "1 Someplace",
        premisesAddressLine2 = null,
        premisesTown = null,
        premisesPostcode = "LA1111A",
        premisesCharacteristics = mutableListOf(
          CharacteristicNames(
            propertyName = "bedCharacteristicPropertyName",
            name = "Bed Characteristic Name",
          ),
        ),
        premisesBedCount = 3,
        roomId = UUID.randomUUID(),
        roomName = "Room Name",
        bedId = UUID.randomUUID(),
        bedName = "Bed Name",
        roomCharacteristics = mutableListOf(
          CharacteristicNames(
            propertyName = "roomCharacteristicPropertyName",
            name = "Room Characteristic Name",
          ),
        ),
      ),
    )

    every {
      mockBedSearchRepository.findTemporaryAccommodationBeds(
        startDate = LocalDate.parse("2023-03-22"),
        durationInDays = 7,
        probationDeliveryUnit = "PDU-1",
        probationRegionId = user.probationRegion.id
      )
    } returns repositorySearchResults

    val authorisableResult = bedSearchService.findTemporaryAccommodationBeds(
      user = user,
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 7,
      probationDeliveryUnit = "PDU-1",
    )

    assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
    authorisableResult as AuthorisableActionResult.Success
    assertThat(authorisableResult.entity is ValidatableActionResult.Success).isTrue
    val result = (authorisableResult.entity as ValidatableActionResult.Success).entity

    assertThat(result).isEqualTo(repositorySearchResults)
  }
}
