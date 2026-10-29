package com.arcana.backend.chat.imagem;

import com.arcana.backend.chat.model.ImagemAnexada;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Base64;

@Component
public class ValidadorImagem {

    public static final int LIMITE_BYTES_IMAGEM = 2 * 1024 * 1024;
    public static final int LIMITE_TAMANHO_MENSAGEM_BYTES = 4 * 1024 * 1024;

    private static final int LIMITE_CARACTERES_BASE64 = ((LIMITE_BYTES_IMAGEM + 2) / 3) * 4;
    private static final String MARCADOR_BASE64 = ";base64,";

    private static final byte[] ASSINATURA_PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] ASSINATURA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] ASSINATURA_GIF87 = {'G', 'I', 'F', '8', '7', 'a'};
    private static final byte[] ASSINATURA_GIF89 = {'G', 'I', 'F', '8', '9', 'a'};
    private static final byte[] ASSINATURA_RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] ASSINATURA_WEBP = {'W', 'E', 'B', 'P'};

    public ImagemAnexada validar(ImagemAnexada recebida) {
        if (recebida == null || recebida.dataUrl() == null || recebida.dataUrl().isBlank()) {
            throw new IllegalArgumentException("Envie uma imagem.");
        }

        String base64 = extrairBase64(recebida.dataUrl());
        if (base64.length() > LIMITE_CARACTERES_BASE64) {
            throw new IllegalArgumentException(mensagemLimite());
        }

        byte[] conteudo;
        try {
            conteudo = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Não foi possível ler a imagem enviada.");
        }

        if (conteudo.length == 0) {
            throw new IllegalArgumentException("Envie uma imagem.");
        }
        if (conteudo.length > LIMITE_BYTES_IMAGEM) {
            throw new IllegalArgumentException(mensagemLimite());
        }

        String mimeType = detectarTipo(conteudo);
        return new ImagemAnexada(
                "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(conteudo),
                mimeType,
                conteudo.length);
    }

    private String extrairBase64(String dataUrl) {
        int marcador = dataUrl.indexOf(MARCADOR_BASE64);
        String base64 = marcador >= 0 ? dataUrl.substring(marcador + MARCADOR_BASE64.length()) : dataUrl;
        return base64.strip();
    }

    private String detectarTipo(byte[] conteudo) {
        if (comecaCom(conteudo, ASSINATURA_PNG, 0)) return "image/png";
        if (comecaCom(conteudo, ASSINATURA_JPEG, 0)) return "image/jpeg";
        if (comecaCom(conteudo, ASSINATURA_GIF87, 0) || comecaCom(conteudo, ASSINATURA_GIF89, 0)) return "image/gif";
        if (comecaCom(conteudo, ASSINATURA_RIFF, 0) && comecaCom(conteudo, ASSINATURA_WEBP, 8)) return "image/webp";
        throw new IllegalArgumentException("Formato não suportado. Envie uma imagem PNG, JPEG, WEBP ou GIF.");
    }

    private boolean comecaCom(byte[] conteudo, byte[] assinatura, int posicao) {
        return conteudo.length >= posicao + assinatura.length
                && Arrays.equals(conteudo, posicao, posicao + assinatura.length, assinatura, 0, assinatura.length);
    }

    private String mensagemLimite() {
        return "A imagem passa do limite de " + (LIMITE_BYTES_IMAGEM / (1024 * 1024)) + " MB.";
    }
}
