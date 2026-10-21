package io.pyhoma.infrastructure.persistence;

import io.pyhoma.domain.model.BaseEntity;
import io.pyhoma.domain.port.Repository;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.PageResult;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.query.Sort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class JpaRepositoryAdapter<T extends BaseEntity> implements Repository<T> {

    private final Class<T> type;
    private final EntityManager em;

    JpaRepositoryAdapter(Class<T> type, EntityManager em) {
        this.type = type;
        this.em = em;
    }

    @Override
    public Class<T> entityType() {
        return type;
    }

    @Override
    public Optional<T> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        T entity = em.find(type, id);
        if (entity != null) {
            em.detach(entity);
        }
        return Optional.ofNullable(entity);
    }

    @Override
    public List<T> findAllById(Collection<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return List.of();
        }
        return find(Query.where(Filter.in("id", distinct)));
    }

    @Override
    public List<T> find(Query query) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(type);
        Root<T> root = cq.from(type);
        cq.select(root).where(PredicateBuilder.build(cb, root, query.filter()));
        List<Order> orders = new ArrayList<>();
        for (Sort s : query.sorts()) {
            orders.add(s.ascending() ? cb.asc(root.get(s.field())) : cb.desc(root.get(s.field())));
        }
        if (query.sorts().stream().noneMatch(s -> "id".equals(s.field()))) {
            orders.add(cb.asc(root.get("id")));
        }
        cq.orderBy(orders);
        TypedQuery<T> tq = em.createQuery(cq);
        if (query.offset() > 0) {
            tq.setFirstResult(query.offset());
        }
        if (query.isPaged()) {
            tq.setMaxResults(query.limit());
        }
        List<T> result = tq.getResultList();
        result.forEach(em::detach);
        return result;
    }

    @Override
    public PageResult<T> page(Query query) {
        long total = count(query.filter());
        List<T> items = total == 0 ? List.of() : find(query);
        return new PageResult<>(items, total, query.offset(), query.limit());
    }

    @Override
    public long count(Filter filter) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<T> root = cq.from(type);
        cq.select(cb.count(root)).where(PredicateBuilder.build(cb, root, filter));
        return em.createQuery(cq).getSingleResult();
    }

    @Override
    public Map<Object, Long> countBy(String field, Filter filter) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<T> root = cq.from(type);
        cq.select(cb.array(root.get(field), cb.count(root)))
                .where(PredicateBuilder.build(cb, root, filter))
                .groupBy(root.get(field));
        Map<Object, Long> result = new LinkedHashMap<>();
        for (Object[] row : em.createQuery(cq).getResultList()) {
            result.put(row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    @Override
    public T save(T entity) {
        T managed;
        if (entity.getId() == null) {
            em.persist(entity);
            managed = entity;
        } else {
            managed = em.merge(entity);
        }
        em.flush();
        em.detach(managed);
        return managed;
    }

    @Override
    public void delete(T entity) {
        if (entity.getId() == null) {
            return;
        }
        T managed = em.find(type, entity.getId());
        if (managed != null) {
            em.remove(managed);
            em.flush();
        }
    }

    @Override
    public int deleteWhere(Filter filter) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaDelete<T> cd = cb.createCriteriaDelete(type);
        Root<T> root = cd.from(type);
        cd.where(PredicateBuilder.build(cb, root, filter));
        int n = em.createQuery(cd).executeUpdate();
        em.clear();
        return n;
    }
}
