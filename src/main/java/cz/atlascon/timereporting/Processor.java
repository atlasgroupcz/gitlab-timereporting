package cz.atlascon.timereporting;

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
            if (ll.target_type() == LabelLink.Type.ISSUE) {
                issueLabels.computeIfAbsent(issues.get(ll.id()), id -> new HashSet<>()).add(label);
            } else if (ll.target_type() == LabelLink.Type.MERGE_REQUEST) {
                mergeRequestLabels.computeIfAbsent(mergeRequests.get(ll.id()), id -> new HashSet<>()).add(label);
            } else {
                throw new IllegalArgumentException("?");
            }
        });
    }


    public List<TimeLog> process(final Instant from, final Instant to) {
        final List<TimeLog> filtered = logs.stream()
                .filter(l -> l.created_at().isAfter(from) && l.created_at().isBefore(to))
                .collect(Collectors.toList());
        LOGGER.info("Processing {} time logs", filtered.size());

        final SunburstBuilder sunburstBuilder = new SunburstBuilder(4);
        filtered.stream().forEach(timeLog -> {
            final Namespace ns = getNamespace(timeLog);
            final Project project = getProject(timeLog);
            final Issue issue = getIssue(timeLog);
            final User user = getUser(timeLog);
            sunburstBuilder.addTime(timeLog.time_spent(), ns.name(), project.name(), issue.title(), user.name());
        });
        final String build = sunburstBuilder.build();

        return filtered;
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


}
