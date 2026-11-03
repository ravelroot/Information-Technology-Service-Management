package io.talos.domain.service;

import io.talos.domain.model.itil.ItilAsset;
import io.talos.domain.model.itil.ItilFollowup;
import io.talos.domain.model.itil.ItilItem;
import io.talos.domain.model.itil.ItilObserver;
import io.talos.domain.model.itil.ItilRelation;
import io.talos.domain.model.itil.ItilStatus;
import io.talos.domain.model.itil.ItilTask;
import io.talos.domain.model.itil.ItilType;
import io.talos.domain.model.itil.RelationType;
import io.talos.domain.query.Filter;
import io.talos.domain.query.Query;
import io.talos.domain.query.Sort;
import io.talos.domain.security.Right;
import io.talos.domain.support.BusinessException;
import io.talos.domain.support.Texts;
import java.text.MessageFormat;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class ItilBulkService extends AbstractService {

    public static final int MAX_ITEMS = 500;

    private static final Set<String> STOP_WORDS = Set.of("the", "and", "for", "with", "not", "nao", "com", "para", "por",
            "uma", "dos", "das", "que", "sem", "meu", "minha", "nos", "nas", "pelo", "pela", "esta", "este", "está",
            "isso", "from", "into", "cannot", "can't", "does", "doesn't", "after", "when");

    public record Failure(Long id, RuntimeException error) {
    }

    public record BulkResult(int succeeded, List<Failure> failures) {

        public int total() {
            return succeeded + failures.size();
        }

        public boolean allSucceeded() {
            return failures.isEmpty();
        }
    }

    public record Similar(ItilType type, Long id, String reference, String title, ItilStatus status, double score) {
    }

    public ItilBulkService(Services services) {
        super(services);
    }

    @SuppressWarnings("unchecked")
    private ItilService<ItilItem> svc(ItilType type) {
        return (ItilService<ItilItem>) services.itil(type);
    }

    public BulkResult assign(ItilType type, Collection<Long> ids, Long userId, Long groupId) {
        require(svc(type).module(), Right.ASSIGN);
        if (userId == null && groupId == null) {
            return new BulkResult(0, List.of());
        }
        return each(ids, id -> {
            ItilItem item = svc(type).get(id);
            svc(type).assign(id, userId != null ? userId : item.getAssigneeId(),
                    groupId != null ? groupId : item.getAssigneeGroupId());
        });
    }

    public BulkResult changeStatus(ItilType type, Collection<Long> ids, ItilStatus status) {
        require(svc(type).module(), Right.UPDATE);
        if (status == null) {
            throw new BusinessException("error.invalidStatus", "—");
        }
        return each(ids, id -> {
            ItilItem item = svc(type).get(id);
            if (item.getStatus() != status) {
                svc(type).changeStatus(id, status);
            }
        });
    }

    public BulkResult changeCategory(ItilType type, Collection<Long> ids, Long categoryId) {
        require(svc(type).module(), Right.UPDATE);
        return each(ids, id -> {
            ItilItem item = svc(type).get(id);
            if (!Objects.equals(item.getCategoryId(), categoryId)) {
                item.setCategoryId(categoryId);
                svc(type).save(item);
            }
        });
    }

    public BulkResult changeUrgencyImpact(ItilType type, Collection<Long> ids, Integer urgency, Integer impact) {
        require(svc(type).module(), Right.UPDATE);
        if (urgency == null && impact == null) {
            return new BulkResult(0, List.of());
        }
        return each(ids, id -> {
            ItilItem item = svc(type).get(id);
            if (urgency != null) {
                item.setUrgency(Math.max(1, Math.min(5, urgency)));
            }
            if (impact != null) {
                item.setImpact(Math.max(1, Math.min(5, impact)));
            }
            svc(type).save(item);
        });
    }

    public BulkResult addFollowup(ItilType type, Collection<Long> ids, String content, boolean privateNote) {
        require(svc(type).module(), Right.UPDATE);
        if (Texts.isBlank(Texts.clean(content))) {
            throw new BusinessException("error.contentRequired");
        }
        return each(ids, id -> services.itilActivity().addFollowup(type, id, content, privateNote));
    }

    public BulkResult trash(ItilType type, Collection<Long> ids) {
        require(svc(type).module(), Right.DELETE);
        return each(ids, id -> svc(type).trash(id));
    }

    public BulkResult restore(ItilType type, Collection<Long> ids) {
        require(svc(type).module(), Right.DELETE);
        return each(ids, id -> svc(type).restore(id));
    }

    public int merge(ItilType type, Long targetId, Collection<Long> sourceIds, Function<String, String> labels) {
        require(svc(type).module(), Right.UPDATE);
        Function<String, String> text = labels == null ? ItilBulkService::fallbackLabel : labels;
        List<Long> sources = new ArrayList<>(new LinkedHashSet<>(sourceIds == null ? List.<Long>of() : sourceIds));
        sources.removeIf(id -> id == null || id.equals(targetId));
        if (sources.isEmpty()) {
            throw new BusinessException("error.mergeNothing");
        }
        if (sources.size() > MAX_ITEMS) {
            throw new BusinessException("error.tooManyItems", MAX_ITEMS);
        }
        return tx(() -> {
            ItilItem target = svc(type).get(targetId);
            if (target.isDeleted()) {
                throw new BusinessException("error.mergeTrashed", target.reference());
            }
            if (target.getStatus().isTerminal()) {
                throw new BusinessException("error.itemClosed");
            }
            List<ItilItem> items = new ArrayList<>();
            for (Long id : sources) {
                ItilItem source = svc(type).get(id);
                if (source.isDeleted()) {
                    throw new BusinessException("error.mergeTrashed", source.reference());
                }
                if (source.getStatus().isTerminal()) {
                    throw new BusinessException("error.mergeClosed", source.reference());
                }
                items.add(source);
            }
            Set<Long> people = new HashSet<>();
            Set<Long> groups = new HashSet<>();
            for (ItilObserver o : repo(ItilObserver.class).find(own(type, targetId))) {
                if (o.getUserId() != null) {
                    people.add(o.getUserId());
                } else if (o.getGroupId() != null) {
                    groups.add(o.getGroupId());
                }
            }
            addIfPresent(people, target.getRequesterId());
            addIfPresent(people, target.getAssigneeId());
            Set<Long> assets = new HashSet<>();
            repo(ItilAsset.class).find(own(type, targetId)).forEach(a -> assets.add(a.getAssetId()));
            for (ItilItem source : items) {
                mergeInto(type, target, source, people, groups, assets, text);
            }
            int minutes = items.stream().mapToInt(i -> i.getActionTimeMinutes() == null ? 0 : i.getActionTimeMinutes()).sum();
            ItilItem fresh = svc(type).reload(targetId);
            fresh.setActionTimeMinutes((fresh.getActionTimeMinutes() == null ? 0 : fresh.getActionTimeMinutes()) + minutes);
            String refs = String.join(", ", items.stream().map(ItilItem::reference).toList());
            ItilFollowup summary = followup(type, targetId, MessageFormat.format(text.apply("itil.merge.targetNote").replace("'", "''"), refs), false);
            insert(summary);
            svc(type).touch(fresh);
            return items.size();
        });
    }

    private void mergeInto(ItilType type, ItilItem target, ItilItem source, Set<Long> people, Set<Long> groups,
                           Set<Long> assets, Function<String, String> text) {
        Long sourceId = source.getId();
        List<ItilFollowup> followups = repo(ItilFollowup.class).find(Query.where(own(type, sourceId)).orderBy(Sort.asc("id")));
        String header = MessageFormat.format(text.apply("itil.merge.copiedFrom").replace("'", "''"), source.reference(), source.getTitle());
        String description = Texts.isBlank(source.getContent()) ? header : header + "\n\n" + source.getContent();
        ItilFollowup intro = followup(type, target.getId(), description, false);
        intro.setAuthorId(source.getRequesterId() == null ? currentUserId() : source.getRequesterId());
        insert(intro);
        for (ItilFollowup f : followups) {
            ItilFollowup c = followup(type, target.getId(), "[" + source.reference() + "] " + f.getContent(), f.isPrivateNote());
            c.setAuthorId(f.getAuthorId());
            c.setSourceId(f.getSourceId());
            insert(c);
        }
        for (ItilTask t : repo(ItilTask.class).find(Query.where(own(type, sourceId)).orderBy(Sort.asc("id")))) {
            t.setItemId(target.getId());
            t.setContent(Texts.abbreviate("[" + source.reference() + "] " + t.getContent(), 60_000));
            update(t);
        }
        for (ItilObserver o : repo(ItilObserver.class).find(own(type, sourceId))) {
            if (o.getUserId() != null && people.add(o.getUserId())) {
                addFollower(type, target.getId(), o.getUserId(), null);
            } else if (o.getUserId() == null && o.getGroupId() != null && groups.add(o.getGroupId())) {
                addFollower(type, target.getId(), null, o.getGroupId());
            }
        }
        for (Long u : new Long[] {source.getRequesterId(), source.getAssigneeId()}) {
            if (u != null && people.add(u)) {
                addFollower(type, target.getId(), u, null);
            }
        }
        for (ItilAsset a : repo(ItilAsset.class).find(own(type, sourceId))) {
            if (assets.add(a.getAssetId())) {
                ItilAsset c = new ItilAsset();
                c.setItemType(type);
                c.setItemId(target.getId());
                c.setAssetId(a.getAssetId());
                insert(c);
            }
        }
        boolean linked = repo(ItilRelation.class).exists(Filter.or(
                Filter.and(Filter.eq("sourceType", type), Filter.eq("sourceId", sourceId),
                        Filter.eq("targetType", type), Filter.eq("targetId", target.getId())),
                Filter.and(Filter.eq("sourceType", type), Filter.eq("sourceId", target.getId()),
                        Filter.eq("targetType", type), Filter.eq("targetId", sourceId))));
        if (!linked) {
            ItilRelation r = new ItilRelation();
            r.setSourceType(type);
            r.setSourceId(sourceId);
            r.setTargetType(type);
            r.setTargetId(target.getId());
            r.setRelationType(RelationType.DUPLICATES);
            r.setCreatedById(currentUserId());
            insert(r);
        }
        insert(followup(type, sourceId, MessageFormat.format(text.apply("itil.merge.sourceNote").replace("'", "''"), target.reference(),
                target.getTitle()), false));
        ItilItem closing = svc(type).reload(sourceId);
        closing.setActionTimeMinutes(0);
        svc(type).closeAsDuplicate(closing);
    }

    public List<Similar> similar(ItilType type, String title, Long excludeId, int limit) {
        if (!can(svc(type).module(), Right.READ)) {
            return List.of();
        }
        Set<String> wanted = tokens(title);
        if (wanted.isEmpty()) {
            return List.of();
        }
        List<String> probes = wanted.stream().sorted(Comparator.comparingInt(String::length).reversed()).limit(4).toList();
        Map<Long, ItilItem> candidates = new LinkedHashMap<>();
        for (String probe : probes) {
            for (ItilItem item : svc(type).search(ItilSearch.create().view(ItilSearch.View.OPEN).text(probe).page(0, 25)).items()) {
                candidates.putIfAbsent(item.getId(), item);
            }
        }
        List<Similar> result = new ArrayList<>();
        for (ItilItem item : candidates.values()) {
            if (Objects.equals(item.getId(), excludeId)) {
                continue;
            }
            double score = jaccard(wanted, tokens(item.getTitle()));
            if (score >= 0.3) {
                result.add(new Similar(type, item.getId(), item.reference(), item.getTitle(), item.getStatus(), score));
            }
        }
        result.sort(Comparator.comparingDouble(Similar::score).reversed().thenComparing(Similar::id, Comparator.reverseOrder()));
        return result.size() > Math.max(1, limit) ? List.copyOf(result.subList(0, Math.max(1, limit))) : List.copyOf(result);
    }

    static Set<String> tokens(String text) {
        Set<String> out = new LinkedHashSet<>();
        if (text == null) {
            return out;
        }
        String plain = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        for (String part : plain.split("[^\\p{L}\\p{N}]+")) {
            if (part.length() >= 3 && !STOP_WORDS.contains(part)) {
                out.add(part);
            }
        }
        return out;
    }

    static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        int common = 0;
        for (String s : a) {
            if (b.contains(s)) {
                common++;
            }
        }
        return (double) common / (a.size() + b.size() - common);
    }

    private BulkResult each(Collection<Long> ids, Consumer<Long> action) {
        List<Long> list = new ArrayList<>(new LinkedHashSet<>(ids == null ? List.<Long>of() : ids));
        list.removeIf(Objects::isNull);
        if (list.size() > MAX_ITEMS) {
            throw new BusinessException("error.tooManyItems", MAX_ITEMS);
        }
        int ok = 0;
        List<Failure> failures = new ArrayList<>();
        for (Long id : list) {
            try {
                action.accept(id);
                ok++;
            } catch (RuntimeException e) {
                failures.add(new Failure(id, e));
            }
        }
        return new BulkResult(ok, List.copyOf(failures));
    }

    private static Filter own(ItilType type, Long id) {
        return Filter.and(Filter.eq("itemType", type), Filter.eq("itemId", id));
    }

    private static void addIfPresent(Set<Long> set, Long id) {
        if (id != null) {
            set.add(id);
        }
    }

    private ItilFollowup followup(ItilType type, Long itemId, String content, boolean privateNote) {
        ItilFollowup f = new ItilFollowup();
        f.setItemType(type);
        f.setItemId(itemId);
        f.setAuthorId(currentUserId());
        f.setContent(Texts.abbreviate(content, 60_000));
        f.setPrivateNote(privateNote);
        return f;
    }

    private void addFollower(ItilType type, Long itemId, Long userId, Long groupId) {
        ItilObserver o = new ItilObserver();
        o.setItemType(type);
        o.setItemId(itemId);
        o.setUserId(userId);
        o.setGroupId(groupId);
        o.setActorRole(io.talos.domain.model.itil.ItilActorRole.FOLLOWER);
        insert(o);
    }

    static String fallbackLabel(String key) {
        return switch (key) {
            case "itil.merge.targetNote" -> "Merged into this item: {0}.";
            case "itil.merge.sourceNote" -> "Merged into {0} ({1}). Follow the conversation there.";
            case "itil.merge.copiedFrom" -> "From {0}: {1}";
            default -> key;
        };
    }
}
