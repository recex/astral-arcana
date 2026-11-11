#version 150

uniform sampler2D Sampler0;
uniform vec4 Params;   // x = limiar, y = suavizacao

in vec2 texCoord0;
out vec4 fragColor;

void main() {
    vec3 c = texture(Sampler0, texCoord0).rgb;
    float br = max(c.r, max(c.g, c.b));
    float k = smoothstep(Params.x - Params.y, Params.x + Params.y, br);
    fragColor = vec4(c * k, 1.0);
}
