-- ${flyway:timestamp}
insert into
  "characteristics" (
    "id",
    "is_active",
    "model_scope",
    "name",
    "property_name",
    "service_scope"
  )
values
  (
    '199334d3-fabb-432f-84c3-0e92eaf13f24',
    true,
    '*',
    'Pub nearby',
    NULL,
    'approved-premises'
  ),
  (
    '94021062-f692-4877-b6e8-f36c7ff87a18',
    true,
    '*',
    'Not suitable for registered sex offenders (RSO)',
    NULL,
    'approved-premises'
  ),
  (
    '7846fbf2-b423-4ccc-b23b-d8b866f86bde',
    true,
    '*',
    'Men only',
    NULL,
    'approved-premises'
  ),
  (
    'a862be08-a96a-4337-9f46-26286db8015f',
    true,
    '*',
    'Wheelchair accessible',
    NULL,
    'approved-premises'
  ),
  (
    'fffb3004-5f0a-4e88-8350-fb89a0168296',
    true,
    'premises',
    'Park nearby',
    NULL,
    'temporary-accommodation'
  ),
  (
    '684f919a-4c4a-4e80-9b3a-1dcd35873b3f',
    true,
    'premises',
    'Pub nearby',
    NULL,
    'temporary-accommodation'
  ),
  (
    '78c5d99b-9702-4bf2-a23d-c2a0cf3a017d',
    true,
    'premises',
    'School nearby',
    NULL,
    'temporary-accommodation'
  ),
  (
    '2fff6ede-7035-4e8d-81ad-5fcd894b99cf',
    true,
    'premises',
    'Men only',
    NULL,
    'temporary-accommodation'
  ),
  (
    '8221f1ad-3aaf-406a-b918-dbdef956ea17',
    true,
    'premises',
    'Women only',
    NULL,
    'temporary-accommodation'
  ),
  (
    '7dd3bac5-3d1c-4acb-b110-b1614b2c95d8',
    true,
    'room',
    'Shared kitchen',
    NULL,
    'temporary-accommodation'
  ),
  (
    'e730bdea-6157-4910-b1b0-450b29bf0c9f',
    true,
    'room',
    'Shared bathroom',
    NULL,
    'temporary-accommodation'
  ),
  (
    'd2f7796a-88e5-4e53-ab6d-dabb145b6a60',
    true,
    '*',
    'Wheelchair accessible',
    NULL,
    'temporary-accommodation'
  ),
  (
    '50328e4a-e786-4299-995e-08ae7501a1b4',
    true,
    'premises',
    'Is this an IAP?',
    'isIAP',
    'approved-premises'
  ),
  (
    '18da0ec2-b691-4ebd-a19e-a09778da451e',
    true,
    'premises',
    'Is this AP a PIPE?',
    'isPIPE',
    'approved-premises'
  ),
  (
    '20afb651-bfe8-45a5-88c5-8964da8345e5',
    true,
    'premises',
    'Is this AP an Enhanced Security AP?',
    'isESAP',
    'approved-premises'
  ),
  (
    '151e21e2-cad0-4338-84ee-404edbacf07c',
    true,
    'premises',
    'Is this AP semi specialist Mental Health?',
    'isSemiSpecialistMentalHealth',
    'approved-premises'
  ),
  (
    'c238722f-624c-4d91-8e62-3559374a259a',
    true,
    'premises',
    'Is this a Recovery Focussed AP?',
    'isRecoveryFocussed',
    'approved-premises'
  ),
  (
    '6885e2ac-9d8f-4586-a46a-c8fa26b39935',
    true,
    'premises',
    'Does this AP accept people who have committed sexual offences against adults?',
    'acceptsSexOffenders',
    'approved-premises'
  ),
  (
    'efc4072c-928c-4e82-9cd7-816d7c16ce4b',
    true,
    'premises',
    'Does this AP accept people who have committed sexual offences against children?',
    'acceptsChildSexOffenders',
    'approved-premises'
  ),
  (
    '0b8eea77-c498-473a-9b99-6c3deec80ec1',
    true,
    'premises',
    'Does this AP accept people who have committed non-sexual offences against children?',
    'acceptsNonSexualChildOffenders',
    'approved-premises'
  ),
  (
    'b4d7903f-78dd-4885-8edd-d2bdc4250729',
    true,
    'premises',
    'Does this AP accept people who have been convicted of hate crimes?',
    'acceptsHateCrimeOffenders',
    'approved-premises'
  ),
  (
    '3bcecef8-eacb-47c2-a28d-6de5e88a683e',
    true,
    'premises',
    'Is this AP Catered (no for Self Catered)?',
    'isCatered',
    'approved-premises'
  ),
  (
    '03df74e0-6d4b-408a-9553-34f22a6bfd73',
    true,
    'premises',
    'Is there a step free entrance to the AP at least 900mm wide?',
    'hasWideStepFreeAccess',
    'approved-premises'
  ),
  (
    '7dc00a6f-a99f-4bb6-8b3a-10862594c99c',
    true,
    'premises',
    'Are corridors leading to communal areas at least 1.2m wide?',
    'hasWideAccessToCommunalAreas',
    'approved-premises'
  ),
  (
    '308ab2a2-bd0b-40dd-9cfa-d8fa0e7925bf',
    true,
    'premises',
    'Do corridors leading to communal areas have step free access?',
    'hasStepFreeAccessToCommunalAreas',
    'approved-premises'
  ),
  (
    'f287a211-2d05-4b7e-af97-78b8aae9dab5',
    true,
    'premises',
    'Does this AP have bathroom facilities that have been adapted for wheelchair users?',
    'hasWheelChairAccessibleBathrooms',
    'approved-premises'
  ),
  (
    '52c7163f-1410-4f57-974d-e59f045ff6e2',
    true,
    'premises',
    'Is there a lift at this AP?',
    'hasLift',
    'approved-premises'
  ),
  (
    'e8f9e6db-3f6f-44ab-8c25-465a00c7908a',
    true,
    'premises',
    'Does this AP have tactile & directional flooring?',
    'hasTactileFlooring',
    'approved-premises'
  ),
  (
    '29aacc17-38e5-47e9-93c7-3eb5523b82c2',
    true,
    'premises',
    'Does this AP have signs in braille?',
    'hasBrailleSignage',
    'approved-premises'
  ),
  (
    '1b5c5dc0-c309-4d1c-a340-d391733c62ff',
    true,
    'premises',
    'Does this AP have a hearing loop?',
    'hasHearingLoop',
    'approved-premises'
  ),
  (
    '3da09890-8414-4ed9-ba98-d6ae873a854f',
    true,
    'premises',
    'Are there any additional restrictions on people that this AP can accommodate?',
    'additionalRestrictions',
    'approved-premises'
  ),
  (
    '83377a71-6cda-4f83-90ca-32513f401500',
    true,
    'room',
    'Is this room located on the ground floor?',
    'IsGroundFloor',
    'approved-premises'
  ),
  (
    '64c0fe8f-939e-4be1-815e-e2e045d5945d',
    true,
    'room',
    'Is the room using only furnishings and bedding supplied by FM?',
    'isFullyFm',
    'approved-premises'
  ),
  (
    'e78626a6-5285-4d54-8eed-db430f4416da',
    true,
    'room',
    'Does this room have Crib7 rated bedding?',
    'hasCrib7Bedding',
    'approved-premises'
  ),
  (
    '4410500d-5bda-4790-a82a-be4af09f2930',
    true,
    'room',
    'Is there a smoke/heat detector in the room?',
    'hasSmokeDetector',
    'approved-premises'
  ),
  (
    '151f8b9a-2877-4e21-b057-ac33ec7e705d',
    true,
    'room',
    'Is this room on the top floor with at least one external wall and not located directly next to a fire exit or a protected stairway?',
    'isTopFloorVulnerable',
    'approved-premises'
  ),
  (
    'a37320ca-a004-460d-b0ef-e14fd8d9529c',
    true,
    'room',
    'Is the room close to the admin/staff office on the ground floor with at least one external wall and not located directly next to a fire exit or a protected stairway?',
    'isGroundFloorNrOffice',
    'approved-premises'
  ),
  (
    'a7f4250f-3956-4a80-9f58-bfc75d101423',
    true,
    'room',
    'is there a water mist extinguisher in close proximity to this room?',
    'hasNearbySprinkler',
    'approved-premises'
  ),
  (
    'a4ba038b-f762-4f19-ae94-2e308637a5ed',
    true,
    'room',
    'Is this room suitable for people who pose an arson risk? (Must answer yes to Q; 6 & 7, and 9 or  10)',
    'isArsonSuitable',
    'approved-premises'
  ),
  (
    '4349fd01-9924-43de-a146-c58018489666',
    true,
    'room',
    'Is this room currently a designated arson room?',
    'isArsonDesignated',
    'approved-premises'
  ),
  (
    '7de46bc5-5624-461d-b413-1a297563fcd3',
    true,
    'room',
    'If IAP - Is there any insurance conditions that prevent a person with arson convictions being placed?',
    'hasArsonInsuranceConditions',
    'approved-premises'
  ),
  (
    '26c02b5d-699d-4452-9981-78f884575e15',
    true,
    'room',
    'Is this room suitable for people convicted of sexual offences?',
    'isSuitedForSexOffenders',
    'approved-premises'
  ),
  (
    '138f63aa-7e51-4581-8d71-dfdf9b85bbd9',
    true,
    'room',
    'Does this room have en-suite bathroom facilities?',
    'hasEnSuite',
    'approved-premises'
  ),
  (
    'a68df538-dd8c-4148-b10e-1682f853a4e5',
    true,
    'room',
    'Are corridors leading to this room of sufficient width to accommodate a wheelchair? (at least 1.2m wide)',
    'isWheelchairAccessible',
    'approved-premises'
  ),
  (
    '1d763a66-f1ec-4d24-9ecc-26267b336ea9',
    true,
    'room',
    'Is the door to this room at least 900mm wide?',
    'hasWideDoor',
    'approved-premises'
  ),
  (
    '8a8033ce-5481-4f77-912b-e57ae4502b0d',
    true,
    'room',
    'Is there step free access to this room and in corridors leading to this room?',
    'hasStepFreeAccess',
    'approved-premises'
  ),
  (
    '76f70761-a2fc-418b-ad35-bd2039f8a808',
    true,
    'room',
    'Are there fixed mobility aids in this room?',
    'hasFixedMobilityAids',
    'approved-premises'
  ),
  (
    'e655ea1e-ac26-4dbe-afb6-2d3410d0b383',
    true,
    'room',
    'Does this room have at least a 1500mmx1500mm turning space?',
    'hasTurningSpace',
    'approved-premises'
  ),
  (
    '2723991d-6fa6-404e-ab15-bae83ca0a43b',
    true,
    'room',
    'Is there provision for people to call for assistance from this room?',
    'hasCallForAssistance',
    'approved-premises'
  ),
  (
    'c48b41dc-fe38-4697-b7ea-4dcab1638240',
    true,
    'room',
    'Can this room be designated as suitable for wheelchair users?   Must answer yes to Q23-26 on previous sheet and Q17-21 on this sheet)',
    'isWheelchairDesignated',
    'approved-premises'
  ),
  (
    'ec8bd119-58ce-4933-b093-830078f99976',
    true,
    'room',
    'Can this room be designated as suitable for people requiring step free access? (Must answer yes to Q23 and 25 on previous sheet and Q19 on this sheet)',
    'isStepFreeDesignated',
    'approved-premises'
  ),
  (
    '99bb0f33-ff92-4606-9d1c-43bcf0c42ef4',
    true,
    '*',
    'Ground floor level access',
    NULL,
    'temporary-accommodation'
  ),
  (
    '62c4d8cf-b612-4110-9e27-5c29982f9fcf',
    false,
    '*',
    'Not suitable for arson offenders',
    NULL,
    'temporary-accommodation'
  ),
  (
    '2183d873-8270-4e93-8518-3a668a053689',
    false,
    'room',
    'Lift access',
    NULL,
    'temporary-accommodation'
  ),
  (
    '12e2e689-b3fb-469d-baec-2fb68e15e85b',
    false,
    'room',
    'Single bed',
    NULL,
    'temporary-accommodation'
  ),
  (
    '08b756e2-0b82-4f49-a124-35ea4ebb1634',
    false,
    'room',
    'Double bed',
    NULL,
    'temporary-accommodation'
  ),
  (
    '0dd72992-0e04-4d69-8858-7471cbcb7c8e',
    true,
    'premises',
    'Is this AP suitable for people at risk of criminal exploitation? N.B Enhanced Security sites answer No, other AP''s answer Yes.',
    'isSuitableForVulnerable',
    'approved-premises'
  ),
  (
    '7d59280e-aca0-4842-994e-b22cbc076fe8',
    true,
    'room',
    'Is this a single room?',
    'isSingle',
    'approved-premises'
  ),
  (
    '81c1ac10-ba12-4a78-9d8c-34d3b5528e5d',
    true,
    'room',
    'Is this room located on the ground floor?',
    'isGroundFloor',
    'approved-premises'
  ),
  (
    'a2dd4661-c7fe-4294-b3fd-3fc877e62aa5',
    false,
    '*',
    'Not suitable for registered sex offenders (RSO)',
    NULL,
    'temporary-accommodation'
  ),
  (
    'c0fc6e07-4ca3-45ac-88ea-375479e1419f',
    true,
    'premises',
    'Not suitable for those with an arson history',
    NULL,
    'temporary-accommodation'
  ),
  (
    'cb7447cd-d629-4c89-be93-cd48c1060af8',
    true,
    'premises',
    'Not suitable for those who pose a sexual risk to adults',
    NULL,
    'temporary-accommodation'
  ),
  (
    '8fcf6f60-bbca-4425-b0e8-9dfbe88f3aa6',
    true,
    'premises',
    'Not suitable for those who pose a sexual risk to children',
    NULL,
    'temporary-accommodation'
  ),
  (
    '454a5ff4-d87a-43f9-8989-135bcc47a635',
    true,
    'premises',
    'Single occupancy',
    NULL,
    'temporary-accommodation'
  ),
  (
    '62a38d3a-4797-4b0f-8681-7befea1035a4',
    true,
    'premises',
    'Shared property',
    NULL,
    'temporary-accommodation'
  ),
  (
    'e2868d4f-cb8f-4a4b-ae27-6b887a63d37c',
    true,
    'premises',
    'Shared entrance',
    NULL,
    'temporary-accommodation'
  ),
  (
    'da0e86b3-fce8-42b6-af0c-ac6f905611e6',
    true,
    'premises',
    'Lift available',
    NULL,
    'temporary-accommodation'
  ),
  (
    '1fb6000b-7c55-44cc-bb8c-38835c99ab99',
    true,
    'premises',
    'Sensitive let',
    NULL,
    'temporary-accommodation'
  ),
  (
    '7f805b33-048e-4034-9ded-21947bf5c728',
    true,
    'premises',
    'Close proximity',
    NULL,
    'temporary-accommodation'
  ),
  (
    '78adf0c6-01cb-4b1a-be3e-1a3f5346a497',
    true,
    'premises',
    'Rural/out of town',
    NULL,
    'temporary-accommodation'
  ),
  (
    '9fb21592-7cca-4ef2-98ca-5fcef22c8ee6',
    true,
    '*',
    'Other – please state in notes',
    NULL,
    'temporary-accommodation'
  )
  ON CONFLICT (id) DO NOTHING;