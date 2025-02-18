# How to add a new endpoint

The Open API Generator Gradle plugin generates boilerplate (request/response models & Spring controllers) for endpoints 
from the `src/main/resources/static/api.yml` file.  It also generates an interface for each top-level 
path which is what we need to implement.

![](./images/openapi.drawio.png)


To create a new endpoint on a top-level path:
 - Edit `src/main/resources/static/api.yml` to add your endpoint, e.g.
    
   ![](./images/openapi-new-endpoint.png)
 - Run the `openapitools`->`openApiGenerate` Gradle Task
 - Look at `build/generated/src/main/kotlin/uk/gov/justice/digital/hmpps/approvedpremisesapi`
    
   ![](./images/openapi-build-output.png)
 - Find the Delegate interface that corresponds to the top-level path for the endpoint you added, 
   e.g. `PremisesApiDelegate`
 - In `src/main/kotlin/controller` create a new equivalently named controller class, e.g. `PremisesController` which 
   implements this interface
    
   ![](./images/implement-delegate-interface.png)
 - The interface has a default implementation which simply returns a 501 "Not Implemented" response when called
 - To actually implement the endpoint, press Alt + Insert (on IntelliJ) in the body of the class, select 
   `Override Methods`
    
   ![](./images/alt-insert-menu.png)
 - From the dialog that appears, select one or more of the endpoints you want to implement - note that the top method 
  `getRequest()` is a fallback handler, you shouldn't ever need to worry about this method.
    
   ![](./images/override-methods-dialog.png)
 - This will insert an override that simply calls the default method on the interface, replace the body of the method 
   with your implement, e.g.
    
   ![](./images/implement-delegate-method.png)

 - You will then need to add a security configuration entry for your endpoint

   In `src/main/kotlin/uk/gov/justice/digital/hmpps/approvedpremisesapi/config/OAuth2ResourceServerSecurityConfiguration.kt::securityFilterChain` add a new entry:
   
   ```
   authorize(HttpMethod.GET, "/premises", permitAll) //Allows any client to access the endpoint (even without a JWT)
   authorize(HttpMethod.GET, "/premises", hasAuthority("ROLE_interventions")) //Allows only clients presenting a valid HMPPS JWT with the ROLE_interventions authority to access the endpoint
   authorize(HttpMethod.PUT, "/cas2/assessments/**", hasRole("CAS2_ASSESSOR")) // Allows only clients with the specified role
   authorize(HttpMethod.GET, "/cas2/assessments/**", hasAnyRole("CAS2_ASSESSOR", "CAS2_ADMIN")) // Allows only clients with at least of the specified roles
   ```

   If you need to access information about the requester from within the endpoint code, you can do so via the following:

   ```
   val principal = SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken
   ```
