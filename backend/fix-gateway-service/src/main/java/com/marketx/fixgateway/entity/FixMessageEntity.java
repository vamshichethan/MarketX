package com.marketx.fixgateway.entity;

import com.marketx.fixgateway.enums.FixDirection;
import com.marketx.fixgateway.enums.FixMessageStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "fix_messages")
public class FixMessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fixMessageId;

    @Enumerated(EnumType.STRING)
    private FixDirection direction;

    private String messageType;
    private String clOrdId;

    @Column(length = 4000)
    private String rawMessage;

    @Enumerated(EnumType.STRING)
    private FixMessageStatus status;

    @Column(length = 2000)
    private String rejectionReason;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getFixMessageId() {
        return fixMessageId;
    }

    public void setFixMessageId(String fixMessageId) {
        this.fixMessageId = fixMessageId;
    }

    public FixDirection getDirection() {
        return direction;
    }

    public void setDirection(FixDirection direction) {
        this.direction = direction;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getClOrdId() {
        return clOrdId;
    }

    public void setClOrdId(String clOrdId) {
        this.clOrdId = clOrdId;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }

    public FixMessageStatus getStatus() {
        return status;
    }

    public void setStatus(FixMessageStatus status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
