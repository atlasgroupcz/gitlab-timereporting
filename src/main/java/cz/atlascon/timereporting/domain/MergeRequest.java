package cz.atlascon.timereporting.domain;

import java.time.Instant;

public record MergeRequest(Integer id,
                           Integer author_id,
                           Integer target_project_id,
                           String target_branch,
                           String source_branch,
                           Instant created_at,
                           String title
) {

}
