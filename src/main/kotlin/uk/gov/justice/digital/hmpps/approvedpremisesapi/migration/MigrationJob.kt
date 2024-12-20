package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

abstract class MigrationJob {
  abstract val shouldRunInTransaction: Boolean
  abstract fun process(pageSize: Int = 10)
}
