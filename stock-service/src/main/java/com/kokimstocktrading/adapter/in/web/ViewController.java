package com.kokimstocktrading.adapter.in.web;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ViewController {

  private final ResourceLoader resourceLoader;

  @GetMapping("/sse-test")
  public ResponseEntity<Resource> sseTestHtml() {
    Resource resource = resourceLoader.getResource("classpath:/view/sse-test.html");
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_HTML)
        .body(resource);
  }
}
