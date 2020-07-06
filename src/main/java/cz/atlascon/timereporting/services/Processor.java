package cz.atlascon.timereporting.services;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import cz.atlascon.timereporting.domain.*;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

public class Processor {

    private static final Logger LOGGER = LoggerFactory.getLogger(Processor.class);
    private static final Issue MR_ISSUE = new Issue(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
            Instant.ofEpochSecond(0), "MergeRequest", "Fake issue for merge requests");
    private static final String PRODUKT_PREFIX = "produkt-";
    private static final String UNKNOWN_PRODUCT = "neznámý";
    private final List<TimeLog> logs;
    private final Map<Integer, Namespace> namespaces;
    private final Map<Integer, Label> labels;
    private final Map<Integer, User> users;
    private final Map<Integer, Project> projects;
    private final Map<Integer, Issue> issues;
    private final Map<Integer, MergeRequest> mergeRequests;
    private final List<LabelLink> labelLinks;
    // labels
    private final Map<MergeRequest, Set<String>> mergeRequestLabels = Maps.newHashMap();
    private final Map<Issue, Set<String>> issueLabels = Maps.newHashMap();
    // products
    private final Map<MergeRequest, String> mergeRequestProduct = Maps.newHashMap();
    private final Map<Issue, String> issueProduct = Maps.newHashMap();

    public Processor(final List<TimeLog> logs,
                     final List<Namespace> namespaces,
                     final List<Label> labels,
                     final List<User> users,
                     final List<Project> projects,
                     final List<Issue> issues,
                     final List<MergeRequest> mergeRequests,
                     final List<LabelLink> labelLinks) {
        this.logs = logs;
        this.namespaces = namespaces.stream().collect(Collectors.toMap(Namespace::id, i -> i));
        this.labels = labels.stream().collect(Collectors.toMap(Label::id, i -> i));
        this.users = users.stream().collect(Collectors.toMap(User::id, i -> i));
        this.projects = projects.stream().collect(Collectors.toMap(Project::id, i -> i));
        this.issues = issues.stream().collect(Collectors.toMap(Issue::id, i -> i));
        this.mergeRequests = mergeRequests.stream().collect(Collectors.toMap(MergeRequest::id, i -> i));
        this.labelLinks = labelLinks;
        labelItems();
    }

    public List<User> getUsers() {
        final List<User> users = new ArrayList<>(Set.copyOf(this.users.values()));
        Collections.sort(users, Comparator.comparing(User::name).thenComparing(User::id));
        return users;
    }

    public Set<String> getComponents(Instant from, Instant to, final ReportElement element) {
        final Set<String> elements = getLogWindow(from, to).stream().map(log ->
                switch (element) {
                    case ISSUE -> getVisualizable(log, element);
                    case NAMESPACE -> getVisualizable(log, element);
                    case PRODUCT -> getVisualizable(log, element);
                    case PROJECT -> getVisualizable(log, element);
                    case USER -> getVisualizable(log, element);
                }).collect(Collectors.toSet());
        final ArrayList<String> sorted = new ArrayList<>(elements);
        Collections.sort(sorted);
        return new LinkedHashSet<>(sorted);
    }

    /**
     * attach labels to merge requests / issues
     */
    private void labelItems() {
        labelLinks.forEach(ll -> {
            final String label = labels.get(ll.label_id()).title().strip().toLowerCase();
            final String product = productFromLabelDescription(labels.get(ll.label_id()).description());
            if (ll.target_type() == LabelLink.Type.ISSUE) {
                final Issue issue = issues.get(ll.target_id());
                issueLabels.computeIfAbsent(issue, id -> new HashSet<>()).add(label);
                if (product != null) {
                    issueProduct.computeIfAbsent(issue, id -> product);
                }
            } else if (ll.target_type() == LabelLink.Type.MERGE_REQUEST) {
                final MergeRequest mergeRequest = mergeRequests.get(ll.target_id());
                mergeRequestLabels.computeIfAbsent(mergeRequest, id -> new HashSet<>()).add(label);
                if (product != null) {
                    mergeRequestProduct.computeIfAbsent(mergeRequest, id -> product);
                }
            } else {
                throw new IllegalArgumentException("?");
            }
        });
    }

    private String productFromLabelDescription(final String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        if (description.contains(PRODUKT_PREFIX)) {
            final List<String> itemz = Splitter.on(' ').splitToList(description.strip().toLowerCase());
            final String prod = itemz.stream().filter(i -> i.contains(PRODUKT_PREFIX)).findFirst().orElse(null);
            return prod.substring(PRODUKT_PREFIX.length());
        } else {
            return null;
        }
    }


    public String getHierarchyReport(final Instant from, final Instant to,
                                     final List<ReportElement> elements) {
        // filter
        final List<TimeLog> filtered = getLogWindow(from, to);
        LOGGER.info("Processing {} time logs", filtered.size());

        // build
        return createSunburst(filtered, elements);

    }

    private List<TimeLog> getLogWindow(final Instant from, final Instant to) {
        return logs.stream().filter(l -> l.created_at().isAfter(from) && l.created_at().isBefore(to))
                .collect(Collectors.toList());
    }

    private String createSunburst(final List<TimeLog> logs,
                                  final List<ReportElement> elements) {
        final HierarchyReportBuilder hierarchyReportBuilder = new HierarchyReportBuilder(elements.size());
        logs.stream().forEach(timeLog -> {
            final List<String> itemz = Lists.newArrayList();
            for (ReportElement element : elements) {
                itemz.add(getVisualizable(timeLog, element));
            }
            hierarchyReportBuilder.addTime(timeLog.time_spent(), itemz.toArray(new String[0]));
        });
        return hierarchyReportBuilder.build();
    }

    private String getVisualizable(final TimeLog timeLog,
                                   final ReportElement element) {
        return switch (element) {
            case ISSUE -> getIssue(timeLog).title();
            case USER -> getUser(timeLog).name();
            case NAMESPACE -> getNamespace(timeLog).name();
            case PROJECT -> getProject(timeLog).name();
            case PRODUCT -> getProduct(timeLog);
            default -> throw new IllegalArgumentException("Unknown element " + element);
        };
    }

    private String getProduct(final TimeLog timeLog) {
        final Issue issue = getIssue(timeLog);
        return issueProduct.getOrDefault(issue, UNKNOWN_PRODUCT);
    }

    private User getUser(final TimeLog timeLog) {
        return users.get(timeLog.user_id());
    }

    private Issue getIssue(final TimeLog timeLog) {
        if (timeLog.issue_id() != null) {
            final Issue issue = issues.get(timeLog.issue_id());
            return issue;
        } else {
            return issues.computeIfAbsent(Integer.MAX_VALUE, n -> MR_ISSUE);
        }
    }

    private Project getProject(final TimeLog timeLog) {
        if (timeLog.merge_request_id() != null) {
            final MergeRequest mr = mergeRequests.get(timeLog.merge_request_id());
            return projects.get(mr.target_project_id());
        } else {
            return projects.get(getIssue(timeLog).project_id());
        }
    }

    private Namespace getNamespace(final TimeLog timeLog) {
        return getNamespace(getProject(timeLog));
    }

    private Namespace getNamespace(final Issue issue) {
        return getNamespace(projects.get(issue.project_id()));
    }

    private Namespace getNamespace(final Project project) {
        return namespaces.get(project.namespace_id());
    }

    private Namespace getNamespace(final MergeRequest mr) {
        return getNamespace(projects.get(mr.target_project_id()));
    }


    public int getTimeLogsCount() {
        return logs.size();
    }

    public SXSSFWorkbook createTimesheet(final Instant from, final Instant to) throws IOException {
        // crate workbook
        final SXSSFWorkbook workbook = new SXSSFWorkbook();
        workbook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        // sort logs by time
        final List<TimeLog> logWindow = getLogWindow(from, to).stream().sorted(Comparator.comparing(TimeLog::created_at)).collect(Collectors.toList());
        // for each user
        for (int userId : users.keySet()) {
            // skip non existing
            final User user = users.get(userId);
            // user logs
            final List<TimeLog> forUser = logWindow.stream().filter(log -> log.user_id() == userId).collect(Collectors.toList());
            if (forUser.isEmpty()) {
                continue;
            }
            // create and fill user sheet
            final SXSSFSheet sheet = workbook.createSheet(user.name() + " (" + user.id() + ")");
            // header
            sheet.createRow(0);
            final SXSSFRow header = sheet.getRow(0);
            header.getCell(0).setCellValue("Datum");
            header.getCell(1).setCellValue("Hodiny");
            header.getCell(2).setCellValue("Namespace");
            header.getCell(3).setCellValue("Project");
            header.getCell(4).setCellValue("Produkt");
            header.getCell(5).setCellValue("Issue");
            for (int i = 0; i < forUser.size(); i++) {
                final TimeLog log = forUser.get(i);
                final LocalDate date = log.created_at().atOffset(ZoneOffset.UTC).toLocalDate();
                sheet.createRow(i + 1);
                final SXSSFRow row = sheet.getRow(i + 1);
                // day
                row.getCell(0).setCellValue(date.toString());
                // worked hours
                final int time = Math.abs(log.time_spent());
                final String timeFormat = DurationFormatUtils.formatDuration(Duration.ofSeconds(time).toMillis(), "H:mm:ss", true);
                row.getCell(1).setCellValue(log.time_spent() < 0 ? ("-" + timeFormat) : timeFormat);
                // namespace
                row.getCell(2).setCellValue(getVisualizable(log, ReportElement.NAMESPACE));
                // project
                row.getCell(3).setCellValue(getVisualizable(log, ReportElement.PROJECT));
                // product
                row.getCell(4).setCellValue(getVisualizable(log, ReportElement.PRODUCT));
                // issue
                final Issue issue = getIssue(log);
                row.getCell(5).setCellValue("[" + issue.id() + "] " + issue.title());
            }
        }
        return workbook;
    }

    public static record DayWork(int minutes, String formated) {
    }

    public Map<LocalDate, DayWork> createCalendar(final int year, final int userId) {
        final LocalDate startDay = LocalDate.of(year, 1, 1);
        final Instant from = startDay.atStartOfDay().toInstant(ZoneOffset.UTC);
        final LocalDate endDay = startDay.plusYears(1).minusDays(1);
        final Instant to = endDay.plusDays(1).atStartOfDay().minusSeconds(1).toInstant(ZoneOffset.UTC);
        // put work to calendar
        final Map<LocalDate, Integer> calendar = Maps.newTreeMap();
        getLogWindow(from, to).stream()
                .filter(l -> l.user_id() == userId)
                .forEach(log -> {
                    final LocalDate logDay = log.created_at().atOffset(ZoneOffset.UTC).toLocalDate();
                    calendar.compute(logDay, (day, time) -> time == null ? log.time_spent() : time + log.time_spent());
                });
        startDay.datesUntil(startDay.plusYears(1)).forEach(day -> {
            calendar.computeIfAbsent(day, d -> 0);
        });
        final Map<LocalDate, DayWork> formated = Maps.newLinkedHashMap();
        calendar.forEach((d, t) -> {
            final int time = Math.abs(t);
            final String timeFormat = DurationFormatUtils.formatDuration(Duration.ofSeconds(time).toMillis(), "H:mm:ss", true);
            final String tm = t < 0 ? ("-" + timeFormat) : timeFormat;
            formated.put(d, new DayWork(t / 60, tm));
        });
        return formated;
    }
}
