package moe.caa.bukkit.dynamicsigns.util;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class Pair<V1, V2> {
    private final V1 v1;
    private final V2 v2;
}
