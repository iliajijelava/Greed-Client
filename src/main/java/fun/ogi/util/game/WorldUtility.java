package fun.ogi.util.game;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.entity.BlockEntity;

public final class WorldUtility {
   public static List<BlockEntity> blockEntities = new ArrayList<>();

   private WorldUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}

