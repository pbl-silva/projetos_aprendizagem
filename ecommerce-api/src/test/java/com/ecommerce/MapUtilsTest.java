package com.ecommerce;

import com.ecommerce.ui.util.MapUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes do MapUtils")
class MapUtilsTest {

    @Test
    @DisplayName("toMap deve retornar o proprio mapa quando o objeto ja e um Map")
    void testToMapComMap() {
        Map<String, Object> original = Map.of("chave", "valor");

        Map<String, Object> resultado = MapUtils.toMap(original);

        assertEquals(original, resultado);
    }

    @Test
    @DisplayName("toMap deve retornar mapa vazio quando o objeto nao e um Map")
    void testToMapComObjetoNaoMap() {
        Map<String, Object> resultado = MapUtils.toMap("não é um mapa");

        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("getAsLong deve converter Long, Integer e String")
    void testGetAsLong() {
        Map<String, Object> map = new HashMap<>();
        map.put("comoLong", 10L);
        map.put("comoInteger", 20);
        map.put("comoString", "30");
        map.put("invalido", "abc");
        map.put("nulo", null);

        assertEquals(10L, MapUtils.getAsLong(map, "comoLong"));
        assertEquals(20L, MapUtils.getAsLong(map, "comoInteger"));
        assertEquals(30L, MapUtils.getAsLong(map, "comoString"));
        assertNull(MapUtils.getAsLong(map, "invalido"));
        assertNull(MapUtils.getAsLong(map, "nulo"));
        assertNull(MapUtils.getAsLong(map, "inexistente"));
    }

    @Test
    @DisplayName("getAsInteger deve converter Integer, Long e String")
    void testGetAsInteger() {
        Map<String, Object> map = new HashMap<>();
        map.put("comoInteger", 10);
        map.put("comoLong", 20L);
        map.put("comoString", "30");
        map.put("invalido", "abc");

        assertEquals(10, MapUtils.getAsInteger(map, "comoInteger"));
        assertEquals(20, MapUtils.getAsInteger(map, "comoLong"));
        assertEquals(30, MapUtils.getAsInteger(map, "comoString"));
        assertNull(MapUtils.getAsInteger(map, "invalido"));
        assertNull(MapUtils.getAsInteger(map, "inexistente"));
    }

    @Test
    @DisplayName("getAsDouble deve converter Double, BigDecimal, Integer, Long e String")
    void testGetAsDouble() {
        Map<String, Object> map = new HashMap<>();
        map.put("comoDouble", 1.5);
        map.put("comoBigDecimal", new BigDecimal("2.5"));
        map.put("comoInteger", 3);
        map.put("comoLong", 4L);
        map.put("comoString", "5.5");
        map.put("invalido", "abc");

        assertEquals(1.5, MapUtils.getAsDouble(map, "comoDouble"));
        assertEquals(2.5, MapUtils.getAsDouble(map, "comoBigDecimal"));
        assertEquals(3.0, MapUtils.getAsDouble(map, "comoInteger"));
        assertEquals(4.0, MapUtils.getAsDouble(map, "comoLong"));
        assertEquals(5.5, MapUtils.getAsDouble(map, "comoString"));
        assertNull(MapUtils.getAsDouble(map, "invalido"));
        assertNull(MapUtils.getAsDouble(map, "inexistente"));
    }

    @Test
    @DisplayName("getAsBigDecimal deve converter BigDecimal, Double, Integer, Long e String")
    void testGetAsBigDecimal() {
        Map<String, Object> map = new HashMap<>();
        map.put("comoBigDecimal", new BigDecimal("1.1"));
        map.put("comoDouble", 2.2);
        map.put("comoInteger", 3);
        map.put("comoLong", 4L);
        map.put("comoString", "5.5");
        map.put("invalido", "abc");

        assertEquals(new BigDecimal("1.1"), MapUtils.getAsBigDecimal(map, "comoBigDecimal"));
        assertEquals(BigDecimal.valueOf(2.2), MapUtils.getAsBigDecimal(map, "comoDouble"));
        assertEquals(BigDecimal.valueOf(3L), MapUtils.getAsBigDecimal(map, "comoInteger"));
        assertEquals(BigDecimal.valueOf(4L), MapUtils.getAsBigDecimal(map, "comoLong"));
        assertEquals(new BigDecimal("5.5"), MapUtils.getAsBigDecimal(map, "comoString"));
        assertNull(MapUtils.getAsBigDecimal(map, "invalido"));
        assertNull(MapUtils.getAsBigDecimal(map, "inexistente"));
    }

    @Test
    @DisplayName("getAsString deve converter valores ou retornar vazio quando nulo")
    void testGetAsString() {
        Map<String, Object> map = new HashMap<>();
        map.put("chave", 123);
        map.put("nulo", null);

        assertEquals("123", MapUtils.getAsString(map, "chave"));
        assertEquals("", MapUtils.getAsString(map, "nulo"));
        assertEquals("", MapUtils.getAsString(map, "inexistente"));
    }

    @Test
    @DisplayName("getAsBoolean deve converter Boolean e String, ou retornar false")
    void testGetAsBoolean() {
        Map<String, Object> map = new HashMap<>();
        map.put("comoBoolean", true);
        map.put("comoString", "true");
        map.put("outraCoisa", 123);

        assertTrue(MapUtils.getAsBoolean(map, "comoBoolean"));
        assertTrue(MapUtils.getAsBoolean(map, "comoString"));
        assertFalse(MapUtils.getAsBoolean(map, "outraCoisa"));
        assertFalse(MapUtils.getAsBoolean(map, "inexistente"));
    }
}