package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult.Failure
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.PersonName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DomainEventTransformer

@ExtendWith(MockKExtension::class)
class DomainEventTransformerTest {

  @MockK
  lateinit var apDeliusContextApiClient: ApDeliusContextApiClient

  @InjectMockKs
  lateinit var domainEventTransformer: DomainEventTransformer

  @Test
  fun `toProbationArea success`() {
    val staffDetail = StaffDetailFactory.staffDetail(
      probationArea = ProbationArea("theProbationCode", "theProbationDescription"),
    )

    val result = domainEventTransformer.toProbationArea(staffDetail)

    assertThat(result.code).isEqualTo("theProbationCode")
    assertThat(result.name).isEqualTo("theProbationDescription")
  }

  @Test
  fun `toStaffMember(UserEntity) returns successfully`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withDeliusStaffCode("theStaffCode")
      .withName("theForenames theSurname")
      .withDeliusUsername("theUsername")
      .produce()

    val staffDetail = StaffDetailFactory.staffDetail(
      code = "theStaffCode",
      name = PersonName("theForenames", "theSurname"),
      deliusUsername = "theUsername",
      probationArea = ProbationArea("theProbationCode", "theProbationDescription"),
    )

    every { apDeliusContextApiClient.getStaffDetail(user.deliusUsername) } returns ClientResult.Success(
      HttpStatus.OK,
      staffDetail,
    )

    val result = domainEventTransformer.toStaffMember(user)

    assertThat(result.staffCode).isEqualTo("theStaffCode")
    assertThat(result.forenames).isEqualTo("theForenames")
    assertThat(result.surname).isEqualTo("theSurname")
    assertThat(result.username).isEqualTo("theUsername")
  }

  @Test
  fun `toStaffMember(UserEntity) fails`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withDeliusStaffCode("theStaffCode")
      .withName("theForenames theSurname")
      .withDeliusUsername("theUsername")
      .produce()

    val response = Failure.StatusCode<StaffDetail>(
      HttpMethod.GET,
      "/",
      HttpStatus.BAD_REQUEST,
      "",
    )

    val expectedException = response.toException()

    every { apDeliusContextApiClient.getStaffDetail(user.deliusUsername) } returns response

    val exception = assertThrows<RuntimeException> { domainEventTransformer.toStaffMember(user) }

    assertThat(exception.message).isEqualTo(expectedException.message)
  }

  @Test
  fun `toWithdrawnBy from staff details success`() {
    val staffDetail = StaffDetailFactory.staffDetail(
      code = "theStaffCode",
      name = PersonName("theForenames", "theSurname", "theMiddleName"),
      deliusUsername = "theUsername",
      probationArea = ProbationArea("theProbationCode", "theProbationDescription"),
    )

    val result = domainEventTransformer.toWithdrawnBy(staffDetail)

    val staffMember = result.staffMember
    assertThat(staffMember.staffCode).isEqualTo("theStaffCode")
    assertThat(staffMember.forenames).isEqualTo("theForenames theMiddleName")
    assertThat(staffMember.surname).isEqualTo("theSurname")
    assertThat(staffMember.username).isEqualTo("theUsername")

    val probationArea = result.probationArea
    assertThat(probationArea.code).isEqualTo("theProbationCode")
    assertThat(probationArea.name).isEqualTo("theProbationDescription")
  }

  @Test
  fun `toWithdrawnBy from user success`() {
    val staffDetail = StaffDetailFactory.staffDetail(
      code = "theStaffCode",
      name = PersonName("theForenames", "theSurname"),
      deliusUsername = "theUsername",
      probationArea = ProbationArea("theProbationCode", "theProbationDescription"),
    )

    val user = UserEntityFactory().withDefaultProbationRegion().produce()

    every {
      apDeliusContextApiClient.getStaffDetail(user.deliusUsername)
    } returns ClientResult.Success(HttpStatus.OK, staffDetail)

    val result = domainEventTransformer.toWithdrawnBy(user)

    val staffMember = result.staffMember
    assertThat(staffMember.staffCode).isEqualTo("theStaffCode")
    assertThat(staffMember.forenames).isEqualTo("theForenames")
    assertThat(staffMember.surname).isEqualTo("theSurname")
    assertThat(staffMember.username).isEqualTo("theUsername")

    val probationArea = result.probationArea
    assertThat(probationArea.code).isEqualTo("theProbationCode")
    assertThat(probationArea.name).isEqualTo("theProbationDescription")
  }

  @Test
  fun `toWithdrawnBy from user failure`() {
    val user = UserEntityFactory().withDefaultProbationRegion().produce()

    every {
      apDeliusContextApiClient.getStaffDetail(user.deliusUsername)
    } returns Failure.CachedValueUnavailable(user.deliusUsername)

    assertThatThrownBy { domainEventTransformer.toWithdrawnBy(user) }
  }
}
