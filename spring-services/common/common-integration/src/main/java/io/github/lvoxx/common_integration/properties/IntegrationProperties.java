package io.github.lvoxx.common_integration.properties;

@Data
@Configuration
@ConfigurationProperties(prefix = "integration")
public class IntegrationProperties {
    
    private ServiceConfig userService;
    private ServiceConfig groupService;
    
    @Data
    public static class ServiceConfig {
        private String url;
        private int timeout;
    }
}