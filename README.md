# Spring Cloud Gateway predicates
This library registers the following Route Predicate Factories:
* XForwardedRemoteAddrRoutePredicateFactory
* AzureServiceTagRemoteAddrRoutePredicateFactory

These predicates can be used in Spring Cloud Gateway to filter requests based on the  X-FORWARDED-FOR header for use with reverse proxies.

Additionally LoggingRoutePredicateFactory can be used in development to log requests for debugging purposes. 

## XForwardedRemoteAddrRoutePredicateFactory
This factory can be used to allow only a static list of addresses.

Sample application.yaml file:
``` yaml
spring:
  main:
    allow-bean-definition-overriding: true
  cloud:
    gateway:
      discovery:
        locator:
          ...
          predicates:
          ...
          
          # Filter based on all the IP addresses that the reverse proxy can use.
          # For Application Gateway, the outbound IP address is ALWAYS the public IP address of Application Gateway itself (i.e. the same
          # IP address that is used for inbound traffic).
          
          # Do NOT use the RemoteAddr predicate as this uses the original "source" IP address of the client.
          # See https://cloud.spring.io/spring-cloud-gateway/reference/html/#the-remoteaddr-route-predicate-factory.
          #- RemoteAddr="20.103.252.85", "13.73.248.16/29", "20.21.37.40/29", "20.36.120.104/29"
          
          # Use a custom predicate which uses the X-FORWARDED-FOR header and looks at the last (i.e. right-most) value in that header.
          # When using Spring Cloud Gateway, this header will ALWAYS contain the last reverse proxy that was used before accessing the gateway,
          # or the original client IP address if no reverse proxy was used. That means that if a client were to spoof the X-FORWARDED-FOR
          # header with a value from the below allow-list, another value would get appended by the gateway with their own client IP and they
          # would NOT be able to bypass the gateway predicate.
          - XForwardedRemoteAddr="20.103.252.85", "13.73.248.16/29", "20.21.37.40/29", "20.36.120.104/29"
```
## AzureServiceTagRemoteAddrRoutePredicateFactory
This factory can be used to filter the traffic allowing only certain addresses coming from Azure Services. This Factory dynamically discovers the list of addresses for Azure Services.
It can be configured to use different Clouds (Azure, China, AzureGov, GermanyGov) and different Azure Services.
The address list is retrieved from Microsoft public sites and kept for a configurable amount of time, by default 720 minutes (12 hours).

Sample application.yaml:
```yaml
azurespringcloud:
  service-address-discovery:
    max-age-minutes: 60 # by default 720 minutes
...
# Configure Spring
spring:
  main:
    allow-bean-definition-overriding: true
  cloud:
    gateway:
      discovery:
        locator:
          predicates:
          
          # Filter based on all the IP addresses that the reverse proxy can use.
          # For Front Door, these are published in the list of Azure IP ranges and service tags as "AzureFrontDoor.Backend".
          # See https://docs.microsoft.com/en-us/azure/frontdoor/front-door-faq#how-do-i-lock-down-the-access-to-my-backend-to-only-azure-front-door-
          
          # Do NOT use the RemoteAddr predicate as this uses the original "source" IP address of the client.
          # See https://cloud.spring.io/spring-cloud-gateway/reference/html/#the-remoteaddr-route-predicate-factory.
          
          # Use a custom predicate which uses the X-FORWARDED-FOR header and looks at the last (i.e. right-most) value in that header.
          # When using Spring Cloud Gateway, this header will ALWAYS contain the last reverse proxy that was used before accessing the gateway,
          # or the original client IP address if no reverse proxy was used. That means that if a client were to spoof the X-FORWARDED-FOR
          # header with a value from the below allow-list, another value would get appended by the gateway with their own client IP and they
          # would NOT be able to bypass the gateway predicate.
          - AzureServiceTagRemoteAddr="Azure", "AzureFrontDoor.Backend", "1"
```

## LoggingRoutePredicateFactory
This factory can be used to trace all requests in Spring Gateway. This should be used only for debugging purposes.

Sample application.yal
```yaml
# Configure Spring
spring:
  cloud:
    gateway:
      discovery:
        locator:
          predicates:
          # Optionally add a custom logging predicate to see request details as they come in.
          - Logging=true
```

## How to use this library
You can install the library in your local repo:
```bash
mvn install
```
Add the dependency in your pom.xml file
```xml
<dependency>
	<groupId>com.azurespringcloud</groupId>
	<artifactId>gateway-afd</artifactId>
	<version>0.0.1-SNAPSHOT</version>
</dependency>
```
And configure the predicates as described above.