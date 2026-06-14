package br.com.soc.triagem_atestados.controller;

import br.com.soc.triagem_atestados.model.Exame;
import br.com.soc.triagem_atestados.model.Usuario;
import br.com.soc.triagem_atestados.model.enums.PerfilUsuario;
import br.com.soc.triagem_atestados.model.enums.StatusExame;
import br.com.soc.triagem_atestados.model.enums.TipoExame;
import br.com.soc.triagem_atestados.repository.ExameRepository;
import br.com.soc.triagem_atestados.repository.UsuarioRepository;
import br.com.soc.triagem_atestados.service.ExameService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Controller
public class ExameController {

    @Autowired
    private ExameService exameService;

    @Autowired
    private ExameRepository exameRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // ==================== 员工端 ====================

    // 员工体检首页
    @GetMapping("/meus-exames")
    public String telaMeusExames(HttpSession session, Model model) {
        if (!PerfilUsuario.FUNCIONARIO.equals(session.getAttribute("perfil"))) {
            return "redirect:/";
        }
        String emailLogado = (String) session.getAttribute("emailUsuario");
        Usuario usuario = usuarioRepository.findByEmail(emailLogado).orElse(null);
        List<Exame> meusExames = (usuario != null)
                ? exameService.listarPorFuncionario(usuario)
                : java.util.Collections.emptyList();
        model.addAttribute("meusExames", meusExames);
        model.addAttribute("tiposExame", TipoExame.values());
        return "exames";
    }

    // 员工预约体检
    @PostMapping("/agendar-exame")
    public String agendarExame(
            @RequestParam("tipoExame") TipoExame tipoExame,
            @RequestParam("dataAgendamento") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAgendamento,
            @RequestParam("clinica") String clinica,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String emailLogado = (String) session.getAttribute("emailUsuario");
        Usuario usuario = usuarioRepository.findByEmail(emailLogado).orElse(null);

        try {
            exameService.agendarExame(usuario, tipoExame, dataAgendamento, clinica);
            redirectAttributes.addFlashAttribute("sucesso", "体检已成功预约！");
        } catch (Exception e) {
            log.error("体检预约失败", e);
            redirectAttributes.addFlashAttribute("erro", "预约失败，请稍后重试。");
        }
        return "redirect:/meus-exames";
    }

    // ==================== 企业端 ====================

    // 企业体检管理面板
    @GetMapping("/portal-exames")
    public String telaExamesEmpresa(HttpSession session, Model model) {
        if (!PerfilUsuario.EMPRESA.equals(session.getAttribute("perfil"))) {
            return "redirect:/";
        }
        List<Exame> todosExames = exameService.listarTodos();
        if (todosExames == null) {
            todosExames = java.util.Collections.emptyList();
        }

        long totalExames = todosExames.size();
        long agendados = todosExames.stream().filter(e -> e.getStatus() == StatusExame.AGENDADO).count();
        long aptos = todosExames.stream().filter(e -> e.getStatus() == StatusExame.APTO).count();
        long inaptos = todosExames.stream().filter(e -> e.getStatus() == StatusExame.INAPTO).count();

        model.addAttribute("exames", todosExames);
        model.addAttribute("totalExames", totalExames);
        model.addAttribute("agendados", agendados);
        model.addAttribute("aptos", aptos);
        model.addAttribute("inaptos", inaptos);

        return "exames-empresa";
    }

    // 企业上传体检结果
    @PostMapping("/exame/resultado/{id}")
    public String registrarResultado(
            @PathVariable Long id,
            @RequestParam("resultado") String resultado,
            @RequestParam("crmMedico") String crmMedico,
            @RequestParam("statusFinal") StatusExame statusFinal,
            @RequestParam(value = "arquivo", required = false) MultipartFile arquivo,
            RedirectAttributes redirectAttributes) {

        try {
            exameService.registrarResultado(id, resultado, arquivo, crmMedico, statusFinal);
            redirectAttributes.addFlashAttribute("sucessoAction", "体检 #" + id + " 结果已录入！");
        } catch (Exception e) {
            log.error("体检结果录入失败", e);
            redirectAttributes.addFlashAttribute("erroAction", "录入失败：" + e.getMessage());
        }
        return "redirect:/portal-exames";
    }

    // 查看体检报告文件
    @GetMapping("/exame/arquivo/{nome}")
    @ResponseBody
    public ResponseEntity<Resource> visualizarArquivoExame(@PathVariable String nome) {
        try {
            Path caminho = Paths.get("uploads").resolve(nome);
            Resource recurso = new UrlResource(caminho.toUri());

            if (recurso.exists() || recurso.isReadable()) {
                String tipoConteudo = java.nio.file.Files.probeContentType(caminho);
                if (tipoConteudo == null) {
                    tipoConteudo = "application/octet-stream";
                }
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + recurso.getFilename() + "\"")
                        .header(HttpHeaders.CONTENT_TYPE, tipoConteudo)
                        .body(recurso);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
