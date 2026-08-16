#version 150

#moj_import <mre:fragment.glsl>

in vec2 FragCoord;
in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;
uniform vec2 Size;
uniform vec4 Radius;
uniform float Time;

out vec4 OutColor;

void main() {
    // 1. Normalized coordinates within the element (0.0 to 1.0)
    vec2 uv = FragCoord;
    // 2. Normalized screen coordinates
    vec2 uv_screen = TexCoord;
    
    // 3. Coordinate relative to the center for shape calculation
    vec2 m2 = uv - 0.5;
    
    // 4. macOS style superellipse shape
    float roundedBox = pow(abs(m2.x * 2.0), 8.0) + pow(abs(m2.y * 2.0), 8.0);
    
    // Simplified logic: removed rb2 (borders)
    float rb1 = clamp((1.0 - roundedBox) * 8.0, 0.0, 1.0); // rounded box body
    float rb3 = (clamp((1.5 - roundedBox) * 2.0, 0.0, 1.0) - clamp(pow(1.0 - roundedBox, 1.0) * 2.0, 0.0, 1.0)); // shadow gradient

    float transition = smoothstep(0.0, 1.0, rb1);
    
    if (transition > 0.0) {
        // 5. Lens Effect (Refraction)
        vec2 lens_uv = ((uv_screen - 0.5) * 1.0 * (1.0 - roundedBox * 0.05) + 0.5);
       
        // 6. Blur (9x9 samples)
        vec4 blurredColor = vec4(0.0);
        float total = 0.0;
        vec2 res = textureSize(Sampler0, 0);
        
        for (float x = -4.0; x <= 4.0; x++) {
            for (float y = -4.0; y <= 4.0; y++) {
                vec2 offset = vec2(x, y) * 0.85 / res;
                blurredColor += texture(Sampler0, offset + lens_uv);
                total += 1.0;
            }
        }
        blurredColor /= total;
        
        // 7. Lighting
        float gradient = clamp((clamp(m2.y, 0.0, 0.2) + 0.1) / 2.0, 0.0, 1.0) 
                       + clamp((clamp(-m2.y, -1.0, 0.2) * rb3 + 0.1) / 2.0, 0.0, 1.0);
        
        // Final lighting without border glow
        vec4 lighting = clamp(blurredColor + vec4(rb1) * gradient * 0.5, 0.0, 1.0);
        
        // 8. Output Integration
        vec4 background = texture(Sampler0, uv_screen);
        vec4 finalGlass = mix(background, lighting, transition);
        
        float alphaMask = ralpha(Size, FragCoord, Radius, 1.0);
        
        OutColor = vec4(finalGlass.rgb * FragColor.rgb, FragColor.a * alphaMask * transition);
        
    } else { 
        discard;
    }
}
