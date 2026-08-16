package fun.ogi.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

import static fun.ogi.util.MinecraftUtil.mc;

public class GlProgram {
    private static final List<Runnable> REGISTERED_PROGRAMS = new ArrayList();
    protected ShaderProgram backingProgram;
    public ShaderProgramKey programKey;

    public GlProgram(Identifier id, VertexFormat vertexFormat) {
        this.programKey = new ShaderProgramKey(id.withPrefixedPath("core/"), vertexFormat, Defines.EMPTY);
        REGISTERED_PROGRAMS.add(() -> {
            try {
                this.backingProgram = mc.getShaderLoader().getProgramToLoad(this.programKey);
                this.setup();
            } catch (ShaderLoader.LoadException var2) {
                System.out.println("[Wyvern] FAILED to load shader program " + this.programKey + " : " + var2.getMessage());
            }
        });
    }

    public RenderPhase renderPhaseProgram() {
        return new net.minecraft.client.render.RenderPhase.ShaderProgram(this.programKey);
    }

    public ShaderProgram use() {
        if (this.backingProgram == null) {

            return null;
        }
        return RenderSystem.setShader(this.programKey);
    }

    public boolean isLoaded() {
        return this.backingProgram != null;
    }

    protected void setup() {
    }

    public GlUniform findUniform(String name) {
        if (this.backingProgram == null) {

            return null;
        }
        return this.backingProgram.getUniform(name);
    }

    @ApiStatus.Internal
    public static void loadAndSetupPrograms() {
        REGISTERED_PROGRAMS.forEach(Runnable::run);
    }
}

