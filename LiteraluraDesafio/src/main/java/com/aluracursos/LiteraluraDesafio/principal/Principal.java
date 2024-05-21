package com.aluracursos.LiteraluraDesafio.principal;

import com.aluracursos.LiteraluraDesafio.model.*;
import com.aluracursos.LiteraluraDesafio.repository.AutorRepository;
import com.aluracursos.LiteraluraDesafio.repository.LibroRepository;
import com.aluracursos.LiteraluraDesafio.service.ConsumoApi;
import com.aluracursos.LiteraluraDesafio.service.ConvierteDatos;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {
    private List<Libro> libros;
    private List<Autor> autores;
    private Scanner sc = new Scanner(System.in);
    private ConsumoApi consumoApi = new ConsumoApi();
    private static final String URL_BASE = "https://gutendex.com/books/";
    private ConvierteDatos conversor = new ConvierteDatos();
    private LibroRepository libroRepositorio;
    private AutorRepository autorRepository;
    private String tituloBuscado="";

    public Principal(LibroRepository libroRepositorio, AutorRepository autorRepository) {
        this.libroRepositorio = libroRepositorio;
        this.autorRepository = autorRepository;
    }

    public void mostrarMenu() {
        var opcion = -1;
        while (opcion != 0) {
            var menu = """
                    1. Buscar libro por título
                    2. Listar libros registrados
                    3. Listar autores registrados
                    4. Listar autores vivos en un determinado año
                    5. Listar libros por idioma
                    6. Top 10 con más descargas de la api
                    7. Top 5 con más descargas de la base de datos
                    8. Consultar autor por nombre
                    9. Listar autores por años de fallecimiento
                                        
                    0. Salir
                    
                    Digite la opción que desea realizar:
                    """;
            System.out.println(menu);
            opcion = sc.nextInt();
            sc.nextLine();

            switch (opcion) {
                case 1:
                    buscarLibro();
                    break;
                case 2:
                    listarTodosLosLibros();
                    break;
                case 3:
                    listarTodosLosAutores();
                    break;
                case 4:
                    ListarAutoresPorAno();
                    break;
                case 5:
                    ListarLibrosPorLenguaje();
                    break;
                case 6:
                    top10LibrosApi();;
                    break;
                case 7:
                    topLibrosDescargaBD();
                    break;
                case 8:
                    conultarAutorPorNombre();
                    break;
                case 9:
                    ListarAutoresPorFechaDeFallecimiento();
                    break;
                case 0:
                    System.out.println("Saliendo de la aplicacion");
                    break;
                default:
                    System.out.println("Opción Invalida");
            }
        }
    }

    private DatosGenerales getDatosLibro() {
        // Buscar por nombre
        System.out.println("\nDigite el nombre del libro que desea buscar:");
        tituloBuscado = sc.nextLine();
        var json = consumoApi.obtenerDatos(URL_BASE + "?search=" + tituloBuscado.replace(" ", "+"));
        DatosGenerales datosBusqueda = conversor.obtenerDatos(json, DatosGenerales.class);
        return datosBusqueda;
    }

    private void buscarLibro() {

        DatosGenerales datosGenerales = getDatosLibro();
        List<Autor> autores = new ArrayList<>();
        Optional<DatosLibro> libroBuscado =datosGenerales.libro().stream()
                .filter(l-> l.titulo().toUpperCase().contains(tituloBuscado.toUpperCase()))
                .findFirst();

        if(libroBuscado.isPresent()){

            System.out.println("Libro Encontrado");

            for (DatosAutor datosAutor : libroBuscado.get().autor()) {
                Autor autor = new Autor();
                autor.setNombre(datosAutor.nombre());
                autor.setFechaNacimiento(datosAutor.fechaDeNacimiento());
                autor.setFechaFallecimiento(datosAutor.fechaDeFallecimiento());

                autores.add(autor);
            }
            Libro libro = new Libro(libroBuscado.get());
            libro.setAutores(autores);
            Optional<Libro> libroExistente = libroRepositorio.findByTituloContainingIgnoreCase(libroBuscado.get().titulo());
            if (!libroExistente.isPresent()) {

                try {
                    for (Autor autor : autores) {
                        autorRepository.save(autor);
                    }
                    libroRepositorio.save(libro);
                } catch (DataIntegrityViolationException e) {
                    System.out.println("Error: Libro ya existe en la base de datos");
                }
            }else {
                System.out.println("Libro Ya Existe En La Base De Datos\n");
            }
            System.out.println(libro.toString());
        }else {
            System.out.println("Libro No Encontrado");
        }

    }

    public void listarTodosLosLibros(){
        libros = libroRepositorio.findAll();
        libros.stream()
                .forEach(System.out::println);
    }

    public void listarTodosLosAutores(){
        autores= autorRepository.ListarTodosLosAutores();
        autores.stream()
                .forEach(System.out::println);
    }

    public List<Autor> ListarAutoresPorAno(){
        System.out.println("Digite el año de nacimiento de los autores que desea buscar:");
        String year = sc.nextLine();

        List<Autor> autores = autorRepository.ListarAutoresPorAno(year);

        List<Autor> autoresOrdenados = autores.stream()
                .sorted((a1, a2) -> {
                    int year1 = Integer.parseInt(a1.getFechaNacimiento().substring(1, 4));
                    int year2 = Integer.parseInt(a2.getFechaNacimiento().substring(1, 4));
                    return Integer.compare(year1, year2);
                })
                .collect(Collectors.toList());
        for (Autor autor : autoresOrdenados){
            System.out.println(autor.toString());
        }

        return autoresOrdenados;
    }

    public List<Libro> ListarLibrosPorLenguaje() {
        System.out.println("Opciones de Idiomas");
        System.out.println("-> Español (es)");
        System.out.println("-> Inglés (en)");
        System.out.println("-> Francés (fr)");
        System.out.println("-> Portugués (pt)");
        List<String> subMenuIdioma = new ArrayList<>(); // Convertir a minúsculas para manejar entradas en mayúsculas o minúsculas
        System.out.println("Digite el código () del idioma que desea buscar los Libros:");
        String idioma = sc.nextLine().toLowerCase();
        subMenuIdioma.add(0, idioma);
        List<Libro> librosEnIdiomaEspecifico = libroRepositorio.ListarLibrosPorIdioma(subMenuIdioma);

        if (librosEnIdiomaEspecifico.isEmpty()) {
            System.out.println("No se encontraron libros en el idioma "+ obtenerNombreIdioma(subMenuIdioma));
        } else {
            System.out.println("Libros en " + obtenerNombreIdioma(subMenuIdioma) + ":");
            librosEnIdiomaEspecifico.forEach(libro -> System.out.println(libro.toString()));
        }

        return librosEnIdiomaEspecifico;
    }

    private String obtenerNombreIdioma(List<String> codigoIdioma) {
        switch (codigoIdioma.get(0)) {
            case "es":
                return "Español";
            case "en":
                return "Inglés";
            case "fr":
                return "Francés";
            case "pt":
                return "Portugués";
            default:
                return "Desconocido";
        }
    }
    public DatosGenerales obtenerDatosGeneralesApi() {
        var json = consumoApi.obtenerDatos(URL_BASE);
        var datos = conversor.obtenerDatos(json, DatosGenerales.class);
        return datos;
    }


    public void top10LibrosApi(){
        var datos= obtenerDatosGeneralesApi();
        System.out.println("\nTOP 10 Libros más desargados:" +
                "\n- - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
        datos.libro().stream()
                .sorted(Comparator.comparing(DatosLibro::numeroDeDescargas))
                .limit(10)
                .map(l ->l.titulo().toUpperCase())
                .forEach(System.out::println);
        System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - \n");

    }

    public void topLibrosDescargaBD(){
//        System.out.println("ingrese que Top quiere filtrar");
//        Integer top = sc.nextInt();
//        sc.nextLine();
        List<Libro> topDinamico = libroRepositorio.obtenerTop(5);
        System.out.println("\nTOP 5 de libros más desargados de la base de datos;" +
                "\n- - - - - - - - - - - - - - - - - - - - - - - - - - - -");
        topDinamico.stream()
                .forEach(l ->
                        System.out.printf("Titulo: %s --> Descargas: %s\n",
                                l.getTitulo(),l.getNumeroDeDescargas())
                );
        System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - -\n");
    }

    public void conultarAutorPorNombre(){
        System.out.println("Digite el nombre del autor");
        var autor= sc.nextLine();
        Optional<Autor> autorPorNombre = autorRepository.AutoresPorNombre(autor);
        if (autorPorNombre.isPresent()){
            System.out.println("\nAutores que coinciden con la búsqueda" +
                    "\n- - - - - - - - - - - - - - - - - - - - - - - - - - - ");
            autorPorNombre.stream()
                    .forEach(System.out::println);
            System.out.println("\n- - - - - - - - - - - - - - - - - - - - - ");
        }else{
            System.out.println("No coincide con ningún autor en la base de datos");
        }

    }

    public List<Autor> ListarAutoresPorFechaDeFallecimiento() {
        System.out.println("Digite el año de fallecimiento de los autores que desea buscar:");
        String year = sc.nextLine();
        List<Autor> autores = autorRepository.ListarAutoresPorAnofallecimiento(year);
        List<Autor> autoresOrdenados = autores;
        if (!autores.isEmpty()) {
            autoresOrdenados.stream()
                    .sorted((a1, a2) -> {
                        int year1 = Integer.parseInt(a1.getFechaNacimiento().substring(1, 4));
                        int year2 = Integer.parseInt(a2.getFechaNacimiento().substring(1, 4));
                        return Integer.compare(year1, year2);
                    })
                    .collect(Collectors.toList());
            System.out.println("\nAutores Que Fallecieron Después Del Año " + year +
                    "\n- - - - - - - - - - - - - - - - - - - - - - - - - - - - ");
            for (Autor autor : autoresOrdenados) {
                System.out.println(autor.toString());
            }
            System.out.println("\n- - - - - - - - - - - - - - - - - - - - - - - - - - ");
        } else {
            System.out.println("La Fecha No Coincide Con Datos De Autores\n");
        }


        return autoresOrdenados;
    }
}
