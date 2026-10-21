package io.pyhoma.domain.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.pyhoma.domain.model.core.Setting;
import io.pyhoma.domain.port.Repository;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.query.Sort;
import io.pyhoma.domain.support.ConcurrentUpdateException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryRepositoryTest {

    private final InMemoryDataStore store = new InMemoryDataStore();
    private final Repository<Setting> repo = store.repository(Setting.class);

    private Setting setting(String key, String value) {
        Setting s = new Setting();
        s.setSettingKey(key);
        s.setSettingValue(value);
        s.setCreatedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        return repo.save(s);
    }

    @Test
    void filtersSortsAndPages() {
        setting("b", "Ação");
        setting("a", "beta");
        setting("c", null);
        assertEquals(1, repo.count(Filter.like("settingValue", "acao")));
        assertEquals(1, repo.count(Filter.isNull("settingValue")));
        List<Setting> sorted = repo.find(Query.all().orderBy(Sort.asc("settingKey")));
        assertEquals(List.of("a", "b", "c"), sorted.stream().map(Setting::getSettingKey).toList());
        var page = repo.page(Query.all().orderBy(Sort.desc("settingKey")).page(1, 2));
        assertEquals(3, page.total());
        assertEquals("a", page.items().get(0).getSettingKey());
        assertEquals(2, repo.count(Filter.in("settingKey", List.of("a", "c", "x"))));
        assertEquals(0, repo.count(Filter.in("settingKey", List.of())));
        assertEquals(3, repo.count(Filter.or(Filter.eq("settingKey", "a"), Filter.not(Filter.eq("settingKey", "a")))));
    }

    @Test
    void optimisticLocking() {
        Setting s = setting("k", "1");
        Setting copy1 = repo.getById(s.getId());
        Setting copy2 = repo.getById(s.getId());
        copy1.setSettingValue("2");
        repo.save(copy1);
        copy2.setSettingValue("3");
        assertThrows(ConcurrentUpdateException.class, () -> repo.save(copy2));
    }

    @Test
    void transactionRollsBack() {
        setting("keep", "1");
        assertThrows(IllegalStateException.class, () -> store.inTransaction(() -> {
            setting("lost", "2");
            throw new IllegalStateException("boom");
        }));
        assertEquals(1, repo.count(Filter.ALL));
        assertNotNull(repo.findFirst(Filter.eq("settingKey", "keep")).orElse(null));
    }

    @Test
    void countByGroups() {
        setting("a", "x");
        setting("b", "x");
        setting("c", "y");
        var counts = repo.countBy("settingValue", Filter.ALL);
        assertEquals(2L, counts.get("x"));
        assertTrue(counts.containsKey("y"));
    }
}
