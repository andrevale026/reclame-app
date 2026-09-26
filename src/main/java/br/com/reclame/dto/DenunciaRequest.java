package br.com.reclame.dto;

import java.util.Map;

public record DenunciaRequest(String modo, String categoria, Map<String, Object> dados, String email) {}
