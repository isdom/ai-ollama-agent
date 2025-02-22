package com.yulore.ollama.service;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LocalOllamaServiceImpl implements LocalOllamaService {
    @Override
    public void setChatHook(final Runnable onStart, final Runnable onEnd) {
        _onStart = onStart;
        _onEnd = onEnd;
    }

    @Override
    public void setAgentId(final String agentId) {
        _agentId = agentId;
    }

    @Override
    public Map<String, String> chat(final String[] roleAndContents) {
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
            final String result = _ollamaApi.chat(OllamaApi.ChatRequest.builder()
                    .model(_model)
                    .stream(false)
                    .messages(messages.toArray(new OllamaApi.Message[0]))
                    .build());
            return Map.of(
                    "agent", _agentId,
                    "result", result
                    );
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
    private String _agentId;
}
