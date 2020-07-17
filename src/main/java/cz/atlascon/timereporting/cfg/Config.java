package cz.atlascon.timereporting.cfg;

import cz.atlascon.timereporting.resources.HealthcheckResource;
import cz.atlascon.timereporting.resources.ReportResource;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.ws.rs.ApplicationPath;

@Component
@ApplicationPath("/rest")
public class Config extends ResourceConfig {

    @PostConstruct
    public void registerEndpoints() {
        // jersey
        register(LoggingFeature.class);
        register(MultiPartFeature.class);
        register(CORSFilter.class);
        // app
        register(ReportResource.class);
        register(HealthcheckResource.class);

    }
}