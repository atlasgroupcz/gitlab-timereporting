package cz.atlascon.timereporting.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.ByteStreams;
import cz.atlascon.timereporting.domain.User;
import cz.atlascon.timereporting.services.DataService;
import cz.atlascon.timereporting.services.ReportElement;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Resource
@Path("/timelogs")
@Singleton
public class ReportResource {

    private final ObjectMapper om = new ObjectMapper();
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
    @Path("/hasData")
    public Response hasData() {
        return Response.ok(dataService.hasData()).build();
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("/getDataTimestamp")
    public Response getDataTimestamp() {
        return Response.ok(dataService.getDataTimestamp().toString()).build();
    }

    //  Hierarchy report
    // =================
    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("/hierarchy")
    public Response getHierarchyReport(@Context final UriInfo info) {

        final List<ReportElement> elements = info.getQueryParameters().get("elements").stream().map(ReportElement::valueOf).collect(Collectors.toList());
        final Instant from = getDate("from", info);
        final Instant to = getDate("to", info);

        final String result = dataService.getProcessor().getHierarchyReport(from, to, elements);

        return Response.ok(result).build();
    }

    private Instant getDate(final String key, final UriInfo info) {
        return LocalDate.parse(info.getQueryParameters().getFirst(key)).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("/hierarchyComponents")
    public Response getHierarchyReportElements(@Context final UriInfo info) {
        final Instant from = getDate("from", info);
        final Instant to = getDate("to", info);
        final ObjectNode node = om.createObjectNode();
        for (ReportElement el : ReportElement.values()) {
            final Set<String> components = dataService.getProcessor().getComponents(from, to, el);
            final ArrayNode ar = om.createArrayNode();
            components.forEach(c -> ar.add(c));
            node.set(el.name(), ar);
        }
        return Response.ok(node.toPrettyString()).build();
    }

    // User reports
    // =================

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("/users")
    public Response getUsers(@Context final UriInfo info) {
        final List<User> users = dataService.getProcessor().getUsers();
        final ArrayNode node = om.createArrayNode();
        for (User user : users) {
            node.add(om.valueToTree(user));
        }
        return Response.ok(node.toPrettyString()).build();
    }

    @GET
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Path("/timesheet")
    public Response getUserTimesheet(@Context final UriInfo info) throws Exception {
        final Instant from = getDate("from", info);
        final Instant to = getDate("to", info);
        try (final SXSSFWorkbook timesheet = dataService.getProcessor().createTimesheet(from, to)) {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                timesheet.write(bos);
                bos.close();
                return Response.ok(bos.toByteArray()).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"timesheet.xlsx\"").build();
            }
        }
    }

}