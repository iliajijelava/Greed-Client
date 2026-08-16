#version 150

uniform sampler2D Sampler0;
uniform vec3 color;
uniform float alpha;

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    vec4 tex = texture(Sampler0, TexCoord);
    float v = max(max(tex.r, tex.g), max(tex.b, tex.a));
    if (v <= 0.01) discard;

    vec2 ts = 1.0 / vec2(textureSize(Sampler0, 0));

    float minN = 1.0;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            if (x == 0 && y == 0) continue;
            vec4 s = texture(Sampler0, TexCoord + vec2(x, y) * ts);
            float sv = max(max(s.r, s.g), max(s.b, s.a));
            minN = min(minN, sv);
        }
    }

    if (minN > 0.01) discard;

    OutColor = vec4(color, alpha);
}
