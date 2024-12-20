package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class SeedColumns(
  val columns: Map<String, String>,
) {
  fun getStringOrNull(label: String) = columns[label]?.trim()?.ifBlank { null }

  fun getUuidOrNull(label: String) = columns[label]?.let { UUID.fromString(it.trim()) }

  fun getIntOrNull(label: String) = columns[label]?.toIntOrNull()

  fun getLongOrNull(label: String) = columns[label]?.toLongOrNull()

  fun getLastDateTimeFromListOrNull(label: String, formatter: DateTimeFormatter): LocalDateTime? {
    val rawValue = getStringOrNull(label) ?: return null

    val lastDateTime = rawValue.split(",").last()
    return LocalDateTime.parse(lastDateTime, formatter)
  }

  fun getDateFromUtcDateTimeOrNull(label: String): LocalDate? {
    val rawValue = getStringOrNull(label) ?: return null

    return LocalDate.parse(rawValue.substring(startIndex = 0, endIndex = 10))
  }

  fun getStringsFromListOrNull(label: String): List<String> {
    val rawValue = getStringOrNull(label) ?: return emptyList()

    return rawValue.split(",")
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
