package br.com.soc.triagem_atestados.config;

import br.com.soc.triagem_atestados.model.DominioCid;
import br.com.soc.triagem_atestados.repository.CidRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Configuration
public class CargaCidConfig {

    @Bean
    CommandLineRunner carregarCid(CidRepository cidRepository) {
        return args -> {
            // 仅在 ICD 编码表为空时加载
            if (cidRepository.count() == 0) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(new ClassPathResource("cid10.csv").getInputStream()))) {

                    String linha;
                    boolean primeiraLinha = true;

                    while ((linha = br.readLine()) != null) {
                        // 跳过表头 (codigo;descricao)
                        if (primeiraLinha) {
                            primeiraLinha = false;
                            continue;
                        }

                        // 按分号切分每行
                        String[] colunas = linha.split(";");

                        // 确保该行包含编码和描述
                        if (colunas.length >= 2) {
                            DominioCid cid = new DominioCid();
                            cid.setCodigo(colunas[0].trim().toUpperCase());
                            cid.setDescricao(colunas[1].trim());

                            cidRepository.save(cid); // 保存到数据库
                        }
                    }
                    System.out.println(">> ICD-10 编码表加载成功！ <<");

                } catch (Exception e) {
                    System.out.println("加载 ICD 编码文件失败: " + e.getMessage());
                }
            }
        };
    }
}