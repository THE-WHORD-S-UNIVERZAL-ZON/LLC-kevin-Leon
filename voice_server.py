from aiohttp import web
import edge_tts

async def handle_tts(request):
    if request.method == "POST":
        data = await request.json()
        texto = data.get("texto", "")
        voz = data.get("voz", "es-PR-VictorNeural")
    else:
        texto = request.query.get("texto", "")
        voz = request.query.get("voz", "es-PR-VictorNeural")

    if not texto:
        return web.Response(status=400, text="Texto requerido")

    comunicador = edge_tts.Communicate(texto, voz)
    audio_stream = b""
    async for chunk in comunicador.stream():
        if chunk["type"] == "audio":
            audio_stream += chunk["data"]

    return web.Response(
        body=audio_stream,
        content_type="audio/mpeg",
        headers={
            "Access-Control-Allow-Origin": "*",
            "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
            "Access-Control-Allow-Headers": "Content-Type"
        }
    )

async def handle_options(request):
    return web.Response(
        headers={
            "Access-Control-Allow-Origin": "*",
            "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
            "Access-Control-Allow-Headers": "Content-Type"
        }
    )

app = web.Application()
app.router.add_get("/api/tts", handle_tts)
app.router.add_post("/api/tts", handle_tts)
app.router.add_options("/api/tts", handle_options)

if __name__ == "__main__":
    web.run_app(app, host="0.0.0.0", port=8000)
