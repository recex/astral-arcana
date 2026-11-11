#version 150

uniform sampler2D Sampler0;
uniform vec4 Params;   // z = intensidade

in vec2 texCoord0;
out vec4 fragColor;

void main() {
    fragColor = vec4(texture(Sampler0, texCoord0).rgb * Params.z, 1.0);
}
