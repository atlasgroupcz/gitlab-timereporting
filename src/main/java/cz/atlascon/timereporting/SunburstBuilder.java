package cz.atlascon.timereporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SunburstBuilder {

    private final Map map = Maps.newHashMap();
    private final ObjectMapper om = new ObjectMapper();
    private final int depth;

    public SunburstBuilder(final int depth) {
        Preconditions.checkArgument(depth > 0);
        this.depth = depth;
    }

    public void addTime(int time, String... hierarchy) {
        Preconditions.checkNotNull(hierarchy);
        Preconditions.checkArgument(hierarchy.length == depth);
        Map current = map;
        for (int i = 0; i < hierarchy.length; i++) {

            if (i == hierarchy.length - 1) {
                // leaf
                final AtomicInteger cnt = (AtomicInteger) current.computeIfAbsent(hierarchy[i], h -> new AtomicInteger());
                cnt.addAndGet(time);
            } else {
                // not leaf
                current = (Map) current.computeIfAbsent(hierarchy[i], h -> new HashMap<>());
            }
        }
    }

    public String build() {
        final ObjectNode root = om.createObjectNode();
        root.put("name", "flare");
        final ArrayNode ar = om.createArrayNode();
        root.set("children", ar);
        appendNodes(ar, map);
        return root.toPrettyString();
    }

    private void appendNodes(final ArrayNode ar, final Map m) {
        m.forEach((k, v) -> {
            // set name
            final ObjectNode on = om.createObjectNode();
            on.put("name", (String) k);
            if (v instanceof AtomicInteger) {
                // leaf
                on.put("value", ((AtomicInteger) v).get());
            } else {
                // not leaf
                Map inner = (Map) v;
                final ArrayNode nextAr = om.createArrayNode();
                on.set("children", nextAr);
                appendNodes(nextAr, inner);
            }
            ar.add(on);
        });
    }

}
