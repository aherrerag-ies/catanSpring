package com.spring.catan;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.catan.dto.NuevaPartidaRequest;
import com.spring.catan.dto.PartidaDTO;
import com.spring.catan.repository.*;
import com.spring.catan.service.*;
import enumeration.EstatPartida;
import enumeration.Terreny;
import enumeration.TipusRecurs;
import herencia.Ciutat;
import herencia.Poblat;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import relacions.Jugador;
import relacions.Partida;
import taulell.Hexagon;
import taulell.HexagonVertex;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:spring_catan;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
class CatanApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PartidaService servei;
    @Autowired PartidaRepository partides;
    @Autowired JugadorRepository jugadors;
    @Autowired HexagonRepository hexagons;
    @Autowired EntityManager em;
    @Autowired TransactionTemplate transaccio;
    @MockitoBean DausService daus;

    @Test
    void crearRetornaPartidaCompletaIPersisteixLaTopologia() throws Exception {
        String resposta = mvc.perform(post("/partida").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jugadors":["Anna","Biel","Cris"],"puntsPerGuanyar":12}
                                """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode partida = json.readTree(resposta);
        int id = partida.get("idPartida").asInt();
        assertEquals("PREPARACIO", partida.get("estat").asText());
        assertEquals(12, partida.get("puntsPerGuanyar").asInt());
        assertEquals(3, partida.get("jugadors").size());
        assertEquals(19, partida.get("hexagons").size());
        int deserts = 0;
        int lladres = 0;
        for (JsonNode h : partida.get("hexagons")) {
            if (h.get("lladre").asBoolean()) lladres++;
            if (h.get("terreny").asText().equals("DESERT")) {
                deserts++;
                assertTrue(h.get("numero").isNull());
            } else {
                assertTrue(h.get("numero").asInt() >= 2);
                assertTrue(h.get("numero").asInt() <= 12);
                assertNotEquals(7, h.get("numero").asInt());
            }
        }
        assertEquals(1, deserts);
        assertEquals(1, lladres);
        transaccio.executeWithoutResult(s -> {
            assertEquals(54L, comptar("select count(v) from Vertex v where v.partida.id = :id", id));
            assertEquals(72L, comptar("select count(a) from Aresta a where a.partida.id = :id", id));
            assertEquals(114L, comptar("select count(h) from HexagonVertex h where h.hexagon.partida.id = :id", id));
            assertEquals(21L, comptar("select count(c) from CartaDesenvolupament c where c.partida.id = :id", id));
            assertEquals(15L, comptar("select count(r) from RecursJugador r where r.jugador.partida.id = :id", id));
        });
    }

    @Test
    void acceptaElJsonAnteriorIPuntsPerDefecte() throws Exception {
        mvc.perform(post("/partida").contentType(MediaType.APPLICATION_JSON)
                        .content(""" 
                                {"jugadores":["Anna","Biel","Cris","Duna"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.puntsPerGuanyar").value(10))
                .andExpect(jsonPath("$.jugadors.length()").value(4));
    }

    static Stream<String> peticionsInvalides() {
        return Stream.of("{}", "{\"jugadors\":[\"Anna\"]}",
                "{\"jugadors\":[\"A\",\"B\",\"C\",\"D\",\"E\"]}",
                "{\"jugadors\":[\"Anna\",\" \"]}", "{\"jugadors\":[\"Anna\",null]}",
                "{\"jugadors\":[\"Anna\",\"Biel\"],\"puntsPerGuanyar\":0}",
                "{\"jugadors\":[\"Anna\",\"Biel\"],\"puntsPerGuanyar\":-1}");
    }

    @ParameterizedTest
    @MethodSource("peticionsInvalides")
    void rebutjaPeticionsInvalidesSenseCrearPartides(String peticio) throws Exception {
        long abans = partides.count();
        mvc.perform(post("/partida").contentType(MediaType.APPLICATION_JSON).content(peticio))
                .andExpect(status().isBadRequest());
        assertEquals(abans, partides.count());
    }

    @Test
    void taulerRetornaUnArrayAmbPoblatsICiutatsAdjacents() throws Exception {
        Fixture fixture = prepararProduccio(false);
        String resposta = mvc.perform(get("/partida/{id}/tauler", fixture.partida()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode tauler = json.readTree(resposta);
        assertTrue(tauler.isArray());
        assertEquals(19, tauler.size());
        JsonNode objectiu = null;
        for (JsonNode hexagon : tauler) {
            if (hexagon.get("id").asInt() == fixture.hexagon()) objectiu = hexagon;
        }
        assertNotNull(objectiu);
        assertEquals("BOSC", objectiu.get("terreny").asText());
        assertEquals(8, objectiu.get("numero").asInt());
        assertFalse(objectiu.get("lladre").asBoolean());
        JsonNode construccions = objectiu.get("construccions");
        assertEquals(2, construccions.size());
        assertEquals("POBLAT", construccions.get(0).get("tipus").asText());
        assertEquals("CIUTAT", construccions.get(1).get("tipus").asText());
        assertEquals(fixture.primerJugador(), construccions.get(0).get("idJugador").asInt());
        assertTrue(construccions.get(0).hasNonNull("idVertex"));
        assertTrue(construccions.get(0).hasNonNull("nomJugador"));
    }

    @Test
    void recursosRetornaJugadorAmbInventariForaDeLaTransaccio() throws Exception {
        Fixture fixture = prepararProduccio(false);
        mvc.perform(get("/jugador/{id}/recursos", fixture.primerJugador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idJugador").value(fixture.primerJugador()))
                .andExpect(jsonPath("$.nom").isString())
                .andExpect(jsonPath("$.recursos.length()").value(5))
                .andExpect(jsonPath("$.recursos[?(@.tipus == 'FUSTA')].quantitat").value(org.hamcrest.Matchers.contains(5)));
    }

    @Test
    void tirarDausRetornaLaProduccioRealIPersisteixElsRecursos() throws Exception {
        Fixture fixture = prepararProduccio(false);
        when(daus.tirar()).thenReturn(4, 4);
        JsonNode tirada = ferTirada(fixture.partida());
        assertEquals(4, tirada.get("dau1").asInt());
        assertEquals(4, tirada.get("dau2").asInt());
        assertEquals(8, tirada.get("tirada").asInt());
        assertEquals(2, tirada.get("recursosRebuts").size());
        assertEquals(1, fustaRebuda(tirada, fixture.primerJugador()));
        assertEquals(2, fustaRebuda(tirada, fixture.segonJugador()));
        assertEquals(6, inventariFusta(fixture.primerJugador()));
        assertEquals(7, inventariFusta(fixture.segonJugador()));
        transaccio.executeWithoutResult(s -> {
            Partida partida = partides.findById(fixture.partida()).orElseThrow();
            assertEquals(8, partida.getUltimaTirada());
            assertTrue(partida.getDausTirats());
        });
        mvc.perform(post("/partida/{id}/daus", fixture.partida()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El jugador actiu ja ha tirat els daus aquest torn"));
        assertEquals(6, inventariFusta(fixture.primerJugador()));
    }

    @Test
    void unaTiradaDeSetInclouTotsElsJugadorsAmbProduccioZero() throws Exception {
        Fixture fixture = prepararProduccio(false);
        when(daus.tirar()).thenReturn(3, 4);
        JsonNode tirada = ferTirada(fixture.partida());
        assertEquals(7, tirada.get("tirada").asInt());
        assertEquals(2, tirada.get("recursosRebuts").size());
        assertEquals(0, fustaRebuda(tirada, fixture.primerJugador()));
        assertEquals(0, fustaRebuda(tirada, fixture.segonJugador()));
        assertEquals(5, inventariFusta(fixture.primerJugador()));
    }

    @Test
    void unHexagonAmbLladreNoProdueixRecursos() throws Exception {
        Fixture fixture = prepararProduccio(true);
        when(daus.tirar()).thenReturn(4, 4);
        JsonNode tirada = ferTirada(fixture.partida());
        assertEquals(0, fustaRebuda(tirada, fixture.primerJugador()));
        assertEquals(0, fustaRebuda(tirada, fixture.segonJugador()));
        assertEquals(5, inventariFusta(fixture.segonJugador()));
    }

    @Test
    void rebutjaDausForaDeLaPartidaEnCurs() throws Exception {
        PartidaDTO partida = crearPartida();
        mvc.perform(post("/partida/{id}/daus", partida.idPartida()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Només es poden tirar els daus en una partida en curs"));
        transaccio.executeWithoutResult(s -> {
            Partida p = partides.findById(partida.idPartida()).orElseThrow();
            assertNull(p.getUltimaTirada());
            assertFalse(p.getDausTirats());
            p.setEstat(EstatPartida.FINALITZADA);
        });
        mvc.perform(post("/partida/{id}/daus", partida.idPartida())).andExpect(status().isBadRequest());
    }

    @Test
    void recursosIPartidesInexistentsRetornen404() throws Exception {
        mvc.perform(get("/jugador/2147483647/recursos")).andExpect(status().isNotFound());
        mvc.perform(get("/partida/2147483647/tauler")).andExpect(status().isNotFound());
        mvc.perform(post("/partida/2147483647/daus")).andExpect(status().isNotFound());
    }

    private PartidaDTO crearPartida() {
        NuevaPartidaRequest peticio = new NuevaPartidaRequest();
        peticio.setJugadores(List.of("Anna", "Biel"));
        return servei.crear(peticio);
    }

    private long comptar(String consulta, int idPartida) {
        return em.createQuery(consulta, Long.class).setParameter("id", idPartida).getSingleResult();
    }

    private Fixture prepararProduccio(boolean lladre) {
        PartidaDTO creada = crearPartida();
        return transaccio.execute(s -> {
            Partida partida = partides.findById(creada.idPartida()).orElseThrow();
            partida.setEstat(EstatPartida.EN_CURS);
            List<Jugador> participants = jugadors.findByPartida_IdOrderByOrdreTorn(partida.getId());
            for (Jugador jugador : participants) {
                jugador.buscarRecurs(TipusRecurs.FUSTA).setQuantitat(5);
            }
            List<Hexagon> tauler = hexagons.findByPartida_IdOrderByCodi(partida.getId());
            for (Hexagon h : tauler) {
                h.setNumero(null);
                h.setTerreny(Terreny.BOSC);
                h.setNumero(2);
            }
            Hexagon objectiu = tauler.get(0);
            objectiu.setNumero(8);
            partida.setHexagonLladre(lladre ? objectiu : tauler.get(18));
            List<HexagonVertex> vertexs = em.createQuery(
                    "select hv from HexagonVertex hv where hv.hexagon.id = :id order by hv.vertex.codi",
                    HexagonVertex.class).setParameter("id", objectiu.getId()).getResultList();
            em.persist(new Poblat(participants.get(0), vertexs.get(0).getVertex()));
            em.persist(new Ciutat(participants.get(1), vertexs.get(1).getVertex()));
            return new Fixture(partida.getId(), objectiu.getId(),
                    participants.get(0).getId(), participants.get(1).getId());
        });
    }

    private JsonNode ferTirada(int idPartida) throws Exception {
        return json.readTree(mvc.perform(post("/partida/{id}/daus", idPartida))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private int fustaRebuda(JsonNode tirada, int idJugador) {
        for (JsonNode jugador : tirada.get("recursosRebuts")) {
            if (jugador.get("idJugador").asInt() == idJugador) {
                for (JsonNode recurs : jugador.get("recursos")) {
                    if (recurs.get("tipus").asText().equals("FUSTA")) return recurs.get("quantitat").asInt();
                }
            }
        }
        throw new AssertionError("No s'ha trobat la producció de fusta del jugador");
    }

    private int inventariFusta(int idJugador) {
        return transaccio.execute(s -> jugadors.findById(idJugador).orElseThrow()
                .buscarRecurs(TipusRecurs.FUSTA).getQuantitat());
    }

    private record Fixture(int partida, int hexagon, int primerJugador, int segonJugador) {
    }
}
