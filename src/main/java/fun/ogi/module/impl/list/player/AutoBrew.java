package fun.ogi.module.impl.list.player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.ListSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.util.NotificationManager;
import fun.ogi.util.time.Timer;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.gui.screen.ingame.BrewingStandScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.BrewingStandScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

@ModuleInformation(moduleName = "Auto Brew", moduleDesc = "Automatically brews potions", moduleCategory = ModuleCategory.PLAYER)
public class AutoBrew extends Module {

    private final ListSetting potions = new ListSetting("Brew", this, "Strength", "Speed", "Fire Resistance", "Invisible");
    private final SliderSetting delay = new SliderSetting("Delay", this, 100, 100, 1000, 10);

    private final Timer timer = new Timer();
    private final Timer actionTimer = new Timer();
    private State state = State.IDLE;
    private BrewingStandBlockEntity currentBrewer;
    private ChestBlockEntity currentChest;
    private final List<BlockPos> processedBrewers = new ArrayList<>();
    private List<BrewingStandBlockEntity> brewersQueue = new ArrayList<>();
    public AutoBrew(){
        addSettings(potions,delay);
    }
    @Subscribe
    public void onUpdate(EventUpdate event) {
        switch (this.state) {
            case IDLE -> handleIdleState();
            case OPENING_BREWER -> handleOpeningState();
            case PROCESSING -> handleProcessingState();
            case DEPOSITING -> handleDepositingState();
            case CLOSING -> handleClosingState();
        }
    }

    private void handleIdleState() {
        if (this.actionTimer.finished(1000L)) {
            if (this.brewersQueue.isEmpty()) {
                this.brewersQueue = this.findBrewers();
            }

            if (!this.brewersQueue.isEmpty()) {
                this.currentBrewer = this.brewersQueue.removeFirst();
                this.state = AutoBrew.State.OPENING_BREWER;
                this.actionTimer.reset();
            }
        }
    }

    private void handleOpeningState() {
        if (mc.currentScreen instanceof BrewingStandScreen) {
            this.state = AutoBrew.State.PROCESSING;
        } else {
            if (this.actionTimer.finished(500L)) {
                BlockPos pos = this.currentBrewer.getPos();
                Vec3d vec = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                BlockHitResult hit = new BlockHitResult(vec, Direction.UP, pos, false);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                this.actionTimer.reset();
            }
        }
    }


    private void handleProcessingState() {
        if (!(mc.player.currentScreenHandler instanceof BrewingStandScreenHandler brew)) {
            this.state = State.IDLE;
            return;
        }

        if (brew.getFuel() <= 0 || brew.getSlot(3).getStack().getItem() == Items.AIR) {
            if (brew.getSlot(4).getStack().getItem() == Items.AIR && brew.getFuel() == 0) {
                if (findIngredient(Items.BLAZE_POWDER) == -1) return;
                swapOneItem(Items.BLAZE_POWDER, 4);
            }

            for (int i = 0; i < 3; i++) {
                if (brew.getSlot(i).getStack().getItem() == Items.AIR) {
                    if (findWaterBottle(brew) == -1) return;
                    quickMove(findWaterBottle(brew));
                }
            }

            if (brew.getSlot(3).getStack().getItem() == Items.AIR) {
                if (isPotionType(brew, Potions.WATER.value())) {
                    if (findIngredient(Items.NETHER_WART) == -1) {
                        NotificationManager.post("Item not found! Need " + Items.NETHER_WART.getName().getString(), NotificationManager.TYPE_ERROR);
                        return;
                    }
                    handleIngredient(Items.NETHER_WART, 3);
                }

                if (potions.isSelected("Strength") && isPotionType(brew, Potions.AWKWARD.value())) {
                    handleIngredient(Items.BLAZE_POWDER, 3);
                } else if (potions.isSelected("Speed") && isPotionType(brew, Potions.AWKWARD.value())) {
                    handleIngredient(Items.SUGAR, 3);
                } else if (potions.isSelected("Fire Resistance") && isPotionType(brew, Potions.AWKWARD.value())) {
                    handleIngredient(Items.MAGMA_CREAM, 3);
                } else if(potions.isSelected("Invisible") && isPotionType(brew,Potions.AWKWARD.value())){
                    handleIngredient(Items.GOLDEN_CARROT, 3);
                }

                if (isPotionType(brew, Potions.STRENGTH.value()) || isPotionType(brew, Potions.SWIFTNESS.value())) {
                    handleIngredient(Items.GLOWSTONE_DUST, 3);
                }

                if (isPotionType(brew, Potions.FIRE_RESISTANCE.value())) {
                    handleIngredient(Items.REDSTONE, 3);
                }
                if(isPotionType(brew,Potions.NIGHT_VISION.value())){
                    handleIngredient(Items.FERMENTED_SPIDER_EYE,3);
                }
                if(isPotionType(brew, Potions.INVISIBILITY.value())){
                    handleIngredient(Items.REDSTONE,3);
                }
                if (isPotionType(brew, Potions.STRONG_STRENGTH.value())
                        || isPotionType(brew, Potions.STRONG_SWIFTNESS.value())
                        || isPotionType(brew, Potions.LONG_FIRE_RESISTANCE.value())) {
                    lootPotions(brew);
                    this.state = State.DEPOSITING;
                }
            }
        }
    }

    private void handleIngredient(Item item, int slot) {
        if (findIngredient(item) == -1) {
            NotificationManager.post("Item not found! Need " + item.getName().getString(), NotificationManager.TYPE_ERROR);
            toggle();
        } else {
            swapOneItem(item, slot);
            mc.player.closeHandledScreen();
        }
    }

    private void handleDepositingState() {
        if (this.actionTimer.finished(500L)) {
            List<ChestBlockEntity> chests = findChests();
            if (!chests.isEmpty()) {
                this.currentChest = chests.getFirst();
                depositPotions();
            }
            this.state = State.CLOSING;
            this.actionTimer.reset();
        }
    }

    private void handleClosingState() {
        if (this.actionTimer.finished(500L)) {
            mc.player.closeHandledScreen();
            if (this.currentBrewer != null) {
                this.processedBrewers.add(this.currentBrewer.getPos());
            }
            this.state = State.IDLE;
            this.currentBrewer = null;
            this.currentChest = null;
            this.actionTimer.reset();
        }
    }

    private List<BrewingStandBlockEntity> findBrewers() {
        List<BrewingStandBlockEntity> brewers = new ArrayList<>();
        int range = 20;
        BlockPos playerPos = BlockPos.ofFloored(mc.player.getPos());
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (mc.world.getBlockEntity(pos) instanceof BrewingStandBlockEntity brewer) {
                        brewers.add(brewer);
                    }
                }
            }
        }
        return brewers;
    }

    private List<ChestBlockEntity> findChests() {
        List<ChestBlockEntity> chests = new ArrayList<>();
        int range = 10;
        BlockPos playerPos = BlockPos.ofFloored(mc.player.getPos());
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (mc.world.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                        chests.add(chest);
                    }
                }
            }
        }
        chests.sort(Comparator.comparingDouble(c -> c.getPos().getSquaredDistance(playerPos)));
        return chests;
    }

    private void depositPotions() {
        if (this.currentChest == null) return;
        int syncId = mc.player.currentScreenHandler.syncId;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isPotion(stack)) {
                int chestSlot = findEmptyChestSlot(this.currentChest);
                if (chestSlot != -1) {
                    int containerSlot = i < 9 ? i + 36 : i;
                    mc.interactionManager.clickSlot(syncId, containerSlot, chestSlot, SlotActionType.QUICK_MOVE, mc.player);
                }
            }
        }
    }

    private boolean isPotion(ItemStack stack) {
        return stack.getItem() == Items.POTION || stack.getItem() == Items.SPLASH_POTION || stack.getItem() == Items.LINGERING_POTION;
    }

    private int findEmptyChestSlot(ChestBlockEntity chest) {
        for (int i = 0; i < chest.size(); i++) {
            if (chest.getStack(i).isEmpty()) return i;
        }
        return -1;
    }

    private void lootPotions(BrewingStandScreenHandler brew) {
        for (int i = 0; i < 3; i++) {
            if (!brew.getSlot(i).getStack().isEmpty()) {
                quickMove(i);
            }
        }
    }

    private void swapOneItem(Item item, int to) {
        if (this.timer.finished((long) (delay.getValue() * 2.0))) {
            int slot = findIngredient(item);
            if (slot != -1) {
                int syncId = mc.player.currentScreenHandler.syncId;
                mc.interactionManager.clickSlot(syncId, slot, to, SlotActionType.SWAP, mc.player);
                this.timer.reset();
            }
        }
    }

    private void quickMove(int slot) {
        int syncId = mc.player.currentScreenHandler.syncId;
        mc.interactionManager.clickSlot(syncId, slot, 0, SlotActionType.QUICK_MOVE, mc.player);
    }

    private int findIngredient(Item item) {
        for (int i = 5; i < 41; i++) {
            if (mc.player.currentScreenHandler.getSlot(i).getStack().getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private boolean isPotionType(BrewingStandScreenHandler brew, Potion potion) {
        for (int i = 0; i < 3; i++) {
            ItemStack stack = brew.getSlot(i).getStack();
            if (stack.getItem() == Items.POTION && stack.get(DataComponentTypes.POTION_CONTENTS) != null) {
                PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
                if (contents.potion().isPresent()) {
                    RegistryEntry<Potion> entry = contents.potion().get();
                    if (entry.value() != potion) return false;
                }
            }
        }
        return true;
    }

    private int findWaterBottle(BrewingStandScreenHandler brew) {
        for (int i = 5; i < 41; i++) {
            ItemStack stack = brew.getSlot(i).getStack();
            if (stack.getItem() == Items.POTION && stack.get(DataComponentTypes.POTION_CONTENTS) != null) {
                PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
                if (contents.potion().isPresent() && contents.potion().get().value() == Potions.WATER.value()) {
                    return i;
                }
            }
        }
        return -1;
    }

    private enum State {
        IDLE,
        OPENING_BREWER,
        PROCESSING,
        DEPOSITING,
        CLOSING
    }
}

