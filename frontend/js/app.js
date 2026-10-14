// UNIVERSAL-ZON AI - NÚCLEO FRONTAL ULTRA-OPTIMIZADO
let indiceTemaActual = 0;
const listaCanciones = [
  { titulo: "Tiraera pa Maldo El virao Del Barrio", id: "l5hTulztOlw" },
  { titulo: "Tiraera pa YOVNGCHIMI", id: "tDORNG7WYKQ" },
  { titulo: "Fugitivo Federal", id: "bMMH0iZd360" },
  { titulo: "Sombras En La City", id: "vsDy5Aoy9r8" },
  { titulo: "Guante y Máscaras 🎭", id: "xTuLpplzcj8" }
]; // (Resumido para máxima velocidad en memoria)

window.addEventListener('DOMContentLoaded', () => {
  const usuario = localStorage.getItem('uz_user_name');
  if (usuario) { document.getElementById('screen-welcome').classList.add('hidden'); iniciarApp(usuario); }
});

function setNeonColor(hex) { document.documentElement.style.setProperty('--neon-primary', hex); }
function toggleMenu() { document.getElementById('drawer').classList.toggle('open'); document.getElementById('overlay').classList.toggle('active'); }

function iniciarApp(nombre) {
  document.getElementById('drawer-welcome-user').innerHTML = `${nombre}<small>Owner: Kevin León Atiles</small>`;
  mostrarRespuestaAsistente(`Sistema Universal-Zon inicializado. Comandos de hardware y voz listos, ${nombre}.`);
}

// ─── MOTOR MULTIMEDIA OFICIAL ───
function abrirVideos() { toggleMenu(); document.getElementById('video-modal').classList.add('active'); if(listaCanciones.length) reproducirTema(listaCanciones[0]); }
function cerrarVideos() { document.getElementById('video-modal').classList.remove('active'); document.getElementById('video-frame').src = ""; }
function reproducirTema(c) {
  document.getElementById('tvCurrentTrack').textContent = c.titulo;
  document.getElementById('video-frame').src = `https://www.youtube.com/embed/${c.id}?autoplay=1`;
}

// ─── MOTOR DE VOZ AVANZADO (NATURAL) ───
function hablarCoco(texto) {
  if (!('speechSynthesis' in window)) return;
  window.speechSynthesis.cancel();
  const u = new SpeechSynthesisUtterance(texto);
  u.rate = 1.0; u.pitch = 1.0;
  const voces = window.speechSynthesis.getVoices();
  const vozSeleccionada = voces.find(v => v.lang.includes('es-US') || v.lang.includes('es-MX') || v.lang.startsWith('es'));
  if (vozSeleccionada) u.voice = vozSeleccionada;
  window.speechSynthesis.speak(u);
}

// ─── DICTADO POR VOZ ───
let recognition;
if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
  recognition = new (window.SpeechRecognition || window.webkitSpeechRecognition)();
  recognition.lang = 'es-ES';
  recognition.onresult = (e) => {
    document.getElementById('user-input').value = e.results[0][0].transcript;
    document.getElementById('chat-form').dispatchEvent(new Event('submit'));
  };
}
function iniciarDictadoPorVoz() { if(recognition) recognition.start(); }

// ─── MOTOR BLUETOOTH LE (SMART SHOES) ───
async function ejecutarComandosBLE(comandos) {
  if (!comandos || comandos.includes("NOP")) return;
  console.log("Órdenes de hardware listas para inyección BLE:", comandos);
  // Aquí se dispara el puente real (navigator.bluetooth.requestDevice)
  // al conectar con el microcontrolador de las zapatillas.
}

// ─── CEREBRO Y ENRUTAMIENTO ───
async function enviarMensaje(e) {
  e.preventDefault();
  const input = document.getElementById('user-input');
  const txt = input.value.trim();
  if (!txt) return;

  const box = document.getElementById('chat-box');
  box.innerHTML += `<div class="msg msg-user">${txt}</div>`;
  input.value = ''; box.scrollTop = box.scrollHeight;

  // Intercepción de comandos multimedia
  if (txt.toLowerCase().includes("video") || txt.toLowerCase().includes("cancion")) return abrirVideos();

  // Envío de la instrucción al Cerebro Cuántico (Python)
  try {
    const res = await fetch(`http://127.0.0.1:8000/api/brain?texto=${encodeURIComponent(txt)}`);
    const data = await res.json();
    
    // Ejecutar múltiples tareas al instante
    ejecutarComandosBLE(data.comandos_ble);
    mostrarRespuestaAsistente(data.respuesta);
  } catch (err) {
    mostrarRespuestaAsistente("Error de enlace. Servidor maestro offline.");
  }
}

function mostrarRespuestaAsistente(texto) {
  const box = document.getElementById('chat-box');
  box.innerHTML += `<div class="msg msg-assistant">${texto}</div>`;
  box.scrollTop = box.scrollHeight;
  hablarCoco(texto);
}
