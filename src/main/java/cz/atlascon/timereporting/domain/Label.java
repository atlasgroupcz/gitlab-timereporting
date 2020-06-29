package cz.atlascon.timereporting.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Label(@JsonProperty("id")int id,
                    @JsonProperty("title")String title,
                    @JsonProperty("color")String color,
                    @JsonProperty("description")String description) {
}
