# Fn Project JRestless support 

Author: [Rae Jeffries-Harris](https://github.com/RaeJ) 

This module allows you to deploy JAX-RS application onto Fn as serverless application. 

## Description
This demonstrates an extension to the [JRestless](https://github.com/bbilger/jrestless) framework that allows you to run a JAX-RS application using the Fn  platform based on the [Fn Java FDK](https://github.com/fnproject/fn-java-fdk)

## Fn Usage Example

Make sure you have all the required pre-requisites for using [Fn Java](https://github.com/fnproject/fn-java-fdk) as described in the docs.

Create a new function using the Fn  CLI as you would if simply creating a Java Function as a Service:

```bash
$ mkdir fnjaxrs
$ cd fnjaxrs
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

Add the `jrestless-handler`  to  the pom.xml file:

```xml
<dependency>
    <groupId>com.fnproject.fn</groupId>
    <artifactId>jrestless-handler</artifactId>
    <version>1.0.0</version>
</dependency>
```

And any other dependencies you need in your app  e.g.:

```xml
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

public class RequestHandler extends com.fnproject.fn.jrestless.FnRequestHandler {
  public RequestHandler() {
    // initialize the container with your resource configuration
    ResourceConfig config = new ResourceConfig()
      .register(com.fnproject.fn.jrestless.FnFeature.class)
      .packages("com.example.restapp");
    init(config);
    // start the container
    start();
  }
}
```



From here make sure you've got the Fn Server up and running ([Quickstart](https://github.com/fnproject/fn#quickstart)).

```bash
$ fn deploy --app jrestless-example --local
```

From here the function can now be called:
```bash
$ curl -H 'Accept: application/json' -X GET 'http://localhost:8080/r/jrestless-example/sample/health'
# {"statusMessage":"up and running"}
```
