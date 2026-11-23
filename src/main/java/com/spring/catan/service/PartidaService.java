package com.spring.catan.service;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.spring.catan.dto.*;
import com.spring.catan.dto.RecursosDTO.RecursDTO;
import com.spring.catan.repository.*;
import enumeration.*;
import herencia.Assentament;
import herencia.Ciutat;
import relacions.*;
import taulell.Hexagon;

@Service
@Transactional(readOnly = true)
public class PartidaService {
    private final PartidaRepository partides;
    private final JugadorRepository jugadors;
    private final HexagonRepository hexagons;
    private final AssentamentRepository assentaments;
    private final CartaDesenvolupamentRepository cartes;
    private final TaulerService tauler;
    private final DausService daus;

    public PartidaService(PartidaRepository partides, JugadorRepository jugadors, HexagonRepository hexagons,
            AssentamentRepository assentaments, CartaDesenvolupamentRepository cartes,
            TaulerService tauler, DausService daus) {
        this.partides = partides;
        this.jugadors = jugadors;
        this.hexagons = hexagons;
        this.assentaments = assentaments;
        this.cartes = cartes;
        this.tauler = tauler;
        this.daus = daus;
    }

    public List<HexagonDTO> tauler(int idPartida) {
        Partida partida = partides.findById(idPartida).orElse(null);
        if (partida == null) {
            return null;
        }
        return tauler.consultar(partida);
    }

    @Transactional
    public PartidaDTO crear(NuevaPartidaRequest peticio) {
        validarPeticio(peticio);
        int punts = 10;
        if (peticio.getPuntosParaGanar() != null) {
            punts = peticio.getPuntosParaGanar();
        }
        Partida partida = partides.save(new Partida(LocalDateTime.now(), 1, null, punts,
                false, false, null, null));
        List<Hexagon> taulerNou = tauler.crear(partida);
        assignarTerrenysINumeros(partida, taulerNou);

        List<String> noms = new ArrayList<>(peticio.getJugadores());
        List<Jugador> participants = new ArrayList<>();
        Collections.shuffle(noms);
        for (int i = 0; i < noms.size(); i++) {
            Jugador jugador = new Jugador(noms.get(i).trim(), ColorJugador.values()[i], i + 1, partida, 0);
            partida.afegirJugador(jugador);
            jugadors.save(jugador);
            participants.add(jugador);
        }
        partida.setJugadorActiu(participants.get(0));

        afegirCartes(partida, TipusCartaDesenvolupament.CAVALLER, 14);
        afegirCartes(partida, TipusCartaDesenvolupament.PUNT_VICTORIA, 5);
        afegirCartes(partida, TipusCartaDesenvolupament.MONOPOLI, 2);

        List<PartidaDTO.JugadorDTO> jugadorsResposta = new ArrayList<>();
        for (Jugador jugador : participants) {
            jugadorsResposta.add(PartidaDTO.JugadorDTO.from(jugador));
        }
        return new PartidaDTO(partida.getId(), partida.getEstat().name(), partida.getRonda(), punts,
                partida.getJugadorActiu().getId(), jugadorsResposta, tauler.consultar(partida));
    }

    @Transactional
    public TiradaDTO tirarDaus(int idPartida) {
        Partida partida = partides.findForUpdate(idPartida).orElse(null);
        if (partida == null) {
            return null;
        }
        if (partida.getEstat() != EstatPartida.EN_CURS) {
            throw new IllegalArgumentException("Només es poden tirar els daus en una partida en curs");
        }
        if (partida.getJugadorActiu() == null) {
            throw new IllegalArgumentException("La partida no té cap jugador actiu");
        }
        if (Boolean.TRUE.equals(partida.getDausTirats())) {
            throw new IllegalArgumentException("El jugador actiu ja ha tirat els daus aquest torn");
        }

        int dau1 = daus.tirar();
        int dau2 = daus.tirar();
        int tirada = dau1 + dau2;
        List<Jugador> participants = jugadors.findByPartida_IdOrderByOrdreTorn(idPartida);
        TipusRecurs[] tipusRecursos = TipusRecurs.values();
        // Cada fila correspon a un jugador i cada columna a un tipus de recurs.
        int[][] produccio = new int[participants.size()][tipusRecursos.length];

        if (tirada != 7) {
            for (Hexagon hexagon : hexagons.findByPartida_IdAndNumeroOrderByCodi(idPartida, tirada)) {
                if (partida.getHexagonLladre() != null
                        && hexagon.getId().equals(partida.getHexagonLladre().getId())) {
                    continue;
                }
                TipusRecurs tipus = recursDelTerreny(hexagon.getTerreny());
                if (tipus == null) {
                    continue;
                }
                for (Assentament assentament : assentaments.findAdjacents(hexagon.getId())) {
                    Jugador propietari = assentament.getPropietari();
                    int quantitat = 1;
                    if (assentament instanceof Ciutat) {
                        quantitat = 2;
                    }
                    int indexJugador = -1;
                    for (int i = 0; i < participants.size(); i++) {
                        if (participants.get(i).getId().equals(propietari.getId())) {
                            indexJugador = i;
                            break;
                        }
                    }
                    if (indexJugador == -1) {
                        throw new IllegalStateException("El propietari de la construcció pertany a una altra partida");
                    }
                    propietari.buscarRecurs(tipus).sumarQuantitat(quantitat);
                    for (int i = 0; i < tipusRecursos.length; i++) {
                        if (tipusRecursos[i] == tipus) {
                            produccio[indexJugador][i] += quantitat;
                            break;
                        }
                    }
                }
            }
        }

        partida.setUltimaTirada(tirada);
        partida.setDausTirats(true);
        // Com en el DAO, el moviment del lladre queda fora d'aquests endpoints.
        partida.setLladrePendent(false);
        List<RecursosDTO> recursosRebuts = new ArrayList<>();
        for (int i = 0; i < participants.size(); i++) {
            Jugador jugador = participants.get(i);
            List<RecursDTO> recursos = new ArrayList<>();
            for (int j = 0; j < tipusRecursos.length; j++) {
                recursos.add(new RecursDTO(tipusRecursos[j].name(), produccio[i][j]));
            }
            recursosRebuts.add(new RecursosDTO(jugador.getId(), jugador.getNom(), recursos));
        }
        return new TiradaDTO(dau1, dau2, tirada, recursosRebuts);
    }

    private void validarPeticio(NuevaPartidaRequest peticio) {
        if (peticio == null || peticio.getJugadores() == null) {
            throw new IllegalArgumentException("Falta la llista de jugadors");
        }
        List<String> noms = peticio.getJugadores();
        if (noms.size() < 2 || noms.size() > 4) {
            throw new IllegalArgumentException("La partida ha de tenir entre 2 i 4 jugadors");
        }
        for (String nom : noms) {
            if (nom == null || nom.trim().isEmpty() || nom.length() > 50) {
                throw new IllegalArgumentException("Cada jugador ha de tenir un nom d'entre 1 i 50 caràcters");
            }
        }
        if (peticio.getPuntosParaGanar() != null && peticio.getPuntosParaGanar() <= 0) {
            throw new IllegalArgumentException("Els punts per guanyar han de ser superiors a zero");
        }
    }

    private void assignarTerrenysINumeros(Partida partida, List<Hexagon> elements) {
        List<Terreny> terrenys = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            terrenys.add(Terreny.BOSC);
            terrenys.add(Terreny.PASTURES);
            terrenys.add(Terreny.CAMPS);
        }
        for (int i = 0; i < 3; i++) {
            terrenys.add(Terreny.TURONS);
            terrenys.add(Terreny.MUNTANYES);
        }
        terrenys.add(Terreny.DESERT);
        Collections.shuffle(terrenys);
        List<Integer> numeros = new ArrayList<>(List.of(2, 3, 3, 4, 4, 5, 5, 6, 6, 8, 8, 9, 9, 10, 10, 11, 11, 12));
        Collections.shuffle(numeros);
        int indexNumero = 0;
        for (int i = 0; i < elements.size(); i++) {
            Hexagon hexagon = elements.get(i);
            Terreny terreny = terrenys.get(i);
            hexagon.setTerreny(terreny);
            if (terreny == Terreny.DESERT) {
                hexagon.setNumero(null);
                partida.setHexagonLladre(hexagon);
            } else {
                hexagon.setNumero(numeros.get(indexNumero));
                indexNumero++;
            }
        }
    }

    private void afegirCartes(Partida partida, TipusCartaDesenvolupament tipus, int quantitat) {
        for (int i = 0; i < quantitat; i++) {
            CartaDesenvolupament carta = new CartaDesenvolupament(tipus, partida, null, null, false);
            cartes.save(carta);
        }
    }

    private TipusRecurs recursDelTerreny(Terreny terreny) {
        if (terreny == null) {
            throw new IllegalStateException("L'hexàgon no té terreny assignat");
        }
        switch (terreny) {
            case BOSC:
                return TipusRecurs.FUSTA;
            case TURONS:
                return TipusRecurs.MAO;
            case PASTURES:
                return TipusRecurs.LLANA;
            case CAMPS:
                return TipusRecurs.BLAT;
            case MUNTANYES:
                return TipusRecurs.MINERAL;
            default:
                return null; // El desert no produeix recursos.
        }
    }
}
