package com.tiles.constant;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Active_Beacon_ID {

    public static List<String> ACTIVE_BEACON_ID_LIST = Arrays.asList(
            // Test
            "AC:23:3F:24:AE:60", "AC:23:3F:24:AE:A8", "AC:23:3F:24:AE:65",
            "AC:23:3F:A0:32:36", "AC:23:3F:24:AE:C5", "AC:23:3F:A0:32:34");

    public static Set<String> ACTIVE_BEACON_ID_SET = new HashSet<>(ACTIVE_BEACON_ID_LIST);

}
