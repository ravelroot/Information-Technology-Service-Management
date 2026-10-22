package io.talos.domain.service;

import io.talos.domain.model.itil.ItilStatus;
import io.talos.domain.model.itil.ItilType;
import io.talos.domain.model.itil.RelationType;

public record RelatedItem(Long relationId, RelationType relationType, boolean outgoing, ItilType type, Long id,
                          String title, ItilStatus status, Integer priority) {
}
