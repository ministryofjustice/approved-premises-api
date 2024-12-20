# 25. Enum naming convention

Date: 2024-12-11

## Status

Accepted

## Context

Currently enums are generated with camelCase names. This is because of the configuration that has been set for a while (   ` put("enumPropertyNaming", "camelCase")`). This is not kotlin linting standards, and has been undetected as the linting tools do not scan the generated code. 
We are moving away from a documentation first approach, so this issue has been highlighted. We cannot switch the config as the camelCase enum names are rooted in the code and database, so that will require further refactoring later. 
This needed a decision because we will eventually create enums by code first, which will be forced to use the linting standards.

There are already discrepancies in the enum naming conventions in the code base - the internal API ones are correct, but different.

Further, a side issue, as the name, jsonproperty, and value were often identical, there are instances of using `enum.toString()` or `enum.name()` where people should have been using `enum.value()`, which will also need to be refactored.

## Options
1) We can however set the x-enum-varnames in the openapi spec to generate UPPERCASE names, which generates with uppercase names and lowercase/underscore values/jsonproperties. This is slightly more work now, but means no refactoring later, which generates as `    @JsonProperty("camelCase") CAMEL_CASE("camelCase")`
2) set both the enum and x-enum-varnames to UPPERCASE, which will generate enums with both names and values/jsonproperties in uppercase. This would mean we can use plain enum names from the UI, and use valueOf() rather than serialising/deserialising, so the extra bloat (jsonproperty/value) that openapi inserts could be removed. which generates as `@JsonProperty("CAMEL_CASE") CAMEL_CASE("CAMEL_CASE")`
3) leave as is, permanently exclude the enums from linting standards, and when we create enums that will be used in the UI, continue the camelCase pattern and also add the jsonproperty and value. which generates as `@JsonProperty("camelCase") camelCase("camelCase")`
4) we cannot set just the enum name to UPPERCASE, because that generates with a lowercase leading letter, which generates as `@JsonProperty("EXAMPLE") eXAMPLE("EXAMPLE")`

## Decision

* use the x-enum-varnames to generate uppercase enums for new enums going into the openApi spec files. 
* This will mean no refactoring later on when we bring generated code into the code base, and also no changes in the UI. 
* Ideally we want to get to a 'plain' enum, which would be UPPERCASE values.
* it will force the user of `enum.value()` rather than `enum.name()` or `enum.toString()`

## Caveats

There is a significant amount of existing code that should be refactored, however this will be an ongoing effort and not a single PR task. 

## Consequences

There will be a difference in the naming conventions, but that is already a prevalent inconsistency between the generated enums and the internal API ones..