package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailAssessmentEntity
import java.util.UUID

@Repository
interface ApprovedPremisesApplicationTestRepository : JpaRepository<ApprovedPremisesApplicationEntity, UUID>

@Repository
interface Cas2ApplicationTestRepository : JpaRepository<Cas2ApplicationEntity, UUID>

@Repository
interface Cas2BailApplicationTestRepository : JpaRepository<Cas2BailApplicationEntity, UUID>

@Repository
interface Cas2BailAssessmentTestRepository : JpaRepository<Cas2BailAssessmentEntity, UUID>

@Repository
interface TemporaryAccommodationApplicationTestRepository : JpaRepository<TemporaryAccommodationApplicationEntity, UUID>
