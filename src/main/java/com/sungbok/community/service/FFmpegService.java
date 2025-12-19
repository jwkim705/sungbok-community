package com.sungbok.community.service;

import com.sungbok.community.dto.FileValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * FFmpeg 서비스
 * 동영상 메타데이터 추출 (ffprobe 사용)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FFmpegService {

    private final OciStorageService ociStorageService;

    @Value("${ffmpeg.probe-path}")
    private String ffprobePath;

    /**
     * 동영상 메타데이터를 추출합니다 (ffprobe 사용)
     * OCI에서 파일을 다운로드하여 임시 파일로 저장 후 검증
     *
     * @param objectKey OCI Object Key
     * @return FileValidationResult (duration, resolution, codec)
     */
    public FileValidationResult extractVideoMetadata(String objectKey) {
        Path tempFile = null;

        try {
            // 1. OCI에서 파일 다운로드 (임시 파일)
            tempFile = Files.createTempFile("video-", ".tmp");
            try (InputStream inputStream = ociStorageService.downloadFull(objectKey);
                 OutputStream outputStream = Files.newOutputStream(tempFile)) {
                inputStream.transferTo(outputStream);
            }

            log.debug("임시 파일 생성 완료: objectKey={}, tempFile={}", objectKey, tempFile);

            // 2. ffprobe 실행 (JSON 출력)
            ProcessBuilder processBuilder = new ProcessBuilder(
                    ffprobePath,
                    "-v", "quiet",
                    "-print_format", "json",
                    "-show_format",
                    "-show_streams",
                    tempFile.toString()
            );

            Process process = processBuilder.start();
            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("ffprobe 실행 실패: exitCode={}, objectKey={}", exitCode, objectKey);
                return FileValidationResult.builder()
                        .valid(false)
                        .errorMessage("동영상 검증 실패")
                        .build();
            }

            // 3. JSON 파싱 (Jackson 3.x)
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(output);

            // duration (format.duration)
            JsonNode formatNode = root.get("format");
            if (formatNode == null || formatNode.get("duration") == null) {
                log.warn("duration 필드 없음: objectKey={}", objectKey);
                return FileValidationResult.builder()
                        .valid(false)
                        .errorMessage("duration 필드를 찾을 수 없습니다")
                        .build();
            }
            Double duration = formatNode.get("duration").asDouble();

            // resolution & codec (streams[0])
            JsonNode streamsNode = root.get("streams");
            if (streamsNode == null || !streamsNode.isArray() || streamsNode.isEmpty()) {
                log.warn("streams 필드 없음: objectKey={}", objectKey);
                return FileValidationResult.builder()
                        .valid(false)
                        .errorMessage("streams 필드를 찾을 수 없습니다")
                        .build();
            }

            JsonNode videoStream = streamsNode.get(0);
            if (videoStream == null) {
                log.warn("video stream 없음: objectKey={}", objectKey);
                return FileValidationResult.builder()
                        .valid(false)
                        .errorMessage("video stream을 찾을 수 없습니다")
                        .build();
            }

            int width = videoStream.get("width") != null ? videoStream.get("width").asInt() : 0;
            int height = videoStream.get("height") != null ? videoStream.get("height").asInt() : 0;
            String resolution = width + "x" + height;
            String codec = videoStream.get("codec_name") != null ? videoStream.get("codec_name").asString() : "unknown";

            log.debug("동영상 메타데이터 추출 완료: objectKey={}, duration={}, resolution={}, codec={}",
                    objectKey, duration, resolution, codec);

            return FileValidationResult.builder()
                    .valid(true)
                    .duration(duration)
                    .resolution(resolution)
                    .codec(codec)
                    .build();

        } catch (Exception e) {
            log.error("동영상 메타데이터 추출 실패: objectKey={}", objectKey, e);
            return FileValidationResult.builder()
                    .valid(false)
                    .errorMessage("메타데이터 추출 중 오류 발생: " + e.getMessage())
                    .build();
        } finally {
            // 4. 임시 파일 삭제
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                    log.debug("임시 파일 삭제 완료: {}", tempFile);
                } catch (IOException e) {
                    log.warn("임시 파일 삭제 실패: {}", tempFile, e);
                }
            }
        }
    }
}
