package cz.atlascon.timereporting.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Group(@JsonProperty("id")int id,
                    @JsonProperty("name")String name,
                    @JsonProperty("description")String description) {
}
