#version 150

uniform sampler2D Sampler0;
uniform vec2 offset;
uniform vec2 texSize;
uniform float fade;
uniform float t;
uniform float dt;
uniform float turb;
uniform float flickAmp;

in vec2 TexCoord;
out vec4 OutColor;

float wob(float y, float tt) {
    return sin(y * 9.0 + tt * 4.3) * 0.6
         + sin(y * 17.0 - tt * 2.1) * 0.3
         + sin(y * 4.0 + tt * 1.3) * 0.4
         + sin(y * 28.0 + tt * 5.7) * 0.15;
}

void main() {
    vec2 uv = TexCoord;
    float dxTurb = (wob(uv.y, t) - wob(uv.y, t - dt)) * turb;
    float yFactor = 0.7 + uv.y * 0.8;
    vec2 disp = vec2(offset.x + dxTurb, offset.y * yFactor);

    vec2 px = 1.0 / max(texSize, vec2(1.0));
    vec2 p = uv - disp;

    vec4 c = texture(Sampler0, p) * 0.36;
    c += texture(Sampler0, p + vec2(px.x, 0.0)) * 0.16;
    c += texture(Sampler0, p + vec2(-px.x, 0.0)) * 0.16;
    c += texture(Sampler0, p + vec2(0.0, px.y)) * 0.16;
    c += texture(Sampler0, p + vec2(0.0, -px.y)) * 0.16;

    float frameScale = clamp(dt * 60.0, 0.25, 2.5);
    float flick = 1.0 - flickAmp + flickAmp * (0.5 + 0.5 * sin(t * 14.0) + 0.25 * sin(t * 9.3));
    float oldA = c.a;
    float retention = pow(clamp(0.985 * flick, 0.0, 1.0), frameScale);
    float newA = max(0.0, oldA * retention - fade * frameScale);
    float scale = oldA > 0.001 ? newA / oldA : 0.0;
    OutColor = vec4(c.rgb * scale, newA);
}
