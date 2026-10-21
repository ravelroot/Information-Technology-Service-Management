package io.pyhoma.domain.service;

import io.pyhoma.domain.model.itil.ItilStatus;
import io.pyhoma.domain.model.itil.ItilType;
import io.pyhoma.domain.model.itil.RelationType;

public record RelatedItem(Long relationId, RelationType relationType, boolean outgoing, ItilType type, Long id,
                          String title, ItilStatus status, Integer priority) {
}
