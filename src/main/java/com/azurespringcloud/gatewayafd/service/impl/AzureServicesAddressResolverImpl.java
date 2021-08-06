package com.azurespringcloud.gatewayafd.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.azurespringcloud.gatewayafd.service.AzureServicesAddressResolver;
import com.azurespringcloud.gatewayafd.service.model.CloudType;
import com.azurespringcloud.gatewayafd.service.model.ServiceTag;
import com.azurespringcloud.gatewayafd.service.model.ServiceTagList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AzureServicesAddressResolverImpl implements AzureServicesAddressResolver {

    private final ObjectMapper mapper = new ObjectMapper();

    private Log logger = LogFactory.getLog(AzureServicesAddressResolver.class);

    private final LoadingCache<ServiceCacheKey, List<String>> addressesCache;
    private final LoadingCache<CloudType, ServiceTagList> tagListCache;

    public AzureServicesAddressResolverImpl(Integer maxAgeInMinutes) {
        CacheLoader<CloudType, ServiceTagList> allTagListLoader = new CacheLoader<CloudType, ServiceTagList>() {
            @Override
            public ServiceTagList load(CloudType key) throws Exception {
                return getServiceTagList(key);
            }
        };

        CacheLoader<ServiceCacheKey, List<String>> addressesLoader = new CacheLoader<ServiceCacheKey, List<String>>() {
            @Override
            public final List<String> load(final ServiceCacheKey key) throws Exception {
                return retrieveAddresses(key.getCloudType(), key.getServiceType());
            }
        };        

        tagListCache = CacheBuilder.newBuilder().expireAfterWrite(maxAgeInMinutes, TimeUnit.MINUTES)
                .refreshAfterWrite(maxAgeInMinutes - 1, TimeUnit.MINUTES).build(allTagListLoader);


        addressesCache = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build(addressesLoader);
        
    }

    private ServiceTagList getServiceTagList(CloudType cloudType) throws IOException {
        String downloadPageUrl = getDownloadUrl(cloudType);
        logger.info(String.format("searching into %s to get the address list source", downloadPageUrl));
        String tagListUrl = extractDownloadPageUrl(downloadPageUrl);
        logger.info(String.format("found %s. downloading", tagListUrl));
        return retrieveObject(tagListUrl, ServiceTagList.class);
    }

    private List<String> retrieveAddresses(CloudType cloudType, String serviceType) {
        ServiceTagList tagList;
        try {
            tagList = this.tagListCache.get(cloudType);
        } catch (ExecutionException e) {
            logger.error("Error retrieving addresses for " + cloudType + " | " + serviceType, e);
            return null;
        }
        Optional<ServiceTag> serviceTags = tagList.getValues().stream()
                .filter(serviceTag -> serviceTag.getName().equals(serviceType)).findFirst();
        if (serviceTags.isPresent()) {
            return serviceTags.get().getProperties().getAddressPrefixes();
        }

        throw new InvalidParameterException("addresses for service " + serviceType + " not found");
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

    @Override
    public List<String> getServiceAddresses(CloudType cloudType, String serviceType) {
        try {
            return addressesCache.get(new ServiceCacheKey(cloudType, serviceType));
        } catch (ExecutionException e) {
            logger.error("Error on getServiceAddress for " + cloudType + " | " + serviceType, e);
            return null;
        }
    }

    private class ServiceCacheKey {
        String serviceType;
        CloudType cloudType;

        ServiceCacheKey(CloudType cloudType, String serviceType) {
            this.serviceType = serviceType;
            this.cloudType = cloudType;
        }

        public String getServiceType() {
            return serviceType;
        }

        public CloudType getCloudType() {
            return cloudType;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj instanceof ServiceCacheKey) {
                ServiceCacheKey other = (ServiceCacheKey) obj;
                return Objects.equals(getServiceType(), other.getServiceType())
                        && Objects.equals(getCloudType(), other.getCloudType());
            }
            return false;
        }
    }

}
