package com.arcana.backend.presence.model;

public record MensagemDoCanal(TipoMensagemDoCanal type, Object payload) {

    public record PresencaDoAmigo(Long usuarioId, EstadoPresenca estado) {}

    public static MensagemDoCanal presenca(Long usuarioId, EstadoPresenca estado) {
        return new MensagemDoCanal(TipoMensagemDoCanal.PRESENCA, new PresencaDoAmigo(usuarioId, estado));
    }

    public static MensagemDoCanal amizadesAlteradas() {
        return new MensagemDoCanal(TipoMensagemDoCanal.AMIZADES_ALTERADAS, null);
    }
}
