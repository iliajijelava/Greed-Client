#version 150

uniform sampler2D Sampler0;
uniform vec3 fillColor;
uniform float fillAlpha;
uniform float shadingStrength;
uniform float keepShading;
uniform float rainbow;
uniform float rainbowTime;
uniform float rainbowSpeed;
uniform float rainbowScale;
uniform float screenH;

in vec2 TexCoord;
out vec4 OutColor;

vec3 hue2rgb(float h) {
    vec3 p = abs(fract(vec3(h) + vec3(0.0, 2.0 / 3.0, 1.0 / 3.0)) * 6.0 - 3.0);
    return clamp(p - 1.0, 0.0, 1.0);
}

void main() {
    vec4 s = texture(Sampler0, TexCoord);
    if (s.a < 0.01) discard;

    vec3 f;
    if (rainbow > 0.5) {
        float yN = gl_FragCoord.y / max(screenH, 1.0);
        float h = fract(yN * rainbowScale + rainbowTime * rainbowSpeed);
        f = hue2rgb(h);
    } else {
        f = fillColor;
    }

    if (keepShading > 0.5) {
        float lum = dot(s.rgb, vec3(0.299, 0.587, 0.114));
        f *= mix(1.0, lum, shadingStrength);
    }

    OutColor = vec4(mix(s.rgb, f, fillAlpha), s.a);
}
