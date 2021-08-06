package com.azurespringcloud.gatewayafd;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;

import com.azurespringcloud.gatewayafd.service.AzureServicesAddressResolver;
import com.azurespringcloud.gatewayafd.service.model.CloudType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GatewayAfdApplicationTests {

	@Autowired
	AzureServicesAddressResolver addressResolver;

	@Test
	void contextLoads() {
	}


	@Test
	void resolveAdfAddresses_AzurePublic() throws IOException{
		List<String> addresses = addressResolver.getServiceAddresses(CloudType.Azure, "AzureFrontDoor.Backend");
		assertNotNull(addresses);
		assertNotEquals(0, addresses.size());
		
	}

}
