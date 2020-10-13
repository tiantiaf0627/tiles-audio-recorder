package com.tiles.constant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Owl_ID {

    public static Map<String, String> OWL_ID_SET = new HashMap<String, String>(){{

        // Test one
        put("001bc509408201bf", "30:45:11:55:F1:94");

    }};

    public static List<String> DEVICE_ID_LIST = new ArrayList<>(OWL_ID_SET.values());
    public static Set<String>  DEVICE_ID_SET = new HashSet<>(DEVICE_ID_LIST);


}
