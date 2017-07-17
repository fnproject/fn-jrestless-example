FROM maven:3.5-jdk-8-alpine as build-stage
WORKDIR /function
ENV MAVEN_OPTS -Dhttp.proxyHost=emea-proxy.uk.oracle.com -Dhttp.proxyPort=80 -Dhttps.proxyHost=emea-proxy.uk.oracle.com -Dhttps.proxyPort=80 -Dhttp.nonProxyHosts=localhost|.oraclecorp.com|.local|10.167.103.241|10.167.103.241 -Dmaven.repo.local=/usr/share/maven/ref/repository
ADD pom.xml /function/pom.xml
RUN ["mvn", "package", "dependency:copy-dependencies", "-DincludeScope=runtime", "-DskipTests=true", "-Dmdep.prependGroupId=true"]
ADD src /function/src
RUN ["mvn", "package"]
FROM registry.oracledx.com/skeppare/jfaas-runtime:latest
WORKDIR /function
COPY --from=build-stage /function/target/*.jar /function/app/
COPY --from=build-stage /function/target/dependency/*.jar /function/lib/
CMD ["com.oracle.faas.jrestlessexample.ExampleClass::handleRequest"]
