import json
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
from quantum_brain import WebPlatformBrain

cerebro = WebPlatformBrain()

class UniversalZonBridge(BaseHTTPRequestHandler):
    def do_GET(self):
        parsed = urlparse(self.path)
        if parsed.path == "/api/brain":
            texto = parse_qs(parsed.query).get("texto", [""])[0]
            try:
                resultado = cerebro.procesar(texto)
                self.send_response(200)
                self.send_header("Content-Type", "application/json; charset=utf-8")
                self.send_header("Access-Control-Allow-Origin", "*")
                self.end_headers()
                self.wfile.write(json.dumps(resultado, ensure_ascii=False).encode("utf-8"))
            except Exception as e:
                self.send_response(500)
                self.end_headers()
        else:
            self.send_response(404)
            self.end_headers()

    # Operación completamente silenciosa (Stealth Mode)
    def log_message(self, format, *args):
        pass

def run_server(port=8000):
    server = ThreadingHTTPServer(('127.0.0.1', port), UniversalZonBridge)
    print(f"Servidor Web THE WHORD'S & UNIVERSAL-ZON LLC Operando (Puerto {port})...")
    server.serve_forever()

if __name__ == '__main__':
    run_server()
