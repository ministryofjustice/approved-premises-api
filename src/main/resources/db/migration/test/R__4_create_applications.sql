
-- ${flyway:timestamp}
TRUNCATE TABLE applications CASCADE;
TRUNCATE TABLE assessments CASCADE;


DO $$
DECLARE
   applicationData json := '{"basic-information":{"sentence-type":{"sentenceType":"bailPlacement"},"situation":{"situation":"bailSentence"},"release-date":{"releaseDate-year":"2022","releaseDate-month":"11","releaseDate-day":"14","releaseDate":"2022-11-14","knowReleaseDate":"yes"},"placement-date":{"startDateSameAsReleaseDate":"yes"},"placement-purpose":{"placementPurposes":["drugAlcoholMonitoring","otherReason"],"otherReason":"Reason"}},"type-of-ap":{"ap-type":{"type":"standard"}},"oasys-import":{"optional-oasys-sections":{"needsLinkedToReoffending":[{"section":1,"name":"accommodation","linkedToHarm":false,"linkedToReOffending":true},{"section":2,"name":"relationships","linkedToHarm":false,"linkedToReOffending":true}],"otherNeeds":[{"section":3,"name":"emotional","linkedToHarm":false,"linkedToReOffending":false},{"section":4,"name":"thinking","linkedToHarm":false,"linkedToReOffending":false}]},"rosh-summary":{"roshAnswers":["Some answer for the first RoSH question. With an extra comment 1","Some answer for the second RoSH question. With an extra comment 2","Some answer for the third RoSH question. With an extra comment 3"],"roshSummaries":[{"questionNumber":"1","label":"The first RoSH question","answer":"Some answer for the first RoSH question"},{"questionNumber":"2","label":"The second RoSH question","answer":"Some answer for the second RoSH question"},{"questionNumber":"3","label":"The third RoSH question","answer":"Some answer for the third RoSH question"}]},"offence-details":{"offenceDetailsAnswers":["Some answer for the first offence details question. With an extra comment 1","Some answer for the second offence details question. With an extra comment 2","Some answer for the third offence details question. With an extra comment 3"],"offenceDetailsSummaries":[{"questionNumber":"1","label":"The first offence details question","answer":"Some answer for the first offence details question"},{"questionNumber":"2","label":"The second offence details question","answer":"Some answer for the second offence details question"},{"questionNumber":"3","label":"The third offence details question","answer":"Some answer for the third offence details question"}]},"supporting-information":{"supportingInformationAnswers":["Some answer for the first supporting information question. With an extra comment 1","Some answer for the second supporting information question. With an extra comment 2","Some answer for the third supporting information question. With an extra comment 3"],"supportingInformationSummaries":[{"questionNumber":"1","label":"The first supporting information question","answer":"Some answer for the first supporting information question"},{"questionNumber":"2","label":"The second supporting information question","answer":"Some answer for the second supporting information question"},{"questionNumber":"3","label":"The third supporting information question","answer":"Some answer for the third supporting information question"}]},"risk-management-plan":{"riskManagementAnswers":["Some answer for the first risk management question. With an extra comment 1","Some answer for the second risk management question. With an extra comment 2","Some answer for the third risk management question. With an extra comment 3"],"riskManagementSummaries":[{"questionNumber":"1","label":"The first risk management question","answer":"Some answer for the first risk management question"},{"questionNumber":"2","label":"The second risk management question","answer":"Some answer for the second risk management question"},{"questionNumber":"3","label":"The third risk management question","answer":"Some answer for the third risk management question"}]},"risk-to-self":{"riskToSelfAnswers":["Some answer for the first risk to self question. With an extra comment 1","Some answer for the second risk to self question. With an extra comment 2","Some answer for the third risk to self question. With an extra comment 3"],"riskToSelfSummaries":[{"questionNumber":"1","label":"The first risk to self question","answer":"Some answer for the first risk to self question"},{"questionNumber":"2","label":"The second risk to self question","answer":"Some answer for the second risk to self question"},{"questionNumber":"3","label":"The third risk to self question","answer":"Some answer for the third risk to self question"}]}},"risk-management-features":{"risk-management-features":{"manageRiskDetails":"est vero nesciunt","additionalFeaturesDetails":"deleniti enim necessitatibus"},"convicted-offences":{"response":"yes"},"type-of-convicted-offence":{"offenceConvictions":["arson","sexualOffence","hateCrimes","childNonSexualOffence"]},"date-of-offence":{"arsonOffence":["current"],"hateCrime":["previous"],"inPersonSexualOffence":["current","previous"]},"rehabilitative-interventions":{"rehabilitativeInterventions":["accommodation","drugsAndAlcohol","childrenAndFamilies","health","educationTrainingAndEmployment","financeBenefitsAndDebt","attitudesAndBehaviour","abuse","other"],"otherIntervention":"Another"}},"prison-information":{"case-notes":{"caseNoteIds":["a30173ca-061f-42c9-a1a2-28c70b282d3f","4a477187-b77f-4fcc-a919-43a6633ee868"],"selectedCaseNotes":[{"authorName":"Denise Collins","id":"a30173ca-061f-42c9-a1a2-28c70b282d3f","createdAt":"2022-11-10","occurredAt":"2022-10-19","sensitive":false,"subType":"Ressettlement","type":"Social Care","note":"Note 1"},{"authorName":"Leticia Mann","id":"4a477187-b77f-4fcc-a919-43a6633ee868","createdAt":"2022-07-24","occurredAt":"2022-09-22","sensitive":true,"subType":"Quality Work","type":"General","note":"Note 2"}],"moreDetail":"some details","adjudications":[{"id":69927,"reportedAt":"2022-10-09","establishment":"Hawthorne","offenceDescription":"Nam vel nisi fugiat veniam possimus omnis.","hearingHeld":false,"finding":"NOT_PROVED"},{"id":39963,"reportedAt":"2022-07-10","establishment":"Oklahoma City","offenceDescription":"Illum maxime enim explicabo soluta sequi voluptas.","hearingHeld":true,"finding":"PROVED"},{"id":77431,"reportedAt":"2022-05-30","establishment":"Jurupa Valley","offenceDescription":"Quis porro nemo voluptates doloribus atque quis provident iure.","hearingHeld":false,"finding":"PROVED"}]}},"location-factors":{"describe-location-factors":{"postcodeArea":"SW1","positiveFactors":"Some positive factors","restrictions":"yes","restrictionDetail":"Restrictions go here","alternativeRadiusAccepted":"yes","alternativeRadius":"70","differentPDU":"no"}},"access-and-healthcare":{"access-needs":{"additionalNeeds":["mobility","learningDisability","neurodivergentConditions"],"religiousOrCulturalNeeds":"yes","religiousOrCulturalNeedsDetails":"sunt at minus","needsInterpreter":"yes","interpreterLanguage":"beatae eligendi explicabo","careActAssessmentCompleted":"yes"},"access-needs-mobility":{"needsWheelchair":"yes","mobilityNeeds":"laboriosam","visualImpairment":"exercitationem"},"covid":{"fullyVaccinated":"yes","highRisk":"yes","additionalCovidInfo":"additional info"}},"further-considerations":{"room-sharing":{"riskToStaff":"no","riskToStaffDetail":"","riskToOthers":"no","riskToOthersDetail":"","sharingConcerns":"yes","sharingConcernsDetail":"Some details here","traumaConcerns":"no","traumaConcernsDetail":"","sharingBenefits":"no","sharingBenefitsDetail":""},"vulnerability":{"exploitable":"no","exploitableDetail":"","exploitOthers":"yes","exploitOthersDetail":"Some details here"},"previous-placements":{"previousPlacement":"yes","previousPlacementDetail":"Some details here"},"complex-case-board":{"complexCaseBoard":"yes","complexCaseBoardDetail":"Some details here"},"catering":{"catering":"yes","cateringDetail":"Some details here"},"arson":{"arson":"yes","arsonDetail":"Some details here"}},"move-on":{"placement-duration":{"duration":"10","durationDetail":"Some more information"},"relocation-region":{"postcodeArea":"XX1"},"plans-in-place":{"arePlansInPlace":"yes"},"type-of-accommodation":{"accommodationType":"foreignNational","otherAccommodationType":""},"foreign-national":{"response":"yes","date-year":"2015","date-month":"1","date-day":"1","date":"2015-01-01"}},"attach-required-documents":{"attach-documents":{"selectedDocuments":[{"id":"aeb43e06-a4a1-460f-9acf-e2495de84604","level":"Offender","fileName":"Random offender document1.pdf","createdAt":"2019-09-10T00:00:00Z","typeCode":"OFFENDER_DOCUMENT","typeDescription":"Offender related","description":"Some Description 1"},{"id":"5f8a2288-0a17-4e45-afd5-5e57794a7b53","level":"Conviction","fileName":"national.asm","createdAt":"2022-09-13","typeCode":"serenade","typeDescription":"porter","description":"Some Description 2"}]}},"check-your-answers":{"review":{"reviewed":"1"}}}';
   applicationDocument json := '{"basic-information":[{"Which of the following best describes the sentence type?":"Standard determinate custody"},{"What type of release will the application support?":"Release on Temporary License (ROTL)"},{"Do you know Justyn Flatley’s release date?":"Yes","Release Date":"Saturday 1 April 2023"},{"Is Saturday 1 April 2023 the date you want the placement to start?":"Yes"},{"What is the purpose of the AP placement?":"Prevent contact, Help individual readjust to life outside custody"}],"type-of-ap":[{"Which type of AP does Justyn Flatley require?":"Standard"}],"oasys-import":[{"Needs linked to reoffending":"9. Alcohol","Needs not linked to risk of serious harm or reoffending":"5. Finance, 11. Thinking and behavioural"},{"R10.1":"[R10.1] IN CUSTODY\r\n \r\n KNOWN ADULTS:\r\n Such as Ms Elaine Underhill and any of the victims of the index offence if they are placed close to Mr Smith cell.\r\n \r\n CHILDREN:\r\n Simon Smith, Roger Smith, Lindy Smith","R10.2":"[R10.2] IN CUSTODY:\r\n \r\n KNOWN ADULTS:\r\n Intimidation, threats of violence, use of weapons or boiling water, physical education and violent assault, long term psychological impact as a result of Mr Smith violent behaviour. This harm may be cause in the course of physical altercation due to seeking revenge or holding grudges...","R10.3":"[R10.3] STATIC RISK FACTORS:\r\n \r\n Mr Smith gender - At the time of the offence, Mr Smith was 35 years old and considered to be in the higher risk group.\r\n \r\n STABLE DYNAMIC RISK FACTORS:\r\n Thinking and behaviour - limited ability to manage mood or emotions poor impulse control. Mr Smith demonstrated cognitive deficit in committing the index offences and this still remains a concern...","R10.4":"[R10.4] CIRCUMSTANCES:\r\n A lack of stable accommodation\r\n unemployment\r\n non constructive use of time\r\n breakdown of family support\r\n being unable to access benefits\r\n \r\n UNDERLYING FACTORS:\r\n pro criminal attitudes supporting commission of crime...","R10.5":"[R10.5] Engage with Mental Health Services in prison and in the community.\r\n Maintain emotional stability.\r\n Abstain from alcohol.\r\n Abstain from illegal drugs.\r\n Regular testing for alcohol and drug use, in prison and on Licence.\r\n Maintain stable accommodation..."},{"2.1":"","2.4.1":"","2.4.2":"","2.12":"","2.5":"","2.8.3":"","2.98":""},{"3.9":"[3.9] Mr Smith told me that he was renting a room in a shared house, prior to his current remand...","4.9":"[4.9] He said that he has since learnt to read and write during periods of custody and an initial assessment of his...","5.9":"[5.9] Mr Smith was in receipt of Job Seekers Allowance prior to his current remand in custody...","9.9":"[9.9] Mr Smith told me that he rarely consumes alcohol and has not had any problems associated with excessive drinking in years...","11.9":"[11.9] The circumstances of the current offence suggest that Mr Smith acted ..."},{"RM28":"[RM28] Mr Smith is currently on remand awaiting sentence","RM35":"[RM35] Placeholder content for additional comments","RM34":"[RM34] Discuss any concerns with line manager/SPO Increase frequency of reporting Bring forward MAPPA/Consider Level 2 MAPPA professionals meetings  considered if MAPPA not applicable. Consider referral to Approved Premises Consider motivational interviewing/engagement officer.  Joint interview with police OM or other relevant agencies? To raise concerns Referral to substance misues worker for additional support. Warning letters to be employed Recall/Breach if risk not manageable.","RM33":"[RM33] Undertake sentence planning with OS within 8 or 16 weeks of sentence carats - continue to work with Mr Smith and refer for relevant programmes during any prison sentence imposed. DIP supervise DRR is imposed or provide support post release if conditions/requirements to manage the specific risks.","RM32":"[RM32] Requires referral to community drug DIP upon  release. To link in with employment/training resources such as College and ETEO. Completion of victim empathy module via 1-1 supervision to build upon awareness of impact of offending.   Level setting to be undertaken by Senior Probation Officer and OMU. Multi Agency Public Protection Panel to be convened if level 2 either at start of supervision or 6 months prior to release","RM31":"[RM31] State they will have secure accommodation for Mr Smith  and partner on release, although she too is subject to a DRR at present. Supportive wider family.  Has completed previously Short Duration programme and attended one of the support sessions .  Engaged with CARATS in custody.  Complied with voluntary drug tests in custody. Some motivation to desist from alcohol/drug use in the future, this needs to be encouraged with use of community resources. Completed drug and heroin awareness programmes in custody.","RM30":"[RM30] Probation Officer, Education training and employment Officer, Prison Offender Supervisor","RM28.1":"[RM28.1] Currently on remand at HMP Wandsworth - Management of case under MAPPA - level not yet set."},{"R8.1.1":"Review 06.10.21:\r\n \r\n         There have been numerous ACCTs opened since 2013 and every subsequent year he has been in custody.  In 2021...","R8.2.1":"Has told prison staff that he swallowed batteries on one occasion due to wanting..","R8.3.1":"Review 06.10.21:\r\n \r\n         A previous assessor opined that Mr Smith was displaying all the characteristics of someone..."}],"risk-management-features":[{"Describe why an AP placement is needed to manage the risk of Justyn Flatley":"Some words","Provide details of any additional measures that will be necessary for the management of risk":"Some words"},{"Has Justyn Flatley ever been convicted of any arson offences, sexual offences, hate crimes or non-sexual offences against children?":"No"},{"Which rehabilitative interventions will support the person''s Approved Premises (AP) placement?":"Drugs and alcohol, Children and families"}],"prison-information":[{"Selected prison case notes that support this application":["Array"],"Adjudications":["Array"]}],"location-factors":[{"What is the preferred location for the AP placement?":"WS1","Give details of any positive factors for the person in this location.":"Some words","Are there any restrictions linked to placement location?":"No","If an AP Placement is not available in the persons preferred area, would a placement further away be considered?":"No","Is the person moving to a different area where they''ll be managed by a different probation delivery unit (PDU)?":"No"},{"Have you agreed Justyn Flatley''s transfer/supervision with the receiving PDU?":"No, I still need to make arrangements"}],"access-and-healthcare":[{"Does Justyn Flatley have any of the following needs?":"Learning disability, hearing impairment","Does Justyn Flatley have any religious or cultural needs?":"No","Details of religious or cultural needs":"","Does Justyn Flatley need an interpreter?":"No","Has a care act assessment been completed?":"No"},{"Has Justyn Flatley been fully vaccinated for COVID-19?":"No","Is Justyn Flatley at a higher risk from COVID-19?":"No"}],"further-considerations":[{"Is there any evidence that the person may pose a risk to AP staff?":"No","Is there any evidence that the person may pose a risk to other AP residents?":"No","Do you have any concerns about the person sharing a bedroom?":"No","Is there any evidence of previous trauma or significant event in the persons history which would indicate that room share may not be suitable?":"No","Is there potential for the person to benefit from a room share?":"No"},{"Are you aware that Justyn Flatley is vulnerable to exploitation from others?":"No","Is there any evidence or expectation that Justyn Flatley may groom, radicalise or exploit others?":"No"},{"Has Justyn Flatley stayed or been offered a placement in an AP before?":"No"},{"Does Justyn Flatley''s gender identity require a complex case board to review their application?":"No"},{"Do you have any concerns about Justyn Flatley catering for themselves?":"No"},{"Does Justyn Flatley need a specialist arson room?":"No"}],"move-on":[{"What duration of placement do you recommend?":"11 weeks","Provide any additional information":"Some words"},{"Where is Justyn Flatley most likely to live when they move on from the AP?":"WS13"},{"Are move on arrangements already in place for when the person leaves the AP?":"No"},{"What type of accommodation will Justyn Flatley have when they leave the AP?":"Supported housing"}],"attach-required-documents":[{"PRE-CONS.pdf":"Previous convictions as of 01/09/2019","Personal circumstances.pdf.pdf":"Personal circumstance of AP - Medication in Posession  - Assessment started on 11/09/2019","NSIOffender.pdf":"Non Statutory Intervention for OPD Community Pathway on 16/10/2019","paroleParom1Report_04092019_122116_OMIC_A_X320741.pdf":"Parole Assessment Report at Berwyn (HMP) requested on 05/09/2019"}],"check-your-answers":[{}]}';
BEGIN


  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at",
    "noms_number"
  )
  values
    (
      'fe1d197f-9025-489e-8f3a-a38c40be3d5c',
      CURRENT_DATE + 28,
      'f9ff1c6e-6876-4ba8-8ca9-d7d2c6f673dc',
      '315VWWC',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 16,
      'M59CS58'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      'fe1d197f-9025-489e-8f3a-a38c40be3d5c',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '76b4a657-bcb0-41e4-9efc-b22b8df2fc17',
      'fe1d197f-9025-489e-8f3a-a38c40be3d5c',
      '531455f4-c76f-4943-b4eb-3c02d8fefa69',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at",
    "noms_number"
  )
  values
    (
      'a1fac092-1acb-4288-978f-a9844167b98b',
      CURRENT_DATE + 6,
      '531455f4-c76f-4943-b4eb-3c02d8fefa69',
      '4Y29R9P',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 28,
      'XWU5JWQ'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      'a1fac092-1acb-4288-978f-a9844167b98b',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '652a578d-9c26-423e-8a57-ad3337674190',
      'a1fac092-1acb-4288-978f-a9844167b98b',
      '7a424213-3a0c-45b0-9a51-4977243c2b21',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at",
    "noms_number"
  )
  values
    (
      '31482b90-33f5-49ac-86ef-b60df16a7812',
      CURRENT_DATE + 27,
      '7a424213-3a0c-45b0-9a51-4977243c2b21',
      '4ZUIHFX',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 12,
      '5A5C8WL'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '31482b90-33f5-49ac-86ef-b60df16a7812',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      'd36fe117-c760-4bd0-9649-89c21834fe95',
      '31482b90-33f5-49ac-86ef-b60df16a7812',
      '531455f4-c76f-4943-b4eb-3c02d8fefa69',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at",
    "noms_number"
  )
  values
    (
      '384c27ba-0f03-470c-9e98-599db620e303',
      CURRENT_DATE + 20,
      '7a424213-3a0c-45b0-9a51-4977243c2b21',
      '52W7TQG',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 25,
      '5HBWEG1'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '384c27ba-0f03-470c-9e98-599db620e303',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '39db6071-b8ce-4575-8066-048b2db1ff69',
      '384c27ba-0f03-470c-9e98-599db620e303',
      '68715a03-06af-49ee-bae5-039c824ab9af',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at",
    "noms_number"
  )
  values
    (
      'd1fa54b2-ffb9-4ac7-91db-154dab8e6a5e',
      CURRENT_DATE + 0,
      '7e8d1738-a07d-4ba4-a8a7-9b7d9c9d27b2',
      '5EC66UT',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 15,
      '530X5EC'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      'd1fa54b2-ffb9-4ac7-91db-154dab8e6a5e',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '6cfd4d77-6273-4c42-becb-bf7790355462',
      'd1fa54b2-ffb9-4ac7-91db-154dab8e6a5e',
      '6dcd2559-2d14-4feb-8faf-89ad30dfa765',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at",
    "noms_number"
  )
  values
    (
      '7279e532-a436-4eb1-b898-c2af4062f601',
      CURRENT_DATE + 9,
      '8a39870c-3a1f-4e05-ad45-a450e15b242d',
      '8LO3HSH',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 22,
      'JD5CLIA'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '7279e532-a436-4eb1-b898-c2af4062f601',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      'b89f5cd6-3b9c-4f9b-ab2c-0ae812240be3',
      '7279e532-a436-4eb1-b898-c2af4062f601',
      'f9ff1c6e-6876-4ba8-8ca9-d7d2c6f673dc',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at",
    "noms_number"
  )
  values
    (
      'ed8719e5-646d-4c64-bd32-3cd290b609a2',
      CURRENT_DATE + 21,
      '68715a03-06af-49ee-bae5-039c824ab9af',
      'BWEFOI7',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 15,
      'SX9UI94'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      'ed8719e5-646d-4c64-bd32-3cd290b609a2',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '93786321-6ac3-473b-8504-c5e4ce32d93a',
      'ed8719e5-646d-4c64-bd32-3cd290b609a2',
      '7a424213-3a0c-45b0-9a51-4977243c2b21',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at",
    "noms_number"
  )
  values
    (
      'f4aad217-048a-4ff7-9897-76f6cf2ad6dc',
      CURRENT_DATE + 5,
      '68715a03-06af-49ee-bae5-039c824ab9af',
      'GSR1T2F',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 26,
      'KXTJQEF'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      'f4aad217-048a-4ff7-9897-76f6cf2ad6dc',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '9b74922b-da80-430d-abfd-eb7494dd26bc',
      'f4aad217-048a-4ff7-9897-76f6cf2ad6dc',
      '6dcd2559-2d14-4feb-8faf-89ad30dfa765',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at",
    "noms_number"
  )
  values
    (
      '14e56cfe-00ad-4137-88f9-50c1b10d2bb2',
      CURRENT_DATE + 13,
      '531455f4-c76f-4943-b4eb-3c02d8fefa69',
      'HRV83TE',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 23,
      'N1RRJZU'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '14e56cfe-00ad-4137-88f9-50c1b10d2bb2',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '0ba78bae-aa42-4bd2-97d6-4e50c20949f8',
      '14e56cfe-00ad-4137-88f9-50c1b10d2bb2',
      '68715a03-06af-49ee-bae5-039c824ab9af',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at",
    "noms_number"
  )
  values
    (
      '92e90a5b-9ccd-40d4-93d3-3bff7c1ad6c9',
      CURRENT_DATE + 3,
      '531455f4-c76f-4943-b4eb-3c02d8fefa69',
      'HTVI42B',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 2,
      '6X1DNW1'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '92e90a5b-9ccd-40d4-93d3-3bff7c1ad6c9',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '8ffe3515-6a52-43d1-9848-789c9428d2d6',
      '92e90a5b-9ccd-40d4-93d3-3bff7c1ad6c9',
      '7a424213-3a0c-45b0-9a51-4977243c2b21',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at",
    "noms_number"
  )
  values
    (
      '6bdbe139-5b6c-457a-95e9-3acb6e1f1bfb',
      CURRENT_DATE + 0,
      '531455f4-c76f-4943-b4eb-3c02d8fefa69',
      'HUN3BN0',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 15,
      'YCSW8BD'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '6bdbe139-5b6c-457a-95e9-3acb6e1f1bfb',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      'b2092cbf-837f-4046-8d87-66990965fe0f',
      '6bdbe139-5b6c-457a-95e9-3acb6e1f1bfb',
      '68715a03-06af-49ee-bae5-039c824ab9af',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at",
    "noms_number"
  )
  values
    (
      '9f757c8c-9d1e-4bcb-932f-391fdba33391',
      CURRENT_DATE + 9,
      '6dcd2559-2d14-4feb-8faf-89ad30dfa765',
      'IHGHXYM',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 24,
      'KWAPTC7'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '9f757c8c-9d1e-4bcb-932f-391fdba33391',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '5277c81b-d500-4d24-9bc0-f5d8eb18fd7a',
      '9f757c8c-9d1e-4bcb-932f-391fdba33391',
      '531455f4-c76f-4943-b4eb-3c02d8fefa69',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at",
    "noms_number"
  )
  values
    (
      '4afbacac-a8c2-45d7-9f4d-10b40695edff',
      CURRENT_DATE + 8,
      '531455f4-c76f-4943-b4eb-3c02d8fefa69',
      'JCRH9V5',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 18,
      'VTDINBA'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '4afbacac-a8c2-45d7-9f4d-10b40695edff',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      'a1285cc1-8482-45cf-8ff7-4f04d64f2f7d',
      '4afbacac-a8c2-45d7-9f4d-10b40695edff',
      '68715a03-06af-49ee-bae5-039c824ab9af',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at",
    "noms_number"
  )
  values
    (
      'd81ba3cf-feb8-4e43-8e10-175c18a75bdd',
      CURRENT_DATE + 24,
      '7a424213-3a0c-45b0-9a51-4977243c2b21',
      'N6OUTAY',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 14,
      'HIR0PIN'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      'd81ba3cf-feb8-4e43-8e10-175c18a75bdd',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      'bb5444b4-ba08-42d2-aba6-48ee774f42b5',
      'd81ba3cf-feb8-4e43-8e10-175c18a75bdd',
      '6dcd2559-2d14-4feb-8faf-89ad30dfa765',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at",
    "noms_number"
  )
  values
    (
      'b0b17e97-f3ac-49dd-95b3-1073d7ee14b3',
      CURRENT_DATE + 14,
      '68715a03-06af-49ee-bae5-039c824ab9af',
      'PI251LM',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 22,
      'QGH3OL6'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      'b0b17e97-f3ac-49dd-95b3-1073d7ee14b3',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '03bccc5b-0354-4559-879b-677d1f1b232e',
      'b0b17e97-f3ac-49dd-95b3-1073d7ee14b3',
      '7e8d1738-a07d-4ba4-a8a7-9b7d9c9d27b2',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at",
    "noms_number"
  )
  values
    (
      '2bd7afdd-d861-477c-a6c7-85395bb0d6b4',
      CURRENT_DATE + 26,
      '6dcd2559-2d14-4feb-8faf-89ad30dfa765',
      'PR5E5Y2',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 1,
      'M9XWJ1S'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '2bd7afdd-d861-477c-a6c7-85395bb0d6b4',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      'd29156d8-24f0-4d6e-8222-10d9dc403582',
      '2bd7afdd-d861-477c-a6c7-85395bb0d6b4',
      '6dcd2559-2d14-4feb-8faf-89ad30dfa765',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at",
    "noms_number"
  )
  values
    (
      '667a407f-609a-4ebc-bf67-4e36e1df7e35',
      CURRENT_DATE + 28,
      '6dcd2559-2d14-4feb-8faf-89ad30dfa765',
      'QA93YYK',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 27,
      'YX2YNT2'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '667a407f-609a-4ebc-bf67-4e36e1df7e35',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '0ae6f411-0957-4790-b853-8883af16f577',
      '667a407f-609a-4ebc-bf67-4e36e1df7e35',
      '8a39870c-3a1f-4e05-ad45-a450e15b242d',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at",
    "noms_number"
  )
  values
    (
      '71db43c7-b9d0-4830-8102-0542b0ee4277',
      CURRENT_DATE + 11,
      '7e8d1738-a07d-4ba4-a8a7-9b7d9c9d27b2',
      'XCMSG3I',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 20,
      'NYVS303'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '71db43c7-b9d0-4830-8102-0542b0ee4277',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      'd354aa03-a2a0-473d-8f42-b6f450ff681d',
      '71db43c7-b9d0-4830-8102-0542b0ee4277',
      '6dcd2559-2d14-4feb-8faf-89ad30dfa765',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at",
    "noms_number"
  )
  values
    (
      '34ca7199-09c6-469f-ab00-c7bc10a598db',
      CURRENT_DATE + 3,
      '8a39870c-3a1f-4e05-ad45-a450e15b242d',
      'YRPARSH',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 10,
      'HBVE0LJ'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '34ca7199-09c6-469f-ab00-c7bc10a598db',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '348fee4c-d625-426b-ab41-864ff480413b',
      '34ca7199-09c6-469f-ab00-c7bc10a598db',
      '7a424213-3a0c-45b0-9a51-4977243c2b21',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at",
    "noms_number"
  )
  values
    (
      '5e575fe9-28a7-4f7c-ba1a-7ae264c0ed15',
      CURRENT_DATE + 8,
      '7e8d1738-a07d-4ba4-a8a7-9b7d9c9d27b2',
      'Z33A1BU',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 10,
      '6DO89QY'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '5e575fe9-28a7-4f7c-ba1a-7ae264c0ed15',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '30d9ce53-50e2-4282-b823-7e4ada027c73',
      '5e575fe9-28a7-4f7c-ba1a-7ae264c0ed15',
      '6dcd2559-2d14-4feb-8faf-89ad30dfa765',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  
END $$;
