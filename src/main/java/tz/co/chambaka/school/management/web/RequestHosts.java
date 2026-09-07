package tz.co.chambaka.school.management.web;

public final class RequestHosts {

    private RequestHosts() {
    }

    public static boolean isLoopback(String host) {
        String name = hostname(host);
        return "localhost".equals(name)
                || "127.0.0.1".equals(name)
                || "0.0.0.0".equals(name)
                || "::1".equals(name);
    }

    public static String hostname(String host) {
        if (host == null || host.isBlank()) {
            return "";
        }
        String value = host.strip().toLowerCase();
        if (value.startsWith("[")) {
            int end = value.indexOf(']');
            return end > 1 ? value.substring(1, end) : value;
        }
        int firstColon = value.indexOf(':');
        int lastColon = value.lastIndexOf(':');
        if (firstColon > 0 && firstColon == lastColon) {
            return value.substring(0, firstColon);
        }
        return value;
    }
}
