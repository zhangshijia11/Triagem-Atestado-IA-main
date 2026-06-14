package br.com.soc.triagem_atestados.repository;

import br.com.soc.triagem_atestados.model.Equipamento;
import br.com.soc.triagem_atestados.model.Usuario;
import br.com.soc.triagem_atestados.model.enums.StatusEquipamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EquipamentoRepository extends JpaRepository<Equipamento, Long> {
    List<Equipamento> findByFuncionario(Usuario funcionario);
    List<Equipamento> findByStatus(StatusEquipamento status);
    long countByStatus(StatusEquipamento status);
}
