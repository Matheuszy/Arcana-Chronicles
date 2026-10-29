package com.arcana.backend;

import com.arcana.backend.chat.imagem.ValidadorImagem;
import com.arcana.backend.chat.model.ImagemAnexada;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class ValidadorImagemTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16};
    static final byte[] GIF = {'G', 'I', 'F', '8', '9', 'a', 1, 0, 1, 0};
    static final byte[] WEBP = {'R', 'I', 'F', 'F', 26, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};

    private final ValidadorImagem validador = new ValidadorImagem();

    static ImagemAnexada comoDataUrl(String mimeDeclarado, byte[] conteudo) {
        return new ImagemAnexada(
                "data:" + mimeDeclarado + ";base64," + Base64.getEncoder().encodeToString(conteudo), null, null);
    }

    @Test
    void deveAceitarOsFormatosPermitidosDetectandoOTipoPeloConteudo() {
        assertEquals("image/png", validador.validar(comoDataUrl("image/png", PNG)).mimeType());
        assertEquals("image/jpeg", validador.validar(comoDataUrl("image/jpeg", JPEG)).mimeType());
        assertEquals("image/gif", validador.validar(comoDataUrl("image/gif", GIF)).mimeType());
        assertEquals("image/webp", validador.validar(comoDataUrl("image/webp", WEBP)).mimeType());
    }

    @Test
    void deveUsarOTipoRealMesmoQuandoOClienteDeclaraOutro() {
        ImagemAnexada validada = validador.validar(comoDataUrl("image/gif", PNG));

        assertEquals("image/png", validada.mimeType());
        assertTrue(validada.dataUrl().startsWith("data:image/png;base64,"));
        assertEquals(PNG.length, validada.sizeBytes());
    }

    @Test
    void deveRecusarArquivoQueNaoEhImagemMesmoComTipoDeImagemDeclarado() {
        byte[] script = "<script>alert(1)</script>".getBytes();

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> validador.validar(comoDataUrl("image/png", script)));

        assertTrue(erro.getMessage().startsWith("Formato não suportado"));
    }

    @Test
    void deveRecusarSvg() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".getBytes();

        assertThrows(IllegalArgumentException.class, () -> validador.validar(comoDataUrl("image/svg+xml", svg)));
    }

    @Test
    void deveRecusarImagemAcimaDoLimite() {
        byte[] grande = Arrays.copyOf(PNG, ValidadorImagem.LIMITE_BYTES_IMAGEM + 1);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> validador.validar(comoDataUrl("image/png", grande)));

        assertEquals("A imagem passa do limite de 2 MB.", erro.getMessage());
    }

    @Test
    void deveAceitarImagemExatamenteNoLimite() {
        byte[] noLimite = Arrays.copyOf(PNG, ValidadorImagem.LIMITE_BYTES_IMAGEM);

        assertEquals(ValidadorImagem.LIMITE_BYTES_IMAGEM, validador.validar(comoDataUrl("image/png", noLimite)).sizeBytes());
    }

    @Test
    void deveRecusarImagemAusenteOuBase64Invalido() {
        assertThrows(IllegalArgumentException.class, () -> validador.validar(null));
        assertThrows(IllegalArgumentException.class, () -> validador.validar(new ImagemAnexada("  ", null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> validador.validar(new ImagemAnexada("data:image/png;base64,@@@", null, null)));
    }
}
