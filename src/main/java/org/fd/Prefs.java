package org.fd;

public final class Prefs {

    public static final String PREFIX = "org.fd.aireporter.";

    public static final String DEFAULT_URL = "http://localhost:11434";
    public static final String DEFAULT_MODEL = "gpt-oss:20b";
    public static final String DEFAULT_TEMPERATURE = "0.0";
    public static final String DEFAULT_HTML_ENCODE = "NO";

    public static final String KEY_BASE_URL = PREFIX + "BASE_URL";
    public static final String KEY_MODEL = PREFIX + "MODEL";
    public static final String KEY_API_KEY = PREFIX + "API_KEY";
    public static final String KEY_PROVIDER = PREFIX + "PROVIDER";
    public static final String KEY_TEMPERATURE = PREFIX + "TEMPERATURE";
    public static final String KEY_HTML_ENCODE = PREFIX +  "HTML_ENCODE";

    public static final String KEY_REPORT_DIR = PREFIX + "reportdir";
    public static final String KEY_EXPORT_ENABLED = PREFIX + "export.enabled";
    public static final String KEY_EXPORT_CONFIGURED = PREFIX + "export.configured";
}
