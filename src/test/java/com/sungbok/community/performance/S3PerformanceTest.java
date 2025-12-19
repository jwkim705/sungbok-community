package com.sungbok.community.performance;

import com.sungbok.community.config.OciStorageProperties;
import com.sungbok.community.service.OciStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * OCI Storage 성능 테스트
 * Pre-signed URL 생성 속도 측정 (Netty vs Apache HTTP Client)
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("S3 Pre-signed URL 생성 성능 테스트")
public class S3PerformanceTest {

    @Autowired
    private OciStorageService ociStorageService;

    @Autowired
    private OciStorageProperties ociStorageProperties;

    private static final int WARMUP_ITERATIONS = 100;
    private static final int TEST_ITERATIONS = 1000;

    @BeforeEach
    void warmup() {
        // JVM 워밍업 (JIT 컴파일)
        System.out.println("=== JVM 워밍업 시작 ===");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            String objectKey = "warmup/" + UUID.randomUUID() + ".jpg";
            ociStorageService.generatePresignedUploadUrl(
                    objectKey,
                    "image/jpeg",
                    1024L,
                    Duration.ofMinutes(15)
            );
        }
        System.out.println("=== JVM 워밍업 완료 ===\n");
    }

    @Test
    @DisplayName("Pre-signed Upload URL 생성 성능 측정 (1000회)")
    void testPresignedUploadUrlPerformance() {
        List<Long> timings = new ArrayList<>();

        System.out.println("📊 Pre-signed Upload URL 성능 테스트 시작");
        System.out.println("반복 횟수: " + TEST_ITERATIONS + "회\n");

        // 1000회 반복 측정
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            String objectKey = String.format("test/%d/%s.jpg", i, UUID.randomUUID());

            long startNano = System.nanoTime();
            String uploadUrl = ociStorageService.generatePresignedUploadUrl(
                    objectKey,
                    "image/jpeg",
                    5_242_880L,
                    Duration.ofMinutes(15)
            );
            long endNano = System.nanoTime();

            assertNotNull(uploadUrl);
            timings.add(endNano - startNano);

            // 진행률 표시 (매 100회)
            if ((i + 1) % 100 == 0) {
                System.out.printf("진행: %d/%d 완료\n", i + 1, TEST_ITERATIONS);
            }
        }

        printStatistics("Upload URL", timings);
    }

    @Test
    @DisplayName("Pre-signed Download URL 생성 성능 측정 (1000회)")
    void testPresignedDownloadUrlPerformance() {
        List<Long> timings = new ArrayList<>();

        System.out.println("📊 Pre-signed Download URL 성능 테스트 시작");
        System.out.println("반복 횟수: " + TEST_ITERATIONS + "회\n");

        // 1000회 반복 측정
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            String objectKey = String.format("test/%d/%s.jpg", i, UUID.randomUUID());

            long startNano = System.nanoTime();
            String downloadUrl = ociStorageService.generatePresignedDownloadUrl(
                    objectKey,
                    Duration.ofMinutes(15)
            );
            long endNano = System.nanoTime();

            assertNotNull(downloadUrl);
            timings.add(endNano - startNano);

            // 진행률 표시 (매 100회)
            if ((i + 1) % 100 == 0) {
                System.out.printf("진행: %d/%d 완료\n", i + 1, TEST_ITERATIONS);
            }
        }

        printStatistics("Download URL", timings);
    }

    @Test
    @DisplayName("혼합 워크로드 성능 측정 (Upload 500 + Download 500)")
    void testMixedWorkloadPerformance() {
        List<Long> uploadTimings = new ArrayList<>();
        List<Long> downloadTimings = new ArrayList<>();

        System.out.println("📊 혼합 워크로드 성능 테스트 시작");
        System.out.println("Upload: 500회, Download: 500회\n");

        for (int i = 0; i < 500; i++) {
            String objectKey = String.format("test/%d/%s.jpg", i, UUID.randomUUID());

            // Upload URL 생성
            long startNano = System.nanoTime();
            ociStorageService.generatePresignedUploadUrl(
                    objectKey,
                    "image/jpeg",
                    5_242_880L,
                    Duration.ofMinutes(15)
            );
            long endNano = System.nanoTime();
            uploadTimings.add(endNano - startNano);

            // Download URL 생성
            startNano = System.nanoTime();
            ociStorageService.generatePresignedDownloadUrl(
                    objectKey,
                    Duration.ofMinutes(15)
            );
            endNano = System.nanoTime();
            downloadTimings.add(endNano - startNano);

            // 진행률 표시 (매 100회)
            if ((i + 1) % 100 == 0) {
                System.out.printf("진행: %d/500 완료\n", i + 1);
            }
        }

        System.out.println("\n=== Upload URL 통계 ===");
        printStatistics("Upload URL", uploadTimings);

        System.out.println("\n=== Download URL 통계 ===");
        printStatistics("Download URL", downloadTimings);
    }

    private void printStatistics(String operation, List<Long> timingsNano) {
        timingsNano.sort(Long::compareTo);

        double avgNano = timingsNano.stream().mapToLong(Long::longValue).average().orElse(0);
        long minNano = timingsNano.get(0);
        long maxNano = timingsNano.get(timingsNano.size() - 1);
        long p50Nano = timingsNano.get(timingsNano.size() / 2);
        long p95Nano = timingsNano.get((int) (timingsNano.size() * 0.95));
        long p99Nano = timingsNano.get((int) (timingsNano.size() * 0.99));

        String report = String.format("""

                ╔══════════════════════════════════════════╗
                ║  %s 성능 통계 (1000회)         ║
                ╠══════════════════════════════════════════╣
                ║  평균:      %8.2f ms                 ║
                ║  최소:      %8.2f ms                 ║
                ║  최대:      %8.2f ms                 ║
                ║  P50:       %8.2f ms                 ║
                ║  P95:       %8.2f ms                 ║
                ║  P99:       %8.2f ms                 ║
                ╚══════════════════════════════════════════╝

                ⚡ 처리량: %.2f requests/sec
                """,
                operation,
                avgNano / 1_000_000.0,
                minNano / 1_000_000.0,
                maxNano / 1_000_000.0,
                p50Nano / 1_000_000.0,
                p95Nano / 1_000_000.0,
                p99Nano / 1_000_000.0,
                1000.0 / (avgNano / 1_000_000_000.0)
        );

        System.out.println(report);

        // 파일로도 저장
        try {
            java.nio.file.Files.write(
                    java.nio.file.Paths.get("performance-result.txt"),
                    report.getBytes(),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
            );
        } catch (Exception e) {
            // 무시
        }
    }
}
