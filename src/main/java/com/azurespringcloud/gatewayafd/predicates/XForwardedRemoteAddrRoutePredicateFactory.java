package com.azurespringcloud.gatewayafd.predicates;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import com.azurespringcloud.gatewayafd.service.AfdAddressResolver;
import com.azurespringcloud.gatewayafd.service.model.CloudType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.RemoteAddrRoutePredicateFactory;
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

/*

This route predicate allows requests to be filtered based on the X-FORWARDED-FOR header for use with reverse proxies.

See https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#modifying-the-way-remote-addresses-are-resolved.

In theory, all the building blocks already exist but they cannot be enabled purely from configuration
as discussed at https://github.com/spring-cloud/spring-cloud-gateway/issues/783.

With this implementation, a separate "XForwardedRemoteAddr" route predicate is offered which
can be configured with a list of allowed IP addresses and which by default has maxTrustedIndex set to 1.
This value means we trust the last (right-most) value in the X-FORWARDED-FOR header, which represents
the last reverse proxy that was used when calling the gateway. That IP address is then checked against
the list of allowed IP addresses and used to determine whether or not the request is allowed.

Example usage in application.yml which trusts two reverse proxies (one using an IPv6 range):

  ...
  - predicates:
    - XForwardedRemoteAddr="20.103.252.85", "2a01:111:2050::/44"

NOTE: Ideally we would also be able to set the maxTrustedIndex in configuration but that's challenging with
shortcut notation (you can have multiple named arguments, but not sure how to format them when one is a list).

Unfortunately, long form doesn't work for some reason either (this results in parser errors while reading configuration):

  - predicates:
    - name: XForwardedRemoteAddr
      args:
        maxTrustedIndex: 12
        sources: "20.103.252.85", "2a01:111:2050::/44"

Similar when quoting the entire list:
        sources: "20.103.252.85, 2a01:111:2050::/44"

Similar when removing all quotes:
        sources: 20.103.252.85, 2a01:111:2050::/44

Similar when using YAML list syntax:
        sources:
        - 20.103.252.85
        - 13.73.248.16/29

*/

public class XForwardedRemoteAddrRoutePredicateFactory
        extends AbstractRoutePredicateFactory<XForwardedRemoteAddrRoutePredicateFactory.Config> {

    private static final Log log = LogFactory.getLog(XForwardedRemoteAddrRoutePredicateFactory.class);

    @Autowired
    AfdAddressResolver addressResolver;

    public XForwardedRemoteAddrRoutePredicateFactory() {
        super(Config.class);

    }

    @Override
    public ShortcutType shortcutType() {
        return ShortcutType.GATHER_LIST;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("sources");
    }

    @Override
    public Predicate<ServerWebExchange> apply(Config config) {
        if (log.isDebugEnabled()) {
            log.debug("Applying XForwardedRemoteAddr route predicate with maxTrustedIndex of "
                    + config.getMaxTrustedIndex() + " for " + config.getSources().size() + " source(s)");
        }

        // Reuse the standard RemoteAddrRoutePredicateFactory but instead of using the
        // default
        // RemoteAddressResolver to determine the client IP address, use an
        // XForwardedRemoteAddressResolver.
        RemoteAddrRoutePredicateFactory.Config wrappedConfig = new RemoteAddrRoutePredicateFactory.Config();
        List<String> sources;
        try {
            sources = addressResolver.getAfdAddresses(CloudType.Azure);
        } catch (IOException e) {
            throw new FatalBeanException("Not possible to get sources", e);
        }
        if (sources == null) {
            throw new FatalBeanException("No sources so nothing to configure");
        }
        wrappedConfig.setSources(sources);
        wrappedConfig
                .setRemoteAddressResolver(XForwardedRemoteAddressResolver.maxTrustedIndex(config.getMaxTrustedIndex()));
        RemoteAddrRoutePredicateFactory remoteAddrRoutePredicateFactory = new RemoteAddrRoutePredicateFactory();
        Predicate<ServerWebExchange> wrappedPredicate = remoteAddrRoutePredicateFactory.apply(wrappedConfig);

        return exchange -> {
            Boolean isAllowed = wrappedPredicate.test(exchange);

            if (log.isDebugEnabled()) {
                ServerHttpRequest request = exchange.getRequest();
                log.debug("Request for \"" + request.getURI() + "\" from client \""
                        + request.getRemoteAddress().getAddress().getHostAddress() + "\" with \""
                        + XForwardedRemoteAddressResolver.X_FORWARDED_FOR + "\" header value of \""
                        + request.getHeaders().get(XForwardedRemoteAddressResolver.X_FORWARDED_FOR) + "\" is "
                        + (isAllowed ? "ALLOWED" : "NOT ALLOWED"));
            }

            return isAllowed;
        };
    }

    public static class Config {

        // Trust the last (right-most) value in the X-FORWARDED-FOR header by default,
        // which
        // represents the last reverse proxy that was used when calling the gateway.
        private int maxTrustedIndex = 1;

        Log logger = LogFactory.getLog(Config.class);

        private List<String> sources = null;

        public int getMaxTrustedIndex() {
            return this.maxTrustedIndex;
        }

        public Config setMaxTrustedIndex(int maxTrustedIndex) {
            this.maxTrustedIndex = maxTrustedIndex;
            return this;
        }

        public List<String> getSources() {
            return this.sources;
        }

        public Config setSources(List<String> sources) {
            this.sources = sources;
            return this;
        }

        public Config setSources(String... sources) {
            this.sources = Arrays.asList(sources);
            return this;
        }
    }
}