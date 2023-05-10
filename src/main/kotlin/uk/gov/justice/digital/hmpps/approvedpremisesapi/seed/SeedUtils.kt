package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

fun String?.trimToNull(): String? {
  if (this.isNullOrBlank()) return null

  return this.trim()
}

fun String?.trimToEmpty(): String {
  return this.trimToNull() ?: ""
}

fun appendCharacteristicIfSet(
  columns: Map<String, String>,
  characteristics: MutableList<String>,
  characteristicName: String,
  csvColumnName: String? = null,
) {

  val columnName = csvColumnName ?: "$characteristicName"

  if (columns[columnName].toBoolean()) {
    characteristics.add(characteristicName)
  }
}

fun getCanonicalRegionName(regionName: String): String {
  return when (regionName.canonicalise()) {
    "east midlands" -> "East Midlands"
    "east of england" -> "East of England"
    "greater manchester" -> "Greater Manchester"
    "kent" -> "Kent, Surrey & Sussex"
    "surrey and sussex" -> "Kent, Surrey & Sussex"
    "kent surrey and sussex" -> "Kent, Surrey & Sussex"
    "london" -> "London"
    "north east" -> "North East"
    "north west" -> "North West"
    "south central" -> "South Central"
    "south west" -> "South West"
    "wales" -> "Wales"
    "west midlands" -> "West Midlands"
    "yorkshire and the humber" -> "Yorkshire & The Humber"
    else -> regionName
  }
}

fun getCanonicalLocalAuthorityName(localAuthorityName: String): String {
  return when (localAuthorityName.canonicalise()) {
    "aberdeen city" -> "Aberdeen City"
    "aberdeenshire" -> "Aberdeenshire"
    "adur" -> "Adur"
    "allerdale" -> "Allerdale"
    "amber valley" -> "Amber Valley"
    "angus" -> "Angus"
    "antrim and newtownabbey" -> "Antrim and Newtownabbey"
    "ards and north down" -> "Ards and North Down"
    "argyll and bute" -> "Argyll and Bute"
    "armagh city" -> "Armagh City, Banbridge and Craigavon"
    "banbridge and craigavon" -> "Armagh City, Banbridge and Craigavon"
    "armagh city banbridge and craigavon" -> "Armagh City, Banbridge and Craigavon"
    "arun" -> "Arun"
    "ashfield" -> "Ashfield"
    "ashford" -> "Ashford"
    "babergh" -> "Babergh"
    "barking and dagenham" -> "Barking and Dagenham"
    "barnet" -> "Barnet"
    "barnsley" -> "Barnsley"
    "barrow in furness" -> "Barrow-in-Furness"
    "basildon" -> "Basildon"
    "basingstoke and deane" -> "Basingstoke and Deane"
    "bassetlaw" -> "Bassetlaw"
    "bath and north east somerset" -> "Bath and North East Somerset"
    "bedford" -> "Bedford"
    "belfast" -> "Belfast"
    "bexley" -> "Bexley"
    "birmingham" -> "Birmingham"
    "blaby" -> "Blaby"
    "blackburn with darwen" -> "Blackburn with Darwen"
    "blackpool" -> "Blackpool"
    "blaenau gwent" -> "Blaenau Gwent"
    "bolsover" -> "Bolsover"
    "bolton" -> "Bolton"
    "boston" -> "Boston"
    "bournemouth" -> "Bournemouth, Christchurch and Poole"
    "christchurch and poole" -> "Bournemouth, Christchurch and Poole"
    "bournemouth christchurch and poole" -> "Bournemouth, Christchurch and Poole"
    "bracknell forest" -> "Bracknell Forest"
    "bradford" -> "Bradford"
    "braintree" -> "Braintree"
    "breckland" -> "Breckland"
    "brent" -> "Brent"
    "brentwood" -> "Brentwood"
    "bridgend" -> "Bridgend"
    "brighton and hove" -> "Brighton and Hove"
    "city of bristol" -> "Bristol, City of"
    "bristol" -> "Bristol, City of"
    "bristol city of" -> "Bristol, City of"
    "broadland" -> "Broadland"
    "bromley" -> "Bromley"
    "bromsgrove" -> "Bromsgrove"
    "broxbourne" -> "Broxbourne"
    "broxtowe" -> "Broxtowe"
    "buckinghamshire" -> "Buckinghamshire"
    "burnley" -> "Burnley"
    "bury" -> "Bury"
    "caerphilly" -> "Caerphilly"
    "calderdale" -> "Calderdale"
    "cambridge" -> "Cambridge"
    "cambridgeshire" -> "Cambridgeshire"
    "cambridgeshire and peterborough" -> "Cambridgeshire and Peterborough"
    "camden" -> "Camden"
    "cannock chase" -> "Cannock Chase"
    "canterbury" -> "Canterbury"
    "cardiff" -> "Cardiff"
    "carlisle" -> "Carlisle"
    "carmarthenshire" -> "Carmarthenshire"
    "castle point" -> "Castle Point"
    "causeway coast and glens" -> "Causeway Coast and Glens"
    "central bedfordshire" -> "Central Bedfordshire"
    "ceredigion" -> "Ceredigion"
    "charnwood" -> "Charnwood"
    "chelmsford" -> "Chelmsford"
    "cheltenham" -> "Cheltenham"
    "cherwell" -> "Cherwell"
    "cheshire east" -> "Cheshire East"
    "cheshire west and chester" -> "Cheshire West and Chester"
    "chesterfield" -> "Chesterfield"
    "chichester" -> "Chichester"
    "chorley" -> "Chorley"
    "city of edinburgh" -> "City of Edinburgh"
    "city of london" -> "City of London"
    "clackmannanshire" -> "Clackmannanshire"
    "colchester" -> "Colchester"
    "conwy" -> "Conwy"
    "copeland" -> "Copeland"
    "cornwall" -> "Cornwall"
    "cotswold" -> "Cotswold"
    "county durham" -> "County Durham"
    "coventry" -> "Coventry"
    "craven" -> "Craven"
    "crawley" -> "Crawley"
    "croydon" -> "Croydon"
    "dumbria" -> "Cumbria"
    "dacorum" -> "Dacorum"
    "darlington" -> "Darlington"
    "dartford" -> "Dartford"
    "denbighshire" -> "Denbighshire"
    "derby" -> "Derby"
    "derbyshire" -> "Derbyshire"
    "derbyshire dales" -> "Derbyshire Dales"
    "derry city and strabane" -> "Derry City and Strabane"
    "devon" -> "Devon"
    "doncaster" -> "Doncaster"
    "dorset" -> "Dorset"
    "dover" -> "Dover"
    "dudley" -> "Dudley"
    "dumfries and galloway" -> "Dumfries and Galloway"
    "dundee city" -> "Dundee City"
    "ealing" -> "Ealing"
    "east ayrshire" -> "East Ayrshire"
    "east cambridgeshire" -> "East Cambridgeshire"
    "east devon" -> "East Devon"
    "east dunbartonshire" -> "East Dunbartonshire"
    "east hampshire" -> "East Hampshire"
    "east hertfordshire" -> "East Hertfordshire"
    "east lindsey" -> "East Lindsey"
    "east lothian" -> "East Lothian"
    "east renfrewshire" -> "East Renfrewshire"
    "east riding of yorkshire" -> "East Riding of Yorkshire"
    "east staffordshire" -> "East Staffordshire"
    "east suffolk" -> "East Suffolk"
    "east sussex" -> "East Sussex"
    "eastbourne" -> "Eastbourne"
    "eastleigh" -> "Eastleigh"
    "eden" -> "Eden"
    "elmbridge" -> "Elmbridge"
    "enfield" -> "Enfield"
    "epping forest" -> "Epping Forest"
    "epsom and ewell" -> "Epsom and Ewell"
    "erewash" -> "Erewash"
    "essex" -> "Essex"
    "exeter" -> "Exeter"
    "falkirk" -> "Falkirk"
    "fareham" -> "Fareham"
    "fenland" -> "Fenland"
    "fermanagh and omagh" -> "Fermanagh and Omagh"
    "fife" -> "Fife"
    "flintshire" -> "Flintshire"
    "folkestone and hythe" -> "Folkestone and Hythe"
    "forest of dean" -> "Forest of Dean"
    "fylde" -> "Fylde"
    "gateshead" -> "Gateshead"
    "gedling" -> "Gedling"
    "glasgow city" -> "Glasgow City"
    "gloucester" -> "Gloucester"
    "gloucestershire" -> "Gloucestershire"
    "gosport" -> "Gosport"
    "gravesham" -> "Gravesham"
    "great yarmouth" -> "Great Yarmouth"
    "greater manchester" -> "Greater Manchester"
    "greenwich" -> "Greenwich"
    "guildford" -> "Guildford"
    "gwynedd" -> "Gwynedd"
    "hackney" -> "Hackney"
    "halton" -> "Halton"
    "hambleton" -> "Hambleton"
    "hammersmith and fulham" -> "Hammersmith and Fulham"
    "hampshire" -> "Hampshire"
    "harborough" -> "Harborough"
    "haringey" -> "Haringey"
    "harlow" -> "Harlow"
    "harrogate" -> "Harrogate"
    "harrow" -> "Harrow"
    "hart" -> "Hart"
    "hartlepool" -> "Hartlepool"
    "hastings" -> "Hastings"
    "havant" -> "Havant"
    "havering" -> "Havering"
    "county of herefordshire" -> "Herefordshire, County of"
    "herefordshire" -> "Herefordshire, County of"
    "herefordshire county of" -> "Herefordshire, County of"
    "hertfordshire" -> "Hertfordshire"
    "hertsmere" -> "Hertsmere"
    "high peak" -> "High Peak"
    "highland" -> "Highland"
    "hillingdon" -> "Hillingdon"
    "hinckley and bosworth" -> "Hinckley and Bosworth"
    "horsham" -> "Horsham"
    "hounslow" -> "Hounslow"
    "huntingdonshire" -> "Huntingdonshire"
    "hyndburn" -> "Hyndburn"
    "inverclyde" -> "Inverclyde"
    "ipswich" -> "Ipswich"
    "isle of anglesey" -> "Isle of Anglesey"
    "isle of wight" -> "Isle of Wight"
    "isles of scilly" -> "Isles of Scilly"
    "islington" -> "Islington"
    "kensington and chelsea" -> "Kensington and Chelsea"
    "kent" -> "Kent"
    "kings lynn and west norfolk" -> "King's Lynn and West Norfolk"
    "city of kingston upon hull" -> "Kingston upon Hull, City of"
    "kingston upon hull" -> "Kingston upon Hull, City of"
    "kingston upon hull city of" -> "Kingston upon Hull, City of"
    "kingston upon thames" -> "Kingston upon Thames"
    "kirklees" -> "Kirklees"
    "knowsley" -> "Knowsley"
    "lambeth" -> "Lambeth"
    "lancashire" -> "Lancashire"
    "lancaster" -> "Lancaster"
    "leeds" -> "Leeds"
    "leicester" -> "Leicester"
    "leicestershire" -> "Leicestershire"
    "lewes" -> "Lewes"
    "lewisham" -> "Lewisham"
    "lichfield" -> "Lichfield"
    "lincoln" -> "Lincoln"
    "lincolnshire" -> "Lincolnshire"
    "lisburn and castlereagh" -> "Lisburn and Castlereagh"
    "liverpool" -> "Liverpool"
    "liverpool city region" -> "Liverpool City Region"
    "luton" -> "Luton"
    "maidstone" -> "Maidstone"
    "maldon" -> "Maldon"
    "malvern hills" -> "Malvern Hills"
    "manchester" -> "Manchester"
    "mansfield" -> "Mansfield"
    "medway" -> "Medway"
    "melton" -> "Melton"
    "mendip" -> "Mendip"
    "merthyr tydfil" -> "Merthyr Tydfil"
    "merton" -> "merton"
    "mid and east antrim" -> "Mid and East Antrim"
    "mid devon" -> "Mid Devon"
    "mid suffolk" -> "Mid Suffolk"
    "mid sussex" -> "Mid Sussex"
    "mid ulster" -> "Mid Ulster"
    "middlesbrough" -> "Middlesbrough"
    "midlothian" -> "Midlothian"
    "milton keynes" -> "Milton Keynes"
    "mole valley" -> "Mole Valley"
    "monmouthshire" -> "Monmouthshire"
    "moray" -> "Moray"
    "na h eileanan siar" -> "Na h-Eileanan Siar"
    "neath port talbot" -> "Neath Port Talbot"
    "new forest" -> "New Forest"
    "newark and sherwood" -> "Newark and Sherwood"
    "newcastle upon tyne" -> "Newcastle upon Tyne"
    "newcastle under lyme" -> "Newcastle-under-Lyme"
    "newham" -> "Newham"
    "newport" -> "Newport"
    "newry" -> "Newry, Mourne and Down"
    "mourne and down" -> "Newry, Mourne and Down"
    "newry mourne and down" -> "Newry, Mourne and Down"
    "norfolk" -> "Norfolk"
    "north ayrshire" -> "North Ayrshire"
    "north devon" -> "North Devon"
    "north east" -> "North East"
    "north east derbyshire" -> "North East Derbyshire"
    "north east lincolnshire" -> "North East Lincolnshire"
    "north hertfordshire" -> "North Hertfordshire"
    "north kesteven" -> "North Kesteven"
    "north lanarkshire" -> "North Lanarkshire"
    "north lincolnshire" -> "North Lincolnshire"
    "north norfolk" -> "North Norfolk"
    "north northamptonshire" -> "North Northamptonshire"
    "north of tyne" -> "North of Tyne"
    "north somerset" -> "North Somerset"
    "north tyneside" -> "North Tyneside"
    "north warwickshire" -> "North Warwickshire"
    "north west leicestershire" -> "North West Leicestershire"
    "north yorkshire" -> "North Yorkshire"
    "northumberland" -> "Northumberland"
    "norwich" -> "Norwich"
    "nottingham" -> "Nottingham"
    "nottinghamshire" -> "Nottinghamshire"
    "nuneaton and bedworth" -> "Nuneaton and Bedworth"
    "oadby and wigston" -> "Oadby and Wigston"
    "oldham" -> "Oldham"
    "orkney islands" -> "Orkney Islands"
    "oxford" -> "Oxford"
    "oxfordshire" -> "Oxfordshire"
    "pembrokeshire" -> "Pembrokeshire"
    "pendle" -> "Pendle"
    "perth and kinross" -> "Perth and Kinross"
    "peterborough" -> "Peterborough"
    "plymouth" -> "Plymouth"
    "portsmouth" -> "Portsmouth"
    "powys" -> "Powys"
    "preston" -> "Preston"
    "reading" -> "Reading"
    "redbridge" -> "Redbridge"
    "redcar and cleveland" -> "Redcar and Cleveland"
    "redditch" -> "Redditch"
    "reigate and banstead" -> "Reigate and Banstead"
    "renfrewshire" -> "Renfrewshire"
    "rhondda cynon taf" -> "Rhondda Cynon Taf"
    "ribble valley" -> "Ribble Valley"
    "richmond upon thames" -> "Richmond upon Thames"
    "richmondshire" -> "Richmondshire"
    "rochdale" -> "Rochdale"
    "rochford" -> "Rochford"
    "rossendale" -> "Rossendale"
    "rother" -> "Rother"
    "rotherham" -> "Rotherham"
    "rugby" -> "Rugby"
    "runnymede" -> "Runnymede"
    "rushcliffe" -> "Rushcliffe"
    "rushmoor" -> "Rushmoor"
    "rutland" -> "Rutland"
    "ryedale" -> "Ryedale"
    "salford" -> "Salford"
    "sandwell" -> "Sandwell"
    "scarborough" -> "Scarborough"
    "scottish borders" -> "Scottish Borders"
    "sedgemoor" -> "Sedgemoor"
    "sefton" -> "Sefton"
    "selby" -> "Selby"
    "sevenoaks" -> "Sevenoaks"
    "sheffield" -> "Sheffield"
    "shetland islands" -> "Shetland Islands"
    "shropshire" -> "Shropshire"
    "slough" -> "Slough"
    "solihull" -> "Solihull"
    "somerset" -> "Somerset"
    "somerset west and taunton" -> "Somerset West and Taunton"
    "south ayrshire" -> "South Ayrshire"
    "south cambridgeshire" -> "South Cambridgeshire"
    "south derbyshire" -> "South Derbyshire"
    "south gloucestershire" -> "South Gloucestershire"
    "south hams" -> "South Hams"
    "south holland" -> "South Holland"
    "south kesteven" -> "South Kesteven"
    "south lakeland" -> "South Lakeland"
    "south lanarkshire" -> "South Lanarkshire"
    "south norfolk" -> "South Norfolk"
    "south oxfordshire" -> "South Oxfordshire"
    "south ribble" -> "South Ribble"
    "south somerset" -> "South Somerset"
    "south staffordshire" -> "South Staffordshire"
    "south tyneside" -> "South Tyneside"
    "south yorkshire" -> "South Yorkshire"
    "southampton" -> "Southampton"
    "southend on sea" -> "Southend-on-Sea"
    "southwark" -> "Southwark"
    "spelthorne" -> "Spelthorne"
    "st albans" -> "St Albans"
    "st helens" -> "St. Helens"
    "stafford" -> "Stafford"
    "staffordshire" -> "Staffordshire"
    "staffordshire moorlands" -> "Staffordshire Moorlands"
    "stevenage" -> "Stevenage"
    "stirling" -> "Stirling"
    "stockport" -> "Stockport"
    "stockton on tees" -> "Stockton-on-Tees"
    "stoke on trent" -> "Stoke-on-Trent"
    "stratford on avon" -> "Stratford-on-Avon"
    "stroud" -> "Stroud"
    "suffolk" -> "Suffolk"
    "sunderland" -> "Sunderland"
    "surrey" -> "Surrey"
    "surrey heath" -> "Surrey Heath"
    "sutton" -> "Sutton"
    "swale" -> "Swale"
    "swansea" -> "Swansea"
    "swindon" -> "Swindon"
    "tameside" -> "Tameside"
    "tamworth" -> "Tamworth"
    "tandridge" -> "Tandridge"
    "tees valley" -> "Tees Valley"
    "teignbridge" -> "Teignbridge"
    "telford and wrekin" -> "Telford and Wrekin"
    "tendring" -> "Tendring"
    "test valley" -> "Test Valley"
    "tewkesbury" -> "Tewkesbury"
    "thanet" -> "Thanet"
    "three rivers" -> "Three Rivers"
    "thurrock" -> "Thurrock"
    "tonbridge and malling" -> "Tonbridge and Malling"
    "torbay" -> "Torbay"
    "torfaen" -> "Torfaen"
    "torridge" -> "Torridge"
    "tower hamlets" -> "Tower Hamlets"
    "trafford" -> "Trafford"
    "tunbridge bells" -> "Tunbridge Wells"
    "uttlesford" -> "Uttlesford"
    "vale of glamorgan" -> "Vale of Glamorgan"
    "vale of white horse" -> "Vale of White Horse"
    "wakefield" -> "Wakefield"
    "walsall" -> "Walsall"
    "waltham forest" -> "Waltham Forest"
    "wandsworth" -> "Wandsworth"
    "warrington" -> "Warrington"
    "warwick" -> "Warwick"
    "warwickshire" -> "Warwickshire"
    "watford" -> "Watford"
    "waverley" -> "Waverley"
    "wealden" -> "Wealden"
    "welwyn hatfield" -> "Welwyn Hatfield"
    "west berkshire" -> "West Berkshire"
    "west devon" -> "West Devon"
    "west dunbartonshire" -> "West Dunbartonshire"
    "west lancashire" -> "West Lancashire"
    "west lindsey" -> "West Lindsey"
    "west lothian" -> "West Lothian"
    "west midlands" -> "West Midlands"
    "west northamptonshire" -> "West Northamptonshire"
    "west of england" -> "West of England"
    "west oxfordshire" -> "West Oxfordshire"
    "west suffolk" -> "West Suffolk"
    "west sussex" -> "West Sussex"
    "west yorkshire" -> "West Yorkshire"
    "westminster" -> "Westminster"
    "wigan" -> "Wigan"
    "wiltshire" -> "Wiltshire"
    "winchester" -> "Winchester"
    "windsor and maidenhead" -> "Windsor and Maidenhead"
    "wirral" -> "Wirral"
    "woking" -> "Woking"
    "wokingham" -> "Wokingham"
    "wolverhampton" -> "Wolverhampton"
    "worcester" -> "Worcester"
    "worcestershire" -> "Worcestershire"
    "worthing" -> "Worthing"
    "wrexham" -> "Wrexham"
    "wychavon" -> "Wychavon"
    "wyre" -> "Wyre"
    "wyre forest" -> "Wyre Forest"
    "york" -> "York"
    else -> localAuthorityName
  }
}

fun String.canonicalise(): String {
  return this.trim()
    .lowercase()
    .replace("&", "and")
    .replace("-", " ")
    .replace(Regex("[^a-z\\s]"), "")
    .replace(Regex("\\s+"), " ")
}
