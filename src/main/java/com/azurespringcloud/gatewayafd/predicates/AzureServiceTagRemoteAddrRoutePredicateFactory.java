package com.azurespringcloud.gatewayafd.predicates;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;


import com.azurespringcloud.gatewayafd.service.AzureServicesAddressResolver;
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

public class AzureServiceTagRemoteAddrRoutePredicateFactory
        extends AbstractRoutePredicateFactory<AzureServiceTagRemoteAddrRoutePredicateFactory.Config> {

    private static final Log log = LogFactory.getLog(XForwardedRemoteAddrRoutePredicateFactory.class);

    @Autowired
    AzureServicesAddressResolver addressResolver;

    
    public AzureServiceTagRemoteAddrRoutePredicateFactory() {
        super(Config.class);
    }

    @Override
    public Predicate<ServerWebExchange> apply(Config config) {

        List<String> serviceSources;
        try {
            serviceSources = addressResolver.getServiceAddresses(config.getCloudType(), config.getServiceType());
        } catch (IOException ioex) {
            throw new FatalBeanException("Not possible to get sources", ioex);
        }
        if (serviceSources == null) {
            throw new FatalBeanException(
                    "No source addresses for service " + config.getCloudType() + "-" + config.getServiceType());
        }

        if (log.isDebugEnabled()) {
            log.debug("Applying XForwardedRemoteAddr route predicate with maxTrustedIndex of "
                    + config.getMaxTrustedIndex() + " for " + serviceSources.size() + " source(s)");
        }

        // Reuse the standard RemoteAddrRoutePredicateFactory but instead of using the
        // default
        // RemoteAddressResolver to determine the client IP address, use an
        // XForwardedRemoteAddressResolver.
        RemoteAddrRoutePredicateFactory.Config wrappedConfig = new RemoteAddrRoutePredicateFactory.Config();

        
        
        wrappedConfig.setSources(serviceSources);
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

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("serviceType", "cloudType", "maxTrustedIndex");
    }

    public static class Config {
        Log logger = LogFactory.getLog(Config.class);

        private int maxTrustedIndex = 1;

        public int getMaxTrustedIndex() {
            return this.maxTrustedIndex;
        }

        public Config setMaxTrustedIndex(int maxTrustedIndex) {
            this.maxTrustedIndex = maxTrustedIndex;
            return this;
        }

        private CloudType cloudType;

        public CloudType getCloudType() {
            return this.cloudType;
        }

        public Config setCloudType(CloudType cloudType) {
            this.cloudType = cloudType;
            return this;
        }

        private String serviceType;

        public String getServiceType() {
            return this.serviceType;
        }

        public Config setServiceType(String serviceType) {
            this.serviceType = serviceType;
            return this;
        }
    }

}
