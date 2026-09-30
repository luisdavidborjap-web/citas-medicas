# Citas médicas

Sistema académico para una clínica que administra disponibilidad, reservas, cancelaciones y recordatorios.

## Arquitectura
Monolito modular con Spring Boot, PostgreSQL, React, REST y Docker.

## Decisiones principales
- Las reservas duplicadas se impiden con una restricción única en PostgreSQL para `doctor + horario`.
- Solo se guardan los datos necesarios para coordinar la cita: nombre, contacto, doctor, horario y motivo breve. No se guarda historia clínica.
- Roles de demostración: ADMIN, RECEPCION y DOCTOR.
- Los recordatorios se reintentan automáticamente hasta 3 veces si el proveedor falla.
- La auditoría registra usuario, rol, acción, entidad, detalle y fecha.
- Métricas: citas duplicadas, ausencias, tiempo para encontrar turno y recordatorios entregados.

## Ejecutar
Con Docker Desktop abierto:

```bash
docker compose up --build
```

Abrir:

```text
http://localhost:8082
```

## Variables del .env
```env
POSTGRES_DB=citas_db
POSTGRES_USER=citas_user
POSTGRES_PASSWORD=citas123
REMINDER_PROVIDER_MODE=OK
```

Para simular que el proveedor de recordatorios falla, cambia:

```env
REMINDER_PROVIDER_MODE=FAIL
```

También existe un paciente de prueba cuyo teléfono contiene `fail`, para probar reintentos sin cambiar la configuración general.
