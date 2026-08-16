package fun.ogi.module.impl.list.misc;

import baritone.api.event.events.TickEvent;
import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.module.settings.StringSetting;
import fun.ogi.util.ClientLogger;
import fun.ogi.util.baritone.BaritoneHelper;
import fun.ogi.util.chatutil.ChatUtil;
import fun.ogi.util.time.Timer;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOffer;

import java.util.*;
import java.util.function.Predicate;

@ModuleInformation(moduleName = "Auto Trade (Dev)",moduleDesc = "Automatically trades with villagers",moduleCategory = ModuleCategory.MISC)
public class AutoTrade extends PveModule {
    private static final long TRADE_SCREEN_TIMEOUT_MS = 3500L;
    private static final long TRADE_VERIFY_DELAY_MS = 700L;
    private static final Set<Item> TRADE_MEAT_ITEMS = Set.of(
            Items.BEEF,
            Items.COOKED_BEEF,
            Items.PORKCHOP,
            Items.COOKED_PORKCHOP,
            Items.CHICKEN,
            Items.COOKED_CHICKEN,
            Items.MUTTON,
            Items.COOKED_MUTTON,
            Items.RABBIT,
            Items.COOKED_RABBIT,
            Items.COD,
            Items.COOKED_COD,
            Items.SALMON,
            Items.COOKED_SALMON
    );
    private final ModeSetting tradeMode = new ModeSetting("Trade mode",this ,"Gold", "Gold", "Meat", "Both");
    private final StringSetting tradeEmeraldStacks = new StringSetting("Emerald stacks", this,"1");
    private final StringSetting tradeGoldChestSign = new StringSetting("Gold chest sign", this, "gold");
    private final StringSetting tradeMeatChestSign = new StringSetting("Meat chest sign", this, "meat");
    private final SliderSetting tradeScanRadius = new SliderSetting("Villager scan radius", this, 32.0, 4.0, 96.0, 1.0);
    private final SliderSetting tradeChestScanRadius = new SliderSetting("Trade chest scan radius", this,16.0, 2.0, 64.0, 1.0);
    private final SliderSetting tradeInteractRange = new SliderSetting("Trade interact range", this,4.5, 2.0, 6.0, 0.5);
    private final SliderSetting tradeActionDelay = new SliderSetting("Trade action delay", this,250.0, 50.0, 1500.0, 50.0);
    private final SliderSetting tradeCommandDelay = new SliderSetting("Trade command delay", this,1200.0, 250.0, 5000.0, 50.0);
    private final SliderSetting tradeCyclePause = new SliderSetting("Trade cycle pause", this,3000.0, 500.0, 60000.0, 500.0);
    private final BooleanSetting tradeUseBaritone = new BooleanSetting("Trade use Baritone", this,true);
    private final BooleanSetting tradeDebug = new BooleanSetting("Trade debug", this,false);

    private enum TradeState { BUYER_OPEN, BUYER_CATEGORY, BUYER_GOLD, BUYER_EMERALD, BUYER_VERIFY, TRADE_SEARCH, TRADE_WAIT_SCREEN, TRADE_BUY, DEPOSIT_FIND, DEPOSIT_OPEN, DEPOSIT_MOVE, WAIT_NEXT_CYCLE }
    private enum TradeTarget { GOLD, MEAT }

    private final Timer tradeSelectionTimer = new Timer();
    private final Timer tradeStopPathingTimer = new Timer();
    private final Set<UUID> tradeVisitedGoldVillagers = new HashSet<>();
    private final Set<UUID> tradeVisitedMeatVillagers = new HashSet<>();
    private TradeState tradeState = TradeState.BUYER_OPEN;
    private TradeTarget tradeTarget = TradeTarget.GOLD;
    private MerchantEntity tradeCurrentMerchant;
    private BlockPos tradeCurrentStorageChest;
    private int tradeSelectedTradeIndex = -1;
    private int tradeBuyerStartEmeralds;
    private int tradeBuyerGoalEmeralds;
    private int tradeBuyerLastEmeralds;
    private int tradeBuyerAttempts;
    private boolean tradeContinueAfterDeposit;
    private int tradeDepositLastCount;
    private int tradeDepositStallAttempts;
    public AutoTrade(){
        addSettings(tradeMode,tradeEmeraldStacks,tradeGoldChestSign,tradeMeatChestSign,tradeScanRadius,tradeInteractRange,tradeActionDelay,tradeCommandDelay,tradeCyclePause,tradeUseBaritone,tradeDebug);
    }
    @Subscribe
    private void onTick(EventUpdate event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        this.autotrade();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.resetTradeState();
        this.startTradeCycle();
    }

    @Override
    public void onDisable() {
        this.stopTradePathing(true);
        this.resetTradeState();
        super.onDisable();
    }

    private void tradeDebug(String m) {
        if (tradeDebug.getValue()) {
            ClientLogger.info("[AutoTrade Debug] " + m);
        }
    }

    private void tradeWarn(String m) {
        if (warnTimer.finished(3000L)) {
            ChatUtil.sendMSG("§c[AutoTrade] " + m);
            warnTimer.reset();
        }
    }

    private void setTradeState(TradeState s) {
        this.tradeState = s;
        this.stateTimer.reset();
        this.actionTimer.reset();
    }

    public void autotrade() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.getNetworkHandler() == null) {
            return;
        }

        switch (this.tradeState) {
            case BUYER_OPEN -> this.handleTradeBuyerOpen();
            case BUYER_CATEGORY -> this.handleTradeBuyerCategory();
            case BUYER_GOLD -> this.handleTradeBuyerGold();
            case BUYER_EMERALD -> this.handleTradeBuyerEmerald();
            case BUYER_VERIFY -> this.handleTradeBuyerVerify();
            case TRADE_SEARCH -> this.handleTradeSearch();
            case TRADE_WAIT_SCREEN -> this.handleTradeWaitScreen();
            case TRADE_BUY -> this.handleTradeBuy();
            case DEPOSIT_FIND -> this.handleTradeDepositFind();
            case DEPOSIT_OPEN -> this.handleTradeDepositOpen();
            case DEPOSIT_MOVE -> this.handleTradeDepositMove();
            case WAIT_NEXT_CYCLE -> this.handleTradeWaitNextCycle();
        }
    }
    private void startTradeCycle() {
        this.tradeVisitedGoldVillagers.clear();
        this.tradeVisitedMeatVillagers.clear();
        this.tradeContinueAfterDeposit = false;
        this.tradeDepositLastCount = 0;
        this.tradeDepositStallAttempts = 0;
        this.startTradeTarget(this.firstTradeTarget());
    }

    private void resetTradeState() {
        this.tradeSelectionTimer.reset();
        this.tradeStopPathingTimer.reset();
        this.tradeVisitedGoldVillagers.clear();
        this.tradeVisitedMeatVillagers.clear();
        this.tradeState = TradeState.BUYER_OPEN;
        this.tradeTarget = TradeTarget.GOLD;
        this.tradeCurrentMerchant = null;
        this.tradeCurrentStorageChest = null;
        this.tradeSelectedTradeIndex = -1;
        this.tradeBuyerStartEmeralds = 0;
        this.tradeBuyerGoalEmeralds = 0;
        this.tradeBuyerLastEmeralds = 0;
        this.tradeBuyerAttempts = 0;
        this.tradeContinueAfterDeposit = false;
    }

    private TradeTarget firstTradeTarget() {
        return this.tradeMode.is("Meat") ? TradeTarget.MEAT : TradeTarget.GOLD;
    }

    private void startTradeTarget(TradeTarget target) {
        this.tradeTarget = target;
        this.tradeCurrentMerchant = null;
        this.tradeCurrentStorageChest = null;
        this.tradeSelectedTradeIndex = -1;
        this.tradeContinueAfterDeposit = false;
        this.startTradeBuyerOrTrade();
    }
    private void startTradeBuyerOrTrade() {
        if (this.countItem(Items.EMERALD) <= 0) {
            this.startTradeBuyer();
        } else {
            this.setTradeState(TradeState.TRADE_SEARCH);
        }
    }

    private void startTradeBuyer() {
        this.tradeBuyerStartEmeralds = this.countItem(Items.EMERALD);
        this.tradeBuyerGoalEmeralds = this.tradeBuyerStartEmeralds + this.getTradeEmeraldBuyAmount();
        this.tradeBuyerLastEmeralds = this.tradeBuyerStartEmeralds;
        this.tradeBuyerAttempts = 0;
        this.setTradeState(TradeState.BUYER_OPEN);
    }

    private void finishTradeBuyer() {
        this.closeTradeScreen();
        this.tradeSelectedTradeIndex = -1;
        this.tradeCurrentMerchant = null;
        this.setTradeState(TradeState.TRADE_SEARCH);
    }

    private void handleTradeBuyerOpen() {
        if (this.countItem(Items.EMERALD) >= this.tradeBuyerGoalEmeralds) {
            this.finishTradeBuyer();
            return;
        }

        if (!this.actionTimer.finished((long)this.tradeCommandDelay.getValue())) {
            return;
        }

        if (mc.currentScreen != null) {
            this.closeTradeScreen();
            this.actionTimer.reset();
            return;
        }

        this.sendTradeChatCommand("/shop");
        this.setTradeState(TradeState.BUYER_CATEGORY);
        this.tradeDebug("opened /shop");
    }
    private void handleTradeBuyerCategory() {
        if (this.countItem(Items.EMERALD) >= this.tradeBuyerGoalEmeralds) {
            this.finishTradeBuyer();
            return;
        }

        GenericContainerScreenHandler handler = this.tradeContainerHandler();
        if (handler != null && this.actionTimer.finished((long)this.tradeActionDelay.getValue())) {
            int chestSlot = this.findTradeShopChestSlot(handler);
            if (chestSlot != -1) {
                this.clickTradeSlot(handler.syncId, chestSlot, 0, SlotActionType.PICKUP);
                this.setTradeState(TradeState.BUYER_GOLD);
                this.tradeDebug("clicked shop chest category");
                return;
            }

            if (this.findTradeContainerSlot(handler, stack -> stack.isOf(Items.GOLD_INGOT)) != -1) {
                this.setTradeState(TradeState.BUYER_GOLD);
                return;
            }

            if (this.findTradeContainerSlot(handler, stack -> stack.isOf(Items.EMERALD)) != -1) {
                this.setTradeState(TradeState.BUYER_EMERALD);
                return;
            }
        }

        if (this.stateTimer.finished(TRADE_SCREEN_TIMEOUT_MS)) {
            this.retryTradeBuyerOrFinish("buyer category not found");
        }
    }

    private void handleTradeBuyerGold() {
        if (this.countItem(Items.EMERALD) >= this.tradeBuyerGoalEmeralds) {
            this.finishTradeBuyer();
            return;
        }

        GenericContainerScreenHandler handler = this.tradeContainerHandler();
        if (handler != null && this.actionTimer.finished((long)this.tradeActionDelay.getValue())) {
            int goldSlot = this.findTradeContainerSlot(handler, stack -> stack.isOf(Items.GOLD_INGOT));
            if (goldSlot != -1) {
                this.clickTradeSlot(handler.syncId, goldSlot, 0, SlotActionType.PICKUP);
                this.setTradeState(TradeState.BUYER_EMERALD);
                this.tradeDebug("clicked shop gold category");
                return;
            }

            if (this.findTradeContainerSlot(handler, stack -> stack.isOf(Items.EMERALD)) != -1) {
                this.setTradeState(TradeState.BUYER_EMERALD);
                return;
            }
        }

        if (this.stateTimer.finished(TRADE_SCREEN_TIMEOUT_MS)) {
            this.retryTradeBuyerOrFinish("buyer gold category not found");
        }
    }

    private void handleTradeBuyerEmerald() {
        if (this.countItem(Items.EMERALD) >= this.tradeBuyerGoalEmeralds) {
            this.finishTradeBuyer();
            return;
        }

        GenericContainerScreenHandler handler = this.tradeContainerHandler();
        if (handler != null && this.actionTimer.finished((long)this.tradeActionDelay.getValue())) {
            int emeraldSlot = this.findTradeContainerSlot(handler, stack -> stack.isOf(Items.EMERALD));
            if (emeraldSlot != -1) {
                this.tradeBuyerLastEmeralds = this.countItem(Items.EMERALD);
                this.clickTradeSlot(handler.syncId, emeraldSlot, 1, SlotActionType.QUICK_MOVE);
                this.setTradeState(TradeState.BUYER_VERIFY);
                this.tradeDebug("shift-right clicked shop emeralds");
                return;
            }
        }

        if (this.stateTimer.finished(TRADE_SCREEN_TIMEOUT_MS)) {
            this.retryTradeBuyerOrFinish("buyer emerald slot not found");
        }
    }
    private void handleTradeBuyerVerify() {
        int emeralds = this.countItem(Items.EMERALD);
        if (emeralds >= this.tradeBuyerGoalEmeralds) {
            this.finishTradeBuyer();
            return;
        }

        if (!this.actionTimer.finished(TRADE_VERIFY_DELAY_MS)) {
            return;
        }

        if (emeralds > this.tradeBuyerLastEmeralds) {
            this.tradeBuyerLastEmeralds = emeralds;
            if (this.tradeBuyerAttempts < this.maxTradeBuyerAttempts()) {
                this.setTradeState(TradeState.BUYER_OPEN);
            } else {
                this.finishTradeBuyer();
            }
            return;
        }

        this.retryTradeBuyerOrFinish("buyer did not add emeralds");
    }

    private void retryTradeBuyerOrFinish(String reason) {
        this.tradeDebug(reason);
        if (this.tradeBuyerAttempts >= this.maxTradeBuyerAttempts()) {
            this.finishTradeBuyer();
        } else {
            this.tradeBuyerAttempts++;
            this.setTradeState(TradeState.BUYER_OPEN);
        }
    }

    private int maxTradeBuyerAttempts() {
        return Math.max(1, (this.getTradeEmeraldBuyAmount() + 63) / 64 + 3);
    }

    private void handleTradeSearch() {
        if (this.countItem(Items.EMERALD) <= 0) {
            if (this.countTradeTargetItems(this.tradeTarget) > 0) {
                this.startTradeDeposit(true);
            } else {
                this.startTradeBuyer();
            }
            return;
        }

        if (!this.hasRoomForTradeTarget(this.tradeTarget)) {
            if (this.countTradeTargetItems(this.tradeTarget) > 0) {
                this.startTradeDeposit(true);
            } else {
                this.tradeWarn("No inventory space for " + this.tradeTarget.name().toLowerCase(Locale.ROOT));
                this.finishTradeTarget();
            }
            return;
        }

        if (mc.currentScreen != null) {
            this.closeTradeScreen();
            this.actionTimer.reset();
            return;
        }

        MerchantEntity merchant = this.findNearestTradeMerchant();
        if (merchant == null) {
            this.startTradeDepositOrFinishTarget();
            return;
        }

        this.tradeCurrentMerchant = merchant;
        double maxDistance = this.tradeInteractRange.getValue() * this.tradeInteractRange.getValue();
        if (mc.player.squaredDistanceTo(merchant) <= maxDistance) {
            if (!this.actionTimer.finished((long)this.tradeActionDelay.getValue())) {
                return;
            }

            this.stopTradePathing();
            this.lookAtTradeTarget(merchant.getEyePos());
            mc.interactionManager.interactEntity(mc.player, merchant, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            this.setTradeState(TradeState.TRADE_WAIT_SCREEN);
            this.tradeDebug("interacting with merchant");
            return;
        }

        if (!this.tradeUseBaritone.getValue()) {
            this.markTradeMerchantVisited(merchant);
            return;
        }

        this.pathTradeTo(merchant.getBlockPos());
    }

    private void handleTradeWaitScreen() {
        if (mc.currentScreen instanceof MerchantScreen && mc.player.currentScreenHandler instanceof MerchantScreenHandler) {
            this.setTradeState(TradeState.TRADE_BUY);
            return;
        }

        if (mc.currentScreen instanceof GenericContainerScreen) {
            this.closeTradeScreen();
        }

        if (this.stateTimer.finished(TRADE_SCREEN_TIMEOUT_MS)) {
            if (this.tradeCurrentMerchant != null) {
                this.markTradeMerchantVisited(this.tradeCurrentMerchant);
            }
            this.tradeCurrentMerchant = null;
            this.setTradeState(TradeState.TRADE_SEARCH);
        }
    }

    private void handleTradeBuy() {
        if (!(mc.currentScreen instanceof MerchantScreen) || !(mc.player.currentScreenHandler instanceof MerchantScreenHandler handler)) {
            if (this.stateTimer.finished(TRADE_SCREEN_TIMEOUT_MS)) {
                this.setTradeState(TradeState.TRADE_SEARCH);
            }
            return;
        }

        if (!this.actionTimer.finished((long)this.tradeActionDelay.getValue())) {
            return;
        }

        if (this.tradeSelectedTradeIndex >= 0) {
            if (this.tradeSelectedTradeIndex >= handler.getRecipes().size()) {
                this.tradeSelectedTradeIndex = -1;
                return;
            }

            TradeOffer selected = handler.getRecipes().get(this.tradeSelectedTradeIndex);
            if (selected.isDisabled() || !this.isTradeTargetStack(selected.getSellItem(), this.tradeTarget)) {
                this.tradeSelectedTradeIndex = -1;
                return;
            }

            ItemStack output = handler.getSlot(2).getStack();
            if (!output.isEmpty() && this.isTradeTargetStack(output, this.tradeTarget)) {
                if (!this.hasRoomForTradeTarget(this.tradeTarget)) {
                    this.closeTradeScreen();
                    this.startTradeDeposit(true);
                    return;
                }

                this.clickTradeSlot(handler.syncId, 2, 0, SlotActionType.QUICK_MOVE);
                this.tradeDebug("bought " + output.getName().getString());
                return;
            }

            if (!this.hasTradePayment(handler, selected)) {
                this.tradeSelectedTradeIndex = -1;
                return;
            }

            if (this.tradeSelectionTimer.finished(1200L)) {
                this.selectMerchantTrade(handler, this.tradeSelectedTradeIndex);
            }
            return;
        }

        if (this.countItem(Items.EMERALD) <= 0 || !this.hasRoomForTradeTarget(this.tradeTarget)) {
            this.closeTradeScreen();
            if (this.countItem(Items.EMERALD) > 0 && this.countTradeTargetItems(this.tradeTarget) > 0) {
                this.startTradeDeposit(true);
            } else {
                this.startTradeDepositOrFinishTarget();
            }
            return;
        }

        int tradeIndex = this.findAvailableTrade(handler, this.tradeTarget);
        if (tradeIndex == -1) {
            this.closeTradeScreen();
            if (this.tradeCurrentMerchant != null) {
                this.markTradeMerchantVisited(this.tradeCurrentMerchant);
            }
            this.tradeCurrentMerchant = null;
            this.setTradeState(TradeState.TRADE_SEARCH);
            return;
        }

        this.selectMerchantTrade(handler, tradeIndex);
    }

    private int findAvailableTrade(MerchantScreenHandler handler, TradeTarget target) {
        for (int i = 0; i < handler.getRecipes().size(); i++) {
            TradeOffer offer = handler.getRecipes().get(i);
            if (!offer.isDisabled() && this.isTradeTargetStack(offer.getSellItem(), target) && this.hasTradePayment(offer)) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasTradePayment(TradeOffer offer) {
        ItemStack first = offer.getDisplayedFirstBuyItem();
        ItemStack second = offer.getDisplayedSecondBuyItem();
        if (!this.hasEnoughForTradePayment(first, second, null)) {
            return false;
        }

        return second.isEmpty() || this.hasEnoughForTradePayment(second, first, null);
    }

    private boolean hasTradePayment(MerchantScreenHandler handler, TradeOffer offer) {
        ItemStack first = offer.getDisplayedFirstBuyItem();
        ItemStack second = offer.getDisplayedSecondBuyItem();
        if (!this.hasEnoughForTradePayment(first, second, handler)) {
            return false;
        }

        return second.isEmpty() || this.hasEnoughForTradePayment(second, first, handler);
    }

    private boolean hasEnoughForTradePayment(ItemStack payment, ItemStack otherPayment, MerchantScreenHandler handler) {
        if (payment == null || payment.isEmpty()) {
            return true;
        }

        int needed = payment.getCount();
        if (otherPayment != null && !otherPayment.isEmpty() && otherPayment.isOf(payment.getItem())) {
            needed += otherPayment.getCount();
        }

        return this.countItem(payment.getItem()) + this.countTradeMerchantInputItem(handler, payment.getItem()) >= needed;
    }

    private int countTradeMerchantInputItem(MerchantScreenHandler handler, Item item) {
        if (handler == null || item == null) {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < 2 && i < handler.slots.size(); i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (stack.isOf(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void selectMerchantTrade(MerchantScreenHandler handler, int index) {
        handler.setRecipeIndex(index);
        handler.switchTo(index);
        mc.getNetworkHandler().sendPacket(new SelectMerchantTradeC2SPacket(index));
        this.tradeSelectedTradeIndex = index;
        this.actionTimer.reset();
        this.tradeSelectionTimer.reset();
        this.tradeDebug("selected merchant trade " + index);
    }

    private void startTradeDepositOrFinishTarget() {
        if (this.countTradeTargetItems(this.tradeTarget) > 0) {
            this.startTradeDeposit(false);
        } else {
            this.finishTradeTarget();
        }
    }

    private void startTradeDeposit(boolean continueTradingAfterDeposit) {
        this.closeTradeScreen();
        this.stopTradePathing();
        this.tradeCurrentStorageChest = null;
        this.tradeContinueAfterDeposit = continueTradingAfterDeposit;
        this.tradeDepositLastCount = this.countTradeTargetItems(this.tradeTarget);
        this.tradeDepositStallAttempts = 0;
        this.setTradeState(TradeState.DEPOSIT_FIND);
    }

    private void handleTradeDepositFind() {
        if (this.countTradeTargetItems(this.tradeTarget) <= 0) {
            this.finishTradeTarget();
            return;
        }

        if (mc.currentScreen != null) {
            this.closeTradeScreen();
            this.actionTimer.reset();
            return;
        }

        String sign = this.tradeTarget == TradeTarget.GOLD ? this.tradeGoldChestSign.getText() : this.tradeMeatChestSign.getText();
        this.tradeCurrentStorageChest = this.findStorageChest(this.tradeChestScanRadius.getIntValue(), sign);
        if (this.tradeCurrentStorageChest == null) {
            this.tradeWarn("Storage chest not found for sign: " + sign);
            if (this.stateTimer.finished(15000L)) this.finishTradeTarget();
            return;
        }

        double maxDistance = this.tradeInteractRange.getValue() * this.tradeInteractRange.getValue();
        if (mc.player.squaredDistanceTo(this.tradeCurrentStorageChest.toCenterPos()) <= maxDistance) {
            this.stopTradePathing();
            this.setTradeState(TradeState.DEPOSIT_OPEN);
        } else if (this.tradeUseBaritone.getValue()) {
            this.pathTradeTo(this.tradeCurrentStorageChest);
        } else {
            this.tradeWarn("Storage chest is out of interact range");
        }
    }

    private void handleTradeDepositOpen() {
        if (this.countTradeTargetItems(this.tradeTarget) <= 0) {
            this.finishTradeTarget();
            return;
        }

        if (mc.currentScreen instanceof GenericContainerScreen && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
            this.setTradeState(TradeState.DEPOSIT_MOVE);
            return;
        }

        if (this.tradeCurrentStorageChest == null) {
            this.setTradeState(TradeState.DEPOSIT_FIND);
            return;
        }

        if (this.actionTimer.finished((long)this.tradeActionDelay.getValue())) {
            this.openContainer(this.tradeCurrentStorageChest, (long)this.tradeActionDelay.getValue());
        }
    }

    private void handleTradeDepositMove() {
        if (this.countTradeTargetItems(this.tradeTarget) <= 0) {
            this.closeTradeScreen();
            this.finishTradeDeposit();
            return;
        }

        GenericContainerScreenHandler handler = this.tradeContainerHandler();
        if (handler == null) {
            this.setTradeState(TradeState.DEPOSIT_FIND);
            return;
        }

        if (!this.actionTimer.finished((long)this.tradeActionDelay.getValue())) {
            return;
        }

        int currentCount = this.countTradeTargetItems(this.tradeTarget);
        if (currentCount < this.tradeDepositLastCount) {
            this.tradeDepositStallAttempts = 0;
        } else {
            this.tradeDepositStallAttempts++;
        }
        this.tradeDepositLastCount = currentCount;
        if (this.tradeDepositStallAttempts >= 5) {
            this.tradeWarn("Storage chest is full or rejected the items.");
            this.closeTradeScreen();
            this.finishTradeTarget();
            return;
        }

        int playerStart = handler.getRows() * 9;
        for (int i = playerStart; i < handler.slots.size(); i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (this.isTradeTargetStack(stack, this.tradeTarget)) {
                this.clickTradeSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE);
                this.tradeDebug("deposited " + stack.getName().getString());
                return;
            }
        }

        this.closeTradeScreen();
        this.finishTradeDeposit();
    }

    private void finishTradeDeposit() {
        if (this.tradeContinueAfterDeposit) {
            this.tradeContinueAfterDeposit = false;
            this.tradeCurrentStorageChest = null;
            this.startTradeBuyerOrTrade();
        } else {
            this.finishTradeTarget();
        }
    }

    private void finishTradeTarget() {
        this.tradeCurrentMerchant = null;
        this.tradeCurrentStorageChest = null;
        this.tradeSelectedTradeIndex = -1;

        if (this.tradeMode.is("Both") && this.tradeTarget == TradeTarget.GOLD) {
            this.startTradeTarget(TradeTarget.MEAT);
        } else {
            this.setTradeState(TradeState.WAIT_NEXT_CYCLE);
        }
    }

    private void handleTradeWaitNextCycle() {
        if (this.actionTimer.finished((long)this.tradeCyclePause.getValue())) {
            this.startTradeCycle();
        }
    }

    private MerchantEntity findNearestTradeMerchant() {
        double radius = this.tradeScanRadius.getValue();
        double radiusSq = radius * radius;
        return mc.world.getEntitiesByClass(
                        MerchantEntity.class,
                        mc.player.getBoundingBox().expand(radius),
                        merchant -> merchant.isAlive()
                                && !this.isTradeMerchantVisited(merchant)
                                && mc.player.squaredDistanceTo(merchant) <= radiusSq
                )
                .stream()
                .min(Comparator.comparingDouble(merchant -> mc.player.squaredDistanceTo(merchant)))
                .orElse(null);
    }

    private boolean isTradeMerchantVisited(MerchantEntity merchant) {
        return this.tradeVisitedSet().contains(merchant.getUuid());
    }

    private void markTradeMerchantVisited(MerchantEntity merchant) {
        this.tradeVisitedSet().add(merchant.getUuid());
        this.tradeDebug("visited merchant " + merchant.getUuid());
    }

    private Set<UUID> tradeVisitedSet() {
        return this.tradeTarget == TradeTarget.GOLD ? this.tradeVisitedGoldVillagers : this.tradeVisitedMeatVillagers;
    }

    private boolean hasRoomForTradeTarget(TradeTarget target) {
        if (mc.player.getInventory().getEmptySlot() != -1) {
            return true;
        }

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (this.isTradeTargetStack(stack, target) && stack.getCount() < stack.getMaxCount()) {
                return true;
            }
        }

        return false;
    }

    private int countTradeTargetItems(TradeTarget target) {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (this.isTradeTargetStack(stack, target)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private boolean isTradeTargetStack(ItemStack stack, TradeTarget target) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        return target == TradeTarget.GOLD ? stack.isOf(Items.GOLD_INGOT) : TRADE_MEAT_ITEMS.contains(stack.getItem());
    }

    private int getTradeEmeraldBuyAmount() {
        return this.getTradeEmeraldStackCount() * 64;
    }

    private int getTradeEmeraldStackCount() {
        try {
            int stacks = Integer.parseInt(this.tradeEmeraldStacks.getText().trim());
            return Math.max(1, Math.min(36, stacks));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private GenericContainerScreenHandler tradeContainerHandler() {
        if (mc.currentScreen instanceof GenericContainerScreen && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler) {
            return handler;
        }
        return null;
    }

    private int findTradeContainerSlot(GenericContainerScreenHandler handler, Predicate<ItemStack> predicate) {
        int containerSlots = handler.getRows() * 9;
        for (int i = 0; i < containerSlots; i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (!stack.isEmpty() && predicate.test(stack)) {
                return i;
            }
        }
        return -1;
    }

    private int findTradeShopChestSlot(GenericContainerScreenHandler handler) {
        return this.findTradeContainerSlot(
                handler,
                stack -> stack.isOf(Items.CHEST) || stack.isOf(Items.TRAPPED_CHEST) || stack.isOf(Items.BARREL)
        );
    }

    private void clickTradeSlot(int syncId, int slot, int button, SlotActionType type) {
        mc.interactionManager.clickSlot(syncId, slot, button, type, mc.player);
        this.actionTimer.reset();
    }

    private void closeTradeScreen() {
        if (mc.player != null && mc.currentScreen != null) {
            mc.player.closeHandledScreen();
        }
    }

    private void sendTradeChatCommand(String command) {
        if (command == null || command.isBlank() || mc.getNetworkHandler() == null) {
            return;
        }

        String trimmed = command.trim();
        mc.getNetworkHandler().sendChatCommand(trimmed.startsWith("/") ? trimmed.substring(1) : trimmed);
        this.commandTimer.reset();
    }

    private void pathTradeTo(BlockPos pos) {
        if (!this.commandTimer.finished((long)this.tradeCommandDelay.getValue())) {
            return;
        }

        if (!BaritoneHelper.pathTo(pos)) {
            this.tradeWarn("Baritone path failed.");
        }
        this.commandTimer.reset();
    }

    private void stopTradePathing() {
        this.stopTradePathing(false);
    }

    private void stopTradePathing(boolean force) {
        if (this.tradeUseBaritone.getValue() && (force || this.tradeStopPathingTimer.finished(1000L))) {
            BaritoneHelper.stop();
            this.tradeStopPathingTimer.reset();
        }
    }

    private void lookAtTradeTarget(Vec3d target) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d delta = target.subtract(eye);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        mc.player.setYaw((float)(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0));
        mc.player.setPitch((float)(-Math.toDegrees(Math.atan2(delta.y, horizontal))));
    }



}

