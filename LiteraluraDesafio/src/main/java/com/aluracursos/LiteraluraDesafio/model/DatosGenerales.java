package com.aluracursos.LiteraluraDesafio.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)

public record DatosGenerales( @JsonAlias("results") List<DatosLibro> libro) {
}
