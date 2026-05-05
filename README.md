# Sistema de Inventario y Responsivas con QR/Barras — Club de Golf Las Lomas

> Proyecto integrador: aplicación web interna para el área de Sistemas/TI del Club de Golf Las Lomas.

## Resumen ejecutivo (Descripción, problema, solución, arquitectura)

### Descripción
Esta solución centraliza el control del inventario de equipo de TI y automatiza la generación de **responsivas** (PDF) con **folio** y **códigos QR/de barras** para identificación y consulta rápida por escaneo.

### Problema identificado
El inventario gestionado en hojas de cálculo y registros manuales provoca **inconsistencias**, duplicidad de información, trazabilidad limitada (difícil responder “quién tiene qué y desde cuándo”), y pérdida de tiempo al asignar equipo y elaborar responsivas.

### Solución
- **Inventario centralizado** en una base de datos **MySQL**.
- **Aplicación web** para registrar/consultar/actualizar equipos (CRUD).
- **Asignación de equipos** a responsables/áreas con **historial**.
- **Generación automática de responsivas (PDF)** con folio.
- **Etiquetado y consulta por QR/barcode** para acelerar búsquedas y auditorías.
- **Bitácora/auditoría** para registrar cambios.
- Gestión de código con GitHub (branches/PR) y validación con CI (Travis CI + pruebas JUnit).

### Arquitectura (alto nivel)
- Usuarios TI (navegador/escáner) → Frontend Web → API Java (Spring Boot) → MySQL
- Generación/lectura de códigos QR/barcode y generación de PDFs como servicios del backend.
- Integración de proyecto con tablero (Zube) e integración continua (Travis CI).


---

## Tabla de contenidos (ToC)
- [Resumen ejecutivo](#resumen-ejecutivo-descripción-problema-solución-arquitectura)
- [Arquitectura](#arquitectura-detalle)
- [Requerimientos](#requerimientos)
- [Instalación](#instalación)
- [Configuración](#configuración)
- [Uso](#uso)
  - [Usuario final (TI)](#usuario-final-ti)
  - [Administrador](#administrador)
- [Pruebas](#pruebas)
- [Contribución](#contribución)
- [Roadmap](#roadmap)
- [Producto](#producto)
- [Licencia](#licencia)

> Wiki/Docs externos (opcional):  
> - Wiki del repositorio: `https://github.com/<org>/<repo>/wiki`  
> - ReadTheDocs (si aplica): `https://<proyecto>.readthedocs.io/`

---

## Arquitectura (detalle)

**Componentes**
- **Frontend web**: interfaz para inventario, asignaciones, búsquedas y consultas por código.
- **Backend API (Java / Spring Boot)**:
  - Autenticación y roles
  - Inventario (CRUD + validaciones)
  - Asignaciones y generación de responsivas (PDF)
  - Generación/lectura QR/barcode
  - Bitácora/auditoría
- **Base de datos MySQL**: equipos, usuarios, asignaciones, historial, bitácora.
- **Almacenamiento de archivos** (local o nube): PDFs de responsivas/etiquetas.
- **CI**: Travis CI ejecuta pruebas JUnit automáticamente por commit/PR.
- **Gestión**: GitHub + Zube (tablero y milestones Beta/GA)

---

## Requerimientos

### Infraestructura / Servidores
- **Servidor de aplicación**: Java (Spring Boot, embebido) o contenedor (Docker).
- **Servidor web** (opcional): Nginx/Apache como reverse proxy.
- **Base de datos**: MySQL 8.x
- **Almacenamiento**: filesystem local o bucket (si se despliega en nube).

### Software / Dependencias
- **Java**: 17+ (recomendado: 17)
- **Build tool**: Maven (recomendado) o Gradle
- **MySQL**: local o remoto
- (Opcional) **Docker + Docker Compose** para levantar todo local “tipo producción”.

### Paquetes adicionales (según tu implementación)
- Spring Boot (web, data, security)
- Driver MySQL
- Librería de PDF (iText/OpenPDF/PDFBox)
- Librería QR/barcode (ZXing u otra)
- JUnit (para pruebas)

---

## Instalación

### 1) Clonar repositorio
```bash
git clone https://github.com/Jonathangranados123/tallerDeproductividadBasadaEnHerramientasTecnologicas.git

