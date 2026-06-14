package br.com.soc.triagem_atestados.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class DominioCid {

    @Id
    private String codigo; // Exemplo: J06.9
    
    private String descricao; // Exemplo: Infeccao aguda das vias aereas...
}