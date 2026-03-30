package com.campus.learning.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "materials")
public class Material {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String filename;

    @Column(name = "file_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private FileType fileType;

    @Column(name = "minio_path", nullable = false)
    private String minioPath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "ocr_text", columnDefinition = "TEXT")
    private String ocrText;

    @Column(name = "ocr_confidence")
    private Float ocrConfidence;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status = Status.uploaded;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum FileType {
        PPT, PDF, IMAGE
    }

    public enum Status {
        uploaded, processing, completed, failed
    }
}