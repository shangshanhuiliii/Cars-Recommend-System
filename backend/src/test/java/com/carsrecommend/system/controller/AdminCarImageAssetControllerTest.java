package com.carsrecommend.system.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "app.auth.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:cars_car_image_asset;MODE=MySQL;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "app.storage.car-image-root=target/test-uploads/car-image-asset-controller-test",
        "app.storage.car-image-public-path=/uploads/car-images",
        "app.storage.car-image-max-size-bytes=5242880"
})
@AutoConfigureMockMvc
@Sql(
        scripts = {"/db/schema.sql", "/db/seed-data.sql"},
        config = @SqlConfig(encoding = "UTF-8")
)
class AdminCarImageAssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void carImageAssetLifecycleCoversUploadAuditAndSoftDelete() throws Exception {
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "../not-image.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "not an image".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/admin/car-images")
                        .file(textFile)
                        .param("carId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        MockMultipartFile oversizeFile = new MockMultipartFile(
                "file",
                "large.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[(5 * 1024 * 1024) + 1]);
        mockMvc.perform(multipart("/api/admin/car-images")
                        .file(oversizeFile)
                        .param("carId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        JsonNode approvedUpload = uploadImage("valid.png", MediaType.IMAGE_PNG_VALUE, pngImage(640, 360));
        long approvedAssetId = approvedUpload.path("id").asLong();
        String approvedPublicUrl = approvedUpload.path("publicUrl").asText();
        assertEquals("PENDING", approvedUpload.path("auditStatus").asText());
        assertTrue(approvedPublicUrl.startsWith("/uploads/car-images/"));
        assertEquals("", jdbcTemplate.queryForObject("SELECT image_url FROM car_model WHERE id = 1", String.class));

        mockMvc.perform(get("/api/admin/car-images")
                        .param("page", "1")
                        .param("size", "10")
                        .param("carId", "1")
                        .param("auditStatus", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(approvedAssetId));

        mockMvc.perform(put("/api/admin/car-images/{id}/audit", approvedAssetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auditStatus\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.auditStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.publicUrl").value(approvedPublicUrl));
        assertEquals(approvedPublicUrl,
                jdbcTemplate.queryForObject("SELECT image_url FROM car_model WHERE id = 1", String.class));

        mockMvc.perform(delete("/api/admin/car-images/{id}", approvedAssetId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("approved image is currently used by the car model"));

        JsonNode rejectedUpload = uploadImage("reject-me.png", MediaType.IMAGE_PNG_VALUE, pngImage(320, 240));
        long rejectedAssetId = rejectedUpload.path("id").asLong();
        mockMvc.perform(put("/api/admin/car-images/{id}/audit", rejectedAssetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content("{\"auditStatus\":\"REJECTED\",\"rejectReason\":\"图片不清晰\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.auditStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectReason").value("图片不清晰"));
        assertEquals(approvedPublicUrl,
                jdbcTemplate.queryForObject("SELECT image_url FROM car_model WHERE id = 1", String.class));

        mockMvc.perform(delete("/api/admin/car-images/{id}", rejectedAssetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        Integer deletedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM car_image_asset WHERE id = ? AND deleted = TRUE",
                Integer.class,
                rejectedAssetId);
        assertEquals(1, deletedCount);
    }

    private JsonNode uploadImage(String filename, String contentType, byte[] bytes) throws Exception {
        MockMultipartFile image = new MockMultipartFile("file", filename, contentType, bytes);
        MvcResult result = mockMvc.perform(multipart("/api/admin/car-images")
                        .file(image)
                        .param("carId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.carId").value(1))
                .andExpect(jsonPath("$.data.auditStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.width").isNumber())
                .andExpect(jsonPath("$.data.height").isNumber())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
    }

    private byte[] pngImage(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(35, 99, 235));
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(width / 4, height / 4, width / 2, height / 2);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }
}
