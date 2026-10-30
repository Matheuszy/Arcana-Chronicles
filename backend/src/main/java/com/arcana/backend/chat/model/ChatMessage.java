package com.arcana.backend.chat.model;

import com.arcana.backend.chat.desafio.ComandoDesafio;
import com.arcana.backend.chat.desafio.DesafioDto;
import com.arcana.backend.chat.dice.PedidoRolagem;
import com.arcana.backend.chat.sala.ComandoSala;
import com.arcana.backend.chat.sala.ItemDto;
import com.arcana.backend.chat.sala.StatusDto;

import java.util.List;
import java.util.Map;

/**
 * Espelha exatamente o ChatMessage do frontend (chat.ts).
 * Usado tanto para receber quanto para enviar mensagens via WebSocket.
 */
public class ChatMessage {

    public enum Type {
        TEXTO, ROLAGEM, IMAGEM, NPC_RESPOSTA, SISTEMA, HISTORICO, PRESENCA, SESSAO_ENCERRADA, STATUS_MESA,
        PARTICIPANTES, REMOVIDO_DA_MESA,
        DESAFIO, DESAFIOS,
        DESAFIO_CRIAR, DESAFIO_ROLAR, DESAFIO_TENTATIVA_EXTRA, DESAFIO_ENCERRAR_FALHA, DESAFIO_CANCELAR,
        DESAFIO_MENSAGEM,
        NOTA_SALVAR, NOTA, STATUS_DEFINIR, STATUS_REMOVER, STATUS, ITEM_CRIAR, ITEM_REMOVER, ITENS,
        NOTA_COMPARTILHAR, NOTA_RECEBIDA, NOTA_COMPARTILHADA
    }

    private String id;
    private String tableId;
    private String authorId;
    private String authorName;
    private Type type;
    private Canal canal;
    private String content;
    private DiceRollResult diceResult;
    private PedidoRolagem rollRequest;
    private ImagemAnexada image;
    private String mentionedNpcId;
    private String createdAt;
    private Map<Canal, List<ChatMessage>> historico;
    private List<Presenca> presentes;
    private ComandoDesafio comandoDesafio;
    private DesafioDto desafio;
    private List<DesafioDto> desafios;
    private ComandoSala comandoSala;
    private StatusDto status;
    private List<ItemDto> itens;

    // ── DiceRollResult aninhado ───────────────────────────────────────────────

    public static class DiceRollResult {
        private String expression;
        private List<Integer> rolls;
        private Integer modifier;
        private Integer total;
        private String attributeUsed;
        private Integer sides;
        private Integer quantity;
        private String mode;
        private Integer kept;
        private boolean critical;
        private boolean fumble;

        public DiceRollResult() {}

        public Integer getSides() { return sides; }
        public void setSides(Integer sides) { this.sides = sides; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public Integer getKept() { return kept; }
        public void setKept(Integer kept) { this.kept = kept; }
        public boolean isCritical() { return critical; }
        public void setCritical(boolean critical) { this.critical = critical; }
        public boolean isFumble() { return fumble; }
        public void setFumble(boolean fumble) { this.fumble = fumble; }

        public String getExpression() { return expression; }
        public void setExpression(String expression) { this.expression = expression; }
        public List<Integer> getRolls() { return rolls; }
        public void setRolls(List<Integer> rolls) { this.rolls = rolls; }
        public Integer getModifier() { return modifier; }
        public void setModifier(Integer modifier) { this.modifier = modifier; }
        public Integer getTotal() { return total; }
        public void setTotal(Integer total) { this.total = total; }
        public String getAttributeUsed() { return attributeUsed; }
        public void setAttributeUsed(String attributeUsed) { this.attributeUsed = attributeUsed; }
    }

    // ── Getters e Setters ─────────────────────────────────────────────────────

    public ChatMessage() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTableId() { return tableId; }
    public void setTableId(String tableId) { this.tableId = tableId; }
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public DiceRollResult getDiceResult() { return diceResult; }
    public void setDiceResult(DiceRollResult diceResult) { this.diceResult = diceResult; }
    public PedidoRolagem getRollRequest() { return rollRequest; }
    public void setRollRequest(PedidoRolagem rollRequest) { this.rollRequest = rollRequest; }
    public ImagemAnexada getImage() { return image; }
    public void setImage(ImagemAnexada image) { this.image = image; }
    public String getMentionedNpcId() { return mentionedNpcId; }
    public void setMentionedNpcId(String mentionedNpcId) { this.mentionedNpcId = mentionedNpcId; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public Canal getCanal() { return canal; }
    public void setCanal(Canal canal) { this.canal = canal; }
    public Map<Canal, List<ChatMessage>> getHistorico() { return historico; }
    public void setHistorico(Map<Canal, List<ChatMessage>> historico) { this.historico = historico; }
    public List<Presenca> getPresentes() { return presentes; }
    public void setPresentes(List<Presenca> presentes) { this.presentes = presentes; }
    public ComandoDesafio getComandoDesafio() { return comandoDesafio; }
    public void setComandoDesafio(ComandoDesafio comandoDesafio) { this.comandoDesafio = comandoDesafio; }
    public DesafioDto getDesafio() { return desafio; }
    public void setDesafio(DesafioDto desafio) { this.desafio = desafio; }
    public List<DesafioDto> getDesafios() { return desafios; }
    public void setDesafios(List<DesafioDto> desafios) { this.desafios = desafios; }
    public ComandoSala getComandoSala() { return comandoSala; }
    public void setComandoSala(ComandoSala comandoSala) { this.comandoSala = comandoSala; }
    public StatusDto getStatus() { return status; }
    public void setStatus(StatusDto status) { this.status = status; }
    public List<ItemDto> getItens() { return itens; }
    public void setItens(List<ItemDto> itens) { this.itens = itens; }
}
