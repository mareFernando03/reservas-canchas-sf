"""API de reservas de Reservas Canchas SF (TP3: estrategia de escalabilidad).

Es *stateless*: ninguna réplica guarda datos propios. Las reservas viven en Redis, así que da
igual qué réplica atienda cada pedido y se pueden agregar o sacar réplicas en cualquier momento.
Cada respuesta lleva el encabezado X-Instancia para ver qué réplica la atendió.
"""
import os
import socket
from datetime import date

import redis
from fastapi import FastAPI, HTTPException, Query, Response
from pydantic import BaseModel

INSTANCIA = socket.gethostname()
HORAS = range(17, 24)  # turnos de 17 a 23, como en la app

# Los mismos datos de ejemplo que la app (Canchas.kt).
CANCHAS = [
    {"id": 1, "nombre": "Cancha 1 - Fútbol 5", "club": "Club Centro", "deporte": "Fútbol 5", "precioHora": 30000},
    {"id": 2, "nombre": "Cancha 2 - Fútbol 5", "club": "Club Centro", "deporte": "Fútbol 5", "precioHora": 30000},
    {"id": 3, "nombre": "Pádel techada", "club": "Pádel Norte", "deporte": "Pádel", "precioHora": 24000},
    {"id": 4, "nombre": "Fútbol 7 sintético", "club": "Complejo Oeste", "deporte": "Fútbol 7", "precioHora": 45000},
]

# Timeouts cortos: si Redis no responde, el pedido falla rápido en vez de colgar la réplica.
db = redis.Redis(host=os.environ.get("REDIS_HOST", "localhost"), port=6379, decode_responses=True,
                 socket_connect_timeout=1, socket_timeout=1)
app = FastAPI(title="Reservas Canchas SF")


@app.middleware("http")
async def marcar_instancia(request, call_next):
    respuesta = await call_next(request)
    respuesta.headers["X-Instancia"] = INSTANCIA
    return respuesta


@app.get("/vivo")
def vivo():
    """La réplica arrancó. Lo usa el startupProbe de Cloud Run: no depende de Redis, para que un
    problema de Redis no impida levantar instancias nuevas."""
    return {"estado": "ok", "instancia": INSTANCIA}


@app.get("/salud")
def salud():
    """La réplica y su conexión a Redis andan. Lo usa el healthcheck de Docker. nginx no lo consulta:
    saca una réplica de servicio cuando le falla un pedido (max_fails)."""
    db.ping()
    return {"estado": "ok", "instancia": INSTANCIA}


@app.get("/canchas")
def canchas(pagina: int = Query(1, ge=1), tamanio: int = Query(20, ge=1, le=50)):
    """Lista paginada: el móvil pide de a una página, nunca la lista entera."""
    desde = (pagina - 1) * tamanio
    return {"pagina": pagina, "total": len(CANCHAS), "canchas": CANCHAS[desde:desde + tamanio]}


def _clave(cancha_id: int, fecha: date, hora: int) -> str:
    return f"turno:{cancha_id}:{fecha.isoformat()}:{hora}"


def _cancha(cancha_id: int) -> dict:
    cancha = next((c for c in CANCHAS if c["id"] == cancha_id), None)
    if cancha is None:
        raise HTTPException(404, "No existe esa cancha")
    return cancha


@app.get("/canchas/{cancha_id}/turnos")
def turnos(cancha_id: int, fecha: date):
    """Disponibilidad de un día: sólo hora y libre/ocupado, el mínimo que necesita la pantalla."""
    _cancha(cancha_id)
    ocupados = db.mget([_clave(cancha_id, fecha, h) for h in HORAS])
    return [{"hora": h, "libre": o is None} for h, o in zip(HORAS, ocupados)]


class Reserva(BaseModel):
    cancha_id: int
    fecha: date
    hora: int
    usuario: str


@app.post("/reservas", status_code=201)
def reservar(reserva: Reserva, respuesta: Response):
    _cancha(reserva.cancha_id)
    if reserva.hora not in HORAS:
        raise HTTPException(422, "Los turnos son de 17 a 23")
    clave = _clave(reserva.cancha_id, reserva.fecha, reserva.hora)
    # SET NX es atómico en Redis: si dos réplicas reciben el mismo turno a la vez, sólo una gana.
    if not db.set(clave, reserva.usuario, nx=True):
        if db.get(clave) == reserva.usuario:
            # Reintento del mismo usuario (por ejemplo, tras un 502 del balanceador): el turno ya es
            # suyo. Así el cliente puede reintentar un POST sin riesgo de reservar dos veces.
            respuesta.status_code = 200
            return {"reservado": True, "instancia": INSTANCIA}
        raise HTTPException(409, "Ese turno ya está reservado")
    return {"reservado": True, "instancia": INSTANCIA}
