package com.personalphotomap.service;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import com.personalphotomap.model.AppUser;
import com.personalphotomap.model.Image;
import com.personalphotomap.repository.ImageRepository;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ImageUploadService {

    private static final Logger logger = LoggerFactory.getLogger(ImageUploadService.class);
    private static final Tika tika = new Tika(); // 🔥 Detector de MIME type

    @Autowired
    private S3Service s3Service;

    @Autowired
    private ImageRepository imageRepository;

    @Async
    public CompletableFuture<String> uploadAndSaveImage(MultipartFile file, String countryId, int year, AppUser user) {
        String threadName = Thread.currentThread().getName();

        try {
            logger.info("🚀 Iniciando upload da imagem: {} na thread: {}", file.getOriginalFilename(), threadName);
            System.out.println(
                    "🚀 [DEBUG] Iniciando upload de: " + file.getOriginalFilename() + " na thread: " + threadName);

            // Validação do tipo de arquivo
            String mimeType = tika.detect(file.getInputStream());
            if (!mimeType.equalsIgnoreCase("image/jpeg")) {
                logger.warn("⚠️ Arquivo inválido detectado: {} | MIME Type: {}", file.getOriginalFilename(), mimeType);
                System.out.println(
                        "⚠️ [DEBUG] Arquivo inválido: " + file.getOriginalFilename() + " | MIME Type: " + mimeType);
                return CompletableFuture.completedFuture(null);
            }

            // Criar nome único para a imagem
            String fileName = UUID.randomUUID().toString() + "_" + StringUtils.cleanPath(file.getOriginalFilename());

            // Simulação de tempo de upload para testar concorrência (remova isso em
            // produção)
            Thread.sleep(1000);

            // Upload para o S3
            String fileUrl = s3Service.uploadFile(file, fileName);

            logger.info("✅ Upload concluído: {} | URL: {} | Thread: {}", file.getOriginalFilename(), fileUrl,
                    threadName);
            System.out.println("✅ [DEBUG] Upload concluído para: " + file.getOriginalFilename() + " | URL: " + fileUrl
                    + " | Thread: " + threadName);

            // Salvar metadados no banco de dados
            Image image = new Image();
            image.setUser(user);
            image.setCountryId(countryId);
            image.setFileName(fileName);
            image.setFilePath(fileUrl);
            image.setYear(year);
            imageRepository.save(image);

            return CompletableFuture.completedFuture(fileUrl);
        } catch (IOException | InterruptedException e) {
            logger.error("❌ Erro no upload da imagem: {}", file.getOriginalFilename(), e);
            System.err.println(
                    "❌ [DEBUG] Erro ao processar: " + file.getOriginalFilename() + " | Erro: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

}
