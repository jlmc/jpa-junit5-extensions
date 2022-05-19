package io.github.jlmc.jpa;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import jakarta.persistence.*;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(of = {"id"})

@Entity
@Table(name = "book")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String title;
}
