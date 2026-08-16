package fun.ogi.module.impl.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.events.AttackEvent;
import fun.ogi.events.PacketEvent;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.*;

@ModuleInformation(moduleName = "Ghost Esp", moduleDesc = "Kill effect like sruls visual", moduleCategory = ModuleCategory.RENDER)
public class GhostEsp extends Module {
    private final BooleanSetting onAttack = new BooleanSetting("On attack", this, false);
    private final BooleanSetting themeColor = new BooleanSetting("Theme color", this, true);
    private static final float DURATION = 3.0F;
    private static final float HEIGHT = 3.5F;
    private final List<Ghost> ghosts = new ArrayList<>();
    private final Map<Integer, PlayerData> trackedPlayers = new HashMap<>();
    private final Set<Integer> ghostedRecently = new HashSet<>();

    public GhostEsp() {
        addSettings(onAttack, themeColor);
    }

    @Subscribe
    public void onPacket(PacketEvent e) {
        if (this.mc.player != null && this.mc.world != null) {
            if (e.getPacket() instanceof EntityStatusS2CPacket packet) {
                byte status = packet.getStatus();
                if ((status == 3 || status == 35) && packet.getEntity(this.mc.world) instanceof PlayerEntity player) {
                    
                    this.ghosts.add(new Ghost(player.getPos(), player.getBodyYaw(), player.isSneaking(), (float) player.age, System.currentTimeMillis(), false));
                    ghostedRecently.add(player.getId());
                }
            }
        }
    }

    @Subscribe
    private void onAttack(AttackEvent e) {
        if (mc.player == null || mc.world == null) return;
        Entity target = e.getEntity();
        if (target != null) {
            if(!this.onAttack.getValue()) return;
            this.ghosts.add(new Ghost(target.getPos(), target.getBodyYaw(), target.isSneaking(), (float) target.age, System.currentTimeMillis(), true));
            ghostedRecently.add(target.getId());
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.world == null || mc.player == null) return;
        long now = System.currentTimeMillis();
        Set<Integer> currentIds = new HashSet<>();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            int id = player.getId();
            currentIds.add(id);
            trackedPlayers.put(id, new PlayerData(player.getPos(), player.getBodyYaw(), player.isSneaking(), (float) player.age, now));
        }
        Iterator<Map.Entry<Integer, PlayerData>> iter = trackedPlayers.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer, PlayerData> entry = iter.next();
            int id = entry.getKey();
            PlayerData pd = entry.getValue();
            if (!currentIds.contains(id) && !ghostedRecently.contains(id) && pd.pos.squaredDistanceTo(mc.player.getPos()) <= 4096.0) {
                
                ghosts.add(new Ghost(pd.pos, pd.yaw, pd.sneak, pd.phase, pd.time, false));
            }
            if (!currentIds.contains(id)) iter.remove();
        }
        ghostedRecently.clear();
    }

    @Subscribe
    public void onRender(EventWorldRenderer e) {
        if (this.mc.player != null && this.mc.world != null && !this.ghosts.isEmpty()) {
            long now = System.currentTimeMillis();
            float dur = 3000.0F;
            this.ghosts.removeIf(gx -> (float) (now - gx.time) >= dur);
            MatrixStack m = e.getMatrices();
            Vec3d cam = e.getCamera().getPos();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            
            float baseR, baseG, baseB;
            if (themeColor.getValue()) {
                
                
                baseR = 0.3f;
                baseG = 0.7f;
                baseB = 1.0f;
            } else {
                baseR = 1.0f;
                baseG = 1.0f;
                baseB = 1.0f;
            }

            for (Ghost g : this.ghosts) {
                float t = (float) (now - g.time) / dur;
                if (t >= 1.0F) continue;

                float alpha = (1.0F - t) * 0.6F;
                
                float rise = HEIGHT * this.ease(t);
                
                if (g.fromAttack) {
                    rise += 1.0F; 
                }

                
                m.push();
                m.translate(g.pos.x - cam.x, g.pos.y + (double) rise - cam.y, g.pos.z - cam.z);
                m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - g.yaw));
                m.scale(-1.0F, -1.0F, 1.0F);
                m.translate(0.0, -1.5, 0.0);
                if (g.sneak) {
                    m.translate(0.0, 0.2, 0.0);
                    m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(28.0F));
                }

                Matrix4f mat = m.peek().getPositionMatrix();
                BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                float u = 0.0625F;
                float swing = MathHelper.sin(g.phase * 0.6662F) * 0.6F;

                
                this.box(buf, mat, -4.0F * u, 0.0F, -2.0F * u, 8.0F * u, 12.0F * u, 4.0F * u, baseR, baseG, baseB, alpha);
                this.box(buf, mat, -4.0F * u, -8.0F * u, -4.0F * u, 8.0F * u, 8.0F * u, 8.0F * u, baseR, baseG, baseB, alpha);

                m.push();
                m.translate(-6.0F * u, 2.0F * u, 0.0F);
                m.multiply(RotationAxis.POSITIVE_X.rotation(-swing));
                m.translate(6.0F * u, -2.0F * u, 0.0F);
                this.box(buf, m.peek().getPositionMatrix(), -8.0F * u, -2.0F * u, -2.0F * u, 4.0F * u, 12.0F * u, 4.0F * u, baseR, baseG, baseB, alpha);
                m.pop();

                m.push();
                m.translate(6.0F * u, 2.0F * u, 0.0F);
                m.multiply(RotationAxis.POSITIVE_X.rotation(swing));
                m.translate(-6.0F * u, -2.0F * u, 0.0F);
                this.box(buf, m.peek().getPositionMatrix(), 4.0F * u, -2.0F * u, -2.0F * u, 4.0F * u, 12.0F * u, 4.0F * u, baseR, baseG, baseB, alpha);
                m.pop();

                m.push();
                m.translate(-2.0F * u, 12.0F * u, 0.0F);
                m.multiply(RotationAxis.POSITIVE_X.rotation(swing));
                m.translate(2.0F * u, -12.0F * u, 0.0F);
                this.box(buf, m.peek().getPositionMatrix(), -4.0F * u, 12.0F * u, -2.0F * u, 4.0F * u, 12.0F * u, 4.0F * u, baseR, baseG, baseB, alpha);
                m.pop();

                m.push();
                m.translate(2.0F * u, 12.0F * u, 0.0F);
                m.multiply(RotationAxis.POSITIVE_X.rotation(-swing));
                m.translate(-2.0F * u, -12.0F * u, 0.0F);
                this.box(buf, m.peek().getPositionMatrix(), 0.0F, 12.0F * u, -2.0F * u, 4.0F * u, 12.0F * u, 4.0F * u, baseR, baseG, baseB, alpha);
                m.pop();

                BufferRenderer.drawWithGlobalProgram(buf.end());
                m.pop();

                
                m.push();
                m.translate(g.pos.x - cam.x, g.pos.y + (double) rise - cam.y, g.pos.z - cam.z);
                m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - g.yaw));
                if (g.sneak) {
                    m.translate(0.0, 0.2, 0.0);
                    m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(28.0F));
                }
                
                this.renderHalo(m, alpha, baseR, baseG, baseB);
                m.pop();
            }

            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }

    private void box(BufferBuilder b, Matrix4f m, float x, float y, float z, float sx, float sy, float sz,
                     float r, float g, float bl, float a) {
        float x2 = x + sx;
        float y2 = y + sy;
        float z2 = z + sz;
        b.vertex(m, x, y, z2).color(r, g, bl, a);
        b.vertex(m, x2, y, z2).color(r, g, bl, a);
        b.vertex(m, x2, y2, z2).color(r, g, bl, a);
        b.vertex(m, x, y2, z2).color(r, g, bl, a);

        b.vertex(m, x2, y, z).color(r, g, bl, a);
        b.vertex(m, x, y, z).color(r, g, bl, a);
        b.vertex(m, x, y2, z).color(r, g, bl, a);
        b.vertex(m, x2, y2, z).color(r, g, bl, a);

        b.vertex(m, x, y, z).color(r, g, bl, a);
        b.vertex(m, x, y, z2).color(r, g, bl, a);
        b.vertex(m, x, y2, z2).color(r, g, bl, a);
        b.vertex(m, x, y2, z).color(r, g, bl, a);

        b.vertex(m, x2, y, z2).color(r, g, bl, a);
        b.vertex(m, x2, y, z).color(r, g, bl, a);
        b.vertex(m, x2, y2, z).color(r, g, bl, a);
        b.vertex(m, x2, y2, z2).color(r, g, bl, a);

        b.vertex(m, x, y2, z2).color(r, g, bl, a);
        b.vertex(m, x2, y2, z2).color(r, g, bl, a);
        b.vertex(m, x2, y2, z).color(r, g, bl, a);
        b.vertex(m, x, y2, z).color(r, g, bl, a);

        b.vertex(m, x, y, z).color(r, g, bl, a);
        b.vertex(m, x2, y, z).color(r, g, bl, a);
        b.vertex(m, x2, y, z2).color(r, g, bl, a);
        b.vertex(m, x, y, z2).color(r, g, bl, a);
    }

    private float ease(float t) {
        return 1.0F - (float) Math.pow((double) (1.0F - MathHelper.clamp(t, 0.0F, 1.0F)), 3.0);
    }

    
    private void renderHalo(MatrixStack m, float alpha, float r, float g, float b) {
        float haloY = 2.25F;
        float bigR = 0.28F;
        float tubeR = 0.07F;
        float spin = (float) (System.currentTimeMillis() % 5000L) / 5000.0F * 360.0F;
        int seg = 48;
        int tube = 16;
        m.push();
        m.translate(0.0F, haloY, 0.0F);
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(12.0F));
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spin));
        Matrix4f mat = m.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);

        for (int i = 0; i < seg; i++) {
            float a0 = (float) ((double) i / (double) seg * Math.PI * 2.0);
            float a1 = (float) ((double) (i + 1) / (double) seg * Math.PI * 2.0);
            float cx0 = (float) Math.cos((double) a0) * bigR;
            float cz0 = (float) Math.sin((double) a0) * bigR;
            float cx1 = (float) Math.cos((double) a1) * bigR;
            float cz1 = (float) Math.sin((double) a1) * bigR;
            BufferBuilder strip = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

            for (int j = 0; j <= tube; j++) {
                float tb = (float) ((double) j / (double) tube * Math.PI * 2.0);
                float tx = (float) Math.cos((double) tb) * tubeR;
                float ty = (float) Math.sin((double) tb) * tubeR;
                float br = 0.5F + 0.5F * (float) Math.sin((double) tb);
                float fa = alpha * (0.6F + 0.4F * br);
                
                float colR = r * (br * 1.0F + (1.0F - br) * 0.8F);
                float colG = g * (br * 1.0F + (1.0F - br) * 0.8F);
                float colB = b * (br * 1.0F + (1.0F - br) * 0.8F);
                strip.vertex(mat, cx0 + (float) Math.cos((double) a0) * tx, ty, cz0 + (float) Math.sin((double) a0) * tx)
                        .color(colR, colG, colB, fa);
                strip.vertex(mat, cx1 + (float) Math.cos((double) a1) * tx, ty, cz1 + (float) Math.sin((double) a1) * tx)
                        .color(colR, colG, colB, fa);
            }
            BufferRenderer.drawWithGlobalProgram(strip.end());
        }

        
        BufferBuilder glow = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= seg; i++) {
            float a = (float) ((double) i / (double) seg * Math.PI * 2.0);
            float cos = (float) Math.cos((double) a);
            float sin = (float) Math.sin((double) a);
            glow.vertex(mat, cos * (bigR + tubeR + 0.04F), 0.0F, sin * (bigR + tubeR + 0.04F))
                    .color(r, g, b, alpha * 0.2F);
            glow.vertex(mat, cos * (bigR - tubeR - 0.04F), 0.0F, sin * (bigR - tubeR - 0.04F))
                    .color(r, g, b, 0.0F);
        }
        BufferRenderer.drawWithGlobalProgram(glow.end());

        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        m.pop();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.ghosts.clear();
        this.trackedPlayers.clear();
        this.ghostedRecently.clear();
    }

    
    private static class Ghost {
        Vec3d pos;
        float yaw;
        boolean sneak;
        float phase;
        long time;
        boolean fromAttack;

        Ghost(Vec3d pos, float yaw, boolean sneak, float phase, long time, boolean fromAttack) {
            this.pos = pos;
            this.yaw = yaw;
            this.sneak = sneak;
            this.phase = phase;
            this.time = time;
            this.fromAttack = fromAttack;
        }
    }

    private static class PlayerData {
        Vec3d pos;
        float yaw;
        boolean sneak;
        float phase;
        long time;

        PlayerData(Vec3d pos, float yaw, boolean sneak, float phase, long time) {
            this.pos = pos;
            this.yaw = yaw;
            this.sneak = sneak;
            this.phase = phase;
            this.time = time;
        }
    }
}