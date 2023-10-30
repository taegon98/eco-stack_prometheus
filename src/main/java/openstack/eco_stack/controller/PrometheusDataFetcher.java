package openstack.eco_stack.controller;

import lombok.extern.slf4j.Slf4j;
import openstack.eco_stack.domain.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@Slf4j
public class PrometheusDataFetcher {

    private final RestTemplate restTemplate;

    @Autowired
    public PrometheusDataFetcher(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @GetMapping("/prometheus-metrics")
    public ResponseEntity<String> getPrometheusMetrics() throws URISyntaxException {

        // Prometheus 메트릭 데이터를 가져올 URL 설정
        String prometheusMetricsURL = "http://133.186.215.103:9000/metrics";
        Token token = new Token(new RestTemplateBuilder());
        String AUTH_TOKEN = token.fetchToken();

        // RestTemplate을 사용하여 메트릭 데이터를 문자열로 가져오기
        ResponseEntity<String> response = restTemplate.getForEntity(prometheusMetricsURL, String.class);
        String metricData = response.getBody();

        // 정규 표현식 패턴
        Pattern pattern = Pattern.compile("instanceName=\"([^\"]+)\".*projectId=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(metricData);

        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String instanceName = matcher.group(1);
            String projectId = matcher.group(2);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Token", AUTH_TOKEN); // AUTH_TOKEN은 토큰 값으로 대체

            HttpEntity<?> entity = new HttpEntity<>(headers);

            String projectURL = "http://133.186.215.103/identity/v3/projects/" + projectId; // projectId는 프로젝트 ID로 대체

            ResponseEntity<String> project = restTemplate.exchange(projectURL, HttpMethod.GET, entity, String.class);

            String projectData = project.getBody();

            result.append("Instance Name: ").append(instanceName).append("\n");
            result.append("Project Id: ").append(projectId).append("\n");
            result.append("Project Info: ").append(projectData).append("\n");
        }

        return ResponseEntity.ok(result.toString());
    }
}