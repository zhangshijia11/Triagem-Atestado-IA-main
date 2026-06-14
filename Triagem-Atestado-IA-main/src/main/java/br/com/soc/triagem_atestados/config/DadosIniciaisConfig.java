package br.com.soc.triagem_atestados.config;

import br.com.soc.triagem_atestados.model.Usuario;
import br.com.soc.triagem_atestados.model.enums.PerfilUsuario;
import br.com.soc.triagem_atestados.repository.UsuarioRepository;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DadosIniciaisConfig {

    @Bean
    CommandLineRunner iniciarBanco(UsuarioRepository usuarioRepository) {
        return args -> {
            // 仅在数据库为空时初始化测试用户
            if (usuarioRepository.count() == 0) {
                Usuario func = new Usuario();
                func.setNome("张丽");
                func.setEmail("zhangli@zhian.com");
                func.setSenha(BCrypt.hashpw("123", BCrypt.gensalt()));
                func.setPerfil(PerfilUsuario.FUNCIONARIO);
                usuarioRepository.save(func);

                Usuario emp = new Usuario();
                emp.setNome("职安通科技");
                emp.setEmail("hr@zhian.com");
                emp.setSenha(BCrypt.hashpw("admin", BCrypt.gensalt()));
                emp.setPerfil(PerfilUsuario.EMPRESA);
                usuarioRepository.save(emp);

                System.out.println(">> 测试用户已加载到 H2 数据库！ <<");
            }
        };
    }
}