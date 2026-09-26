package br.com.reclame.controller;

import br.com.reclame.dto.DenunciaRequest;
import br.com.reclame.model.Denuncia;
import br.com.reclame.service.DenunciaService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DenunciaController {
    private final DenunciaService service;
    public DenunciaController(DenunciaService service) { this.service = service; }

    @GetMapping("/health") public Map<String,Object> health() { return Map.of("ok",true,"app","RECLAME"); }

    @GetMapping("/denuncias/{protocolo}")
    public ResponseEntity<Map<String,Object>> status(@PathVariable String protocolo) {
        return service.findByProtocol(protocolo)
                .<ResponseEntity<Map<String,Object>>>map(d -> ResponseEntity.ok(Map.of("ok",true,"protocolo",d.getProtocolo(),"categoria",DenunciaService.CATEGORIES.getOrDefault(d.getCategoria(),d.getCategoria()),"status","Recebida","criado_em",d.getCriadoEm().toString())))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("ok",false,"erro","Protocolo não encontrado.")));
    }

    @PostMapping("/denuncias")
    public ResponseEntity<Map<String,Object>> create(@RequestBody(required=false) DenunciaRequest request) {
        try {
            Denuncia d = service.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("ok",true,"protocolo",d.getProtocolo(),"mensagem","Denúncia registrada com sucesso."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("ok",false,"erro",e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok",false,"erro","Não foi possível concluir o envio. Verifique a configuração do banco e do e-mail."));
        }
    }
}
