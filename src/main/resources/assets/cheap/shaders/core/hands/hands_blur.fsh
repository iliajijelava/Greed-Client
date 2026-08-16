#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform float strength;

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    vec4 sharp = texture(Sampler0, TexCoord);
    vec4 blurred = texture(Sampler1, TexCoord);
    vec3 col = blurred.a > 0.001 ? blurred.rgb / blurred.a : blurred.rgb;
    OutColor = mix(sharp, vec4(col, blurred.a), strength);
}
