#version 150

uniform sampler2D Sampler0; // hand (mask + shaded scene)
uniform sampler2D Sampler1; // reflected world texture
uniform vec4 multiplier;    // reflection multiplier
uniform float mixFactor;    // hand/reflection blend (0..1)

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    vec4 hand = texture(Sampler0, TexCoord);
    if (hand.a < 0.01) discard;

    vec3 reflection = texture(Sampler1, TexCoord).rgb;
    vec3 mirror = reflection * multiplier.rgb;
    vec3 col = mix(mirror, hand.rgb, clamp(mixFactor, 0.0, 1.0));

    OutColor = vec4(col, hand.a * multiplier.a);
}
