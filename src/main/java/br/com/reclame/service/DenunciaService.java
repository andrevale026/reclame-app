package br.com.reclame.service;

import br.com.reclame.dto.DenunciaRequest;
import br.com.reclame.model.Denuncia;
import br.com.reclame.repository.DenunciaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DenunciaService {
    public static final Map<String, String> CATEGORIES = Map.of(
            "assedio", "Assédio", "aluno", "Aluno", "professor", "Professor",
            "tecnico", "Técnico", "infraestrutura", "Infraestrutura", "outros", "Outros");
    private static final Set<String> INFRA_AREAS = Set.of("Área Comum", "Ar Condicionado", "Banheiros", "Bebedouros", "Pintura", "Piso", "Restaurante", "Sala de Aula", "Outros");
    private static final Set<String> OCCUPATIONS = Set.of("Aluno", "Professor", "Técnico", "Outros");

    private final DenunciaRepository repository;
    private final FernetService fernet;
    private final ObjectMapper mapper;
    private final JavaMailSender mailSender;

    @Value("${reclame.email.ouvidoria:}") private String emailOuvidoria;
    @Value("${reclame.email.coordenacao:}") private String emailCoordenacao;
    @Value("${reclame.email.prefeitura-academica:}") private String emailPrefeitura;
    @Value("${spring.mail.host:}") private String smtpHost;
    @Value("${spring.mail.username:}") private String smtpUser;
    @Value("${spring.mail.password:}") private String smtpPassword;
    @Value("${spring.mail.from:${spring.mail.username:}}") private String smtpFrom;

    public DenunciaService(DenunciaRepository repository, FernetService fernet, ObjectMapper mapper, JavaMailSender mailSender) {
        this.repository = repository; this.fernet = fernet; this.mapper = mapper; this.mailSender = mailSender;
    }

    public Denuncia create(DenunciaRequest request) {
        Validated v = validate(request);
        String protocolo = UUID.randomUUID().toString();
        String json;
        try { json = mapper.writeValueAsString(v.dados()); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException("Dados inválidos."); }

        Denuncia d = new Denuncia();
        d.setProtocolo(protocolo); d.setModo(v.modo()); d.setCategoria(v.categoria());
        d.setDadosCriptografados(fernet.encrypt(json));
        d.setEmailDenuncianteCriptografado(fernet.encrypt(v.email()));
        d = repository.saveAndFlush(d);

        String body = formatEmailBody(protocolo, v.modo(), v.categoria(), v.dados());
        sendEmail(destinationFor(v.categoria()), "RECLAME - Nova denúncia [" + CATEGORIES.get(v.categoria()) + "] - " + protocolo, body);
        if ("identificado".equals(v.modo()) && !v.email().isBlank())
            sendEmail(v.email(), "RECLAME - Cópia da denúncia - " + protocolo, body);
        return d;
    }

    public Optional<Denuncia> findByProtocol(String protocol) {
        try { return repository.findByProtocolo(UUID.fromString(protocol).toString()); }
        catch (Exception e) { return Optional.empty(); }
    }

    private Validated validate(DenunciaRequest r) {
        if (r == null) throw new IllegalArgumentException("JSON inválido ou ausente.");
        String modo = Objects.toString(r.modo(), "");
        String categoria = Objects.toString(r.categoria(), "");
        Map<String,Object> dados = r.dados() == null ? new LinkedHashMap<>() : r.dados();
        String email = Objects.toString(r.email(), "").trim();
        if (!Set.of("anonimo", "identificado").contains(modo)) throw new IllegalArgumentException("Modo de acesso inválido.");
        if (!CATEGORIES.containsKey(categoria)) throw new IllegalArgumentException("Categoria inválida.");
        if (modo.equals("anonimo") && categoria.equals("assedio")) throw new IllegalArgumentException("A denúncia de assédio exige identificação das partes.");
        if (modo.equals("identificado")) {
            if (email.isBlank()) throw new IllegalArgumentException("O e-mail do denunciante é obrigatório na entrada identificada.");
            if (email.length() > 254) throw new IllegalArgumentException("O e-mail informado é muito longo.");
            if (!email.contains("@")) throw new IllegalArgumentException("Informe um e-mail válido.");
        } else email = "";

        switch (categoria) {
            case "assedio" -> { require(dados,"assediado","Nome do assediado"); require(dados,"assediador","Nome do assediador"); require(dados,"ocupacao","Ocupação do assediador"); require(dados,"local","Local do assédio"); require(dados,"data","Data do acontecimento"); require(dados,"hora","Horário aproximado"); require(dados,"relato","Relato do fato"); if (!OCCUPATIONS.contains(text(dados,"ocupacao"))) throw new IllegalArgumentException("Ocupação do assediador inválida."); }
            case "aluno" -> { require(dados,"curso","Nome do curso"); require(dados,"aluno","Nome do aluno denunciado"); require(dados,"relato","Relato do fato"); }
            case "professor" -> { require(dados,"curso","Nome do curso"); require(dados,"professor","Nome do professor denunciado"); require(dados,"disciplina","Disciplina"); require(dados,"relato","Relato do fato"); }
            case "tecnico" -> { require(dados,"tecnico","Nome do técnico"); require(dados,"relato","Relato do fato"); }
            case "infraestrutura" -> { if (!INFRA_AREAS.contains(text(dados,"area"))) throw new IllegalArgumentException("Selecione uma área de infraestrutura."); require(dados,"relato","Relato do fato"); }
            case "outros" -> require(dados,"relato","Relato do fato");
        }
        dados.forEach((k,v) -> { if (v instanceof String s && s.length() > 5000) throw new IllegalArgumentException("O campo '"+k+"' ultrapassa o limite permitido."); });
        return new Validated(modo,categoria,dados,email);
    }

    private static String text(Map<String,Object> dados, String key) { return Objects.toString(dados.get(key), "").trim(); }
    private static void require(Map<String,Object> dados, String key, String label) { if (text(dados,key).isBlank()) throw new IllegalArgumentException(label + " é obrigatório."); }

    private String destinationFor(String categoria) {
        return switch (categoria) { case "assedio" -> emailOuvidoria; case "infraestrutura" -> emailPrefeitura; default -> emailCoordenacao; };
    }

    private void sendEmail(String to, String subject, String body) {
        if (smtpHost.isBlank() || smtpUser.isBlank() || smtpPassword.isBlank() || smtpFrom.isBlank() || to == null || to.isBlank())
            throw new IllegalStateException("SMTP ou destinatário institucional não configurado.");
        SimpleMailMessage msg = new SimpleMailMessage(); msg.setFrom(smtpFrom); msg.setTo(to); msg.setSubject(subject); msg.setText(body); mailSender.send(msg);
    }

    private String formatEmailBody(String protocolo, String modo, String categoria, Map<String,Object> dados) {
        Map<String,String> labels = Map.ofEntries(Map.entry("assediado","Nome do assediado"),Map.entry("assediador","Nome do assediador"),Map.entry("ocupacao","Ocupação do assediador"),Map.entry("local","Local do assédio"),Map.entry("data","Data do acontecimento"),Map.entry("hora","Horário aproximado"),Map.entry("curso","Nome do curso"),Map.entry("aluno","Nome do aluno denunciado"),Map.entry("professor","Nome do professor denunciado"),Map.entry("disciplina","Disciplina"),Map.entry("tecnico","Nome do técnico"),Map.entry("area","Área"),Map.entry("relato","Relato do fato"));
        StringBuilder b = new StringBuilder("RECLAME - NOVA DENÚNCIA\n\nProtocolo: ").append(protocolo).append("\nModo: ").append(modo.equals("identificado")?"Identificado":"Anônimo").append("\nCategoria: ").append(CATEGORIES.get(categoria)).append("\n\n");
        dados.forEach((k,v) -> b.append(labels.getOrDefault(k,k)).append(": ").append(v).append("\n\n")); return b.toString();
    }

    private record Validated(String modo, String categoria, Map<String,Object> dados, String email) {}
}
