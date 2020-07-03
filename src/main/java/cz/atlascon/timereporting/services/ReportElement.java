package cz.atlascon.timereporting.services;

import cz.atlascon.timereporting.domain.*;

public enum ReportElement {

    NAMESPACE(Namespace.class),
    PROJECT(Project.class),
    ISSUE(Issue.class),
    USER(User.class),
    LABEL(Label.class);

    private final Class el;

    ReportElement(final Class el) {
        this.el = el;
    }
}
