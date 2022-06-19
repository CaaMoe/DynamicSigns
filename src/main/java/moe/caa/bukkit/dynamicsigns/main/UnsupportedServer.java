package moe.caa.bukkit.dynamicsigns.main;

public class UnsupportedServer extends Exception {
    public UnsupportedServer() {
    }

    public UnsupportedServer(String message) {
        super(message);
    }

    public UnsupportedServer(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedServer(Throwable cause) {
        super(cause);
    }

    public UnsupportedServer(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
