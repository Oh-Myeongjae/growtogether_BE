package com.campfiredev.growtogether.bootcamp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentUpdateRequest {

    private Long memberId;
    private String content;
}
