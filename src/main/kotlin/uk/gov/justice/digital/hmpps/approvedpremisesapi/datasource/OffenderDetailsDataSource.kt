package uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource

import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.UserOffenderAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asOffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asUserOffenderAccess

@Deprecated(
  """
    This service was introduced as a 'man in the middle' whilst migration from community-api to ap-and-delius-context, 
    allowing us to switch between the two backends via configuration. That configuration has been removed and it now
    always uses ap-and-delius-context.
    
    This service now introduces unnecessary complexity and it should no longer be used
    """,
  ReplaceWith("[OffenderService]"),
)
@Component
class OffenderDetailsDataSource(
  val apDeliusContextApiClient: ApDeliusContextApiClient,
) {
  fun getOffenderDetailSummary(crn: String): ClientResult<OffenderDetailSummary> {
    return getOffenderDetailSummaries(listOf(crn)).values.first()
  }

  @Suppress("UNCHECKED_CAST", "MagicNumber") // Safe as we only do this for non-success types
  fun getOffenderDetailSummaries(crns: List<String>): Map<String, ClientResult<OffenderDetailSummary>> {
    if (crns.size > 500) {
      throw InternalServerErrorProblem("Cannot bulk request more than 500 CRNs. ${crns.size} has been provided.")
    }

    return when (val clientResult = apDeliusContextApiClient.getSummariesForCrns(crns)) {
      is ClientResult.Success -> {
        val crnToAccessResult = clientResult.body.cases.associateBy(
          keySelector = { it.crn },
          valueTransform = { it },
        )

        crns.associateBy(
          keySelector = { it },
          valueTransform = { crn ->
            val access = crnToAccessResult[crn]

            if (access != null) {
              clientResult.copyWithBody(body = access.asOffenderDetailSummary())
            } else {
              ClientResult.Failure.StatusCode(
                method = HttpMethod.GET,
                path = "/probation-cases/summaries",
                status = HttpStatus.NOT_FOUND,
                body = null,
              )
            }
          },
        )
      }
      else -> return crns.associateWith { clientResult as ClientResult<OffenderDetailSummary> }
    }
  }

  fun getUserAccessForOffenderCrn(
    deliusUsername: String,
    crn: String,
  ): ClientResult<UserOffenderAccess> {
    return getUserAccessForOffenderCrns(deliusUsername, listOf(crn)).values.first()
  }

  @Suppress("UNCHECKED_CAST", "MagicNumber") // Safe as we only do this for non-success types
  fun getUserAccessForOffenderCrns(
    deliusUsername: String,
    crns: List<String>,
  ): Map<String, ClientResult<UserOffenderAccess>> {
    if (crns.size > 500) {
      throw InternalServerErrorProblem("Cannot bulk request more than 500 CRNs. ${crns.size} has been provided.")
    }

    return when (val clientResult = apDeliusContextApiClient.getUserAccessForCrns(deliusUsername, crns)) {
      is ClientResult.Success -> {
        val crnToAccessResult = clientResult.body.access.associateBy(
          keySelector = { it.crn },
          valueTransform = { it },
        )

        crns.associateBy(
          keySelector = { it },
          valueTransform = { crn ->
            val access = crnToAccessResult[crn]

            if (access != null) {
              clientResult.copyWithBody(body = access.asUserOffenderAccess())
            } else {
              ClientResult.Failure.StatusCode(
                method = HttpMethod.GET,
                path = "/users/access?username=$deliusUsername",
                status = HttpStatus.NOT_FOUND,
                body = null,
              )
            }
          },
        )
      }
      else -> crns.associateWith { clientResult as ClientResult<UserOffenderAccess> }
    }
  }
}
