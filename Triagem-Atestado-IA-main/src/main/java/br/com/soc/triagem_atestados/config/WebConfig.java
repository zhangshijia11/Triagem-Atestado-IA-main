package br.com.soc.triagem_atestados.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AutenticacaoInterceptor autenticacaoInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(autenticacaoInterceptor)
                .addPathPatterns("/**") // /** 保护所有路由
                .excludePathPatterns(   // 无需登录的例外路由):
                        "/",            // 登录页面
                        "/login",       // A ação de fazer login
                        "/css/**",      // Arquivos de estilo (se não o login fica sem CSS)
                        "/js/**",       // Arquivos JavaScript
                        "/img/**",      // Imagens públicas
                        "/h2-console/**" // O console do banco H2
                );
    }
}