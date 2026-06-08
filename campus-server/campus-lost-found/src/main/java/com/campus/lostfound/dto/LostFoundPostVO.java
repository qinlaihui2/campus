package com.campus.lostfound.dto;

import com.campus.lostfound.entity.LostFoundPost;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LostFoundPostVO extends LostFoundPost {
    private String publisherName;
    private String publisherAvatar;
}
