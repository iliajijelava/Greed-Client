#version 150

uniform sampler2D Sampler0;
uniform vec2 texelSize;
uniform float width;
uniform vec3 color;
uniform float alpha;

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    vec2 uv = TexCoord;
    float center = texture(Sampler0, uv).a;
    if (center > 0.01) discard;

    float maxA = 0.0;
    for (int x = -2; x <= 2; x++) {
        for (int y = -2; y <= 2; y++) {
            if (x == 0 && y == 0) continue;
            vec2 d = vec2(float(x), float(y)) * texelSize * width;
            maxA = max(maxA, texture(Sampler0, uv + d).a);
        }
    }

    if (maxA < 0.01) discard;
    OutColor = vec4(color, maxA * alpha);
}
