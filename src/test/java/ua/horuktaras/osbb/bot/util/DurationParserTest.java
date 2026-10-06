package ua.horuktaras.osbb.bot.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class DurationParserTest {

    @Test
    void parse30m() {
        Duration result = DurationParser.parse("30m");
        assertEquals(Duration.ofMinutes(30), result);
    }

    @Test
    void parse2h() {
        Duration result = DurationParser.parse("2h");
        assertEquals(Duration.ofHours(2), result);
    }

    @Test
    void parse1d() {
        Duration result = DurationParser.parse("1d");
        assertEquals(Duration.ofDays(1), result);
    }

    @Test
    void parse7d() {
        Duration result = DurationParser.parse("7d");
        assertEquals(Duration.ofDays(7), result);
    }

    @Test
    void parse60s() {
        Duration result = DurationParser.parse("60s");
        assertEquals(Duration.ofSeconds(60), result);
    }

    @Test
    void parseUppercaseM() {
        Duration result = DurationParser.parse("15M");
        assertEquals(Duration.ofMinutes(15), result);
    }

    @Test
    void invalidFormatThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("abc"));
    }

    @Test
    void invalidUnitThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("10x"));
    }

    @Test
    void emptyStringThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse(""));
    }

    @Test
    void nullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse(null));
    }

    @Test
    void parseWithWhitespace() {
        Duration result = DurationParser.parse("  5h  ");
        assertEquals(Duration.ofHours(5), result);
    }
}
