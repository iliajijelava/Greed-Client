package fun.ogi.module.impl.list.misc;

import com.google.common.eventbus.Subscribe;
import fun.ogi.Cheap;
import fun.ogi.command.Command;
import fun.ogi.command.impl.AutoPilotCommand;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.impl.list.player.Stealer;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.StringSetting;
import fun.ogi.util.chatutil.ChatUtil;
import fun.ogi.util.baritone.BaritoneHelper;
import fun.ogi.util.funevents.FunEvent;
import fun.ogi.util.funevents.FunEventLocation;
import fun.ogi.util.funevents.FunEventsUtil;
import fun.ogi.util.time.Timer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@ModuleInformation(moduleName = "Auto Event", moduleDesc = "Automatically farms events like britva", moduleCategory = ModuleCategory.MISC)
public class AutoEvent extends PveModule {

    
    
    private static final Set<String> EXCLUDED_EVENTS = Set.of("Адская резня", "Сундук смерти");

    private static final int MAX_FLY_WAIT_ATTEMPTS = 15;

    private final ModeSetting eventType = new ModeSetting("Event Type", this, "System", "System");
    private final StringSetting anBase = new StringSetting("Base anarch moment",this, "109");

    private final List<FunEvent> funEvents = new CopyOnWriteArrayList<>();
    private final Random random = new Random();

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> updateTask;

    private status state;
    private FunEvent selectedEvent;
    private int waitTicks = 0;
    private int flyWaitAttempts = 0;
    private boolean waitingMessageSent = false;

    private LootPhase lootPhase;
    private BlockPos enderChestPos;

    private final Timer depositTimer = new Timer();
    private boolean baseAnarchySwitched;
    private boolean baseGotoSent;
    private BlockPos baseChestPos;

    private enum LootPhase {
        HOME, HOME_WAIT, FIND_CHEST, WALK_TO_CHEST, OPEN_CHEST, LOOT_CHEST
    }

    public AutoEvent() {
        addSettings(eventType,anBase);
    }

    @Override
    public void onEnable() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AutoEvent-Fetch");
            t.setDaemon(true);
            return t;
        });

        long initialDelay = (long) (Math.random() * 5); 
        updateTask = scheduler.scheduleAtFixedRate(this::refreshEvents, initialDelay, 30, TimeUnit.SECONDS);

        this.state = status.STATUS_IDLE;
        this.selectedEvent = null;
        this.waitTicks = 0;
        this.flyWaitAttempts = 0;
        this.waitingMessageSent = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (updateTask != null) {
            updateTask.cancel(true);
            updateTask = null;
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        super.onDisable();
    }

    private void refreshEvents() {
        try {
            List<FunEvent> fetched = new FunEventsUtil().getAllEvents(type());

            boolean changed = fetched.size() != funEvents.size()
                    || !fetched.equals(funEvents);

            funEvents.clear();
            funEvents.addAll(fetched);

            if (changed && !funEvents.isEmpty()) {
                mc.execute(() -> {
                    if (!isEnabled()) return;
                    ChatUtil.sendMSG("§6[AutoEvent] Обновлено §f" + funEvents.size() + " §6ивентов:");
                    for (FunEvent event : funEvents) {
                        ChatUtil.sendMSG("§a" + event);
                    }
                });
            }
        } catch (IOException e) {
            if (Thread.currentThread().isInterrupted()) return;
            mc.execute(() -> ChatUtil.sendMSG("§c[AutoEvent] Ошибка: " + e.getMessage()));
        } catch (Exception e) {
            mc.execute(() -> ChatUtil.sendMSG("§c[AutoEvent] Неожиданная ошибка: " + e.getMessage()));
        }
    }

    public List<FunEvent> getEvents() {
        return funEvents;
    }

    @Subscribe
    private void onEventUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        switch (state) {
            case STATUS_IDLE -> {
                try {
                    if (!hasElytraEquipped()) {
                        equipElytra();
                        getFireworkInHA();
                    } else {
                        getFireworkInHA();
                    }
                    this.state = status.STATUS_GET_EVENTS;
                } catch (Exception e) {
                    sendMessage("§c[AutoEvent] " + e.getMessage());
                }
            }

            case STATUS_GET_EVENTS -> {
                List<FunEvent> candidates = filterEvents(funEvents);

                if (candidates.isEmpty()) {
                    
                    if (!waitingMessageSent) {
                        sendMessage("§7[AutoEvent] Подходящих ивентов пока нет, жду...");
                        waitingMessageSent = true;
                    }
                    return;
                }

                waitingMessageSent = false;
                selectedEvent = candidates.get(random.nextInt(candidates.size()));
                flyWaitAttempts = 0;
                sendMessage("§a[AutoEvent] Выбран ивент: §f" + selectedEvent);
                this.state = status.STATUS_SWITCH_ANARCHY;
            }

            case STATUS_SWITCH_ANARCHY -> {
                if (selectedEvent == null) {
                    this.state = status.STATUS_GET_EVENTS;
                    return;
                }

                String digits = selectedEvent.getServer() != null
                        ? selectedEvent.getServer().replaceAll("[^0-9]", "")
                        : "";

                if (digits.isEmpty()) {
                    sendMessage("§c[AutoEvent] Не удалось определить номер анархии для ивента, пропускаю");
                    selectedEvent = null;
                    this.state = status.STATUS_GET_EVENTS;
                    return;
                }

                sendChatCommand("an" + digits);

                waitTicks = 100; 
                this.state = status.STATUS_WAITING_EVENT;
            }

            case STATUS_WAITING_EVENT -> {
                sendChatCommand("/rtp");
                waitTicks = 40; 
                this.state = status.STATUS_WAIT_EVENT;
            }

            case STATUS_WAIT_EVENT -> {
                if (mc.player.currentScreenHandler == null || mc.currentScreen == null) {
                    waitTicks = 10;
                    return;
                }

                
                mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        14,
                        1,
                        SlotActionType.PICKUP,
                        mc.player
                );

                waitTicks = 20;
                this.state = status.STATUS_FLY_TO_EVENT;
            }

            case STATUS_FLY_TO_EVENT -> {
                if (selectedEvent == null) {
                    this.state = status.STATUS_GET_EVENTS;
                    return;
                }

                
                selectedEvent = refreshSelectedEvent(selectedEvent);

                if (!selectedEvent.hasCoordinates()) {
                    flyWaitAttempts++;
                    if (flyWaitAttempts > MAX_FLY_WAIT_ATTEMPTS) {
                        sendMessage("§c[AutoEvent] Координаты ивента так и не объявились, выбираю другой");
                        selectedEvent = null;
                        flyWaitAttempts = 0;
                        this.state = status.STATUS_GET_EVENTS;
                        return;
                    }
                    waitTicks = 20;
                    return;
                }

                flyWaitAttempts = 0;
                flyToEventViaBaritone(selectedEvent);

                this.state = status.STATUS_WAIT_ARRIVE;
            }

            case STATUS_WAIT_ARRIVE -> {
                if (selectedEvent == null) {
                    this.state = status.STATUS_GET_EVENTS;
                    return;
                }

                if (isAutopilotActive()) return;

                FunEventLocation loc = selectedEvent.getLocation();
                double dx = mc.player.getX() - loc.getX();
                double dz = mc.player.getZ() - loc.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);

                if (dist <= 5.0) {
                    sendChatCommand("/sethome");
                    sendMessage("§a[AutoEvent] Долетели до ивента, установлен /sethome");
                    this.state = status.STATUS_WAIT_CHESTS;
                } else {
                    sendMessage("§c[AutoEvent] Автопилот не долетел до ивента, выбираю другой");
                    selectedEvent = null;
                    this.state = status.STATUS_GET_EVENTS;
                }
            }
            case STATUS_WAIT_CHESTS -> {
                FunEventLocation loc = selectedEvent.getLocation();
                Cheap.getInstance().getCommandManager().handleCommand(".autopilot " +  + (loc.getX() + random.nextInt(100,300)) + (loc.getZ() + random.nextInt(100,300)));
                if(selectedEvent.getTimeLeftSeconds() <= 10){
                    useEnderPearl();
                    this.state = status.STATUS_LOOT_CHESTS;
                }
            }
            case STATUS_LOOT_CHESTS -> lootChests();
            case STATUS_GO_TO_BASE -> {
                if (!baseAnarchySwitched) {
                    sendChatCommand("/an" + getBaseAnarchy());
                    baseAnarchySwitched = true;
                    stateTimer.reset();
                    return;
                }

                if (!stateTimer.finished(40000L)) return;

                if (baseChestPos == null) {
                    baseChestPos = findNearestChest(30);
                    if (baseChestPos == null) {
                        sendMessage("§c[AutoEvent] Сундук на базе не найден");
                        resetGoToBase();
                        this.state = status.STATUS_IDLE;
                        return;
                    }
                    baseGotoSent = false;
                }

                if (!isChestOpen()) {
                    if (mc.player.squaredDistanceTo(baseChestPos.toCenterPos()) > 9.0) {
                        if (!baseGotoSent) {
                            walkTo(baseChestPos);
                            baseGotoSent = true;
                            stateTimer.reset();
                        } else if (stateTimer.finished(60000L)) {
                            sendMessage("§c[AutoEvent] Не смог подойти к сундуку на базе");
                            stopWalking();
                            resetGoToBase();
                            this.state = status.STATUS_IDLE;
                        }
                        return;
                    }
                    stopWalking();
                    openContainer(baseChestPos, 30L);
                    return;
                }

                if (depositResources()) {
                    sendMessage("§a[AutoEvent] Ресурсы сданы в сундук");
                    stopWalking();
                    if (mc.player.currentScreenHandler != null) {
                        mc.player.closeHandledScreen();
                    }
                    resetGoToBase();
                    this.state = status.STATUS_IDLE;
                }
            }
            default -> {
                
            }
        }
    }
    private int getBaseAnarchy(){
        String txt = anBase.getValueAsString();
        return Integer.parseInt(txt);
    }

    private boolean depositResources() {
        if (!isChestOpen() || mc.player == null || mc.interactionManager == null) return false;

        Stealer stealer = Cheap.getInstance().getModuleStorage().get(Stealer.class);
        if (stealer != null && stealer.isEnabled()) {
            stealer.setEnabled(false);
        }

        if (!depositTimer.finished(150L)) return false;
        depositTimer.reset();

        ScreenHandler handler = mc.player.currentScreenHandler;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty() || shouldKeepForBase(stack)) continue;

            int handlerSlot = findPlayerInventorySlot(handler, i);
            if (handlerSlot == -1) continue;

            mc.interactionManager.clickSlot(handler.syncId, handlerSlot, 0, SlotActionType.QUICK_MOVE, mc.player);
            return false;
        }

        return true;
    }

    private boolean shouldKeepForBase(ItemStack stack) {
        if (stack.getItem() == Items.ELYTRA) return true;
        if (stack.getItem() == Items.FIREWORK_ROCKET) return true;
        if (stack.getItem() == Items.ENDER_PEARL) return true;
        return stack.get(DataComponentTypes.FOOD) != null;
    }

    private void resetGoToBase() {
        baseAnarchySwitched = false;
        baseGotoSent = false;
        baseChestPos = null;
    }
    private void lootChests() {
        if (lootPhase == null) lootPhase = LootPhase.HOME;

        switch (lootPhase) {
            case HOME -> {
                stopAutopilot();
                sendChatCommand("/home");
                stateTimer.reset();
                lootPhase = LootPhase.HOME_WAIT;
            }
            case HOME_WAIT -> {
                if (stateTimer.finished(8000) && mc.player.isOnGround()) {
                    lootPhase = LootPhase.FIND_CHEST;
                }
            }
            case FIND_CHEST -> {
                enderChestPos = findNearestEnderChest(64.0);
                if (enderChestPos == null) {
                    sendMessage("§c[AutoEvent] Эндер-сундук дома не найден");
                    endLoot();
                    return;
                }
                stateTimer.reset();
                lootPhase = LootPhase.WALK_TO_CHEST;
            }
            case WALK_TO_CHEST -> {
                if (stateTimer.finished(20000)) {
                    sendMessage("§c[AutoEvent] Не смог подойти к эндер-сундуку");
                    endLoot();
                    return;
                }
                if (mc.player.squaredDistanceTo(enderChestPos.toCenterPos()) < 6.25) {
                    stopWalking();
                    lootPhase = LootPhase.OPEN_CHEST;
                    return;
                }
                walkTo(enderChestPos);
            }
            case OPEN_CHEST -> {
                if (isChestOpen()) {
                    lootPhase = LootPhase.LOOT_CHEST;
                    return;
                }
                openContainer(enderChestPos, 40);
            }
            case LOOT_CHEST -> {
                Stealer stealer = Cheap.getInstance().getModuleStorage().get(Stealer.class);
                if (!isChestOpen()) {
                    if (stealer != null && stealer.isEnabled()) {
                        stealer.setEnabled(false);
                    }
                    sendMessage("§a[AutoEvent] Эндер-сундук вылутан");
                    endLoot();
                    return;
                }
                if (stealer != null && !stealer.isEnabled()) {
                    stealer.setEnabled(true);
                }
            }
        }
    }

    private void walkTo(BlockPos target) {
        if (detectBaritone()) {
            if (!BaritoneHelper.isPathing()) {
                BaritoneHelper.pathToNear(target, 2);
            }
            return;
        }
        lookAt(target);
        mc.options.forwardKey.setPressed(true);
    }

    private void stopWalking() {
        if (mc.options != null) {
            mc.options.forwardKey.setPressed(false);
        }
        if (detectBaritone()) {
            BaritoneHelper.cancelGoal();
        }
    }

    private void stopAutopilot() {
        if (isAutopilotActive()) {
            Cheap.getInstance().getCommandManager().handleCommand(".autopilot stop");
        }
    }

    private BlockPos findNearestEnderChest(double range) {
        BlockPos origin = mc.player.getBlockPos();
        int r = (int) Math.ceil(range);
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos p = origin.add(x, y, z);
                    if (mc.world.getBlockEntity(p) instanceof EnderChestBlockEntity) {
                        double d = mc.player.squaredDistanceTo(p.toCenterPos());
                        if (d <= range * range && d < bestDist) {
                            bestDist = d;
                            best = p.toImmutable();
                        }
                    }
                }
            }
        }

        return best;
    }
    private BlockPos findNearestChest( double range) {
        BlockPos origin = mc.player.getBlockPos();
        int r = (int) Math.ceil(range);
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos p = origin.add(x, y, z);
                    if (mc.world.getBlockEntity(p) instanceof ChestBlockEntity) {
                        double d = mc.player.squaredDistanceTo(p.toCenterPos());
                        if (d <= range * range && d < bestDist) {
                            bestDist = d;
                            best = p.toImmutable();
                        }
                    }
                }
            }
        }

        return best;
    }


    private void endLoot() {
        stopWalking();
        Stealer stealer = Cheap.getInstance().getModuleStorage().get(Stealer.class);
        if (stealer != null && stealer.isEnabled()) {
            stealer.setEnabled(false);
        }
        lootPhase = null;
        enderChestPos = null;
        this.state = status.STATUS_GO_TO_BASE;
    }

    private List<FunEvent> filterEvents(List<FunEvent> source) {
        List<FunEvent> result = new ArrayList<>();
        for (FunEvent event : source) {
            if (EXCLUDED_EVENTS.contains(event.getIdRu())) continue;

            String loot = event.getLoot();
            if (loot == null || loot.isEmpty() || "null".equalsIgnoreCase(loot)) continue;

            result.add(event);
        }
        return result;
    }

    
    private FunEvent refreshSelectedEvent(FunEvent old) {
        if (old == null) return null;
        for (FunEvent e : funEvents) {
            if (e.getId() != null && e.getId().equals(old.getId())
                    && e.getServer() != null && e.getServer().equals(old.getServer())) {
                return e;
            }
        }
        return old;
    }

    private void sendChatCommand(String command) {
        if (mc.player != null) {
            mc.player.networkHandler.sendChatCommand(command.startsWith("/") ? command.substring(1) : command);
        }
    }

    private void flyToEventViaBaritone(FunEvent event) {
        int x = event.getLocation().getX();
        int z = event.getLocation().getZ();
        int y = event.getLocation().getY() + 30;
        Cheap.getInstance().getCommandManager().handleCommand(".autopilot " + x + y + z);
        sendMessage("§a[AutoEvent] Автопилот долетел до ивента: X: " + x + " Z: " + z);
    }

    private boolean isAutopilotActive() {
        for (Command cmd : Cheap.getInstance().getCommandManager().getCommands()) {
            if (cmd instanceof AutoPilotCommand autopilot && autopilot.isActive()) {
                return true;
            }
        }
        return false;
    }

    private void getFireworkInHA() {
        int slot = searchItemHotbar(Items.FIREWORK_ROCKET);
        if (slot == -1) {
            sendMessage("НУЖНЫ ФЕЕРВЕРКИ В ХОТБАРЕ");
            return;
        }
        mc.player.getInventory().selectedSlot = slot;
    }
    private void useEnderPearl() {
        assert mc.player != null;
        float prevPitch = mc.player.getPitch();
        int slot = searchItemHotbar(Items.ENDER_PEARL);
        if (slot == -1) {
            sendMessage("§c[AutoEvent] No Ender pearls in hotbar!");
            return;
        }
        mc.player.setPitch(-90);
        mc.player.getInventory().selectedSlot = slot;
        mc.interactionManager.interactItem(mc.player, net.minecraft.util.Hand.MAIN_HAND);
        mc.player.setPitch(prevPitch);
    }
    private void useFirework() {
        int slot = searchItemHotbar(Items.FIREWORK_ROCKET);
        if (slot == -1) {
            sendMessage("§c[AutoEvent] No fireworks in hotbar!");
            return;
        }

        mc.player.getInventory().selectedSlot = slot;
        mc.interactionManager.interactItem(mc.player, net.minecraft.util.Hand.MAIN_HAND);
    }

    private int searchItemHotbar(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) {
                return i;
            }
        }
        return -1;
    }

    private String type() {
        if (eventType.is("System")) return "system";
        if (eventType.is("User")) return "user";
        return "all";
    }

    private boolean hasElytraEquipped() {
        ItemStack chestplate = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        return chestplate.isOf(Items.ELYTRA) && chestplate.getDamage() < chestplate.getMaxDamage() - 10;
    }

    private void equipElytra() {
        int slot = findBestElytraSlot();
        if (slot == -1) {
            sendMessage("§c[BaseFinder] No usable elytra in inventory!");
            setEnabled(false);
            return;
        }

        ItemStack currentChestplate = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        boolean hasChestplate = !currentChestplate.isEmpty() && !currentChestplate.isOf(Items.ELYTRA);

        if (hasChestplate) {
            int emptySlot = -1;
            for (int i = 0; i < 36; i++) {
                if (mc.player.getInventory().getStack(i).isEmpty()) {
                    emptySlot = i;
                    break;
                }
            }

            if (emptySlot != -1) {
                swapChestWithSlot(emptySlot);
            }
        }

        swapChestWithSlot(slot);
        sendMessage("§a[AutoEvent] Elytra equipped automatically");
    }

    private int findBestElytraSlot() {
        int elytraSlot = -1;

        for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            ItemStack stack = mc.player.getInventory().main.get(i);
            if (stack.isOf(Items.ELYTRA) && stack.getDamage() < stack.getMaxDamage() - 10) {
                elytraSlot = i + 9;
                break;
            }
        }

        if (elytraSlot == -1) {
            for (int i = 0; i < mc.player.getInventory().offHand.size(); i++) {
                ItemStack stack = mc.player.getInventory().offHand.get(i);
                if (stack.isOf(Items.ELYTRA) && stack.getDamage() < stack.getMaxDamage() - 10) {
                    elytraSlot = 45;
                    break;
                }
            }
        }

        return elytraSlot;
    }

    private void swapChestWithSlot(int slot) {
        if (slot >= 0 && slot <= 8) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, slot, SlotActionType.SWAP, mc.player);
        } else if (slot >= 9 && slot <= 35) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 8, SlotActionType.SWAP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 8, SlotActionType.SWAP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 8, SlotActionType.SWAP, mc.player);
        }
    }

    private void sendMessage(String message) {
        ChatUtil.sendMSG(message);
    }

    public enum status {
        STATUS_IDLE,
        STATUS_WAITING_EVENT,
        STATUS_GET_EVENTS,
        STATUS_WAIT_EVENT,
        STATUS_SWITCH_ANARCHY,
        STATUS_FLY_TO_EVENT,
        STATUS_WAIT_ARRIVE,
        STATUS_WAIT_CHESTS,
        STATUS_LOOT_CHESTS,
        STATUS_LEAVE_FROM_EVENT,
        STATUS_GO_TO_BASE,
        STATUS_WAIT_RESOURCES,
        STATUS_WANT_RESOURCES,
    }
}