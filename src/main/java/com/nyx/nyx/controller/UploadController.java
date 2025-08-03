package com.nyx.nyx.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.net.NetworkInterface;
import java.net.InetAddress;

// New imports for persistence
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Controller
public class UploadController {

    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/file-storage";

    // Store mapping of 4-digit codes to file names
    private static final Map<String, String> codeToFileMap = new ConcurrentHashMap<>();
    private static final Random random = new Random();

    // New fields for persistence
    private final String MAPPING_FILE = System.getProperty("user.dir") + "/file-storage/mappings.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void initializeFileMap() {
        loadMappingsFromFile();
        // Also recover any files that might not be in the mapping
        recoverUnmappedFiles();
    }

    @PreDestroy
    public void cleanup() {
        saveMappingsToFile();
    }

    private void loadMappingsFromFile() {
        try {
            Path mappingPath = Paths.get(MAPPING_FILE);
            if (Files.exists(mappingPath)) {
                String json = Files.readString(mappingPath);

                // Use readValue with HashMap.class instead of TypeReference
                @SuppressWarnings("unchecked")
                Map<String, String> loadedMappings = objectMapper.readValue(json, HashMap.class);

                // Verify files still exist before adding to map
                for (Map.Entry<String, String> entry : loadedMappings.entrySet()) {
                    Path filePath = Paths.get(UPLOAD_DIR).resolve(entry.getValue());
                    if (Files.exists(filePath)) {
                        codeToFileMap.put(entry.getKey(), entry.getValue());
                    }
                }
                System.out.println("Loaded " + codeToFileMap.size() + " file mappings from storage");
            }
        } catch (Exception e) {
            System.err.println("Error loading mappings: " + e.getMessage());
            // Fall back to recovery method
            recoverUnmappedFiles();
        }
    }

    private void saveMappingsToFile() {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String json = objectMapper.writeValueAsString(codeToFileMap);
            Files.writeString(Paths.get(MAPPING_FILE), json);
            System.out.println("Saved " + codeToFileMap.size() + " file mappings");
        } catch (Exception e) {
            System.err.println("Error saving mappings: " + e.getMessage());
        }
    }

    private void recoverUnmappedFiles() {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (Files.exists(uploadPath)) {
                Files.list(uploadPath)
                        .filter(Files::isRegularFile)
                        .filter(file -> !file.getFileName().toString().equals("mappings.json"))
                        .forEach(file -> {
                            String fileName = file.getFileName().toString();
                            // Check if filename starts with 4 digits
                            if (fileName.matches("^\\d{4}.*")) {
                                String code = fileName.substring(0, 4);
                                if (!codeToFileMap.containsKey(code)) {
                                    codeToFileMap.put(code, fileName);
                                    System.out.println("Recovered unmapped file: " + code + " -> " + fileName);
                                }
                            }
                        });
                // Save the updated mappings
                saveMappingsToFile();
            }
        } catch (IOException e) {
            System.err.println("Error recovering unmapped files: " + e.getMessage());
        }
    }

    @GetMapping("/")
    public String showIndexPage() {
        return "index"; // maps to index.html
    }

    @GetMapping("/upload")
    public String showUploadPage() {
        return "upload"; // maps to upload.html
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "⚠ No file selected.");
            return "redirect:/upload";
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique 4-digit code
            String shareCode = generateUniqueCode();

            // Get file extension
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            // Create new filename with code
            String newFileName = shareCode + fileExtension;
            Path filePath = uploadPath.resolve(newFileName);

            // Save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Store mapping
            codeToFileMap.put(shareCode, newFileName);

            // Save mappings to file immediately
            saveMappingsToFile();

            // Use flash attributes - these will be cleared after redirect
            redirectAttributes.addFlashAttribute("message", "✅ Uploaded: " + originalFileName);
            redirectAttributes.addFlashAttribute("shareCode", shareCode);
            redirectAttributes.addFlashAttribute("fileName", originalFileName);

        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("message", "❌ Upload failed: " + e.getMessage());
        }

        return "redirect:/upload";
    }

    @GetMapping("/share/{code}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String code) {
        try {
            String fileName = codeToFileMap.get(code);
            if (fileName == null) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = Paths.get(UPLOAD_DIR).resolve(fileName);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Get original filename (remove code prefix)
                String originalName = fileName.substring(4); // Remove 4-digit code
                if (originalName.startsWith(".")) {
                    originalName = "file" + originalName; // Handle files that start with extension
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + originalName + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/info/{code}")
    public String showFileInfo(@PathVariable String code, Model model) {
        String fileName = codeToFileMap.get(code);
        if (fileName == null) {
            model.addAttribute("error", "File not found for code: " + code);
            return "file-info";
        }

        try {
            Path filePath = Paths.get(UPLOAD_DIR).resolve(fileName);
            if (Files.exists(filePath)) {
                String originalName = fileName.substring(4); // Remove 4-digit code
                if (originalName.startsWith(".")) {
                    originalName = "file" + originalName;
                }

                long fileSize = Files.size(filePath);
                model.addAttribute("fileName", originalName);
                model.addAttribute("fileSize", formatFileSize(fileSize));
                model.addAttribute("shareCode", code);
                model.addAttribute("downloadUrl", "/share/" + code);
            } else {
                model.addAttribute("error", "File not found");
            }
        } catch (IOException e) {
            model.addAttribute("error", "Error reading file information");
        }

        return "file-info";
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = String.format("%04d", random.nextInt(10000));
        } while (codeToFileMap.containsKey(code));
        return code;
    }

    @GetMapping("/api/network-info")
    @ResponseBody
    public String getNetworkInfo() {
        try {
            // Get all network interfaces
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                // Skip loopback and inactive interfaces
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                // Get IP addresses for this interface
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();

                    // We want IPv4 addresses that are not loopback
                    if (!address.isLoopbackAddress() &&
                            address instanceof java.net.Inet4Address &&
                            !address.isLinkLocalAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            return "Unable to get IP";
        }
        return "No IP found";
    }

    @GetMapping("/vault")
    public String showVaultPage(Model model) {
        List<FileInfo> files = getAllUploadedFiles();
        model.addAttribute("files", files);
        return "vault";
    }

    @GetMapping("/vault/delete/{code}")
    public String deleteFile(@PathVariable String code, RedirectAttributes redirectAttributes) {
        try {
            String fileName = codeToFileMap.get(code);
            if (fileName != null) {
                Path filePath = Paths.get(UPLOAD_DIR).resolve(fileName);
                Files.deleteIfExists(filePath);
                codeToFileMap.remove(code);

                // Save updated mappings
                saveMappingsToFile();

                redirectAttributes.addFlashAttribute("message", "✅ File deleted successfully");
            } else {
                redirectAttributes.addFlashAttribute("message", "❌ File not found");
            }
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("message", "❌ Error deleting file: " + e.getMessage());
        }
        return "redirect:/vault";
    }

    private List<FileInfo> getAllUploadedFiles() {
        List<FileInfo> files = new ArrayList<>();

        for (Map.Entry<String, String> entry : codeToFileMap.entrySet()) {
            String code = entry.getKey();
            String fileName = entry.getValue();

            try {
                Path filePath = Paths.get(UPLOAD_DIR).resolve(fileName);
                if (Files.exists(filePath)) {
                    String originalName = getOriginalFileName(fileName);
                    String extension = getFileExtension(originalName);
                    long fileSize = Files.size(filePath);

                    FileInfo fileInfo = new FileInfo();
                    fileInfo.setCode(code);
                    fileInfo.setFileName(originalName);
                    fileInfo.setExtension(extension);
                    fileInfo.setSize(formatFileSize(fileSize));
                    fileInfo.setSizeBytes(fileSize);
                    fileInfo.setUploadTime(Files.getLastModifiedTime(filePath).toString());

                    files.add(fileInfo);
                }
            } catch (IOException e) {
                // Skip files that can't be read
            }
        }

        // Sort by upload time (newest first)
        files.sort((a, b) -> b.getUploadTime().compareTo(a.getUploadTime()));
        return files;
    }

    private String getOriginalFileName(String storedFileName) {
        // Remove the 4-digit code prefix
        String originalName = storedFileName.substring(4);
        if (originalName.startsWith(".")) {
            originalName = "file" + originalName;
        }
        return originalName;
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        }
        return "";
    }

    // Inner class for file information
    public static class FileInfo {
        private String code;
        private String fileName;
        private String extension;
        private String size;
        private long sizeBytes;
        private String uploadTime;

        // Getters and setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public String getExtension() { return extension; }
        public void setExtension(String extension) { this.extension = extension; }

        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }

        public long getSizeBytes() { return sizeBytes; }
        public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

        public String getUploadTime() { return uploadTime; }
        public void setUploadTime(String uploadTime) { this.uploadTime = uploadTime; }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}