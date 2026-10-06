package ua.horuktaras.osbb.bot.util;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {

    private static final Pattern PATTERN = Pattern.compile("^(\\d+)([smhd])$", Pattern.CASE_INSENSITIVE);

    private DurationParser() {}

    /**
     * Parses duration strings like "30m", "2h", "1d", "7d", "60s".
     *
     * @param input the duration string
     * @return parsed Duration
     * @throws IllegalArgumentException if format is invalid
     */
    public static Duration parse(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Duration string must not be null");
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Duration string must not be empty");
        }

        Matcher matcher = PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Invalid duration format: '" + input + "'. Expected format: <number><unit> where unit is s, m, h, or d. E.g. 30m, 2h, 1d");
        }

        long value = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2).toLowerCase();

        return switch (unit) {
            case "s" -> Duration.ofSeconds(value);
            case "m" -> Duration.ofMinutes(value);
            case "h" -> Duration.ofHours(value);
            case "d" -> Duration.ofDays(value);
            default -> throw new IllegalArgumentException("Unknown time unit: " + unit);
        };
    }
}
