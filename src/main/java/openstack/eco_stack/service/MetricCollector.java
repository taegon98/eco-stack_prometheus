package openstack.eco_stack.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public interface MetricCollector {
    String prometheusUrl = "http://180.210.80.14:9090";
    String region = "Asia/Seoul";
    ZoneId seoulZoneId = ZoneId.of(region);
    ZonedDateTime now = ZonedDateTime.now(seoulZoneId);
    ZonedDateTime oneDayAgo = now.minusHours(24);
}
