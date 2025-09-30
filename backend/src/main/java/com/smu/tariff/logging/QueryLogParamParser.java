package com.smu.tariff.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class QueryLogParamParser {

    private static final ObjectMapper mapper = new ObjectMapper();

    // Try to parse params as JSON (e.g. {"origin":"SGP","dest":"USA"}),
    // otherwise fall back to simple key:val pairs separated by commas.
    public static Map<String, String> parse(String params) {
        Map<String, String> out = new HashMap<>();
        if (params == null) return out;

        String s = params.trim();
        if (s.isEmpty()) return out;

        // First try JSON
        try {
            // permissive: if it's not strictly JSON this will throw
            Map<String, Object> m = mapper.readValue(s, new TypeReference<Map<String, Object>>() {});
            for (Map.Entry<String, Object> e : m.entrySet()) {
                if (e.getValue() != null) out.put(e.getKey().toLowerCase(), e.getValue().toString());
            }
            return out;
        } catch (JsonProcessingException e) {
            // Not valid JSON - continue to simple parsing
        }

        // strip braces if present
        if (s.startsWith("{" ) && s.endsWith("}")) {
            s = s.substring(1, s.length()-1).trim();
        }

        if (s.isEmpty()) return out;

        String[] parts = s.split(",");
        for (String p : parts) {
            String pair = p.trim();
            if (pair.isEmpty()) continue;
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                out.put(kv[0].trim().toLowerCase(), kv[1].trim());
            }
        }
        return out;
    }
}
