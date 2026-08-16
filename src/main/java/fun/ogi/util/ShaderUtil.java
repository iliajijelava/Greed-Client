package fun.ogi.util;

import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public final class ShaderUtil {

    public static final ShaderProgramKey roundedRect = register("rect", "rounded_rect", VertexFormats.POSITION_COLOR);
    public static final ShaderProgramKey roundedRectOutline = register("rect", "rounded_rect_outline", VertexFormats.POSITION_COLOR);
    public static final ShaderProgramKey ringArc = register("ring_arc", "ring_arc", VertexFormats.POSITION_COLOR);
    public static final ShaderProgramKey roundedTexture = register("texture", "texture_rect", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey liquidGlass = register("liquidglass", "liquid", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey kawaseDown = register("kawase_down", "kawase_down", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey kawaseUp = register("kawase_up", "kawase_up", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey gradientRect = register("gradient_rect", "gradient", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shadowRect = register("shadow_rect", "shadow", VertexFormats.POSITION_COLOR);
    public static final ShaderProgramKey shadow6Rect = register("shadow6", "shadow", VertexFormats.POSITION_COLOR);
    public static final ShaderProgramKey fontsMsdf = register("fonts", "fonts", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey face = register("face", "face", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey gradient6Rect = register("gradient6", "gradient", VertexFormats.POSITION_COLOR);
    public static final ShaderProgramKey sonar = register("sonar", "sonar", VertexFormats.POSITION_COLOR);
    public static final ShaderProgramKey scanEffect = register("sonar", "scan_effect", VertexFormats.POSITION_TEXTURE);
    public static final ShaderProgramKey blockOverlay = register("blockoverlay", "block_overlay", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey blockOverlayBalatro = register("blockoverlay", "block_overlay_balatro", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey blockOverlayPlasma = register("blockoverlay", "block_overlay_plasma", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey worldShader = register("worldshader", "world_shader", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey customSkySummer = register("skyshader", "sky_summer", VertexFormats.POSITION);
    public static final ShaderProgramKey customSkyWater = register("skyshader", "sky_water", VertexFormats.POSITION);
    public static final ShaderProgramKey customSkyOcean = register("skyshader", "sky_ocean", VertexFormats.POSITION);
    public static final ShaderProgramKey customSkyCaustic = register("skyshader", "caustic", VertexFormats.POSITION);
    public static final ShaderProgramKey customSkyPlasma = register("skyshader", "sky_plasma", VertexFormats.POSITION);
    public static final ShaderProgramKey customSkyShader = register("skyshader", "sky_shader", VertexFormats.POSITION);
    public static final ShaderProgramKey customSkyPulsar = register("skyshader","pulsar",VertexFormats.POSITION);
    public static final ShaderProgramKey customSkyThunder = register("skyshader","thunder",VertexFormats.POSITION);
    public static final ShaderProgramKey chamsFill = register("chams", "chams_fill", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderHandsMaskDiff = register("hands", "hands_mask_diff", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderHandsOverlay = register("hands", "hands_overlay", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderHandsGlow = register("hands", "hands_glow", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderHandsKawaseDown = register("hands", "hands_kawase_down", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderHandsKawaseUp = register("hands", "hands_kawase_up", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderHandsBlit = register("hands", "hands_blit", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderHandsBlurMix = register("hands", "hands_blur_mix", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderHandsFill = register("hands", "hands_fill", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderHandsExtract = register("hands", "hands_extract", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderHandsOutline = register("hands", "hands_outline", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderHandsMirror = register("hands", "hands_mirror", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderHandsBlur = register("hands", "hands_blur", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderHandsTrailAdvect = register("hands", "hands_trail_advect", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderHandsTrailColor = register("hands", "hands_trail_color", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderHandMaskDiff = register("handstrail", "mask_diff", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderHandTrail = register("handstrail", "hand_trail", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderHandFire = register("handstrail", "hand_fire", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderHandsFirePretty = register("handstrail", "hands_fire_pretty", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderEspGlow = register("shaderesp", "glow", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderEspFill = register("shaderesp", "fill", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderEspOutline = register("shaderesp", "outline", VertexFormats.POSITION_TEXTURE_COLOR);

    private ShaderUtil() {
    }

    private static ShaderProgramKey register(String packageName, String shaderName, VertexFormat vertexFormat) {
        return new ShaderProgramKey(Identifier.of("cheap", "core/" + packageName + "/" + shaderName), vertexFormat, Defines.EMPTY);
    }
}

