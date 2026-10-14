import re
import math
import unicodedata
import urllib.request
import urllib.parse
import json
from collections import Counter

# Importar el motor matemático absoluto
try:
    import sympy as sp
except ImportError:
    sp = None

class WebPlatformBrain:
    def __init__(self):
        # 1. COMANDOS DE INTERFAZ WEB (Acciones locales)
        self.corpus = {
            "MUSICA": ["musica", "videos", "reproducir", "cancion", "tiraera", "escuchar", "youtube", "clip", "tema", "oficial"],
            "NEON_COLOR": ["color", "luces", "tema", "iluminacion", "ponlo", "cambia a", "pantalla", "brillo", "neon"],
            "LEGALES": ["normas", "politicas", "terminos", "privacidad", "reglas", "legal", "contrato", "empresa"],
            "PERFIL": ["cambiar nombre", "mi perfil", "alias", "quien soy", "usuario"]
        }
        
        # Variables simbólicas listas para usar en álgebra
        if sp:
            self.x, self.y, self.z = sp.symbols('x y z')

    def _normalizar(self, texto: str) -> str:
        if not texto: return ""
        texto = texto.lower()
        texto = ''.join(c for c in unicodedata.normalize('NFD', texto) if unicodedata.category(c) != 'mn')
        return ' '.join(re.sub(r'[^\w\s]', '', texto).split())

    def _vector(self, text: str):
        return Counter(self._normalizar(text).split())

    def _similitud(self, v1, v2):
        inter = set(v1.keys()) & set(v2.keys())
        num = sum([v1[x] * v2[x] for x in inter])
        d1 = math.sqrt(sum([v1[x]**2 for x in v1.keys()]))
        d2 = math.sqrt(sum([v2[x]**2 for x in v2.keys()]))
        return float(num) / (d1 * d2) if d1 and d2 else 0.0

    # EL BUSCADOR FANTASMA (Solo si no es matemáticas)
    def _buscar_en_red(self, pregunta: str) -> str:
        busqueda = re.sub(r'^(que es|quien es|como funciona|que significa|donde esta|quien fue|cual es)\s+', '', pregunta, flags=re.IGNORECASE)
        busqueda_codificada = urllib.parse.quote(busqueda.strip())
        url = f"https://es.wikipedia.org/w/api.php?action=query&prop=extracts&exintro=1&explaintext=1&exchars=400&generator=search&gsrsearch={busqueda_codificada}&gsrlimit=1&format=json"
        
        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'UniversalZonAI/1.0'})
            with urllib.request.urlopen(req, timeout=4) as response:
                data = json.loads(response.read().decode('utf-8'))
                pages = data.get('query', {}).get('pages', {})
                if pages:
                    page_id = list(pages.keys())[0]
                    extracto = pages[page_id].get('extract', '')
                    if extracto and not extracto.isspace():
                        return extracto.replace('\n', ' ').strip()
            return "La búsqueda está en blanco. No encontré información exacta sobre eso en la red."
        except Exception:
            return "Tuve un problema de red al buscar la respuesta. Verifica la conexión a internet."

    # EL TRADUCTOR MATEMÁTICO (El embudo hacia SymPy)
    def _resolver_matematicas(self, entrada: str) -> str:
        if not sp:
            return "El módulo matemático SymPy no está instalado en el sistema local."
        
        expr = entrada.lower()
        expr = re.sub(r'^(cuanto es|resuelve|calcula|dime el resultado de|que es igual a)\s+', '', expr)
        
        reemplazos = {
            " mas ": "+", " menos ": "-", " por ": "*", " multiplicado por ": "*",
            " entre ": "/", " dividido por ": "/", " dividido ": "/",
            " al cuadrado": "**2", " al cubo": "**3", " a la ": "**",
            " raiz cuadrada de ": "sqrt(", " raiz de ": "sqrt(",
            " seno de ": "sin(", " coseno de ": "cos(", " tangente de ": "tan(",
            " logaritmo de ": "log(", " pi ": "pi"
        }
        
        for palabra, simbolo in reemplazos.items():
            expr = expr.replace(palabra, simbolo)
        
        parentesis_abiertos = expr.count('(')
        parentesis_cerrados = expr.count(')')
        if parentesis_abiertos > parentesis_cerrados:
            expr += ')' * (parentesis_abiertos - parentesis_cerrados)
            
        try:
            if " x " in entrada or " equis " in entrada:
                expr = expr.replace("x", "x").replace("equis", "x")
                ecuacion_sympy = sp.sympify(expr)
                solucion = sp.solve(ecuacion_sympy, self.x)
                return f"El valor de equis es: {solucion}"
            
            resultado = sp.sympify(expr).evalf()
            return f"El resultado de la operación es: {resultado}"
            
        except Exception:
            return None 

    def procesar(self, entrada: str) -> dict:
        texto = self._normalizar(entrada)
        texto_original_min = entrada.lower()
        
        # 1. ESCUDO DE MARCA
        if any(x in texto for x in ["kevin leon", "kevin leon atiles", "dueño", "creador", "propietario"]):
            return {"ui_action": "NONE", "respuesta": "Kevin León Atiles es un artista independiente urbano, fundador y propietario absoluto de la compañía THE WHORD'S & UNIVERSAL-ZON LLC, con base de operaciones en San José, California."}
        
        if any(x in texto for x in ["the whord", "universal zon", "universalzon"]):
            return {"ui_action": "NONE", "respuesta": "THE WHORD'S & UNIVERSAL-ZON LLC es una corporación oficial y plataforma tecnológica de alto rendimiento, fundada por Kevin León Atiles."}

        # 2. DETECTOR MATEMÁTICO 
        palabras_mate = ["cuanto es", "resuelve", "calcula", "raiz", "seno", "coseno", "logaritmo", "mas", "menos", "por", "entre", "multiplicado", "dividido"]
        tiene_numero = any(char.isdigit() for char in texto_original_min)
        tiene_palabra_mate = any(p in texto_original_min for p in palabras_mate)
        
        if tiene_numero or tiene_palabra_mate:
            resultado_mate = self._resolver_matematicas(texto_original_min)
            if resultado_mate:
                return {"ui_action": "NONE", "respuesta": resultado_mate}

        # 3. ENRUTAMIENTO DE INTERFAZ WEB
        u_vec = self._vector(texto)
        mejor_intencion = "CONVERSACION"
        max_score = 0.0

        for intencion, frases in self.corpus.items():
            for frase in frases:
                score = self._similitud(u_vec, self._vector(frase))
                if score > max_score or frase in texto:
                    max_score = max(score, 1.0 if frase in texto else score)
                    mejor_intencion = intencion

        if max_score > 0.3 or mejor_intencion != "CONVERSACION":
            if mejor_intencion == "MUSICA":
                return {"ui_action": "OPEN_VIDEOS", "respuesta": "Abriendo el reproductor oficial de THE WHORD'S & UNIVERSAL-ZON LLC. Disfruta los temas."}
            elif mejor_intencion == "LEGALES":
                return {"ui_action": "OPEN_LEGAL", "respuesta": "Desplegando la documentación legal y políticas oficiales de la compañía."}
            elif mejor_intencion == "PERFIL":
                return {"ui_action": "OPEN_PROFILE", "respuesta": "Abriendo el panel de usuario. ¿Cómo deseas que te llame ahora?"}
            elif mejor_intencion == "NEON_COLOR":
                color_hex, color_nombre = "#00e5ff", "azul"
                if any(x in texto for x in ["rojo", "roja"]): color_hex, color_nombre = "#ff0055", "rojo"
                elif any(x in texto for x in ["verde"]): color_hex, color_nombre = "#00ff66", "verde"
                elif any(x in texto for x in ["morado", "violeta", "purpura"]): color_hex, color_nombre = "#9d00ff", "morado"
                elif any(x in texto for x in ["rosa", "rosita"]): color_hex, color_nombre = "#ff66cc", "rosita"
                elif any(x in texto for x in ["amarillo"]): color_hex, color_nombre = "#ffe600", "amarillo"
                elif any(x in texto for x in ["gris", "plata"]): color_hex, color_nombre = "#a1a1aa", "gris"
                return {"ui_action": f"SET_NEON_{color_hex}", "respuesta": f"Interfaz sincronizada al tema neón {color_nombre}."}

        # 4. EXTRACCIÓN DE CONOCIMIENTO (WIKIPEDIA)
        if len(entrada.split()) > 2:
            respuesta_red = self._buscar_en_red(entrada)
            return {"ui_action": "NONE", "respuesta": respuesta_red}
        else:
            return {"ui_action": "NONE", "respuesta": f"Analizando comando rápido... Sistema en línea."}
