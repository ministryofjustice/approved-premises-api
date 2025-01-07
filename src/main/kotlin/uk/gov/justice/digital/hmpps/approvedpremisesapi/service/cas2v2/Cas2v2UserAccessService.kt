package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationEntity

@Service("Cas2v2UserAccessService")
class Cas2v2UserAccessService {
  fun userCanViewCas2v2Application(user: NomisUserEntity, application: Cas2v2ApplicationEntity): Boolean {
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
