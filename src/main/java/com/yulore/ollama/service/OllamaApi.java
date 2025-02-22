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

// doc: https://github.com/ollama/ollama/blob/main/docs/api.md#generate-a-chat-completion

@FeignClient(
        name = "ollama",
        url = "http://127.0.0.1:11434",
        configuration = OllamaApi.OllamaApiConfig.class)
public interface OllamaApi {
    /*
    POST /api/chat
            Generate the next message in a chat with a provided model. This is a streaming endpoint, so there will be a series of responses. Streaming can be disabled using "stream": false. The final response object will include statistics and additional data from the request.

            Parameters
            model: (required) the model name
            messages: the messages of the chat, this can be used to keep a chat memory
            tools: list of tools in JSON for the model to use if supported

            The message object has the following fields:

            role: the role of the message, either system, user, assistant, or tool
            content: the content of the message
            images (optional): a list of images to include in the message (for multimodal models such as llava)
            tool_calls (optional): a list of tools in JSON that the model wants to use
            Advanced parameters (optional):

            format: the format to return a response in. Format can be json or a JSON schema.
            options: additional model parameters listed in the documentation for the Modelfile such as temperature
            stream: if false the response will be returned as a single response object, rather than a stream of objects
            keep_alive: controls how long the model will stay loaded into memory following the request (default: 5m)
    */

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
    @ToString
    class ChatResponse {
        public String model;
        public String created_at;
        public Message message;
        public String done_reason;
        public boolean done;
        public long total_duration;
        public long load_duration;
        public long prompt_eval_count;
        public long prompt_eval_duration;
        public long eval_count;
        public long eval_duration;
    }
     */

    @RequestMapping(value = "/api/chat", method = RequestMethod.POST)
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
