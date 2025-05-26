package com.cinefinder.chat.data.entity;

public enum ChatType {
    SYSTEM,  // 시스템 메시지 (입장, 퇴장 등)
    CHAT,     // 일반 사용자 메시지
    PREV,
    JOIN,
    LEAVE
}