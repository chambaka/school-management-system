package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class AddressText {

    public static final String SAMPLE =
            "{\"v\":1,\"line\":\"Mtaa 1\",\"region\":\"DAR ES SALAAM\",\"district\":\"ILALA\",\"ward\":\"KARIAKOO\",\"box\":\"\"}";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AddressText() {
    }

    public static void require(String address) {
        if (!valid(address)) {
            throw new BusinessException("Enter region, district, ward, and street");
        }
    }

    public static boolean valid(String address) {
        if (address == null || address.isBlank()) {
            return false;
        }
        try {
            JsonNode node = MAPPER.readTree(address);
            return node.path("v").asInt() == 1
                    && present(node, "line")
                    && present(node, "region")
                    && present(node, "district")
                    && present(node, "ward");
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean present(JsonNode node, String field) {
        return !node.path(field).asText("").isBlank();
    }
}
