package cz.atlascon.timereporting.domain;

public record LabelLink(
        int id,
        int label_id,
        int target_id,
        Type target_type
) {

    public enum Type {
        MERGE_REQUEST,
        ISSUE
    }

}
