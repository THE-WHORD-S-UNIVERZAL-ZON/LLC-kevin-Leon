import re
import math
import unicodedata
import urllib.request
import urllib.parse
import json
import subprocess
from collections import Counter

try:
    import sympy as sp
except ImportError:
    sp = None

class WebPlatformBrain:
    def __init__(self):
        self.corpus = {
            "MUSICA": ["musica", "videos", "reproducir", "cancion", "tiraera", "escuchar", "youtube", "clip", "tema", "oficial"],
            "NEON_COLOR": ["color", "luces", "tema", "iluminacion", "ponlo", "cambia a", "pantalla", "brillo", "neon"],
            "LEGALES": ["normas", "politicas", "terminos", "privacidad", "reglas", "legal", "contrato", "empresa"],
            "PERFIL": ["cambiar nombre", "mi perfil", "alias", "quien soy", "usuario"]
        }
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
            return "La búsqueda está en blanco. No encontré información en la red."
        except Exception:
            return "Problema de red al buscar la respuesta."

    def _resolver_matematicas(self, entrada: str) -> str:
        if not sp: return None
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

    # SISTEMA NERVIOSO DE HARDWARE Y NOTIFICACIONES
    def _control_hardware(self, texto: str) -> str:
        try:
            # 1. Leer Notificaciones
            if "notificacion" in texto or "notificaciones" in texto or "mensajes" in texto:
                resultado = subprocess.run(['termux-notification-list'], capture_output=True, text=True)
                datos = json.loads(resultado.stdout)
                
                if not datos:
                    return "Tu bandeja está limpia. No hay notificaciones nuevas."
                
                mensajes_reales = []
                # Filtrar solo notificaciones útiles (ignorar las del sistema)
                for notif in datos:
                    app = notif.get("packageName", "").lower()
                    titulo = notif.get("title", "")
                    contenido = notif.get("content", "")
                    
                    if "android" not in app and "termux" not in app and titulo:
                        mensajes_reales.append(f"De {titulo}: {contenido}")
                
                if mensajes_reales:
                    resumen = ". ".join(mensajes_reales[:3]) # Solo lee las primeras 3 para no aburrir
                    return f"Tienes {len(mensajes_reales)} alertas recientes. Escucha: {resumen}"
                else:
                    return "Solo tienes notificaciones silenciosas del sistema."

            # 2. Batería
            elif "bateria" in texto or "batería" in texto or "carga" in texto:
                resultado = subprocess.run(['termux-battery-status'], capture_output=True, text=True)
                datos = json.loads(resultado.stdout)
                porcentaje = datos.get("percentage", "desconocido")
                estado = datos.get("status", "desconocido").replace("DISCHARGING", "descargándose").replace("CHARGING", "cargándose")
                return f"Nivel de batería físico al {porcentaje} por ciento. El dispositivo está {estado}."
            
            # 3. Linterna
            elif "enciende la linterna" in texto or "luz" in texto:
                subprocess.run(['termux-torch', 'on'])
                return "Linterna encendida, Kevin."
                
            elif "apaga la linterna" in texto:
                subprocess.run(['termux-torch', 'off'])
                return "Linterna apagada."
                
        except Exception as e:
            return "Error de hardware. Verifica que diste los permisos a Termux API."
            
        return None

    def procesar(self, entrada: str) -> dict:
        texto = self._normalizar(entrada)
        texto_original_min = entrada.lower()
        
        # 1. ESCUDO DE MARCA
        if any(x in texto for x in ["kevin leon", "kevin leon atiles", "dueño", "creador"]):
            return {"ui_action": "NONE", "respuesta": "Kevin León Atiles es el fundador y propietario de THE WHORD'S & UNIVERSAL-ZON LLC."}
        
        # 2. CONTROL DE HARDWARE Y NOTIFICACIONES
        palabras_hardware = ["bateria", "batería", "carga", "linterna", "luz", "vibra", "notificacion", "notificaciones", "mensajes"]
        if any(p in texto for p in palabras_hardware):
            resp_hardware = self._control_hardware(texto_original_min)
            if resp_hardware:
                return {"ui_action": "NONE", "respuesta": resp_hardware}

        # 3. DETECTOR MATEMÁTICO 
        palabras_mate = ["cuanto es", "resuelve", "calcula", "raiz", "seno", "coseno", "logaritmo", "mas", "menos", "por", "entre"]
        if any(char.isdigit() for char in texto_original_min) or any(p in texto_original_min for p in palabras_mate):
            resultado_mate = self._resolver_matematicas(texto_original_min)
            if resultado_mate:
                return {"ui_action": "NONE", "respuesta": resultado_mate}

        # 4. ENRUTAMIENTO DE INTERFAZ WEB
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
            if mejor_intencion == "MUSICA": return {"ui_action": "OPEN_VIDEOS", "respuesta": "Abriendo música."}
            elif mejor_intencion == "LEGALES": return {"ui_action": "OPEN_LEGAL", "respuesta": "Abriendo legales."}
            elif mejor_intencion == "PERFIL": return {"ui_action": "OPEN_PROFILE", "respuesta": "Abriendo perfil."}
            elif mejor_intencion == "NEON_COLOR": return {"ui_action": "SET_NEON_#00e5ff", "respuesta": "Luces neón listas."}

        # 5. WIKIPEDIA
        if len(entrada.split()) > 2:
            return {"ui_action": "NONE", "respuesta": self._buscar_en_red(entrada)}
        else:
            return {"ui_action": "NONE", "respuesta": f"Sistema en línea."}
