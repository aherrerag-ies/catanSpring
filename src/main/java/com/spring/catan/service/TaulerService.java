package com.spring.catan.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.spring.catan.dto.HexagonDTO;
import com.spring.catan.dto.HexagonDTO.ConstruccioDTO;
import com.spring.catan.repository.*;
import herencia.Assentament;
import herencia.Ciutat;
import relacions.Partida;
import taulell.*;

@Service
@Transactional(readOnly = true)
public class TaulerService {
    private final HexagonRepository hexagons;
    private final VertexRepository vertexs;
    private final ArestaRepository arestes;
    private final HexagonVertexRepository relacions;
    private final AssentamentRepository assentaments;

    public TaulerService(HexagonRepository hexagons, VertexRepository vertexs, ArestaRepository arestes,
            HexagonVertexRepository relacions, AssentamentRepository assentaments) {
        this.hexagons = hexagons;
        this.vertexs = vertexs;
        this.arestes = arestes;
        this.relacions = relacions;
        this.assentaments = assentaments;
    }

    public List<HexagonDTO> consultar(Partida partida) {
        List<HexagonDTO> tauler = new ArrayList<>();
        List<Hexagon> llistaHexagons = hexagons.findByPartida_IdOrderByCodi(partida.getId());

        for (Hexagon hexagon : llistaHexagons) {
            List<ConstruccioDTO> construccions = new ArrayList<>();
            List<Assentament> adjacents = assentaments.findAdjacents(hexagon.getId());

            for (Assentament assentament : adjacents) {
                String tipus = "POBLAT";
                if (assentament instanceof Ciutat) {
                    tipus = "CIUTAT";
                }
                ConstruccioDTO construccio = new ConstruccioDTO(assentament.getId(), tipus,
                        assentament.getVertex().getId(), assentament.getPropietari().getId(),
                        assentament.getPropietari().getNom());
                construccions.add(construccio);
            }

            boolean lladre = false;
            if (partida.getHexagonLladre() != null) {
                lladre = hexagon.getId().equals(partida.getHexagonLladre().getId());
            }
            String terreny = null;
            if (hexagon.getTerreny() != null) {
                terreny = hexagon.getTerreny().name();
            }
            tauler.add(new HexagonDTO(hexagon.getId(), hexagon.getCodi(), terreny,
                    hexagon.getNumero(), lladre, construccions));
        }
        return tauler;
    }

    @Transactional
    public List<Hexagon> crear(Partida partida) {
        Map<Integer, Vertex> vertexsPerCodi = new HashMap<>();
        for (String[] fila : llegirCsv("vertexs.csv")) {
            Vertex vertex = vertexs.save(new Vertex(Integer.valueOf(fila[0]), partida));
            partida.afegirVertex(vertex);
            vertexsPerCodi.put(vertex.getCodi(), vertex);
        }

        Map<Integer, Hexagon> hexagonsPerCodi = new LinkedHashMap<>();
        for (String[] fila : llegirCsv("hexagons.csv")) {
            Hexagon hexagon = hexagons.save(new Hexagon(Integer.valueOf(fila[0]), Integer.valueOf(fila[1]),
                    Integer.valueOf(fila[2]), null, null, partida));
            partida.afegirHexagon(hexagon);
            hexagonsPerCodi.put(hexagon.getCodi(), hexagon);
        }

        int totalRelacions = 0;
        for (String[] fila : llegirCsv("hexagon_vertex.csv")) {
            relacions.save(new HexagonVertex(obtenir(hexagonsPerCodi, fila[0]),
                    obtenir(vertexsPerCodi, fila[1]), Integer.valueOf(fila[2])));
            totalRelacions++;
        }

        int totalArestes = 0;
        for (String[] fila : llegirCsv("arestes.csv")) {
            Aresta aresta = arestes.save(new Aresta(Integer.valueOf(fila[0]), partida,
                    obtenir(vertexsPerCodi, fila[1]), obtenir(vertexsPerCodi, fila[2])));
            partida.afegirAresta(aresta);
            totalArestes++;
        }

        if (vertexsPerCodi.size() != 54 || hexagonsPerCodi.size() != 19
                || totalRelacions != 114 || totalArestes != 72) {
            throw new IllegalStateException("Les dades del tauler no tenen les quantitats esperades");
        }
        return new ArrayList<>(hexagonsPerCodi.values());
    }

    private List<String[]> llegirCsv(String fitxer) {
        InputStream entrada = TaulerService.class.getResourceAsStream("/csv/" + fitxer);
        if (entrada == null) {
            throw new IllegalStateException("No es troba el fitxer /csv/" + fitxer);
        }
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
            List<String[]> files = new ArrayList<>();
            lector.readLine(); // Saltem la capçalera.
            String linia = lector.readLine();
            while (linia != null) {
                if (!linia.isBlank()) {
                    String[] camps = linia.split(",", -1);
                    for (int i = 0; i < camps.length; i++) {
                        camps[i] = camps[i].trim();
                    }
                    files.add(camps);
                }
                linia = lector.readLine();
            }
            return files;
        } catch (IOException e) {
            throw new UncheckedIOException("Error en llegir " + fitxer, e);
        }
    }

    private <T> T obtenir(Map<Integer, T> mapa, String codi) {
        T element = mapa.get(Integer.valueOf(codi));
        if (element == null) {
            throw new IllegalStateException("No s'ha trobat l'element amb el codi " + codi + " del CSV");
        }
        return element;
    }
}
