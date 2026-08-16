#version 150

// Final composite: scene after hands + smoke/fire around the silhouette (port of hand_fire).
// Drawn over the frame with replace blending.
//   Sampler0 — scene after hands
//   Sampler1 — blurred trail (smoke)
//   Sampler2 — hand mask (.r)

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform vec2 texSize;
uniform float time;
uniform float intensity;
uniform float handSoftness;
uniform float handBlur;
uniform float smoke;
uniform float activity;
uniform vec4 glowColor;

in vec2 TexCoord;
out vec4 OutColor;

vec2 texelSize = vec2(0.0);

float sampleMask(vec2 uv) {
    return texture(Sampler2, clamp(uv, vec2(0.0), vec2(1.0))).r;
}

float edgeMask(vec2 uv) {
    float c = sampleMask(uv);
    float e = 0.0;
    e += abs(c - sampleMask(uv + vec2( texelSize.x, 0.0)));
    e += abs(c - sampleMask(uv + vec2(-texelSize.x, 0.0)));
    e += abs(c - sampleMask(uv + vec2(0.0,  texelSize.y)));
    e += abs(c - sampleMask(uv + vec2(0.0, -texelSize.y)));
    return clamp(e * 0.9, 0.0, 1.0);
}

void main() {
    vec2 texCoord = TexCoord;
    texelSize = 1.0 / max(texSize, vec2(1.0));

    float topDistance = 1.0 - texCoord.y;
    float topEdgeFade = smoothstep(0.012, 0.085, topDistance);
    vec4 scene = texture(Sampler0, texCoord);
    vec4 smokeTex = texture(Sampler1, texCoord);

    float intensityC = clamp(intensity, 0.0, 1.5);
    float softness = clamp(handSoftness, 0.4, 2.5);
    float blurSetting = clamp(handBlur, 0.2, 3.0);
    float smokeSetting = clamp(smoke, 0.0, 0.8);
    float mask = sampleMask(texCoord);
    float edge = edgeMask(texCoord);

    float activityC = clamp(activity, 0.0, 1.0);
    float trail = clamp(smokeTex.a, 0.0, 1.0);
    float softMix = clamp((softness - 0.4) / 2.1, 0.0, 1.0);
    float blurMix = clamp((blurSetting - 0.2) / 2.8, 0.0, 1.0);
    float bloom = pow(trail, mix(0.78, 0.48, softMix));
    float aura = smoothstep(0.012, mix(0.24, 0.38, blurMix), trail);
    float smokeAlpha = clamp(max(bloom * 0.62, aura * 0.38), 0.0, 0.76);
    smokeAlpha *= (0.84 + intensityC * 0.46 + smokeSetting * 0.35 + activityC * 0.12 + softMix * 0.14);
    smokeAlpha = clamp(smokeAlpha, 0.0, 0.86);
    smokeAlpha = smokeAlpha < 0.005 ? 0.0 : smokeAlpha;
    smokeAlpha *= 1.0 - mask * 0.18;
    smokeAlpha *= topEdgeFade;

    // The composite runs before the HUD (HEAD), but still writes over the frame with replace.
    // Discard pixels without smoke so the framebuffer stays untouched there (less overdraw).
    if (smokeAlpha < 0.004) discard;

    vec3 smokeColor = clamp(smokeTex.rgb * (1.18 + smokeSetting * 0.24), 0.0, 1.0);
    vec3 outColor = scene.rgb;
    outColor += smokeColor * smokeAlpha;
    outColor += smokeColor * edge * bloom * (0.11 + intensityC * 0.10) * (1.0 - softMix * 0.28) * topEdgeFade;

    OutColor = vec4(clamp(outColor, 0.0, 1.0), scene.a);
}
