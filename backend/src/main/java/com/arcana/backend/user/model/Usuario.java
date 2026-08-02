package com.arcana.backend.user.model;

import com.arcana.backend.character.model.Personagem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private  String username;

    @Column(nullable = false, unique = true)
    private  String email;

    @Column(nullable = false)
    private  String password;

    @OneToMany(mappedBy="ownerId", cascade = CascadeType.ALL, orphanRemoval = true)
    List<Personagem> personagens = new ArrayList<>();


    public Usuario() {

    }

    public Usuario(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public String setUsername(String username) {
        this.username = username;
        return this.username;
    }

    public String setEmail(String email) {
        this.email = email;
        return this.email;
    }

    public String setPassword(String password) {
        this.password = password;
        return this.password;
    }

    public List<Personagem> setPersonagens(Personagem personagem) {
        this.personagens.add(personagem);
        return this.personagens;
    }

}
