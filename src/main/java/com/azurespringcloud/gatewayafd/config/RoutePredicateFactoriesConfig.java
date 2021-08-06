package com.azurespringcloud.gatewayafd.config;

import com.azurespringcloud.gatewayafd.predicates.AzureServiceTagRemoteAddrRoutePredicateFactory;
import com.azurespringcloud.gatewayafd.predicates.LoggingRoutePredicateFactory;
import com.azurespringcloud.gatewayafd.predicates.XForwardedRemoteAddrRoutePredicateFactory;
import com.azurespringcloud.gatewayafd.service.AzureServicesAddressResolver;
import com.azurespringcloud.gatewayafd.service.impl.AzureServicesAddressResolverImpl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoutePredicateFactoriesConfig {
    @Value("${azurespringcloud.service-address-discovery.max-age-minutes:720}")
    private Integer maxAgeInMinutes;
    
    @Bean
    public AzureServicesAddressResolver azureAddressResolver(){
        return new AzureServicesAddressResolverImpl(maxAgeInMinutes);
    }

    @Bean
    public XForwardedRemoteAddrRoutePredicateFactory xForwardedRemoteAddr() {
        return new XForwardedRemoteAddrRoutePredicateFactory();
    }

    @Bean AzureServiceTagRemoteAddrRoutePredicateFactory azureServiceTagRemoteAddr(){
        return new AzureServiceTagRemoteAddrRoutePredicateFactory();
    }

    @Bean
    public LoggingRoutePredicateFactory logging() {
        return new LoggingRoutePredicateFactory();
    }
}
