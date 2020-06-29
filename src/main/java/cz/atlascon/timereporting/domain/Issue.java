package cz.atlascon.timereporting.domain;

import java.time.Instant;

public record Issue(Integer id,
                    Integer author_id,
                    Integer project_id,
                    Instant created_at,
                    String title,
                    String description
) {

}
