package com.example.urlshortener.agentic.tools;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.example.urlshortener.agentic.AgentTool;
import com.example.urlshortener.agentic.RiskLevel;
import com.example.urlshortener.agentic.ToolObservation;

/**
 * Heuristic host risk checks without fetching the destination (SSRF-safe).
 * Literal private/loopback IPs and known metadata hostnames are flagged; no DNS lookup.
 */
@Component
public class HostRiskTool implements AgentTool {

    private static final Pattern IPV4 = Pattern.compile(
            "^(?:\\d{1,3}\\.){3}\\d{1,3}$");
    private static final Pattern IPV6_HINT = Pattern.compile(".*:.*");

    private static final Set<String> SUSPICIOUS_HOSTS = Set.of(
            "localhost",
            "metadata.google.internal",
            "metadata",
            "169.254.169.254");

    @Override
    public String name() {
        return "host_risk";
    }

    @Override
    public ToolObservation observe(String destinationUrl) {
        try {
            URI uri = URI.create(destinationUrl.trim());
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return new ToolObservation(name(), RiskLevel.HIGH, "missing host");
            }
            String normalized = host.toLowerCase(Locale.ROOT);
            if (SUSPICIOUS_HOSTS.contains(normalized) || normalized.endsWith(".localhost")) {
                return new ToolObservation(name(), RiskLevel.HIGH, "restricted host");
            }
            if (isLiteralIp(normalized) && isPrivateOrLoopbackLiteral(normalized)) {
                return new ToolObservation(name(), RiskLevel.HIGH, "private or loopback address");
            }
            if (normalized.endsWith(".zip") || normalized.endsWith(".mov")) {
                return new ToolObservation(name(), RiskLevel.MEDIUM, "potentially confusing TLD");
            }
            return new ToolObservation(name(), RiskLevel.LOW, "host accepted");
        } catch (IllegalArgumentException ex) {
            return new ToolObservation(name(), RiskLevel.HIGH, "unparseable host");
        }
    }

    private static boolean isLiteralIp(String host) {
        return IPV4.matcher(host).matches() || IPV6_HINT.matcher(host).matches();
    }

    private static boolean isPrivateOrLoopbackLiteral(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress();
        } catch (UnknownHostException ex) {
            return false;
        }
    }
}
