package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class SeedColumns(
  val columns: Map<String, String>,
) {
  fun getStringOrNull(label: String) = columns[label]?.trim()?.ifBlank { null }

  fun getUuidOrNull(label: String): UUID? = getStringOrNull(label)?.let { UUID.fromString(it) }

  fun getIntOrNull(label: String) = columns[label]?.toIntOrNull()

  fun getLongOrNull(label: String) = columns[label]?.toLongOrNull()

  fun getLastDateTimeFromListOrNull(label: String, formatter: DateTimeFormatter): LocalDateTime? {
    val rawValue = getStringOrNull(label) ?: return null

    val lastDateTime = rawValue.split(",").last()
    return LocalDateTime.parse(lastDateTime, formatter)
  }

  fun getDateTimeFromUtcDateTimeOrNull(label: String, formatter: DateTimeFormatter): LocalDateTime? {
    val rawValue = getStringOrNull(label) ?: return null
    return LocalDateTime.parse(rawValue, formatter)
  }

  fun getDateFromUtcDateTimeOrNull(label: String): LocalDate? {
    val rawValue = getStringOrNull(label) ?: return null
    return LocalDate.parse(rawValue.substring(startIndex = 0, endIndex = 10))
  }

  fun getCommaSeparatedValues(label: String): List<String> {
    val rawValue = getStringOrNull(label) ?: return emptyList()

    return rawValue.split(",").filter(String::isNotBlank).map(String::trim)
  }

  fun getYesNoBooleanOrNull(label: String): Boolean? {
    val rawValue = getStringOrNull(label)?.uppercase() ?: return null

    return when (rawValue) {
      "YES" -> {
        true
      }
      "NO" -> {
        false
      }
      else -> {
        error("'$rawValue' is not a recognised boolean for '$label' (use yes | no)")
      }
    }
  }
}
