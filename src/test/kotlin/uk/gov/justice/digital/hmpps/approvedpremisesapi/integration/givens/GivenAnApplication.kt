package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.OffsetDateTime

@Suppress("LongParameterList")
fun IntegrationTestBase.givenACas1Application(
  createdByUser: UserEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  submittedAt: OffsetDateTime? = null,
  eventNumber: String = randomInt(1, 9).toString(),
  isWomensApplication: Boolean? = null,
  block: (application: ApplicationEntity) -> Unit = {},
) = givenAnApplication(createdByUser, crn, submittedAt, eventNumber, isWomensApplication, block)

@Suppress("LongParameterList")
fun IntegrationTestBase.givenAnApplication(
  createdByUser: UserEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  submittedAt: OffsetDateTime? = null,
  eventNumber: String = randomInt(1, 9).toString(),
  isWomensApplication: Boolean? = null,
  block: (application: ApplicationEntity) -> Unit = {},
): ApprovedPremisesApplicationEntity {
  val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
    withPermissiveSchema()
  }

  val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
    withCrn(crn)
    withCreatedByUser(createdByUser)
    withApplicationSchema(applicationSchema)
    withSubmittedAt(submittedAt)
    withEventNumber(eventNumber)
    withIsWomensApplication(isWomensApplication)
  }

  block(application)

  return application
}

fun IntegrationTestBase.givenASubmittedApplication(
  createdByUser: UserEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  block: (application: ApplicationEntity) -> Unit = {},
): ApprovedPremisesApplicationEntity {
  return givenAnApplication(
    createdByUser,
    crn,
    OffsetDateTime.now(),
  ) { application ->
    block(application)
  }
}
