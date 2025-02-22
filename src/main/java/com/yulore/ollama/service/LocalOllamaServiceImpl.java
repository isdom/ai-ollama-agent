package com.yulore.ollama.service;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class LocalOllamaServiceImpl implements LocalOllamaService {
    @Override
    public void setChatHook(final Runnable onStart, final Runnable onEnd) {
        _onStart = onStart;
        _onEnd = onEnd;
    }

    @Override
    public String chat(final String[] roleAndContents) {
        if (null != _onStart) {
            _onStart.run();
        }

        try {
            final List<OllamaApi.Message> messages = new ArrayList<>();
            for (int i = 0; i < roleAndContents.length; i+=2) {
                messages.add(OllamaApi.Message.builder()
                        .role(roleAndContents[i])
                        .content(roleAndContents[i+1])
                        .build());
            }
            return _ollamaApi.chat(OllamaApi.ChatRequest.builder()
                    .model(_model)
                    .stream(false)
                    .messages(messages.toArray(new OllamaApi.Message[0]))
                    .build());
        } finally {
            if (null != _onEnd) {
                _onEnd.run();
            }
        }
    }

    @Override
    public boolean isOllamaOnline() {
        try {
            final String version = _ollamaApi.version();
            return !Strings.isNullOrEmpty(version);
        } catch (Exception ignored) {
        }
        return false;
    }

    @Value("${ollama.model}")
    private String _model;

    @Autowired
    private OllamaApi _ollamaApi;

    private Runnable _onStart = null;
    private Runnable _onEnd = null;
}
