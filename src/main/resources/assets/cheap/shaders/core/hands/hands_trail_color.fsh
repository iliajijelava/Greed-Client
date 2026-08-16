#version 150

uniform sampler2D Sampler0;
uniform vec3 color;
uniform vec3 color2;
uniform float intensity;
uniform float emission;

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    float cov = texture(Sampler0, TexCoord).a;
    float a = clamp(cov * intensity, 0.0, 1.0) * emission;
    if (a <= 0.001) discard;
    vec3 grad = mix(color, color2, TexCoord.y);
    OutColor = vec4(grad * a, a);
}
