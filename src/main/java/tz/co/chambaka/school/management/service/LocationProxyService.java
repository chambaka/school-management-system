package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class LocationProxyService {

    private static final Logger log = LoggerFactory.getLogger(LocationProxyService.class);

    private final RestClient restClient;
    private final String baseUrl;

    public LocationProxyService(@Value("${location.service.base-url:http://127.0.0.1:8586}") String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.restClient = RestClient.builder().baseUrl(this.baseUrl).build();
    }

    public ResponseEntity<byte[]> forward(String pathAndQuery) {
        try {
            ResponseEntity<byte[]> upstream = restClient.get()
                    .uri(pathAndQuery)
                    .retrieve()
                    .toEntity(byte[].class);
            return sanitize(upstream);
        } catch (RestClientException ex) {
            log.warn("Location service request failed: {}{} — {}", baseUrl, pathAndQuery, ex.getMessage());
            throw new BusinessException("Location service unavailable");
        }
    }

    static ResponseEntity<byte[]> sanitize(ResponseEntity<byte[]> upstream) {
        HttpHeaders headers = new HttpHeaders();
        MediaType contentType = upstream.getHeaders().getContentType();
        if (contentType != null) {
            headers.setContentType(contentType);
        }
        return new ResponseEntity<>(upstream.getBody(), headers, upstream.getStatusCode());
    }
}
