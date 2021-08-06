package com.azurespringcloud.gatewayafd.service;

import java.io.IOException;
import java.util.List;

import com.azurespringcloud.gatewayafd.service.model.CloudType;

public interface AzureServicesAddressResolver {
    List<String> getServiceAddresses(CloudType cloudType, String serviceType) throws IOException;
    // List<String> getAfdAddresses() throws IOException;

}
