package com.tyh.chat.model;

import java.util.Set;

/** 对外公开的模型信息，不包含任何认证信息。 */
public record ModelSummary(
        String name,
        String provider,
        String modelName,
        Set<String> capabilities,
        boolean enabled) {

    public static ModelSummary from(ModelEntry entry) {
        return new ModelSummary(
                entry.getName(),
                entry.getProvider(),
                entry.getModelName(),
                Set.copyOf(entry.getCapabilities()),
                true);
    }
}
