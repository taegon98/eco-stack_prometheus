package openstack.eco_stack.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Component
@Slf4j
public class Token {

    private final RestTemplate restTemplate;

    @Autowired
    public Token(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
        this.username = "admin";
        this.password = "20secret23";
        this.authUrl = "http://133.186.215.103/identity/v3/auth/tokens";
    }

    private String authUrl;

    private String username;

    private String password;

    public String fetchToken() throws URISyntaxException {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String jsonRequest = "{\"auth\": {\"identity\": {\"methods\": [\"password\"],\"password\": {\"user\": {\"name\": \"" + username + "\",\"domain\": {\"id\": \"default\"},\"password\": \"" + password + "\"}},\"scope\": {\"domain\": {\"id\": \"default\"}}}}}";
        RequestEntity<String> requestEntity = new RequestEntity<>(jsonRequest, headers, HttpMethod.POST, new URI(authUrl));

        ResponseEntity<String> responseEntity = restTemplate.exchange(requestEntity, String.class);

        HttpHeaders responseHeaders = responseEntity.getHeaders();
        log.info(responseHeaders.toString());
        List<String> tokenList = responseHeaders.get("X-Subject-Token");

        log.info("success");
        return tokenList.get(0);
    }
}
