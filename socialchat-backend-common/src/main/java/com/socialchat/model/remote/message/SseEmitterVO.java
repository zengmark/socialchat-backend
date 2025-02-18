package com.socialchat.model.remote.message;

import lombok.Data;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.Serializable;

@Data
public class SseEmitterVO extends SseEmitter implements Serializable {
    private static final long serialVersionUID = 1L;
}
