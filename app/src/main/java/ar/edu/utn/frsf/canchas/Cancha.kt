package ar.edu.utn.frsf.canchas

data class Cancha(
    val id: Int,
    val nombre: String,
    val club: String,
    val deporte: String,
    val direccion: String,
    val telefono: String,
    val precioHora: Int,
    val latitud: Double,
    val longitud: Double,
)

/**
 * Datos de ejemplo. En el TP1 no hay backend: la lista vive en memoria.
 * Los clubes y teléfonos son ficticios; las coordenadas caen en San Francisco (Córdoba).
 */
object Canchas {
    val todas = listOf(
        Cancha(1, "Cancha 1 - Fútbol 5", "Club Centro", "Fútbol 5", "Bv. 25 de Mayo 1500", "03564000001", 30000, -31.4275, -62.0832),
        Cancha(2, "Cancha 2 - Fútbol 5", "Club Centro", "Fútbol 5", "Bv. 25 de Mayo 1500", "03564000001", 30000, -31.4275, -62.0832),
        Cancha(3, "Pádel techada", "Pádel Norte", "Pádel", "Av. Libertador 800", "03564000002", 24000, -31.4190, -62.0870),
        Cancha(4, "Fútbol 7 sintético", "Complejo Oeste", "Fútbol 7", "Av. Rosario de Santa Fe 3000", "03564000003", 45000, -31.4330, -62.1010),
    )

    fun porId(id: Int): Cancha? = todas.firstOrNull { it.id == id }
}
