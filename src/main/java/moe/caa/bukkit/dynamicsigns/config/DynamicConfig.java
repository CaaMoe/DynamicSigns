package moe.caa.bukkit.dynamicsigns.config;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import moe.caa.bukkit.dynamicsigns.util.Pair;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class DynamicConfig {
    private final String path;
    private final List<Pair<Integer, Entry>> dynamicContents;
    private final int visualRange;
    private final int lookMeRange;
    private final transient int countTicks;

    private DynamicConfig(String path, List<Pair<Integer, Entry>> dynamicContents, int visualRange, int lookMeRange) {
        this.path = path;
        this.dynamicContents = dynamicContents;
        this.visualRange = visualRange;
        this.lookMeRange = lookMeRange;
        this.countTicks = dynamicContents.stream().map(Pair::getV1).mapToInt(value -> value).sum();
    }


    public static DynamicConfig of(String path, ConfigurationSection section) {
        List<Pair<Integer, Entry>> dynamicContents = section.getMapList("dynamicContents").stream()
                .map(map -> new Pair<>(Integer.parseInt(map.get("delay").toString()), Entry.of(map)))
                .collect(Collectors.toList());
        int visualRange = section.getInt("visualRange", 10);
        int lookMeRange = section.getInt("lookMeRange", 5);

        return new DynamicConfig(path, dynamicContents, visualRange, lookMeRange);
    }

    public Entry getContentAT(int tick) {
        tick = tick % countTicks;
        for (Pair<Integer, Entry> content : dynamicContents) {
            tick -= content.getV1();
            if (tick <= 0) {
                return content.getV2();
            }
        }
        return dynamicContents.get(0).getV2();
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    public static class Entry {
        private final List<String> content;
        private final boolean glowingText;
        private final int rotation;

        public static Entry of(Map<?, ?> section) {
            List<String> content = ((List<?>) section.get("content")).stream().map(Object::toString).collect(Collectors.toList());
            if (content.size() != 4) {
                throw new IndexOutOfBoundsException("Size of contents is " + content.size() + ", not 4.");
            }
            boolean glowingText = section.containsKey("glowingText") && (Boolean) section.get("glowingText");  // default false
            int rotation = section.containsKey("rotation") ? (Integer) section.get("rotation") : -1;
            return new Entry(content, glowingText, rotation);
        }
    }
}
