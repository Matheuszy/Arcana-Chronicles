package com.arcana.backend.character.service;

import com.arcana.backend.character.PersonagemRequestDto;
import com.arcana.backend.character.PersonagemResponseDto;
import com.arcana.backend.character.model.Atributo;
import com.arcana.backend.character.model.Atributos;
import com.arcana.backend.character.model.ClassePersonagem;
import com.arcana.backend.character.model.DeslocamentoAdicional;
import com.arcana.backend.character.model.Equipamento;
import com.arcana.backend.character.model.Magia;
import com.arcana.backend.character.model.Moeda;
import com.arcana.backend.character.model.Pericia;
import com.arcana.backend.character.model.Personagem;
import com.arcana.backend.character.model.TipoEquipamento;
import com.arcana.backend.character.model.TipoJogador;
import com.arcana.backend.character.repository.PacoteEquipamentoRepository;
import com.arcana.backend.character.repository.PersonagemRepository;
import com.arcana.backend.character.secao.SecaoAtributosDto;
import com.arcana.backend.character.secao.SecaoAtributosRequestDto;
import com.arcana.backend.character.secao.SecaoEquipamentoDto;
import com.arcana.backend.character.secao.SecaoEquipamentoRequestDto;
import com.arcana.backend.character.secao.SecaoHabilidadesDto;
import com.arcana.backend.character.secao.SecaoHabilidadesRequestDto;
import com.arcana.backend.character.secao.SecaoMagiasDto;
import com.arcana.backend.character.secao.SecaoMagiasRequestDto;
import com.arcana.backend.character.secao.SecaoPersonagemDto;
import com.arcana.backend.character.secao.SecaoPersonagemRequestDto;
import com.arcana.backend.character.secao.SecaoRolagensDto;
import com.arcana.backend.config.exception.RecursoNaoEncontradoException;
import com.arcana.backend.config.exception.RegraDeNegocioException;
import com.arcana.backend.table.SessoesAtivasDeMesa;
import com.arcana.backend.user.model.PapelUsuario;
import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.repository.UsuarioRepositorie;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class PersonagemService {

    private static final int LIMITE_ESPACOS_POR_CIRCULO = 20;
    public static final Set<Integer> DADOS_DE_VIDA = Set.of(6, 8, 10, 12);

    private final PersonagemRepository personagemRepository;
    private final UsuarioRepositorie usuarioRepositorie;
    private final ObjectMapper objectMapper;
    private final SessoesAtivasDeMesa sessoesAtivas;
    private final PacoteEquipamentoRepository pacoteRepository;

    public PersonagemService(PersonagemRepository personagemRepository,
                             UsuarioRepositorie usuarioRepositorie,
                             ObjectMapper objectMapper,
                             SessoesAtivasDeMesa sessoesAtivas,
                             PacoteEquipamentoRepository pacoteRepository) {
        this.personagemRepository = personagemRepository;
        this.usuarioRepositorie = usuarioRepositorie;
        this.objectMapper = objectMapper;
        this.sessoesAtivas = sessoesAtivas;
        this.pacoteRepository = pacoteRepository;
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
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + ownerId));
        validarTipoParaPapel(owner, dto.kind());
        if (owner.getPapel() == PapelUsuario.JOGADOR && sessoesAtivas.usuarioEmSessao(ownerId)) {
            throw new RegraDeNegocioException("Jogadores não podem criar fichas durante uma mesa. Saia da mesa para criar.");
        }

        Personagem personagem = new Personagem();
        mapDtoToEntity(dto, personagem);
        personagem.setOwner(owner);
        if (dto.pacoteEquipamento() != null && !dto.pacoteEquipamento().isBlank()) {
            String chave = dto.pacoteEquipamento().trim().toUpperCase();
            personagem.aplicarPacote(pacoteRepository.findById(chave)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Pacote de equipamento não encontrado: " + chave)));
        }

        return PersonagemResponseDto.from(personagemRepository.save(personagem));
    }

    /** Atualiza um personagem existente */
    @Transactional
    public PersonagemResponseDto atualizar(Long id, PersonagemRequestDto dto, Long ownerId) {
        Personagem personagem = findAndValidateOwner(id, ownerId);
        if (dto.kind() != personagem.getKind()) {
            validarTipoParaPapel(personagem.getOwner(), dto.kind());
        }
        mapDtoToEntity(dto, personagem);
        return PersonagemResponseDto.from(personagemRepository.save(personagem));
    }

    @Transactional
    public PersonagemResponseDto definirCaAdicional(Long id, Long ownerId, int valor) {
        Personagem personagem = findAndValidateOwner(id, ownerId);
        personagem.setCaAdicional(valor);
        return PersonagemResponseDto.from(personagemRepository.save(personagem));
    }

    @Transactional
    public PersonagemResponseDto acrescentarMoedas(Long id, Long ownerId, Moeda moeda, int quantidade) {
        Personagem personagem = findAndValidateOwner(id, ownerId);
        personagem.acrescentarMoedas(moeda, quantidade);
        return PersonagemResponseDto.from(personagemRepository.save(personagem));
    }

    @Transactional
    public SecaoPersonagemDto buscarSecaoPersonagem(Long id, Long ownerId) {
        return SecaoPersonagemDto.from(findAndValidateOwner(id, ownerId));
    }

    @Transactional
    public SecaoAtributosDto buscarSecaoAtributos(Long id, Long ownerId) {
        return SecaoAtributosDto.from(findAndValidateOwner(id, ownerId));
    }

    @Transactional
    public SecaoHabilidadesDto buscarSecaoHabilidades(Long id, Long ownerId) {
        return SecaoHabilidadesDto.from(findAndValidateOwner(id, ownerId));
    }

    @Transactional
    public SecaoEquipamentoDto buscarSecaoEquipamento(Long id, Long ownerId) {
        return SecaoEquipamentoDto.from(findAndValidateOwner(id, ownerId));
    }

    @Transactional
    public SecaoMagiasDto buscarSecaoMagias(Long id, Long ownerId) {
        return SecaoMagiasDto.from(findAndValidateOwner(id, ownerId));
    }

    @Transactional
    public SecaoRolagensDto buscarSecaoRolagens(Long id, Long ownerId) {
        return SecaoRolagensDto.from(findAndValidateOwner(id, ownerId));
    }

    @Transactional
    public SecaoPersonagemDto atualizarSecaoPersonagem(Long id, SecaoPersonagemRequestDto dto, Long ownerId) {
        Personagem personagem = findAndValidateOwner(id, ownerId);
        if (dto.kind() != personagem.getKind()) {
            validarTipoParaPapel(personagem.getOwner(), dto.kind());
        }
        aplicarPersonagem(dto, personagem);
        return SecaoPersonagemDto.from(personagemRepository.save(personagem));
    }

    @Transactional
    public SecaoAtributosDto atualizarSecaoAtributos(Long id, SecaoAtributosRequestDto dto, Long ownerId) {
        Personagem personagem = findAndValidateOwner(id, ownerId);
        aplicarAtributos(dto, personagem);
        return SecaoAtributosDto.from(personagemRepository.save(personagem));
    }

    @Transactional
    public SecaoHabilidadesDto atualizarSecaoHabilidades(Long id, SecaoHabilidadesRequestDto dto, Long ownerId) {
        Personagem personagem = findAndValidateOwner(id, ownerId);
        aplicarHabilidades(dto, personagem);
        return SecaoHabilidadesDto.from(personagemRepository.save(personagem));
    }

    @Transactional
    public SecaoEquipamentoDto atualizarSecaoEquipamento(Long id, SecaoEquipamentoRequestDto dto, Long ownerId) {
        Personagem personagem = findAndValidateOwner(id, ownerId);
        aplicarEquipamento(dto, personagem);
        return SecaoEquipamentoDto.from(personagemRepository.save(personagem));
    }

    @Transactional
    public SecaoMagiasDto atualizarSecaoMagias(Long id, SecaoMagiasRequestDto dto, Long ownerId) {
        Personagem personagem = findAndValidateOwner(id, ownerId);
        aplicarMagias(dto, personagem);
        return SecaoMagiasDto.from(personagemRepository.save(personagem));
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
                .orElseThrow(() -> new RecursoNaoEncontradoException("Personagem não encontrado: " + id));

        if (!personagem.getOwner().getId().equals(ownerId)) {
            throw new AccessDeniedException("Personagem não pertence a este usuário.");
        }
        return personagem;
    }

    private void validarTipoParaPapel(Usuario usuario, TipoJogador kind) {
        boolean ehPersonagemDeJogador = kind == TipoJogador.PLAYER;
        if (usuario.isMestre() && ehPersonagemDeJogador) {
            throw new AccessDeniedException("Contas de mestre só criam NPCs, monstros e chefes.");
        }
        if (!usuario.isMestre() && !ehPersonagemDeJogador) {
            throw new AccessDeniedException("Contas de jogador só criam personagens do tipo jogador.");
        }
    }

    private void mapDtoToEntity(PersonagemRequestDto dto, Personagem personagem) {
        personagem.setLevel(dto.level());
        aplicarPersonagem(SecaoPersonagemRequestDto.de(dto), personagem);
        aplicarAtributos(SecaoAtributosRequestDto.de(dto), personagem);
        aplicarHabilidades(SecaoHabilidadesRequestDto.de(dto), personagem);
        aplicarEquipamento(SecaoEquipamentoRequestDto.de(dto), personagem);
        aplicarMagias(SecaoMagiasRequestDto.de(dto), personagem);
    }

    private void aplicarPersonagem(SecaoPersonagemRequestDto dto, Personagem personagem) {
        personagem.setName(dto.name());
        personagem.setKind(dto.kind());
        personagem.getClasses().clear();
        if (dto.classes() != null && !dto.classes().isEmpty()) {
            dto.classes().forEach(classe -> personagem.getClasses().add(paraClasse(classe)));
            personagem.setLevel(personagem.getClasses().stream().mapToInt(ClassePersonagem::getNivel).sum());
        }
        personagem.setRaca(textoOuNulo(dto.raca()));
        personagem.setAlinhamento(textoOuNulo(dto.alinhamento()));
        personagem.setAntecedente(textoOuNulo(dto.antecedente()));
        personagem.setTracosPersonalidade(textoOuNulo(dto.tracosPersonalidade()));
        personagem.setIdeais(textoOuNulo(dto.ideais()));
        personagem.setVinculos(textoOuNulo(dto.vinculos()));
        personagem.setDefeitos(textoOuNulo(dto.defeitos()));
        personagem.setBackstory(dto.backstory());
        personagem.setPersonalityPrompt(dto.personalityPrompt());
        personagem.setAvatarUrl(dto.avatarUrl());
    }

    private void aplicarAtributos(SecaoAtributosRequestDto dto, Personagem personagem) {
        personagem.setHpMax(dto.hpMax());
        personagem.setHpCurrent(dto.hpCurrent());
        personagem.setVidaTemporaria(dto.vidaTemporaria());
        personagem.setArmorClass(dto.armorClass());
        personagem.setCaAdicional(valorOuZero(dto.caAdicional()));
        personagem.setBonusIniciativa(valorOuZero(dto.bonusIniciativa()));
        personagem.setDeslocamento(dto.deslocamento());
        personagem.setInspiracao(Boolean.TRUE.equals(dto.inspiracao()));
        personagem.setSucessosContraMorte(valorOuZero(dto.sucessosContraMorte()));
        personagem.setFalhasContraMorte(valorOuZero(dto.falhasContraMorte()));
        personagem.setBonusProficiencia(dto.proficiencyBonus() != null ? dto.proficiencyBonus() : 2);
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

        personagem.getDeslocamentosAdicionais().clear();
        if (dto.deslocamentosAdicionais() != null) {
            dto.deslocamentosAdicionais().stream()
                    .map(d -> new DeslocamentoAdicional(d.tipo(), d.metros(), textoOuNulo(d.origem()),
                            Boolean.TRUE.equals(d.temporario())))
                    .forEach(personagem.getDeslocamentosAdicionais()::add);
        }

        personagem.getSalvaguardas().clear();
        if (dto.savingThrows() != null) {
            dto.savingThrows().forEach((chave, bonus) ->
                    personagem.getSalvaguardas().put(Atributo.daChave(chave), bonus));
        }

        personagem.getSkills().clear();
        if (dto.skills() != null) {
            dto.skills().forEach((chave, bonus) ->
                    personagem.getSkills().put(Pericia.daChave(chave).getChave(), bonus));
        }
    }

    private void aplicarHabilidades(SecaoHabilidadesRequestDto dto, Personagem personagem) {
        personagem.setHabilidades(textoOuNulo(dto.habilidades()));
        personagem.setHabilidadesRaca(textoOuNulo(dto.habilidadesRaca()));
        personagem.setTalentos(textoOuNulo(dto.talentos()));
        substituirLista(personagem.getIdiomas(), dto.idiomas(), "Idioma repetido");
        substituirLista(personagem.getProficienciasFerramentas(), dto.proficienciasFerramentas(), "Proficiência em ferramenta repetida");
        substituirLista(personagem.getProficienciasArmas(), dto.proficienciasArmas(), "Proficiência em arma repetida");
        substituirLista(personagem.getProficienciasArmaduras(), dto.proficienciasArmaduras(), "Proficiência em armadura repetida");
    }

    private void aplicarEquipamento(SecaoEquipamentoRequestDto dto, Personagem personagem) {
        personagem.getEquipamentos().clear();
        if (dto.equipamentos() != null) {
            dto.equipamentos().stream()
                    .map(this::paraEquipamento)
                    .forEach(personagem.getEquipamentos()::add);
        }
        personagem.getItens().clear();
        if (dto.items() != null) {
            dto.items().stream().map(String::trim).forEach(personagem.getItens()::add);
        }
        personagem.setMoedasCobre(valorOuZero(dto.moedasCobre()));
        personagem.setMoedasPrata(valorOuZero(dto.moedasPrata()));
        personagem.setMoedasElectro(valorOuZero(dto.moedasElectro()));
        personagem.setMoedasOuro(valorOuZero(dto.moedasOuro()));
        personagem.setMoedasPlatina(valorOuZero(dto.moedasPlatina()));
    }

    private void aplicarMagias(SecaoMagiasRequestDto dto, Personagem personagem) {
        validarEspacosDeMagia(dto);
        personagem.getSpells().clear();
        if (dto.spells() != null) {
            dto.spells().stream()
                    .map(s -> new Magia(s.name(), s.level(), s.description()))
                    .forEach(personagem.getSpells()::add);
        }
        personagem.setSpellSlots(serializeSlots(dto.spellSlots()));
        personagem.setSpellSlotsCurrent(serializeSlots(dto.spellSlotsCurrent()));
    }

    private Equipamento paraEquipamento(PersonagemRequestDto.EquipamentoDto dto) {
        boolean arma = dto.tipo() == TipoEquipamento.ARMA;
        String atributo = textoOuNulo(dto.atributo());
        return new Equipamento(
                dto.tipo(),
                dto.nome().trim(),
                arma ? textoOuNulo(dto.dano()) : null,
                arma && atributo != null ? Atributo.daChave(atributo) : null,
                arma ? null : dto.classeArmadura(),
                textoOuNulo(dto.propriedades()));
    }

    private ClassePersonagem paraClasse(PersonagemRequestDto.ClasseDto dto) {
        String nome = dto.classe().trim();
        int dadoVida = dto.dadoVida() != null ? dto.dadoVida() : ClassePersonagem.DADO_VIDA_PADRAO;
        if (!DADOS_DE_VIDA.contains(dadoVida)) {
            throw new IllegalArgumentException("O dado de vida de " + nome + " deve ser d6, d8, d10 ou d12.");
        }
        int disponiveis = dto.dadosVidaDisponiveis() != null ? dto.dadosVidaDisponiveis() : dto.nivel();
        if (disponiveis > dto.nivel()) {
            throw new IllegalArgumentException("Os dados de vida disponíveis de " + nome
                    + " não podem passar do nível da classe (" + dto.nivel() + ").");
        }
        return new ClassePersonagem(nome, textoOuNulo(dto.subclasse()), dto.nivel(), dadoVida, disponiveis);
    }

    private void validarEspacosDeMagia(SecaoMagiasRequestDto dto) {
        Map<String, Integer> totais = dto.spellSlots() == null ? Map.of() : dto.spellSlots();
        Map<String, Integer> atuais = dto.spellSlotsCurrent() == null ? Map.of() : dto.spellSlotsCurrent();
        totais.forEach((circulo, total) -> validarEspacosDoCirculo(circulo, total, "Espaços de magia"));
        atuais.forEach((circulo, atual) -> {
            validarEspacosDoCirculo(circulo, atual, "Espaços de magia restantes");
            if (atual > totais.getOrDefault(circulo, 0)) {
                throw new IllegalArgumentException(
                        "Os espaços restantes do " + circulo + "º círculo não podem passar do total.");
            }
        });
        if (dto.spells() == null) return;
        dto.spells().stream()
                .filter(magia -> magia.level() != null && magia.level() >= 1)
                .forEach(magia -> {
                    if (totais.getOrDefault(String.valueOf(magia.level()), 0) < 1) {
                        throw new IllegalArgumentException(
                                "Defina quantos espaços de magia o " + magia.level() + "º círculo possui.");
                    }
                });
    }

    private void validarEspacosDoCirculo(String circulo, Integer quantidade, String descricao) {
        if (!circulo.matches("[1-9]")) {
            throw new IllegalArgumentException("Círculo de magia inválido: " + circulo);
        }
        if (quantidade == null || quantidade < 0 || quantidade > LIMITE_ESPACOS_POR_CIRCULO) {
            throw new IllegalArgumentException(
                    descricao + " do " + circulo + "º círculo devem ficar entre 0 e " + LIMITE_ESPACOS_POR_CIRCULO + ".");
        }
    }

    private void substituirLista(List<String> atual, List<String> recebida, String repetida) {
        atual.clear();
        if (recebida == null) return;
        Set<String> vistos = new HashSet<>();
        for (String entrada : recebida) {
            String valor = entrada.trim();
            if (!vistos.add(valor.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException(repetida + " na ficha: " + valor);
            }
            atual.add(valor);
        }
    }

    private int valorOuZero(Integer valor) {
        return valor != null ? valor : 0;
    }

    private String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
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
