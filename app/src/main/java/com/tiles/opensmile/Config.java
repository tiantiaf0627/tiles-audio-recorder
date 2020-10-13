package com.tiles.opensmile;

import java.util.ArrayList;

/**
 * Created by tiantianfeng on 10/18/17.
 */

public class Config {

    public String[] assets = {
            "liveinput_android_radar.conf",
            "BufferModeRb.conf.inc",
            "BufferModeLive.conf.inc",
            "BufferModeRbLag.conf.inc",
            "BufferMode.conf.inc",
            "messages.conf.inc",
            "features.conf.inc",
            "jelly_vad.conf",
            "message_vad.conf.inc",
            "FrameModeFunctionals.conf.inc",
            "FrameModeFunctionalsLive.conf.inc",
            "eGeMAPSv01a_core.func.conf.inc",
            "GeMAPSv01a_core.lld.conf.inc",
            "GeMAPSv01a_core.func.conf.inc",
            "eGeMAPSv01a_core.lld.conf.inc",
            "emobase_live4.conf",
            "jelly_vad_opensource.conf",
            "message_vad_pitch.conf.inc",
            "jelly_vad_pitch.conf",
            "jelly_vad_NA.conf",
            "emobase_live4_no_wav.conf",
            "TILEs_core_lld_lldde_llddede.conf",
            "TILEs_core_temp.conf",
            "TILEs_core_lld.conf",
            "TILEs_phase2_core_lld.conf",
            "TILEs_core_lld_ambient.conf"
    };

    public String mainConf = "liveinput_android.conf";

    // VAD
    public String vadConf = "jelly_vad.conf";
    public String vadPitchConf = "jelly_vad_pitch.conf";
    public String vadNAConf = "jelly_vad_NA.conf";

    // Feature Extraction
    public String saveDataConf = "TILEs_phase2_core_lld.conf";
    // public String saveDataConf = "TILEs_core_lld.conf";
    public String saveDataConf_no_Wav = "TILEs_core_lld.conf";
    public String saveAmbientDataConf = "TILEs_core_lld_ambient.conf";

    // Debug
    public String debugConf = "emobase_live4.conf";

}
