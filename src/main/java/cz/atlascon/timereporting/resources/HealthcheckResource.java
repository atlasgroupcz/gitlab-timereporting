package cz.atlascon.timereporting.resources;

import javax.annotation.Resource;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Resource
@Path("/health")
@Singleton
public class HealthcheckResource {

    @GET
    public String getHealthCheck() {
        return "ok";
    }

}
