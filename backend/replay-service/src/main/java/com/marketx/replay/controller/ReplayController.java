package com.marketx.replay.controller;

import com.marketx.replay.dto.CreateReplaySessionRequest;
import com.marketx.replay.dto.ReplaySessionResponse;
import com.marketx.replay.dto.SpeedUpdateRequest;
import com.marketx.replay.service.ReplayService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

@RestController
@RequestMapping("/replay/sessions")
public class ReplayController {
    private final ReplayService replayService;

    public ReplayController(ReplayService replayService) {
        this.replayService = replayService;
    }

    @PostMapping
    public ReplaySessionResponse createSession(@Valid @RequestBody CreateReplaySessionRequest request) {
        return replayService.createSession(request);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReplaySessionResponse uploadAndCreateSession(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "1") int speedMultiplier
    ) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Replay upload file is empty");
        }

        Path uploadDir = Path.of("data", "replay", "uploads");
        Files.createDirectories(uploadDir);
        Path destination = uploadDir.resolve(file.getOriginalFilename() == null ? "replay-upload.csv" : file.getOriginalFilename()).normalize();
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return replayService.createSession(new CreateReplaySessionRequest(destination.toString(), speedMultiplier));
    }

    @PostMapping("/{sessionId}/play")
    public ReplaySessionResponse play(@PathVariable String sessionId) {
        return replayService.play(sessionId);
    }

    @PostMapping("/{sessionId}/pause")
    public ReplaySessionResponse pause(@PathVariable String sessionId) {
        return replayService.pause(sessionId);
    }

    @PostMapping("/{sessionId}/stop")
    public ReplaySessionResponse stop(@PathVariable String sessionId) {
        return replayService.stop(sessionId);
    }

    @PostMapping("/{sessionId}/speed")
    public ReplaySessionResponse updateSpeed(
            @PathVariable String sessionId,
            @Valid @RequestBody SpeedUpdateRequest request
    ) {
        return replayService.updateSpeed(sessionId, request.speedMultiplier());
    }

    @GetMapping("/{sessionId}")
    public ReplaySessionResponse getSession(@PathVariable String sessionId) {
        return replayService.getSession(sessionId);
    }

    @GetMapping
    public List<ReplaySessionResponse> getSessions() {
        return replayService.getSessions();
    }
}
