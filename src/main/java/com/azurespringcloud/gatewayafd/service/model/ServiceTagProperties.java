package com.azurespringcloud.gatewayafd.service.model;

import java.util.List;

import lombok.Data;

@Data
public class ServiceTagProperties {
    Integer changeNumber;
    String region;
    Integer regionId;
    String platform;
    String systemService;
    List<String> addressPrefixes;
    List<String> networkFeatures;
}
