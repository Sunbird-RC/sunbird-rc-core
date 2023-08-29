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

    @Value("${outside.up.pending.item.subject}")
    private String outsideUpPendingItemSubject;

    @Value("${from.up.pending.item.subject}")
    private String fromUpPendingItemSubject;

    @Value("${good.standing.pending.item.subject}")
    private String goodStandingPendingItemSubject;

    @Value("${up.council.name}")
    private String upCouncilName;

    @Value("${regulator.table.name}")
    private String regulatorTableName;

    @Value("${student.foreign.verification.table.name}")
    private String studentForeignVerificationTableName;

    @Value("${student.outside.verification.table.name}")
    private String studentOutsideVerificationTableName;

    @Value("${claim.url}")
    private String claimUrl;

    @Value("${registry.shard.id}")
    private String registryShardId;

    @Value("${student.foreign.entity.name}")
    private String studentForeignEntityName;

    @Value("${student.from.up.entity.name}")
    private String studentFromUpEntityName;

    @Value("${student.from.outside.entity.name}")
    private String studentFromOutsideEntityName;

    @Value("${student.good.standing.entity.name}")
    private String studentGoodStandingEntityName;
}
