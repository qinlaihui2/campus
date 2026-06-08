package com.campus.message.dto;

import com.campus.message.entity.Conversation;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ConversationVO extends Conversation {
    private String peerName;
    private String peerAvatar;
}
