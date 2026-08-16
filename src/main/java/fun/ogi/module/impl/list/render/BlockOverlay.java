package fun.ogi.module.impl.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.module.theme.ThemeManager;
import fun.ogi.util.ShaderUtil;
import fun.ogi.util.animation.Animation;
import fun.ogi.util.animation.Easing;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;

import java.awt.Color;

@ModuleInformation(moduleName = "Block Overlay", moduleDesc = "Renders a custom block overlay", moduleCategory = ModuleCategory.RENDER)
public class BlockOverlay extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", this, "Shader", "Shader", "Strings");
    private final ModeSetting shaderPreset = new ModeSetting("Shader Preset", this, "Cloud", "Cloud", "Balatro", "Plasma");
    private final SliderSetting waveSpeed = new SliderSetting("Wave Speed", this, 1.2, 0.1, 5.0, 0.1);
    private final SliderSetting waveScale = new SliderSetting("Wave Scale", this, 1.0, 1.0, 3.0, 0.1);
    private final SliderSetting lineSpeed = new SliderSetting("Line Speed", this, 1.4, 0.1, 5.0, 0.1);
    private final SliderSetting lineJitter = new SliderSetting("Line Jitter", this, 0.55, 0.0, 1.5, 0.01);
    private final SliderSetting outline = new SliderSetting("Outline Width", this, 1.1, 0.1, 5.0, 0.1);
    private final SliderSetting glow = new SliderSetting("Glow", this, 1.0, 0.0, 5.0, 0.1);
    private final SliderSetting fill = new SliderSetting("Fill", this, 0.6, 0.0, 1.0, 0.01);
    private final SliderSetting alpha = new SliderSetting("Alpha", this, 1.0, 0.0, 1.0, 0.01);
    private final SliderSetting smooth = new SliderSetting("Smooth", this, 0.24, 0.05, 0.6, 0.01);

    private static NativeImageBackedTexture whiteTexture;

    private static int getWhiteTextureId() {
        if (whiteTexture == null) {
            NativeImage image = new NativeImage(1, 1, false);
            image.setColorArgb(0, 0, 0xFFFFFFFF);
            whiteTexture = new NativeImageBackedTexture(image);
        }
        return whiteTexture.getGlId();
    }

    private BlockPos prevBlock;
    private BlockPos currentBlock;
    private final Animation positionAnim = new Animation();
    private Box displayBox;
    private Box targetBox;
    private int cachedThemeColor;
    private float overlayAlpha;

    public BlockOverlay() {
        addSettings(mode, shaderPreset, waveSpeed, waveScale, lineSpeed, lineJitter, outline, glow, fill, alpha, smooth);
    }

    @Override
    public void onDisable() {
        currentBlock = null;
        prevBlock = null;
        displayBox = null;
        targetBox = null;
        overlayAlpha = 0.0f;
        super.onDisable();
    }

    @Subscribe
    public void onRender(EventWorldRenderer e) {
        if (mc == null || mc.world == null || mc.player == null) return;

        BlockHitResult result = getOutlineHitResult();
        Box worldBox = null;
        net.minecraft.util.math.Vec3d cam = e.getCamera().getPos();

        if (result != null && result.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = result.getBlockPos();
            if (pos != null && !mc.world.getBlockState(pos).isAir()) {
                VoxelShape shape = mc.world.getBlockState(pos).getOutlineShape(mc.world, pos);
                if (shape != null && !shape.isEmpty()) {
                    worldBox = shape.getBoundingBox().offset(pos).expand(0.002).offset(-cam.x, -cam.y, -cam.z);
                }
            }

            if (worldBox != null) {
                if (currentBlock == null || !pos.equals(currentBlock)) {
                    prevBlock = currentBlock;
                    currentBlock = pos;
                    positionAnim.start(0, 1, 120, Easing.CUBIC_OUT);
                }
            }
        }

        positionAnim.update();

        if (worldBox != null) {
            if (displayBox == null || targetBox == null || prevBlock == null) {
                displayBox = worldBox;
                targetBox = worldBox;
            } else {
                targetBox = worldBox;
                displayBox = lerpBox(displayBox, targetBox, (float) smooth.getValue());
            }
            overlayAlpha = lerpValue(overlayAlpha, 1.0f, Math.min(1.0f, (float) smooth.getValue() * 1.35f));
        } else {
            if (displayBox == null || targetBox == null) {
                currentBlock = null;
                prevBlock = null;
                overlayAlpha = 0.0f;
                return;
            }
            overlayAlpha = lerpValue(overlayAlpha, 0.0f, 0.18f);
            if (overlayAlpha <= 0.02f) {
                currentBlock = null;
                prevBlock = null;
                displayBox = null;
                targetBox = null;
                overlayAlpha = 0.0f;
                return;
            }
        }

        cachedThemeColor = ThemeManager.getInstance().getPrimary();

        if (mode.getValue().equals("Strings")) {
            drawStrings(e.getMatrices(), displayBox);
        } else {
            drawShaderBox(e.getMatrices(), displayBox);
        }
    }

    private void drawShaderBox(MatrixStack ms, Box box) {
        Color c = new Color(cachedThemeColor);
        float r = c.getRed() / 255f;
        float g = c.getGreen() / 255f;
        float b = c.getBlue() / 255f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        ShaderProgramKey key = getShaderKey();
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(key);
        if (shader != null) {
            float time = (System.currentTimeMillis() % 100000L) / 1000.0f;
            float sc = (float) waveScale.getValue();

            setUniform(shader, "color", r, g, b);
            setUniform(shader, "color2", Math.min(r * 1.8f, 1f), Math.min(g * 1.8f, 1f), Math.min(b * 1.8f, 1f));
            setUniform(shader, "time", time);
            setUniform(shader, "speed", (float) waveSpeed.getValue());
            setUniform(shader, "scale", sc);
            setUniform(shader, "outline", (float) outline.getValue());
            setUniform(shader, "glow", (float) glow.getValue());
            setUniform(shader, "fill", (float) fill.getValue());
            setUniform(shader, "alpha", (float) (alpha.getValue() * overlayAlpha));
            setUniform(shader, "outlineOnly", 0.0f);
            setUniform(shader, "texelSize", 1f, 1f);

            RenderSystem.setShader(key);
            RenderSystem.setShaderTexture(0, getWhiteTextureId());
        } else {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        }

        Matrix4f matrix = ms.peek().getPositionMatrix();

        int a = (int) (alpha.getValue() * overlayAlpha * 255);
        int fillA = (int) (fill.getValue() * a);
        int outA = a;

        if (shader != null) {
            drawFilledBoxTextured(matrix, box, 255, 255, 255, fillA);
        } else {
            drawFilledBox(matrix, box, (int)(r*255), (int)(g*255), (int)(b*255), fillA);
        }
        drawOutlinedBox(matrix, box, (int)(r*255), (int)(g*255), (int)(b*255), outA);

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void drawStrings(MatrixStack ms, Box box) {
        int color = cachedThemeColor;
        Color c = new Color(color);
        float r = c.getRed() / 255f;
        float g = c.getGreen() / 255f;
        float b = c.getBlue() / 255f;
        int a = (int) (alpha.getValue() * overlayAlpha * 200);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Matrix4f matrix = ms.peek().getPositionMatrix();

        drawFilledBox(matrix, box, (int)(r*255), (int)(g*255), (int)(b*255), (int)(a * fill.getValue()));

        int strandsPerFace = 5;
        float t = (System.currentTimeMillis() % 100000L) / 1000.0f * (float) lineSpeed.getValue();
        float bendBase = 0.06f + (float) lineJitter.getValue() * 0.20f;
        int baseAlpha = (int) (alpha.getValue() * overlayAlpha * 210);
        long seed = currentBlock != null ? currentBlock.asLong() : 1L;

        for (int face = 0; face < 6; face++) {
            int[] neighbors = faceNeighbors(face);
            for (int strand = 0; strand < strandsPerFace; strand++) {
                int key = face * 1000 + strand * 53;
                int adj = neighbors[strand % neighbors.length];
                double phase = t * (0.95 + rand01(seed, key + 1) * 0.55) + strand * 0.83 + face * 1.11;
                double edgeT = clamp01(0.5 + Math.sin(phase * 1.37 + rand01(seed, key + 2) * 6.2831853) * 0.38);

                Vec3d pivot = edgePoint(box, face, adj, edgeT, 0.0015);
                Vec3d start = facePoint(box, face,
                        clamp01(0.5 + (rand01(seed, key + 3) - 0.5) * 0.46),
                        clamp01(0.5 + (rand01(seed, key + 4) - 0.5) * 0.46), 0.0015);
                Vec3d end = facePoint(box, adj,
                        clamp01(0.5 + (rand01(seed, key + 5) - 0.5) * 0.46),
                        clamp01(0.5 + (rand01(seed, key + 6) - 0.5) * 0.46), 0.0015);

                Vec3d[] basisA = faceBasis(face);
                Vec3d[] basisB = faceBasis(adj);
                Vec3d normalA = faceNormal(face);
                Vec3d normalB = faceNormal(adj);
                double bendA = bendBase * (0.7 + rand01(seed, key + 7))
                        * Math.sin(phase * 1.9 + rand01(seed, key + 8) * 6.2831853);
                double bendB = bendBase * (0.7 + rand01(seed, key + 9))
                        * Math.cos(phase * 1.7 + rand01(seed, key + 10) * 6.2831853);

                Vec3d dirA = pivot.subtract(start);
                Vec3d c1a = start.add(dirA.multiply(0.38)).add(basisA[0].multiply(bendA)).add(basisA[1].multiply(-bendA * 0.55));
                Vec3d c2a = start.add(dirA.multiply(0.76)).add(basisA[0].multiply(-bendA * 0.65)).add(basisA[1].multiply(bendA * 0.4));

                Vec3d dirB = end.subtract(pivot);
                Vec3d c1b = pivot.add(dirB.multiply(0.24)).add(basisB[0].multiply(bendB)).add(basisB[1].multiply(bendB * 0.45));
                Vec3d c2b = pivot.add(dirB.multiply(0.62)).add(basisB[0].multiply(-bendB * 0.7)).add(basisB[1].multiply(-bendB * 0.35));

                int alphaLine = Math.max(18, Math.min(255, (int) (baseAlpha * (0.74 + 0.26 * Math.sin(phase * 2.6)))));
                drawBezierRibbon(matrix, start, c1a, c2a, pivot, normalA, 18, color, alphaLine, 0.0025f);
                drawBezierRibbon(matrix, pivot, c1b, c2b, end, normalB, 18, color, alphaLine, 0.0025f);
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private ShaderProgramKey getShaderKey() {
        return switch (shaderPreset.getValue()) {
            case "Balatro" -> ShaderUtil.blockOverlayBalatro;
            case "Plasma" -> ShaderUtil.blockOverlayPlasma;
            default -> ShaderUtil.blockOverlay;
        };
    }

    private BlockHitResult getOutlineHitResult() {
        if (mc.player == null || mc.world == null) return null;

        Vec3d start = mc.player.getCameraPosVec(1.0f);
        Vec3d direction = mc.player.getRotationVec(1.0f);
        double reach = 6.0;
        Vec3d end = start.add(direction.multiply(reach));

        HitResult outlineHit = mc.world.raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        return outlineHit instanceof BlockHitResult blockHit ? blockHit : null;
    }

    private Box lerpBox(Box a, Box b, float t) {
        return new Box(
                a.minX + (b.minX - a.minX) * t,
                a.minY + (b.minY - a.minY) * t,
                a.minZ + (b.minZ - a.minZ) * t,
                a.maxX + (b.maxX - a.maxX) * t,
                a.maxY + (b.maxY - a.maxY) * t,
                a.maxZ + (b.maxZ - a.maxZ) * t
        );
    }

    private float lerpValue(float current, float target, float speed) {
        return current + (target - current) * speed;
    }

    private void drawFilledBox(Matrix4f matrix, Box box, int r, int g, int b, int a) {
        if (a <= 0) return;
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buf.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void drawOutlinedBox(Matrix4f matrix, Box box, int r, int g, int b, int a) {
        if (a <= 0) return;
        float w = 0.03f;
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        buf.vertex(matrix, (float) box.minX - w, (float) box.minY - w, (float) box.minZ - w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX - w, (float) box.minY - w, (float) box.maxZ + w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX - w, (float) box.minY, (float) box.maxZ + w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX - w, (float) box.minY, (float) box.minZ - w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX + w, (float) box.minY - w, (float) box.minZ - w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX + w, (float) box.minY, (float) box.minZ - w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX + w, (float) box.minY, (float) box.maxZ + w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX + w, (float) box.minY - w, (float) box.maxZ + w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX - w, (float) box.minY - w, (float) box.minZ - w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX + w, (float) box.minY - w, (float) box.minZ - w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX + w, (float) box.minY - w, (float) box.maxZ + w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX - w, (float) box.minY - w, (float) box.maxZ + w).color(r, g, b, a);

        buf.vertex(matrix, (float) box.minX - w, (float) box.maxY, (float) box.minZ - w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX - w, (float) box.maxY, (float) box.maxZ + w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX - w, (float) box.maxY + w, (float) box.maxZ + w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX - w, (float) box.maxY + w, (float) box.minZ - w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX + w, (float) box.maxY, (float) box.minZ - w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX + w, (float) box.maxY + w, (float) box.minZ - w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX + w, (float) box.maxY + w, (float) box.maxZ + w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX + w, (float) box.maxY, (float) box.maxZ + w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX - w, (float) box.maxY, (float) box.minZ - w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX - w, (float) box.maxY + w, (float) box.minZ - w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX + w, (float) box.maxY + w, (float) box.minZ - w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX + w, (float) box.maxY, (float) box.minZ - w).color(r, g, b, a);


        buf.vertex(matrix, (float) box.minX - w, (float) box.minY - w, (float) box.minZ - w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX - w, (float) box.maxY + w, (float) box.minZ - w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX - w, (float) box.maxY + w, (float) box.maxZ + w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX - w, (float) box.minY - w, (float) box.maxZ + w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX + w, (float) box.minY - w, (float) box.minZ - w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX + w, (float) box.minY - w, (float) box.maxZ + w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX + w, (float) box.maxY + w, (float) box.maxZ + w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX + w, (float) box.maxY + w, (float) box.minZ - w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX - w, (float) box.minY - w, (float) box.minZ - w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX - w, (float) box.minY - w, (float) box.maxZ + w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX + w, (float) box.minY - w, (float) box.maxZ + w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX + w, (float) box.minY - w, (float) box.minZ - w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX - w, (float) box.maxY + w, (float) box.minZ - w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX + w, (float) box.maxY + w, (float) box.minZ - w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.maxX + w, (float) box.maxY + w, (float) box.maxZ + w).color(r, g, b, a);
        buf.vertex(matrix, (float) box.minX - w, (float) box.maxY + w, (float) box.maxZ + w).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void drawFilledBoxTextured(Matrix4f matrix, Box box, int r, int g, int b, int a) {
        if (a <= 0) return;
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        float x1 = (float) box.minX, x2 = (float) box.maxX;
        float y1 = (float) box.minY, y2 = (float) box.maxY;
        float z1 = (float) box.minZ, z2 = (float) box.maxZ;

        buf.vertex(matrix, x1, y1, z1).texture(0, 0).color(r, g, b, a);
        buf.vertex(matrix, x1, y1, z2).texture(0, 1).color(r, g, b, a);
        buf.vertex(matrix, x2, y1, z2).texture(1, 1).color(r, g, b, a);
        buf.vertex(matrix, x2, y1, z1).texture(1, 0).color(r, g, b, a);

        buf.vertex(matrix, x1, y2, z1).texture(0, 0).color(r, g, b, a);
        buf.vertex(matrix, x2, y2, z1).texture(1, 0).color(r, g, b, a);
        buf.vertex(matrix, x2, y2, z2).texture(1, 1).color(r, g, b, a);
        buf.vertex(matrix, x1, y2, z2).texture(0, 1).color(r, g, b, a);

        buf.vertex(matrix, x1, y1, z1).texture(0, 0).color(r, g, b, a);
        buf.vertex(matrix, x2, y1, z1).texture(1, 0).color(r, g, b, a);
        buf.vertex(matrix, x2, y2, z1).texture(1, 1).color(r, g, b, a);
        buf.vertex(matrix, x1, y2, z1).texture(0, 1).color(r, g, b, a);

        buf.vertex(matrix, x1, y1, z2).texture(0, 0).color(r, g, b, a);
        buf.vertex(matrix, x1, y2, z2).texture(0, 1).color(r, g, b, a);
        buf.vertex(matrix, x2, y2, z2).texture(1, 1).color(r, g, b, a);
        buf.vertex(matrix, x2, y1, z2).texture(1, 0).color(r, g, b, a);

        buf.vertex(matrix, x1, y1, z1).texture(0, 0).color(r, g, b, a);
        buf.vertex(matrix, x1, y2, z1).texture(0, 1).color(r, g, b, a);
        buf.vertex(matrix, x1, y2, z2).texture(1, 1).color(r, g, b, a);
        buf.vertex(matrix, x1, y1, z2).texture(1, 0).color(r, g, b, a);

        buf.vertex(matrix, x2, y1, z1).texture(0, 0).color(r, g, b, a);
        buf.vertex(matrix, x2, y1, z2).texture(1, 0).color(r, g, b, a);
        buf.vertex(matrix, x2, y2, z2).texture(1, 1).color(r, g, b, a);
        buf.vertex(matrix, x2, y2, z1).texture(0, 1).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void setUniform(ShaderProgram shader, String name, float value) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    private void setUniform(ShaderProgram shader, String name, float x, float y) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(x, y);
    }

    private void setUniform(ShaderProgram shader, String name, float x, float y, float z) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(x, y, z);
    }

    private void drawBezierRibbon(Matrix4f matrix, Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, Vec3d faceNormal, int samples, int color, int alpha, float halfWidth) {
        Vec3d[] points = new Vec3d[samples + 1];
        for (int s = 0; s <= samples; s++) {
            float u = (float) s / (float) samples;
            points[s] = cubicBezier(p0, p1, p2, p3, u);
        }

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        BufferBuilder quads = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < samples; i++) {
            Vec3d a = points[i];
            Vec3d B = points[i + 1];
            Vec3d dir = B.subtract(a);
            if (dir.lengthSquared() < 1.0E-6) continue;

            Vec3d perp = faceNormal.crossProduct(dir).normalize().multiply(halfWidth);
            Vec3d aL = a.add(perp);
            Vec3d aR = a.subtract(perp);
            Vec3d bL = B.add(perp);
            Vec3d bR = B.subtract(perp);

            quads.vertex(matrix, (float) aL.x, (float) aL.y, (float) aL.z).color(r, g, b, alpha);
            quads.vertex(matrix, (float) aR.x, (float) aR.y, (float) aR.z).color(r, g, b, alpha);
            quads.vertex(matrix, (float) bR.x, (float) bR.y, (float) bR.z).color(r, g, b, alpha);
            quads.vertex(matrix, (float) bL.x, (float) bL.y, (float) bL.z).color(r, g, b, alpha);
        }
        BufferRenderer.drawWithGlobalProgram(quads.end());
    }

    private Vec3d cubicBezier(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, float t) {
        double it = 1.0 - t;
        double it2 = it * it;
        double t2 = t * t;
        return p0.multiply(it2 * it)
                .add(p1.multiply(3.0 * it2 * t))
                .add(p2.multiply(3.0 * it * t2))
                .add(p3.multiply(t2 * t));
    }

    private int[] faceNeighbors(int face) {
        return switch (face) {
            case 0, 1 -> new int[]{2, 3, 4, 5};
            case 2, 3 -> new int[]{0, 1, 4, 5};
            default -> new int[]{0, 1, 2, 3};
        };
    }

    private Vec3d[] faceBasis(int face) {
        return switch (face) {
            case 0, 1 -> new Vec3d[]{new Vec3d(1, 0, 0), new Vec3d(0, 0, 1)};
            case 2, 3 -> new Vec3d[]{new Vec3d(1, 0, 0), new Vec3d(0, 1, 0)};
            default -> new Vec3d[]{new Vec3d(0, 0, 1), new Vec3d(0, 1, 0)};
        };
    }

    private Vec3d faceNormal(int face) {
        return switch (face) {
            case 0 -> new Vec3d(0, 1, 0);
            case 1 -> new Vec3d(0, -1, 0);
            case 2 -> new Vec3d(0, 0, -1);
            case 3 -> new Vec3d(0, 0, 1);
            case 4 -> new Vec3d(-1, 0, 0);
            default -> new Vec3d(1, 0, 0);
        };
    }

    private Vec3d edgePoint(Box box, int faceA, int faceB, double t, double inset) {
        double x = Double.NaN, y = Double.NaN, z = Double.NaN;

        double[] fixedA = faceFixedCoords(box, faceA, inset);
        if (!Double.isNaN(fixedA[0])) x = fixedA[0];
        if (!Double.isNaN(fixedA[1])) y = fixedA[1];
        if (!Double.isNaN(fixedA[2])) z = fixedA[2];

        double[] fixedB = faceFixedCoords(box, faceB, inset);
        if (!Double.isNaN(fixedB[0])) x = fixedB[0];
        if (!Double.isNaN(fixedB[1])) y = fixedB[1];
        if (!Double.isNaN(fixedB[2])) z = fixedB[2];

        double tt = clamp01(t);
        if (Double.isNaN(x)) x = lerp(box.minX, box.maxX, tt);
        if (Double.isNaN(y)) y = lerp(box.minY, box.maxY, tt);
        if (Double.isNaN(z)) z = lerp(box.minZ, box.maxZ, tt);
        return new Vec3d(x, y, z);
    }

    private double[] faceFixedCoords(Box box, int face, double inset) {
        return switch (face) {
            case 0 -> new double[]{Double.NaN, box.maxY - inset, Double.NaN};
            case 1 -> new double[]{Double.NaN, box.minY + inset, Double.NaN};
            case 2 -> new double[]{Double.NaN, Double.NaN, box.minZ + inset};
            case 3 -> new double[]{Double.NaN, Double.NaN, box.maxZ - inset};
            case 4 -> new double[]{box.minX + inset, Double.NaN, Double.NaN};
            default -> new double[]{box.maxX - inset, Double.NaN, Double.NaN};
        };
    }

    private Vec3d facePoint(Box box, int face, double u, double v, double inset) {
        u = clamp01(u);
        v = clamp01(v);
        return switch (face) {
            case 0 -> new Vec3d(lerp(box.minX, box.maxX, u), box.maxY - inset, lerp(box.minZ, box.maxZ, v));
            case 1 -> new Vec3d(lerp(box.minX, box.maxX, u), box.minY + inset, lerp(box.minZ, box.maxZ, v));
            case 2 -> new Vec3d(lerp(box.minX, box.maxX, u), lerp(box.minY, box.maxY, v), box.minZ + inset);
            case 3 -> new Vec3d(lerp(box.minX, box.maxX, u), lerp(box.minY, box.maxY, v), box.maxZ - inset);
            case 4 -> new Vec3d(box.minX + inset, lerp(box.minY, box.maxY, v), lerp(box.minZ, box.maxZ, u));
            default -> new Vec3d(box.maxX - inset, lerp(box.minY, box.maxY, v), lerp(box.minZ, box.maxZ, u));
        };
    }

    private double rand01(long seed, int salt) {
        long x = seed + 0x9E3779B97F4A7C15L * (salt + 1L);
        x ^= (x >>> 30);
        x *= 0xBF58476D1CE4E5B9L;
        x ^= (x >>> 27);
        x *= 0x94D049BB133111EBL;
        x ^= (x >>> 31);
        return (double) (x & 0xFFFFFF) / (double) 0x1000000;
    }

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}

