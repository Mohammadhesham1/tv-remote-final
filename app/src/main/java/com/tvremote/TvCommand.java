package com.tvremote;

/**
 * TV Remote command protocol.
 * Commands are sent as JSON over UDP port 7777 or TCP port 7778.
 *
 * Format: {"cmd":"<command>","val":<optional_value>}
 */
public class TvCommand {
    // Navigation
    public static final String DPAD_UP    = "dpad_up";
    public static final String DPAD_DOWN  = "dpad_down";
    public static final String DPAD_LEFT  = "dpad_left";
    public static final String DPAD_RIGHT = "dpad_right";
    public static final String DPAD_OK    = "dpad_ok";

    // System
    public static final String POWER      = "power";
    public static final String POWER_OFF  = "power_off";
    public static final String HOME       = "home";
    public static final String BACK       = "back";
    public static final String MENU       = "menu";

    // Volume
    public static final String VOL_UP     = "vol_up";
    public static final String VOL_DOWN   = "vol_down";
    public static final String MUTE       = "mute";
    public static final String VOL_SET    = "vol_set";

    // Channel
    public static final String CH_UP      = "ch_up";
    public static final String CH_DOWN    = "ch_down";
    public static final String CH_NUM     = "ch_num";

    // Media
    public static final String PLAY_PAUSE = "play_pause";
    public static final String STOP       = "stop";
    public static final String REWIND     = "rewind";
    public static final String FAST_FWD   = "fast_fwd";
    public static final String PREV       = "prev";
    public static final String NEXT       = "next";

    // Apps
    public static final String OPEN_APP   = "open_app";
    public static final String TV_INPUT   = "tv_input";

    // Keyboard
    public static final String TEXT_INPUT = "text_input";
    public static final String VOICE_CMD  = "voice_cmd";

    // Color buttons (for smart TV menus)
    public static final String BTN_RED    = "btn_red";
    public static final String BTN_GREEN  = "btn_green";
    public static final String BTN_YELLOW = "btn_yellow";
    public static final String BTN_BLUE   = "btn_blue";

    public static String build(String cmd) {
        return "{\"cmd\":\"" + cmd + "\"}";
    }

    public static String build(String cmd, String val) {
        return "{\"cmd\":\"" + cmd + "\",\"val\":\"" + val + "\"}";
    }

    public static String build(String cmd, int val) {
        return "{\"cmd\":\"" + cmd + "\",\"val\":" + val + "}";
    }
}
