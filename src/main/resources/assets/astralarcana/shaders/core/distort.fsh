#version 150

uniform sampler2D Sampler0;
uniform vec2 ScreenSize;
uniform vec4 Lens;      // xy = centro (uv), z = forca, w = raio de influencia
uniform float Horizon;  // raio do horizonte de eventos
uniform vec4 Wave;      // xy = centro, z = raio atual, w = largura
uniform float WaveStr;  // forca da onda de choque
uniform vec3 Blur;      // xy = centro do blur radial, z = quantidade
uniform float Chroma;   // aberracao cromatica
uniform vec4 Swirl;     // xy = centro, z = torcao (radianos), w = raio (tornado)
uniform vec4 Grade;     // x = dessaturar, y = inverter, z = vinheta, w = glitch
uniform vec4 Tint;      // rgb = cor, a = quantidade
uniform float Clock;    // tempo em segundos

in vec2 texCoord0;
out vec4 fragColor;

float hash(float n) {
    return fract(sin(n * 12.9898) * 43758.5453);
}

vec3 fetch(vec2 uv, vec2 ca) {
    return vec3(texture(Sampler0, uv + ca).r, texture(Sampler0, uv).g, texture(Sampler0, uv - ca).b);
}

void main() {
    vec2 asp = vec2(ScreenSize.x / ScreenSize.y, 1.0);
    vec2 uv = texCoord0;
    vec2 off = vec2(0.0);
    float chroma = Chroma;
    float dark = 1.0;
    vec3 glowAdd = vec3(0.0);

    // lente gravitacional (buraco negro)
    if (Lens.z > 0.0) {
        vec2 p = (uv - Lens.xy) * asp;
        float d = length(p);
        float R = Lens.w;
        float k = min(Lens.z * R * R / (d * d + R * R * 0.25), 0.93);
        off += (-p * k) / asp;
        chroma += k * 0.015;
        dark = smoothstep(Horizon * 0.95, Horizon * 1.2, d);
        float x = (d - Horizon * 1.35) / (Horizon * 0.14 + 0.0001);
        float ring = exp(-x * x);
        glowAdd += vec3(1.0, 0.72, 0.38) * ring * 0.55 * min(Lens.z * 2.0, 1.0);
    }

    // onda de choque
    if (WaveStr > 0.0) {
        vec2 p = (uv - Wave.xy) * asp;
        float d = length(p);
        float x = d - Wave.z;
        float m = exp(-(x * x) / (Wave.w * Wave.w));
        vec2 dir = p / max(d, 0.0001);
        off -= (dir / asp) * (m * WaveStr * sin(x / Wave.w * 2.0));
        chroma += m * WaveStr * 0.25;
    }

    // redemoinho (tornado)
    if (Swirl.z != 0.0) {
        vec2 p = (uv - Swirl.xy) * asp;
        float d = length(p);
        if (d < Swirl.w) {
            float f = 1.0 - d / Swirl.w;
            float ang = Swirl.z * f * f;
            float s = sin(ang);
            float c = cos(ang);
            vec2 q = vec2(c * p.x - s * p.y, s * p.x + c * p.y);
            off += (q - p) / asp;
            chroma += f * 0.006;
        }
    }

    // glitch (faixas horizontais deslocadas)
    if (Grade.w > 0.0) {
        float row = floor(uv.y * 48.0);
        float tt = floor(Clock * 14.0);
        float n = hash(row + tt * 17.0);
        if (n > 1.0 - Grade.w * 0.35) {
            off.x += (hash(row * 3.1 + tt) - 0.5) * 0.09 * Grade.w;
            chroma += 0.012 * Grade.w;
        }
    }

    vec2 suv = uv + off;
    vec3 col;
    if (Blur.z > 0.001) {
        vec2 toC = Blur.xy - suv;
        vec3 acc = vec3(0.0);
        for (int i = 0; i < 10; i++) {
            float t = float(i) / 10.0;
            acc += fetch(suv + toC * (t * Blur.z), (suv - 0.5) * chroma);
        }
        col = acc / 10.0;
    } else {
        col = fetch(suv, (suv - 0.5) * chroma);
    }

    col = col * dark + glowAdd;

    // color grading
    float lum = dot(col, vec3(0.299, 0.587, 0.114));
    col = mix(col, vec3(lum), Grade.x);
    col = mix(col, vec3(1.0) - col, Grade.y);
    float vig = smoothstep(0.35, 0.95, length((uv - 0.5) * asp));
    col *= 1.0 - Grade.z * vig;
    col = mix(col, col * Tint.rgb * 1.6, Tint.a);

    fragColor = vec4(col, 1.0);
}
