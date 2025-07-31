package com.nyx.nyx.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

@Controller
public class UploadController {

    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads";

    @GetMapping("/upload")
    public String showUploadPage() {
        return "upload"; // maps to upload.html
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, Model model) {
        if (file.isEmpty()) {
            model.addAttribute("message", "⚠ No file selected.");
            return "upload";
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            model.addAttribute("message", "✅ Uploaded: " + file.getOriginalFilename());

        } catch (IOException e) {
            model.addAttribute("message", "❌ Upload failed: " + e.getMessage());
        }

        return "upload";
    }
}
