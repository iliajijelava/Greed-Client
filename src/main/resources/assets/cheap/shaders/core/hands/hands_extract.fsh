#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    vec4 scene = texture(Sampler0, TexCoord);
    float mask = texture(Sampler1, TexCoord).a;
    if (mask < 0.01) discard;
    OutColor = vec4(scene.rgb, mask);
}
