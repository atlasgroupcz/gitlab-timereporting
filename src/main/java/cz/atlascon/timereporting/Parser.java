package cz.atlascon.timereporting;

import cz.atlascon.timereporting.domain.*;
import org.apache.commons.csv.CSVRecord;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

public class Parser {


    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_DATE)
            .appendLiteral(' ')
            .append(ISO_LOCAL_TIME)
            .toFormatter();

    private CSVRecord record;

    public void setRecord(final CSVRecord record) {
        this.record = record;
    }

    private String getString(final String key) {
        return record.get(key);
    }

    private Integer getInt(final String key) {
        final String num = getString(key);
        return num.isBlank() ? null : Integer.parseInt(num);
    }

    private final Instant getInstant(String key) {
        final String val = getString(key);
        if (val.isBlank()) {
            return null;
        } else {
            final TemporalAccessor parsed = DATE_TIME_FORMATTER.parse(val);
            return LocalDateTime.from(parsed).toInstant(ZoneOffset.UTC);
        }
    }

    public TimeLog parseTimeLog() {
        return new TimeLog(
                getInt("id"),
                getInt("time_spent"),
                getInt("user_id"),
                getInstant("created_at"),
                getInstant("updated_at"),
                getInt("issue_id"),
                getInt("merge_request_id"));
    }

    public Group parseGroup() {
        return new Group(
                getInt("id"),
                getString("name"),
                getString("description")
        );
    }

    public Label parseLabel() {
        return new Label(
                getInt("id"),
                getString("title"),
                getString("color"),
                getString("description")
        );
    }

    public User parseUser() {
        return new User(
                getInt("id"),
                getString("email"),
                getString("name")
        );
    }

    public Project parseProject() {
        return new Project(
                getInt("id"),
                getString("name"),
                getString("description")
        );
    }

    public Issue parseIssue() {
        return new Issue(
                getInt("id"),
                getInt("author_id"),
                getInt("project_id"),
                getInstant("created_at"),
                getString("title"),
                getString("description")
        );
    }

    public MergeRequest parseMergeRequest() {
        return new MergeRequest(
                getInt("id"),
                getInt("author_id"),
                getInt("target_project_id"),
                getString("target_branch"),
                getString("source_branch"),
                getInstant("created_at"),
                getString("title")
        );
    }
}
