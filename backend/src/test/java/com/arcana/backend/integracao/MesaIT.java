package com.arcana.backend.integracao;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MesaIT extends IntegracaoTest {

    private String mesa(Long mesaId) {
        return "/api/tables/" + mesaId;
    }

    @Test
    void deveCriarMesaComOMestreComoParticipante() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);

        getComo(mestre.token(), mesa(mesaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABERTA"))
                .andExpect(jsonPath("$.masterId").value(mestre.id()))
                .andExpect(jsonPath("$.participants", hasSize(1)))
                .andExpect(jsonPath("$.participants[0].role").value("MESTRE"));
    }

    @Test
    void deveImpedirJogadorDeCriarMesa() throws Exception {
        Conta jogador = registrar("JOGADOR");

        postComo(jogador.token(), "/api/tables", Map.of("name", "Mesa pirata", "description", "x"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveRecusarMesaSemNome() throws Exception {
        Conta mestre = registrar("MESTRE");

        postComo(mestre.token(), "/api/tables", Map.of("name", "", "description", "x"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveIncluirJogadorApenasDepoisDoAceiteDoMestre() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        Conta jogador = registrar("JOGADOR");
        Long personagemId = criarPersonagem(jogador, "PLAYER");

        Long solicitacaoId = solicitarEntrada(jogador, mesaId, personagemId);
        getComo(mestre.token(), mesa(mesaId)).andExpect(jsonPath("$.participants", hasSize(1)));
        getComo(jogador.token(), mesa(mesaId) + "/solicitacoes/minha")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"));
        postComo(jogador.token(), mesa(mesaId) + "/solicitacoes", Map.of("characterId", personagemId))
                .andExpect(status().isConflict());

        getComo(mestre.token(), mesa(mesaId) + "/solicitacoes")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(solicitacaoId.intValue())));
        postComo(mestre.token(), mesa(mesaId) + "/solicitacoes/" + solicitacaoId + "/aceitar", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACEITA"));

        getComo(jogador.token(), mesa(mesaId))
                .andExpect(jsonPath("$.participants", hasSize(2)))
                .andExpect(jsonPath("$.participants[1].userId").value(jogador.id()))
                .andExpect(jsonPath("$.participants[1].characterId").value(personagemId));
    }

    @Test
    void deveImpedirJogadorDeVerOuDecidirSolicitacoes() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        JogadorNaMesa membro = jogadorAceitoNaMesa(mestre, mesaId);
        Conta candidato = registrar("JOGADOR");
        Long solicitacaoId = solicitarEntrada(candidato, mesaId, criarPersonagem(candidato, "PLAYER"));

        getComo(membro.conta().token(), mesa(mesaId) + "/solicitacoes").andExpect(status().isForbidden());
        postComo(membro.conta().token(), mesa(mesaId) + "/solicitacoes/" + solicitacaoId + "/aceitar", null)
                .andExpect(status().isForbidden());
        postComo(candidato.token(), mesa(mesaId) + "/solicitacoes/" + solicitacaoId + "/aceitar", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void deveBloquearNovaSolicitacaoLogoAposRecusa() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        Conta jogador = registrar("JOGADOR");
        Long personagemId = criarPersonagem(jogador, "PLAYER");
        Long solicitacaoId = solicitarEntrada(jogador, mesaId, personagemId);

        postComo(mestre.token(), mesa(mesaId) + "/solicitacoes/" + solicitacaoId + "/recusar", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECUSADA"));

        postComo(jogador.token(), mesa(mesaId) + "/solicitacoes", Map.of("characterId", personagemId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("15 minutos")));
    }

    @Test
    void deveImpedirEntradaComPersonagemDeOutroUsuarioOuComNpc() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        Conta jogador = registrar("JOGADOR");
        Conta outroJogador = registrar("JOGADOR");
        Long personagemAlheio = criarPersonagem(outroJogador, "PLAYER");
        Long npcProprio = criarPersonagem(jogador, "MONSTER");

        postComo(jogador.token(), mesa(mesaId) + "/solicitacoes", Map.of("characterId", personagemAlheio))
                .andExpect(status().isForbidden());
        postComo(jogador.token(), mesa(mesaId) + "/solicitacoes", Map.of("characterId", npcProprio))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveImpedirMestreDeEntrarComoJogador() throws Exception {
        Conta mestre = registrar("MESTRE");
        Conta outroMestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);

        postComo(outroMestre.token(), mesa(mesaId) + "/solicitacoes",
                Map.of("characterId", criarPersonagem(outroMestre, "PLAYER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveFecharEntradaQuandoMesaSaiDeAberta() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        Conta jogador = registrar("JOGADOR");
        Long personagemId = criarPersonagem(jogador, "PLAYER");

        patchComo(mestre.token(), mesa(mesaId) + "/status", Map.of("status", "EM_ANDAMENTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));

        postComo(jogador.token(), mesa(mesaId) + "/solicitacoes", Map.of("characterId", personagemId))
                .andExpect(status().isConflict());
    }

    @Test
    void devePermitirPausarERetomarMasNuncaReabrirMesaEncerrada() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);

        patchComo(mestre.token(), mesa(mesaId) + "/status", Map.of("status", "PAUSADA"))
                .andExpect(jsonPath("$.status").value("PAUSADA"));
        patchComo(mestre.token(), mesa(mesaId) + "/status", Map.of("status", "EM_ANDAMENTO"))
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
        patchComo(mestre.token(), mesa(mesaId) + "/status", Map.of("status", "ENCERRADA"))
                .andExpect(jsonPath("$.status").value("ENCERRADA"));

        patchComo(mestre.token(), mesa(mesaId) + "/status", Map.of("status", "ABERTA"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Uma mesa encerrada não pode ser reaberta."));
        getComo(mestre.token(), mesa(mesaId)).andExpect(jsonPath("$.status").value("ENCERRADA"));
    }

    @Test
    void deveImpedirJogadorDeAlterarStatusDaMesa() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        JogadorNaMesa jogador = jogadorAceitoNaMesa(mestre, mesaId);

        patchComo(jogador.conta().token(), mesa(mesaId) + "/status", Map.of("status", "ENCERRADA"))
                .andExpect(status().isForbidden());
        getComo(mestre.token(), mesa(mesaId)).andExpect(jsonPath("$.status").value("ABERTA"));
    }

    @Test
    void deveRecusarStatusInexistente() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);

        patchComo(mestre.token(), mesa(mesaId) + "/status", Map.of("status", "FINALIZADA"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void devePermitirJogadorSairEMestreRemoverParticipante() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        JogadorNaMesa quemSai = jogadorAceitoNaMesa(mestre, mesaId);
        JogadorNaMesa removido = jogadorAceitoNaMesa(mestre, mesaId);

        postComo(quemSai.conta().token(), mesa(mesaId) + "/sair", null).andExpect(status().isOk());
        postComo(quemSai.conta().token(), mesa(mesaId) + "/sair", null).andExpect(status().isConflict());

        deleteComo(removido.conta().token(), mesa(mesaId) + "/participantes/" + mestre.id())
                .andExpect(status().isForbidden());
        deleteComo(mestre.token(), mesa(mesaId) + "/participantes/" + mestre.id())
                .andExpect(status().isConflict());
        deleteComo(mestre.token(), mesa(mesaId) + "/participantes/" + removido.conta().id())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants", hasSize(1)));
        deleteComo(mestre.token(), mesa(mesaId) + "/participantes/" + removido.conta().id())
                .andExpect(status().isNotFound());

        postComo(mestre.token(), mesa(mesaId) + "/sair", null).andExpect(status().isConflict());
    }

    @Test
    void deveLiberarFichaDoParticipanteApenasParaOMestre() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        JogadorNaMesa aria = jogadorAceitoNaMesa(mestre, mesaId);
        JogadorNaMesa brom = jogadorAceitoNaMesa(mestre, mesaId);

        getComo(mestre.token(), mesa(mesaId) + "/fichas/" + aria.personagemId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aria.personagemId()));
        getComo(brom.conta().token(), mesa(mesaId) + "/fichas/" + aria.personagemId())
                .andExpect(status().isForbidden());
        getComo(brom.conta().token(), "/api/characters/" + aria.personagemId())
                .andExpect(status().isForbidden());
    }

    @Test
    void deveManterNpcDoMestreFechadoParaOsJogadores() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        JogadorNaMesa jogador = jogadorAceitoNaMesa(mestre, mesaId);
        Long npc = criarPersonagem(mestre, "BOSS");

        postComo(mestre.token(), mesa(mesaId) + "/npcs/" + npc, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.npcIds", contains(npc.intValue())));

        getComo(jogador.conta().token(), "/api/characters/" + npc).andExpect(status().isForbidden());
        getComo(jogador.conta().token(), mesa(mesaId) + "/fichas/" + npc).andExpect(status().isForbidden());
    }

    @Test
    void deveValidarDonoETipoAoAssociarNpc() throws Exception {
        Conta mestre = registrar("MESTRE");
        Conta outroMestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        JogadorNaMesa jogador = jogadorAceitoNaMesa(mestre, mesaId);
        Long npcAlheio = criarPersonagem(outroMestre, "MONSTER");
        Long personagemDoMestre = criarPersonagem(mestre, "PLAYER");
        Long npcProprio = criarPersonagem(mestre, "NPC_FRIENDLY");

        postComo(mestre.token(), mesa(mesaId) + "/npcs/" + npcAlheio, null).andExpect(status().isForbidden());
        postComo(mestre.token(), mesa(mesaId) + "/npcs/" + personagemDoMestre, null).andExpect(status().isBadRequest());
        postComo(jogador.conta().token(), mesa(mesaId) + "/npcs/" + npcProprio, null).andExpect(status().isForbidden());

        postComo(mestre.token(), mesa(mesaId) + "/npcs/" + npcProprio, null).andExpect(status().isOk());
        deleteComo(mestre.token(), mesa(mesaId) + "/npcs/" + npcProprio)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.npcIds", not(hasItem(npcProprio.intValue()))));
    }

    @Test
    void devePermitirExcluirMesaApenasAoMestreCriador() throws Exception {
        Conta mestre = registrar("MESTRE");
        Conta outroMestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        JogadorNaMesa jogador = jogadorAceitoNaMesa(mestre, mesaId);

        deleteComo(jogador.conta().token(), mesa(mesaId)).andExpect(status().isForbidden());
        deleteComo(outroMestre.token(), mesa(mesaId)).andExpect(status().isForbidden());

        deleteComo(mestre.token(), mesa(mesaId)).andExpect(status().isNoContent());
        getComo(mestre.token(), mesa(mesaId)).andExpect(status().isNotFound());
    }

    @Test
    void deveResponderNaoEncontradoParaMesaInexistente() throws Exception {
        Conta mestre = registrar("MESTRE");

        getComo(mestre.token(), mesa(999_999L)).andExpect(status().isNotFound());
        patchComo(mestre.token(), mesa(999_999L) + "/status", Map.of("status", "PAUSADA"))
                .andExpect(status().isNotFound());
    }
}
