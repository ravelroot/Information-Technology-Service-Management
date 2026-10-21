package io.pyhoma.domain.service;

import io.pyhoma.domain.model.itil.ItilStatus;
import io.pyhoma.domain.model.itil.TicketType;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

public class ItilSearch {

    public enum View { ALL, OPEN, MINE_ASSIGNED, MY_GROUPS, MINE_REQUESTED, UNASSIGNED, OVERDUE, SOLVED, CLOSED, TRASH }

    private String text;
    private View view = View.OPEN;
    private Set<ItilStatus> statuses = EnumSet.noneOf(ItilStatus.class);
    private TicketType ticketType;
    private Long assigneeId;
    private Long assigneeGroupId;
    private Long requesterId;
    private Long categoryId;
    private Long locationId;
    private Integer minPriority;
    private LocalDate openedFrom;
    private LocalDate openedTo;
    private String sortField = "id";
    private boolean ascending;
    private int page;
    private int size = 50;

    public static ItilSearch create() {
        return new ItilSearch();
    }

    public ItilSearch text(String v) {
        this.text = v;
        return this;
    }

    public ItilSearch view(View v) {
        this.view = v == null ? View.ALL : v;
        return this;
    }

    public ItilSearch statuses(Set<ItilStatus> v) {
        this.statuses = v == null || v.isEmpty() ? EnumSet.noneOf(ItilStatus.class) : EnumSet.copyOf(v);
        return this;
    }

    public ItilSearch ticketType(TicketType v) {
        this.ticketType = v;
        return this;
    }

    public ItilSearch assigneeId(Long v) {
        this.assigneeId = v;
        return this;
    }

    public ItilSearch assigneeGroupId(Long v) {
        this.assigneeGroupId = v;
        return this;
    }

    public ItilSearch requesterId(Long v) {
        this.requesterId = v;
        return this;
    }

    public ItilSearch categoryId(Long v) {
        this.categoryId = v;
        return this;
    }

    public ItilSearch locationId(Long v) {
        this.locationId = v;
        return this;
    }

    public ItilSearch minPriority(Integer v) {
        this.minPriority = v;
        return this;
    }

    public ItilSearch openedBetween(LocalDate from, LocalDate to) {
        this.openedFrom = from;
        this.openedTo = to;
        return this;
    }

    public ItilSearch sort(String field, boolean asc) {
        this.sortField = field;
        this.ascending = asc;
        return this;
    }

    public ItilSearch page(int pageIndex, int pageSize) {
        this.page = pageIndex;
        this.size = pageSize;
        return this;
    }

    public String text() {
        return text;
    }

    public View view() {
        return view;
    }

    public Set<ItilStatus> statuses() {
        return statuses;
    }

    public TicketType ticketType() {
        return ticketType;
    }

    public Long assigneeId() {
        return assigneeId;
    }

    public Long assigneeGroupId() {
        return assigneeGroupId;
    }

    public Long requesterId() {
        return requesterId;
    }

    public Long categoryId() {
        return categoryId;
    }

    public Long locationId() {
        return locationId;
    }

    public Integer minPriority() {
        return minPriority;
    }

    public LocalDate openedFrom() {
        return openedFrom;
    }

    public LocalDate openedTo() {
        return openedTo;
    }

    public String sortField() {
        return sortField;
    }

    public boolean ascending() {
        return ascending;
    }

    public int pageIndex() {
        return page;
    }

    public int pageSize() {
        return size;
    }
}
