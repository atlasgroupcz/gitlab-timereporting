package cz.atlascon.timereporting.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record User(@JsonProperty("id")int id,
                   @JsonProperty("email")String email,
                   @JsonProperty("name")String name) {
}
