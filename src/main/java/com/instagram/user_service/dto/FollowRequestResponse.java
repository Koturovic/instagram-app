package com.instagram.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowRequestResponse {

    /** Id zahteva (kad je profil privatni); null kad je odmah follow (javni) */
    private Long requestId;
    /** true ako je odmah uspostavljena follow relacija (javni profil) */
    private Boolean followed;
}
