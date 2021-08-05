package com.azurespringcloud.gatewayafd.config;

import com.azurespringcloud.gatewayafd.predicates.LoggingRoutePredicateFactory;
import com.azurespringcloud.gatewayafd.predicates.XForwardedRemoteAddrRoutePredicateFactory;
import com.azurespringcloud.gatewayafd.service.AfdAddressResolver;
import com.azurespringcloud.gatewayafd.service.impl.AfdAddressResolverImpl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdfPredicatesConfig {
    @Bean
    public XForwardedRemoteAddrRoutePredicateFactory xForwardedRemoteAddr() {
        return new XForwardedRemoteAddrRoutePredicateFactory();
    }

    @Bean
    public LoggingRoutePredicateFactory logging() {
        return new LoggingRoutePredicateFactory();
    }

    @Bean
    public AfdAddressResolver afdResolver(){
        return new AfdAddressResolverImpl();
    }
}
