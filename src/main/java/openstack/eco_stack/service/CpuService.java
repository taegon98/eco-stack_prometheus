package openstack.eco_stack.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class CpuService {

    public Map<String, Double> cpu_코어수(String metricData) {
        Map<String, Double> result = new HashMap<>();

        // 정규식 패턴 설정
        Pattern pattern = Pattern.compile("libvirt_domain_info_virtual_cpus\\{[^}]*\\} ([0-9.]+)");

        Matcher matcher = pattern.matcher(metricData);

        while (matcher.find()) {
            try {
                double value = Double.parseDouble(matcher.group(1));
                // 프로젝트 아이디 추출
                String projectId = extractProjectId(matcher.group(0));

                if (projectId != null) {
                    // 프로젝트 아이디별로 값을 매핑
                    result.put(projectId, value);
                }
            } catch (NumberFormatException e) {
                log.error("Error parsing virtual CPU metric value", e);
            }
        }

        return result;
    }

    public Map<String, Double> cpu_사용시간(String metricData) {
        Map<String, Double> result = new HashMap<>();

        // 정규식 패턴 변경
        Pattern pattern = Pattern.compile("libvirt_domain_info_cpu_time_seconds_total\\{[\\w=\".,:\\s-]*\\} ([0-9.]+)");

        Matcher matcher = pattern.matcher(metricData);

        while (matcher.find()) {
            try {
                double cpuMetric = Double.parseDouble(matcher.group(1));

                // 프로젝트 아이디 추출
                String projectId = extractProjectId(matcher.group(0));

                if (projectId != null) {
                    result.put(projectId, cpuMetric);
                }
            } catch (NumberFormatException e) {
                log.error("Error parsing CPU metric value", e);
            }
        }
        return result;
    }

    public String extractProjectId(String metricData) {
        // 정규식 패턴 변경
        Pattern pattern = Pattern.compile("projectId=\"([^\"]+)\"");

        Matcher matcher = pattern.matcher(metricData);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null; // 일치하는 프로젝트 아이디가 없는 경우
    }




}
