package io.opensaber.claim.service;

import io.opensaber.claim.entity.Claim;
import io.opensaber.claim.repository.ClaimRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class ClaimService {

    private final ClaimRepository claimRepository;

    @Autowired
    public ClaimService(ClaimRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    public Claim save(Claim claim) {
        return claimRepository.save(claim);
    }

    public Optional<Claim> findById(long id) {
        return claimRepository.findById(id);
    }

    public List<Claim> findAll() {
        return StreamSupport
                .stream(claimRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
    }
}
