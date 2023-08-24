package dev.sunbirdrc.claim.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class PropertyMapper {
    @Value("${simple.mail.message.from}")
    private String simpleMailMessageFrom;

    @Value("${foreign.pending.item.subject}")
    private String foreignPendingItemSubject;

    @Value("${up.council.name}")
    private String upCouncilName;

    @Value("${regulator.table.name}")
    private String regulatorTableName;

    @Value("${student.foreign.verification.table.name}")
    private String studentForeignVerificationTableName;

    @Value("${claim.url}")
    private String claimUrl;

    @Value("${registry.shard.id}")
    private String registryShardId;
}
