package fun.ogi.module.impl.list.misc;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.pathing.goals.GoalBlock;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.eventbus.Subscribe;
import fun.ogi.events.EventMouse;
import fun.ogi.events.render.EventHud;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.module.settings.StringSetting;
import fun.ogi.module.theme.ThemeManager;
import fun.ogi.util.time.Timer;
import fun.ogi.util.animation.Animation;
import fun.ogi.util.animation.Easing;
import fun.ogi.util.render.Draggable;
import fun.ogi.util.render.builders.Builder;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.msdf.MsdfFont;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.block.entity.SignText;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registries;import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.screen.slot.SlotActionType;

import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleInformation(moduleName = "AutoWarden", moduleDesc = "Automatically loots warden chests", moduleCategory = ModuleCategory.PLAYER)
public class AutoWarden extends Module {

    private static final String CLAN_HOME_PREFIX = "clan home ";

    private final StringSetting warehouse = new StringSetting("Хом стежа", this, "").visible(() -> false);
    private final StringSetting warehouseHome = new StringSetting("Storage home name", this, "st");
    private final StringSetting lootHome = new StringSetting("Warden name", this, "warden");
    private final BooleanSetting autoLoot = new BooleanSetting("Auto loot", this, true);
    private final BooleanSetting autoDeposit = new BooleanSetting("Auto deposit", this, true);
    private final BooleanSetting autoSupplies = new BooleanSetting("Auto supplies", this, true);
    private final StringSetting supplySignKeywords = new StringSetting("Sign keywoards", this, "Invisible,Food").visible(autoSupplies::getValue);
    private final SliderSetting openRetryDelay = new SliderSetting("Open delay", this, 150, 50, 1500, 50);
    private final SliderSetting rejoinLead = new SliderSetting("Rejoin delay (milisec)", this, 3000, 500, 15000, 100);
    private final StringSetting exitThreshold = new StringSetting("Exit treshold", this, "60");

    private final Timer stateTimer = new Timer();
    private final Timer foodUseTimer = new Timer();
    private final Timer invisibilityUseTimer = new Timer();
    private final Map<BlockPos, Long> ignoredChests = new ConcurrentHashMap<>();

    private final Draggable wardenDrag = new Draggable(5, 70, 120, 20);
    private final Animation wardenAnimation = new Animation();
    private int deathCount = 0;
    private long sessionStartedAt = -1L;
    private boolean wasDead = false;
    private static final Supplier<MsdfFont> BIKO_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
    private static final Supplier<MsdfFont> ICON_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("nur").data("nur").build());

    private static final int SUPPLY_SEARCH_RADIUS = 32;
    private static final int MIN_FOOD_ITEMS = 8;
    private static final long SUPPLY_RETRY_DELAY_MS = 15000L;
    private static final long SUPPLY_TIMEOUT_MS = 45000L;
    private static final long SCAN_FIND_TIMEOUT_MS = 750L;
    private static final long CHEST_OPEN_TIMEOUT_MS = 20000L;
    private static final long LOOT_EMPTY_CHECK_DELAY_MS = 500L;
    private static final long TIMED_CHEST_LOOT_GRACE_MS = 3000L;
    private static final long BARITONE_NO_PATH_RESET_MS = 4000L;
    private static final int LOOT_EMPTY_REQUIRED_CHECKS = 2;
    private static final long HOME_TELEPORT_WAIT_MS = 6000L;
    private static final long ARENA_RETURN_LEAD_MS = 9000L;
    private static final long LAST_KNOWN_CHEST_FALLBACK_MS = 300000L;
    private static final double CHEST_OPEN_RANGE_SQ = 9.0;
    private static final double HOLOGRAM_READ_RADIUS_SQ = 4.0;
    private static final String[] DEFAULT_SUPPLY_SIGN_KEYWORDS = {"инвиз", "невид", "еда", "invis", "food"};
    private static final String[] LOOT_ITEMS = {
        "Totem", "Netherite Helmet", "Netherite Chestplate", "Netherite Leggings",
        "Netherite Boots", "Netherite Sword", "Netherite Pickaxe", "Enchanted Golden Apple",
        "Player Head", "ENDER_EYE", "Shulker Box", "Netherite Ingot", "Dragon Head",
        "Elytra", "Snowball", "Splash Potion", "Tripwire Hook", "Netherite Scrap",
        "Beacon", "Villager Spawn Egg", "Paper", "FIREWORK_ROCKET", "PHANTOM_MEMBRANE",
        "Diamond", "TOTEM_OF_UNDYING", "Golden Apple", "Golden Carrot", "tnt"
    };
    private static final String[] STRICT_LOOT_ITEM_IDS = {
        "totem of undying", "netherite helmet", "netherite chestplate", "netherite leggings",
        "netherite boots", "netherite sword", "netherite pickaxe", "enchanted golden apple",
        "shulker box", "netherite ingot", "dragon head", "elytra", "snowball", "splash potion",
        "phantom_membrane", "tripwire hook", "netherite scrap", "beacon", "ender_eye",
        "villager spawn egg", "paper", "firework rocket", "phantom membrane", "diamond",
        "golden apple", "golden carrot", "tnt"
    };

    private static final Pattern TIME_PATTERN = Pattern.compile("(?<!\\d)(\\d{1,2})\\s*[:\uFF1A]\\s*(\\d{1,2})(?:\\s*[:\uFF1A]\\s*(\\d{1,2}))?(?!\\d)");
    private static final Pattern SECONDS_PATTERN = Pattern.compile("(?<![:\\d])(\\d{1,3})\\s*(?:секунд(?:а|ы)?|сек\\.?|sec(?:\\.|ond)?s?|seconds?|с\\.?|s\\.?|c\\.?)");
    private static final Pattern MIN_SEC_PATTERN = Pattern.compile("(?<!\\d)(\\d{1,3})\\s*(?:мин(?:\\.|ут(?:а|ы)?)?|м\\.?|min(?:\\.|ute)?s?|m\\.?)\\s*(?:(\\d{1,2})\\s*(?:секунд(?:а|ы)?|сек\\.?|sec(?:\\.|ond)?s?|seconds?|с\\.?|s\\.?|c\\.?))?");

    private State state = State.IDLE;
    private int targetAnarchy = -1;
    private BlockPos targetChest;
    private BlockPos lastKnownLootChest;
    private long lastKnownLootChestAt = -1L;
    private long targetOpenTime = -1L;
    private long targetFoundOpenTime = -1L;
    private long scannedChestOpenTime = -1L;
    private int scanIndex = 0;
    private int depositSlotIndex = 0;
    private boolean lootedCurrentChest = false;
    private boolean openedCurrentChest = false;
    private boolean aimedCurrentChest = false;
    private boolean checkingUntimedChest = false;
    private boolean openingTimedChestImmediately = false;
    private boolean pendingDeposit = false;
    private boolean warehouseHomeCommandSent = false;
    private boolean pendingClanStorageWithdraw = false;
    private boolean farmHomeCommandSent = false;
    private boolean supplyHomeCommandSent = false;
    private boolean returnCommandSent = false;
    private long lootContainerOpenedAt = -1L;
    private int emptyLootChecks = 0;
    private boolean pausedByTeleportBossBar = false;
    private long baritoneNoPathSince = -1L;
    private boolean pausedByHomeTeleport = false;
    private long homeTeleportPauseUntil = 0L;
    private State openTimeoutState = null;
    private long openTimeoutStartedAt = -1L;
    private BlockPos lastOpenAttemptChest;
    private long lastOpenAttemptAt = -1L;
    private BlockPos pendingOpenedChest;
    private long openAttemptStartedAt = -1L;
    private int chestOpenRecoveries = 0;
    private BlockPos supplyChest;
    private State supplyReturnState = State.RUSH_JOIN;
    private long supplyStartedAt = -1L;
    private long supplyRetryAfter = 0L;
    private boolean aimedSupplyChest = false;
    private boolean initialSupplyPending = false;
    private boolean forceSupplyPending = false;
    private boolean supplyTookInvisibility = false;
    private boolean supplyTookFood = false;
    private boolean autoEatingFood = false;
    private int previousFoodSlot = -1;
    private int foodHotbarSlot = -1;
    private int foodUseDelayTicks = 0;
    private boolean autoDrinkingInvisibility = false;
    private int previousInvisibilitySlot = -1;
    private int invisibilityHotbarSlot = -1;
    private int invisibilityUseDelayTicks = 0;
    private Boolean previousBaritoneFreeLook;
    private Boolean previousBaritoneRightClickContainerOnArrival;

    public AutoWarden() {
        addSettings(warehouseHome, lootHome, autoLoot, autoDeposit, autoSupplies, supplySignKeywords, openRetryDelay, rejoinLead, exitThreshold);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.state = State.IDLE;
        this.stateTimer.reset();
        this.initialSupplyPending = this.autoSupplies.getValue();
        this.supplyRetryAfter = 0L;
        this.sessionStartedAt = System.currentTimeMillis();
        this.deathCount = 0;
        this.wasDead = false;
        this.wardenAnimation.setValue(0f);
        this.configureBaritone();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.cleanupAll();
        IBaritone baritone = this.getBaritone();
        if (baritone != null) {
            baritone.getPathingBehavior().cancelEverything();
        }
        this.restoreBaritoneSettings();
    }

    @Subscribe
    public void onTick(EventUpdate event) {
        try {
            this.processTick();
        } catch (Throwable t) {
            this.handleTickError();
        }
    }

    @Subscribe
    public void onRender(EventHud event) {
        if (mc.player == null) return;
        if (mc.currentScreen == null && !this.wardenAnimation.isRunning() && this.wardenAnimation.getValue() <= 0.01f) return;

        String statusText = switch (this.state) {
            case IDLE -> "Idle";
            case SCAN_NEXT, SCAN_WAIT_JOIN, SCAN_FIND_HOLOGRAM -> "Scanning...";
            case SCAN_PATHING -> "Pathing to scan...";
            case SCAN_READ_HOLOGRAM -> "Reading hologram...";
            case HUB_WAITING -> "Waiting in hub...";
            case ARENA_SET_HOME -> "Setting arena home...";
            case ARENA_OPEN -> "Opening arena chest...";
            case ARENA_WAIT_RETURN, ARENA_RETURN, ARENA_RETURN_WAIT -> "Returning from arena...";
            case RUSH_JOIN, RUSH_PATH -> "Rushing to chest...";
            case WAIT_OPEN -> "Waiting to open...";
            case LOOTING -> "Looting chest...";
            case SUPPLY_WAIT_JOIN -> "Going for supplies...";
            case SUPPLY_PATHING -> this.supplyChest == null ? "Finding supply chest..." : "Pathing to supplies...";
            case SUPPLY_OPEN, SUPPLY_TAKE -> "Taking supplies...";
            case CLAN_STORAGE_OPEN, CLAN_STORAGE_DEPOSIT -> "Depositing to clan storage...";
            case CLAN_STORAGE_WITHDRAW_OPEN, CLAN_STORAGE_WITHDRAW -> "Withdrawing from clan storage...";
            case GO_WAREHOUSE -> "Going to warehouse...";
            case RETURN_FARM_HOME -> "Returning home...";
            case WAREHOUSE_WAIT_JOIN, WAREHOUSE_FIND_CHEST, WAREHOUSE_PATHING, WAREHOUSE_OPEN -> "At warehouse...";
            case DEPOSITING -> "Depositing items...";
        };

        String deathsText = "" + this.deathCount;
        String uptimeText = "" + this.formatSessionTime();

        Matrix4f matrix = event.getDrawContext().getMatrices().peek().getPositionMatrix();
        Color accentColor = new Color(ThemeManager.getInstance().getPrimary());
        Color textColor = Color.WHITE;
        Color secondaryColor = new Color(ThemeManager.getInstance().getPalette().getTextSecondary());
        float ts = 9f;
        float iconSz = 8f;
        float headerH = 16f;
        float itemH = 16f;
        float gap = 2f;
        float rounding = 3f;
        float pad = 5f;

        float minW = BIKO_FONT.get().getWidth("AutoWarden", ts) + 40f;
        float statusW = BIKO_FONT.get().getWidth(statusText, ts);
        float deathsW = BIKO_FONT.get().getWidth(deathsText, 8f);
        float uptimeW = BIKO_FONT.get().getWidth(uptimeText, 8f);
        float maxValueW = Math.max(deathsW, uptimeW);
        float contentW = statusW + pad + maxValueW + pad;
        float totalW = Math.max(minW, contentW);
        float totalH = headerH + gap + itemH + gap + itemH + gap + 4;

        this.wardenDrag.setWidth(totalW);
        this.wardenDrag.setHeight(totalH);

        boolean shouldShow = mc.currentScreen != null || this.wardenAnimation.getValue() > 0.01f;
        if (shouldShow) {
            this.wardenAnimation.update();
            this.wardenAnimation.start(this.wardenAnimation.getValue(), 1f, 300, Easing.QUART_OUT);
        } else {
            this.wardenAnimation.update();
            this.wardenAnimation.start(this.wardenAnimation.getValue(), 0f, 200, Easing.QUART_OUT);
            if (this.wardenAnimation.getValue() <= 0.01f) return;
        }
        float sizeAnim = this.wardenAnimation.getValue();

        float x = this.wardenDrag.getX();
        float y = this.wardenDrag.getY();
        float cx = x + totalW / 2f;
        float cy = y + totalH / 2f;
        event.getDrawContext().getMatrices().push();
        event.getDrawContext().getMatrices().translate(cx, cy, 0);
        event.getDrawContext().getMatrices().scale(sizeAnim, sizeAnim, 1);
        event.getDrawContext().getMatrices().translate(-cx, -cy, 0);
        matrix = event.getDrawContext().getMatrices().peek().getPositionMatrix();

        Builder.rectangle().size(new SizeState(totalW, totalH)).radius(new QuadRadiusState(rounding))
                .color(new QuadColorState(new Color(20, 20, 20, 255))).build().render(matrix, x, y);

        float iconCharW = ICON_FONT.get().getWidth("V", iconSz);
        Builder.text().text("V").font(ICON_FONT.get()).size(iconSz).thickness(0.08f).color(accentColor)
                .build().render(matrix, x + pad, y + (headerH - iconSz) / 2f);
        float titleX = x + pad + iconCharW + 3f;
        Builder.text().text("AutoWarden").font(BIKO_FONT.get()).size(ts).thickness(0.06f).color(textColor)
                .build().render(matrix, titleX, y + (headerH - ts) / 2f);

        float curY = y + headerH + gap;

        Builder.text().text(statusText).font(BIKO_FONT.get()).size(ts).thickness(0.06f).color(textColor)
                .build().render(matrix, x + pad, curY + (itemH - ts) / 2f);
        float statusValW = BIKO_FONT.get().getWidth(statusText, 8f);
        curY += itemH + gap;

        Builder.text().text(deathsText).font(BIKO_FONT.get()).size(ts).thickness(0.06f).color(textColor)
                .build().render(matrix, x + pad, curY + (itemH - ts) / 2f);
        float deathsX = x + totalW - pad - deathsW;
        Builder.text().text(deathsText).font(BIKO_FONT.get()).size(8f).thickness(0.06f).color(secondaryColor)
                .build().render(matrix, deathsX, curY + (itemH - 8f) / 2f);
        curY += itemH + gap;

        Builder.text().text(uptimeText).font(BIKO_FONT.get()).size(ts).thickness(0.06f).color(textColor)
                .build().render(matrix, x + pad, curY + (itemH - ts) / 2f);
        float uptimeX = x + totalW - pad - uptimeW;
        Builder.text().text(uptimeText).font(BIKO_FONT.get()).size(8f).thickness(0.06f).color(secondaryColor)
                .build().render(matrix, uptimeX, curY + (itemH - 8f) / 2f);

        event.getDrawContext().getMatrices().pop();
    }

    @Subscribe
    public void onMouse(EventMouse event) {
        if (mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) {
            if (event.getAction() == 1) {
                if (this.wardenDrag.onClick((int) event.getMouseX(), (int) event.getMouseY(), event.getButton())) return;
            } else if (event.getAction() == 0) {
                this.wardenDrag.onRelease(event.getButton());
            }
        }
    }

    private void processTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        boolean isDead = mc.player.isDead();
        if (isDead && !this.wasDead) {
            this.deathCount++;
        }
        this.wasDead = isDead;
        if (isDead) return;

        IBaritone baritone = this.getBaritone();

        if (this.isInHub()) {
            this.sendHomeCommand();
            return;
        }

        if (this.pausedByHomeTeleport) {
            if (System.currentTimeMillis() < this.homeTeleportPauseUntil) return;
            this.pausedByHomeTeleport = false;
            this.stateTimer.reset();
        }

        this.handleBaritonePathingTimeout(baritone);
        this.clearExpiredIgnoredChests();

        if (this.shouldHandleSupplies()) {
            if (baritone != null) baritone.getPathingBehavior().cancelEverything();
            this.disableAutoJump();
            if (this.goToSupply()) {
                this.stateTimer.reset();
                return;
            }
        }

        switch (this.state) {
            case IDLE -> {
                if (this.initialSupplyPending || this.forceSupplyPending) {
                    if (!this.autoSupplies.getValue()) {
                        this.initialSupplyPending = false;
                        this.forceSupplyPending = false;
                    } else {
                        if (this.goToSupply()) return;
                        if (this.forceSupplyPending) { this.stateTimer.reset(); return; }
                        this.initialSupplyPending = false;
                    }
                }
                this.scanIndex = 0;
                this.state = this.isWaitingForTimedChest() ? State.HUB_WAITING : State.SCAN_NEXT;
            }
            case SCAN_NEXT -> { this.stateTimer.reset(); this.state = State.SCAN_FIND_HOLOGRAM; }
            case SCAN_WAIT_JOIN -> { this.stateTimer.reset(); this.state = State.SCAN_FIND_HOLOGRAM; }
            case SCAN_FIND_HOLOGRAM -> this.handleScanFindHologram(baritone);
            case SCAN_PATHING -> this.handleScanPathing(baritone);
            case SCAN_READ_HOLOGRAM -> this.handleScanReadHologram(baritone);
            case HUB_WAITING -> this.handleHubWaiting();
            case ARENA_SET_HOME -> this.handleArenaSetHome();
            case ARENA_OPEN -> this.handleArenaOpen();
            case ARENA_WAIT_RETURN -> this.handleArenaWaitReturn();
            case ARENA_RETURN -> this.handleArenaReturn();
            case ARENA_RETURN_WAIT -> this.handleArenaReturnWait();
            case RUSH_JOIN -> this.handleRushJoin(baritone);
            case RUSH_PATH -> this.handleRushPath(baritone);
            case WAIT_OPEN -> this.handleWaitOpen(baritone);
            case LOOTING -> this.handleLooting(baritone);
            case SUPPLY_WAIT_JOIN -> this.handleSupplyWaitJoin();
            case SUPPLY_PATHING -> this.handleSupplyPathing(baritone);
            case SUPPLY_OPEN -> this.handleSupplyOpen(baritone);
            case SUPPLY_TAKE -> this.handleSupplyTake();
            case CLAN_STORAGE_OPEN -> this.handleClanStorageOpen();
            case CLAN_STORAGE_DEPOSIT -> this.handleClanStorageDeposit();
            case CLAN_STORAGE_WITHDRAW_OPEN -> this.handleClanStorageWithdrawOpen();
            case CLAN_STORAGE_WITHDRAW -> this.handleClanStorageWithdraw();
            case GO_WAREHOUSE -> this.handleGoWarehouse();
            case RETURN_FARM_HOME -> this.handleReturnFarmHome();
            case WAREHOUSE_WAIT_JOIN -> this.state = State.WAREHOUSE_OPEN;
            case WAREHOUSE_FIND_CHEST -> this.handleWarehouseFindChest(baritone);
            case WAREHOUSE_PATHING -> this.state = State.WAREHOUSE_OPEN;
            case WAREHOUSE_OPEN -> this.handleWarehouseOpen(baritone);
            case DEPOSITING -> this.handleDepositing();
        }
    }

    private void handleScanFindHologram(IBaritone baritone) {
        BlockPos nearestHolo = this.findNearestHologram();
        long scannedOpenTime = this.scannedChestOpenTime;
        BlockPos timedChest = this.findTimedChest();
        BlockPos nearestChest = this.findNearestChestNearby();
        BlockPos bestChest = this.pickBestChest(nearestChest, this.pickBestChest(timedChest, nearestHolo));
        boolean hasTimedChest = nearestHolo != null && nearestHolo.equals(bestChest);

        if (bestChest != null) {
            this.targetChest = bestChest;
            this.rememberChest(bestChest);
            this.targetFoundOpenTime = hasTimedChest ? scannedOpenTime : -1L;
            this.lootedCurrentChest = false;
            this.openedCurrentChest = false;
            this.aimedCurrentChest = false;
            this.checkingUntimedChest = !hasTimedChest;
            this.chestOpenRecoveries = 0;
            if (baritone != null) baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(this.targetChest.getX(), this.targetChest.getY(), this.targetChest.getZ()));
            this.stateTimer.reset();
            this.state = State.SCAN_PATHING;
            return;
        }

        if (this.stateTimer.finished(SCAN_FIND_TIMEOUT_MS)) {
            BlockPos lastKnown = this.getLastKnownChest();
            if (lastKnown != null) {
                this.targetChest = lastKnown;
                this.targetFoundOpenTime = -1L;
                this.lootedCurrentChest = false;
                this.openedCurrentChest = false;
                this.aimedCurrentChest = false;
                this.checkingUntimedChest = true;
                this.chestOpenRecoveries = 0;
                if (baritone != null) baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(this.targetChest.getX(), this.targetChest.getY(), this.targetChest.getZ()));
                this.state = State.SCAN_PATHING;
                this.stateTimer.reset();
                return;
            }
            ++this.scanIndex;
            this.state = State.SCAN_NEXT;
            this.stateTimer.reset();
        }
    }

    private void handleScanPathing(IBaritone baritone) {
        if (this.targetChest == null) { this.state = State.SCAN_FIND_HOLOGRAM; return; }
        double distance = mc.player.getPos().distanceTo(Vec3d.ofCenter(this.targetChest));
        if (this.isNearChest(this.targetChest) || (distance <= 4.0 && (baritone == null || !baritone.getPathingBehavior().isPathing()))) {
            if (baritone != null) baritone.getPathingBehavior().cancelEverything();
            this.stateTimer.reset();
            this.state = State.SCAN_READ_HOLOGRAM;
        } else if (baritone != null && !baritone.getPathingBehavior().isPathing() && this.stateTimer.finished(2000)) {
            baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(this.targetChest.getX(), this.targetChest.getY(), this.targetChest.getZ()));
            this.stateTimer.reset();
        }
    }

    private void handleScanReadHologram(IBaritone baritone) {
        if (this.targetChest == null) { this.state = State.SCAN_FIND_HOLOGRAM; return; }

        long timeLeft = this.readChestTime(this.targetChest);
        long timedOpenAt = -1L;
        long timedOpenTime = -1L;

        if (this.targetFoundOpenTime > System.currentTimeMillis()) {
            timedOpenAt = Math.max(0L, (this.targetFoundOpenTime - System.currentTimeMillis() + 999L) / 1000L);
        }
        if (timeLeft >= 0L) timedOpenTime = this.calculateOpenTime(timeLeft);
        if (timedOpenAt >= 0L && (timeLeft < 0L || timedOpenAt < timeLeft)) { timeLeft = timedOpenAt; timedOpenTime = this.targetFoundOpenTime; }
        if (timeLeft < 0L && this.targetFoundOpenTime != -1L) { timeLeft = timedOpenAt; timedOpenTime = this.targetFoundOpenTime; }

        if (timeLeft >= 0L && timedOpenTime != -1L) {
            long timeUntilOpen = timedOpenTime - System.currentTimeMillis();
            this.targetAnarchy = 1;
            this.returnCommandSent = false;
            this.lootedCurrentChest = false;
            this.openedCurrentChest = false;
            this.aimedCurrentChest = false;
            if (timeUntilOpen > this.getMaxWaitTime()) {
                this.targetOpenTime = -1L;
                this.checkingUntimedChest = true;
                this.openingTimedChestImmediately = false;
                this.state = State.WAIT_OPEN;
                this.stateTimer.reset();
                return;
            }
            this.targetOpenTime = timedOpenTime;
            this.checkingUntimedChest = false;
            this.openingTimedChestImmediately = false;
            this.state = this.shouldSetArenaHome(timeUntilOpen) ? State.ARENA_SET_HOME : State.WAIT_OPEN;
            this.stateTimer.reset();
        } else if (timeLeft < 0L) {
            this.targetOpenTime = -1L;
            this.checkingUntimedChest = true;
            this.lootedCurrentChest = false;
            this.openedCurrentChest = false;
            this.aimedCurrentChest = false;
            this.state = State.WAIT_OPEN;
            this.stateTimer.reset();
        } else if (this.stateTimer.finished(4000)) {
            this.ignoredChests.put(this.targetChest, System.currentTimeMillis() + LAST_KNOWN_CHEST_FALLBACK_MS);
            this.targetChest = null;
            this.checkingUntimedChest = false;
            this.state = State.SCAN_FIND_HOLOGRAM;
            this.stateTimer.reset();
        }
    }

    private void handleHubWaiting() {
        if (this.targetAnarchy == -1 || this.targetOpenTime == -1L) return;
        long timeLeft = this.targetOpenTime - System.currentTimeMillis();
        if (timeLeft < -10000L) {
            this.resetChestState();
            this.state = State.SCAN_NEXT;
            this.stateTimer.reset();
            return;
        }
        if (timeLeft <= this.getRejoinLead()) {
            this.state = State.RUSH_JOIN;
            this.stateTimer.reset();
        }
    }

    private void handleArenaSetHome() {
        if (this.targetChest == null || this.targetOpenTime == -1L) {
            this.resetChestState();
            this.state = State.SCAN_NEXT;
            this.stateTimer.reset();
            return;
        }
        if (!this.isNearChest(this.targetChest)) {
            this.state = State.RUSH_PATH;
            this.stateTimer.reset();
            return;
        }
        if (mc.world != null) mc.player.stopRiding();
        this.disableAutoJump();
        this.sendCommand(this.getWarehouseHomeCommand());
        this.state = State.ARENA_OPEN;
        this.stateTimer.reset();
    }

    private void handleArenaOpen() {
        if (this.targetOpenTime == -1L) {
            this.resetChestState();
            this.state = State.SCAN_NEXT;
            this.stateTimer.reset();
            return;
        }
        if (this.targetOpenTime - System.currentTimeMillis() <= ARENA_RETURN_LEAD_MS) {
            this.state = State.ARENA_RETURN;
            this.stateTimer.reset();
            return;
        }
        if (this.isInventoryOpen()) {
            if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler) {
                if (this.isContainerOpen() && this.tryTakeFromContainer(handler)) {
                    this.state = State.ARENA_WAIT_RETURN;
                    this.stateTimer.reset();
                }
            }
        } else if (this.stateTimer.finished(800)) {
            this.sendCommand("darena");
            this.stateTimer.reset();
        }
    }

    private void handleArenaWaitReturn() {
        if (this.targetOpenTime == -1L) {
            this.resetChestState();
            this.state = State.SCAN_NEXT;
            this.stateTimer.reset();
            return;
        }
        if (mc.world != null) mc.player.stopRiding();
        if (this.targetOpenTime - System.currentTimeMillis() <= ARENA_RETURN_LEAD_MS) {
            this.state = State.ARENA_RETURN;
            this.stateTimer.reset();
        }
    }

    private void handleArenaReturn() {
        if (mc.world != null) mc.player.stopRiding();
        this.sendCommand(this.getWarehouseHomeCommand());
        this.state = State.ARENA_RETURN_WAIT;
        this.stateTimer.reset();
    }

    private void handleArenaReturnWait() {
        if (!this.stateTimer.finished(6000)) return;
        this.aimedCurrentChest = false;
        this.state = this.isNearChest(this.targetChest) ? State.WAIT_OPEN : State.RUSH_PATH;
        this.stateTimer.reset();
    }

    private void handleRushJoin(IBaritone baritone) {
        if (this.isNearChest(this.targetChest)) {
            this.aimedCurrentChest = false;
            this.state = State.WAIT_OPEN;
            this.stateTimer.reset();
        } else {
            if (this.targetChest != null && baritone != null) {
                baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(this.targetChest.getX(), this.targetChest.getY(), this.targetChest.getZ()));
            }
            this.state = State.RUSH_PATH;
        }
    }

    private void handleRushPath(IBaritone baritone) {
        if (this.targetChest == null) { this.state = State.SCAN_FIND_HOLOGRAM; return; }
        if (this.isNearChest(this.targetChest)) {
            if (baritone != null) baritone.getPathingBehavior().cancelEverything();
            this.aimedCurrentChest = false;
            this.state = State.WAIT_OPEN;
            this.stateTimer.reset();
        } else if (baritone != null && !baritone.getPathingBehavior().isPathing() && this.stateTimer.finished(2000)) {
            baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(this.targetChest.getX(), this.targetChest.getY(), this.targetChest.getZ()));
            this.stateTimer.reset();
        }
    }

    private void handleWaitOpen(IBaritone baritone) {
        if (baritone != null) baritone.getPathingBehavior().cancelEverything();
        BlockPos currentChest = this.validateTargetChest();
        if (currentChest == null) {
            this.state = State.RUSH_PATH;
            this.stateTimer.reset();
            return;
        }
        if (!this.isNearChest(currentChest)) {
            this.state = State.RUSH_PATH;
            this.stateTimer.reset();
            return;
        }
        if (this.isChestConfirmedOpen(currentChest)) {
            this.resetOpenAttempts();
            this.rememberChest(currentChest);
            this.openedCurrentChest = true;
            this.aimedCurrentChest = false;
            this.chestOpenRecoveries = 0;
            this.lootContainerOpenedAt = System.currentTimeMillis();
            this.emptyLootChecks = 0;
            this.state = State.LOOTING;
            this.stateTimer.reset();
            return;
        }
        if (this.isInventoryOpen()) {
            this.resetOpenAttempts();
            this.closeScreen();
            this.aimedCurrentChest = false;
            this.state = State.RUSH_PATH;
            this.stateTimer.reset();
            return;
        }
        long timeLeft = this.targetOpenTime > 0L ? this.targetOpenTime - System.currentTimeMillis() : 0L;
        if (this.targetOpenTime > 0L && timeLeft > 0L && !this.openingTimedChestImmediately) {
            this.resetOpenAttempts();
            this.closeScreen();
            return;
        }
        if (this.targetOpenTime > 0L && timeLeft < -10000L && !this.openingTimedChestImmediately) {
            this.closeScreen();
            this.resetChestState();
            this.state = State.SCAN_NEXT;
            this.stateTimer.reset();
            return;
        }
        if (this.isTimedOut(State.WAIT_OPEN)) {
            this.retryChestOpen(State.WAIT_OPEN);
            return;
        }
        if (this.aimedCurrentChest && !this.isChestStillThere(currentChest)) return;
        if (!this.attemptChestOpen(currentChest, true)) return;
        this.aimedCurrentChest = true;
        this.stateTimer.reset();
    }

    private void handleLooting(IBaritone baritone) {
        if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler) {
            if (this.stateTimer.finished(60)) {
                this.lootChest(handler);
                this.stateTimer.reset();
            }
            return;
        }
        if (!this.lootedCurrentChest) {
            if (this.isTimedChestLootGrace()) {
                this.openedCurrentChest = false;
                this.aimedCurrentChest = false;
                this.state = State.WAIT_OPEN;
                this.stateTimer.reset();
                return;
            }
            if (this.openedCurrentChest || this.checkingUntimedChest) {
                if (this.targetChest != null) this.ignoredChests.put(this.targetChest, System.currentTimeMillis() + LAST_KNOWN_CHEST_FALLBACK_MS);
                this.targetChest = null;
                this.openedCurrentChest = false;
                this.aimedCurrentChest = false;
                this.checkingUntimedChest = false;
                this.state = State.SCAN_FIND_HOLOGRAM;
                this.stateTimer.reset();
                return;
            }
            this.state = State.WAIT_OPEN;
            this.stateTimer.reset();
            return;
        }
        this.checkingUntimedChest = false;
        this.openedCurrentChest = false;
        this.aimedCurrentChest = false;
        if (this.autoDeposit.getValue() && (this.pendingDeposit || this.hasLootToDeposit())) {
            this.stateTimer.reset();
            this.state = State.CLAN_STORAGE_OPEN;
        } else {
            this.pendingDeposit = false;
            this.resetChestState();
            this.scanIndex = 0;
            this.state = State.SCAN_NEXT;
            this.stateTimer.reset();
        }
    }

    private void handleSupplyWaitJoin() {
        String home = this.getHouseHome();
        if (home.isBlank()) { this.supplyRetryAfter = System.currentTimeMillis() + SUPPLY_RETRY_DELAY_MS; this.finishSupplies(false); return; }
        if (this.isSupplyTimedOut()) this.supplyStartedAt = System.currentTimeMillis();
        if (!this.supplyHomeCommandSent) {
            if (mc.world != null) mc.player.stopRiding();
            this.sendCommand(home);
            this.supplyHomeCommandSent = true;
            this.stateTimer.reset();
        } else if (this.stateTimer.finished(6000)) {
            this.state = State.SUPPLY_PATHING;
            this.stateTimer.reset();
        }
    }

    private void handleSupplyPathing(IBaritone baritone) {
        if (this.getHouseHome().isBlank()) { this.supplyRetryAfter = System.currentTimeMillis() + SUPPLY_RETRY_DELAY_MS; this.finishSupplies(false); return; }
        if (this.isSupplyTimedOut()) { this.supplyStartedAt = System.currentTimeMillis(); this.supplyChest = null; }
        if (this.supplyChest == null) {
            if (this.stateTimer.finished(500)) { this.supplyChest = this.findSupplyChest(); this.stateTimer.reset(); }
            return;
        }
        if (this.isNearChest(this.supplyChest)) {
            if (baritone != null) baritone.getPathingBehavior().cancelEverything();
            this.aimedSupplyChest = false;
            this.state = State.SUPPLY_OPEN;
            this.stateTimer.reset();
        } else if (baritone != null && (!baritone.getPathingBehavior().isPathing() || this.stateTimer.finished(2000))) {
            baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(this.supplyChest.getX(), this.supplyChest.getY(), this.supplyChest.getZ()));
            this.stateTimer.reset();
        }
    }

    private void handleSupplyOpen(IBaritone baritone) {
        if (baritone != null) baritone.getPathingBehavior().cancelEverything();
        if (this.isTimedOut(State.SUPPLY_OPEN)) { this.retryChestOpen(State.SUPPLY_OPEN); return; }
        BlockPos chest = this.validateSupplyChest();
        if (this.getHouseHome().isBlank() || chest == null || this.isSupplyTimedOut()) {
            this.closeScreen();
            this.supplyRetryAfter = System.currentTimeMillis() + SUPPLY_RETRY_DELAY_MS;
            this.finishSupplies(false);
            return;
        }
        if (!this.isNearChest(chest)) {
            this.closeScreen();
            this.resetOpenAttempts();
            this.aimedSupplyChest = false;
            this.state = State.SUPPLY_PATHING;
            this.stateTimer.reset();
            return;
        }
        if (this.isChestConfirmedOpen(chest)) {
            this.resetOpenAttempts();
            this.closeScreen();
            this.aimedSupplyChest = false;
            this.state = State.SUPPLY_TAKE;
            this.stateTimer.reset();
            return;
        }
        if (this.isInventoryOpen()) {
            this.closeScreen();
            this.resetOpenAttempts();
            this.stateTimer.reset();
            return;
        }
        if (this.aimedSupplyChest && !this.isChestStillThere(chest)) return;
        if (!this.attemptChestOpen(chest, false)) return;
        this.aimedSupplyChest = true;
        this.openedCurrentChest = true;
        this.stateTimer.reset();
    }

    private void handleSupplyTake() {
        if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler) {
            if (this.stateTimer.finished(80)) { this.takeSupplies(handler); this.stateTimer.reset(); }
            return;
        }
        this.finishSupplies(false);
    }

    private void handleClanStorageOpen() {
        if (!this.hasLootToDeposit()) { this.returnToScanning(); return; }
        if (this.isInventoryOpen()) {
            this.depositSlotIndex = 0;
            this.state = State.CLAN_STORAGE_DEPOSIT;
            this.stateTimer.reset();
        } else if (this.stateTimer.finished(800)) {
            this.sendCommand("clan storage");
            this.stateTimer.reset();
        }
    }

    private void handleClanStorageDeposit() {
        if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler) {
            if (this.stateTimer.finished(120)) { this.depositToClanStorage(handler); this.stateTimer.reset(); }
            return;
        }
        if (this.hasLootToDeposit()) {
            this.pendingClanStorageWithdraw = true;
            this.warehouseHomeCommandSent = false;
            this.state = State.GO_WAREHOUSE;
            this.stateTimer.reset();
        } else {
            this.returnToScanning();
        }
    }

    private void handleClanStorageWithdrawOpen() {
        if (this.isInventoryOpen()) {
            this.pendingClanStorageWithdraw = false;
            this.depositSlotIndex = 0;
            this.state = State.CLAN_STORAGE_WITHDRAW;
            this.stateTimer.reset();
        } else if (this.stateTimer.finished(800)) {
            this.sendCommand("clan storage");
            this.stateTimer.reset();
        }
    }

    private void handleClanStorageWithdraw() {
        if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler) {
            if (this.stateTimer.finished(120)) { this.withdrawFromClanStorage(handler); this.stateTimer.reset(); }
            return;
        }
        this.state = State.WAREHOUSE_OPEN;
        this.stateTimer.reset();
    }

    private void handleGoWarehouse() {
        String home = this.getHouseHome();
        if (home.isBlank()) { this.state = State.IDLE; return; }
        if (!this.hasLootToDeposit()) {
            if (this.pendingDeposit && !this.stateTimer.finished(2000)) return;
            this.pendingDeposit = false;
            this.resetChestState();
            this.scanIndex = 0;
            this.state = State.SCAN_NEXT;
            this.stateTimer.reset();
            return;
        }
        this.ignoredChests.clear();
        this.targetChest = null;
        if (!this.warehouseHomeCommandSent) {
            if (mc.world != null) mc.player.stopRiding();
            this.sendCommand(home);
            this.warehouseHomeCommandSent = true;
            this.stateTimer.reset();
            return;
        }
        if (this.stateTimer.finished(6000)) {
            this.state = this.pendingClanStorageWithdraw ? State.CLAN_STORAGE_WITHDRAW_OPEN : State.WAREHOUSE_OPEN;
            this.stateTimer.reset();
        }
    }

    private void handleReturnFarmHome() {
        if (!this.farmHomeCommandSent) {
            if (mc.world != null) mc.player.stopRiding();
            this.sendCommand(this.getFarmHome());
            this.farmHomeCommandSent = true;
            this.stateTimer.reset();
            return;
        }
        if (this.stateTimer.finished(6000)) {
            this.farmHomeCommandSent = false;
            this.warehouseHomeCommandSent = false;
            this.pendingDeposit = false;
            this.ignoredChests.clear();
            this.resetChestState();
            this.scanIndex = 0;
            this.state = State.SCAN_NEXT;
            this.stateTimer.reset();
        }
    }

    private void handleWarehouseFindChest(IBaritone baritone) {
        if (baritone != null && (this.targetChest == null || this.isNearChest(this.targetChest))) {
            baritone.getPathingBehavior().cancelEverything();
        }
        if (this.targetChest != null && this.isChestConfirmedOpen(this.targetChest)) {
            if (!this.isChestStillThere(this.targetChest)) { this.returnToScanning(); return; }
            this.resetOpenAttempts();
            this.depositSlotIndex = 0;
            this.state = State.DEPOSITING;
            this.stateTimer.reset();
            return;
        }
        if (this.isInventoryOpen()) {
            this.closeScreen();
            this.resetOpenAttempts();
            this.stateTimer.reset();
            return;
        }
        if (!this.hasLootToDeposit()) { this.returnToScanning(); return; }
        if (!this.stateTimer.finished(this.getOpenRetryDelay())) return;
        if (this.targetChest == null || !this.isChestStillThere(this.targetChest)) {
            this.targetChest = this.findWarehouseChest();
        }
        if (this.targetChest == null) { this.stateTimer.reset(); return; }
        if (!this.isNearChest(this.targetChest)) {
            this.resetOpenAttempts();
            if (baritone != null) baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(this.targetChest.getX(), this.targetChest.getY(), this.targetChest.getZ()));
            this.stateTimer.reset();
            return;
        }
        if (this.isTimedOut(State.WAREHOUSE_OPEN)) { this.retryChestOpen(State.WAREHOUSE_OPEN); return; }
        if (this.attemptChestOpen(this.targetChest, false)) this.stateTimer.reset();
    }

    private void handleWarehouseOpen(IBaritone baritone) {
        this.state = State.WAREHOUSE_FIND_CHEST;
    }

    private void handleDepositing() {
        if (mc.currentScreen instanceof HandledScreen handledScreen) {
            if (this.targetChest == null || !this.isChestStillThere(this.targetChest)) { this.returnToScanning(); return; }
            if (this.stateTimer.finished(150)) { this.depositToChest(handledScreen); this.stateTimer.reset(); }
            return;
        }
        if (this.hasLootToDeposit()) {
            if (this.targetChest != null) this.ignoredChests.put(this.targetChest, System.currentTimeMillis() + LAST_KNOWN_CHEST_FALLBACK_MS);
            this.targetChest = null;
            this.state = State.WAREHOUSE_FIND_CHEST;
            this.stateTimer.reset();
        } else {
            this.returnToScanning();
        }
    }

    private boolean isInHub() {
        if (mc.player == null) return false;
        BlockPos pos = mc.player.getBlockPos();
        return pos.getX() == 0 && pos.getZ() == 0 && pos.getY() == 90;
    }

    private void sendHomeCommand() {
        long now = System.currentTimeMillis();
        if (this.pausedByHomeTeleport && now < this.homeTeleportPauseUntil) return;
        String home = this.getHouseHome();
        if (home.isBlank()) home = CLAN_HOME_PREFIX + this.warehouseHome.getText();
        this.sendCommand(home);
        this.stopEatingFood();
        this.stopDrinkingInvisibility();
        IBaritone baritone = this.getBaritone();
        if (baritone != null) baritone.getPathingBehavior().cancelEverything();
        this.disableAutoJump();
        if (mc.world != null) mc.player.stopRiding();
        this.pausedByHomeTeleport = true;
        this.homeTeleportPauseUntil = now + 5000L;
        this.stateTimer.reset();
    }

    private boolean shouldHandleSupplies() {
        if (!this.autoSupplies.getValue()) return false;
        return switch (this.state) {
            case ARENA_SET_HOME, ARENA_OPEN, ARENA_WAIT_RETURN, ARENA_RETURN, ARENA_RETURN_WAIT,
                 SUPPLY_WAIT_JOIN, SUPPLY_PATHING, SUPPLY_OPEN, SUPPLY_TAKE,
                 CLAN_STORAGE_OPEN, CLAN_STORAGE_DEPOSIT, CLAN_STORAGE_WITHDRAW_OPEN, CLAN_STORAGE_WITHDRAW,
                 GO_WAREHOUSE, RETURN_FARM_HOME, WAREHOUSE_WAIT_JOIN, WAREHOUSE_FIND_CHEST,
                 WAREHOUSE_PATHING, WAREHOUSE_OPEN, DEPOSITING -> false;
            default -> this.getInvisibilityCount() < 1 || this.getFoodCount() < MIN_FOOD_ITEMS;
        };
    }

    private boolean goToSupply() {
        if (!this.autoSupplies.getValue() || mc.currentScreen != null || this.getHouseHome().isBlank()) return false;
        long now = System.currentTimeMillis();
        this.supplyChest = this.findSupplyChest();
        this.supplyReturnState = State.SCAN_NEXT;
        this.supplyStartedAt = now;
        this.aimedSupplyChest = false;
        this.supplyTookInvisibility = this.hasInvisibilityPotions();
        this.supplyTookFood = false;
        this.supplyHomeCommandSent = false;
        this.state = this.supplyChest == null ? State.SUPPLY_WAIT_JOIN : (this.isNearChest(this.supplyChest) ? State.SUPPLY_OPEN : State.SUPPLY_PATHING);
        this.stateTimer.reset();
        return true;
    }

    private boolean isWaitingForTimedChest() {
        if (this.targetAnarchy == -1 || this.targetOpenTime == -1L || this.targetChest == null) return false;
        long timeLeft = this.targetOpenTime - System.currentTimeMillis();
        long minWait = Math.max(10000L, this.getRejoinLead() + 2000L);
        return timeLeft <= this.getMaxWaitTime() && timeLeft > minWait;
    }

    private boolean isTimedOut(State checkState) {
        long now = System.currentTimeMillis();
        if (this.openTimeoutState != checkState) {
            this.openTimeoutState = checkState;
            this.openTimeoutStartedAt = now;
            return false;
        }
        if (checkState == State.WAIT_OPEN && this.openingTimedChestImmediately && this.targetOpenTime > 0L && now <= this.targetOpenTime + 20000L) return false;
        return this.openTimeoutStartedAt > 0L && now - this.openTimeoutStartedAt > 20000L;
    }

    private void resetOpenAttempts() {
        this.openTimeoutState = null;
        this.openTimeoutStartedAt = -1L;
        this.lastOpenAttemptChest = null;
        this.lastOpenAttemptAt = -1L;
        this.pendingOpenedChest = null;
        this.openAttemptStartedAt = -1L;
    }

    private void retryChestOpen(State retryState) {
        this.stopEatingFood();
        this.stopDrinkingInvisibility();
        this.resetOpenAttempts();
        if (mc.world != null) mc.player.stopRiding();

        if (retryState == State.SUPPLY_OPEN) {
            this.supplyChest = null;
            this.aimedSupplyChest = false;
            this.supplyStartedAt = System.currentTimeMillis();
            this.state = State.SUPPLY_PATHING;
        } else if (retryState == State.WAREHOUSE_OPEN) {
            if (this.targetChest != null) this.ignoredChests.put(this.targetChest, System.currentTimeMillis() + 60000L);
            this.targetChest = null;
            this.state = State.WAREHOUSE_FIND_CHEST;
        } else {
            if (this.targetChest != null && this.chestOpenRecoveries < 4) {
                ++this.chestOpenRecoveries;
                this.openedCurrentChest = false;
                this.aimedCurrentChest = false;
                this.checkingUntimedChest = false;
                this.openingTimedChestImmediately = false;
                this.state = this.isNearChest(this.targetChest) ? State.WAIT_OPEN : State.RUSH_PATH;
                this.stateTimer.reset();
                return;
            }
            if (this.targetChest != null) this.ignoredChests.put(this.targetChest, System.currentTimeMillis() + 60000L);
            this.targetChest = null;
            this.openedCurrentChest = false;
            this.aimedCurrentChest = false;
            this.checkingUntimedChest = false;
            this.openingTimedChestImmediately = false;
            this.chestOpenRecoveries = 0;
            this.state = State.SCAN_FIND_HOLOGRAM;
        }
        this.stateTimer.reset();
    }

    private void handleBaritonePathingTimeout(IBaritone baritone) {
        if (baritone == null || !this.isBaritonePathingNeeded()) { this.baritoneNoPathSince = -1L; return; }
        IPathingBehavior pb = baritone.getPathingBehavior();
        boolean hasPath = pb.getInProgress().isPresent();
        boolean hasGoal = pb.getGoal() != null;
        boolean stuck = hasGoal && !pb.hasPath() && !pb.isPathing();
        boolean isPathing = hasPath || stuck;
        if (!isPathing) { this.baritoneNoPathSince = -1L; return; }
        long now = System.currentTimeMillis();
        if (this.baritoneNoPathSince == -1L) { this.baritoneNoPathSince = now; return; }
        if (now - this.baritoneNoPathSince >= BARITONE_NO_PATH_RESET_MS) this.resetBaritonePathing();
    }

    private boolean isBaritonePathingNeeded() {
        return switch (this.state) {
            case SCAN_PATHING, RUSH_PATH, SUPPLY_PATHING, WAREHOUSE_FIND_CHEST -> true;
            case WAREHOUSE_OPEN -> this.targetChest != null && !this.isNearChest(this.targetChest);
            default -> false;
        };
    }

    private void resetBaritonePathing() {
        IBaritone baritone = this.getBaritone();
        if (baritone != null) baritone.getPathingBehavior().cancelEverything();
        this.resetOpenAttempts();
        this.baritoneNoPathSince = -1L;
        this.aimedCurrentChest = false;
        this.aimedSupplyChest = false;
        this.openedCurrentChest = false;
        switch (this.state) {
            case SUPPLY_PATHING -> { this.supplyChest = null; this.state = State.SUPPLY_PATHING; }
            case WAREHOUSE_FIND_CHEST, WAREHOUSE_OPEN -> { this.targetChest = null; this.state = State.WAREHOUSE_FIND_CHEST; }
            default -> { this.targetChest = null; this.targetFoundOpenTime = -1L; this.scannedChestOpenTime = -1L; this.state = State.SCAN_FIND_HOLOGRAM; }
        }
        this.stateTimer.reset();
    }

    private BlockPos findNearestHologram() {
        this.scannedChestOpenTime = -1L;
        if (mc.player == null || mc.world == null) return null;
        long now = System.currentTimeMillis();
        Vec3d playerPos = mc.player.getEyePos();
        List<HologramData> holograms = new ArrayList<>();
        Box searchBox = mc.player.getBoundingBox().expand(150.0);

        for (Entity entity : mc.world.getEntities()) {
            if (!searchBox.intersects(entity.getBoundingBox()) || !this.isValidHologram(entity)) continue;
            String text = this.getHologramText(entity);
            long time = this.parseTime(text);
            if (time < 0L) continue;
            BlockPos entityPos = entity.getBlockPos();
            BlockPos chestPos = entityPos;
            if (mc.world.isChunkLoaded(entityPos.getX() >> 4, entityPos.getZ() >> 4)) {
                BlockPos chestBelow = null;
                for (int i = 1; i <= 3; i++) {
                    BlockPos check = entityPos.down(i);
                    Block block = mc.world.getBlockState(check).getBlock();
                    if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) {
                        chestBelow = check;
                        break;
                    }
                }
                if (chestBelow == null) continue;
                chestPos = chestBelow;
            }
            Long ignoreUntil = this.ignoredChests.get(chestPos);
            if (ignoreUntil != null && ignoreUntil > now) continue;
            long openTime = now + time * 1000L + 1000L;
            double distance = entity.getPos().distanceTo(playerPos);
            HologramData existing = null;
            for (HologramData h : holograms) {
                if (h.pos.equals(chestPos)) { existing = h; break; }
            }
            if (existing == null) {
                holograms.add(new HologramData(chestPos, openTime, distance));
            } else {
                existing.openTime = Math.max(existing.openTime, openTime);
                existing.distance = Math.min(existing.distance, distance);
            }
        }

        HologramData best = null;
        for (HologramData h : holograms) {
            if (best == null) { best = h; continue; }
            boolean timed = h.openTime > now;
            boolean bestTimed = best.openTime > now;
            if (timed && !bestTimed) { best = h; continue; }
            if (timed && bestTimed && h.openTime < best.openTime) { best = h; continue; }
            if (!timed && !bestTimed && h.distance < best.distance) { best = h; }
        }
        if (best == null) return null;
        this.scannedChestOpenTime = best.openTime;
        return best.pos;
    }

    private BlockPos findTimedChest() {
        if (mc.player == null || mc.world == null) return null;
        long now = System.currentTimeMillis();
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos bestChest = null;
        double bestDistance = Double.MAX_VALUE;
        int chunkX = playerPos.getX() >> 4;
        int chunkZ = playerPos.getZ() >> 4;

        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                int cx = chunkX + dx;
                int cz = chunkZ + dz;
                if (!mc.world.getChunkManager().isChunkLoaded(cx, cz)) continue;
                WorldChunk chunk = mc.world.getChunk(cx, cz);
                if (chunk == null) continue;
                for (BlockPos pos : chunk.getBlockEntities().keySet()) {
                    BlockPos chestPos = pos;
                    if (Math.abs(chestPos.getY() - playerPos.getY()) > 48) continue;
                    if (!isValidChestBlock(chestPos)) continue;
                    Long ignoreUntil = this.ignoredChests.get(chestPos);
                    if (ignoreUntil != null && ignoreUntil > now) continue;
                    double dist = playerPos.getSquaredDistance(Vec3d.ofCenter(chestPos));
                    if (dist > 22500.0 || dist >= bestDistance) continue;
                    bestDistance = dist;
                    bestChest = chestPos;
                }
            }
        }
        BlockPos nearbyChest = this.findNearestChestNearby();
        return this.pickBestChest(bestChest, nearbyChest);
    }

    private BlockPos findNearestChestNearby() {
        if (mc.player == null || mc.world == null) return null;
        long now = System.currentTimeMillis();
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos bestChest = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.iterateOutwards(playerPos, 24, 16, 24)) {
            if (!isValidChestBlock(pos)) continue;
            Long ignoreUntil = this.ignoredChests.get(pos);
            if (ignoreUntil != null && ignoreUntil > now) continue;
            double dist = playerPos.getSquaredDistance(Vec3d.ofCenter(pos));
            if (dist < bestDistance) { bestDistance = dist; bestChest = pos; }
        }
        return bestChest;
    }

    private BlockPos pickBestChest(BlockPos c1, BlockPos c2) {
        if (c1 == null) return c2;
        if (c2 == null) return c1;
        if (mc.player == null) return c1;
        return mc.player.getBlockPos().getSquaredDistance(Vec3d.ofCenter(c1)) <= mc.player.getBlockPos().getSquaredDistance(Vec3d.ofCenter(c2)) ? c1 : c2;
    }

    private void rememberChest(BlockPos pos) {
        if (pos == null) return;
        this.lastKnownLootChest = pos;
        this.lastKnownLootChestAt = System.currentTimeMillis();
    }

    private BlockPos getLastKnownChest() {
        if (this.lastKnownLootChest == null || this.lastKnownLootChestAt <= 0L) return null;
        if (System.currentTimeMillis() - this.lastKnownLootChestAt > LAST_KNOWN_CHEST_FALLBACK_MS) return null;
        return this.lastKnownLootChest;
    }

    private long readChestTime(BlockPos pos) {
        if (mc.player == null || mc.world == null || pos == null) return -1L;
        Vec3d center = Vec3d.ofCenter(pos);
        double bestDist = Double.MAX_VALUE;
        long bestTime = -1L;
        Box box = new Box(pos.getX() - 2, pos.getY(), pos.getZ() - 2, pos.getX() + 3, pos.getY() + 7, pos.getZ() + 3);
        for (Entity entity : mc.world.getEntities()) {
            if (!box.intersects(entity.getBoundingBox()) || !this.isValidHologram(entity)) continue;
            double dx = entity.getX() - center.x;
            double dz = entity.getZ() - center.z;
            double hDist = dx * dx + dz * dz;
            if (entity.getY() + 0.25 < pos.getY() || hDist > HOLOGRAM_READ_RADIUS_SQ) continue;
            long time = this.parseTime(this.getHologramText(entity));
            if (time < 0L) continue;
            if (bestTime == -1L || hDist + 0.25 < bestDist) { bestDist = hDist; bestTime = time; }
            else if (Math.abs(hDist - bestDist) <= 0.25 && time <= bestTime) { bestTime = time; }
        }
        return bestTime;
    }

    private boolean isValidHologram(Entity entity) {
        return (entity instanceof ArmorStandEntity as && as.isMarker()) || entity instanceof ItemFrameEntity;
    }

    private String getHologramText(Entity entity) {
        Text name = entity.getCustomName();
        String text = name != null ? name.getString() : "";
        return this.cleanString(text);
    }

    private long parseTime(String text) {
        if (text == null || text.isBlank()) return -1L;
        text = this.cleanString(text).replace('\u00a0', ' ').replace('\u202f', ' ');
        Matcher m = TIME_PATTERN.matcher(text);
        if (m.find()) {
            long h = Long.parseLong(m.group(1));
            long min = Long.parseLong(m.group(2));
            if (m.group(3) == null) return h * 60L + min;
            return h * 3600L + min * 60L + Long.parseLong(m.group(3));
        }
        m = MIN_SEC_PATTERN.matcher(text);
        if (m.find()) {
            long total = Long.parseLong(m.group(1)) * 60L;
            if (m.group(2) != null) total += Long.parseLong(m.group(2));
            return total;
        }
        if (text.contains(":")) return -1L;
        m = SECONDS_PATTERN.matcher(text);
        if (m.find()) return Long.parseLong(m.group(1));
        return -1L;
    }

    private boolean attemptChestOpen(BlockPos pos, boolean isLoot) {
        if (pos == null || mc.player == null || mc.interactionManager == null) return false;
        if (!isValidChestBlock(pos) || !this.isNearChest(pos)) return false;
        long now = System.currentTimeMillis();
        if (this.lastOpenAttemptChest != null && this.lastOpenAttemptChest.equals(pos) && now - this.lastOpenAttemptAt < this.getOpenRetryDelay()) return false;
        this.pendingOpenedChest = pos;
        this.openAttemptStartedAt = now;
        this.lastOpenAttemptChest = pos;
        this.lastOpenAttemptAt = now;
        this.pointAtBlock(pos);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, this.getHitResult(pos));
        mc.player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    private boolean isChestConfirmedOpen(BlockPos pos) {
        if (!this.isInventoryOpen() || pos == null || this.pendingOpenedChest == null) return false;
        if (!this.pendingOpenedChest.equals(pos)) return false;
        if (this.openAttemptStartedAt <= 0L || System.currentTimeMillis() - this.openAttemptStartedAt > 5000L) return false;
        return this.isNearChest(pos);
    }

    private boolean isChestStillThere(BlockPos pos) {
        return isValidChestBlock(pos) && this.isNearChest(pos);
    }

    private BlockPos validateTargetChest() {
        if (this.targetChest != null && isValidChestBlock(this.targetChest)) return this.targetChest;
        return null;
    }

    private BlockPos validateSupplyChest() {
        if (this.supplyChest != null && isValidChestBlock(this.supplyChest)) return this.supplyChest;
        return this.findSupplyChest();
    }

    private boolean isNearChest(BlockPos pos) {
        return mc.player != null && pos != null && mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos)) <= CHEST_OPEN_RANGE_SQ;
    }

    private void pointAtBlock(BlockPos pos) {
        Vec3d target = Vec3d.ofCenter(pos);
        double dx = target.x - mc.player.getX();
        double dy = target.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double dz = target.z - mc.player.getZ();
        mc.player.setYaw((float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f);
        mc.player.setPitch((float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)))));
    }

    private net.minecraft.util.hit.BlockHitResult getHitResult(BlockPos pos) {
        return new net.minecraft.util.hit.BlockHitResult(Vec3d.ofCenter(pos), net.minecraft.util.math.Direction.UP, pos, false);
    }

    private boolean isValidChestBlock(BlockPos pos) {
        if (mc.world == null || pos == null) return false;
        Block block = mc.world.getBlockState(pos).getBlock();
        return block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.BARREL || block instanceof net.minecraft.block.ShulkerBoxBlock;
    }

    private boolean isValidChestWithSign(BlockPos pos) {
        if (mc.world == null || pos == null) return false;
        if (!isValidChestBlock(pos)) return false;
        for (BlockPos check : BlockPos.iterateOutwards(pos, 1, 2, 1)) {
            BlockEntity be = mc.world.getBlockEntity(check);
            if (be instanceof SignBlockEntity sign && this.isSupplySign(sign)) return true;
        }
        return false;
    }

    private boolean isSupplySign(SignBlockEntity sign) {
        String text = this.cleanString(this.getSignText(sign.getFrontText()) + " " + this.getSignText(sign.getBackText()));
        for (String keyword : this.getSupplyKeywords()) {
            if (!keyword.isBlank() && text.contains(this.cleanString(keyword))) return true;
        }
        return false;
    }

    private String getSignText(SignText signText) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) sb.append(' ').append(signText.getMessage(i, false).getString());
        return sb.toString().replaceAll("\u00a7.", "");
    }

    private List<String> getSupplyKeywords() {
        List<String> keywords = new ArrayList<>();
        String raw = this.supplySignKeywords.getText();
        if (raw != null) {
            for (String part : raw.split("[,;\\s]+")) {
                String kw = this.cleanString(part).trim();
                if (!kw.isBlank()) keywords.add(kw);
            }
        }
        if (keywords.isEmpty()) keywords.addAll(List.of(DEFAULT_SUPPLY_SIGN_KEYWORDS));
        return keywords;
    }

    private BlockPos findSupplyChest() {
        if (mc.player == null || mc.world == null) return null;
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.iterateOutwards(playerPos, SUPPLY_SEARCH_RADIUS, 1, SUPPLY_SEARCH_RADIUS)) {
            if (!isValidChestWithSign(pos)) continue;
            double dist = playerPos.getSquaredDistance(Vec3d.ofCenter(pos));
            if (dist < bestDist) { bestDist = dist; best = pos; }
        }
        return best != null ? best : this.getAimedChest();
    }

    private BlockPos getAimedChest() {
        if (mc.world == null) return null;
        net.minecraft.util.hit.HitResult hr = mc.crosshairTarget;
        if (hr instanceof net.minecraft.util.hit.BlockHitResult bhr) {
            BlockPos pos = bhr.getBlockPos();
            if (isValidChestBlock(pos) && mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos)) <= 36.0) return pos;
        }
        return null;
    }

    private BlockPos findWarehouseChest() {
        BlockPos aimed = this.getAimedChest();
        if (aimed != null) return aimed;
        if (mc.player == null || mc.world == null) return null;
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        long now = System.currentTimeMillis();
        for (BlockPos pos : BlockPos.iterateOutwards(playerPos, 32, 1, 32)) {
            Long ignore = this.ignoredChests.get(pos);
            if (ignore != null && ignore > now) continue;
            if (!isValidChestBlock(pos)) continue;
            double dist = playerPos.getSquaredDistance(Vec3d.ofCenter(pos));
            if (dist < bestDist) { bestDist = dist; best = pos; }
        }
        return best;
    }

    private void lootChest(GenericContainerScreenHandler handler) {
        int totalSlots = handler.getInventory().size();
        boolean looted = false;
        for (int i = 0; i < totalSlots; i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (stack.isEmpty() || !this.isValuableLoot(stack)) continue;
            mc.interactionManager.clickSlot(handler.syncId, handler.getSlot(i).id, 0, SlotActionType.QUICK_MOVE, mc.player);
            looted = true;
        }
        if (looted) {
            this.lootedCurrentChest = true;
            this.emptyLootChecks = 0;
            this.pendingDeposit = true;
        }
        this.checkEmptyChest(handler);
    }

    private void checkEmptyChest(GenericContainerScreenHandler handler) {
        if (this.isTimedChestLootGrace()) { this.emptyLootChecks = 0; return; }
        if (this.lootContainerOpenedAt > 0L && System.currentTimeMillis() - this.lootContainerOpenedAt < LOOT_EMPTY_CHECK_DELAY_MS) return;
        boolean hasItems = false;
        for (int i = 0; i < handler.getInventory().size(); i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (stack.isEmpty()) continue;
            hasItems = true;
            if (this.isValuableLoot(stack)) { this.emptyLootChecks = 0; return; }
        }
        if (hasItems) return;
        if (++this.emptyLootChecks >= LOOT_EMPTY_REQUIRED_CHECKS) {
            mc.player.closeHandledScreen();
        }
    }

    private void takeSupplies(GenericContainerScreenHandler handler) {
        int totalSlots = handler.getInventory().size();
        if (!this.supplyTookInvisibility) {
            int slot = this.findItemSlot(handler, totalSlots, this::isInvisibilityPotion);
            this.supplyTookInvisibility = true;
            if (slot != -1) { this.takeItemFromSupply(handler, slot, totalSlots); return; }
        }
        if (!this.supplyTookFood) {
            int slot = this.findItemSlot(handler, totalSlots, this::isFood);
            this.supplyTookFood = true;
            if (slot != -1) { this.takeItemFromSupply(handler, slot); return; }
        }
        mc.player.closeHandledScreen();
        this.finishSupplies(true);
    }

    private void takeItemFromSupply(GenericContainerScreenHandler handler, int slot, int totalSlots) {
        ItemStack stack = handler.getSlot(slot).getStack();
        int emptySlot = this.findEmptySlot(handler, totalSlots, stack);
        if (emptySlot == -1) {
            this.supplyRetryAfter = System.currentTimeMillis() + SUPPLY_RETRY_DELAY_MS;
            mc.player.closeHandledScreen();
            this.finishSupplies(false);
            return;
        }
        mc.interactionManager.clickSlot(handler.syncId, handler.getSlot(slot).id, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(handler.syncId, handler.getSlot(emptySlot).id, 1, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(handler.syncId, handler.getSlot(slot).id, 0, SlotActionType.PICKUP, mc.player);
    }

    private void takeItemFromSupply(GenericContainerScreenHandler handler, int slot) {
        ItemStack stack = handler.getSlot(slot).getStack();
        if (!this.canFitItem(stack)) {
            this.supplyRetryAfter = System.currentTimeMillis() + SUPPLY_RETRY_DELAY_MS;
            mc.player.closeHandledScreen();
            this.finishSupplies(false);
            return;
        }
        mc.interactionManager.clickSlot(handler.syncId, handler.getSlot(slot).id, 0, SlotActionType.QUICK_MOVE, mc.player);
    }

    private int findItemSlot(GenericContainerScreenHandler handler, int max, Predicate<ItemStack> predicate) {
        for (int i = 0; i < max; i++) {
            if (predicate.test(handler.getSlot(i).getStack())) return i;
        }
        return -1;
    }

    private int findEmptySlot(GenericContainerScreenHandler handler, int max, ItemStack stack) {
        for (int i = 0; i < max; i++) {
            ItemStack cur = handler.getSlot(i).getStack();
            if (cur.isEmpty() || (ItemStack.areItemsEqual(cur, stack) && cur.getCount() < cur.getMaxCount())) return i;
        }
        for (int i = 0; i < max; i++) {
            if (handler.getSlot(i).getStack().isEmpty()) return i;
        }
        return -1;
    }

    private boolean canFitItem(ItemStack stack) {
        if (mc.player == null || stack == null) return false;
        for (int i = 0; i < 36; i++) {
            ItemStack inv = mc.player.getInventory().getStack(i);
            if (inv.isEmpty()) return true;
            if (ItemStack.areItemsEqual(inv, stack) && inv.getCount() < inv.getMaxCount()) return true;
        }
        return false;
    }

    private void depositToClanStorage(GenericContainerScreenHandler handler) {
        int startSlot = handler.getInventory().size() - 36;
        if (startSlot < 0) { mc.player.closeHandledScreen(); return; }
        for (int i = startSlot + this.depositSlotIndex; i < handler.getInventory().size(); i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (stack.isEmpty() || !this.isDepositItem(stack)) continue;
            ItemStack original = stack.copy();
            mc.interactionManager.clickSlot(handler.syncId, handler.getSlot(i).id, 0, SlotActionType.QUICK_MOVE, mc.player);
            ItemStack after = handler.getSlot(i).getStack();
            if (!after.isEmpty() && ItemStack.areItemsEqual(after, original) && after.getCount() >= original.getCount()) {
                this.depositSlotIndex = 0;
                this.pendingClanStorageWithdraw = true;
                this.warehouseHomeCommandSent = false;
                mc.player.closeHandledScreen();
                this.state = State.GO_WAREHOUSE;
                this.stateTimer.reset();
                return;
            }
            this.depositSlotIndex = i - startSlot + 1;
            return;
        }
        this.depositSlotIndex = 0;
        mc.player.closeHandledScreen();
    }

    private void withdrawFromClanStorage(GenericContainerScreenHandler handler) {
        int totalSlots = handler.getInventory().size();
        for (int i = this.depositSlotIndex; i < totalSlots; i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (stack.isEmpty() || !this.isDepositItem(stack)) continue;
            if (!this.canFitItem(stack)) {
                this.pendingClanStorageWithdraw = true;
                this.depositSlotIndex = 0;
                mc.player.closeHandledScreen();
                return;
            }
            mc.interactionManager.clickSlot(handler.syncId, handler.getSlot(i).id, 0, SlotActionType.QUICK_MOVE, mc.player);
            this.depositSlotIndex = i + 1;
            return;
        }
        this.depositSlotIndex = 0;
        this.pendingClanStorageWithdraw = false;
        mc.player.closeHandledScreen();
        this.state = State.WAREHOUSE_OPEN;
        this.stateTimer.reset();
    }

    private void depositToChest(HandledScreen<?> screen) {
        Inventory inv = ((GenericContainerScreenHandler)screen.getScreenHandler()).getInventory();
        int startSlot = inv.size() - 36;
        for (int i = this.depositSlotIndex; i < 36; i++) {
            int idx = startSlot + i;
            ItemStack stack = inv.getStack(idx);
            if (stack.isEmpty() || !this.isDepositItem(stack)) continue;
            mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, idx, 0, SlotActionType.QUICK_MOVE, mc.player);
            this.depositSlotIndex = i + 1;
            return;
        }
        this.depositSlotIndex = 0;
        mc.player.closeHandledScreen();
        if (this.hasLootToDeposit()) {
            if (this.targetChest != null) this.ignoredChests.put(this.targetChest, System.currentTimeMillis() + LAST_KNOWN_CHEST_FALLBACK_MS);
            this.targetChest = null;
            this.state = State.WAREHOUSE_FIND_CHEST;
        } else if (this.pendingClanStorageWithdraw) {
            this.state = State.CLAN_STORAGE_WITHDRAW_OPEN;
        } else {
            this.pendingDeposit = false;
            this.returnToScanning();
        }
        this.stateTimer.reset();
    }

    private boolean tryTakeFromContainer(GenericContainerScreenHandler handler) {
        for (int i = 0; i < handler.getInventory().size(); i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (!stack.isEmpty() && stack.getItem() == Items.PAPER) {
                mc.interactionManager.clickSlot(handler.syncId, handler.getSlot(i).id, 0, SlotActionType.PICKUP, mc.player);
                return true;
            }
        }
        return false;
    }

    private boolean isValuableLoot(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !this.autoLoot.getValue()) return false;
        String name = this.normalizeString(stack.getName().getString());
        String id = this.normalizeString(Registries.ITEM.getId(stack.getItem()).toString());
        for (String strictId : STRICT_LOOT_ITEM_IDS) {
            if (id.equals(this.normalizeString(strictId))) return true;
        }
        for (String lootItem : LOOT_ITEMS) {
            String n = this.normalizeString(lootItem);
            if (!n.isEmpty() && !this.isStrictId(n) && name.contains(n)) return true;
        }
        return false;
    }

    private boolean isStrictId(String name) {
        for (String strictId : STRICT_LOOT_ITEM_IDS) {
            if (name.equals(this.normalizeString(strictId))) return true;
        }
        return false;
    }

    private boolean isDepositItem(ItemStack stack) {
        return !this.isSupplyItem(stack) && this.isValuableLoot(stack);
    }

    private boolean isSupplyItem(ItemStack stack) {
        return this.isInvisibilityPotion(stack) || this.isFood(stack);
    }

    private boolean hasLootToDeposit() {
        if (mc.player == null) return false;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && this.isDepositItem(stack)) return true;
        }
        return false;
    }

    private boolean isInvisibilityPotion(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!stack.isOf(Items.POTION)) return false;
        String name = this.normalizeString(stack.getName().getString());
        if (name.contains("инвиз") || name.contains("невид") || name.contains("invis")) return true;
        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) return false;
        for (Object o : contents.getEffects()) {
            StatusEffectInstance effect = (StatusEffectInstance) o;
            if (effect.getEffectType().value() == StatusEffects.INVISIBILITY) return true;
        }
        return false;
    }

    private boolean isFood(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.contains(DataComponentTypes.FOOD);
    }

    private int getInvisibilityCount() {
        return this.countItems(this::isInvisibilityPotion);
    }

    private boolean hasInvisibilityPotions() {
        if (mc.player == null) return false;
        for (int i = 0; i < 9; i++) {
            if (this.isInvisibilityPotion(mc.player.getInventory().getStack(i))) return true;
        }
        return false;
    }

    private int getFoodCount() {
        return this.countItems(this::isFood);
    }

    private int countItems(Predicate<ItemStack> predicate) {
        if (mc.player == null) return 0;
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (predicate.test(stack)) count += stack.getCount();
        }
        return count;
    }

    private boolean isInventoryOpen() {
        return mc.currentScreen instanceof HandledScreen && mc.player != null;
    }

    private boolean isContainerOpen() {
        return mc.player != null && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler;
    }

    private boolean isTimedChestLootGrace() {
        return this.openingTimedChestImmediately && this.targetOpenTime > 0L && System.currentTimeMillis() < this.targetOpenTime + TIMED_CHEST_LOOT_GRACE_MS;
    }

    private boolean shouldSetArenaHome(long timeUntilOpen) {
        return timeUntilOpen > 9000L && timeUntilOpen <= this.getMaxWaitTime();
    }

    private void finishSupplies(boolean success) {
        this.closeScreen();
        this.resetOpenAttempts();
        if (mc.world != null) mc.player.stopRiding();
        this.supplyChest = null;
        this.supplyStartedAt = -1L;
        this.aimedSupplyChest = false;
        this.supplyTookInvisibility = false;
        this.supplyTookFood = false;
        this.supplyHomeCommandSent = false;
        if (!success && this.initialSupplyPending) {
            this.state = State.IDLE;
            this.stateTimer.reset();
            return;
        }
        if (success) {
            this.initialSupplyPending = false;
            this.forceSupplyPending = false;
            this.returnToScanning();
            return;
        }
        State returnState = this.supplyReturnState == null ? State.RUSH_JOIN : this.supplyReturnState;
        this.state = returnState;
        if (this.state == State.HUB_WAITING) {
            this.returnCommandSent = false;
            this.sendHomeCommand();
        }
        this.stateTimer.reset();
    }

    private void returnToScanning() {
        this.closeScreen();
        this.resetOpenAttempts();
        if (this.targetChest != null) this.ignoredChests.put(this.targetChest, System.currentTimeMillis() + LAST_KNOWN_CHEST_FALLBACK_MS);
        this.targetChest = null;
        this.depositSlotIndex = 0;
        if (mc.player != null && mc.world != null) mc.player.closeHandledScreen();
        this.state = State.WAREHOUSE_FIND_CHEST;
        this.stateTimer.reset();
    }

    private void returnToScanningSimple() {
        this.pendingDeposit = false;
        this.pendingClanStorageWithdraw = false;
        this.warehouseHomeCommandSent = false;
        this.resetChestState();
        this.scanIndex = 0;
        this.state = State.SCAN_NEXT;
        this.stateTimer.reset();
    }

    private void cleanupAll() {
        this.closeScreen();
        this.stopEatingFood();
        this.stopDrinkingInvisibility();
        this.resetOpenAttempts();
        this.resetChestState();
        this.ignoredChests.clear();
        this.depositSlotIndex = 0;
        this.scanIndex = 0;
        this.pendingDeposit = false;
        this.warehouseHomeCommandSent = false;
        this.pendingClanStorageWithdraw = false;
        this.farmHomeCommandSent = false;
        this.supplyHomeCommandSent = false;
        this.chestOpenRecoveries = 0;
        this.lootContainerOpenedAt = -1L;
        this.emptyLootChecks = 0;
        this.supplyRetryAfter = 0L;
        this.initialSupplyPending = this.autoSupplies.getValue();
        this.forceSupplyPending = false;
        this.state = State.IDLE;
        this.stateTimer.reset();
    }

    private void resetChestState() {
        this.closeScreen();
        this.stopEatingFood();
        this.stopDrinkingInvisibility();
        this.resetOpenAttempts();
        this.targetAnarchy = -1;
        this.targetChest = null;
        this.targetOpenTime = -1L;
        this.targetFoundOpenTime = -1L;
        this.scannedChestOpenTime = -1L;
        this.lootedCurrentChest = false;
        this.openedCurrentChest = false;
        this.aimedCurrentChest = false;
        this.checkingUntimedChest = false;
        this.openingTimedChestImmediately = false;
        this.chestOpenRecoveries = 0;
        this.lootContainerOpenedAt = -1L;
        this.emptyLootChecks = 0;
        this.returnCommandSent = false;
        this.warehouseHomeCommandSent = false;
        this.pendingClanStorageWithdraw = false;
        this.farmHomeCommandSent = false;
        this.supplyHomeCommandSent = false;
        this.supplyChest = null;
        this.supplyReturnState = State.RUSH_JOIN;
        this.supplyStartedAt = -1L;
        this.aimedSupplyChest = false;
        this.supplyTookInvisibility = false;
        this.supplyTookFood = false;
    }

    private void handleTickError() {
        try {
            this.stopEatingFood();
            this.stopDrinkingInvisibility();
            this.resetOpenAttempts();
            IBaritone baritone = this.getBaritone();
            if (baritone != null) baritone.getPathingBehavior().cancelEverything();
            if (mc.world != null) mc.player.stopRiding();
            this.targetChest = null;
            this.supplyChest = null;
            this.openedCurrentChest = false;
            this.aimedCurrentChest = false;
            this.aimedSupplyChest = false;
            this.checkingUntimedChest = false;
            this.openingTimedChestImmediately = false;
            this.chestOpenRecoveries = 0;
            this.depositSlotIndex = 0;
            this.emptyLootChecks = 0;
            this.pendingClanStorageWithdraw = false;
            this.warehouseHomeCommandSent = false;
            this.farmHomeCommandSent = false;
            this.supplyHomeCommandSent = false;
            this.forceSupplyPending = false;
            this.state = this.initialSupplyPending ? State.IDLE : State.SCAN_NEXT;
            this.stateTimer.reset();
        } catch (Throwable t) {
            this.state = State.IDLE;
        }
    }

    private void clearExpiredIgnoredChests() {
        long now = System.currentTimeMillis();
        this.ignoredChests.entrySet().removeIf(e -> e.getValue() <= now);
    }

    private void sendCommand(String cmd) {
        if (cmd == null || cmd.isBlank() || mc.player == null) return;
        mc.player.networkHandler.sendChatCommand(cmd.trim());
    }

    private void sendHomeDirect() {
        String home = this.getHouseHome();
        if (!this.autoSupplies.getValue() || home.isBlank()) {
            this.sendCommand(this.getFarmHome());
            return;
        }
        if (mc.world != null) mc.player.stopRiding();
        this.supplyChest = null;
        this.supplyReturnState = State.RETURN_FARM_HOME;
        this.supplyStartedAt = System.currentTimeMillis();
        this.aimedSupplyChest = false;
        this.supplyTookInvisibility = this.hasInvisibilityPotions();
        this.supplyTookFood = false;
        this.sendCommand(home);
        this.supplyHomeCommandSent = true;
        this.state = State.SUPPLY_WAIT_JOIN;
        this.stateTimer.reset();
    }

    private void closeScreen() {
        if (mc.currentScreen != null && !this.autoEatingFood && !this.autoDrinkingInvisibility) {
            mc.player.closeHandledScreen();
        }
    }

    private void disableAutoJump() {
        if (mc.player == null) return;
        mc.options.jumpKey.setPressed(false);
    }

    private void switchToSlot(int slot) {
        if (mc.player == null || slot < 0 || slot > 8) return;
        if (mc.player.getInventory().selectedSlot != slot) {
            mc.player.getInventory().selectedSlot = slot;
            mc.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(slot));
        }
    }

    private int findHotbarSlot(Predicate<ItemStack> predicate) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (predicate.test(mc.player.getInventory().getStack(i))) return i;
        }
        return -1;
    }

    private boolean isSupplyTimedOut() {
        return this.supplyStartedAt > 0L && System.currentTimeMillis() - this.supplyStartedAt > SUPPLY_TIMEOUT_MS;
    }

    private boolean isSupplyChestNear() {
        return this.supplyChest != null && this.isNearChest(this.supplyChest);
    }

    private String getHouseHome() { return this.getSettingText(this.warehouseHome); }
    private String getFarmHome() { return this.getSettingText(this.lootHome); }
    private String getWarehouseHomeCommand() { return this.getSettingWithPrefix(this.warehouse, "sethome "); }

    private String getSettingText(StringSetting setting) {
        String val = setting.getText();
        if (val == null || val.isBlank()) return "";
        String trimmed = val.trim();
        if (trimmed.startsWith("/")) trimmed = trimmed.substring(1).trim();
        if (trimmed.regionMatches(true, 0, CLAN_HOME_PREFIX, 0, CLAN_HOME_PREFIX.length())) return trimmed;
        return CLAN_HOME_PREFIX + trimmed;
    }

    private String getSettingWithPrefix(StringSetting setting, String prefix) {
        String val = setting.getText();
        if (val == null || val.isBlank()) return "";
        String trimmed = val.trim();
        if (trimmed.startsWith("/")) trimmed = trimmed.substring(1).trim();
        if (trimmed.regionMatches(true, 0, prefix, 0, prefix.length())) return trimmed;
        return prefix + trimmed;
    }

    private long getOpenRetryDelay() { return Math.max(0, (long) this.openRetryDelay.getValue()); }
    private long getRejoinLead() { return Math.max(500L, (long) this.rejoinLead.getValue()); }
    private long getMaxWaitTime() {
        try { return Math.max(1L, Long.parseLong(this.exitThreshold.getText())) * 1000L; }
        catch (Exception e) { return 60000L; }
    }
    private long calculateOpenTime(long timeLeft) { return System.currentTimeMillis() + Math.max(0L, timeLeft) * 1000L + 1000L; }

    private String formatTime(long millis) {
        if (millis <= 0L) return "READY";
        long sec = (millis + 999L) / 1000L;
        return String.format(Locale.ROOT, "%02d:%02d", sec / 60, sec % 60);
    }

    private String formatSessionTime() {
        if (this.sessionStartedAt <= 0L) return "00:00";
        long elapsed = System.currentTimeMillis() - this.sessionStartedAt;
        long sec = elapsed / 1000L;
        long min = sec / 60;
        long hr = min / 60;
        if (hr > 0) return String.format(Locale.ROOT, "%dh %02dm", hr, min % 60);
        return String.format(Locale.ROOT, "%02d:%02d", min, sec % 60);
    }

    private String cleanString(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replace("minecraft:", "").replace('_', ' ').trim();
    }

    private String normalizeString(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replace("minecraft:", "").replace('_', ' ').trim();
    }

    private void configureBaritone() {
        try {
            if (this.previousBaritoneFreeLook == null) {
                this.previousBaritoneFreeLook = (Boolean) BaritoneAPI.getSettings().freeLook.value;
                this.previousBaritoneRightClickContainerOnArrival = (Boolean) BaritoneAPI.getSettings().rightClickContainerOnArrival.value;
            }
            BaritoneAPI.getSettings().freeLook.value = false;
            BaritoneAPI.getSettings().rightClickContainerOnArrival.value = false;
        } catch (Throwable ignored) {}
    }

    private void restoreBaritoneSettings() {
        try {
            if (this.previousBaritoneFreeLook != null) {
                BaritoneAPI.getSettings().freeLook.value = this.previousBaritoneFreeLook;
                BaritoneAPI.getSettings().rightClickContainerOnArrival.value = this.previousBaritoneRightClickContainerOnArrival;
            }
        } catch (Throwable ignored) {}
        this.previousBaritoneFreeLook = null;
        this.previousBaritoneRightClickContainerOnArrival = null;
    }

    private IBaritone getBaritone() {
        try { return BaritoneAPI.getProvider().getPrimaryBaritone(); }
        catch (Throwable t) { return null; }
    }

    private void stopEatingFood() {
        if (!this.autoEatingFood) { this.resetFoodState(); return; }
        if (this.previousFoodSlot != -1) this.switchToSlot(this.previousFoodSlot);
        this.resetFoodState();
    }

    private void stopDrinkingInvisibility() {
        if (!this.autoDrinkingInvisibility) { this.resetInvisibilityState(); return; }
        if (this.previousInvisibilitySlot != -1) this.switchToSlot(this.previousInvisibilitySlot);
        this.resetInvisibilityState();
    }

    private void resetFoodState() {
        this.autoEatingFood = false;
        this.previousFoodSlot = -1;
        this.foodHotbarSlot = -1;
        this.foodUseDelayTicks = 0;
        this.foodUseTimer.reset();
    }

    private void resetInvisibilityState() {
        this.autoDrinkingInvisibility = false;
        this.previousInvisibilitySlot = -1;
        this.invisibilityHotbarSlot = -1;
        this.invisibilityUseDelayTicks = 0;
        this.invisibilityUseTimer.reset();
    }

    private class HologramData {
        final BlockPos pos;
        long openTime;
        double distance;
        HologramData(BlockPos pos, long openTime, double distance) {
            this.pos = pos;
            this.openTime = openTime;
            this.distance = distance;
        }
    }

    private enum State {
        IDLE,
        SCAN_NEXT,
        SCAN_WAIT_JOIN,
        SCAN_FIND_HOLOGRAM,
        SCAN_PATHING,
        SCAN_READ_HOLOGRAM,
        HUB_WAITING,
        ARENA_SET_HOME,
        ARENA_OPEN,
        ARENA_WAIT_RETURN,
        ARENA_RETURN,
        ARENA_RETURN_WAIT,
        RUSH_JOIN,
        RUSH_PATH,
        WAIT_OPEN,
        LOOTING,
        SUPPLY_WAIT_JOIN,
        SUPPLY_PATHING,
        SUPPLY_OPEN,
        SUPPLY_TAKE,
        CLAN_STORAGE_OPEN,
        CLAN_STORAGE_DEPOSIT,
        CLAN_STORAGE_WITHDRAW_OPEN,
        CLAN_STORAGE_WITHDRAW,
        GO_WAREHOUSE,
        RETURN_FARM_HOME,
        WAREHOUSE_WAIT_JOIN,
        WAREHOUSE_FIND_CHEST,
        WAREHOUSE_PATHING,
        WAREHOUSE_OPEN,
        DEPOSITING
    }
}

