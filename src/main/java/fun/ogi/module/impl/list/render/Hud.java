package fun.ogi.module.impl.list.render;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.Cheap;
import fun.ogi.command.impl.BindCommand;
import fun.ogi.events.EventMouse;
import fun.ogi.events.render.EventHud;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.module.theme.ThemeManager;
import fun.ogi.util.NotificationManager;
import fun.ogi.mixin.CooldownUtil;
import fun.ogi.util.animation.Animation;
import fun.ogi.util.animation.Easing;
import fun.ogi.util.chatutil.ChatUtil;
import fun.ogi.util.render.Draggable;
import fun.ogi.util.render.ShadowUtil;
import fun.ogi.util.render.builders.Builder;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.helper.ScissorUtils;
import fun.ogi.util.render.msdf.MsdfFont;
import fun.ogi.util.render.renderers.impl.BuiltBlur;
import fun.ogi.util.render.renderers.impl.BuiltRectangle;
import fun.ogi.util.render.renderers.impl.BuiltText;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.*;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.*;
import java.util.List;

@ModuleInformation(moduleName = "Hud", moduleCategory = ModuleCategory.RENDER)
public class Hud extends fun.ogi.module.Module {
    public BooleanSetting Pots = new BooleanSetting("Potion List", this, true);
    public BooleanSetting Keybinds = new BooleanSetting("Keybinds", this, true);
    public BooleanSetting armor = new BooleanSetting("Armor", this, true);
    public BooleanSetting watermark = new BooleanSetting("Watermark", this, true);
    public BooleanSetting targetHud = new BooleanSetting("Target HUD", this, true);
    public BooleanSetting staffList = new BooleanSetting("Staff List", this, true);
    public BooleanSetting cooldowns = new BooleanSetting("Cooldowns",this,true);
    public BooleanSetting notifications = new BooleanSetting("Notifications", this, true);
    public BooleanSetting moduleList = new BooleanSetting("Module List", this, true);

    public SliderSetting hudScale = new SliderSetting("HUD Scale", this, 1.0, 0.5, 1.5, 0.05);
    public SliderSetting targetHudScale = new SliderSetting("Target HUD Scale", this, 1.0, 0.5, 1.5, 0.05);
    public ModeSetting hudStyle = new ModeSetting("HUD Style", this, "Default", "Default", "Liquid Glass", "Solid");
    public ModeSetting hudMode = new ModeSetting("Hud Mode", this, "Default", "Default", "Macan","Old","YouGame");

    public final Draggable potionsDrag = new Draggable(5, 45, 100, 20);
    public final Draggable keybindsDrag = new Draggable(5, 5, 100, 20);
    public final Draggable armorDrag = new Draggable(5, 170, 100, 20);
    public final Draggable cooldownsDrag = new Draggable(75,120,100,20);
    private final Animation keybindsAnimation = new Animation();
    private final Animation potionsAnimation = new Animation();
    private final Animation armorAnimation = new Animation();
    private final Animation cooldownsAnimation = new Animation();
    private final Animation staffAnimation = new Animation();
    private boolean wasKeybindsVisible;
    private final Set<Integer> lowDuraNotified = new HashSet<>();
    private final Map<RegistryEntry<StatusEffect>, Integer> lastEffects = new HashMap<>();
    private static final Supplier<MsdfFont> BIKO_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
    private static final Supplier<MsdfFont> ICON_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("nur").data("nur").build());
    private static final Supplier<MsdfFont> RUSSIAN_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("regular_semibold").data("regular_semibold").build());
    private static final Supplier<MsdfFont> MACAN_ICONS_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("macan_icons").data("macan_icons").build());
    private static final Supplier<MsdfFont> MACAN_ICONS2_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("macan_icons2").data("macan_icons2").build());

    private int currentMouseX, currentMouseY;

    private float animatedCooldownNameBackWidth = -1f;
    private float animatedCooldownTimeBackWidth = -1f;
    private float animatedPotionNameBackWidth = -1f;
    private float animatedPotionTimeBackWidth = -1f;
    private float animatedKeybindNameBackWidth = -1f;
    private float animatedKeybindBindBackWidth = -1f;
    private long previousCooldownLayoutTime;
    private long previousPotionLayoutTime;
    private long previousKeybindLayoutTime;

    public final WaterMark waterMarkComp = new WaterMark();
    public final TargetHud targetHudComp = new TargetHud();
    public final StaffList staffListComp = new StaffList();
    public final ModuleList moduleListComp = new ModuleList();
    public final YouGameHud youGameComp = new YouGameHud();

    private static final float ROUNDING = 4.0f;
    private static final Color BG_COLOR = new Color(20, 20, 25, 255);
    private static final Color OUTLINE_COLOR = new Color(255, 255, 255, 255);
    private static final Color ACCENT_COLOR = new Color(50, 150, 255);
    private static final Color TEXT_COLOR = new Color(220, 220, 220);

    public Hud() {
        addSetting(Pots);
        addSetting(Keybinds);
        addSetting(armor);
        addSetting(watermark);
        addSetting(targetHud);
        addSetting(staffList);
        addSetting(cooldowns);
        addSetting(notifications);
        addSetting(moduleList);
//        addSetting(hudScale);
//        addSetting(targetHudScale);
        addSetting(hudStyle);
        addSetting(hudMode);
    }

    
    private String getEffectBaseName(StatusEffectInstance effect) {
        String name = effect.getEffectType().value().getName().getString();
        if (name.equals("Fire Resistance")) {
            name = "Ognestoi";
        }
        return name;
    }

    private String getEffectFullName(StatusEffectInstance effect) {
        String name = getEffectBaseName(effect);
        if (effect.getAmplifier() > 0) {
            name += " " + (effect.getAmplifier() + 1);
        }
        return name;
    }

    @Subscribe
    public void onEventHud(EventHud e) {

        if (mc.player == null || mc.world == null) return;
        float scale = (float) hudScale.getValue();
        e.getDrawContext().getMatrices().push();
        e.getDrawContext().getMatrices().scale(scale, scale, 1);
        currentMouseX = (int) (mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth() / scale);
        currentMouseY = (int) (mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight() / scale);
        float screenWidth = mc.getWindow().getScaledWidth() / scale;
        float screenHeight = mc.getWindow().getScaledHeight() / scale;


        potionsDrag.onDraw(currentMouseX, currentMouseY, screenWidth, screenHeight);
        keybindsDrag.onDraw(currentMouseX, currentMouseY, screenWidth, screenHeight);
        armorDrag.onDraw(currentMouseX, currentMouseY, screenWidth, screenHeight);
        cooldownsDrag.onDraw(currentMouseX,currentMouseY,screenWidth,screenHeight);
        if (notifications.getValue()) NotificationManager.draggable.onDraw(currentMouseX, currentMouseY, screenWidth, screenHeight);

        if (Pots.getValue()) {
            onPotion(e.getDrawContext());
        }
        if (Keybinds.getValue()) {
            onKeybinds(e.getDrawContext());
        }
        if(armor.getValue()){
            onArmor(e.getDrawContext());
        }
        String style = hudStyle.getValue();
        String mode = hudMode.getValue();
        if (watermark.getValue()) {
            waterMarkComp.render(e.getDrawContext(), style, mode);
        }
        float hudW = mc.getWindow().getScaledWidth() / scale;
        float hudH = mc.getWindow().getScaledHeight() / scale;
        if (targetHud.getValue()) {
            float targetS = (float) targetHudScale.getValue();
            targetHudComp.render(e.getDrawContext(), targetS, style,
                    (int) (currentMouseX / targetS), (int) (currentMouseY / targetS),
                    hudW / targetS, hudH / targetS, mode);
        }
        if (staffList.getValue()) {
            staffListComp.render(e.getDrawContext(), style, mode, currentMouseX, currentMouseY, (int) hudW, (int) hudH);
        }
        if (moduleList.getValue()) {
            moduleListComp.render(e.getDrawContext(), style, mode, currentMouseX, currentMouseY, (int) hudW, (int) hudH);
        }
        if (notifications.getValue()) {
            NotificationManager.render(e.getDrawContext().getMatrices().peek().getPositionMatrix(), style, mode);
        }
        if(cooldowns.getValue()){
            onCooldowns(e.getDrawContext());
        }

        ScissorUtils.unset();
        e.getDrawContext().getMatrices().pop();
    }
    @Subscribe
    public void onMouse(EventMouse event){
        float scale = (float) hudScale.getValue();
        int mouseX = (int) (event.getMouseX() / scale);
        int mouseY = (int) (event.getMouseY() / scale);
        if (mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) {
            if (event.getAction() == 1) {
                if (potionsDrag.onClick(mouseX, mouseY, event.getButton())) return;
                if (Keybinds.getValue() && keybindsDrag.onClick(mouseX, mouseY, event.getButton())) return;
                if (armor.getValue() && armorDrag.onClick(mouseX, mouseY, event.getButton())) return;
                if(cooldowns.getValue() && cooldownsDrag.onClick(mouseX,mouseY,event.getButton())) return;
                if (notifications.getValue() && NotificationManager.draggable.onClick(mouseX, mouseY, event.getButton())) return;
            } else if (event.getAction() == 0) {
                potionsDrag.onRelease(event.getButton());
                keybindsDrag.onRelease(event.getButton());
                armorDrag.onRelease(event.getButton());
                cooldownsDrag.onRelease(event.getButton());
                if (notifications.getValue()) NotificationManager.draggable.onRelease(event.getButton());
            }
        }
        if (targetHud.getValue()) targetHudComp.handleMouse(mouseX, mouseY, event.getButton(), event.getAction());
        if (staffList.getValue()) staffListComp.handleMouse(mouseX, mouseY, event.getButton(), event.getAction());
        if (moduleList.getValue()) moduleListComp.handleMouse(mouseX, mouseY, event.getButton(), event.getAction());
    }
    public void onCooldowns(DrawContext context){
        if (hudMode.getValue().equals("Macan")) {
            renderMacanCooldowns(context);
            return;
        }
        if (hudMode.getValue().equals("YouGame")) {
            youGameComp.renderCooldowns(context, cooldownsDrag);
            return;
        }
        if (mc.player == null) return;

        net.minecraft.entity.player.ItemCooldownManager manager = mc.player.getItemCooldownManager();
        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);

        java.util.List<String[]> cooldownItems = CooldownUtil.getActiveCooldowns(manager, mc.player, tickDelta);

        boolean placeholder = cooldownItems.isEmpty() && mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
        if (cooldownItems.isEmpty() && !placeholder) return;

        cooldownsAnimation.update();
        cooldownsAnimation.start(cooldownsAnimation.getValue(), 1f, 300, Easing.QUART_OUT);
        float sizeAnim = cooldownsAnimation.getValue();
        if (sizeAnim <= 0.01f) return;

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        Color accentColor = new Color(ThemeManager.getInstance().getPrimary());
        Color textColor = Color.WHITE;
        Color hudBg = new Color(ThemeManager.getInstance().getPalette().getHudBackground());
        hudBg = new Color(hudBg.getRed(), hudBg.getGreen(), hudBg.getBlue(), 255);
        Color sepColor = new Color(166, 166, 166, 255);
        float ts = 9f;
        float iconSz = 8f;
        float headerH = 16f;
        float itemH = 13f;
        float gap = 2f;
        float rounding = 3f;
        float pad = 5f;
        float sepW = 0.5f;
        float timeSize = 8f;

        float maxValWidth = 0f;
        if (placeholder) {
            maxValWidth = BIKO_FONT.get().getWidth("--:--", timeSize);
        }
        for (String[] item : cooldownItems) {
            float tw = BIKO_FONT.get().getWidth(item[1], timeSize);
            if (tw > maxValWidth) maxValWidth = tw;
        }

        float minW = BIKO_FONT.get().getWidth("Cooldowns", ts) + 40f;
        float maxNameW = 0f;
        if (placeholder) {
            maxNameW = BIKO_FONT.get().getWidth("Pearl", ts);
        }
        for (String[] item : cooldownItems) {
            float nw = BIKO_FONT.get().getWidth(item[0], ts);
            if (nw > maxNameW) maxNameW = nw;
        }
        float contentW = maxNameW + sepW + pad + maxValWidth + pad;
        float totalW = Math.max(minW, contentW);
        int rowCount = placeholder ? 1 : cooldownItems.size();
        float totalH = headerH + (rowCount > 0 ? gap + rowCount * (itemH + gap) : 0);

        cooldownsDrag.setWidth(totalW);
        cooldownsDrag.setHeight(totalH);

        float x = cooldownsDrag.getX();
        float y = cooldownsDrag.getY();

        float ccx = x + totalW / 2f;
        float ccy = y + totalH / 2f;
        context.getMatrices().push();
        context.getMatrices().translate(ccx, ccy, 0);
        context.getMatrices().scale(sizeAnim, sizeAnim, 1);
        context.getMatrices().translate(-ccx, -ccy, 0);
        matrix = context.getMatrices().peek().getPositionMatrix();

        if (hudMode.getValue().equals("Old")) {
            Color gradStart = new Color(ThemeManager.getInstance().getPrimary());
            Color gradEnd = new Color(ThemeManager.getInstance().getSecondary());
            ShadowUtil.gradient(gradStart, gradEnd, totalW, totalH, new QuadRadiusState(4)).render(matrix, x, y, 0);
        } else if (hudStyle.getValue().equals("Default")) {
            ShadowUtil.dark(totalW, totalH, new QuadRadiusState(4)).render(matrix, x, y, 0);
        }
        if (hudStyle.getValue().equals("Liquid Glass")) {
            Builder.liquid().size(new SizeState(totalW, totalH)).radius(new QuadRadiusState(4))
                    .color(new QuadColorState(new Color(255, 255, 255, 255))).build().render(matrix, x, y);
        } else if (hudStyle.getValue().equals("Colored Liquid")) {
            Builder.coloredLiquid().size(new SizeState(totalW, totalH)).radius(new QuadRadiusState(4))
                    .color(new QuadColorState(new Color(hudBg.getRed(), hudBg.getGreen(), hudBg.getBlue(), 150)))
                    .build().render(matrix, x, y);
        } else if (hudStyle.getValue().equals("Default")){
            Builder.rectangle().size(new SizeState(totalW,totalH)).radius(new QuadRadiusState(4)).color(new QuadColorState(new Color(20, 20, 20,255))).build().render(matrix,x,y);
        } else {
            Builder.blur().size(new SizeState(totalW, totalH)).radius(new QuadRadiusState(rounding))
                    .color(new QuadColorState(hudBg)).blurRadius(10).smoothness(1f).build().render(matrix, x, y);
        }

        float iconCharW = ICON_FONT.get().getWidth("P", iconSz);
        Builder.text().text("P").font(ICON_FONT.get()).size(iconSz).thickness(0.08f).color(accentColor)
                .build().render(matrix, x + pad, y + (headerH - iconSz) / 2f);
        float titleX = x + pad + iconCharW + 3f;
        Builder.text().text("Cooldowns").font(BIKO_FONT.get()).size(ts).thickness(0.06f).color(textColor)
                .build().render(matrix, titleX, y + (headerH - ts) / 2f);

        float curY = y + headerH + gap;

        if (placeholder) {
            renderCooldownRow(matrix, accentColor, textColor, hudBg, sepColor,
                    x, curY, totalW, itemH, pad, ts, sepW, rounding, timeSize,
                    "Pearl", "--:--");
            curY += itemH + gap;
        } else {
            for (String[] item : cooldownItems) {
                renderCooldownRow(matrix, accentColor, textColor, hudBg, sepColor,
                        x, curY, totalW, itemH, pad, ts, sepW, rounding, timeSize,
                        item[0], item[1]);
                curY += itemH + gap;
            }
        }
        context.getMatrices().pop();
    }

    private void renderCooldownRow(Matrix4f matrix, Color accentColor, Color textColor,
                                   Color hudBg, Color sepColor, float x, float y, float totalW, float itemH,
                                   float pad, float ts, float sepW, float rounding, float timeSize,
                                   String name, String time) {
        Builder.text().text(name).font(BIKO_FONT.get()).size(ts).thickness(0.06f).color(textColor)
                .build().render(matrix, x + pad, y + (itemH - ts) / 2f);

        float timeW = BIKO_FONT.get().getWidth(time, timeSize);
        float timeX = x + totalW - pad - timeW;
        Builder.text().text(time).font(BIKO_FONT.get()).size(timeSize).thickness(0.06f).color(accentColor)
                .build().render(matrix, timeX, y + (itemH - timeSize) / 2f);
    }

    public void onPotion(DrawContext context) {
        if (hudMode.getValue().equals("Macan")) {
            renderMacanPotions(context);
            return;
        }
        if (hudMode.getValue().equals("YouGame")) {
            youGameComp.renderPotions(context, potionsDrag);
            return;
        }
        if (mc.player == null) return;

        Collection<StatusEffectInstance> effects = mc.player.getStatusEffects();

        Set<RegistryEntry<StatusEffect>> currentTypes = new HashSet<>();
        for (StatusEffectInstance effect : effects) {
            currentTypes.add(effect.getEffectType());
            lastEffects.putIfAbsent(effect.getEffectType(), effect.getDuration());
        }

        Iterator<Map.Entry<RegistryEntry<StatusEffect>, Integer>> it = lastEffects.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<RegistryEntry<StatusEffect>, Integer> entry = it.next();
            if (!currentTypes.contains(entry.getKey())) {
                String effectName = entry.getKey().value().getName().getString();
                if (effectName.equals("Fire Resistance")) effectName = "Ognestoi";
                NotificationManager.post(effectName + " Ended", NotificationManager.TYPE_ERROR, 2000);
                ChatUtil.sendMSG(effectName + " Ended");
                it.remove();
            }
        }

        boolean placeholder = effects.isEmpty() && mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
        if (effects.isEmpty() && !placeholder) return;

        potionsAnimation.update();
        potionsAnimation.start(potionsAnimation.getValue(), 1f, 300, Easing.QUART_OUT);
        float sizeAnim = potionsAnimation.getValue();
        if (sizeAnim <= 0.01f) return;

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        
        if (hudMode.getValue().equals("Old")) {
            renderOldPotions(context, matrix, effects, placeholder, sizeAnim);
            return;
        }

        
        Color accentColor = new Color(ThemeManager.getInstance().getPrimary());
        Color textColor = Color.WHITE;
        Color hudBg = new Color(ThemeManager.getInstance().getPalette().getHudBackground());
        hudBg = new Color(hudBg.getRed(), hudBg.getGreen(), hudBg.getBlue(), 255);
        Color sepColor = new Color(166, 166, 166, 255);
        boolean isRussian = mc.getLanguageManager().getLanguage().startsWith("ru");
        MsdfFont textFont = isRussian ? RUSSIAN_FONT.get() : BIKO_FONT.get();
        float ts = 9f;
        float iconSz = 8f;
        float headerH = 16f;
        float itemH = 13f;
        float gap = 2f;
        float rounding = 3f;
        float pad = 5f;
        float sepW = 0.5f;
        float effectIconSize = 12f;
        float effectIconGap = 4f;

        float maxValWidth = 0f;
        for (StatusEffectInstance effect : effects) {
            int ticks = effect.getDuration();
            String time = ticks > 32767 ? "**:**" : String.format("%d:%02d", (ticks / 20) / 60, (ticks / 20) % 60);
            float tw = BIKO_FONT.get().getWidth(time, 8f);
            if (tw > maxValWidth) maxValWidth = tw;
        }
        if (placeholder) {
            float tw = BIKO_FONT.get().getWidth("--:--", 8f);
            if (tw > maxValWidth) maxValWidth = tw;
        }

        float minW = BIKO_FONT.get().getWidth("Potions", ts) + 60f;
        float maxNameW = 0f;
        if (placeholder) {
            maxNameW = BIKO_FONT.get().getWidth("Blank Effect", ts);
        }
        for (StatusEffectInstance effect : effects) {
            String name = getEffectFullName(effect);
            float nw = textFont.getWidth(name, ts);
            if (nw > maxNameW) maxNameW = nw;
        }
        float contentW = maxNameW + pad + maxValWidth + pad + effectIconSize + effectIconGap;
        float totalW = Math.max(minW, contentW);
        int rowCount = placeholder ? 1 : effects.size();
        float totalH = headerH + (rowCount > 0 ? gap + rowCount * (itemH + gap) : 0);

        potionsDrag.setWidth(totalW);
        potionsDrag.setHeight(totalH);

        float x = potionsDrag.getX();
        float y = potionsDrag.getY();

        float cx = x + totalW / 2f;
        float cy = y + totalH / 2f;
        context.getMatrices().push();
        context.getMatrices().translate(cx, cy, 0);
        context.getMatrices().scale(sizeAnim, sizeAnim, 1);
        context.getMatrices().translate(-cx, -cy, 0);
        matrix = context.getMatrices().peek().getPositionMatrix();

        if (hudStyle.getValue().equals("Liquid Glass")) {
            Builder.liquid().size(new SizeState(totalW, totalH)).radius(new QuadRadiusState(4))
                    .color(new QuadColorState(new Color(255, 255, 255, 255))).build().render(matrix, x, y);
        }else if (hudStyle.getValue().equals("Default")){
            ShadowUtil.dark(totalW, totalH, new QuadRadiusState(4)).render(matrix, x, y, 0);
            Builder.rectangle().size(new SizeState(totalW,totalH)).radius(new QuadRadiusState(4)).color(new QuadColorState(new Color(20, 20, 20,255))).build().render(matrix,x,y);
        }
        else {
            Builder.blur().size(new SizeState(totalW, totalH)).radius(new QuadRadiusState(rounding))
                    .color(new QuadColorState(hudBg)).blurRadius(10).smoothness(1f).build().render(matrix, x, y);
        }

        float iconCharW = ICON_FONT.get().getWidth("V", iconSz);
        Builder.text().text("V").font(ICON_FONT.get()).size(iconSz).thickness(0.08f).color(accentColor)
                .build().render(matrix, x + pad, y + (headerH - iconSz) / 2f);
        float titleX = x + pad + iconCharW + 3f;
        Builder.text().text("Potions").font(BIKO_FONT.get()).size(ts).thickness(0.06f).color(textColor)
                .build().render(matrix, titleX, y + (headerH - ts) / 2f);

        float curY = y + headerH + gap;

        if (placeholder) {
            renderPotionRow(context, matrix, textFont, accentColor, textColor, hudBg, sepColor,
                    x, curY, totalW, itemH, pad, ts, iconSz, sepW, rounding,
                    "Blank Effect", "--:--", 0f, null);
            curY += itemH + gap;
        } else {
            for (StatusEffectInstance effect : effects) {
                String name = getEffectFullName(effect);
                int ticks = effect.getDuration();
                String time = ticks > 32767 ? "**:**" : String.format("%d:%02d", (ticks / 20) / 60, (ticks / 20) % 60);
                float progress = Math.min(1.0f, (float) ticks / 2400f);
                Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(effect.getEffectType());
                renderPotionRow(context, matrix, textFont, accentColor, textColor, hudBg, sepColor,
                        x, curY, totalW, itemH, pad, ts, iconSz, sepW, rounding,
                        name, time, progress, sprite);
                curY += itemH + gap;
            }
        }
        context.getMatrices().pop();
    }

    
    private void renderOldPotions(DrawContext context, Matrix4f matrix,
                                  Collection<StatusEffectInstance> effects, boolean placeholder, float sizeAnim) {
        float padX = 8f;
        float ts = 13f;
        float itemH = 24f;
        float gap = 4f;
        float rounding = 4f;
        float timeSize = 13f;
        float effectIconSize = 16f;
        float effectIconGap = 6f;
        float nameTimeGap = 12f;

        boolean isRussian = mc.getLanguageManager().getLanguage().startsWith("ru");
        MsdfFont textFont = isRussian ? RUSSIAN_FONT.get() : BIKO_FONT.get();

        
        List<OldPotionData> potionData = new ArrayList<>();
        if (placeholder) {
            potionData.add(new OldPotionData("Blank Effect", "--:--", 0f, null));
        } else {
            for (StatusEffectInstance effect : effects) {
                String name = getEffectFullName(effect);
                int ticks = effect.getDuration();
                String time = ticks > 32767 ? "**:**" : String.format("%d:%02d", (ticks / 20) / 60, (ticks / 20) % 60);
                Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(effect.getEffectType());
                potionData.add(new OldPotionData(name, time, 0f, sprite));
            }
        }

        
        float maxW = 0f;
        for (OldPotionData d : potionData) {
            float nw = textFont.getWidth(d.name, ts);
            float tw = BIKO_FONT.get().getWidth(d.time, timeSize);
            float w = padX + effectIconSize + effectIconGap + nw + nameTimeGap + tw + padX;
            if (w > maxW) maxW = w;
        }
        float totalW = maxW;
        int rowCount = potionData.size();
        float totalH = rowCount > 0 ? rowCount * itemH + (rowCount - 1) * gap : 0f;

        potionsDrag.setWidth(totalW);
        potionsDrag.setHeight(totalH);

        float x = potionsDrag.getX();
        float y = potionsDrag.getY();

        
        float cx = x + totalW / 2f;
        float cy = y + totalH / 2f;
        context.getMatrices().push();
        context.getMatrices().translate(cx, cy, 0);
        context.getMatrices().scale(sizeAnim, sizeAnim, 1);
        context.getMatrices().translate(-cx, -cy, 0);
        matrix = context.getMatrices().peek().getPositionMatrix();

        Color gradStart = new Color(ThemeManager.getInstance().getPrimary());
        Color gradEnd = new Color(ThemeManager.getInstance().getSecondary());

        
        float curY = y;
        for (OldPotionData d : potionData) {
            ShadowUtil.gradient(gradStart, gradEnd, totalW, itemH, new QuadRadiusState(rounding)).render(matrix, x, curY, 0);
            
            Builder.rectangle().radius(new QuadRadiusState(rounding))
                    .size(new SizeState(totalW, itemH))
                    .color(new QuadColorState(gradStart, gradEnd, gradStart, gradEnd))
                    .build().render(matrix, x, curY);

            
            float textX = x + padX;
            if (d.sprite != null) {
                float iconY2 = curY + (itemH - effectIconSize) / 2f;
                drawPotionSprite(context.getMatrices(), d.sprite, x + padX, iconY2, effectIconSize);
                textX += effectIconSize + effectIconGap;
            }

            
            Builder.text().text(d.name).font(textFont).size(ts).thickness(0.06f).color(Color.WHITE)
                    .build().render(matrix, textX, curY + (itemH - ts) / 2f);

            
            float timeW = BIKO_FONT.get().getWidth(d.time, timeSize);
            float timeX = x + totalW - padX - timeW;
            Builder.text().text(d.time).font(BIKO_FONT.get()).size(timeSize).thickness(0.06f).color(Color.WHITE)
                    .build().render(matrix, timeX, curY + (itemH - timeSize) / 2f);

            curY += itemH + gap;
        }

        context.getMatrices().pop();
    }

    
    private static class OldPotionData {
        String name;
        String time;
        float progress;
        Sprite sprite;
        OldPotionData(String name, String time, float progress, Sprite sprite) {
            this.name = name;
            this.time = time;
            this.progress = progress;
            this.sprite = sprite;
        }
    }

    private void renderPotionRow(DrawContext context, Matrix4f matrix, MsdfFont textFont, Color accentColor, Color textColor,
                                 Color hudBg, Color sepColor, float x, float y, float totalW, float itemH,
                                 float pad, float ts, float iconSz, float sepW, float rounding,
                                 String name, String time, float progress, Sprite sprite) {
        float textX = x + pad;
        if (sprite != null) {
            float iconSize = 12f;
            float iconY = y + (itemH - iconSize) / 2f;
            drawPotionSprite(context.getMatrices(), sprite, x + pad, iconY, iconSize);
            textX += iconSize + 4f;
        }

        Builder.text().text(name).font(textFont).size(ts).thickness(0.04f).color(textColor)
                .build().render(matrix, textX, y + (itemH - ts) / 2f);

        float timeSize = 8f;
        float timeW = BIKO_FONT.get().getWidth(time, timeSize);
        float timeX = x + totalW - pad - timeW;
        Builder.text().text(time).font(BIKO_FONT.get()).size(timeSize).thickness(0.06f).color(new Color(ThemeManager.getInstance().getPalette().getTextSecondary()))
                .build().render(matrix, timeX, y + (itemH - timeSize) / 2f);
    }

    private void drawPotionSprite(MatrixStack matrices, Sprite sprite, float x, float y, float size) {
        float x1 = x, x2 = x + size, y1 = y, y2 = y + size;
        RenderSystem.setShaderTexture(0, sprite.getAtlasId());
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Matrix4f m = matrices.peek().getPositionMatrix();
        buf.vertex(m, x1, y1, 0).texture(sprite.getMinU(), sprite.getMinV()).color(255, 255, 255, 255);
        buf.vertex(m, x1, y2, 0).texture(sprite.getMinU(), sprite.getMaxV()).color(255, 255, 255, 255);
        buf.vertex(m, x2, y2, 0).texture(sprite.getMaxU(), sprite.getMaxV()).color(255, 255, 255, 255);
        buf.vertex(m, x2, y1, 0).texture(sprite.getMaxU(), sprite.getMinV()).color(255, 255, 255, 255);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    public void onKeybinds(DrawContext context) {
        if (hudMode.getValue().equals("Macan")) {
            renderMacanKeybinds(context);
            return;
        }
        if (hudMode.getValue().equals("YouGame")) {
            youGameComp.renderKeybinds(context, keybindsDrag);
            return;
        }
        if (hudMode.getValue().equals("Old")) {
            
            Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
            renderOldKeybinds(context, matrix);
            return;
        }
        java.util.List<fun.ogi.module.Module> enabled = Cheap.getInstance().getModuleStorage().getModules().stream()
                .filter(m -> m.isEnabled() && m.getKeybind() != 0 && m.getKeybind() != -1)
                .toList();

        boolean shouldBeVisible = !enabled.isEmpty() || (mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen);

        if (shouldBeVisible && !wasKeybindsVisible) {
            keybindsAnimation.start(keybindsAnimation.getValue(), 1f, 250, Easing.QUART_OUT);
        } else if (!shouldBeVisible && wasKeybindsVisible) {
            keybindsAnimation.start(keybindsAnimation.getValue(), 0f, 250, Easing.QUART_OUT);
        }

        wasKeybindsVisible = shouldBeVisible;
        keybindsAnimation.update();

        float animScale = keybindsAnimation.getValue();
        if (animScale <= 0.01f) return;

        context.getMatrices().push();
        float cx = keybindsDrag.getX() + keybindsDrag.getWidth() / 2f;
        float cy = keybindsDrag.getY() + keybindsDrag.getHeight() / 2f;
        context.getMatrices().translate(cx, cy, 0);
        context.getMatrices().scale(animScale, animScale, 1);
        context.getMatrices().translate(-cx, -cy, 0);

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        Color accentColor = new Color(ThemeManager.getInstance().getPrimary());
        Color textColor = Color.WHITE;
        Color hudBg = new Color(ThemeManager.getInstance().getPalette().getHudBackground());
        hudBg = new Color(hudBg.getRed(), hudBg.getGreen(), hudBg.getBlue(), 255);
        Color sepColor = new Color(166, 166, 166, 255);
        float ts = 9f;
        float iconSz = 8f;
        float headerH = 16f;
        float itemH = 13f;
        float gap = 2f;
        float rounding = 3f;
        float pad = 5f;
        float sepW = 0.5f;

        java.util.List<fun.ogi.module.Module> listToDraw = enabled;
        if (listToDraw.isEmpty() && mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) {
            listToDraw = new ArrayList<>();
        }

        float maxBindWidth = 0f;
        for (fun.ogi.module.Module m : listToDraw) {
            String bind = BindCommand.keyToName(m.getKeybind());
            float bw = BIKO_FONT.get().getWidth(bind, 8f);
            if (bw > maxBindWidth) maxBindWidth = bw;
        }

        float minW = BIKO_FONT.get().getWidth("Keybinds", ts) + 40f;
        float maxNameW = 0f;
        for (fun.ogi.module.Module m : listToDraw) {
            float nw = BIKO_FONT.get().getWidth(m.getName(), ts);
            if (nw > maxNameW) maxNameW = nw;
        }
        float contentW = maxNameW + sepW + pad + maxBindWidth + pad;
        float totalW = Math.max(minW, contentW);
        int rowCount = listToDraw.size();
        float totalH = headerH + (rowCount > 0 ? gap + rowCount * (itemH + gap) : 0);

        keybindsDrag.setWidth(totalW);
        keybindsDrag.setHeight(totalH);

        float x = keybindsDrag.getX();
        float y = keybindsDrag.getY();

        if (hudStyle.getValue().equals("Liquid Glass")) {
            Builder.liquid().size(new SizeState(totalW, totalH)).radius(new QuadRadiusState(4))
                    .color(new QuadColorState(new Color(255, 255, 255, 255))).build().render(matrix, x, y);
        } else if (hudStyle.getValue().equals("Colored Liquid")) {
            Builder.coloredLiquid().size(new SizeState(totalW, totalH)).radius(new QuadRadiusState(4))
                    .color(new QuadColorState(new Color(20,200,20, 200)))
                    .build().render(matrix, x, y);
        } else if (hudStyle.getValue().equals("Default")){
            ShadowUtil.dark(totalW, totalH, new QuadRadiusState(4)).render(matrix, x, y, 0);
            Builder.rectangle().size(new SizeState(totalW,totalH)).radius(new QuadRadiusState(4)).color(new QuadColorState(new Color(20, 20, 20,255))).build().render(matrix,x,y);
        }
        else {
            Builder.blur().size(new SizeState(totalW, totalH)).radius(new QuadRadiusState(rounding))
                    .color(new QuadColorState(hudBg)).blurRadius(10).smoothness(1f).build().render(matrix, x, y);
        }

        float iconCharW = ICON_FONT.get().getWidth("F", iconSz);
        Builder.text().text("F").font(ICON_FONT.get()).size(iconSz).thickness(0.08f).color(accentColor)
                .build().render(matrix, x + pad, y + (headerH - iconSz) / 2f);
        float titleX = x + pad + iconCharW + 3f;
        Builder.text().text("Keybinds").font(BIKO_FONT.get()).size(ts).thickness(0.06f).color(textColor)
                .build().render(matrix, titleX, y + (headerH - ts) / 2f);

        float curY = y + headerH + gap;
        for (fun.ogi.module.Module m : listToDraw) {
            String bind = BindCommand.keyToName(m.getKeybind());

            Builder.text().text(m.getName()).font(BIKO_FONT.get()).size(ts).thickness(0.06f).color(textColor)
                    .build().render(matrix, x + pad, curY + (itemH - ts) / 2f);

            float bindW = BIKO_FONT.get().getWidth(bind, 8f);
            float bindX = x + totalW - pad - bindW;
            Builder.text().text(bind).font(BIKO_FONT.get()).size(8f).thickness(0.06f).color(accentColor)
                    .build().render(matrix, bindX, curY + (itemH - 8f) / 2f);

            curY += itemH + gap;
        }

        context.getMatrices().pop();
    }
    public void renderOldKeybinds(DrawContext context, Matrix4f matrix) {
        java.util.List<Module> enabled = Cheap.getInstance().getModuleStorage().getModules().stream()
                .filter(m -> m.isEnabled() && m.getKeybind() != 0 && m.getKeybind() != -1)
                .toList();

        boolean shouldBeVisible = !enabled.isEmpty() || (mc.currentScreen instanceof ChatScreen);
        if (!shouldBeVisible) return;

        
        float padX = 5f;
        float padY = 5f;
        float ts = 9f;
        float iconSz = 8f;
        float headerH = 16f;
        float itemH = 13f;
        float gap = 2f;
        float rounding = 3f;
        float sepW = 0.5f;

        
        java.util.List<Module> listToDraw = enabled;
        if (listToDraw.isEmpty() && mc.currentScreen instanceof ChatScreen) {
            listToDraw = new ArrayList<>();
            
        }

        
        float maxBindWidth = 0f;
        for (Module m : listToDraw) {
            String bind = BindCommand.keyToName(m.getKeybind());
            float bw = BIKO_FONT.get().getWidth(bind, 8f);
            if (bw > maxBindWidth) maxBindWidth = bw;
        }
        float maxNameW = 0f;
        for (Module m : listToDraw) {
            float nw = BIKO_FONT.get().getWidth(m.getName(), ts);
            if (nw > maxNameW) maxNameW = nw;
        }
        float minW = BIKO_FONT.get().getWidth("Keybinds", ts) + 40f;
        float contentW = maxNameW + sepW + padX + maxBindWidth + padX;
        float totalW = Math.max(minW, contentW);
        int rowCount = listToDraw.size();
        float totalH = padY + headerH + padY + (rowCount > 0 ? gap + rowCount * (itemH + gap) : 0);

        keybindsDrag.setWidth(totalW);
        keybindsDrag.setHeight(totalH);

        float x = keybindsDrag.getX();
        float y = keybindsDrag.getY();

        
        Color gradStart = new Color(ThemeManager.getInstance().getPrimary());
        Color gradEnd = new Color(ThemeManager.getInstance().getSecondary());
        ShadowUtil.gradient(gradStart, gradEnd, totalW, headerH, new QuadRadiusState(rounding)).render(matrix, x, y + 5, 0);
        Builder.rectangle()
                .size(new SizeState(totalW, headerH))
                .radius(new QuadRadiusState(rounding))
                .color(new QuadColorState(gradStart, gradEnd, gradStart, gradEnd))
                .build().render(matrix, x, y +5);

        
        float iconCharW = ICON_FONT.get().getWidth("F", iconSz);
        float iconY = y + (headerH - iconSz) / 2f;
        Builder.text().text("F").font(ICON_FONT.get()).size(iconSz).thickness(0.08f).color(Color.WHITE)
                .build().render(matrix, x + padX, iconY + 5);
        float titleX = x + padX + iconCharW + 3f;
        float titleY = y + (headerH - ts) / 2f;
        Builder.text().text("Keybinds").font(BIKO_FONT.get()).size(ts).thickness(0.06f).color(Color.WHITE)
                .build().render(matrix, titleX, titleY + 5);

        
        float curY = y + padY + headerH + gap;
        for (Module m : listToDraw) {
            String bind = BindCommand.keyToName(m.getKeybind());

            
            ShadowUtil.gradient(gradStart, gradEnd, totalW, itemH, new QuadRadiusState(rounding)).render(matrix, x, curY, 0);
            Builder.rectangle().radius(new QuadRadiusState(rounding))
                    .size(new SizeState(totalW, itemH))
                    .color(new QuadColorState(new Color(20, 20, 20, 155)))
                    .build().render(matrix, x, curY);

            
            Builder.text().text(m.getName()).font(BIKO_FONT.get()).size(ts).thickness(0.06f).color(Color.WHITE)
                    .build().render(matrix, x + padX, curY + (itemH - ts) / 2f);

            
            float bindW = BIKO_FONT.get().getWidth(bind, 8f);
            float bindX = x + totalW - padX - bindW;
            Builder.text().text(bind).font(BIKO_FONT.get()).size(8f).thickness(0.06f).color(Color.WHITE)
                    .build().render(matrix, bindX, curY + (itemH - 8f) / 2f);

            curY += itemH + gap;
        }
    }
    public void onArmor(DrawContext context){
        if (hudMode.getValue().equals("YouGame")) {
            youGameComp.renderArmor(context, armorDrag);
            return;
        }
        if(mc.player == null) return;

        armorAnimation.update();
        armorAnimation.start(armorAnimation.getValue(), 1f, 300, Easing.QUART_OUT);
        float sizeAnim = armorAnimation.getValue();
        if (sizeAnim <= 0.01f) return;

        float x = armorDrag.getX();
        float y = armorDrag.getY();
        float width = 82f;
        float height = 20f;
        armorDrag.setWidth(width);
        armorDrag.setHeight(height);
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        float cx = x + width / 2f;
        float cy = y + height / 2f;
        context.getMatrices().push();
        context.getMatrices().translate(cx, cy, 0);
        context.getMatrices().scale(sizeAnim, sizeAnim, 1);
        context.getMatrices().translate(-cx, -cy, 0);
        matrix = context.getMatrices().peek().getPositionMatrix();

        Color hudBg = new Color(ThemeManager.getInstance().getPalette().getHudBackground());
        hudBg = new Color(hudBg.getRed(), hudBg.getGreen(), hudBg.getBlue(), 255);
        if(hudMode.getValueAsString().equals("Old")){
            Color gradStart = new Color(ThemeManager.getInstance().getPrimary());
            Color gradEnd = new Color(ThemeManager.getInstance().getSecondary());
            ShadowUtil.gradient(gradStart, gradEnd, width, height, new QuadRadiusState(5)).render(matrix, x, y, 0);
            Builder.rectangle()
                    .size(new SizeState(width, height))
                    .radius(new QuadRadiusState(5,5,5,5))
                    .color(new QuadColorState(gradStart, gradEnd, gradStart, gradEnd))
                    .build().render(matrix, x, y);
        } else if (hudStyle.getValue().equals("Liquid Glass")) {
            Builder.liquid()
                    .size(new SizeState(width, height))
                    .radius(new QuadRadiusState(5, 5, 5, 5))
                    .color(new QuadColorState(new Color(255, 255, 255, 255)))
                    .build()
                    .render(matrix, x, y);
        } else if (hudStyle.getValue().equals("Colored Liquid")) {
            Builder.coloredLiquid()
                    .size(new SizeState(width, height))
                    .radius(new QuadRadiusState(5, 5, 5, 5))
                    .color(new QuadColorState(new Color(hudBg.getRed(), hudBg.getGreen(), hudBg.getBlue(), 150)))
                    .build()
                    .render(matrix, x, y);
        } else if (hudStyle.getValue().equals("Default")){
            ShadowUtil.dark(width, height, new QuadRadiusState(4)).render(matrix, x, y, 0);
            Builder.rectangle().size(new SizeState(width,height)).radius(new QuadRadiusState(4)).color(new QuadColorState(new Color(20, 20, 20,255))).build().render(matrix,x,y);
        }
        else {
            Builder.blur()
                    .size(new SizeState(width, height))
                    .color(new QuadColorState(hudBg))
                    .radius(new QuadRadiusState(5))
                    .smoothness(1f)
                    .blurRadius(10)
                    .build()
                    .render(matrix, x, y, 0);
        }

        int offset = 0;
        for (int i = 3; i >= 0; i--) {
            ItemStack stack = mc.player.getInventory().armor.get(i);
            if (stack.isEmpty()) continue;
            context.drawItem(stack, (int) x + 4 + offset, (int) y + 2);
            context.drawStackOverlay(mc.textRenderer, stack, (int) x + 4 + offset, (int) y + 2);
            offset += 20;
        }

        context.getMatrices().pop();
    }

    

    private void renderMacanBlur(Matrix4f matrix, float x, float y, float w, float h, int argb, Vector4f round) {
        Color c = new Color(argb, true);
        float rl = round.x, rr = round.y, br = round.z, bl = round.w;
        Builder.blur().size(new SizeState(w, h)).radius(new QuadRadiusState(rl, rr, br, bl))
                .color(new QuadColorState(c)).blurRadius(10).smoothness(1f).build().render(matrix, x, y);
    }

    private void renderMacanBlur(Matrix4f matrix, float x, float y, float w, float h, int argb, float round) {
        renderMacanBlur(matrix, x, y, w, h, argb, new Vector4f(round));
    }

    private float alignToPixel(float value) {
        return Math.round(value * 2f) / 2f;
    }

    private void drawRingSegment(Matrix4f matrix, float cx, float cy, float radius, float thickness, float softness, float startAngle, float sweepAngle, int color) {
        float clamped = Math.max(0, Math.min(360, sweepAngle));
        if (clamped <= 0) return;
        float innerR = Math.max(0, radius - thickness);
        float innerSoft = Math.max(0, innerR - Math.min(softness, innerR));
        float outerSoft = radius + softness;
        int transparent = color & 0x00FFFFFF;
        int segments = Math.max(180, (int)Math.ceil(clamped / 1.6));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        drawRingStrip(matrix, cx, cy, outerSoft, radius, transparent, color, startAngle, clamped, segments);
        drawRingStrip(matrix, cx, cy, radius, innerR, color, color, startAngle, clamped, segments);
        if (innerR > 0) drawRingStrip(matrix, cx, cy, innerR, innerSoft, color, transparent, startAngle, clamped, segments);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void drawRingStrip(Matrix4f matrix, float cx, float cy, float outer, float inner, int outerColor, int innerColor, float start, float sweep, int segs) {
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segs; i++) {
            float p = i / (float)segs;
            float a = (float)Math.toRadians(start + sweep * p);
            float cos = (float)Math.cos(a);
            float sin = (float)Math.sin(a);
            addRingVertex(buf, matrix, cx + cos * outer, cy + sin * outer, outerColor);
            addRingVertex(buf, matrix, cx + cos * inner, cy + sin * inner, innerColor);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void addRingVertex(BufferBuilder buf, Matrix4f matrix, float x, float y, int color) {
        Color c = new Color(color, true);
        buf.vertex(matrix, x, y, 0).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }

    private void renderProgressRing(Matrix4f matrix, float x, float y, float progress, float alpha) {
        float p = Math.max(0, Math.min(1, progress));
        int trackColor = new Color(255, 255, 255, (int)(0.12f * alpha * 255)).getRGB();
        int accentColor = new Color(ThemeManager.getInstance().getPrimary()).getRGB();
        int activeColor = ((int)(alpha * 255) << 24) | (accentColor & 0x00FFFFFF);
        float cxa = alignToPixel(x + 4);
        float cya = alignToPixel(y + 4);
        drawRingSegment(matrix, cxa, cya, 3f, 0.5f, 0.85f, 270, 360, trackColor);
        if (p <= 0) return;
        drawRingSegment(matrix, cxa, cya, 3f, 0.5f, 0.85f, 270, 360 * p, activeColor);
    }

    private float getLayoutDeltaSeconds(long prev) {
        long now = System.nanoTime();
        if (prev == 0) return 1f / 60f;
        float delta = (now - prev) / 1_000_000_000f;
        return Math.min(delta, 0.1f);
    }

    private float smoothWidth(float current, float target, float delta) {
        if (current < 0) return target;
        if (Math.abs(target - current) < 0.01f) return target;
        float factor = 1f - (float)Math.exp(-24 * delta);
        return current + (target - current) * factor;
    }

    private void renderMacanCooldowns(DrawContext context) {
        if (mc.player == null) return;
        var manager = mc.player.getItemCooldownManager();
        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
        var entries = CooldownUtil.getActiveCooldownsFull(manager, mc.player, tickDelta);
        boolean placeholder = entries.isEmpty() && mc.currentScreen instanceof ChatScreen;
        if (entries.isEmpty() && !placeholder) return;

        cooldownsAnimation.update();
        cooldownsAnimation.start(cooldownsAnimation.getValue(), 1f, 300, Easing.QUART_OUT);
        float anim = cooldownsAnimation.getValue();
        if (anim <= 0.01f) return;

        float delta = getLayoutDeltaSeconds(previousCooldownLayoutTime);
        previousCooldownLayoutTime = System.nanoTime();

        float pad = 5f;
        float itemScale = 0.7f;
        float itemSize = 16f * itemScale;
        float rowH = itemSize;
        float rowGap = 5f;
        int back2 = new Color(12, 12, 12, (int)(200 * anim)).getRGB();
        int back1 = new Color(22, 22, 22, (int)(153 * anim)).getRGB();
        float iconBackW = pad + itemSize + pad;

        var display = new ArrayList<>(entries);
        if (placeholder) display.add(new CooldownUtil.CooldownEntry(Items.GOLDEN_APPLE.getDefaultStack(), "Golden Apple", "0:08", 0.78f));

        float maxNameW = 0, maxTimeW = 0;
        for (var e : display) {
            maxNameW = Math.max(maxNameW, BIKO_FONT.get().getWidth(e.name(), 7f));
            maxTimeW = Math.max(maxTimeW, 8f + 5f + BIKO_FONT.get().getWidth(e.time(), 6f));
        }

        float targetNameBackW = pad + maxNameW + pad;
        float targetTimeBackW = pad + maxTimeW + pad;
        float nameBackW = smoothWidth(animatedCooldownNameBackWidth, targetNameBackW, delta);
        float timeBackW = smoothWidth(animatedCooldownTimeBackWidth, targetTimeBackW, delta);
        animatedCooldownNameBackWidth = nameBackW;
        animatedCooldownTimeBackWidth = timeBackW;

        float nameBackX = cooldownsDrag.getX() + iconBackW;
        float timeBackX = nameBackX + nameBackW;
        float totalW = iconBackW + nameBackW + timeBackW;
        float animPad = pad * anim;
        float contentH = display.size() * (rowH + rowGap) - rowGap;
        float totalH = contentH > 0 ? contentH + animPad * 2 : 0;

        cooldownsDrag.setWidth(totalW);
        cooldownsDrag.setHeight(totalH);
        float x = cooldownsDrag.getX();
        float y = cooldownsDrag.getY();

        context.getMatrices().push();
        float cx = x + totalW / 2f;
        float cy = y + totalH / 2f;
        context.getMatrices().translate(cx, cy, 0);
        context.getMatrices().scale(anim, anim, 1);
        context.getMatrices().translate(-cx, -cy, 0);
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        if (!display.isEmpty() && totalH > 0) {
            ShadowUtil.dark(totalW, totalH, new QuadRadiusState(5)).render(matrix, x, y, 0);
            Vector4f iconRound = new Vector4f(5, 5, 0, 0);
            renderMacanBlur(matrix, x, y, iconBackW, totalH, back2, iconRound);
            renderMacanBlur(matrix, timeBackX, y, timeBackW, totalH, back2, new Vector4f(0, 0, 5, 5));
            renderMacanBlur(matrix, nameBackX, y, nameBackW, totalH, back1, 0f);
        }

        float off = animPad;
        for (var entry : display) {
            float rowY = y + off;
            float rowCenterY = rowY + rowH / 2f;

            float itemX = x + pad;
            float itemY = rowY;
            context.getMatrices().push();
            context.getMatrices().translate(itemX, itemY, 300);
            context.getMatrices().scale(itemScale, itemScale, 1);
            context.drawItem(entry.stack(), 0, 0);
            context.getMatrices().pop();

            ScissorUtils.push();
            ScissorUtils.setFromComponentCoordinates(nameBackX, rowY, nameBackW, rowH);
            Builder.text().text(entry.name()).font(BIKO_FONT.get()).size(7f).thickness(0.06f).color(Color.WHITE)
                    .build().render(matrix, nameBackX + pad, rowCenterY - 3.5f);
            ScissorUtils.pop();

            float indX = timeBackX + pad;
            float indY = rowCenterY - 4f;
            float timeW = BIKO_FONT.get().getWidth(entry.time(), 6f);
            float timeX = timeBackX + timeBackW - pad - timeW;

            ScissorUtils.push();
            ScissorUtils.setFromComponentCoordinates(timeBackX, rowY, timeBackW, rowH);
            renderProgressRing(matrix, indX, indY, entry.progress(), 1f);
            Builder.text().text(entry.time()).font(BIKO_FONT.get()).size(6f).thickness(0.06f).color(Color.WHITE)
                    .build().render(matrix, timeX, rowCenterY - 3f);
            ScissorUtils.pop();

            off += rowH + rowGap;
        }

        context.getMatrices().pop();
    }

    private void renderMacanPotions(DrawContext context) {
        if (mc.player == null) return;
        var effects = mc.player.getStatusEffects();
        boolean placeholder = effects.isEmpty() && mc.currentScreen instanceof ChatScreen;
        if (effects.isEmpty() && !placeholder) return;

        potionsAnimation.update();
        potionsAnimation.start(potionsAnimation.getValue(), 1f, 300, Easing.QUART_OUT);
        float anim = potionsAnimation.getValue();
        if (anim <= 0.01f) return;

        float delta = getLayoutDeltaSeconds(previousPotionLayoutTime);
        previousPotionLayoutTime = System.nanoTime();

        float pad = 5f;
        float itemSize = 16f * 0.7f;
        float rowH = itemSize;
        float rowGap = 5f;
        int back2 = new Color(12, 12, 12, (int)(200 * anim)).getRGB();
        int back1 = new Color(22, 22, 22, (int)(153 * anim)).getRGB();
        float iconBackW = pad + itemSize + pad;

        record PotionRender(String name, String level, String duration, int color, float progress) {}

        List<PotionRender> renderList = new ArrayList<>();
        if (placeholder) {
            renderList.add(new PotionRender("Speed", "", "1:30", 0, 0.75f));
        } else {
            for (var eff : effects) {
                String n = getEffectBaseName(eff);
                int lvl = eff.getAmplifier() + 1;
                String lvlStr = lvl <= 1 ? "" : switch(lvl) {
                    case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
                    case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII"; case 9 -> "IX";
                    default -> "X";
                };
                int ticks = eff.getDuration();
                String dur = ticks > 32767 ? "inf" : String.format("%d:%02d", ticks / 20 / 60, (ticks / 20) % 60);
                float prog = eff.isInfinite() ? 1f : Math.min(1f, ticks / 2400f);
                renderList.add(new PotionRender(n, lvlStr, dur, 0, prog));
            }
        }

        float maxNameW = 0, maxTimeW = 0;
        for (var r : renderList) {
            float nameW = BIKO_FONT.get().getWidth(r.name(), 7f);
            if (!r.level().isEmpty()) nameW += 4f + BIKO_FONT.get().getWidth(r.level(), 7f);
            maxNameW = Math.max(maxNameW, nameW);
            maxTimeW = Math.max(maxTimeW, 8f + 5f + BIKO_FONT.get().getWidth(r.duration(), 6f));
        }

        float targetNameBackW = pad + maxNameW + pad;
        float targetTimeBackW = pad + maxTimeW + pad;
        float nameBackW = smoothWidth(animatedPotionNameBackWidth, targetNameBackW, delta);
        float timeBackW = smoothWidth(animatedPotionTimeBackWidth, targetTimeBackW, delta);
        animatedPotionNameBackWidth = nameBackW;
        animatedPotionTimeBackWidth = timeBackW;

        float nameBackX = potionsDrag.getX() + iconBackW;
        float timeBackX = nameBackX + nameBackW;
        float totalW = iconBackW + nameBackW + timeBackW;
        float animPad = pad * anim;
        float contentH = renderList.size() * (rowH + rowGap) - rowGap;
        float totalH = contentH > 0 ? contentH + animPad * 2 : 0;

        potionsDrag.setWidth(totalW);
        potionsDrag.setHeight(totalH);
        float x = potionsDrag.getX();
        float y = potionsDrag.getY();

        context.getMatrices().push();
        float cx = x + totalW / 2f;
        float cy = y + totalH / 2f;
        context.getMatrices().translate(cx, cy, 0);
        context.getMatrices().scale(anim, anim, 1);
        context.getMatrices().translate(-cx, -cy, 0);
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        if (!renderList.isEmpty() && totalH > 0) {
            ShadowUtil.dark(totalW, totalH, new QuadRadiusState(5)).render(matrix, x, y, 0);
            renderMacanBlur(matrix, x, y, iconBackW, totalH, back2, new Vector4f(5, 5, 0, 0));
            renderMacanBlur(matrix, timeBackX, y, timeBackW, totalH, back2, new Vector4f(0, 0, 5, 5));
            renderMacanBlur(matrix, nameBackX, y, nameBackW, totalH, back1, 0f);
        }

        float off = animPad;
        var spriteManager = mc.getStatusEffectSpriteManager();
        int accentRGB = ThemeManager.getInstance().getPrimary();

        for (var entry : renderList) {
            float rowY = y + off;
            float rowCenterY = rowY + rowH / 2f;

            float iconY = rowY + (rowH - 11.2f) / 2f;
            var effectsList = mc.player != null ? mc.player.getStatusEffects() : List.<StatusEffectInstance>of();
            for (var eff : effectsList) {
                String n = getEffectBaseName(eff);
                if (n.equals(entry.name())) {
                    var sprite = spriteManager.getSprite(eff.getEffectType());
                    if (sprite != null) {
                        drawPotionSprite(context.getMatrices(), sprite, x + pad, iconY, itemSize);
                    }
                    break;
                }
            }

            ScissorUtils.push();
            ScissorUtils.setFromComponentCoordinates(nameBackX, rowY, nameBackW, rowH);
            float nx = nameBackX + pad;
            float ny = rowCenterY - 3.5f;
            Builder.text().text(entry.name()).font(BIKO_FONT.get()).size(7f).thickness(0.04f).color(Color.WHITE)
                    .build().render(matrix, nx, ny);
            if (!entry.level().isEmpty()) {
                float lx = nx + BIKO_FONT.get().getWidth(entry.name(), 7f) + 4f;
                Builder.text().text(entry.level()).font(BIKO_FONT.get()).size(7f).thickness(0.04f)
                        .color(new Color(accentRGB)).build().render(matrix, lx, ny);
            }
            ScissorUtils.pop();

            float indX = timeBackX + pad;
            float indY = rowCenterY - 4f;
            float timeW = BIKO_FONT.get().getWidth(entry.duration(), 6f);
            float timeX = timeBackX + timeBackW - pad - timeW;

            ScissorUtils.push();
            ScissorUtils.setFromComponentCoordinates(timeBackX, rowY, timeBackW, rowH);
            renderProgressRing(matrix, indX, indY, entry.progress(), 1f);
            Builder.text().text(entry.duration()).font(BIKO_FONT.get()).size(6f).thickness(0.06f).color(Color.WHITE)
                    .build().render(matrix, timeX, rowCenterY - 3f);
            ScissorUtils.pop();

            off += rowH + rowGap;
        }

        context.getMatrices().pop();
    }

    private void renderMacanKeybinds(DrawContext context) {
        var modules = Cheap.getInstance().getModuleStorage().getModules().stream()
                .filter(m -> m.isEnabled() && m.getKeybind() != 0 && m.getKeybind() != -1)
                .toList();
        boolean shouldShow = !modules.isEmpty() || mc.currentScreen instanceof ChatScreen;
        if (shouldShow && !wasKeybindsVisible) {
            keybindsAnimation.start(keybindsAnimation.getValue(), 1f, 250, Easing.QUART_OUT);
        } else if (!shouldShow && wasKeybindsVisible) {
            keybindsAnimation.start(keybindsAnimation.getValue(), 0f, 250, Easing.QUART_OUT);
        }
        wasKeybindsVisible = shouldShow;
        keybindsAnimation.update();
        float anim = keybindsAnimation.getValue();
        if (anim <= 0.01f) return;

        float delta = getLayoutDeltaSeconds(previousKeybindLayoutTime);
        previousKeybindLayoutTime = System.nanoTime();

        record KeybindEntry(String name, String bind, String glyph, float anim) {}
        List<KeybindEntry> entries = new ArrayList<>();
        for (var m : modules) {
            entries.add(new KeybindEntry(m.getName(), BindCommand.keyToName(m.getKeybind()), getCategoryGlyph(m.getCategory()), 1f));
        }
        if (entries.isEmpty() && mc.currentScreen instanceof ChatScreen) {
            entries.add(new KeybindEntry("Preview", "N/A", "3", anim));
        }

        float pad = 5f;
        float itemSize = 16f * 0.7f;
        float rowH = itemSize;
        float rowGap = 5f;
        int back2 = new Color(12, 12, 12, (int)(200 * anim)).getRGB();
        int back1 = new Color(22, 22, 22, (int)(153 * anim)).getRGB();
        float iconBackW = pad + itemSize + pad;

        float maxNameW = 0, maxBindW = 0;
        for (var e : entries) {
            maxNameW = Math.max(maxNameW, BIKO_FONT.get().getWidth(e.name(), 7f));
            maxBindW = Math.max(maxBindW, BIKO_FONT.get().getWidth(e.bind(), 6f));
        }

        float targetNameBackW = pad + maxNameW + pad + 3f;
        float targetBindBackW = pad + maxBindW + pad + 6f;
        float nameBackW = smoothWidth(animatedKeybindNameBackWidth, targetNameBackW, delta);
        float bindBackW = smoothWidth(animatedKeybindBindBackWidth, targetBindBackW, delta);
        animatedKeybindNameBackWidth = nameBackW;
        animatedKeybindBindBackWidth = bindBackW;

        float nameBackX = keybindsDrag.getX() + iconBackW;
        float bindBackX = nameBackX + nameBackW;
        float totalW = iconBackW + nameBackW + bindBackW;
        float animPad = pad * anim;
        float contentH = entries.size() * (rowH + rowGap) - rowGap;
        float totalH = contentH > 0 ? contentH + animPad * 2 : 0;

        keybindsDrag.setWidth(totalW);
        keybindsDrag.setHeight(totalH);
        float x = keybindsDrag.getX();
        float y = keybindsDrag.getY();

        context.getMatrices().push();
        float cx = x + totalW / 2f;
        float cy = y + totalH / 2f;
        context.getMatrices().translate(cx, cy, 0);
        context.getMatrices().scale(anim, anim, 1);
        context.getMatrices().translate(-cx, -cy, 0);
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        if (!entries.isEmpty() && totalH > 0) {
            ShadowUtil.dark(totalW, totalH, new QuadRadiusState(5)).render(matrix, x, y, 0);
            renderMacanBlur(matrix, x, y, iconBackW, totalH, back2, new Vector4f(5, 5, 0, 0));
            renderMacanBlur(matrix, bindBackX, y, bindBackW, totalH, back2, new Vector4f(0, 0, 5, 5));
            renderMacanBlur(matrix, nameBackX, y, nameBackW, totalH, back1, 0f);
        }

        float off = animPad;
        int accentRGB = ThemeManager.getInstance().getPrimary();
        Color accentColor = new Color(accentRGB);

        for (var entry : entries) {
            float rowY = y + off;
            float rowCenterY = rowY + rowH / 2f;
            float rowHVisible = rowH * entry.anim();

            ScissorUtils.push();
            ScissorUtils.setFromComponentCoordinates(x, rowY, totalW, rowHVisible);

            float iconBaseX = x + iconBackW / 2f;
            Builder.text().text(entry.glyph()).font(MACAN_ICONS2_FONT.get()).size(7f).thickness(0.08f).color(accentColor)
                    .build().render(matrix, iconBaseX - MACAN_ICONS2_FONT.get().getWidth(entry.glyph(), 7f) / 2f, rowCenterY - 3.5f);

            ScissorUtils.push();
            ScissorUtils.setFromComponentCoordinates(nameBackX, rowY, nameBackW, rowHVisible);
            Builder.text().text(entry.name()).font(BIKO_FONT.get()).size(7f).thickness(0.06f).color(Color.WHITE)
                    .build().render(matrix, nameBackX + pad, rowCenterY - 3.5f);
            ScissorUtils.pop();

            ScissorUtils.push();
            ScissorUtils.setFromComponentCoordinates(bindBackX, rowY, bindBackW, rowHVisible);
            float bindW = BIKO_FONT.get().getWidth(entry.bind(), 6f);
            float bindX = bindBackX + (bindBackW - bindW) / 2f;
            Builder.text().text(entry.bind()).font(BIKO_FONT.get()).size(6f).thickness(0.06f).color(Color.WHITE)
                    .build().render(matrix, bindX, rowCenterY - 3f);
            ScissorUtils.pop();

            ScissorUtils.pop();

            off += rowH + rowGap;
        }

        context.getMatrices().pop();
    }

    private String getCategoryGlyph(ModuleCategory cat) {
        return switch (cat) {
            case COMBAT -> "0"; case MOVEMENT -> "1"; case PLAYER -> "2";
            case RENDER -> "3"; case MISC -> "4"; case THEMES -> "4";
        };
    }
}