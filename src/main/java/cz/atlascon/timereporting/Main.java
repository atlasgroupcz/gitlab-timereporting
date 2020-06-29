package cz.atlascon.timereporting;

import cz.atlascon.timereporting.domain.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {

        final Parser parser = new Parser();
        try (final ZipFile zip = new ZipFile("/tmp/export.zip")) {
            List<TimeLog> logs = parse(zip, "timelogs.csv", parser, parser::parseTimeLog);
            List<Group> groups = parse(zip, "namespaces.csv", parser, parser::parseGroup);
            List<Label> labels = parse(zip, "labels.csv", parser, parser::parseLabel);
            List<User> users = parse(zip, "users.csv", parser, parser::parseUser);
            List<Project> projects = parse(zip, "projects.csv", parser, parser::parseProject);
            List<Issue> issues = parse(zip, "issues.csv", parser, parser::parseIssue);
            List<MergeRequest> mergeRequests = parse(zip, "merge_requests.csv", parser, parser::parseMergeRequest);
            List<LabelLink> labelLinks = parse(zip, "label_links.csv", parser, parser::parseLabelLink);
            LOGGER.info("Parsed {} time logs", logs.size());
            
        }


    }

    private static <E> List<E> parse(final ZipFile zip,
                                     final String entryName,
                                     final Parser parser,
                                     final Supplier<E> parse) throws Exception {
        final List<E> list = new ArrayList<>();
        final ZipEntry entry = zip.getEntry(entryName);
        try (InputStream in = zip.getInputStream(entry);
             InputStreamReader reader = new InputStreamReader(in);) {
            final CSVParser csv = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
            final Iterator<CSVRecord> it = csv.iterator();
            while (it.hasNext()) {
                final CSVRecord rec = it.next();
                parser.setRecord(rec);
                E itm = parse.get();
                list.add(itm);
            }
        }
        return list;
    }

}
