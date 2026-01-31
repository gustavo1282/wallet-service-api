package com.guga.walletserviceapi.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.guga.walletserviceapi.model.outbox.OutboxEvent;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
}
