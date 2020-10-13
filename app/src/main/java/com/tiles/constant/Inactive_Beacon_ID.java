package com.tiles.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Inactive_Beacon_ID {

    public static List<String> INACTIVE_BEACON_ID_LIST = Arrays.asList(

            // Test
            "AC:23:3F:A0:33:E2",
            "AC:23:3F:24:AE:8B",
            "AC:23:3F:24:AE:65",
            "AC:23:3F:24:AE:A0",
            "AC:23:3F:A0:33:F3",
            "AC:23:3F:24:AE:5B",
            "AC:23:3F:A0:32:34",
            "AC:23:3F:A0:32:39",
            "AC:23:3F:24:AE:60",
            "AC:23:3F:A0:32:FD",
            "AC:23:3F:24:AE:C5",
            "AC:23:3F:24:AE:A8",
            "AC:23:3F:A0:32:36"
    );

    public static Set<String> INACTIVE_BEACON_ID_SET = new HashSet<>(INACTIVE_BEACON_ID_LIST);

}
