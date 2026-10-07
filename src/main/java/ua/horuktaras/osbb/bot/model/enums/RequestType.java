package ua.horuktaras.osbb.bot.model.enums;

public enum RequestType {

    BREAKAGE("🔧 Поломка"),
    COMPLAINT("📣 Скарга"),
    SERVICE("🛠️ Послуга");

    private final String displayName;

    RequestType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
