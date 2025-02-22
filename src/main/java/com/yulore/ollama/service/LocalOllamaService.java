package com.yulore.ollama.service;


import com.yulore.api.OllamaService;

public interface LocalOllamaService extends OllamaService {

    void setChatHook(final Runnable onStart, final Runnable onEnd);
    void setAgentId(final String agentId);

    boolean isOllamaOnline();
}
