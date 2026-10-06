package ua.horuktaras.osbb.bot.moderation;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LinkDetector {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)\\b(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern TELEGRAM_INVITE_PATTERN = Pattern.compile(
            "(?i)(t\\.me/\\+[\\w\\-]+|t\\.me/joinchat/[\\w\\-]+|telegram\\.me/joinchat/[\\w\\-]+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern RAW_DOMAIN_PATTERN = Pattern.compile(
            "(?i)\\b([\\w\\-]+\\.(?:com|org|net|io|co|uk|de|fr|ua|ru|info|biz|me|app|dev|xyz|online|site|club|shop|store))(?:[/?][\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]*)?\\b",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Checks if the given text contains any blocked link.
     *
     * @param text           the message text
     * @param allowedDomains set of domain names that are permitted (null or empty = block all links)
     * @return true if a blocked link is present
     */
    public boolean containsBlockedLink(String text, Set<String> allowedDomains) {
        if (text == null || text.isBlank()) return false;

        List<String> links = extractLinks(text);
        if (links.isEmpty()) return false;

        for (String link : links) {
            String domain = extractDomain(link);
            if (domain == null) continue;
            if (allowedDomains != null && !allowedDomains.isEmpty()) {
                boolean allowed = allowedDomains.stream()
                        .anyMatch(d -> domain.equalsIgnoreCase(d) || domain.toLowerCase().endsWith("." + d.toLowerCase()));
                if (!allowed) return true;
            } else {
                return true;
            }
        }
        return false;
    }

    public List<String> extractLinks(String text) {
        if (text == null || text.isBlank()) return List.of();

        List<String> links = new ArrayList<>();
        Matcher urlMatcher = URL_PATTERN.matcher(text);
        while (urlMatcher.find()) {
            links.add(urlMatcher.group(1));
        }

        Matcher inviteMatcher = TELEGRAM_INVITE_PATTERN.matcher(text);
        while (inviteMatcher.find()) {
            String match = inviteMatcher.group(1);
            if (!match.startsWith("http")) {
                links.add("https://" + match);
            }
        }

        Matcher domainMatcher = RAW_DOMAIN_PATTERN.matcher(text);
        while (domainMatcher.find()) {
            String match = domainMatcher.group(0);
            if (links.stream().noneMatch(l -> l.contains(match))) {
                links.add("http://" + match);
            }
        }

        return links;
    }

    public boolean isTelegramInviteLink(String text) {
        if (text == null || text.isBlank()) return false;
        return TELEGRAM_INVITE_PATTERN.matcher(text).find();
    }

    private String extractDomain(String url) {
        try {
            String withScheme = url.startsWith("http") ? url : "https://" + url;
            java.net.URI uri = java.net.URI.create(withScheme);
            String host = uri.getHost();
            if (host == null) return null;
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception e) {
            return null;
        }
    }
}
