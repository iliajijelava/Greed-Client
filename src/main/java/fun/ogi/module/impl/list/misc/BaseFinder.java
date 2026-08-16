package fun.ogi.module.impl.list.misc;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.util.NotificationManager;
import fun.ogi.util.chatutil.ChatUtil;
import fun.ogi.util.time.Timer;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

@ModuleInformation(
   moduleName = "Base Finder",
   moduleDesc = "Scans the world with elytra and finds players",
   moduleCategory = ModuleCategory.MISC
)
public class BaseFinder extends Module {
   private final List<PlayerRecord> foundPlayers = new ArrayList<>();
   private final Random random = new Random();
   private final Timer fireworkTimer = new Timer();
   private final Timer climbFireworkTimer = new Timer();
   private final Timer stateTimer = new Timer();

   private BlockPos currentTarget;
   private final int minCoord = -2500;
   private final int maxCoord = 2500;
   private boolean isFirstRun = true;
   private final int targetHeight = 120;
   private final int fireworkInterval = 5000;
   private boolean reachedTargetHeight;
   private boolean elytraEquipped;
   private boolean isWaitingForElytra;

   private static class PlayerRecord {
      final String name;
      final BlockPos position;
      final Date foundTime;

      PlayerRecord(String name, BlockPos position) {
         this.name = name;
         this.position = position;
         this.foundTime = new Date();
      }
   }

   @Override
   public void onEnable() {
      super.onEnable();
      foundPlayers.clear();
      isFirstRun = true;
      reachedTargetHeight = false;
      elytraEquipped = false;
      isWaitingForElytra = false;
      currentTarget = null;
      fireworkTimer.reset();
      climbFireworkTimer.reset();
      stateTimer.reset();

      ChatUtil.sendMSG("§6========== Base Finder / Поиск баз ==========");
      ChatUtil.sendMSG("§e=== ENGLISH ===");
      ChatUtil.sendMSG("§b1. §fPut on an elytra and fireworks in your hotbar.");
      ChatUtil.sendMSG("§b2. §fEnable the module — it will automatically:");
      ChatUtil.sendMSG("§7    • Equip the elytra from your inventory");
      ChatUtil.sendMSG("§7    • Take off and fly up to Y=120");
      ChatUtil.sendMSG("§7    • Scan the area for other players");
      ChatUtil.sendMSG("§7    • Fly to random points within ±2500 blocks");
      ChatUtil.sendMSG("§7    • Use fireworks every 5 seconds to maintain speed");
      ChatUtil.sendMSG("§b3. §fFound players are saved to §epve-results/§f folder.");
      ChatUtil.sendMSG("§b4. §fModule avoids obstacles and maintains altitude.");
      ChatUtil.sendMSG("§e=== РУССКИЙ ===");
      ChatUtil.sendMSG("§b1. §fНадень элитру и положи фейерверки в хотбар.");
      ChatUtil.sendMSG("§b2. §fВключи модуль — он автоматически:");
      ChatUtil.sendMSG("§7    • Наденет элитру из инвентаря");
      ChatUtil.sendMSG("§7    • Взлетит и наберёт высоту Y=120");
      ChatUtil.sendMSG("§7    • Просканирует местность на наличие игроков");
      ChatUtil.sendMSG("§7    • Полетит в случайные точки в радиусе ±2500 блоков");
      ChatUtil.sendMSG("§7    • Будет использовать фейерверки каждые 5 секунд");
      ChatUtil.sendMSG("§b3. §fНайденные игроки сохраняются в папку §epve-results/§f.");
      ChatUtil.sendMSG("§b4. §fМодуль облетает препятствия и держит высоту.");
      ChatUtil.sendMSG("§6============================================");
   }

   @Override
   public void onDisable() {
      if (!foundPlayers.isEmpty()) saveResultsToFile();
      currentTarget = null;
      super.onDisable();
      sendMessage("§c[BaseFinder] Scanning stopped.");
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player == null || mc.world == null) return;

      try {
         if (!elytraEquipped) {
            if (!hasElytraEquipped()) {
               if (!isWaitingForElytra || stateTimer.finished(1000L)) {
                  equipElytra();
                  isWaitingForElytra = true;
                  stateTimer.reset();
               }
               return;
            }
            equipElytra();
            elytraEquipped = true;
            sendMessage("§a[BaseFinder] Elytra equipped, preparing for takeoff...");
            return;
         }

         if (!mc.player.isGliding()) {
            if (mc.player.isOnGround()) {
               mc.player.jump();
               return;
            }

            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            return;
         }

         if (!reachedTargetHeight) {
            climbToTargetHeight();
            return;
         }

         checkForPlayers();
         maintainAltitude();
         avoidCollisions();

         if (isFirstRun || currentTarget == null || isAtTarget()) {
            isFirstRun = false;
            setRandomTarget();
         }

         flyToTarget();
      } catch (Exception e) {
         sendMessage("§c[BaseFinder] Error: " + e.getMessage());
      }
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
      sendMessage("§a[BaseFinder] Elytra equipped automatically");
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

   private void climbToTargetHeight() {
      if (mc.player.getY() < targetHeight) {
         mc.player.setPitch(-45.0F);

         if (climbFireworkTimer.finished(1000L)) {
            useFirework();
            climbFireworkTimer.reset();
         }
      } else {
         reachedTargetHeight = true;
         mc.player.setPitch(0.0F);
         sendMessage("§a[BaseFinder] Reached target height of 120 blocks");
      }
   }

   private void maintainAltitude() {
      if (!mc.player.isGliding()) return;

      double y = mc.player.getY();
      if (y < targetHeight - 5) {
         mc.player.setPitch(-15.0F);
      } else if (y > targetHeight + 5) {
         mc.player.setPitch(5.0F);
      } else {
         mc.player.setPitch(0.0F);
      }
   }

   private void avoidCollisions() {
      if (!mc.player.isGliding()) return;

      Vec3d lookVec = mc.player.getRotationVec(1.0F);
      BlockPos checkPos = BlockPos.ofFloored(
         mc.player.getX() + lookVec.x * 15.0,
         mc.player.getY() + lookVec.y * 15.0,
         mc.player.getZ() + lookVec.z * 15.0
      );

      if (!mc.world.getBlockState(checkPos).isAir()) {
         mc.player.setYaw(mc.player.getYaw() + 45.0F);
         sendMessage("§e[BaseFinder] Avoiding obstacle");
      }
   }

   private boolean isAtTarget() {
      if (mc.player == null || currentTarget == null) return true;
      return mc.player.squaredDistanceTo(currentTarget.toCenterPos()) <= 225.0;
   }

   private void setRandomTarget() {
      int x = Math.min(maxCoord, Math.max(minCoord, random.nextInt(maxCoord - minCoord) + minCoord));
      int z = Math.min(maxCoord, Math.max(minCoord, random.nextInt(maxCoord - minCoord) + minCoord));
      currentTarget = new BlockPos(x, targetHeight, z);
      sendMessage("§b[BaseFinder] Flying to: X: " + x + " Z: " + z);
   }

   private void flyToTarget() {
      if (currentTarget == null || !mc.player.isGliding()) return;

      Vec3d targetVec = currentTarget.toCenterPos();
      Vec3d playerVec = mc.player.getPos();
      Vec3d direction = targetVec.subtract(playerVec).normalize();

      float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F;
      mc.player.setYaw(yaw);

      if (fireworkTimer.finished(fireworkInterval)) {
         useFirework();
         fireworkTimer.reset();
      }
   }

   private void useFirework() {
      int slot = searchItemHotbar(Items.FIREWORK_ROCKET);
      if (slot == -1) {
         sendMessage("§c[BaseFinder] No fireworks in hotbar!");
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

   private void checkForPlayers() {
      synchronized (foundPlayers) {
         for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            boolean exists = foundPlayers.stream()
               .anyMatch(r -> r.name.equals(player.getName().getString()));

            if (!exists) {
               PlayerRecord record = new PlayerRecord(
                  player.getName().getString(),
                  player.getBlockPos()
               );
               foundPlayers.add(record);

               String msg = String.format(
                  "§6[BaseFinder] Player found: §e%s §6at §bX: %d Y: %d Z: %d",
                  record.name,
                  record.position.getX(),
                  record.position.getY(),
                  record.position.getZ()
               );
               sendMessage(msg);
            }
         }
      }
   }

   private void saveResultsToFile() {
      String filename = "Eternall_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".txt";
      Path resultDir = mc.runDirectory.toPath().resolve("pve-results");
      Path resultFile = resultDir.resolve(filename);

      synchronized (foundPlayers) {
         try {
            Files.createDirectories(resultDir);
         } catch (IOException e) {
            sendMessage("§c[BaseFinder] Failed to create results directory: " + e.getMessage());
            return;
         }
         try (BufferedWriter writer = Files.newBufferedWriter(resultFile, StandardCharsets.UTF_8)) {
            writer.write("Found players - " + new Date() + "\n\n");

            for (PlayerRecord record : foundPlayers) {
               writer.write(String.format(
                  "Player: %-16s | Coords: X: %-6d Y: %-4d Z: %-6d | Time: %s%n",
                  record.name,
                  record.position.getX(),
                  record.position.getY(),
                  record.position.getZ(),
                  new SimpleDateFormat("HH:mm:ss").format(record.foundTime)
               ));
            }

            sendMessage("§a[BaseFinder] Results saved to: §e" + resultFile);
         } catch (IOException e) {
            sendMessage("§c[BaseFinder] Failed to save results: " + e.getMessage());
         }
      }
   }

   private void sendMessage(String message) {
      ChatUtil.sendMSG(message);
   }
}

