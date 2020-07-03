package cz.atlascon.timereporting.resources;

import com.google.common.io.ByteStreams;
import cz.atlascon.timereporting.services.DataService;
import cz.atlascon.timereporting.services.ReportElement;
import cz.atlascon.timereporting.services.ReportType;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Resource
@Path("/timelogs")
public class ReportResource {

    private final DataService dataService;

    @Inject
    public ReportResource(final DataService dataService) {
        this.dataService = dataService;
    }

    @PUT
    @Path("/upload")
    public Response upload(InputStream in) throws Exception {
        final File tempFile = File.createTempFile("zip-upload", ".zip");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            ByteStreams.copy(in, fos);
        }
        dataService.importFromFile(tempFile);
        final int logs = dataService.getProcessor().getTimeLogsCount();
        return Response.ok("Imported " + logs + " timelogs").build();
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("/report/{reportType}")
    public Response getDoc(@PathParam("reportType") final ReportType type,
                           @Context final UriInfo info) {

        final List<ReportElement> elements = info.getQueryParameters().get("elements").stream().map(ReportElement::valueOf).collect(Collectors.toList());
        final Instant from = LocalDate.parse(info.getQueryParameters().getFirst("from")).atStartOfDay().toInstant(ZoneOffset.UTC);
        final Instant to = LocalDate.parse(info.getQueryParameters().getFirst("to")).atStartOfDay().toInstant(ZoneOffset.UTC);

        final String result = dataService.getProcessor().process(from, to, type, elements);

        return Response.ok(result).build();

    }
}
