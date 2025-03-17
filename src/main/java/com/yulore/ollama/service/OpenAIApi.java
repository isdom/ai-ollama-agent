package com.yulore.ollama.service;

import feign.Request;
import lombok.Builder;
import lombok.ToString;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.concurrent.TimeUnit;

// doc: https://github.com/ollama/ollama/blob/main/docs/openai.md

@FeignClient(
        name = "ollama",
        url = "http://127.0.0.1:11434",
        configuration = OpenAIApi.OllamaApiConfig.class)
public interface OpenAIApi {

    @Builder
    @ToString
    class Message {
        public String role;
        public String content;
    }

    @Builder
    @ToString
    class ChatRequest {
        public String model;
        public boolean stream;
        public Message[] messages;
    }

    /*
    {
      "id": "xxxxxx",
      "object": "chat.completion",
      "created": 1677649420,
      "model": "gpt-3.5-turbo",
      "usage": { "prompt_tokens": 56, "completion_tokens": 31, "total_tokens": 87 },
      "choices": [
        {
          "message": {
            "role": "assistant",
            "content": "The 2020 World Series was played in Arlington, Texas at the Globe Life Field, which was the new home stadium for the Texas Rangers."
          },
          "finish_reason": "stop",
          "index": 0
        }
      ]
    }
    */

    @RequestMapping(value = "/v1/chat/completions", method = RequestMethod.POST)
    String chat(@RequestBody ChatRequest request);

    @RequestMapping(value = "/api/version", method = RequestMethod.GET)
    String version();

    // 配置类定义
    class OllamaApiConfig {
        @Bean
        public Request.Options apiOptions() {
            // connect(200ms), read(500ms), followRedirects(true)
            return new Request.Options(200, TimeUnit.MILLISECONDS, 10, TimeUnit.MINUTES,true);
        }
    }

}
