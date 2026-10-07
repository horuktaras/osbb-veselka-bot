package ua.horuktaras.osbb.bot.model.enums;

import java.util.List;

public enum RequestStatus {

    NEW("🆕 Нова"),
    PROCESSED("✅ Опрацьована"),
    IN_PROGRESS_OSBB("🔧 В роботі ОСББ"),
    IN_PROGRESS_CONTRACTOR("🏢 В роботі підрядника"),
    VERIFICATION("🔍 Перевірка"),
    CLOSED("🔒 Закрита");

    private final String displayName;

    RequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<RequestStatus> nextStatuses() {
        return switch (this) {
            case NEW -> List.of(PROCESSED);
            case PROCESSED -> List.of(IN_PROGRESS_OSBB, IN_PROGRESS_CONTRACTOR);
            case IN_PROGRESS_OSBB -> List.of(VERIFICATION, CLOSED);
            case IN_PROGRESS_CONTRACTOR -> List.of(VERIFICATION, CLOSED);
            case VERIFICATION -> List.of(IN_PROGRESS_OSBB, IN_PROGRESS_CONTRACTOR, CLOSED);
            case CLOSED -> List.of();
        };
    }
}
