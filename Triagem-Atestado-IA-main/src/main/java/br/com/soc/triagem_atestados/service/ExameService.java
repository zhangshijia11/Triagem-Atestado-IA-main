package br.com.soc.triagem_atestados.service;

import br.com.soc.triagem_atestados.model.Exame;
import br.com.soc.triagem_atestados.model.Usuario;
import br.com.soc.triagem_atestados.model.enums.StatusExame;
import br.com.soc.triagem_atestados.model.enums.TipoExame;
import br.com.soc.triagem_atestados.repository.ExameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ExameService {

    @Autowired
    private ExameRepository exameRepository;

    // 员工预约体检
    public Exame agendarExame(Usuario funcionario, TipoExame tipoExame, LocalDate dataAgendamento, String clinica) {
        Exame exame = new Exame();
        exame.setFuncionario(funcionario);
        exame.setTipoExame(tipoExame);
        exame.setDataAgendamento(dataAgendamento);
        exame.setClinica(clinica);
        exame.setStatus(StatusExame.AGENDADO);
        return exameRepository.save(exame);
    }

    // 公司上传体检结果 + 报告文件
    public Exame registrarResultado(Long exameId, String resultado, MultipartFile arquivo,
                                     String crmMedico, StatusExame statusFinal) throws Exception {
        Exame exame = exameRepository.findById(exameId)
                .orElseThrow(() -> new IllegalArgumentException("未找到编号为 " + exameId + " 的体检记录。"));

        exame.setResultado(resultado);
        exame.setCrmMedico(crmMedico);
        exame.setDataRealizacao(LocalDate.now());
        exame.setStatus(statusFinal);

        // 保存报告文件
        if (arquivo != null && !arquivo.isEmpty()) {
            Path caminhoPasta = Paths.get("uploads");
            if (!Files.exists(caminhoPasta)) {
                Files.createDirectories(caminhoPasta);
            }
            String nomeOriginal = arquivo.getOriginalFilename();
            String extensao = (nomeOriginal != null && nomeOriginal.contains("."))
                    ? nomeOriginal.substring(nomeOriginal.lastIndexOf("."))
                    : ".pdf";
            String nomeArquivoUnico = UUID.randomUUID().toString() + extensao;
            Path caminhoArquivo = caminhoPasta.resolve(nomeArquivoUnico);
            Files.copy(arquivo.getInputStream(), caminhoArquivo, StandardCopyOption.REPLACE_EXISTING);
            exame.setNomeArquivo(nomeArquivoUnico);
        }

        return exameRepository.save(exame);
    }

    // 员工查看自己的体检记录
    public List<Exame> listarPorFuncionario(Usuario funcionario) {
        if (funcionario == null) return java.util.Collections.emptyList();
        List<Exame> result = exameRepository.findByFuncionario(funcionario);
        return result != null ? result : java.util.Collections.emptyList();
    }

    // 公司查看全部体检记录
    public List<Exame> listarTodos() {
        List<Exame> result = exameRepository.findAll();
        return result != null ? result : java.util.Collections.emptyList();
    }
}
