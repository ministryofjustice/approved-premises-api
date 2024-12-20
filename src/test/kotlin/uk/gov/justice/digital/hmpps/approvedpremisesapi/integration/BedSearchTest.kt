package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesBedSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchAttributes
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultRoomSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CharacteristicPair
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchResultOverlap
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenSomeOffenders
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddListCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@SuppressWarnings("LargeClass")
class BedSearchTest : IntegrationTestBase() {

  lateinit var probationRegion: ProbationRegionEntity

  @BeforeEach
  fun before() {
    probationRegion = givenAProbationRegion()
  }

  @Nested
  inner class BedSearchForApprovedPremises {
    @Test
    fun `Searching for a Bed without JWT returns 401`() {
      webTestClient.post()
        .uri("/beds/search")
        .bodyValue(
          ApprovedPremisesBedSearchParameters(
            postcodeDistrict = "AA11",
            maxDistanceMiles = 20,
            requiredCharacteristics = listOf(),
            startDate = LocalDate.parse("2023-03-23"),
            durationDays = 7,
            serviceName = "approved-premises",
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Searching for an Approved Premises Bed without MATCHER role returns 403`() {
      givenAUser { _, jwt ->
        webTestClient.post()
          .uri("/beds/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            ApprovedPremisesBedSearchParameters(
              postcodeDistrict = "AA11",
              maxDistanceMiles = 20,
              requiredCharacteristics = listOf(),
              startDate = LocalDate.parse("2023-03-23"),
              durationDays = 7,
              serviceName = "approved-premises",
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Searching for an Approved Premises Bed returns 200 with correct body`() {
      givenAUser(
        roles = listOf(UserRole.CAS1_MATCHER),
      ) { _, jwt ->
        val postCodeDistrictLatLong = LatLong(50.1044, -2.3992)
        val tenMilesFromPostcodeDistrict = postCodeDistrictLatLong.plusLatitudeMiles(10)

        val postcodeDistrict = postCodeDistrictFactory.produceAndPersist {
          withOutcode("AA11")
          withLatitude(postCodeDistrictLatLong.latitude)
          withLongitude(postCodeDistrictLatLong.longitude)
        }

        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withLatitude(tenMilesFromPostcodeDistrict.latitude)
          withLongitude(tenMilesFromPostcodeDistrict.longitude)
          withStatus(PropertyStatus.active)
        }

        val room = roomEntityFactory.produceAndPersist {
          withPremises(premises)
        }

        val bed = bedEntityFactory.produceAndPersist {
          withName("Matching Bed")
          withRoom(room)
        }

        webTestClient.post()
          .uri("/beds/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            ApprovedPremisesBedSearchParameters(
              postcodeDistrict = postcodeDistrict.outcode,
              maxDistanceMiles = 20,
              requiredCharacteristics = listOf(),
              startDate = LocalDate.parse("2023-03-23"),
              durationDays = 7,
              serviceName = "approved-premises",
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              BedSearchResults(
                resultsRoomCount = 1,
                resultsPremisesCount = 1,
                resultsBedCount = 1,
                results = listOf(
                  ApprovedPremisesBedSearchResult(
                    distanceMiles = BigDecimal("10.016010816899744"),
                    premises = BedSearchResultPremisesSummary(
                      id = premises.id,
                      name = premises.name,
                      addressLine1 = premises.addressLine1,
                      postcode = premises.postcode,
                      characteristics = listOf(),
                      addressLine2 = premises.addressLine2,
                      town = premises.town,
                      bedCount = 1,
                    ),
                    room = BedSearchResultRoomSummary(
                      id = room.id,
                      name = room.name,
                      characteristics = listOf(),
                    ),
                    bed = BedSearchResultBedSummary(
                      id = bed.id,
                      name = bed.name,
                    ),
                    serviceName = ServiceName.approvedPremises,
                  ),
                ),
              ),
            ),
          )
      }
    }
  }

  @Nested
  inner class BedSearchForTemporaryAccommodationPremises {
    @Test
    fun `Searching for a Temporary Accommodation Bed returns 200 with correct body`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(searchPdu)
          withProbationRegion(probationRegion)
          withStatus(PropertyStatus.active)
          withNotes(randomStringMultiCaseWithNumbers(100))
        }

        val room = roomEntityFactory.produceAndPersist {
          withPremises(premises)
        }

        val bed = bedEntityFactory.produceAndPersist {
          withName("Matching Bed")
          withRoom(room)
        }

        webTestClient.post()
          .uri("/beds/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            TemporaryAccommodationBedSearchParameters(
              startDate = LocalDate.parse("2023-03-23"),
              durationDays = 7,
              serviceName = "temporary-accommodation",
              probationDeliveryUnits = listOf(searchPdu.id),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              BedSearchResults(
                resultsRoomCount = 1,
                resultsPremisesCount = 1,
                resultsBedCount = 1,
                results = listOf(
                  createTemporaryAccommodationBedSearchResult(
                    premises,
                    room,
                    bed,
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
          )
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed returns results that do not include beds with current turnarounds`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(searchPdu)
          withProbationRegion(probationRegion)
          withStatus(PropertyStatus.active)
        }

        val room = roomEntityFactory.produceAndPersist {
          withPremises(premises)
        }

        val bed = bedEntityFactory.produceAndPersist {
          withName("Matching Bed")
          withRoom(room)
        }

        val booking = bookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withPremises(premises)
          withBed(bed)
          withArrivalDate(LocalDate.parse("2022-12-21"))
          withDepartureDate(LocalDate.parse("2023-03-21"))
        }

        val turnaround = turnaroundFactory.produceAndPersist {
          withBooking(booking)
          withWorkingDayCount(2)
        }

        booking.turnarounds = mutableListOf(turnaround)

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        searchTemporaryAccommodationBedSpaceAndAssertNoAvailability(
          jwt,
          LocalDate.parse("2023-03-23"),
          7,
          searchPdu.id,
        )
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed returns results when existing booking departure-date is same as search start-date`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(searchPdu)
          withProbationRegion(probationRegion)
          withStatus(PropertyStatus.active)
        }

        val room = roomEntityFactory.produceAndPersist {
          withPremises(premises)
        }

        val bed = bedEntityFactory.produceAndPersist {
          withName("Matching Bed")
          withRoom(room)
        }

        val booking = bookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withPremises(premises)
          withBed(bed)
          withArrivalDate(LocalDate.parse("2022-12-21"))
          withDepartureDate(LocalDate.parse("2023-03-21"))
        }

        val turnaround = turnaroundFactory.produceAndPersist {
          withBooking(booking)
          withWorkingDayCount(2)
        }

        booking.turnarounds = mutableListOf(turnaround)

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        searchTemporaryAccommodationBedSpaceAndAssertNoAvailability(
          jwt,
          LocalDate.parse("2023-03-21"),
          7,
          searchPdu.id,
        )
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed returns results which include overlapping bookings for rooms in the same premises`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { user, jwt ->
        givenSomeOffenders { offenderSequence ->
          val offenders = offenderSequence.take(4).toList()

          val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

          val applications = mutableListOf<TemporaryAccommodationApplicationEntity>()
          val assessments = mutableListOf<AssessmentEntity>()

          offenders.mapIndexed { i, (offenderDetails, inmateDetails) ->
            val (application, assessment) = createAssessment(user, offenderDetails.otherIds.crn)
            applications += application
            assessments += assessment
          }

          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withProbationRegion(probationRegion)
            withLocalAuthorityArea(localAuthorityArea)
            withProbationDeliveryUnit(searchPdu)
            withProbationRegion(probationRegion)
            withStatus(PropertyStatus.active)
          }

          val roomOne = roomEntityFactory.produceAndPersist {
            withPremises(premises)
          }

          val roomTwo = roomEntityFactory.produceAndPersist {
            withPremises(premises)
          }

          val roomThree = roomEntityFactory.produceAndPersist {
            withPremises(premises)
          }

          val roomFour = roomEntityFactory.produceAndPersist {
            withPremises(premises)
          }

          val bedOne = bedEntityFactory.produceAndPersist {
            withName("matching bed")
            withRoom(roomOne)
          }

          val bedTwo = bedEntityFactory.produceAndPersist {
            withName("matching bed, but with an overlapping booking")
            withRoom(roomTwo)
          }

          val bedThree = bedEntityFactory.produceAndPersist {
            withName("bed in a different room, with an overlapping booking")
            withRoom(roomThree)
          }

          val bedFour = bedEntityFactory.produceAndPersist {
            withName("bed in a different room, with an overlapping booking")
            withRoom(roomFour)
          }

          val fullPersonOffenderDetails = offenders.first().first
          val fullPersonApplication = applications.first()
          val fullPersonAssessment = assessments.first()
          val fullPersonCaseSummary = CaseSummaryFactory()
            .fromOffenderDetails(fullPersonOffenderDetails)
            .withPnc(fullPersonOffenderDetails.otherIds.pncNumber)
            .produce()

          val overlappingBookingSameRoom = bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withApplication(fullPersonApplication)
            withPremises(premises)
            withBed(bedTwo)
            withArrivalDate(LocalDate.parse("2023-07-15"))
            withDepartureDate(LocalDate.parse("2023-08-15"))
            withCrn(fullPersonCaseSummary.crn)
            withId(UUID.randomUUID())
          }

          val currentRestrictionOffenderDetails = offenders.drop(1).first().first
          val currentRestrictionApplication = applications.drop(1).first()
          val currentRestrictionAssessment = assessments.drop(1).first()
          val currentRestrictionCaseSummary = CaseSummaryFactory()
            .fromOffenderDetails(currentRestrictionOffenderDetails)
            .withPnc(currentRestrictionOffenderDetails.otherIds.pncNumber)
            .withCurrentRestriction(true)
            .produce()

          val currentRestrictionOverlappingBooking = bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withApplication(currentRestrictionApplication)
            withPremises(premises)
            withBed(bedThree)
            withArrivalDate(LocalDate.parse("2023-08-27"))
            withDepartureDate(LocalDate.parse("2023-09-13"))
            withCrn(currentRestrictionCaseSummary.crn)
            withId(UUID.randomUUID())
          }

          val userExcludedOffenderDetails = offenders.drop(2).first().first
          val userExcludedApplication = applications.drop(2).first()
          val userExcludedAssessment = assessments.drop(2).first()
          val userExcludedCaseSummary = CaseSummaryFactory()
            .fromOffenderDetails(userExcludedOffenderDetails)
            .withPnc(userExcludedOffenderDetails.otherIds.pncNumber)
            .withCurrentExclusion(true)
            .produce()

          val userExcludedOverlappingBooking = bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withApplication(userExcludedApplication)
            withPremises(premises)
            withBed(bedFour)
            withArrivalDate(LocalDate.parse("2023-08-25"))
            withDepartureDate(LocalDate.parse("2023-09-25"))
            withCrn(userExcludedCaseSummary.crn)
            withId(UUID.randomUUID())
          }

          apDeliusContextAddListCaseSummaryToBulkResponse(listOf(fullPersonCaseSummary, userExcludedCaseSummary, currentRestrictionCaseSummary))

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(fullPersonCaseSummary.crn)
                .produce(),
              CaseAccessFactory()
                .withCrn(userExcludedCaseSummary.crn)
                .withUserExcluded(true)
                .produce(),
              CaseAccessFactory()
                .withCrn(currentRestrictionCaseSummary.crn)
                .withUserRestricted(true)
                .produce(),
            ),
            user.deliusUsername,
          )

          webTestClient.post()
            .uri("/beds/search")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              TemporaryAccommodationBedSearchParameters(
                startDate = LocalDate.parse("2023-08-01"),
                durationDays = 31,
                serviceName = "temporary-accommodation",
                probationDeliveryUnits = listOf(searchPdu.id),
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                BedSearchResults(
                  resultsRoomCount = 1,
                  resultsPremisesCount = 1,
                  resultsBedCount = 1,
                  results = listOf(
                    createTemporaryAccommodationBedSearchResult(
                      premises,
                      roomOne,
                      bedOne,
                      searchPdu.name,
                      numberOfBeds = 4,
                      numberOfBookedBeds = 3,
                      premisesCharacteristics = listOf(),
                      roomCharacteristics = listOf(),
                      listOf(
                        TemporaryAccommodationBedSearchResultOverlap(
                          name = "${fullPersonCaseSummary.name.forename} ${fullPersonCaseSummary.name.surname}",
                          crn = fullPersonCaseSummary.crn,
                          personType = PersonType.fullPerson,
                          sex = fullPersonCaseSummary.gender!!,
                          days = 15,
                          bookingId = overlappingBookingSameRoom.id,
                          roomId = roomTwo.id,
                          assessmentId = fullPersonAssessment.id,
                        ),
                        TemporaryAccommodationBedSearchResultOverlap(
                          name = "Limited Access Offender",
                          crn = currentRestrictionCaseSummary.crn,
                          personType = PersonType.restrictedPerson,
                          days = 5,
                          bookingId = currentRestrictionOverlappingBooking.id,
                          roomId = roomThree.id,
                          assessmentId = currentRestrictionAssessment.id,
                        ),
                        TemporaryAccommodationBedSearchResultOverlap(
                          name = "Limited Access Offender",
                          crn = userExcludedCaseSummary.crn,
                          personType = PersonType.restrictedPerson,
                          days = 7,
                          bookingId = userExcludedOverlappingBooking.id,
                          roomId = roomFour.id,
                          assessmentId = userExcludedAssessment.id,
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed returns results which include overlapping bookings across multiple premises`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

          val (application, assessment) = createAssessment(user, offenderDetails.otherIds.crn)

          val caseSummary = CaseSummaryFactory()
            .fromOffenderDetails(offenderDetails)
            .withPnc(offenderDetails.otherIds.pncNumber)
            .produce()

          val premisesOne = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withName("Premises One")
            withProbationRegion(probationRegion)
            withLocalAuthorityArea(localAuthorityArea)
            withProbationDeliveryUnit(searchPdu)
            withProbationRegion(probationRegion)
            withStatus(PropertyStatus.active)
          }

          val premisesTwo = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withName("Premises Two")
            withProbationRegion(probationRegion)
            withLocalAuthorityArea(localAuthorityArea)
            withProbationDeliveryUnit(searchPdu)
            withProbationRegion(probationRegion)
            withStatus(PropertyStatus.active)
          }

          val roomInPremisesOne = roomEntityFactory.produceAndPersist {
            withPremises(premisesOne)
          }

          val roomInPremisesTwo = roomEntityFactory.produceAndPersist {
            withPremises(premisesTwo)
          }

          val matchingBedInPremisesOne = bedEntityFactory.produceAndPersist {
            withName("matching bed in premises one")
            withRoom(roomInPremisesOne)
          }

          val overlappingBedInPremisesOne = bedEntityFactory.produceAndPersist {
            withName("overlapping bed in premises one")
            withRoom(roomInPremisesOne)
          }

          val matchingBedInPremisesTwo = bedEntityFactory.produceAndPersist {
            withName("matching bed in premises two")
            withRoom(roomInPremisesTwo)
          }

          val overlappingBedInPremisesTwo = bedEntityFactory.produceAndPersist {
            withName("overlapping bed in premises two")
            withRoom(roomInPremisesTwo)
          }

          val overlappingBookingForBedInPremisesOne = bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withApplication(application)
            withPremises(premisesOne)
            withBed(overlappingBedInPremisesOne)
            withArrivalDate(LocalDate.parse("2023-07-15"))
            withDepartureDate(LocalDate.parse("2023-08-15"))
            withCrn(offenderDetails.otherIds.crn)
            withId(UUID.randomUUID())
          }

          val overlappingBookingForBedInPremisesTwo = bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withApplication(application)
            withPremises(premisesTwo)
            withBed(overlappingBedInPremisesTwo)
            withArrivalDate(LocalDate.parse("2023-08-25"))
            withDepartureDate(LocalDate.parse("2023-09-25"))
            withCrn(offenderDetails.otherIds.crn)
            withId(UUID.randomUUID())
          }

          val cancelledOverlappingBookingForBedInPremisesTwo = bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withApplication(application)
            withPremises(premisesTwo)
            withBed(overlappingBedInPremisesTwo)
            withArrivalDate(LocalDate.parse("2023-07-25"))
            withDepartureDate(LocalDate.parse("2023-08-05"))
            withCrn(offenderDetails.otherIds.crn)
            withId(UUID.randomUUID())
          }

          cancellationEntityFactory.produceAndPersist {
            withBooking(cancelledOverlappingBookingForBedInPremisesTwo)
            withReason(cancellationReasonEntityFactory.produceAndPersist())
          }

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          webTestClient.post()
            .uri("/beds/search")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              TemporaryAccommodationBedSearchParameters(
                startDate = LocalDate.parse("2023-08-01"),
                durationDays = 31,
                serviceName = "temporary-accommodation",
                probationDeliveryUnits = listOf(searchPdu.id),
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                BedSearchResults(
                  resultsRoomCount = 2,
                  resultsPremisesCount = 2,
                  resultsBedCount = 2,
                  results = listOf(
                    createTemporaryAccommodationBedSearchResult(
                      premisesOne,
                      roomInPremisesOne,
                      matchingBedInPremisesOne,
                      searchPdu.name,
                      numberOfBeds = 2,
                      numberOfBookedBeds = 1,
                      premisesCharacteristics = listOf(),
                      roomCharacteristics = listOf(),
                      listOf(
                        TemporaryAccommodationBedSearchResultOverlap(
                          name = "${caseSummary.name.forename} ${caseSummary.name.surname}",
                          crn = overlappingBookingForBedInPremisesOne.crn,
                          personType = PersonType.fullPerson,
                          sex = caseSummary.gender!!,
                          days = 15,
                          bookingId = overlappingBookingForBedInPremisesOne.id,
                          roomId = roomInPremisesOne.id,
                          assessmentId = assessment.id,
                        ),
                      ),
                    ),
                    createTemporaryAccommodationBedSearchResult(
                      premisesTwo,
                      roomInPremisesTwo,
                      matchingBedInPremisesTwo,
                      searchPdu.name,
                      numberOfBeds = 2,
                      numberOfBookedBeds = 1,
                      premisesCharacteristics = listOf(),
                      roomCharacteristics = listOf(),
                      overlaps = listOf(
                        TemporaryAccommodationBedSearchResultOverlap(
                          name = "${caseSummary.name.forename} ${caseSummary.name.surname}",
                          crn = overlappingBookingForBedInPremisesTwo.crn,
                          personType = PersonType.fullPerson,
                          sex = caseSummary.gender!!,
                          days = 7,
                          bookingId = overlappingBookingForBedInPremisesTwo.id,
                          roomId = roomInPremisesTwo.id,
                          assessmentId = assessment.id,
                        ),
                      ),
                    ),
                  ),
                ),
              ),
              true,
            )
        }
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed returns results which do not include non-overlapping bookings`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(searchPdu)
          withProbationRegion(probationRegion)
          withStatus(PropertyStatus.active)
        }

        val roomOne = roomEntityFactory.produceAndPersist {
          withPremises(premises)
        }

        val bedOne = bedEntityFactory.produceAndPersist {
          withName("matching bed with no bookings")
          withRoom(roomOne)
        }

        val bedTwo = bedEntityFactory.produceAndPersist {
          withName("matching bed with an non-overlapping booking")
          withRoom(roomOne)
        }

        val nonOverlappingBooking = bookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withPremises(premises)
          withBed(bedTwo)
          withArrivalDate(LocalDate.parse("2024-01-01"))
          withDepartureDate(LocalDate.parse("2024-01-31"))
          withCrn(randomStringMultiCaseWithNumbers(16))
          withId(UUID.randomUUID())
        }

        webTestClient.post()
          .uri("/beds/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            TemporaryAccommodationBedSearchParameters(
              startDate = LocalDate.parse("2023-08-01"),
              durationDays = 31,
              serviceName = "temporary-accommodation",
              probationDeliveryUnits = listOf(searchPdu.id),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              BedSearchResults(
                resultsRoomCount = 1,
                resultsPremisesCount = 1,
                resultsBedCount = 2,
                results = listOf(
                  createTemporaryAccommodationBedSearchResult(
                    premises,
                    roomOne,
                    bedOne,
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createTemporaryAccommodationBedSearchResult(
                    premises,
                    roomOne,
                    bedTwo,
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
            true,
          )
          .jsonPath("$.results[*].overlaps[*].bookingId").value(Matchers.not(nonOverlappingBooking.id))
          .jsonPath("$.results[*].overlaps[*].roomId").value(Matchers.not(nonOverlappingBooking.bed?.room!!.id))
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed returns results which do not consider cancelled bookings as overlapping`() {
      givenAUser(
        probationRegion = probationRegion,
      ) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

          val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(probationRegion)
          }

          val (application, assessment) = createAssessment(user, offenderDetails.otherIds.crn)

          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withProbationRegion(probationRegion)
            withLocalAuthorityArea(localAuthorityArea)
            withProbationDeliveryUnit(searchPdu)
            withProbationRegion(probationRegion)
            withStatus(PropertyStatus.active)
          }

          val roomOne = roomEntityFactory.produceAndPersist {
            withPremises(premises)
          }

          val bedOne = bedEntityFactory.produceAndPersist {
            withName("matching bed with no bookings")
            withRoom(roomOne)
          }

          val bedTwo = bedEntityFactory.produceAndPersist {
            withName("matching bed with a cancelled booking")
            withRoom(roomOne)
          }

          val nonOverlappingBooking = bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withApplication(application)
            withPremises(premises)
            withBed(bedTwo)
            withArrivalDate(LocalDate.parse("2023-07-15"))
            withDepartureDate(LocalDate.parse("2023-08-15"))
            withCrn(offenderDetails.otherIds.crn)
            withId(UUID.randomUUID())
          }

          nonOverlappingBooking.cancellations += cancellationEntityFactory.produceAndPersist {
            withBooking(nonOverlappingBooking)
            withYieldedReason {
              cancellationReasonEntityFactory.produceAndPersist()
            }
          }

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            user.deliusUsername,
          )

          webTestClient.post()
            .uri("/beds/search")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              TemporaryAccommodationBedSearchParameters(
                startDate = LocalDate.parse("2023-09-01"),
                durationDays = 31,
                serviceName = "temporary-accommodation",
                probationDeliveryUnits = listOf(searchPdu.id),
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                BedSearchResults(
                  resultsRoomCount = 1,
                  resultsPremisesCount = 1,
                  resultsBedCount = 2,
                  results = listOf(
                    createTemporaryAccommodationBedSearchResult(
                      premises,
                      roomOne,
                      bedOne,
                      searchPdu.name,
                      numberOfBeds = 2,
                      numberOfBookedBeds = 0,
                      premisesCharacteristics = listOf(),
                      roomCharacteristics = listOf(),
                      overlaps = listOf(),
                    ),
                    createTemporaryAccommodationBedSearchResult(
                      premises,
                      roomOne,
                      bedTwo,
                      searchPdu.name,
                      numberOfBeds = 2,
                      numberOfBookedBeds = 0,
                      premisesCharacteristics = listOf(),
                      roomCharacteristics = listOf(),
                      overlaps = listOf(),
                    ),
                  ),
                ),
              ),
              true,
            )
            .jsonPath("$.results[*].overlaps[*].bookingId").value(Matchers.not(nonOverlappingBooking.id))
            .jsonPath("$.results[*].overlaps[*].roomId").value(Matchers.not(nonOverlappingBooking.bed?.room!!.id))
        }
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bedspace in a Shared Property returns only bedspaces in shared properties`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
        val beds = createPremisesAndBedsWithCharacteristics(
          localAuthorityArea,
          searchPdu,
          BedSearchAttributes.SHARED_PROPERTY,
        )

        val expextedPremisesOneBedOne = beds.first()
        val expextedPremisesOneRoomOne = expextedPremisesOneBedOne.room
        val expextedPremisesTwoBedOne = beds.drop(1).first()
        val expextedPremisesTwoRoomOne = expextedPremisesTwoBedOne.room
        val expextedPremisesThreeBedOne = beds.drop(2).first()
        val expextedPremisesThreeRoomOne = expextedPremisesThreeBedOne.room

        webTestClient.post()
          .uri("/beds/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            TemporaryAccommodationBedSearchParameters(
              startDate = LocalDate.parse("2024-08-27"),
              durationDays = 84,
              serviceName = "temporary-accommodation",
              probationDeliveryUnits = listOf(searchPdu.id),
              attributes = listOf(BedSearchAttributes.SHARED_PROPERTY),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              BedSearchResults(
                resultsRoomCount = 3,
                resultsPremisesCount = 2,
                resultsBedCount = 3,
                results = listOf(
                  createTemporaryAccommodationBedSearchResult(
                    expextedPremisesOneRoomOne.premises,
                    expextedPremisesOneRoomOne,
                    expextedPremisesOneBedOne,
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(
                      CharacteristicPair(
                        propertyName = "isSharedProperty",
                        name = "Shared property",
                      ),
                    ),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createTemporaryAccommodationBedSearchResult(
                    expextedPremisesThreeRoomOne.premises,
                    expextedPremisesThreeRoomOne,
                    expextedPremisesThreeBedOne,
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(
                      CharacteristicPair(
                        propertyName = "isSharedProperty",
                        name = "Shared property",
                      ),
                    ),
                    roomCharacteristics = listOf(
                      CharacteristicPair(
                        propertyName = "isWheelchairAccessible",
                        name = "Wheelchair accessible",
                      ),
                    ),
                    overlaps = listOf(),
                  ),
                  createTemporaryAccommodationBedSearchResult(
                    expextedPremisesTwoRoomOne.premises,
                    expextedPremisesTwoRoomOne,
                    expextedPremisesTwoBedOne,
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(
                      CharacteristicPair(
                        propertyName = "isMenOnly",
                        name = "Men only",
                      ),
                      CharacteristicPair(
                        propertyName = "isSharedProperty",
                        name = "Shared property",
                      ),
                    ),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
            true,
          )
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bedspace in a Single Occupancy Property returns only bedspaces in properties with single occupancy`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
        val beds = createPremisesAndBedsWithCharacteristics(
          localAuthorityArea,
          searchPdu,
          BedSearchAttributes.SINGLE_OCCUPANCY,
        )

        val expextedPremisesOneBedOne = beds.first()
        val expextedPremisesOneRoomOne = expextedPremisesOneBedOne.room
        val expextedPremisesTwoBedOne = beds.drop(1).first()
        val expextedPremisesTwoRoomOne = expextedPremisesTwoBedOne.room
        val expextedPremisesThreeBedOne = beds.drop(2).first()
        val expextedPremisesThreeRoomOne = expextedPremisesThreeBedOne.room

        webTestClient.post()
          .uri("/beds/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            TemporaryAccommodationBedSearchParameters(
              startDate = LocalDate.parse("2024-08-27"),
              durationDays = 84,
              serviceName = "temporary-accommodation",
              probationDeliveryUnits = listOf(searchPdu.id),
              attributes = listOf(BedSearchAttributes.SINGLE_OCCUPANCY),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              BedSearchResults(
                resultsRoomCount = 3,
                resultsPremisesCount = 2,
                resultsBedCount = 3,
                results = listOf(
                  createTemporaryAccommodationBedSearchResult(
                    expextedPremisesOneRoomOne.premises,
                    expextedPremisesOneRoomOne,
                    expextedPremisesOneBedOne,
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    listOf(
                      CharacteristicPair(
                        propertyName = "isSingleOccupancy",
                        name = "Single occupancy",
                      ),
                    ),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createTemporaryAccommodationBedSearchResult(
                    expextedPremisesThreeRoomOne.premises,
                    expextedPremisesThreeRoomOne,
                    expextedPremisesThreeBedOne,
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(
                      CharacteristicPair(
                        propertyName = "isSingleOccupancy",
                        name = "Single occupancy",
                      ),
                    ),
                    roomCharacteristics = listOf(
                      CharacteristicPair(
                        propertyName = "isWheelchairAccessible",
                        name = "Wheelchair accessible",
                      ),
                    ),
                    overlaps = listOf(),
                  ),
                  createTemporaryAccommodationBedSearchResult(
                    expextedPremisesTwoRoomOne.premises,
                    expextedPremisesTwoRoomOne,
                    expextedPremisesTwoBedOne,
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    listOf(
                      CharacteristicPair(
                        propertyName = "isWomenOnly",
                        name = "Women only",
                      ),
                      CharacteristicPair(
                        propertyName = "isSingleOccupancy",
                        name = "Single occupancy",
                      ),
                    ),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
            true,
          )
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation with wheelchair accessible returns only bedspaces with wheelchair accessible`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
        val beds = createPremisesAndBedsWithCharacteristics(
          localAuthorityArea,
          searchPdu,
          BedSearchAttributes.WHEELCHAIR_ACCESSIBLE,
        )

        val expextedPremisesOneBedOne = beds.first()
        val expextedPremisesOneRoomOne = expextedPremisesOneBedOne.room
        val expextedPremisesTwoBedOne = beds.drop(1).first()
        val expextedPremisesTwoRoomOne = expextedPremisesTwoBedOne.room

        webTestClient.post()
          .uri("/beds/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            TemporaryAccommodationBedSearchParameters(
              startDate = LocalDate.parse("2024-08-27"),
              durationDays = 84,
              serviceName = "temporary-accommodation",
              probationDeliveryUnits = listOf(searchPdu.id),
              attributes = listOf(BedSearchAttributes.WHEELCHAIR_ACCESSIBLE),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              BedSearchResults(
                resultsRoomCount = 2,
                resultsPremisesCount = 2,
                resultsBedCount = 2,
                results = listOf(
                  createTemporaryAccommodationBedSearchResult(
                    expextedPremisesOneRoomOne.premises,
                    expextedPremisesOneRoomOne,
                    expextedPremisesOneBedOne,
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(
                      CharacteristicPair(
                        propertyName = "isSharedProperty",
                        name = "Shared property",
                      ),
                    ),
                    roomCharacteristics = listOf(
                      CharacteristicPair(
                        propertyName = "isWheelchairAccessible",
                        name = "Wheelchair accessible",
                      ),
                    ),
                    overlaps = listOf(),
                  ),
                  createTemporaryAccommodationBedSearchResult(
                    expextedPremisesTwoRoomOne.premises,
                    expextedPremisesTwoRoomOne,
                    expextedPremisesTwoBedOne,
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    listOf(
                      CharacteristicPair(
                        propertyName = "isSingleOccupancy",
                        name = "Single occupancy",
                      ),
                    ),
                    roomCharacteristics = listOf(
                      CharacteristicPair(
                        propertyName = "isWheelchairAccessible",
                        name = "Wheelchair accessible",
                      ),
                    ),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
            true,
          )
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed should not return bed when given premises bedspace endDate is same as search start date`() {
      givenAUser { _, jwt ->
        val durationDays = 7
        val searchStartDate = LocalDate.parse("2023-03-23")
        val searchPdu = createTemporaryAccommodationWithBedSpaceEndDate(searchStartDate, searchStartDate)

        searchTemporaryAccommodationBedSpaceAndAssertNoAvailability(jwt, searchStartDate, durationDays, searchPdu.id)
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed should not return bed when given premises bedspace endDate is between search start date and end date`() {
      givenAUser { _, jwt ->
        val durationDays = 7
        val searchStartDate = LocalDate.parse("2023-03-23")
        val bedEndDate = searchStartDate.plusDays(2)
        val searchPdu = createTemporaryAccommodationWithBedSpaceEndDate(searchStartDate, bedEndDate)

        searchTemporaryAccommodationBedSpaceAndAssertNoAvailability(jwt, searchStartDate, durationDays, searchPdu.id)
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed should not return bed when given premises bedspace endDate is same as search end date`() {
      givenAUser { _, jwt ->
        val durationDays = 7
        val searchStartDate = LocalDate.parse("2023-03-23")
        val bedEndDate = searchStartDate.plusDays(durationDays.toLong() - 1)
        val searchPdu = createTemporaryAccommodationWithBedSpaceEndDate(searchStartDate, bedEndDate)

        searchTemporaryAccommodationBedSpaceAndAssertNoAvailability(jwt, searchStartDate, durationDays, searchPdu.id)
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed should not return bed when given premises bedspace endDate less than than search start date`() {
      givenAUser { _, jwt ->
        val durationDays = 7
        val searchStartDate = LocalDate.parse("2023-03-23")
        val bedEndDate = searchStartDate.minusDays(1)
        val searchPdu = createTemporaryAccommodationWithBedSpaceEndDate(searchStartDate, bedEndDate)

        searchTemporaryAccommodationBedSpaceAndAssertNoAvailability(jwt, searchStartDate, durationDays, searchPdu.id)
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed should return single bed when given premises has got 2 rooms where one with endDate and another room without enddate`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val searchStartDate = LocalDate.parse("2023-03-23")
        val durationDays = 7
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(searchPdu)
          withProbationRegion(probationRegion)
          withStatus(PropertyStatus.active)
        }

        val room = roomEntityFactory.produceAndPersist {
          withPremises(premises)
        }

        bedEntityFactory.produceAndPersist {
          withName("not Matching Bed")
          withEndDate { searchStartDate.plusDays(2) }
          withRoom(room)
        }

        val roomWithoutEndDate = roomEntityFactory.produceAndPersist {
          withPremises(premises)
        }

        val bedWithoutEndDate = bedEntityFactory.produceAndPersist {
          withName("Matching Bed")
          withRoom(roomWithoutEndDate)
        }

        webTestClient.post()
          .uri("/beds/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            TemporaryAccommodationBedSearchParameters(
              startDate = searchStartDate,
              durationDays = durationDays,
              serviceName = "temporary-accommodation",
              probationDeliveryUnits = listOf(searchPdu.id),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              BedSearchResults(
                resultsRoomCount = 1,
                resultsPremisesCount = 1,
                resultsBedCount = 1,
                results = listOf(
                  createTemporaryAccommodationBedSearchResult(
                    premises,
                    roomWithoutEndDate,
                    bedWithoutEndDate,
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
          )
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed should return bed when given premises bedspace endDate after search end date`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val durationDays = 7
        val searchStartDate = LocalDate.parse("2023-03-23")
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(searchPdu)
          withProbationRegion(probationRegion)
          withStatus(PropertyStatus.active)
        }

        val room = roomEntityFactory.produceAndPersist {
          withPremises(premises)
        }

        val bed = bedEntityFactory.produceAndPersist {
          withName("Matching Bed")
          withEndDate { searchStartDate.plusDays(durationDays.toLong() + 2) }
          withRoom(room)
        }

        webTestClient.post()
          .uri("/beds/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            TemporaryAccommodationBedSearchParameters(
              startDate = searchStartDate,
              durationDays = durationDays,
              serviceName = "temporary-accommodation",
              probationDeliveryUnits = listOf(searchPdu.id),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              BedSearchResults(
                resultsRoomCount = 1,
                resultsPremisesCount = 1,
                resultsBedCount = 1,
                results = listOf(
                  createTemporaryAccommodationBedSearchResult(
                    premises,
                    room,
                    bed,
                    searchPdu.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
          )
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed should return no bed when given premises has got 2 rooms where one with endDate in the passed and another room with matching end date`() {
      givenAUser { _, jwt ->
        val durationDays = 7
        val searchStartDate = LocalDate.parse("2023-03-23")
        val bedEndDate = searchStartDate.plusDays(1)
        val searchPdu = createTemporaryAccommodationWithBedSpaceEndDate(searchStartDate, bedEndDate)

        searchTemporaryAccommodationBedSpaceAndAssertNoAvailability(jwt, searchStartDate, durationDays, searchPdu.id)
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed should return bed matches searching pdu`() {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withName(randomStringLowerCase(8))
        withProbationRegion(probationRegion)
      }

      val pduTwo = probationDeliveryUnitFactory.produceAndPersist {
        withName(randomStringLowerCase(8))
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val searchStartDate = LocalDate.parse("2024-03-12")
        val durationDays = 7
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val premisesOne = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withName("Premises One")
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(searchPdu)
          withProbationRegion(probationRegion)
          withStatus(PropertyStatus.active)
        }

        val roomOne = roomEntityFactory.produceAndPersist {
          withName("Room One")
          withPremises(premisesOne)
        }

        val bedOne = bedEntityFactory.produceAndPersist {
          withName("Bed One")
          withEndDate { searchStartDate.plusDays(20) }
          withRoom(roomOne)
        }

        val roomTwo = roomEntityFactory.produceAndPersist {
          withName("Room Two")
          withPremises(premisesOne)
        }

        val bedTwo = bedEntityFactory.produceAndPersist {
          withName("Bed Two")
          withRoom(roomTwo)
        }

        val premisesTwo = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withName("Premises Two")
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(pduTwo)
          withProbationRegion(probationRegion)
          withStatus(PropertyStatus.active)
        }

        val roomThree = roomEntityFactory.produceAndPersist {
          withPremises(premisesTwo)
        }

        bedEntityFactory.produceAndPersist {
          withName("Bed Three")
          withEndDate { searchStartDate.plusDays(40) }
          withRoom(roomThree)
        }

        val roomFour = roomEntityFactory.produceAndPersist {
          withPremises(premisesTwo)
        }

        bedEntityFactory.produceAndPersist {
          withName("Bed Four")
          withRoom(roomFour)
        }

        webTestClient.post()
          .uri("/beds/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            TemporaryAccommodationBedSearchParameters(
              startDate = searchStartDate,
              durationDays = durationDays,
              serviceName = "temporary-accommodation",
              probationDeliveryUnits = listOf(searchPdu.id),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              BedSearchResults(
                resultsRoomCount = 2,
                resultsPremisesCount = 1,
                resultsBedCount = 2,
                results = listOf(
                  createTemporaryAccommodationBedSearchResult(
                    premisesOne,
                    roomOne,
                    bedOne,
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createTemporaryAccommodationBedSearchResult(
                    premisesOne,
                    roomTwo,
                    bedTwo,
                    searchPdu.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
            true,
          )
      }
    }

    @Test
    fun `Searching for a Temporary Accommodation Bed in multiple pdus should return bed matches searching pdus`() {
      val pduOne = probationDeliveryUnitFactory.produceAndPersist {
        withName("Probation Delivery Unit One")
        withProbationRegion(probationRegion)
      }

      val pduTwo = probationDeliveryUnitFactory.produceAndPersist {
        withName("Probation Delivery Unit Two")
        withProbationRegion(probationRegion)
      }

      val pduThree = probationDeliveryUnitFactory.produceAndPersist {
        withName("Probation Delivery Unit Three")
        withProbationRegion(probationRegion)
      }

      givenAUser(
        probationRegion = probationRegion,
      ) { _, jwt ->
        val searchStartDate = LocalDate.parse("2024-08-12")
        val durationDays = 7
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val premisesOne = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withName("Premises One")
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(pduOne)
          withProbationRegion(probationRegion)
          withStatus(PropertyStatus.active)
        }

        val (roomOnePremisesOne, bedOnePremisesOne) = createBedspace(premisesOne, "Room One", listOf())
        val (roomTwoPremisesOne, bedTwoPremisesOne) = createBedspace(premisesOne, "Room Two", listOf())

        val premisesTwo = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withName("Premises Two")
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(pduTwo)
          withProbationRegion(probationRegion)
          withStatus(PropertyStatus.active)
        }

        val (roomOnePremisesTwo, bedOnePremisesTwo) = createBedspace(premisesTwo, "Room One", listOf())

        val premisesThree = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withName("Premises Three")
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
          withProbationDeliveryUnit(pduThree)
          withProbationRegion(probationRegion)
          withStatus(PropertyStatus.active)
        }

        val (roomOnePremisesThree, bedOnePremisesThree) = createBedspace(premisesThree, "Room One", listOf())

        webTestClient.post()
          .uri("/beds/search")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            TemporaryAccommodationBedSearchParameters(
              startDate = searchStartDate,
              durationDays = durationDays,
              serviceName = "temporary-accommodation",
              probationDeliveryUnits = listOf(pduOne.id, pduThree.id),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              BedSearchResults(
                resultsRoomCount = 3,
                resultsPremisesCount = 2,
                resultsBedCount = 3,
                results = listOf(
                  createTemporaryAccommodationBedSearchResult(
                    premisesOne,
                    roomOnePremisesOne,
                    bedOnePremisesOne,
                    pduOne.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createTemporaryAccommodationBedSearchResult(
                    premisesOne,
                    roomTwoPremisesOne,
                    bedTwoPremisesOne,
                    pduOne.name,
                    numberOfBeds = 2,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                  createTemporaryAccommodationBedSearchResult(
                    premisesThree,
                    roomOnePremisesThree,
                    bedOnePremisesThree,
                    pduThree.name,
                    numberOfBeds = 1,
                    numberOfBookedBeds = 0,
                    premisesCharacteristics = listOf(),
                    roomCharacteristics = listOf(),
                    overlaps = listOf(),
                  ),
                ),
              ),
            ),
            true,
          )
      }
    }

    private fun searchTemporaryAccommodationBedSpaceAndAssertNoAvailability(
      jwt: String,
      searchStartDate: LocalDate,
      durationDays: Int,
      pduId: UUID,
    ) {
      webTestClient.post()
        .uri("/beds/search")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          TemporaryAccommodationBedSearchParameters(
            startDate = searchStartDate,
            durationDays = durationDays,
            serviceName = "temporary-accommodation",
            probationDeliveryUnits = listOf(pduId),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(
          objectMapper.writeValueAsString(
            BedSearchResults(
              resultsRoomCount = 0,
              resultsPremisesCount = 0,
              resultsBedCount = 0,
              results = listOf(),
            ),
          ),
        )
    }

    private fun createTemporaryAccommodationPremisesWithCharacteristics(
      premisesName: String,
      probationRegion: ProbationRegionEntity,
      localAuthorityArea: LocalAuthorityAreaEntity,
      probationDeliveryUnit: ProbationDeliveryUnitEntity,
      characteristics: MutableList<CharacteristicEntity>,
    ): TemporaryAccommodationPremisesEntity {
      return temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withName(premisesName)
        withProbationRegion(probationRegion)
        withLocalAuthorityArea(localAuthorityArea)
        withProbationDeliveryUnit(probationDeliveryUnit)
        withStatus(PropertyStatus.active)
        withCharacteristics(characteristics)
      }
    }

    private fun createBedspace(premises: PremisesEntity, roomName: String, roomCharacteristics: List<CharacteristicEntity>): Pair<RoomEntity, BedEntity> {
      val room = when {
        roomCharacteristics.isEmpty() -> {
          roomEntityFactory.produceAndPersist {
            withName(roomName)
            withPremises(premises)
          }
        }
        else -> {
          roomEntityFactory.produceAndPersist {
            withName(roomName)
            withPremises(premises)
            withCharacteristicsList(roomCharacteristics)
          }
        }
      }

      val bed = bedEntityFactory.produceAndPersist {
        withName(randomStringMultiCaseWithNumbers(10))
        withRoom(room)
      }

      return Pair(room, bed)
    }

    private fun createTemporaryAccommodationWithBedSpaceEndDate(
      searchStartDate: LocalDate,
      bedEndDate: LocalDate,
    ): ProbationDeliveryUnitEntity {
      val searchPdu = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }
      val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withProbationRegion(probationRegion)
        withLocalAuthorityArea(localAuthorityArea)
        withProbationDeliveryUnit(searchPdu)
        withProbationRegion(probationRegion)
        withStatus(PropertyStatus.active)
      }

      val room = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      bedEntityFactory.produceAndPersist {
        withName("not Matching Bed")
        withEndDate { bedEndDate }
        withRoom(room)
      }

      val roomWithoutEndDate = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      bedEntityFactory.produceAndPersist {
        withName("Not Matching Bed")
        withEndDate { searchStartDate.minusDays(5) }
        withRoom(roomWithoutEndDate)
      }
      return searchPdu
    }

    @SuppressWarnings("LongParameterList")
    private fun createTemporaryAccommodationBedSearchResult(
      premises: PremisesEntity,
      room: RoomEntity,
      bed: BedEntity,
      pduName: String,
      numberOfBeds: Int,
      numberOfBookedBeds: Int,
      premisesCharacteristics: List<CharacteristicPair>,
      roomCharacteristics: List<CharacteristicPair>,
      overlaps: List<TemporaryAccommodationBedSearchResultOverlap>,
    ): TemporaryAccommodationBedSearchResult {
      return TemporaryAccommodationBedSearchResult(
        premises = BedSearchResultPremisesSummary(
          id = premises.id,
          name = premises.name,
          addressLine1 = premises.addressLine1,
          postcode = premises.postcode,
          probationDeliveryUnitName = pduName,
          characteristics = premisesCharacteristics,
          addressLine2 = premises.addressLine2,
          town = premises.town,
          notes = premises.notes,
          bedCount = numberOfBeds,
          bookedBedCount = numberOfBookedBeds,
        ),
        room = BedSearchResultRoomSummary(
          id = room.id,
          name = room.name,
          characteristics = roomCharacteristics,
        ),
        bed = BedSearchResultBedSummary(
          id = bed.id,
          name = bed.name,
        ),
        serviceName = ServiceName.temporaryAccommodation,
        overlaps = overlaps,
      )
    }

    private fun createPremisesAndBedsWithCharacteristics(
      localAuthorityArea: LocalAuthorityAreaEntity,
      pdu: ProbationDeliveryUnitEntity,
      bedSearchAttribute: BedSearchAttributes,
    ): List<BedEntity> {
      val premisesSingleOccupancyCharacteristic = characteristicRepository.findByName("Single occupancy")
      val premisesSharedPropertyCharacteristic = characteristicRepository.findByName("Shared property")
      val premisesMenOnlyCharacteristic = characteristicRepository.findByName("Men only")
      val premisesWomenOnlyCharacteristic = characteristicRepository.findByName("Women only")
      val premisesPubNearbyCharacteristic = characteristicRepository.findByName("Pub nearby")
      val wheelchairAccessibleCharacteristic = characteristicRepository.findByName("Wheelchair accessible")
      var beds = listOf<BedEntity>()

      val premisesSingleOccupancy = createTemporaryAccommodationPremisesWithCharacteristics(
        "Premises Single Occupancy",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesSingleOccupancyCharacteristic!!),
      )

      val (singleOccupancyRoomOne, singleOccupancyBedOne) = createBedspace(premisesSingleOccupancy, "Premises Single Occupancy Room One", listOf())
      val (premisesSingleOccupancyWheelchairAccessibleRoomOne, premisesSingleOccupancyWheelchairAccessibleBedOne) = createBedspace(
        premisesSingleOccupancy,
        "Premises Single Occupancy with Wheelchair Accessible Room One",
        listOf(wheelchairAccessibleCharacteristic!!),
      )

      val premisesSharedProperty = createTemporaryAccommodationPremisesWithCharacteristics(
        "Premises Shared Property",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesSharedPropertyCharacteristic!!),
      )

      val (sharedPropertyRoomOne, sharedPropertyBedOne) = createBedspace(premisesSharedProperty, "Premises Shared Property Room One", listOf())
      val (premisesSharedPropertyWheelchairAccessibleRoomOne, premisesSharedPropertyWheelchairAccessibleBedOne) = createBedspace(
        premisesSharedProperty,
        "Premises Shared Property with Wheelchair Accessible Room One",
        listOf(wheelchairAccessibleCharacteristic),
      )

      val premisesMenOnly = createTemporaryAccommodationPremisesWithCharacteristics(
        "Premises Men Only",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesMenOnlyCharacteristic!!),
      )

      val (premisesMenOnlyRoomOne, premisesMenOnlyBedOne) = createBedspace(premisesMenOnly, "Premises Men Only Room One", listOf())

      val premisesWomenOnly = createTemporaryAccommodationPremisesWithCharacteristics(
        "Premises Women Only",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesWomenOnlyCharacteristic!!),
      )

      val (premisesWomenOnlyRoomOne, premisesWomenOnlyBedOne) = createBedspace(premisesWomenOnly, "Premises Women Only Room One", listOf())

      val premisesPubNearby = createTemporaryAccommodationPremisesWithCharacteristics(
        "Premises Pub Nearby",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesPubNearbyCharacteristic!!),
      )

      val (premisesPubNearbyRoomOne, premisesPubNearbyBedOne) = createBedspace(premisesPubNearby, "Premises Pub Nearby Room One", listOf())

      val premisesSingleOccupancyWomenOnly = createTemporaryAccommodationPremisesWithCharacteristics(
        "Premises Single Occupancy - Women Only",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesWomenOnlyCharacteristic, premisesSingleOccupancyCharacteristic),
      )

      val (premisesSingleOccupancyWomenOnlyRoomOne, premisesSingleOccupancyWomenOnlyBedOne) = createBedspace(
        premisesSingleOccupancyWomenOnly,
        "Premises Single Occupancy Women Only Room One",
        listOf(),
      )

      val premisesSharedPropertyMenOnly = createTemporaryAccommodationPremisesWithCharacteristics(
        "Premises Shared Property - Men Only",
        probationRegion,
        localAuthorityArea,
        pdu,
        mutableListOf(premisesMenOnlyCharacteristic, premisesSharedPropertyCharacteristic),
      )

      val (premisesSharedPropertyMenOnlyRoomOne, premisesSharedPropertyMenOnlyBedOne) = createBedspace(
        premisesSharedPropertyMenOnly,
        "Premises Shared Property Men Only Room One",
        listOf(),
      )

      when (bedSearchAttribute) {
        BedSearchAttributes.SINGLE_OCCUPANCY -> beds = listOf(
          singleOccupancyBedOne,
          premisesSingleOccupancyWomenOnlyBedOne,
          premisesSingleOccupancyWheelchairAccessibleBedOne,
        )

        BedSearchAttributes.SHARED_PROPERTY -> beds = listOf(
          sharedPropertyBedOne,
          premisesSharedPropertyMenOnlyBedOne,
          premisesSharedPropertyWheelchairAccessibleBedOne,
        )

        BedSearchAttributes.WHEELCHAIR_ACCESSIBLE ->
          beds =
            listOf(premisesSharedPropertyWheelchairAccessibleBedOne, premisesSingleOccupancyWheelchairAccessibleBedOne)
      }
      return beds
    }

    private fun createAssessment(user: UserEntity, crn: String): Pair<TemporaryAccommodationApplicationEntity, AssessmentEntity> {
      val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
        withPermissiveSchema()
      }

      val assessmentSchema = temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
        withPermissiveSchema()
        withAddedAt(OffsetDateTime.now())
      }

      val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
        withCrn(crn)
        withCreatedByUser(user)
        withProbationRegion(user.probationRegion)
        withApplicationSchema(applicationSchema)
      }

      val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
        withApplication(application)
        withAssessmentSchema(assessmentSchema)
      }
      assessment.schemaUpToDate = true

      return Pair(application, assessment)
    }
  }
}
