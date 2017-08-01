# JRestless-jfaas
JRestless-jfaas allows you to create Oracle Functions applications using JAX-RS.

## Description
This demonstrates an extension to the [JRestless](https://github.com/bbilger/jrestless) framework that allows you to run a JAX-RS application using the Oracle Functions platform.

## Oracle Functions Usage Example

Make sure you have all the required pre-requisites for using [JFaaS](https://gitlab-odx.oracle.com/odx/jfaas/blob/master/README.md) as described in the docs.

Create a new function using the Oracle Functions CLI as you would if simply creating a Java Function as a Service:

```bash
$ mkdir oracle-functions-usage-example
$ cd oracle-functions-usage-example
$ fn init --runtime=java jbloggs/jrestless
Runtime: java
function boilerplate generated.
func.yaml created
```
Ignore the HelloFunction class for now - we will replace this later on.

Replace the func.yaml file with the following contents:
```yaml
name: jbloggs/jrestless
version: 0.0.1
runtime: java
cmd: com.example.faas.RequestHandler::handleRequest
path: /sample/health
format: http
```

Add these maven dependencies in the pom.xml file:

```xml
<dependency>
    <groupId>com.jrestless.core</groupId>
    <artifactId>jrestless-core-container</artifactId>
    <version>0.5.1</version>
</dependency>

<dependency>
    <groupId>org.glassfish.jersey.media</groupId>
    <artifactId>jersey-media-json-jackson</artifactId>
    <version>2.25.1</version>
</dependency>
```

Also add the following repository in the pom.xml:
```xml
<repository>
   <snapshots>
       <enabled>false</enabled>
   </snapshots>
   <id>central</id>
   <name>bintray</name>
   <url>http://jcenter.bintray.com</url>
</repository>
```

Remove the Java class HelloFunction and in its place create a new JAX-RS resource and response object. Be sure to also delete the class HelloFunctionTest found in the test folder under the package com.example.faas.
(`src/main/java/com/example/faas/SampleResource.java`):

```java
package com.example.faas;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/sample")
public class SampleResource {
  @GET
  @Path("/health")
  @Produces({ MediaType.APPLICATION_JSON })
  public Response getHealthStatus() {
    return Response.ok(new HealthStatusResponse("up and running")).build();
  }
  static class HealthStatusResponse {
    private final String statusMessage;
    HealthStatusResponse(String statusMessage) {
      this.statusMessage = statusMessage;
    }
    public String getStatusMessage() {
      return statusMessage;
    }
  }
}
```

Create the request handler (`src/main/java/com/example/faas/RequestHandler.java`):
```java
package com.example.faas;

import org.glassfish.jersey.server.ResourceConfig;

public class RequestHandler extends OracleFunctionsRequestObjectHandler {
  public RequestHandler() {
    // initialize the container with your resource configuration
    ResourceConfig config = new ResourceConfig()
      .register(OracleFeature.class)
      .packages("com.example.faas");
    init(config);
    // start the container
    start();
  }
}
```

At the moment as Oracle Functions is not currently integrated into jrestless for this to work the package com.example.faas also needs the classes; [OracleFunctionsRequestObjectHandler](https://gitlab-odx.oracle.com/odx/jrestless-jfaas/blob/master/src/main/java/com/oracle/faas/jrestlessexample/OracleFunctionsRequestObjectHandler.java), [OracleFunctionsRequestHandler](https://gitlab-odx.oracle.com/odx/jrestless-jfaas/blob/master/src/main/java/com/oracle/faas/jrestlessexample/OracleFunctionsRequestHandler.java) and [OracleFeature](https://gitlab-odx.oracle.com/odx/jrestless-jfaas/blob/master/src/main/java/com/oracle/faas/jrestlessexample/OracleFeature.java).


From here make sure you've got the Oracle Functions Server up and running ([Quickstart](https://github.com/fnproject/fn#quickstart)).

```bash
$ fn build
$ fn apps create jrestless-example
$ fn routes create jrestless-example /sample/health
```

From here the function can now be called:
```bash
$ curl -H 'Accept: application/json' -X GET 'http://localhost:8080/r/jrestless-example/sample/health'
# {"statusMessage":"up and running"}
```
