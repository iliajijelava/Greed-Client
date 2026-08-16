#version 150

uniform sampler2D Sampler0;
in vec2 texCoord;
in vec4 vertexColor;
out vec4 fragColor;

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

void main() {
    vec3 msd = texture(Sampler0, texCoord).rgb;
    float sd = median(msd.r, msd.g, msd.b);
    float screenPxDistance = (sd - 0.5) * (1.0 / fwidth(sd));
    float opacity = clamp(screenPxDistance + 0.5, 0.0, 1.0);
    fragColor = vec4(vertexColor.rgb, vertexColor.a * opacity);
}