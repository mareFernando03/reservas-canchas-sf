"""Corre las pruebas del informe del TP3 de punta a punta y guarda la salida en docs/TP3-pruebas.log.

Uso (desde la raíz del repo, con Docker Desktop andando): python backend/pruebas.py
0. Línea base con 1 réplica.  1. Carga con 3 réplicas.  2. El mismo turno pedido 20 veces a la
vez, y el reintento del ganador.  3. Se cae una réplica en medio de la carga.  4. Escalado a 5.
"""
import json
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

BACKEND = Path(__file__).resolve().parent
LOG = BACKEND.parent / "docs" / "TP3-pruebas.log"
salida = []


def anotar(texto=""):
    print(texto)
    salida.append(texto)


def compose(*args):
    subprocess.run(["docker", "compose", *args], cwd=BACKEND, check=True, capture_output=True)


def escalar(n):
    compose("up", "-d", "--build", "--scale", f"api={n}")
    time.sleep(12)  # que las réplicas pasen el healthcheck y nginx las descubra (valid=5s)


def carga(pedidos=2000, concurrencia=50):
    r = subprocess.run([sys.executable, str(BACKEND / "prueba_carga.py"), str(pedidos), str(concurrencia)],
                       capture_output=True, text=True, encoding="utf-8")
    for linea in r.stdout.strip().splitlines():
        anotar(linea)


def reservar(usuario):
    datos = json.dumps({"cancha_id": 4, "fecha": "2026-10-24", "hora": 20, "usuario": usuario}).encode()
    req = urllib.request.Request("http://localhost:8080/reservas", data=datos, method="POST",
                                 headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req) as r:
            return r.status, r.headers["X-Instancia"]
    except urllib.error.HTTPError as e:
        return e.code, e.headers["X-Instancia"]


compose("down", "-v")
anotar("== 0. Línea base: 1 réplica ==")
escalar(1)
carga()

anotar("== 1. Carga con 3 réplicas ==")
escalar(3)
carga()

anotar("== 2. El mismo turno pedido 20 veces a la vez (20 usuarios distintos) ==")
with ThreadPoolExecutor(20) as hilos:
    resultados = list(hilos.map(lambda i: reservar(f"jugador{i}"), range(20)))
anotar(f"respuestas: {dict(sorted(Counter(s for s, _ in resultados).items()))} · réplicas que atendieron: "
       f"{len({i for _, i in resultados})}")
with urllib.request.urlopen("http://localhost:8080/canchas/4/turnos?fecha=2026-10-24") as r:
    anotar("disponibilidad: " + r.read().decode())
ganador = next(f"jugador{i}" for i, (s, _) in enumerate(resultados) if s == 201)
anotar(f"el ganador reintenta su POST: {reservar(ganador)[0]} (idempotente: el turno ya es suyo)")
anotar(f"otro usuario intenta el mismo turno: {reservar('jugador99')[0]}")

anotar("== 3. Se cae una réplica (docker kill) a los 2 s de empezar la carga ==")
threading.Timer(2, lambda: subprocess.run(["docker", "kill", "canchas-api-2"], capture_output=True)).start()
carga(4000)
anotar("[canchas-api-2 apagada a los 2 s]")

anotar("== 4. Escalado a 5 réplicas ==")
escalar(5)
carga()

compose("down", "-v")
LOG.write_text("\n".join(salida) + "\n", encoding="utf-8")
print(f"\nGuardado en {LOG}")
