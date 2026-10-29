package com.arcana.backend.chat.model;

import com.arcana.backend.chat.dice.PedidoRolagem;

import java.util.List;

/**
 * Espelha exatamente o ChatMessage do frontend (chat.ts).
 * Usado tanto para receber quanto para enviar mensagens via WebSocket.
 */
public class ChatMessage {

    public enum Type {
        TEXTO, ROLAGEM, NPC_RESPOSTA, SISTEMA, HISTORICO, PRESENCA, SESSAO_ENCERRADA
    }

    private String id;
    private String tableId;
    private String authorId;
    private String authorName;
    private Type type;
    private String content;
    private DiceRollResult diceResult;
    private PedidoRolagem rollRequest;
    private String mentionedNpcId;
    private String createdAt;
    private List<ChatMessage> historico;
    private List<Presenca> presentes;

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
    public String getMentionedNpcId() { return mentionedNpcId; }
    public void setMentionedNpcId(String mentionedNpcId) { this.mentionedNpcId = mentionedNpcId; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public List<ChatMessage> getHistorico() { return historico; }
    public void setHistorico(List<ChatMessage> historico) { this.historico = historico; }
    public List<Presenca> getPresentes() { return presentes; }
    public void setPresentes(List<Presenca> presentes) { this.presentes = presentes; }
}
