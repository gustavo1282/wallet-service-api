package com.gugawallet.walletconsumer.service;

import com.gugawallet.walletconsumer.model.CustomerStatus;

import org.springframework.stereotype.Service;

@Service
public class ScorePolicyService {

    public ScoreDecision evaluate(Integer score) {
        if (score == null) {
            return new ScoreDecision(CustomerStatus.REVIEW, null, "score-missing");
        }
        if (score > 90) {
            return new ScoreDecision(CustomerStatus.ACTIVE, 20, "approved-a");
        }
        if (score > 80) {
            return new ScoreDecision(CustomerStatus.ACTIVE, 50, "approved-b");
        }
        if (score >= 60) {
            return new ScoreDecision(CustomerStatus.REVIEW, null, "manual-review");
        }
        return new ScoreDecision(CustomerStatus.REJECTED, null, "rejected");
    }

    public record ScoreDecision(CustomerStatus status, Integer initialDeposit, String reason) {
    }
}
