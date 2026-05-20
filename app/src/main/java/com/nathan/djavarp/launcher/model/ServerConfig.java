package com.nathan.djavarp.launcher.model;

/**
 * Static configuration for the Djava Launcher featured server and
 * server-id constants understood by the native side.
 */
public final class ServerConfig {

    /** Featured (default) server IP. */
    public static final String FEATURED_IP = "151.240.0.201";

    /** Featured (default) server port. */
    public static final int FEATURED_PORT = 7798;

    /** Description shown on the featured card if the server is unreachable. */
    public static final String FEATURED_DESCRIPTION =
            "Server resmi Djava Launcher dengan gameplay role play yang imersif, " +
            "ekonomi seimbang, dan komunitas aktif. Bergabung sekarang dan mulai " +
            "petualananmu di kota dengan dunia yang hidup.";

    /** Sentinel serverid value telling native side to read host/port from settings.ini. */
    public static final int CUSTOM_SERVER_ID = 99;

    private ServerConfig() {
    }
}
