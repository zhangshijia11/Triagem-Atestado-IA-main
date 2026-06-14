package br.com.soc.triagem_atestados.service;

import br.com.soc.triagem_atestados.model.Atestado;
import br.com.soc.triagem_atestados.model.Usuario;
import br.com.soc.triagem_atestados.model.enums.StatusAtestado;
import br.com.soc.triagem_atestados.repository.AtestadoRepository;
import br.com.soc.triagem_atestados.repository.CidRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.time.LocalDate;

@Service
public class AtestadoService {

    @Autowired
    private TriagemIAService iaService;

    @Autowired
    private AtestadoRepository atestadoRepository;

    @Autowired
    private CidRepository cidRepository;

    // Processa texto digitado manualmente (sem OCR)
    public void processarTextoESalvarAtestado(String textoAtestado, MultipartFile anexo, Usuario funcionario) throws Exception {
        String jsonExtraido = iaService.extrairDadosDeTexto(textoAtestado);
        // anexo pode ser null ou vazio (opcional)
        MultipartFile arquivoSalvar = (anexo != null && !anexo.isEmpty()) ? anexo : null;
        salvarAtestado(jsonExtraido, arquivoSalvar, funcionario);
    }

    // Processa imagem com OCR + IA
    public void processarESalvarAtestado(MultipartFile arquivo, Usuario funcionario) throws Exception {
        String dadosExtraidos = iaService.extrairDados(arquivo);
        salvarAtestado(dadosExtraidos, arquivo, funcionario);
    }

    // Lógica comum de parsing do JSON, validação e gravação
    private void salvarAtestado(String jsonBruto, MultipartFile arquivo, Usuario funcionario) throws Exception {
        String jsonLimpo = jsonBruto.replace("```json", "")
                                     .replace("```", "")
                                     .trim();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode;
        try {
            jsonNode = mapper.readTree(jsonLimpo);
        } catch (Exception e) {
            throw new IllegalArgumentException("AI 无法读取该文件，请上传更清晰的照片。");
        }

        if (!jsonNode.hasNonNull("diasAfastamento")) {
            throw new IllegalArgumentException("数据不完整，AI 无法识别请假天数。");
        }

        // 验证执业证号格式（允许为空）
        String crmExtraido = jsonNode.get("crm").asText().trim().toUpperCase();
        crmExtraido = crmExtraido.replaceAll("\\s+", "");

        if (!crmExtraido.isEmpty() &&
            !crmExtraido.matches("^\\d{10,20}$") &&
            !crmExtraido.matches("^\\d{4,10}[/-]?[A-Z]{2}$")) {
            throw new IllegalArgumentException("安全警告：提取到的执业证号 (" + crmExtraido + ") 格式无效。");
        }

        // 构建 Atestado 对象
        Atestado atestado = new Atestado();
        atestado.setCrm(crmExtraido);

        if (jsonNode.hasNonNull("cid") && !jsonNode.get("cid").asText().equals("null")) {
            String cidExtraido = jsonNode.get("cid").asText().trim().toUpperCase();

            if (!cidRepository.existsById(cidExtraido)) {
                throw new IllegalArgumentException("提取到的 ICD 编码 (" + cidExtraido + ") 不在官方数据库中，请核实文件。");
            }

            atestado.setCid(cidExtraido);
        } else {
            atestado.setCid("N/A");
        }

        int dias = jsonNode.get("diasAfastamento").asInt();
        atestado.setDiasAfastamento(dias);
        atestado.setDataEnvio(LocalDate.now());
        atestado.setStatus(StatusAtestado.PENDENTE);
        atestado.setFuncionario(funcionario);

        // 职业健康安全业务规则：依据《工伤保险条例》
        if (dias > 15) {
            atestado.setResumoDiagnostico("【审核警告】请假超过15天，建议申请劳动能力鉴定（依据《工伤保险条例》）。");
        } else {
            atestado.setResumoDiagnostico("短期病假（由用人单位支付病假工资）。");
        }

        // Salva arquivo se houver
        if (arquivo != null && !arquivo.isEmpty()) {
            Path caminhoPasta = Paths.get("uploads");
            if (!Files.exists(caminhoPasta)) {
                Files.createDirectories(caminhoPasta);
            }

            String nomeOriginal = arquivo.getOriginalFilename();
            String extensao = (nomeOriginal != null && nomeOriginal.contains("."))
                ? nomeOriginal.substring(nomeOriginal.lastIndexOf("."))
                : ".jpg";

            String nomeArquivoUnico = UUID.randomUUID().toString() + extensao;
            Path caminhoArquivo = caminhoPasta.resolve(nomeArquivoUnico);
            Files.copy(arquivo.getInputStream(), caminhoArquivo, StandardCopyOption.REPLACE_EXISTING);
            atestado.setNomeArquivo(nomeArquivoUnico);
        }

        atestadoRepository.save(atestado);
    }
}
