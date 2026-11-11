#version 150

uniform sampler2D Sampler0;   // cor da cena (copia)
uniform sampler2D Sampler1;   // profundidade da cena (copia)

uniform mat4 InvPV;           // inversa de (projecao * rotacao da camera): reconstroi posicao relativa a camera
uniform mat4 PrevPV;          // projecao * rotacao do frame anterior
uniform vec3 CamDelta;        // movimento da camera desde o frame anterior
uniform vec4 Info;            // x = Y absoluto da camera, y = motion blur, z = passos do raymarch

uniform mat4 LightPosA;       // 4 luzes: coluna i = (xyz relativo a camera, raio)
uniform mat4 LightColA;       //          coluna i = (rgb, intensidade)
uniform mat4 LightPosB;
uniform mat4 LightColB;
uniform mat4 OrbPos;          // 4 orbes volumetricos: (xyz, raio)
uniform mat4 OrbCol;          //                       (rgb, densidade)
uniform mat4 OrbPar;          //                       (tempo, turbulencia, tipo 0=fogo/nebulosa 1=vazio, intensidade)
uniform vec4 ShockP0;         // ondas 3D: (xyz, raio)
uniform vec4 ShockC0;         //           (rgb, forca)
uniform vec4 ShockP1;
uniform vec4 ShockC1;
uniform vec2 ShockW;          // largura de cada onda
uniform vec4 Atmos;           // neblina: rgb, densidade
uniform vec4 AtmosB;          // x = queda com a altura, y = altura base, z = maximo

uniform vec2 ScreenSize;
uniform vec4 Lens;
uniform float Horizon;
uniform vec4 Wave;
uniform float WaveStr;
uniform vec3 Blur;
uniform float Chroma;
uniform vec4 Swirl;
uniform vec4 Grade;
uniform vec4 Tint;
uniform float Clock;

in vec2 texCoord0;
out vec4 fragColor;

float hash(float n) {
    return fract(sin(n * 12.9898) * 43758.5453);
}

float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.zyx + 31.32);
    return fract((p.x + p.y) * p.z);
}

float noise3(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    float a = mix(hash31(i), hash31(i + vec3(1.0, 0.0, 0.0)), f.x);
    float b = mix(hash31(i + vec3(0.0, 1.0, 0.0)), hash31(i + vec3(1.0, 1.0, 0.0)), f.x);
    float c = mix(hash31(i + vec3(0.0, 0.0, 1.0)), hash31(i + vec3(1.0, 0.0, 1.0)), f.x);
    float d = mix(hash31(i + vec3(0.0, 1.0, 1.0)), hash31(i + vec3(1.0, 1.0, 1.0)), f.x);
    return mix(mix(a, b, f.y), mix(c, d, f.y), f.z);
}

float fbm(vec3 p) {
    float a = 0.5;
    float s = 0.0;
    for (int i = 0; i < 3; i++) {
        s += a * noise3(p);
        p = p * 2.03 + 17.0;
        a *= 0.5;
    }
    return s;
}

vec3 reconstruct(vec2 uv, float depth) {
    vec4 p = InvPV * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

vec3 fetch(vec2 uv, vec2 ca) {
    return vec3(texture(Sampler0, uv + ca).r, texture(Sampler0, uv).g, texture(Sampler0, uv - ca).b);
}

void applyLight(vec4 lp, vec4 lc, vec3 posRel, vec3 rd, float dist, bool sky, inout vec3 relight, inout vec3 halo) {
    if (lp.w <= 0.0) return;
    if (!sky) {
        float d = length(lp.xyz - posRel);
        float att = clamp(1.0 - d / lp.w, 0.0, 1.0);
        att *= att;
        relight += lc.rgb * (att * lc.a);
    }
    float tc = dot(lp.xyz, rd);
    if (tc > 0.0 && (sky || tc < dist)) {
        float perp2 = dot(lp.xyz, lp.xyz) - tc * tc;
        float r = lp.w * 0.12;
        halo += lc.rgb * (lc.a * 0.6 * exp(-perp2 / (r * r)));
    }
}

void applyShock(vec4 sp, vec4 sc, float w, vec3 posRel, bool sky, inout vec3 add) {
    if (sp.w <= 0.0 || sc.a <= 0.0 || sky) return;
    float x = (length(posRel - sp.xyz) - sp.w) / w;
    add += sc.rgb * (exp(-x * x) * sc.a);
}

void marchOrb(vec4 op, vec4 oc, vec4 opar, vec3 rd, float sceneT, inout vec3 col) {
    if (op.w <= 0.0) return;
    vec3 c = op.xyz;
    float R = op.w;
    float b = dot(c, rd);
    float disc = b * b - (dot(c, c) - R * R);
    if (disc <= 0.0) return;
    float sq = sqrt(disc);
    float t0 = max(b - sq, 0.0);
    float t1 = min(b + sq, sceneT);
    if (t1 <= t0) return;
    int steps = int(Info.z);
    float dt = (t1 - t0) / float(steps);
    float k = dt / R;
    vec3 acc = vec3(0.0);
    float trans = 1.0;
    float time = opar.x;
    for (int i = 0; i < 32; i++) {
        if (i >= steps) break;
        float t = t0 + (float(i) + 0.5) * dt;
        vec3 q = (rd * t - c) / R;
        float r2 = dot(q, q);
        if (r2 > 1.0) continue;
        float rr = sqrt(r2);
        float fall = 1.0 - rr;
        float n = fbm(q * 3.0 * opar.y + vec3(0.0, -time * 0.6, time * 0.3));
        float dens = oc.a * max(n * 1.6 - 0.35, 0.0) * fall * fall * 3.0;
        if (opar.z < 0.5) {
            float h = hash31(floor(q * 14.0 + vec3(0.0, time, 0.0)));
            float spark = step(0.992, h) * (0.5 + 0.5 * sin(time * 8.0 + h * 60.0));
            vec3 emit = mix(oc.rgb, vec3(1.0, 0.95, 0.8), fall * fall) * (0.6 + n);
            acc += trans * (emit * dens + oc.rgb * spark * 2.0) * k * 6.0 * opar.w;
            trans *= exp(-dens * k * 5.0);
        } else {
            float rim = smoothstep(0.55, 0.9, rr) * (1.0 - smoothstep(0.92, 1.0, rr));
            acc += trans * oc.rgb * rim * n * k * 6.0 * opar.w;
            trans *= exp(-dens * k * 12.0);
        }
    }
    col = col * trans + acc;
}

void main() {
    vec2 asp = vec2(ScreenSize.x / ScreenSize.y, 1.0);
    vec2 uv = texCoord0;
    vec2 off = vec2(0.0);
    float chroma = Chroma;
    float dark = 1.0;
    vec3 glowAdd = vec3(0.0);

    // ---- distorcao em espaco de tela ----
    if (Lens.z > 0.0) {
        vec2 p = (uv - Lens.xy) * asp;
        float d = length(p);
        float R = Lens.w;
        float k = min(Lens.z * R * R / (d * d + R * R * 0.25), 0.93);
        off += (-p * k) / asp;
        chroma += k * 0.015;
        dark = smoothstep(Horizon * 0.95, Horizon * 1.2, d);
        float x = (d - Horizon * 1.35) / (Horizon * 0.14 + 0.0001);
        glowAdd += vec3(1.0, 0.72, 0.38) * exp(-x * x) * 0.55 * min(Lens.z * 2.0, 1.0);
    }
    if (WaveStr > 0.0) {
        vec2 p = (uv - Wave.xy) * asp;
        float d = length(p);
        float x = d - Wave.z;
        float m = exp(-(x * x) / (Wave.w * Wave.w));
        vec2 dir = p / max(d, 0.0001);
        off -= (dir / asp) * (m * WaveStr * sin(x / Wave.w * 2.0));
        chroma += m * WaveStr * 0.25;
    }
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
    if (Grade.w > 0.0) {
        float row = floor(uv.y * 48.0);
        float tt = floor(Clock * 14.0);
        if (hash(row + tt * 17.0) > 1.0 - Grade.w * 0.35) {
            off.x += (hash(row * 3.1 + tt) - 0.5) * 0.09 * Grade.w;
            chroma += 0.012 * Grade.w;
        }
    }

    vec2 suv = clamp(uv + off, vec2(0.001), vec2(0.999));

    // ---- profundidade / posicao no espaco ----
    float depth = texture(Sampler1, suv).r;
    bool sky = depth >= 0.99999;
    vec3 posRel = reconstruct(suv, depth);
    float dist = length(posRel);
    vec3 rd = normalize(reconstruct(suv, 1.0));
    float sceneT = sky ? 100000.0 : dist;

    // ---- cor da cena: motion blur (reprojecao) > blur radial > simples ----
    vec3 col;
    vec2 ca = (suv - 0.5) * chroma;
    if (Info.y > 0.001) {
        vec4 pc = PrevPV * vec4(posRel + CamDelta, 1.0);
        vec2 vel = vec2(0.0);
        if (pc.w > 0.0) vel = clamp((suv - (pc.xy / pc.w * 0.5 + 0.5)) * Info.y, vec2(-0.03), vec2(0.03));
        vec3 acc = vec3(0.0);
        for (int i = 0; i < 8; i++) {
            float t = float(i) / 7.0 - 0.5;
            acc += fetch(suv + vel * t, ca);
        }
        col = acc / 8.0;
    } else if (Blur.z > 0.001) {
        vec2 toC = Blur.xy - suv;
        vec3 acc = vec3(0.0);
        for (int i = 0; i < 10; i++) {
            float t = float(i) / 10.0;
            acc += fetch(suv + toC * (t * Blur.z), ca);
        }
        col = acc / 10.0;
    } else {
        col = fetch(suv, ca);
    }
    col = col * dark + glowAdd;

    // ---- iluminacao dinamica (re-iluminacao por profundidade + halo volumetrico) ----
    vec3 relight = vec3(0.0);
    vec3 halo = vec3(0.0);
    for (int i = 0; i < 4; i++) {
        applyLight(LightPosA[i], LightColA[i], posRel, rd, dist, sky, relight, halo);
        applyLight(LightPosB[i], LightColB[i], posRel, rd, dist, sky, relight, halo);
    }
    col += col * relight * 1.5 + relight * 0.1 + halo;

    // ---- ondas de choque 3D que varrem a geometria ----
    vec3 shock = vec3(0.0);
    applyShock(ShockP0, ShockC0, ShockW.x, posRel, sky, shock);
    applyShock(ShockP1, ShockC1, ShockW.y, posRel, sky, shock);
    col += shock;

    // ---- orbes volumetricos (raymarching com oclusao pela profundidade) ----
    for (int i = 0; i < 4; i++) {
        marchOrb(OrbPos[i], OrbCol[i], OrbPar[i], rd, sceneT, col);
    }

    // ---- neblina atmosferica ----
    if (Atmos.w > 0.0) {
        float wy = Info.x + posRel.y;
        float hf = exp(-max(wy - AtmosB.y, 0.0) * AtmosB.x);
        float f = sky ? AtmosB.z * 0.6 : (1.0 - exp(-Atmos.w * dist)) * hf;
        col = mix(col, Atmos.rgb, min(f, AtmosB.z));
    }

    // ---- color grading ----
    float lum = dot(col, vec3(0.299, 0.587, 0.114));
    col = mix(col, vec3(lum), Grade.x);
    col = mix(col, vec3(1.0) - col, Grade.y);
    float vig = smoothstep(0.35, 0.95, length((uv - 0.5) * asp));
    col *= 1.0 - Grade.z * vig;
    col = mix(col, col * Tint.rgb * 1.6, Tint.a);

    fragColor = vec4(max(col, vec3(0.0)), 1.0);
}
