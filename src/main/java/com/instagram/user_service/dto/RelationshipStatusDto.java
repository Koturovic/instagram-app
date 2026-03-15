package com.instagram.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipStatusDto {
    private boolean following;
    private boolean pending;
    private boolean blocked;
}