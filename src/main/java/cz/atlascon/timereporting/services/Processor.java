package cz.atlascon.timereporting.services;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import cz.atlascon.timereporting.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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


    public String process(final Instant from,
                          final Instant to,
                          final ReportType type,
                          final List<ReportElement> elements) {
        // filter
        final List<TimeLog> filtered = logs.stream()
                .filter(l -> l.created_at().isAfter(from) && l.created_at().isBefore(to))
                .collect(Collectors.toList());
        LOGGER.info("Processing {} time logs", filtered.size());

        // build
        return switch (type) {
            case SUNBURST -> createSunburst(filtered, elements);
            default -> throw new IllegalArgumentException("Unknown report type");
        };

    }

    private String createSunburst(final List<TimeLog> logs,
                                  final List<ReportElement> elements) {
        final SunburstBuilder sunburstBuilder = new SunburstBuilder(elements.size());
        logs.stream().forEach(timeLog -> {
            final List<String> itemz = Lists.newArrayList();
            for (ReportElement element : elements) {
                itemz.add(getVisualizable(timeLog, element));
            }
            sunburstBuilder.addTime(timeLog.time_spent(), itemz.toArray(new String[0]));
        });
        return sunburstBuilder.build();
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
}
