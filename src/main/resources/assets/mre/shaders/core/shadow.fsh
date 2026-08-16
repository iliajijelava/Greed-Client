#version 150

#moj_import <mre:fragment.glsl>

in vec2 FragCoord; // normalized fragment coord relative to the primitive
in vec4 FragColor;

uniform vec2 Size; // shadow quad size (rect + 2 * (softness + margin))
uniform vec2 RectSize; // inner rectangle size
uniform vec4 Radius; // rectangle corner radius
uniform vec2 Offset; // shadow offset relative to the rectangle
uniform float Softness; // shadow edge softness

out vec4 OutColor;

void main() {
    vec2 center = Size * 0.5;
    vec2 rectCenter = center + Offset;

    float dist = rdist(rectCenter - (FragCoord * Size), RectSize * 0.5 - 1.0, Radius);
    float shadow = 1.0 - smoothstep(0.0, Softness, dist);

    vec4 color = vec4(FragColor.rgb, FragColor.a * shadow);

    if (color.a == 0.0) { // alpha test
        discard;
    }

    OutColor = color;
}
