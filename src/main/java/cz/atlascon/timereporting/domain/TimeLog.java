package cz.atlascon.timereporting.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record TimeLog(
        @JsonProperty("id")int id,
        @JsonProperty("time_spent")int time_spent,
        @JsonProperty("user_id")int user_id,
        @JsonProperty("created_at")Instant created_at,
        @JsonProperty("updated_at")Instant updated_at,
        @JsonProperty("issue_id")Integer issue_id,
        @JsonProperty("merge_request_id")Integer merge_request_id) {

}
