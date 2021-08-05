package com.azurespringcloud.gatewayafd.predicates;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

// See https://www.baeldung.com/spring-cloud-gateway-routing-predicate-factories

public class LoggingRoutePredicateFactory extends AbstractRoutePredicateFactory<LoggingRoutePredicateFactory.Config> {

    private static final Log log = LogFactory.getLog(LoggingRoutePredicateFactory.class);

    public LoggingRoutePredicateFactory() {
        super(Config.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("isEnabled");
    }

    @Override
    public Predicate<ServerWebExchange> apply(Config config) {
        Boolean isEnabled = config.getIsEnabled();

        return exchange -> {
            if (isEnabled) {
                ServerHttpRequest request = exchange.getRequest();
                InetSocketAddress remoteAddress = request.getRemoteAddress();

                log.info("Request:");
                log.info("- URL: " + request.getURI());
                log.info("- HTTP Method: " + request.getMethodValue());
                log.info("- Is HTTPS: " + (request.getSslInfo() != null));
                log.info("- Remote Address: " + remoteAddress.getAddress().getHostAddress());
                log.info("- Remote Host: " + remoteAddress.getHostName());
                log.info("- Remote Port: " + remoteAddress.getPort());

                log.info("Request Headers:");
                for (Map.Entry<String, List<String>> entry : request.getHeaders().entrySet()) {
                    log.info("- " + entry.getKey() + ": " + entry.getValue());
                }
            }

            return true;
        };
    }

    public static class Config {

        private Boolean isEnabled = true;

        public Boolean getIsEnabled() {
            return this.isEnabled;
        }

        public Config setIsEnabled(boolean isEnabled) {
            this.isEnabled = isEnabled;
            return this;
        }
    }
}