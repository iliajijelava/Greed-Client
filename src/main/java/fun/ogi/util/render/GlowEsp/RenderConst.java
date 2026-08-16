package fun.ogi.util.render.GlowEsp;

import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public class RenderConst {
   public static ShaderProgramKey GLOWESP_SHADER_KEY;

   public static void load() {
      GLOWESP_SHADER_KEY = new ShaderProgramKey(Identifier.of("cheap", "glowesp"), VertexFormats.POSITION_COLOR, Defines.EMPTY);
   }
}

