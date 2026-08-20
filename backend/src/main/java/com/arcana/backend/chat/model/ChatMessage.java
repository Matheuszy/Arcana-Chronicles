package com.arcana.backend.chat.model;

import java.util.List;

/**
 * Espelha exatamente o ChatMessage do frontend (chat.ts).
 * Usado tanto para receber quanto para enviar mensagens via WebSocket.
 */
public class ChatMessage {

    public enum Type {
        TEXTO, ROLAGEM, NPC_RESPOSTA, SISTEMA
    }

    private String id;
    private String tableId;
    private String authorId;
    private String authorName;
    private Type type;
    private String content;
    private DiceRollResult diceResult;
    private String mentionedNpcId;
    private String createdAt;

    // ── DiceRollResult aninhado ───────────────────────────────────────────────

    public static class DiceRollResult {
        private String expression;
        private List<Integer> rolls;
        private Integer modifier;
        private Integer total;
        private String attributeUsed;

        public DiceRollResult() {}

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
    public String getMentionedNpcId() { return mentionedNpcId; }
    public void setMentionedNpcId(String mentionedNpcId) { this.mentionedNpcId = mentionedNpcId; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
