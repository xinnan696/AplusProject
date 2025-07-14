package com.ucd.urbanflow.dto;


import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Component
public class TraCIClient {
    private RestTemplate restTemplate = new RestTemplate();
    private final String BASE_URL = "http://10.241.114.122:8000";

    public TraCIClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(20000);
        factory.setReadTimeout(60000);
        this.restTemplate = new RestTemplate(factory);
    }

    public boolean checkSUMOStatus(){
        String url = BASE_URL + "/status";
        try {
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("connection")){
                Map<?, ?> conn = (Map<?, ?>) response.get("connection");
                return Boolean.TRUE.equals(conn.get("sumo_connected"));
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    // check junction_id
    public boolean checkJunctionExists(String junctionId){
        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/junction/exists").queryParam("junctionId", junctionId).toUriString();
        try {
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            return Boolean.TRUE.equals(response.get("exists"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // set duration
    public boolean setSignalDuration(String junctionId, int duration){
        String url = BASE_URL + "/trafficlight/set_duration";
        Map<String, Object> body = new HashMap<>();
        body.put("junctionId", junctionId);
        body.put("duration", duration);
        try {
            Map<?, ?> response = restTemplate.postForObject(url, body, Map.class);
            return "success".equals(response.get("status"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // set state+duration
    public boolean setSignalStateAndDuration(String junctionId, int lightIndex, String state, int duration) {
        String url = BASE_URL + "/trafficlight/set_state_duration";
        Map<String, Object> body = new HashMap<>();
        body.put("junctionId", junctionId);
        body.put("state", state);
        body.put("duration", duration);
        body.put("lightIndex", lightIndex);

        try {
            Map<?, ?> response = restTemplate.postForObject(url, body, Map.class);
            return "VERIFIED_AND_RUNNING".equals(response.get("status"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
