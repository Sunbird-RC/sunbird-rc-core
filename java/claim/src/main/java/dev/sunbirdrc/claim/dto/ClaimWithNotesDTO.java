package dev.sunbirdrc.claim.dto;

import dev.sunbirdrc.claim.entity.Claim;
import dev.sunbirdrc.claim.entity.ClaimNote;
import lombok.Data;

import java.util.List;

@Data
public class ClaimWithNotesDTO {
    Claim claim;
    List<ClaimNote> notes;
}
