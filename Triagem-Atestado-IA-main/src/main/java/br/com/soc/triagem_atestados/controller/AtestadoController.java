package br.com.soc.triagem_atestados.controller;

import br.com.soc.triagem_atestados.model.Atestado;
import br.com.soc.triagem_atestados.model.Usuario;
import br.com.soc.triagem_atestados.model.enums.PerfilUsuario;
import br.com.soc.triagem_atestados.model.enums.StatusAtestado;
import br.com.soc.triagem_atestados.repository.AtestadoRepository;
import br.com.soc.triagem_atestados.repository.UsuarioRepository;
import br.com.soc.triagem_atestados.service.AtestadoService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


@Slf4j // 使用 Slf4j 日志
@Controller // Spring MVC 控制器
public class AtestadoController {

    @Autowired
    private AtestadoService atestadoService;

    @Autowired
    private AtestadoRepository atestadoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;
    

    // 打开病假单上传页面
   @GetMapping("/novo-atestado")
    public String telaNovoAtestado(HttpSession session) {
        // 验证是否为员工身份
        if (!PerfilUsuario.FUNCIONARIO.equals(session.getAttribute("perfil"))) {
    return "redirect:/";
}
        return "novo-atestado";
    }

    // 接收图片并用 AI 提取数据
   @PostMapping("/enviar-atestado")
public String receberAtestado(
    // 从 HTML 表单获取上传文件
        @RequestParam("ficheiroAtestado") MultipartFile arquivo, 
        HttpSession session, 
        RedirectAttributes redirectAttributes) {
    // O arquivo está vazio? Lança o erro
    if (arquivo.isEmpty()) {
        redirectAttributes.addFlashAttribute("erro", "请选择一个有效的文件。");
        return "redirect:/novo-atestado";
    }

    try {
        // 获取当前登录用户
        String emailLogado = (String) session.getAttribute("emailUsuario");
       // Como o comando findByEmail devolve um Optional, ou seja pode devolver um usuario ou não, o orElse faz com que se estiver vazia devolve null
        Usuario usuario = usuarioRepository.findByEmail(emailLogado).orElse(null);

        // 委托给 Service 处理
        atestadoService.processarESalvarAtestado(arquivo, usuario);

        redirectAttributes.addFlashAttribute("sucesso", "病假条已成功提交至企业审核队列！");
        return "redirect:/portal-colaborador";

    } catch (IllegalArgumentException e) {
            // Service 层异常处理
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/novo-atestado";
            
        } catch (Exception e) {
            log.error("Erro critico: Falha ao processar e extrair dados do atestado.", e);
            
            redirectAttributes.addFlashAttribute("erro", "服务器处理文件时出错，请稍后重试。");
            return "redirect:/novo-atestado";
        }
}

	    // 接收纯文本手动输入
	    @PostMapping("/enviar-texto")
	    public String receberTexto(
            @RequestParam(value = "anexo", required = false) MultipartFile anexo,
	            @RequestParam("textoAtestado") String textoAtestado,
	            HttpSession session,
	            RedirectAttributes redirectAttributes) {

	        if (textoAtestado == null || textoAtestado.isBlank()) {
	            redirectAttributes.addFlashAttribute("erro", "请输入病假条信息。");
	            return "redirect:/novo-atestado";
	        }

	        try {
	            String emailLogado = (String) session.getAttribute("emailUsuario");
	            Usuario usuario = usuarioRepository.findByEmail(emailLogado).orElse(null);

	            atestadoService.processarTextoESalvarAtestado(textoAtestado, anexo, usuario);

	            redirectAttributes.addFlashAttribute("sucesso", "病假条处理成功！");
	            return "redirect:/portal-colaborador";

	        } catch (IllegalArgumentException e) {
	            redirectAttributes.addFlashAttribute("erro", e.getMessage());
	            return "redirect:/novo-atestado";
	        } catch (Exception e) {
	            log.error("Erro ao processar texto do atestado.", e);
	            redirectAttributes.addFlashAttribute("erro", "处理文本时出错。");
	            return "redirect:/novo-atestado";
	        }
	    }

    // 企业看板路由
    @GetMapping("/portal-empresa")
    public String exibirPortalEmpresa(HttpSession session, Model model) {
        // 验证是否为企业身份
        if (!PerfilUsuario.EMPRESA.equals(session.getAttribute("perfil"))) {
            return "redirect:/";
        }
        // 查询所有病假条
        List<Atestado> listaAtestados = atestadoRepository.findAll();
        
        // 看板 KPI 卡片:
        // 总数
        long totalAtestados = listaAtestados.size();
        // 按状态筛选统计
        long pendentes = listaAtestados.stream().filter(a -> a.getStatus() == StatusAtestado.PENDENTE).count();
        long aprovados = listaAtestados.stream().filter(a -> a.getStatus() == StatusAtestado.APROVADO).count();
        long recusados = listaAtestados.stream().filter(a -> a.getStatus() == StatusAtestado.RECUSADO).count();
                
        /* Map<String, Long> frequenciaCid = listaAtestados.stream()
                .collect(Collectors.groupingBy(Atestado::getCid, Collectors.counting())); */

        // 将数据放入 Model 供模板使用
        model.addAttribute("atestados", listaAtestados);
        model.addAttribute("totalAtestados", totalAtestados);
        model.addAttribute("pendentes", pendentes);
        model.addAttribute("aprovados", aprovados);
        model.addAttribute("recusados", recusados);
        
       // model.addAttribute("chartLabels", frequenciaCid.keySet());
       // model.addAttribute("chartData", frequenciaCid.values());


        return "empresa";
    }

    // 人事操作：批准/拒绝
    @PostMapping("/atestado/aprovar/{id}")
    public String aprovarAtestado(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Atestado atestado = atestadoRepository.findById(id).orElse(null);
        if (atestado != null) {
            atestado.setStatus(StatusAtestado.APROVADO);
            atestadoRepository.save(atestado);
            redirectAttributes.addFlashAttribute("sucessoAction", "病假条 #" + id + " 已成功通过！");
        }
        return "redirect:/portal-empresa";
    }

    @PostMapping("/atestado/recusar/{id}")
    public String recusarAtestado(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Atestado atestado = atestadoRepository.findById(id).orElse(null);
        if (atestado != null) {
            atestado.setStatus(StatusAtestado.RECUSADO);
            atestadoRepository.save(atestado);
            redirectAttributes.addFlashAttribute("erroAction", "病假条 #" + id + " 已被管理员拒绝。");
        }
        return "redirect:/portal-empresa";
    }

    // 查看上传的病假单图片
    // {nome} 是 URL 路径变量
    @GetMapping("/atestado/arquivo/{nome}")
    @ResponseBody // @ResponseBody: 返回原始字节
    // Resource: 文件内容容器
    public ResponseEntity<Resource> visualizarArquivo(@PathVariable String nome) {
        try {
            // 拼接上传目录路径
            Path caminho = Paths.get("uploads").resolve(nome);
            // 转换为 Java Resource 对象
            Resource recurso = new UrlResource(caminho.toUri());
            
            // 文件存在且可读?
            if (recurso.exists() || recurso.isReadable()) {
                
                // 浏览器需要 Content-Type 头
                // 否则无法识别文件类型
                // 探测文件 MIME 类型
                String tipoConteudo = java.nio.file.Files.probeContentType(caminho);
                
                if (tipoConteudo == null) {   
                // 未知类型则设为下载
                tipoConteudo = "application/octet-stream";
                }
                
                // 构建 HTTP 200 响应
                return ResponseEntity.ok()
                        // Content-Disposition 内联显示
                        // 设置文件名
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + recurso.getFilename() + "\"")
                       // 设置内容类型
                        .header(HttpHeaders.CONTENT_TYPE, tipoConteudo)
                       // 将文件字节写入响应流
                        .body(recurso);
            } else {
                // 文件不存在返回 404
                // build() 用于空响应体
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            // 异常返回 400
            return ResponseEntity.badRequest().build();
        }
    }
}