package ua.horuktaras.osbb.bot.moderation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LinkDetectorTest {

    private LinkDetector linkDetector;

    @BeforeEach
    void setUp() {
        linkDetector = new LinkDetector();
    }

    @Test
    void detectsHttpLink() {
        assertTrue(linkDetector.containsBlockedLink("Check this out http://example.com", Set.of()));
    }

    @Test
    void detectsHttpsLink() {
        assertTrue(linkDetector.containsBlockedLink("Visit https://example.com for more info", Set.of()));
    }

    @Test
    void detectsTelegramInviteLink() {
        assertTrue(linkDetector.isTelegramInviteLink("Join us at t.me/+abc123xyz"));
    }

    @Test
    void detectsTelegramJoinChatLink() {
        assertTrue(linkDetector.isTelegramInviteLink("t.me/joinchat/ABCDEFG"));
    }

    @Test
    void doesNotFlagAllowedDomain() {
        assertFalse(linkDetector.containsBlockedLink(
                "Check https://allowed.com for details",
                Set.of("allowed.com")));
    }

    @Test
    void flagsNonAllowedDomainWhenAllowedListPresent() {
        assertTrue(linkDetector.containsBlockedLink(
                "Check https://blocked.com for details",
                Set.of("allowed.com")));
    }

    @Test
    void doesNotFlagPlainText() {
        assertFalse(linkDetector.containsBlockedLink("Hello, how are you today?", Set.of()));
    }

    @Test
    void nullTextReturnsFalse() {
        assertFalse(linkDetector.containsBlockedLink(null, Set.of()));
    }

    @Test
    void blankTextReturnsFalse() {
        assertFalse(linkDetector.containsBlockedLink("   ", Set.of()));
    }

    @Test
    void extractLinksFindsHttpsLink() {
        var links = linkDetector.extractLinks("Visit https://example.com now");
        assertFalse(links.isEmpty());
        assertTrue(links.stream().anyMatch(l -> l.contains("example.com")));
    }

    @Test
    void nullAllowedDomainsBlocksAllLinks() {
        assertTrue(linkDetector.containsBlockedLink("https://anysite.org", null));
    }
}
