package com.azurespringcloud.gatewayafd.service;

import java.io.IOException;
import java.util.List;

import com.azurespringcloud.gatewayafd.service.model.CloudType;

public interface AfdAddressResolver {
    List<String> getAfdAddresses(CloudType cloudType) throws IOException;
    List<String> getAfdAddresses() throws IOException;

}
