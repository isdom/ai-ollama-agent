package com.yulore.ollama.service;

import com.google.common.base.Strings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
@Service
public class LocalOllamaServiceImpl implements LocalOllamaService {
    @PostConstruct
    private void init() {
        api_timer = timerProvider.getObject("llm.ds32.duration", "", new String[]{"ollama", "chat"});
        gaugeProvider.getObject((Supplier<Number>)_workers::get, "llm.ds32.workers", "", new String[0]);
        chat_counter = counterProvider.getObject("llm.ds32.chat", "", new String[0]);
    }

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
        final int idx = _chatCount.incrementAndGet();
        final long startInMs = System.currentTimeMillis();
        log.info("[{}]: chat start", idx);
        if (null != _onStart) {
            _onStart.run();
        }

        _workers.incrementAndGet();
        try {
            final List<OllamaApi.Message> messages = new ArrayList<>();
            for (int i = 0; i < roleAndContents.length; i+=2) {
                messages.add(OllamaApi.Message.builder()
                        .role(roleAndContents[i])
                        .content(roleAndContents[i+1])
                        .build());
            }
            final Timer.Sample sample = Timer.start();
            final String result = _ollamaApi.chat(OllamaApi.ChatRequest.builder()
                    .model(_model)
                    .stream(false)
                    .messages(messages.toArray(new OllamaApi.Message[0]))
                    .build());
            sample.stop(api_timer);
            return Map.of(
                    "agent", _agentId,
                    "result", result
                    );
        } finally {
            _workers.decrementAndGet();
            chat_counter.increment();
            if (null != _onEnd) {
                _onEnd.run();
            }
            log.info("[{}]: chat end cost: {} s", idx, (System.currentTimeMillis() - startInMs) / 1000.0f);
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

    private final ObjectProvider<Timer> timerProvider;
    private final ObjectProvider<Gauge> gaugeProvider;
    private final ObjectProvider<Counter> counterProvider;

    private Timer api_timer;
    private Counter chat_counter;

    private Runnable _onStart = null;
    private Runnable _onEnd = null;
    private String _agentId;
    private final AtomicInteger _workers = new AtomicInteger(0);
    private final AtomicInteger _chatCount = new AtomicInteger(0);
}
