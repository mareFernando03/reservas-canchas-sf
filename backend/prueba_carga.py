"""Prueba de carga contra el balanceador local (TP3).

Uso: python backend/prueba_carga.py [pedidos] [concurrencia]
Manda pedidos de disponibilidad en paralelo y cuenta cuántos atendió cada réplica, cuántos
fallaron y la latencia (mediana y percentil 95). Sólo usa la biblioteca estándar.
"""
import statistics
import sys
import time
import urllib.request
from collections import Counter
from concurrent.futures import ThreadPoolExecutor

URL = "http://localhost:8080/canchas/1/turnos?fecha=2026-10-24"
pedidos = int(sys.argv[1]) if len(sys.argv) > 1 else 2000
concurrencia = int(sys.argv[2]) if len(sys.argv) > 2 else 50


def pedir(_):
    inicio = time.perf_counter()
    try:
        with urllib.request.urlopen(URL, timeout=5) as r:
            return r.headers["X-Instancia"], time.perf_counter() - inicio
    except Exception:
        return None, time.perf_counter() - inicio


inicio = time.perf_counter()
with ThreadPoolExecutor(concurrencia) as hilos:
    resultados = list(hilos.map(pedir, range(pedidos)))
total = time.perf_counter() - inicio

ok = [lat for inst, lat in resultados if inst]
por_instancia = Counter(inst for inst, _ in resultados if inst)
print(f"{pedidos} pedidos, {concurrencia} en paralelo, {total:.1f} s ({pedidos / total:.0f} pedidos/s)")
print(f"OK: {len(ok)}  fallidos: {pedidos - len(ok)}")
if ok:
    ok.sort()
    print(f"latencia mediana {statistics.median(ok) * 1000:.0f} ms · p95 {ok[int(len(ok) * 0.95) - 1] * 1000:.0f} ms")
for inst, n in sorted(por_instancia.items()):
    print(f"  réplica {inst}: {n} pedidos")
