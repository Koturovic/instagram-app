package com.instagram.interaction_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipStatusResponse {
    private Boolean following;
    private Boolean pending;
    private Boolean blocked;
}
