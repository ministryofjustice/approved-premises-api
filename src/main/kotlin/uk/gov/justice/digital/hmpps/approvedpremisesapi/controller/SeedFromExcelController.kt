package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.SeedFromExcelApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedService

@Service
class SeedFromExcelController(private val seedService: SeedService) : SeedFromExcelApiDelegate {
  override fun seedFromExcelPost(seedFromExcelRequest: SeedFromExcelRequest): ResponseEntity<Unit> {
    throwIfNotLoopbackRequest()

    seedService.seedExcelData(seedFromExcelRequest.seedType, seedFromExcelRequest.premisesId, seedFromExcelRequest.fileName)

    return ResponseEntity(HttpStatus.ACCEPTED)
  }
}
