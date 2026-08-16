#version 150

#moj_import <mre:fragment.glsl>

in vec2 FragCoord; // normalized fragment coord relative to the primitive
in vec4 FragColor;

uniform vec2 Size; // rectangle size
uniform vec4 Radius; // radius for each vertex
uniform float Thickness; // border thickness in gui pixels
uniform vec2 Smoothness; // anti-aliasing scale (kept so the uniform stays active)

out vec4 OutColor;

void main() {
    vec2 center = Size * 0.5;

    // outer filled shape
    float distOuter = rdist(center - (FragCoord.xy * Size), center - 1.0, Radius);
    float aa = max(fwidth(distOuter), 0.001) * max(max(Smoothness.x, Smoothness.y), 0.5);
    float outerAlpha = 1.0 - smoothstep(-aa, aa, distOuter);

    // inner shape punched out, inset by Thickness
    vec4 innerRadius = max(Radius - vec4(Thickness), 0.0);
    float distInner = rdist(center - (FragCoord.xy * Size), center - 1.0 - Thickness, innerRadius);
    float innerAlpha = 1.0 - smoothstep(-aa, aa, distInner);

    float alpha = outerAlpha * (1.0 - innerAlpha);

    vec4 color = vec4(FragColor.rgb, FragColor.a * alpha);

    if (color.a == 0.0) { // alpha test
        discard;
    }

    OutColor = color;
}
