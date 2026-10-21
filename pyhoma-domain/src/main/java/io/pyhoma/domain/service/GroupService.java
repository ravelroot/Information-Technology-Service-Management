package io.pyhoma.domain.service;

import io.pyhoma.domain.model.core.GroupMember;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.core.UserGroup;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.PageResult;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.query.Sort;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.support.BusinessException;
import io.pyhoma.domain.support.Texts;
import io.pyhoma.domain.support.Validator;
import java.util.List;

public class GroupService extends AbstractService {

    public GroupService(Services services) {
        super(services);
    }

    public PageResult<UserGroup> search(String text, int page, int size) {
        require(Module.GROUP, Right.READ);
        Filter f = Filter.and(security().orgFilter(), Filter.search(text, "name", "email", "notes"));
        return repo(UserGroup.class).page(Query.where(f).orderBy(Sort.asc("name")).page(page, size));
    }

    public List<UserGroup> assignable() {
        return repo(UserGroup.class).find(Query.where(Filter.and(Filter.isTrue("assignable"), security().orgFilter()))
                .orderBy(Sort.asc("name")));
    }

    public UserGroup get(Long id) {
        UserGroup g = repo(UserGroup.class).getById(id);
        checkVisible(g);
        return g;
    }

    public UserGroup save(UserGroup group) {
        require(Module.GROUP, group.isNew() ? Right.CREATE : Right.UPDATE);
        return saveInternal(group);
    }

    UserGroup saveInternal(UserGroup group) {
        group.setName(Texts.clean(group.getName()));
        Validator.create().required("name", group.getName()).required("orgUnitId", group.getOrgUnitId())
                .email("email", group.getEmail()).throwIfInvalid();
        return tx(() -> persist(group));
    }

    public void delete(Long id) {
        require(Module.GROUP, Right.DELETE);
        txRun(() -> {
            UserGroup g = get(id);
            repo(GroupMember.class).deleteWhere(Filter.eq("groupId", id));
            purge(g);
        });
    }

    public List<GroupMember> members(Long groupId) {
        return repo(GroupMember.class).find(Query.where(Filter.eq("groupId", groupId)).orderBy(Sort.asc("id")));
    }

    public List<Long> memberIds(Long groupId) {
        return members(groupId).stream().map(GroupMember::getUserId).toList();
    }

    public List<Long> managerIds(Long groupId) {
        return members(groupId).stream().filter(GroupMember::isManager).map(GroupMember::getUserId).toList();
    }

    public GroupMember addMember(Long groupId, Long userId, boolean manager) {
        require(Module.GROUP, Right.UPDATE);
        return addMemberInternal(groupId, userId, manager);
    }

    GroupMember addMemberInternal(Long groupId, Long userId, boolean manager) {
        return tx(() -> {
            if (repo(GroupMember.class).exists(Filter.and(Filter.eq("groupId", groupId), Filter.eq("userId", userId)))) {
                throw new BusinessException("error.alreadyMember");
            }
            GroupMember m = new GroupMember();
            m.setGroupId(groupId);
            m.setUserId(userId);
            m.setManager(manager);
            return insert(m);
        });
    }

    public void setManager(Long memberId, boolean manager) {
        require(Module.GROUP, Right.UPDATE);
        txRun(() -> {
            GroupMember m = repo(GroupMember.class).getById(memberId);
            m.setManager(manager);
            update(m);
        });
    }

    public void removeMember(Long memberId) {
        require(Module.GROUP, Right.UPDATE);
        txRun(() -> purge(repo(GroupMember.class).getById(memberId)));
    }
}
