package com.arcana.backend.chat.model;

public record ImagemAnexada(String dataUrl, String mimeType, Integer sizeBytes, String descricao) {

    public ImagemAnexada(String dataUrl, String mimeType, Integer sizeBytes) {
        this(dataUrl, mimeType, sizeBytes, null);
    }
}
