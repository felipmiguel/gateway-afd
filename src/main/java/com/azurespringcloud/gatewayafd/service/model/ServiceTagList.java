package com.azurespringcloud.gatewayafd.service.model;

import java.util.List;

import lombok.Data;

@Data
public class ServiceTagList {
    Integer changeNumber;
    String cloud;
    List<ServiceTag> values;
    
}
