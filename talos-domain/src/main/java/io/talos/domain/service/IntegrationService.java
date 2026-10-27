package io.talos.domain.service;

import io.talos.domain.model.core.Module;
import io.talos.domain.model.itil.ItilItem;
import io.talos.domain.security.Right;
import io.talos.domain.support.BusinessException;
import io.talos.domain.support.ValidationException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Integrações de saída: envia os eventos do atendimento para Slack, Microsoft Teams, Discord,
 * Google Chat ou qualquer endpoint HTTP que aceite JSON. A configuração fica na tabela de
 * configurações (uma chave por webhook), o envio é assíncrono e nunca interrompe a operação
 * que originou o evento.
 */
public class IntegrationService extends AbstractService {

    public enum Kind { SLACK, TEAMS, DISCORD, GOOGLE_CHAT, GENERIC }

    public enum Event {
        ITEM_CREATED("item.created"),
        ITEM_ASSIGNED("item.assigned"),
        ITEM_STATUS("item.status_changed"),
        ITEM_SOLVED("item.solved"),
        EXPIRATION_ALERT("inventory.expiration_alert"),
        TEST("integration.test");

        private final String code;

        Event(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }

        public static List<Event> selectable() {
            return List.of(ITEM_CREATED, ITEM_ASSIGNED, ITEM_STATUS, ITEM_SOLVED, EXPIRATION_ALERT);
        }
    }

    public record Webhook(String id, String name, Kind kind, String url, String secret, boolean enabled, Set<Event> events) {
        public Webhook {
            events = events == null || events.isEmpty() ? EnumSet.noneOf(Event.class) : EnumSet.copyOf(events);
        }

        public static Webhook blank() {
            return new Webhook(null, "", Kind.SLACK, "", "", true, EnumSet.of(Event.ITEM_CREATED, Event.ITEM_SOLVED));
        }
    }

    public record Payload(Event event, String itemType, Long itemId, String reference, String title, String status,
                          Integer priority, String actor, String detail) {
    }

    public record Delivery(LocalDateTime at, String webhookId, String webhookName, Event event, int httpStatus, boolean ok,
                           String detail) {
    }

    /** Transporte HTTP substituível em testes. */
    @FunctionalInterface
    public interface Transport {
        int post(URI uri, String body, Map<String, String> headers) throws Exception;
    }

    static final String INDEX_KEY = "integration.webhooks";
    static final String PREFIX = "integration.webhook.";
    private static final int MAX_WEBHOOKS = 20;
    private static final int MAX_LOG = 60;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final Deque<Delivery> log = new ArrayDeque<>();
    private volatile Transport transport;
    private volatile Function<String, String> labels = key -> key.substring(key.lastIndexOf('.') + 1);
    private volatile ExecutorService executor;

    public IntegrationService(Services services) {
        super(services);
    }

    /** Textos legíveis (traduções) usados nas mensagens; a interface registra o seu dicionário. */
    public void setLabels(Function<String, String> labels) {
        this.labels = labels == null ? this.labels : labels;
    }

    public void setTransport(Transport transport) {
        this.transport = transport;
    }

    // ---------------------------------------------------------------- configuração

    public List<Webhook> webhooks() {
        List<Webhook> list = new ArrayList<>();
        for (String id : ids()) {
            String raw = services.settings().get(PREFIX + id, null);
            if (raw != null && !raw.isBlank()) {
                list.add(decode(id, raw));
            }
        }
        return list;
    }

    public Webhook save(Webhook w) {
        require(Module.SETTINGS, Right.UPDATE);
        Webhook clean = validate(w);
        List<String> ids = new ArrayList<>(ids());
        String id = clean.id() == null || clean.id().isBlank() ? UUID.randomUUID().toString().substring(0, 8) : clean.id();
        if (!ids.contains(id)) {
            if (ids.size() >= MAX_WEBHOOKS) {
                throw new BusinessException("error.integrationLimit", MAX_WEBHOOKS);
            }
            ids.add(id);
        }
        Webhook saved = new Webhook(id, clean.name(), clean.kind(), clean.url(), clean.secret(), clean.enabled(), clean.events());
        services.settings().set(PREFIX + id, encode(saved));
        services.settings().set(INDEX_KEY, String.join(",", ids));
        return saved;
    }

    public void delete(String id) {
        require(Module.SETTINGS, Right.UPDATE);
        List<String> ids = new ArrayList<>(ids());
        if (ids.remove(id)) {
            services.settings().set(PREFIX + id, "");
            services.settings().set(INDEX_KEY, String.join(",", ids));
        }
    }

    public void setEnabled(String id, boolean enabled) {
        webhooks().stream().filter(w -> w.id().equals(id)).findFirst().ifPresent(w ->
                save(new Webhook(w.id(), w.name(), w.kind(), w.url(), w.secret(), enabled, w.events())));
    }

    private List<String> ids() {
        String raw = services.settings().get(INDEX_KEY, "");
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
    }

    static Webhook validate(Webhook w) {
        Map<String, String> errors = new LinkedHashMap<>();
        String name = w.name() == null ? "" : w.name().trim();
        if (name.isEmpty()) {
            errors.put("name", "validation.required");
        } else if (name.length() > 80) {
            errors.put("name", "validation.tooLong");
        }
        String url = w.url() == null ? "" : w.url().trim();
        if (url.isEmpty()) {
            errors.put("url", "validation.required");
        } else if (!isAllowedUrl(url)) {
            errors.put("url", "validation.webhookUrl");
        } else if (url.length() > 1200) {
            errors.put("url", "validation.tooLong");
        }
        if (w.events().isEmpty()) {
            errors.put("events", "validation.required");
        }
        String secret = w.secret() == null ? "" : w.secret().trim();
        if (secret.length() > 200) {
            errors.put("secret", "validation.tooLong");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return new Webhook(w.id(), name, w.kind() == null ? Kind.GENERIC : w.kind(), url, secret, w.enabled(), w.events());
    }

    /** Somente HTTPS; HTTP é aceito apenas para a própria máquina (testes locais). */
    static boolean isAllowedUrl(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost();
            if (host == null || host.isBlank() || uri.getUserInfo() != null) {
                return false;
            }
            if (scheme.equals("https")) {
                return true;
            }
            return scheme.equals("http") && (host.equals("localhost") || host.equals("127.0.0.1") || host.equals("[::1]"));
        } catch (Exception e) {
            return false;
        }
    }

    static String encode(Webhook w) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("name", w.name());
        m.put("kind", w.kind().name());
        m.put("url", w.url());
        m.put("secret", w.secret() == null ? "" : w.secret());
        m.put("enabled", String.valueOf(w.enabled()));
        m.put("events", w.events().stream().map(Enum::name).collect(Collectors.joining(",")));
        return m.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    static Webhook decode(String id, String raw) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String part : raw.split("&")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                m.put(part.substring(0, eq), URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }
        Kind kind;
        try {
            kind = Kind.valueOf(m.getOrDefault("kind", "GENERIC"));
        } catch (IllegalArgumentException e) {
            kind = Kind.GENERIC;
        }
        Set<Event> events = EnumSet.noneOf(Event.class);
        for (String e : m.getOrDefault("events", "").split(",")) {
            try {
                if (!e.isBlank()) {
                    events.add(Event.valueOf(e.trim()));
                }
            } catch (IllegalArgumentException ignored) {
                // evento de uma versão mais nova: ignora
            }
        }
        return new Webhook(id, m.getOrDefault("name", id), kind, m.getOrDefault("url", ""), m.getOrDefault("secret", ""),
                Boolean.parseBoolean(m.getOrDefault("enabled", "true")), events);
    }

    // ---------------------------------------------------------------- envio

    /** Publica um evento de chamado, problema ou mudança. */
    public void publishItem(Event event, ItilItem item) {
        if (item == null) {
            return;
        }
        String actor = security().isAuthenticated() ? security().current().fullName() : null;
        publish(new Payload(event, item.getClass().getSimpleName(), item.getId(), item.reference(), item.getTitle(),
                item.getStatus() == null ? null : item.getStatus().name(), item.getPriority(), actor, null));
    }

    /** Publica o resumo diário de garantias, contratos e licenças a vencer. */
    public void publishExpiration(int warranties, int contracts, int licenses, long overUsed, int days) {
        Function<String, String> l = labels;
        String title = l.apply("integration.expiration.warranties") + ": " + warranties + " · "
                + l.apply("integration.expiration.contracts") + ": " + contracts + " · "
                + l.apply("integration.expiration.licenses") + ": " + licenses
                + (overUsed > 0 ? " · " + l.apply("integration.expiration.overUsed") + ": " + overUsed : "");
        publish(new Payload(Event.EXPIRATION_ALERT, "Inventory", null, "", title, null, null, null,
                l.apply("integration.expiration.window").replace("{0}", String.valueOf(days))));
    }

    /** Envia para todos os webhooks ativos que assinam o evento. Nunca lança exceção. */
    public void publish(Payload payload) {
        List<Webhook> targets;
        try {
            targets = webhooks().stream().filter(Webhook::enabled).filter(w -> w.events().contains(payload.event())).toList();
        } catch (RuntimeException e) {
            return;
        }
        String org = services.settings().get(SettingKeys.ORGANIZATION_NAME, "Talos.");
        LocalDateTime at = now();
        for (Webhook w : targets) {
            String body = body(w.kind(), payload, org, at, labels);
            executor().submit(() -> deliver(w, payload.event(), body, at));
        }
    }

    /** Envia uma mensagem de teste e devolve o resultado (sem exigir que o webhook esteja salvo). */
    public CompletableFuture<Delivery> test(Webhook w) {
        require(Module.SETTINGS, Right.UPDATE);
        Webhook clean = validate(w);
        String actor = security().isAuthenticated() ? security().current().fullName() : null;
        Payload p = new Payload(Event.TEST, "Ticket", 0L, "#0", labels.apply("integration.test.title"), "NEW", 3, actor,
                labels.apply("integration.test.detail"));
        String org = services.settings().get(SettingKeys.ORGANIZATION_NAME, "Talos.");
        LocalDateTime at = now();
        String body = body(clean.kind(), p, org, at, labels);
        return CompletableFuture.supplyAsync(() -> deliver(clean, Event.TEST, body, at), executor());
    }

    public List<Delivery> deliveries() {
        synchronized (log) {
            return Collections.unmodifiableList(new ArrayList<>(log));
        }
    }

    private Delivery deliver(Webhook w, Event event, String body, LocalDateTime at) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json; charset=utf-8");
        headers.put("User-Agent", "Talos/" + Services.VERSION);
        headers.put("X-Talos-Event", event.code());
        if (w.kind() == Kind.GENERIC && w.secret() != null && !w.secret().isBlank()) {
            headers.put("X-Talos-Signature", "sha256=" + sign(w.secret(), body));
        }
        Delivery d;
        try {
            int status = transport().post(URI.create(w.url()), body, headers);
            boolean ok = status >= 200 && status < 300;
            d = new Delivery(at, w.id(), w.name(), event, status, ok, ok ? "OK" : "HTTP " + status);
        } catch (Exception e) {
            String msg = e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage());
            d = new Delivery(at, w.id(), w.name(), event, 0, false, msg.length() > 160 ? msg.substring(0, 160) : msg);
        }
        synchronized (log) {
            log.addFirst(d);
            while (log.size() > MAX_LOG) {
                log.removeLast();
            }
        }
        return d;
    }

    private Transport transport() {
        Transport t = transport;
        if (t != null) {
            return t;
        }
        HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).followRedirects(HttpClient.Redirect.NEVER).build();
        t = (uri, body, headers) -> {
            HttpRequest.Builder b = HttpRequest.newBuilder(uri).timeout(TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            headers.forEach(b::header);
            return client.send(b.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
        };
        transport = t;
        return t;
    }

    private ExecutorService executor() {
        ExecutorService e = executor;
        if (e == null) {
            synchronized (this) {
                if (executor == null) {
                    executor = Executors.newFixedThreadPool(2, r -> {
                        Thread th = new Thread(r, "talos-integrations");
                        th.setDaemon(true);
                        return th;
                    });
                }
                e = executor;
            }
        }
        return e;
    }

    // ---------------------------------------------------------------- formatos

    static String sign(String secret, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }

    static String headline(Payload p, Function<String, String> labels) {
        String type = labels.apply("type." + p.itemType());
        String what = labels.apply("integration.event." + p.event().name());
        return type + " " + Objects.toString(p.reference(), "") + " — " + what;
    }

    static List<String[]> facts(Payload p, Function<String, String> labels) {
        List<String[]> facts = new ArrayList<>();
        if (p.status() != null) {
            facts.add(new String[] {labels.apply("integration.field.status"), labels.apply("enum.ItilStatus." + p.status())});
        }
        if (p.priority() != null) {
            facts.add(new String[] {labels.apply("integration.field.priority"), labels.apply("priority." + p.priority())});
        }
        if (p.actor() != null && !p.actor().isBlank()) {
            facts.add(new String[] {labels.apply("integration.field.actor"), p.actor()});
        }
        return facts;
    }

    static String body(Kind kind, Payload p, String org, LocalDateTime at, Function<String, String> labels) {
        String headline = headline(p, labels);
        String title = Objects.toString(p.title(), "");
        List<String[]> facts = facts(p, labels);
        String factLine = facts.stream().map(f -> f[0] + ": " + f[1]).collect(Collectors.joining(" · "));
        String detail = p.detail() == null ? "" : p.detail();
        return switch (kind) {
            case SLACK -> json(Map.of("text", "*" + headline + "*\n" + title
                    + (factLine.isEmpty() ? "" : "\n" + factLine) + (detail.isEmpty() ? "" : "\n_" + detail + "_")
                    + "\n_" + org + "_"));
            case GOOGLE_CHAT -> json(Map.of("text", "*" + headline + "*\n" + title
                    + (factLine.isEmpty() ? "" : "\n" + factLine) + (detail.isEmpty() ? "" : "\n" + detail)
                    + "\n_" + org + "_"));
            case DISCORD -> "{\"username\":\"Talos.\",\"embeds\":[{\"title\":" + str(headline)
                    + ",\"description\":" + str(title + (detail.isEmpty() ? "" : "\n" + detail))
                    + ",\"color\":10115882,\"fields\":[" + facts.stream()
                    .map(f -> "{\"name\":" + str(f[0]) + ",\"value\":" + str(f[1]) + ",\"inline\":true}")
                    .collect(Collectors.joining(",")) + "],\"footer\":{\"text\":" + str(org) + "}}]}";
            case TEAMS -> "{\"type\":\"message\",\"attachments\":[{\"contentType\":\"application/vnd.microsoft.card.adaptive\","
                    + "\"content\":{\"$schema\":\"http://adaptivecards.io/schemas/adaptive-card.json\",\"type\":\"AdaptiveCard\","
                    + "\"version\":\"1.4\",\"body\":["
                    + "{\"type\":\"TextBlock\",\"text\":" + str(headline) + ",\"weight\":\"Bolder\",\"size\":\"Medium\",\"wrap\":true},"
                    + "{\"type\":\"TextBlock\",\"text\":" + str(title) + ",\"wrap\":true}"
                    + (detail.isEmpty() ? "" : ",{\"type\":\"TextBlock\",\"text\":" + str(detail) + ",\"wrap\":true,\"isSubtle\":true}")
                    + ",{\"type\":\"FactSet\",\"facts\":[" + facts.stream()
                    .map(f -> "{\"title\":" + str(f[0]) + ",\"value\":" + str(f[1]) + "}")
                    .collect(Collectors.joining(",")) + "]},"
                    + "{\"type\":\"TextBlock\",\"text\":" + str(org) + ",\"isSubtle\":true,\"size\":\"Small\"}]}}]}";
            case GENERIC -> "{\"event\":" + str(p.event().code())
                    + ",\"occurredAt\":" + str(at.atZone(ZoneId.systemDefault()).toOffsetDateTime().toString())
                    + ",\"organization\":" + str(org)
                    + ",\"item\":{\"type\":" + str(p.itemType()) + ",\"id\":" + (p.itemId() == null ? "null" : p.itemId())
                    + ",\"reference\":" + str(p.reference()) + ",\"title\":" + str(p.title())
                    + ",\"status\":" + str(p.status()) + ",\"priority\":" + (p.priority() == null ? "null" : p.priority()) + "}"
                    + ",\"actor\":" + str(p.actor()) + ",\"detail\":" + str(p.detail()) + ",\"text\":" + str(headline + " — " + title)
                    + "}";
        };
    }

    private static String json(Map<String, String> fields) {
        return fields.entrySet().stream().map(e -> str(e.getKey()) + ":" + str(e.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
    }

    static String str(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder b = new StringBuilder(s.length() + 8).append('"');
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
                }
            }
        }
        return b.append('"').toString();
    }
}
