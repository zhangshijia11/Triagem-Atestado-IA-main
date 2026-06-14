package br.com.soc.triagem_atestados.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service
public class TriagemIAService {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String apiUrl;
    private final String modelName;
    private final ObjectMapper objectMapper;
    private final String tesseractPath;
    private final String tessdataDir;

    public TriagemIAService(
            @Value("${deepseek.api.key}") String apiKey,
            @Value("${deepseek.api.url:https://api.deepseek.com/v1/chat/completions}") String apiUrl,
            @Value("${deepseek.api.model:deepseek-v4-flash}") String modelName,
            @Value("${tesseract.path:C:/Program Files/Tesseract-OCR/tesseract.exe}") String tesseractPath,
            @Value("${tesseract.tessdata.dir:}") String tessdataDir) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.modelName = modelName;
        this.objectMapper = new ObjectMapper();
        this.tesseractPath = tesseractPath;
        // Usa diretório do usuário para dados de idioma (por + eng)
        this.tessdataDir = (tessdataDir != null && !tessdataDir.isBlank())
            ? tessdataDir
            : System.getProperty("user.home") + "/tessdata";
    }

    // Processa texto digitado manualmente — envia direto para DeepSeek (sem OCR)
    public String extrairDadosDeTexto(String textoAtestado) throws Exception {
        return enviarParaDeepSeek(textoAtestado);
    }

    public String extrairDados(MultipartFile arquivo) throws Exception {
        // Passo 1: OCR - Extrai texto da imagem usando Tesseract
        String textoOCR = executarOCR(arquivo);

        // Se o OCR não extraiu nada útil, tenta uma abordagem alternativa
        if (textoOCR == null || textoOCR.isBlank() || textoOCR.length() < 10) {
            throw new Exception("OCR 未能从图片中提取文字，请使用更清晰、光线更好的照片。");
        }

        // Passo 2: Envia o texto extraído para a DeepSeek estruturar os dados em JSON
        return enviarParaDeepSeek(textoOCR);
    }

    private String executarOCR(MultipartFile arquivo) throws Exception {
        Path tempDir = Files.createTempDirectory("ocr_");
        Path imagemFile = tempDir.resolve("atestado.png");
        Path outputFile = tempDir.resolve("resultado");

        try {
            Files.copy(arquivo.getInputStream(), imagemFile, StandardCopyOption.REPLACE_EXISTING);

            // 使用中文简体 + 英文 OCR 语言包
            // PSM 3 = Fully automatic page segmentation
            ProcessBuilder pb = new ProcessBuilder(
                tesseractPath,
                imagemFile.toString(),
                outputFile.toString(),
                "-l", "chi_sim+eng",
                "--psm", "3",
                "--tessdata-dir", tessdataDir
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder logOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logOutput.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            System.out.println("[OCR] Tesseract exit code: " + exitCode);

            Path txtOutput = Paths.get(outputFile.toString() + ".txt");
            String texto;
            if (Files.exists(txtOutput)) {
                texto = Files.readString(txtOutput);
            } else if (Files.exists(outputFile)) {
                texto = Files.readString(outputFile);
            } else {
                texto = "";
            }

            System.out.println("[OCR] Texto extraído (" + texto.length() + " chars): " + texto.substring(0, Math.min(200, texto.length())));
            return texto.trim();
        } finally {
            try { Files.deleteIfExists(imagemFile); } catch (Exception ignored) {}
            try { Files.deleteIfExists(outputFile); } catch (Exception ignored) {}
            try { Files.deleteIfExists(Paths.get(outputFile.toString() + ".txt")); } catch (Exception ignored) {}
            try { Files.deleteIfExists(tempDir); } catch (Exception ignored) {}
        }
    }

    private String enviarParaDeepSeek(String textoOCR) throws Exception {
        String prompt = """
            你是一位医学文件分析专家。下面是从中国医院出具的病假单中通过 OCR 提取的文字。
            OCR 可能包含很多错误——字母混淆、文字错乱、数字识别不准。请运用你的医学文件知识，
            在存在噪声的情况下识别出关键信息。

            病假单 OCR 文字：
            ---
            %s
            ---

            请提取并严格返回一个 JSON 对象，包含以下字段：
            - "crm": 医师执业证书编号（执业证号），如 "110000000000000"。寻找 "执业证号"、
              "医师执业证"、"证书编号" 附近 15-18 位数字。
            - "cid": ICD 疾病编码（如 "M54.5"、"J06.9"），若未找到则为 null。
            - "diasAfastamento": 建议休假天数的整数值。寻找 "建议休假"、"休息"、"天"、
              "休假天数" 附近的数字。

            规则：
            1. 若未找到执业证号，返回空字符串 ""。
            2. 若未找到 ICD 编码，返回 null。
            3. 若未找到天数，返回 0。
            4. 不得编造文字中不存在的数据。
            5. 只返回 JSON，不要 markdown，不要解释。
            """.formatted(textoOCR);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("messages", List.of(
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", 0.0);
        requestBody.put("max_tokens", 1000); // Mais tokens para o modelo de raciocínio

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl, HttpMethod.POST, request, String.class);

            String responseBody = response.getBody();
            System.out.println("[DeepSeek] Raw response (first 500 chars): " +
                (responseBody != null ? responseBody.substring(0, Math.min(500, responseBody.length())) : "null"));

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode message = root.path("choices").get(0).path("message");

            // V4 Pro coloca a resposta em reasoning_content
            String content = message.path("content").asText();
            String reasoning = message.path("reasoning_content").asText();

            System.out.println("[DeepSeek] content length: " + (content != null ? content.length() : 0));
            System.out.println("[DeepSeek] reasoning_content length: " + (reasoning != null ? reasoning.length() : 0));

            // Usa o que tiver conteúdo útil
            String result;
            if (content != null && !content.isBlank()) {
                result = content;
            } else if (reasoning != null && !reasoning.isBlank()) {
                result = reasoning;
                System.out.println("[DeepSeek] Usando reasoning_content");
            } else {
                System.out.println("[DeepSeek] ERRO: resposta vazia!");
                throw new Exception("A IA retornou uma resposta vazia. O OCR não extraiu texto suficiente da imagem.");
            }

            // Extrai o JSON da resposta (pode vir com texto ao redor)
            result = extrairJSON(result);

            System.out.println("[DeepSeek] JSON final: " + result);
            return result;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("resposta vazia")) {
                throw e;
            }
            System.err.println("[DeepSeek] Error: " + e.getMessage());
            throw new Exception("Falha ao comunicar com a IA: " + e.getMessage(), e);
        }
    }

    private String extrairJSON(String texto) {
        // Tenta encontrar um objeto JSON no texto (entre { e })
        int inicio = texto.indexOf('{');
        int fim = texto.lastIndexOf('}');
        if (inicio >= 0 && fim > inicio) {
            String json = texto.substring(inicio, fim + 1).trim();
            // Remove marcações markdown
            json = json.replace("```json", "").replace("```", "").trim();
            return json;
        }
        return texto.trim();
    }
}
