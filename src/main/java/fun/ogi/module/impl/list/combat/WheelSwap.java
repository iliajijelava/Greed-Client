package fun.ogi.module.impl.list.combat;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventHud;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.KeySetting;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.util.NotificationManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@ModuleInformation(moduleName = "Wheel Swap", moduleDesc = "Quick potion wheel", moduleCategory = ModuleCategory.COMBAT)
public class WheelSwap extends Module {

   private final KeySetting wheelKey = new KeySetting("Wheel Key", this, -1);

   private final ModeSetting slot1 = new ModeSetting("Slot 1", this, "Assasinka", "Assasinka", "Voda", "Palladinka", "Radiacia", "Snotvornoe", "Xlopushka");
   private final ModeSetting slot2 = new ModeSetting("Slot 2", this, "Assasinka", "Assasinka", "Voda", "Palladinka", "Radiacia", "Snotvornoe", "Xlopushka");
   private final ModeSetting slot3 = new ModeSetting("Slot 3", this, "Assasinka", "Assasinka", "Voda", "Palladinka", "Radiacia", "Snotvornoe", "Xlopushka");
   private final ModeSetting slot4 = new ModeSetting("Slot 4", this, "Assasinka", "Assasinka", "Voda", "Palladinka", "Radiacia", "Snotvornoe", "Xlopushka");
   private final ModeSetting slot5 = new ModeSetting("Slot 5", this, "Assasinka", "Assasinka", "Voda", "Palladinka", "Radiacia", "Snotvornoe", "Xlopushka");
   private final ModeSetting slot6 = new ModeSetting("Slot 6", this, "Assasinka", "Assasinka", "Voda", "Palladinka", "Radiacia", "Snotvornoe", "Xlopushka");

   private boolean wheelOpen = false;
   private int selectedIndex = -1;
   private boolean wasLeftDown = false;
   private boolean wasRightDown = false;
   private boolean prevKeyDown = false;

   private final int[] foundSlots = new int[6];
   private final int[] foundCounts = new int[6];

   private static final float OUTER_R = 75f;
   private static final float INNER_R = 50f;

   public WheelSwap() {
      addSettings(wheelKey, slot1, slot2, slot3, slot4, slot5, slot6);
   }

   private String getSlotValue(int index) {
      return switch (index) {
         case 0 -> slot1.getValue();
         case 1 -> slot2.getValue();
         case 2 -> slot3.getValue();
         case 3 -> slot4.getValue();
         case 4 -> slot5.getValue();
         case 5 -> slot6.getValue();
         default -> "Assasinka";
      };
   }

   private String getDisplayName(String key) {
      return switch (key) {
         case "Assasinka" -> "[★] Зелье Ассасина";
         case "Voda" -> "[★] Святая Вода";
         case "Palladinka" -> "[★] Зелье Палладина";
         case "Radiacia" -> "[★] Радиация";
         case "Snotvornoe" -> "[★] Снотворное";
         case "Xlopushka" -> "[★] Хлопушка";
         default -> "";
      };
   }

   private ItemStack createStack(String key) {
      ItemStack stack = Items.SPLASH_POTION.getDefaultStack();
      stack.set(DataComponentTypes.ITEM_NAME, Text.literal(getDisplayName(key)));
      int color = getPotionColor(key);
      stack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.of(color), List.of(), Optional.empty()));
      return stack;
   }

   private int getPotionColor(String key) {
      return switch (key) {
         case "Assasinka" -> 0x2D2D2D;
         case "Voda" -> 0xFFFFFF;
         case "Palladinka" -> 0x55CCFF;
         case "Radiacia" -> 0xAAFF00;
         case "Snotvornoe" -> 0x2A2A5E;
         case "Xlopushka" -> 0xFF69B4;
         default -> 0xFFFFFF;
      };
   }

    @Subscribe
   public void onUpdate(EventUpdate e) {
      if (mc.player == null) return;

      boolean keyDown = wheelKey.isKeyDown();

      if (!wheelOpen && keyDown && !prevKeyDown) {
         if (mc.currentScreen != null) return;
         wheelOpen = true;
         scanInventory();
         mc.mouse.unlockCursor();
         prevKeyDown = true;
         return;
      }

      if (wheelOpen) {
         if (!keyDown) {
            closeWheel();
            return;
         }

         long window = mc.getWindow().getHandle();
         boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
         boolean rightDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

         if (leftDown && !wasLeftDown && selectedIndex != -1) {
            usePotion(selectedIndex);
            closeWheel();
            return;
         }

         if (rightDown && !wasRightDown && selectedIndex != -1) {
            setSlotToDefault(selectedIndex);
         }

         wasLeftDown = leftDown;
         wasRightDown = rightDown;
      }

      prevKeyDown = keyDown;
   }

   private void scanInventory() {
      Arrays.fill(foundSlots, -1);
      Arrays.fill(foundCounts, 0);

      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.isEmpty()) continue;

         String name = stack.getName().getString().toLowerCase().replaceAll("§.", "").trim();

         for (int s = 0; s < 6; s++) {
            String slotType = getSlotValue(s);
            String displayName = getDisplayName(slotType).toLowerCase().replaceAll("§.", "").trim();
            if (name.contains(displayName) || name.contains(slotType.toLowerCase())) {
               if (foundSlots[s] == -1) {
                  foundSlots[s] = i;
               }
               foundCounts[s]++;
            }
         }
      }
   }

   private void usePotion(int slotIndex) {
      if (mc.player == null || mc.interactionManager == null) return;
      int invSlot = foundSlots[slotIndex];
      if (invSlot == -1) return;

      if (invSlot >= 9 && invSlot <= 35) {
         mc.interactionManager.clickSlot(
            mc.player.currentScreenHandler.syncId,
            invSlot,
            mc.player.getInventory().selectedSlot,
            SlotActionType.SWAP,
            mc.player
         );
      } else {
         mc.player.getInventory().selectedSlot = invSlot;
      }

      mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
      NotificationManager.post("Used " + getSlotValue(slotIndex), 1, 1500);

      scanInventory();
   }

   private void setSlotToDefault(int index) {
      switch (index) {
         case 0 -> slot1.setIndex(0);
         case 1 -> slot2.setIndex(0);
         case 2 -> slot3.setIndex(0);
         case 3 -> slot4.setIndex(0);
         case 4 -> slot5.setIndex(0);
         case 5 -> slot6.setIndex(0);
      }
   }

   private void closeWheel() {
      wheelOpen = false;
      selectedIndex = -1;
      wasLeftDown = false;
      wasRightDown = false;
      mc.mouse.lockCursor();
   }

   @Subscribe
   public void onRender(EventHud event) {
      if (!wheelOpen || mc.player == null) return;

      DrawContext context = event.getDrawContext();
      Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

      float cx = mc.getWindow().getScaledWidth() / 2f;
      float cy = mc.getWindow().getScaledHeight() / 2f;

      double mx = mc.mouse.getX() * mc.getWindow().getScaledWidth() / (double) mc.getWindow().getWidth();
      double my = mc.mouse.getY() * mc.getWindow().getScaledHeight() / (double) mc.getWindow().getHeight();

      int n = 6;
      selectedIndex = getHoverIndex((float) mx, (float) my, cx, cy);

      for (int i = 0; i < n; i++) {
         boolean hovered = (i == selectedIndex);
         boolean hasItem = foundSlots[i] != -1;
         double slice = Math.PI * 2.0 / n;
         double startAngle = i * slice;
         double endAngle = startAngle + slice;

         Color segColor;
         Color borderC;
         if (!hasItem) {
            segColor = new Color(60, 30, 30, 80);
            borderC = new Color(100, 40, 40, 60);
         } else if (hovered) {
            segColor = new Color(255, 255, 255, 50);
            borderC = new Color(100, 180, 255, 200);
         } else {
            segColor = new Color(30, 35, 30, 100);
            borderC = new Color(255, 255, 255, 40);
         }

         drawRadialSegment(matrix, cx, cy, INNER_R, OUTER_R, startAngle, endAngle, segColor, borderC);

         double midAngle = startAngle + slice / 2.0;
         float iconDist = INNER_R + (OUTER_R - INNER_R) / 2f;
         float itemX = cx + (float) Math.cos(midAngle) * iconDist;
         float itemY = cy + (float) Math.sin(midAngle) * iconDist;

         String key = getSlotValue(i);
         ItemStack stack = createStack(key);
         context.getMatrices().push();
         context.getMatrices().translate(itemX - 8f, itemY - 8f, 0);
         context.drawItem(stack, 0, 0);
         context.getMatrices().pop();

         if (foundCounts[i] > 1) {
            context.drawText(mc.textRenderer, "x" + foundCounts[i], (int) (itemX + 3f), (int) (itemY + 5f), 0xFFFFAA00, true);
         }
      }

      float cs = 3f;
      Color crossColor = new Color(255, 255, 255, 180);
      drawLine(matrix, cx - cs, cy, cx + cs, cy, crossColor);
      drawLine(matrix, cx, cy - cs, cx, cy + cs, crossColor);
   }

   private int getHoverIndex(float mouseX, float mouseY, float cx, float cy) {
      float dx = mouseX - cx;
      float dy = mouseY - cy;
      float dist = MathHelper.sqrt(dx * dx + dy * dy);
      if (dist < INNER_R || dist > OUTER_R) return -1;

      double ang = Math.atan2(dy, dx);
      if (ang < 0) ang += Math.PI * 2.0;

      int idx = (int) (ang / (Math.PI * 2.0 / 6.0));
      return (idx < 0 || idx >= 6) ? -1 : idx;
   }

   private void drawLine(Matrix4f matrix, float x1, float y1, float x2, float y2, Color color) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
      buffer.vertex(matrix, x1, y1, 0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
      buffer.vertex(matrix, x2, y2, 0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.disableBlend();
   }

   private void drawRadialSegment(Matrix4f matrix, float cx, float cy, float innerR, float outerR, double startAngle, double endAngle, Color bg, Color border) {
      int steps = 24;
      double delta = (endAngle - startAngle) / steps;

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder builder = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
      for (int i = 0; i <= steps; i++) {
         double a = startAngle + i * delta;
         float cos = (float) Math.cos(a);
         float sin = (float) Math.sin(a);
         builder.vertex(matrix, cx + cos * outerR, cy + sin * outerR, 0).color(bg.getRed(), bg.getGreen(), bg.getBlue(), bg.getAlpha());
         builder.vertex(matrix, cx + cos * innerR, cy + sin * innerR, 0).color(bg.getRed(), bg.getGreen(), bg.getBlue(), bg.getAlpha());
      }
      BufferRenderer.drawWithGlobalProgram(builder.end());

      BufferBuilder lineBuilder = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
      for (int i = 0; i <= steps; i++) {
         double a = startAngle + i * delta;
         float cos = (float) Math.cos(a);
         float sin = (float) Math.sin(a);
         lineBuilder.vertex(matrix, cx + cos * outerR, cy + sin * outerR, 0).color(border.getRed(), border.getGreen(), border.getBlue(), border.getAlpha());
      }
      for (int i = steps; i >= 0; i--) {
         double a = startAngle + i * delta;
         float cos = (float) Math.cos(a);
         float sin = (float) Math.sin(a);
         lineBuilder.vertex(matrix, cx + cos * innerR, cy + sin * innerR, 0).color(border.getRed(), border.getGreen(), border.getBlue(), border.getAlpha());
      }
      BufferRenderer.drawWithGlobalProgram(lineBuilder.end());
   }
}

