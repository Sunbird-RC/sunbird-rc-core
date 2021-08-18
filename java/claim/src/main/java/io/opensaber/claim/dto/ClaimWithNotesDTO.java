package io.opensaber.claim.dto;

import io.opensaber.claim.entity.Claim;
import io.opensaber.claim.entity.ClaimNote;
import lombok.Data;

import java.util.List;

@Data
public class ClaimWithNotesDTO {
    Claim claim;
    List<ClaimNote> notes;
}
