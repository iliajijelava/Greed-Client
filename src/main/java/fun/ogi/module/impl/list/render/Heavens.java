package fun.ogi.module.impl.list.render;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.SliderSetting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

@ModuleInformation(
    moduleName = "Heavens",
    moduleDesc = "Super krasota brat ai tigrt lev",
    moduleCategory = ModuleCategory.RENDER
)
public class Heavens extends Module {
    private final List<HeavensRenderer.Tile> tiles = new ArrayList<>();

    public final SliderSetting tileCount = new SliderSetting("Plates count", this, 20.0, 5.0, 100.0, 1.0);
    public final SliderSetting spawnRadius = new SliderSetting("Radius", this, 3.0, 1.0, 10.0, 0.5);
    public final SliderSetting tileSize = new SliderSetting("Size", this, 0.3, 0.1, 0.8, 0.05);
    public final SliderSetting tileThickness = new SliderSetting("Scale", this, 0.05, 0.01, 0.15, 0.01);
    public final SliderSetting pillarHeight = new SliderSetting("Height", this, 0.5, 0.1, 2.0, 0.05);
    public final SliderSetting pillarWidth = new SliderSetting("Width", this, 0.08, 0.02, 0.3, 0.01);
    public final SliderSetting animationSpeed = new SliderSetting("Speed", this, 800.0, 200.0, 2000.0, 50.0);
    public final SliderSetting lifetime = new SliderSetting("Living time (sec)", this, 5.0, 1.0, 15.0, 0.5);
    public final SliderSetting glowIntensity = new SliderSetting("Intesnivity", this, 1.0, 0.1, 3.0, 0.1);
    public final SliderSetting tileAlpha = new SliderSetting("Transparency", this, 0.3, 0.1, 1.0, 0.05);
    public final SliderSetting pillarAlpha = new SliderSetting("Transparency 2", this, 0.6, 0.1, 1.0, 0.05);

    public Heavens() {
        addSettings(tileCount, spawnRadius, tileSize, tileThickness,
            pillarHeight, pillarWidth, animationSpeed, lifetime,
            glowIntensity, tileAlpha, pillarAlpha);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        tiles.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        tiles.clear();
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        double playerX = mc.player.getX();
        double playerY = mc.player.getY();
        double playerZ = mc.player.getZ();

        tiles.removeIf(tile -> {
            double dx = tile.x - playerX;
            double dz = tile.z - playerZ;
            return Math.sqrt(dx * dx + dz * dz) > spawnRadius.getFloatValue() + 5.0;
        });

        if (tiles.size() < tileCount.getIntValue()) {
            double radius = spawnRadius.getFloatValue();
            double angle = Math.random() * Math.PI * 2.0;
            double distance = Math.random() * radius;

            double x = playerX + Math.cos(angle) * distance;
            double z = playerZ + Math.sin(angle) * distance;

            BlockPos groundPos = findGround(x, playerY, z);
            double finalY = groundPos != null ? groundPos.getY() : playerY - 1.0;

            tiles.add(new HeavensRenderer.Tile(x, finalY, z, (int) animationSpeed.getFloatValue(), (int) (lifetime.getValue() * 1000)));
         }

         long now = System.currentTimeMillis();
         tiles.removeIf(tile -> tile.isExpired(now));
      }

   private BlockPos findGround(double x, double startY, double z) {
        BlockPos startPos = BlockPos.ofFloored(x, startY, z);

        for (int i = 0; i < 10; i++) {
            BlockPos checkPos = startPos.down(i);
            if (mc.world.getBlockState(checkPos).isSolidBlock(mc.world, checkPos)) {
                return checkPos.up(1);
            }
        }

        for (int i = 1; i < 5; i++) {
            BlockPos checkPos = startPos.up(i);
            if (mc.world.getBlockState(checkPos).isSolidBlock(mc.world, checkPos)) {
                return checkPos.up(1);
            }
        }

        return startPos;
    }

    @Subscribe
    public void onWorldRender(EventWorldRenderer e) {
        if (tiles.isEmpty()) return;
        HeavensRenderer.renderAll(e, tiles, this);
    }
}

