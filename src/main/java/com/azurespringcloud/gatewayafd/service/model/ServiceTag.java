package com.azurespringcloud.gatewayafd.service.model;

import lombok.Data;

@Data
public class ServiceTag {
    String name;
    String id;
    ServiceTagProperties properties;   
}
