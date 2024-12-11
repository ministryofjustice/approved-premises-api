package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2bail

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailApplicationEntity

@Service("Cas2BailUserAccessService")
class Cas2BailUserAccessService {
  fun userCanViewCas2BailApplication(user: NomisUserEntity, application: Cas2BailApplicationEntity): Boolean {
    return if (user.id == application.createdByUser.id) {
      true
    } else if (application.submittedAt == null) {
      false
    } else {
      offenderIsFromSamePrisonAsUser(application.referringPrisonCode, user.activeCaseloadId)
    }
  }

  fun offenderIsFromSamePrisonAsUser(referringPrisonCode: String?, activeCaseloadId: String?): Boolean {
    return if (referringPrisonCode !== null && activeCaseloadId !== null) {
      activeCaseloadId == referringPrisonCode
    } else {
      false
    }
  }
}
