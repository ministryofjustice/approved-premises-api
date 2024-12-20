package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Borough
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Ldu
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.PersonName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Team
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomEmailAddress
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomNumberChars
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate

object StaffDetailFactory {
  @Suppress("LongParameterList")
  fun staffDetail(
    email: String? = randomEmailAddress(),
    telephoneNumber: String? = randomNumberChars(11),
    teams: List<Team> = listOf(team()),
    probationArea: ProbationArea = probationArea(),
    deliusUsername: String? = randomStringUpperCase(10),
    name: PersonName = PersonName(randomStringLowerCase(6), randomStringLowerCase(6), randomStringLowerCase(6)),
    code: String = randomStringUpperCase(10),
    active: Boolean = true,
  ) =
    StaffDetail(
      email = email,
      telephoneNumber = telephoneNumber,
      teams = teams,
      probationArea = probationArea,
      username = deliusUsername,
      name = name,
      code = code,
      active = active,
    )

  fun team() =
    Team(
      code = randomStringMultiCaseWithNumbers(10),
      name = randomStringMultiCaseWithNumbers(10),
      ldu = Ldu(code = randomStringUpperCase(10), name = randomStringUpperCase(10)),
      borough = Borough(randomStringMultiCaseWithNumbers(5), randomStringMultiCaseWithNumbers(25)),
      startDate = LocalDate.now().minusYears(1),
      endDate = null,
    )

  fun probationArea() =
    ProbationArea(code = randomStringUpperCase(10), description = randomStringMultiCaseWithNumbers(50))
}
