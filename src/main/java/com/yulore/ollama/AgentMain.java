package com.yulore.ollama;


import com.yulore.api.MasterService;
import com.yulore.api.OllamaService;
import com.yulore.ollama.service.LocalOllamaService;
import com.yulore.util.ExceptionUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRemoteService;
import org.redisson.api.RedissonClient;
import org.redisson.api.RemoteInvocationOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Function;

@Slf4j
@Component
public class AgentMain {
    @PostConstruct
    public void start() {
        log.info("Ollama-Agent: started with redisson: {}", redisson.getConfig().useSingleServer().getDatabase());

        scheduler = Executors.newScheduledThreadPool(1, new DefaultThreadFactory("reportExecutor"));
        workers = Executors.newFixedThreadPool(_service_workers, new DefaultThreadFactory("workersExecutor"));

        checkAndScheduleNext((tm) -> {
            if (!localOllamaService.isOllamaOnline()) {
                log.warn("agent({}) local_ollama_not_online, wait for re-try", agentId);
                // continue to check ollama status
                return true;
            } else {
                log.info("agent({}): wait_for_local_ollama_online_cost: {} s",
                        agentId, (System.currentTimeMillis() - tm) / 1000.0f);

                registerAndUpdateAgentStatus();
                // stop checking status for service is online
                return false;
            }
        }, System.currentTimeMillis());
    }

    private void registerAndUpdateAgentStatus() {
        // CosyVoice is online, stop check and begin to register RemoteService
        final RRemoteService rs = redisson.getRemoteService(_service_ollama);
        rs.register(OllamaService.class, localOllamaService, _service_workers, workers);

        final MasterService masterService = redisson.getRemoteService(_service_master)
                .get(MasterService.class, RemoteInvocationOptions.defaults().noAck().noResult());

        localOllamaService.setAgentId(agentId);
        final Runnable doUpdate = () -> masterService.updateAgentStatus(agentId, rs.getFreeWorkers(OllamaService.class), System.currentTimeMillis());
        localOllamaService.setChatHook(
                // start to work
                doUpdate,
                // worker back to idle
                ()->scheduler.schedule(doUpdate, 10, TimeUnit.MILLISECONDS));

        checkAndScheduleNext((startedTimestamp)-> {
            doUpdate.run();
            return true;
        }, System.currentTimeMillis());
    }

    private void checkAndScheduleNext(final Function<Long, Boolean> doCheck, final Long timestamp) {
        try {
            if (doCheck.apply(timestamp)) {
                scheduler.schedule(()->checkAndScheduleNext(doCheck, timestamp), _check_interval, TimeUnit.MILLISECONDS);
            }
        } catch (final Exception ex) {
            log.warn("agent({}) checkAndScheduleNext exception: {}", agentId, ExceptionUtil.exception2detail(ex));
        }
        finally {
        }
    }

    @PreDestroy
    public void stop() {
        workers.shutdownNow();
        scheduler.shutdownNow();
        final RRemoteService remoteService = redisson.getRemoteService(_service_ollama);
        remoteService.deregister(OllamaService.class);

        log.info("Ollama-Agent: shutdown");
    }

    @Value("${service.ollama}")
    private String _service_ollama;

    @Value("${service.workers:1}")
    private int _service_workers;

    @Value("${service.master}")
    private String _service_master;

    @Autowired
    private RedissonClient redisson;

    @Autowired
    private LocalOllamaService localOllamaService;

    @Value("${agent.check_interval:10000}") // default: 10 * 1000ms
    private long _check_interval;

    private ScheduledExecutorService scheduler;
    private ExecutorService workers;

    private final String agentId = UUID.randomUUID().toString();
}