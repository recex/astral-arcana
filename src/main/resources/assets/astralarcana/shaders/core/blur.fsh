#version 150

uniform sampler2D Sampler0;
uniform vec2 Dir;      // passo de um texel na direcao do blur

in vec2 texCoord0;
out vec4 fragColor;

void main() {
    float w0 = 0.227027;
    float w1 = 0.1945946;
    float w2 = 0.1216216;
    float w3 = 0.054054;
    float w4 = 0.016216;
    vec3 acc = texture(Sampler0, texCoord0).rgb * w0;
    acc += (texture(Sampler0, texCoord0 + Dir * 1.0).rgb + texture(Sampler0, texCoord0 - Dir * 1.0).rgb) * w1;
    acc += (texture(Sampler0, texCoord0 + Dir * 2.0).rgb + texture(Sampler0, texCoord0 - Dir * 2.0).rgb) * w2;
    acc += (texture(Sampler0, texCoord0 + Dir * 3.0).rgb + texture(Sampler0, texCoord0 - Dir * 3.0).rgb) * w3;
    acc += (texture(Sampler0, texCoord0 + Dir * 4.0).rgb + texture(Sampler0, texCoord0 - Dir * 4.0).rgb) * w4;
    fragColor = vec4(acc, 1.0);
}
