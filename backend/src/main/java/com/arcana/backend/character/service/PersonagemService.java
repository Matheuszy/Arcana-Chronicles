package com.arcana.backend.character.service;

import com.arcana.backend.character.PersonagemRequestDto;
import com.arcana.backend.character.PersonagemResponseDto;
import com.arcana.backend.character.model.Atributos;
import com.arcana.backend.character.model.Magia;
import com.arcana.backend.character.model.Personagem;
import com.arcana.backend.character.model.TipoJogador;
import com.arcana.backend.character.repository.PersonagemRepository;
import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.repository.UsuarioRepositorie;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PersonagemService {

    private final PersonagemRepository personagemRepository;
    private final UsuarioRepositorie usuarioRepositorie;
    private final ObjectMapper objectMapper;

    public PersonagemService(PersonagemRepository personagemRepository,
                             UsuarioRepositorie usuarioRepositorie,
                             ObjectMapper objectMapper) {
        this.personagemRepository = personagemRepository;
        this.usuarioRepositorie = usuarioRepositorie;
        this.objectMapper = objectMapper;
    }

    /** Lista todos os personagens de um usuário, opcionalmente filtrados por tipo */
    public List<PersonagemResponseDto> listarPorUsuario(Long ownerId, TipoJogador kind) {
        List<Personagem> personagens = (kind != null)
                ? personagemRepository.findByOwnerIdAndKind(ownerId, kind)
                : personagemRepository.findByOwnerId(ownerId);

        return personagens.stream()
                .map(PersonagemResponseDto::from)
                .toList();
    }

    /** Busca um personagem por ID — lança exceção se não existir ou não pertencer ao usuário */
    public PersonagemResponseDto buscarPorId(Long id, Long ownerId) {
        Personagem personagem = findAndValidateOwner(id, ownerId);
        return PersonagemResponseDto.from(personagem);
    }

    /** Cria um novo personagem vinculado ao usuário */
    @Transactional
    public PersonagemResponseDto criar(PersonagemRequestDto dto, Long ownerId) {
        Usuario owner = usuarioRepositorie.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + ownerId));

        Personagem personagem = new Personagem();
        mapDtoToEntity(dto, personagem);
        personagem.setOwner(owner);

        return PersonagemResponseDto.from(personagemRepository.save(personagem));
    }

    /** Atualiza um personagem existente */
    @Transactional
    public PersonagemResponseDto atualizar(Long id, PersonagemRequestDto dto, Long ownerId) {
        Personagem personagem = findAndValidateOwner(id, ownerId);
        mapDtoToEntity(dto, personagem);
        return PersonagemResponseDto.from(personagemRepository.save(personagem));
    }

    /** Remove um personagem */
    @Transactional
    public void deletar(Long id, Long ownerId) {
        Personagem personagem = findAndValidateOwner(id, ownerId);
        personagemRepository.delete(personagem);
    }

    // ── helpers privados ──────────────────────────────────────────────────────

    private Personagem findAndValidateOwner(Long id, Long ownerId) {
        Personagem personagem = personagemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Personagem não encontrado: " + id));

        if (!personagem.getOwner().getId().equals(ownerId)) {
            throw new RuntimeException("Acesso negado: personagem não pertence a este usuário.");
        }
        return personagem;
    }

    private void mapDtoToEntity(PersonagemRequestDto dto, Personagem personagem) {
        personagem.setName(dto.name());
        personagem.setKind(dto.kind());
        personagem.setLevel(dto.level());
        personagem.setHpMax(dto.hpMax());
        personagem.setHpCurrent(dto.hpCurrent());
        personagem.setArmorClass(dto.armorClass());
        personagem.setBackstory(dto.backstory());
        personagem.setPersonalityPrompt(dto.personalityPrompt());
        personagem.setAvatarUrl(dto.avatarUrl());
        personagem.setEquipment(dto.equipment());

        // Atributos (@Embedded)
        if (dto.attributes() != null) {
            personagem.setAttributes(new Atributos(
                    dto.attributes().forca(),
                    dto.attributes().destreza(),
                    dto.attributes().constituicao(),
                    dto.attributes().inteligencia(),
                    dto.attributes().sabedoria(),
                    dto.attributes().carisma()
            ));
        }

        // Perícias
        personagem.setSkills(dto.skills() != null ? dto.skills() : List.of());

        // Magias — substitui a lista inteira
        personagem.getSpells().clear();
        if (dto.spells() != null) {
            dto.spells().stream()
                    .map(s -> new Magia(s.name(), s.level(), s.description()))
                    .forEach(personagem.getSpells()::add);
        }

        // Spell slots — serializa Map para JSON
        personagem.setSpellSlots(serializeSlots(dto.spellSlots()));
    }

    private String serializeSlots(Map<String, Integer> slots) {
        if (slots == null || slots.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(slots);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
