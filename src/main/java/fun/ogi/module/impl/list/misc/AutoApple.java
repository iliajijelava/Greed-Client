package fun.ogi.module.impl.list.misc;

import baritone.api.BaritoneAPI;
import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.util.NotificationManager;
import fun.ogi.util.chatutil.ChatUtil;
import fun.ogi.util.rotation.Rotation;
import fun.ogi.util.rotation.RotationComponent;

import fun.ogi.util.rotation.RotationUtil;
import fun.ogi.util.time.Timer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

@ModuleInformation(
   moduleName = "Auto Apple",
   moduleDesc = "Automatically grows trees and farms apples",
   moduleCategory = ModuleCategory.MISC
)
public class AutoApple extends Module {
   private static final String APPLE_LEAVES_TYPES = "oak_leaves spruce_leaves birch_leaves jungle_leaves acacia_leaves dark_oak_leaves cherry_leaves mangrove_leaves pale_oak_leaves";
   private static final String APPLE_LOG_TYPES = "oak_log spruce_log birch_log jungle_log acacia_log dark_oak_log cherry_log mangrove_log pale_oak_log";

   private final ModeSetting appleBreakMode = new ModeSetting("Apple break mode", this, "Stand", "Stand", "Baritone");
   private final BooleanSetting appleAutoStop = new BooleanSetting("Apple Auto Stop", this, true);
   private final SliderSetting appleActionDelay = new SliderSetting("Apple Action Delay (ms)", this, 1.0, 0.0, 500.0, 1.0);
   private final SliderSetting appleBreakTickDelay = new SliderSetting("Apple Break Tick Delay", this, 1.0, 0.0, 250.0, 1.0);
   private final SliderSetting appleToolSwitchDelay = new SliderSetting("Apple tool switch delay (ms)", this, 80.0, 0.0, 1000.0, 10.0);
   private final SliderSetting appleScanRadius = new SliderSetting("Apple scan radius", this, 6.0, 1.0, 10.0, 1.0);
   private final SliderSetting appleScanHeight = new SliderSetting("Apple scan height", this, 20.0, 1.0, 40.0, 1.0);
   private final SliderSetting appleMaxFarmDistance = new SliderSetting("Max farm distance", this, 5.0, 1.0, 12.0, 0.5);
   private final SliderSetting appleReachDistance = new SliderSetting("Reach distance", this, 5.5, 1.0, 6.0, 0.1);
   private final SliderSetting appleRotationYawStep = new SliderSetting("Rotation yaw", this, 180.0, 1.0, 180.0, 1.0);
   private final SliderSetting appleRotationPitchStep = new SliderSetting("Rotation pitch", this, 180.0, 1.0, 180.0, 1.0);
   private final SliderSetting appleBreakTolerance = new SliderSetting("Break tolerance", this, 20.0, 1.0, 45.0, 1.0);
   private final SliderSetting appleBonemealDelay = new SliderSetting("Bone meal delay", this, 150.0, 0.0, 1000.0, 10.0);

   private BlockPos appleFarmLocation;
   private BlockPos appleCurrentTargetBlock;
   private Direction appleTargetBlockSide;
   private boolean appleIsBaritoneMining;

   private final Timer actionTimer = new Timer();
   private final Timer breakTimer = new Timer();
   private final Timer notifyTimer = new Timer();

   public AutoApple() {
      addSettings(
         appleBreakMode, appleAutoStop,
         appleActionDelay, appleBreakTickDelay, appleToolSwitchDelay,
         appleScanRadius, appleScanHeight, appleMaxFarmDistance,
         appleReachDistance, appleRotationYawStep, appleRotationPitchStep,
         appleBreakTolerance, appleBonemealDelay
      );
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
      this.autoapple();
   }

   @Override
   public void onEnable() {
      ChatUtil.sendMSG("§6========== Auto Apple / Авто Яблоко ==========");
      ChatUtil.sendMSG("§e=== ENGLISH ===");
      ChatUtil.sendMSG("§b1. §fStand next to a dirt, grass, coarse dirt, or rooted dirt block.");
      ChatUtil.sendMSG("§b2. §fPlace oak saplings and bone meal in your hotbar.");
      ChatUtil.sendMSG("§b3. §fPut an axe or hoe in your hotbar for chopping.");
      ChatUtil.sendMSG("§b4. §fEnable the module — it will automatically:");
      ChatUtil.sendMSG("§7    • Plant an oak sapling on the dirt block");
      ChatUtil.sendMSG("§7    • Apply bone meal to grow the tree instantly");
      ChatUtil.sendMSG("§7    • Chop down the tree (logs + leaves)");
      ChatUtil.sendMSG("§7    • Repeat the cycle");
      ChatUtil.sendMSG("§b5. §fTwo break modes:");
      ChatUtil.sendMSG("§7    • §eStand§7 — rotates and breaks blocks manually");
      ChatUtil.sendMSG("§7    • §eBaritone§7 — uses Baritone's #mine command");
      ChatUtil.sendMSG("§b6. §fAuto Stop — stops Baritone when tree is fully chopped.");
      ChatUtil.sendMSG("§b7. §fAdjust delays and distances in the settings if needed.");
      ChatUtil.sendMSG("§e=== РУССКИЙ ===");
      ChatUtil.sendMSG("§b1. §fВстань рядом с блоком земли, травы, грубой земли или корневища.");
      ChatUtil.sendMSG("§b2. §fПоложи саженцы дуба и костную муку в хотбар.");
      ChatUtil.sendMSG("§b3. §fПоложи топор или мотыгу в хотбар для рубки.");
      ChatUtil.sendMSG("§b4. §fВключи модуль — он автоматически:");
      ChatUtil.sendMSG("§7    • Посадит саженец дуба на землю");
      ChatUtil.sendMSG("§7    • Применит костную муку для мгновенного роста дерева");
      ChatUtil.sendMSG("§7    • Срубит дерево (брёвна + листву)");
      ChatUtil.sendMSG("§7    • Повторит цикл");
      ChatUtil.sendMSG("§b5. §fДва режима рубки:");
      ChatUtil.sendMSG("§7    • §eStand§7 — самостоятельно поворачивается и ломает блоки");
      ChatUtil.sendMSG("§7    • §eBaritone§7 — использует команду #mine из Baritone");
      ChatUtil.sendMSG("§b6. §fАвто-стоп — останавливает Baritone когда дерево полностью срублено.");
      ChatUtil.sendMSG("§b7. §fНастрой задержки и дистанцию в настройках модуля при необходимости.");
      ChatUtil.sendMSG("§6==============================================");
      super.onEnable();
   }

   @Override
   public void onDisable() {
      if (this.appleIsBaritoneMining && this.appleAutoStop.getValue()) {
         this.stopBaritone();
      }
      this.appleIsBaritoneMining = false;
      this.appleFarmLocation = null;
      this.appleCurrentTargetBlock = null;
      this.appleTargetBlockSide = null;
      super.onDisable();
   }

   public void autoapple() {
      if (!this.actionTimer.finished((long) appleActionDelay.getValue())) return;

      if (this.appleFarmLocation == null) {
         List<BlockPos> dirtBlocks = findAppleDirtBlocks();
         if (dirtBlocks.isEmpty()) {
            this.warn("Auto Apple", "Block of dirt not found! Stand near it.");
            return;
         }
         this.appleFarmLocation = dirtBlocks.get(0);
         ChatUtil.sendMSG("Farm point set: " + this.appleFarmLocation.toShortString());
      }

      if (mc.player.getPos().distanceTo(this.appleFarmLocation.toCenterPos()) > appleMaxFarmDistance.getValue()) {
         this.warn("Auto Apple", "You are too far from the farm point!");
         return;
      }

      BlockPos saplingPos = this.appleFarmLocation.up();
      BlockState state = mc.world.getBlockState(saplingPos);

      if (state.isAir()) {
         if (this.appleIsBaritoneMining && this.appleAutoStop.getValue()) {
            this.stopBaritone();
            this.appleIsBaritoneMining = false;
         }

         if (this.ensureHotbarItem(stack -> stack.isOf(Items.OAK_SAPLING), appleToolSwitchDelay.getValue())) {
            this.useBlock(this.appleFarmLocation, Direction.UP, Hand.MAIN_HAND);
            this.actionTimer.reset();
         } else if (this.notifyTimer.finished(5000L)) {
            this.warn("Auto Apple", "No oak saplings!");
            this.notifyTimer.reset();
         }
         return;
      }

      if (state.isOf(Blocks.OAK_SAPLING)) {
         if (this.appleIsBaritoneMining && this.appleAutoStop.getValue()) {
            this.stopBaritone();
            this.appleIsBaritoneMining = false;
         }

         if (this.ensureHotbarItem(stack -> stack.isOf(Items.BONE_MEAL), appleToolSwitchDelay.getValue())) {
            if (this.actionTimer.finished((long) appleBonemealDelay.getValue())) {
               this.useBlock(saplingPos, Direction.UP, Hand.MAIN_HAND);
               this.actionTimer.reset();
            }
         } else if (this.notifyTimer.finished(5000L)) {
            this.warn("Auto Apple", "No bone meal!");
            this.notifyTimer.reset();
         }
         return;
      }

      if (this.isLog(saplingPos) || this.isLeaves(saplingPos)) {
         if (appleBreakMode.is("Baritone")) {
            if (!this.appleIsBaritoneMining) {
               this.runBaritoneCommand("#mine " + APPLE_LOG_TYPES + " " + APPLE_LEAVES_TYPES);
               this.appleIsBaritoneMining = true;
            }
         } else {
            BlockPos target = this.findBestBlockToChop(saplingPos);
            if (target != null) {
               this.appleCurrentTargetBlock = target;
               this.appleTargetBlockSide = this.getVisibleSide(target);

               if (this.appleTargetBlockSide != null) {
                  boolean toolReady = this.isLog(target)
                     ? this.ensureHotbarItem(stack -> stack.getItem() instanceof AxeItem, appleToolSwitchDelay.getValue())
                     : this.ensureHotbarItem(this::isAxeOrHoe, appleToolSwitchDelay.getValue());
                  if (!toolReady) {
                     this.warn("Auto Apple", "No suitable tool for chopping tree!");
                     return;
                  }
                  if (this.breakTimer.finished((long) appleBreakTickDelay.getValue())) {
                     this.aimAtBlock(target, this.appleTargetBlockSide);
                     if (this.rotateAndCheckAim(target, this.appleTargetBlockSide, appleBreakTolerance.getValue())) {
                        mc.interactionManager.attackBlock(target, this.appleTargetBlockSide);
                        mc.player.swingHand(Hand.MAIN_HAND);
                        this.breakTimer.reset();
                     }
                  }
               }
            } else {
               this.appleCurrentTargetBlock = null;
            }
         }
      } else {
         if (this.appleIsBaritoneMining && this.appleAutoStop.getValue()) {
            this.stopBaritone();
            this.appleIsBaritoneMining = false;
         }
         this.appleCurrentTargetBlock = null;
      }
   }

   private List<BlockPos> findAppleDirtBlocks() {
      List<BlockPos> list = new ArrayList<>();
      BlockPos feet = mc.player.getBlockPos();
      int r = (int) appleScanRadius.getValue();
      for (int x = -r; x <= r; x++) {
         for (int y = -3; y <= 2; y++) {
            for (int z = -r; z <= r; z++) {
               BlockPos p = feet.add(x, y, z);
               BlockState s = mc.world.getBlockState(p);
               BlockState above = mc.world.getBlockState(p.up());
               boolean existingFarm = above.isOf(Blocks.OAK_SAPLING) || this.isLog(p.up()) || this.isLeaves(p.up());
               boolean canPlant = above.isAir() && Blocks.OAK_SAPLING.getDefaultState().canPlaceAt(mc.world, p.up());
               if ((s.isOf(Blocks.DIRT) || s.isOf(Blocks.GRASS_BLOCK) || s.isOf(Blocks.COARSE_DIRT) || s.isOf(Blocks.ROOTED_DIRT))
                  && (existingFarm || canPlant)) {
                  list.add(p);
               }
            }
         }
      }
      list.sort(Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(p.toCenterPos())));
      return list;
   }

   private BlockPos findBestBlockToChop(BlockPos center) {
      BlockPos best = null;
      double minScore = Double.MAX_VALUE;
      int r = (int) appleScanRadius.getValue();
      int h = (int) appleScanHeight.getValue();

      for (int x = -r; x <= r; x++) {
         for (int y = 0; y <= h; y++) {
            for (int z = -r; z <= r; z++) {
               BlockPos p = center.add(x, y, z);
               if (this.isLog(p) || this.isLeaves(p)) {
                  double distSq = mc.player.getEyePos().squaredDistanceTo(p.toCenterPos());
                  if (distSq <= appleReachDistance.getValue() * appleReachDistance.getValue()) {
                     if (distSq < minScore) {
                        minScore = distSq;
                        best = p.toImmutable();
                     }
                  }
               }
            }
         }
      }
      return best;
   }

   private Direction getVisibleSide(BlockPos pos) {
      Vec3d eyePos = mc.player.getEyePos();
      for (Direction side : Direction.values()) {
         Vec3d sideCenter = Vec3d.ofCenter(pos).add(Vec3d.of(side.getVector()).multiply(0.5));
         BlockHitResult result = mc.world.raycast(new RaycastContext(eyePos, sideCenter, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
         if (result != null && result.getType() == BlockHitResult.Type.BLOCK && result.getBlockPos().equals(pos)) {
            return side;
         }
      }


      Vec3d diff = eyePos.subtract(Vec3d.ofCenter(pos));
      Direction fallback = Direction.UP;
      float maxDot = Float.NEGATIVE_INFINITY;
      for (Direction side : Direction.values()) {
         Vec3d v = Vec3d.of(side.getVector());
         float dot = (float) (diff.x * v.x + diff.y * v.y + diff.z * v.z);
         if (dot > maxDot) {
            maxDot = dot;
            fallback = side;
         }
      }
      return fallback;
   }

   private void aimAtBlock(BlockPos pos, Direction side) {
      Vec3d hitVec = Vec3d.ofCenter(pos).add(Vec3d.of(side.getVector()).multiply(0.45));
      Rotation rotation = Rotation.fromVec3d(hitVec);
      RotationComponent.update(rotation, (float) appleRotationYawStep.getValue(), (float) appleRotationPitchStep.getValue(), 0, 100,1,1,true);
   }

   private boolean rotateAndCheckAim(BlockPos pos, Direction side, double tolerance) {
      Vec3d hitVec = Vec3d.ofCenter(pos).add(Vec3d.of(side.getVector()).multiply(0.45));
      Rotation rotation = Rotation.fromVec3d(hitVec);
      float yawDiff = Math.abs(RotationUtil.getAngleDifference(mc.player.getYaw(), rotation.getYaw()));
      float pitchDiff = Math.abs(rotation.getPitch() - mc.player.getPitch());
      return yawDiff <= tolerance && pitchDiff <= tolerance;
   }

   private boolean ensureHotbarItem(Predicate<ItemStack> predicate, double delay) {
      if (predicate.test(mc.player.getMainHandStack())) return true;
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (predicate.test(stack)) {
            mc.player.getInventory().selectedSlot = i;
            return true;
         }
      }
      return false;
   }

   private void useBlock(BlockPos pos, Direction dir, Hand hand) {
      Vec3d hitPos = Vec3d.ofCenter(pos);
      BlockHitResult hit = new BlockHitResult(hitPos, dir, pos, false);
      mc.interactionManager.interactBlock(mc.player, hand, hit);
   }

   private void stopBaritone() {
      BaritoneAPI.getProvider().getPrimaryBaritone().getMineProcess().cancel();
   }

   private void runBaritoneCommand(String cmd) {
      String[] blockNames = cmd.replace("#mine ", "").split(" ");
      BaritoneAPI.getProvider().getPrimaryBaritone().getMineProcess().mineByName(Integer.MAX_VALUE, blockNames);
   }

   private boolean isLog(BlockPos pos) {
      if (mc.world == null) return false;
      Block b = mc.world.getBlockState(pos).getBlock();
      return b == Blocks.OAK_LOG || b == Blocks.SPRUCE_LOG || b == Blocks.BIRCH_LOG
         || b == Blocks.JUNGLE_LOG || b == Blocks.ACACIA_LOG || b == Blocks.DARK_OAK_LOG
         || b == Blocks.CHERRY_LOG || b == Blocks.MANGROVE_LOG || b == Blocks.PALE_OAK_LOG;
   }

   private boolean isLeaves(BlockPos pos) {
      if (mc.world == null) return false;
      Block b = mc.world.getBlockState(pos).getBlock();
      return b == Blocks.OAK_LEAVES || b == Blocks.SPRUCE_LEAVES || b == Blocks.BIRCH_LEAVES
         || b == Blocks.JUNGLE_LEAVES || b == Blocks.ACACIA_LEAVES || b == Blocks.DARK_OAK_LEAVES
         || b == Blocks.CHERRY_LEAVES || b == Blocks.MANGROVE_LEAVES || b == Blocks.PALE_OAK_LEAVES;
   }

   private boolean isAxeOrHoe(ItemStack stack) {
      return stack.getItem() instanceof AxeItem || stack.getItem() instanceof HoeItem;
   }

   private void warn(String title, String text) {
      NotificationManager.post(title + ": " + text);
   }

   private void warn(String text) {
      NotificationManager.post(text);
   }
}

