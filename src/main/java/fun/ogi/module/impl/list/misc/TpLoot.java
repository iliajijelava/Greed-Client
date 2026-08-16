package fun.ogi.module.impl.list.misc;

import java.util.List;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.util.chatutil.ChatUtil;
import fun.ogi.util.time.Timer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.math.Vec3d;
@ModuleInformation(moduleName = "Tp Loot",moduleDesc = "Teleports to resources", moduleCategory = ModuleCategory.MISC)
public class TpLoot extends Module {

    public static TpLoot INSTANCE = new TpLoot();

    private final SliderSetting range = new SliderSetting("Distance",this, 10.0F, 3.0F, 50.0F, 1.0F);
    private final SliderSetting lootDelay = new SliderSetting("Loot delay",this, 500.0F, 100.0F, 5000.0F, 50.0F);
    private final ModeSetting afterLoot = new ModeSetting("After loot", this,"Back", "Back", "Tp to spawn");
    private final SliderSetting actionDelay = new SliderSetting("Action delay",this, 1000.0F, 200.0F, 10000.0F, 100.0F);

    private final Timer lootTimer = new Timer();
    private final Timer actionTimer = new Timer();
    private Vec3d originalPos = null;
    private boolean waitingAction = false;

    private static final List<Item> TARGET_ITEMS = List.of(
            Items.NETHERITE_SWORD,
            Items.NETHERITE_HELMET,
            Items.NETHERITE_CHESTPLATE,
            Items.NETHERITE_LEGGINGS,
            Items.NETHERITE_BOOTS,
            Items.PLAYER_HEAD,
            Items.GOLDEN_APPLE,
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.END_CRYSTAL,
            Items.TOTEM_OF_UNDYING,
            Items.ELYTRA
    );

    public TpLoot() {
        addSettings(range, lootDelay, afterLoot, actionDelay);
    }

    
    @Subscribe
    @SuppressWarnings("unused")
    public void onTick(final EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        if (this.waitingAction) {
            if (this.actionTimer.finished((long) this.actionDelay.getFloatValue())) {
                if (afterLoot.getValueAsString().equals("Back") && this.originalPos != null) {
                    this.teleportTo(this.originalPos);
                    ChatUtil.sendMSG("TpLoot: backing");
                }
                if (afterLoot.getValueAsString().equals("Tp to spawn")) {
                    mc.player.networkHandler.sendChatCommand("spawn");
                    ChatUtil.sendMSG("TpLoot: used /spawn");
                }
                this.waitingAction = false;
                this.originalPos = null;
                this.lootTimer.reset();
            }
            return;
        }

        if (!this.lootTimer.finished((long) this.lootDelay.getFloatValue())) return;

        ItemEntity targetItem = this.findTargetItem();
        if (targetItem == null) return;

        this.originalPos = mc.player.getPos();

        Vec3d itemPos = targetItem.getPos();
        this.teleportTo(itemPos);

        ItemStack stack = targetItem.getStack();
        ChatUtil.sendMSG("TpLoot: looted " + stack.getName().getString());

        this.lootTimer.reset();
        this.waitingAction = true;
        this.actionTimer.reset();
    }

    private ItemEntity findTargetItem() {
        double maxRange = this.range.getValue();
        ItemEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemEntity itemEntity)) continue;

            ItemStack stack = itemEntity.getStack();
            if (!this.isTargetItem(stack.getItem())) continue;

            double dist = mc.player.squaredDistanceTo(entity);
            if (dist > maxRange * maxRange) continue;

            if (dist < closestDist) {
                closestDist = dist;
                closest = itemEntity;
            }
        }

        return closest;
    }

    private boolean isTargetItem(Item item) {
        return TARGET_ITEMS.contains(item);
    }

    
    private void teleportTo(Vec3d pos) {
        int packets = (int) Math.ceil(mc.player.getPos().distanceTo(pos) / 10.0);
        packets = Math.max(packets, 3);

        for (int i = 0; i < packets; i++) {
            mc.player.networkHandler.sendPacket(new OnGroundOnly(mc.player.isOnGround(), mc.player.horizontalCollision));
        }

        mc.player.networkHandler.sendPacket(new PositionAndOnGround(pos.x, pos.y, pos.z, false, mc.player.horizontalCollision));
        mc.player.setPosition(pos.x, pos.y, pos.z);
    }

    public void onEnable() {
        this.originalPos = null;
        this.waitingAction = false;
        this.lootTimer.reset();
        this.actionTimer.reset();
        super.onEnable();
    }

    public void onDisable() {
        this.originalPos = null;
        this.waitingAction = false;
        super.onDisable();
    }
}

