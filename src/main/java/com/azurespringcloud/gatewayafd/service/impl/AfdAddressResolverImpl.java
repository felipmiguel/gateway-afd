package com.azurespringcloud.gatewayafd.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.InvalidParameterException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.azurespringcloud.gatewayafd.service.AfdAddressResolver;
import com.azurespringcloud.gatewayafd.service.model.CloudType;
import com.azurespringcloud.gatewayafd.service.model.ServiceTag;
import com.azurespringcloud.gatewayafd.service.model.ServiceTagList;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;

public class AfdAddressResolverImpl implements AfdAddressResolver {

    private ObjectMapper mapper = new ObjectMapper();

    private Log logger = LogFactory.getLog(AfdAddressResolver.class);

    @Value("${azurespringcloud.cloud-type:Azure}")
    private String cloud;
    @Value("${azurespringcloud.max-age-minutes:720}")
    private Integer maxAgeInMinutes;

    @Value("${azurespringcloud.service-type:AzureFrontDoor.Backend}")
    private String serviceType;

    private List<String> savedAddressList;
    private OffsetDateTime lastRetrieval;

    @Override
    public List<String> getAfdAddresses(CloudType cloudType) throws IOException {
        if (lastRetrieval == null || savedAddressList == null) {
            logger.info("Retrieving the list for the first time");
            savedAddressList = retrieveAddresses(cloudType);
            lastRetrieval = OffsetDateTime.now();
        }
        if (lastRetrieval != null) {
            if (lastRetrieval.plusMinutes(maxAgeInMinutes).isBefore(OffsetDateTime.now())) {
                logger.info("The last retrieval was at " + lastRetrieval + ". Retrieving the list.");
                List<String> list = retrieveAddresses(cloudType);
                savedAddressList = list;
                lastRetrieval = OffsetDateTime.now();
            }
        }
        return savedAddressList;
    }

    private List<String> retrieveAddresses(CloudType cloudType)
            throws IOException, JsonParseException, JsonMappingException {
        String downloadPageUrl = getDownloadUrl(cloudType);
        logger.info(String.format("searching into %s to get the address list source", downloadPageUrl));
        String tagListUrl = extractDownloadPageUrl(downloadPageUrl);
        logger.info(String.format("found %s. downloading", tagListUrl));
        return downloadAddressList(tagListUrl);
    }

    @Override
    public List<String> getAfdAddresses() throws IOException {
        CloudType cloudType = CloudType.valueOf(this.cloud);
        return getAfdAddresses(cloudType);
    }

    private List<String> downloadAddressList(String sourceUrl)
            throws JsonParseException, JsonMappingException, IOException {

        ServiceTagList tagList = this.retrieveObject(sourceUrl, ServiceTagList.class);
        Optional<ServiceTag> frontDoorBackend = tagList.getValues().stream()
                .filter(serviceTag -> serviceTag.getName().equals(serviceType)).findFirst();
        if (frontDoorBackend.isPresent()) {
            return frontDoorBackend.get().getProperties().getAddressPrefixes();
        }

        throw new InvalidParameterException("address not found in " + sourceUrl);
    }

    private <T> T retrieveObject(String endpoint, Class<T> valueType) throws IOException {

        URL url = URI.create(endpoint).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.connect();
        T result;
        try (InputStream inputStream = connection.getInputStream()) {

            result = mapper.readValue(inputStream, valueType);
        }
        connection.disconnect();
        return result;

    }

    private String extractDownloadPageUrl(String address) throws IOException {
        URL url = URI.create(address).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.connect();
        StringBuffer buffer;
        try (InputStream inputStream = connection.getInputStream()) {

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            buffer = new StringBuffer(connection.getContentLength());
            String line;
            int lastIndex = 0;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
                buffer.append("\n");
                int urlStartIndex = buffer.indexOf("https://download.microsoft.com/download", lastIndex);
                if (urlStartIndex >= 0) {
                    int urlEndIndex = buffer.indexOf("\"", urlStartIndex);
                    if (urlEndIndex != -1) {
                        return buffer.substring(urlStartIndex, urlEndIndex);
                    }
                } else {
                    lastIndex = buffer.length();
                }
            }
        } finally {
            connection.disconnect();
        }
        throw new InvalidParameterException("Download page not found in address " + address);
    }

    private String getDownloadId(CloudType cloudType) {
        switch (cloudType) {
            case China:
                return "57062"; // China: http://www.microsoft.com/en-us/download/details.aspx?id=57062
            case AzureGov:
                return "57063"; // US Gov: http://www.microsoft.com/en-us/download/details.aspx?id=57063
            case GermanyGov:
                return "57064"; // Germany: http://www.microsoft.com/en-us/download/details.aspx?id=57064
            case Azure:
                return "56519"; // Public: https://www.microsoft.com/en-us/download/details.aspx?id=56519
            default:
                throw new InvalidParameterException("Unexpected cloud type: " + cloudType);
        }
    }

    private String getDownloadUrl(CloudType cloudType) {
        String downloadPage = String.format("https://www.microsoft.com/en-us/download/confirmation.aspx?id=%s",
                getDownloadId(cloudType));
        return downloadPage;
    }

}
