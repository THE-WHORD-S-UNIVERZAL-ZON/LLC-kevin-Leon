#!/bin/bash
echo "=================================================="
echo "1. CARPETA ACTUAL Y ARCHIVOS PRINCIPALES"
echo "=================================================="
pwd
ls -la

echo ""
echo "=================================================="
echo "2. ÁRBOL COMPLETO DEL PROYECTO"
echo "=================================================="
find . -maxdepth 3 -not -path '*/.*' -not -path './build*' | sort

echo ""
echo "=================================================="
echo "3. TODOS LOS ARCHIVOS DE CÓDIGO DETECTADOS"
echo "=================================================="
echo "--- Kotlin / Java ---"
find . -type f \( -name "*.kt" -o -name "*.java" \)
echo "--- Python ---"
find . -type f -name "*.py"
echo "--- Web / Cloudflare / JS / HTML / Config ---"
find . -type f \( -name "*.js" -o -name "*.ts" -o -name "*.html" -o -name "*.json" -o -name "*.toml" \) -not -path '*/.*'

echo ""
echo "=================================================="
echo "4. BÚSQUEDA DE LA RESPUESTA FALSA Y EL SERVIDOR"
echo "=================================================="
echo "[Buscando 'Mensaje procesado' o 'sincronizado']:"
grep -rnEI "Mensaje procesado|sincronizado" . 2>/dev/null

echo ""
echo "[Buscando referencias a kelepr13 o workers.dev]:"
grep -rnEI "workers\.dev|kelepr13" . 2>/dev/null

echo ""
echo "=================================================="
echo "5. MODELOS DE INTELIGENCIA ARTIFICIAL (GGUF / PESOS)"
echo "=================================================="
find . -type f \( -name "*.gguf" -o -name "*.bin" \) -o -size +20M 2>/dev/null

echo ""
echo "=================================================="
echo "6. ESTADO DE GIT"
echo "=================================================="
git status -s
