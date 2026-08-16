package fun.ogi.module.impl.list.misc;

import com.google.common.eventbus.Subscribe;
import fun.ogi.Cheap;
import fun.ogi.events.render.EventHandledScreenRender;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.mixin.HandledScreenAccessor;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.ListSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleInformation(
        moduleName = "Auc helper",
        moduleDesc = "Helps in auction",
        moduleCategory = ModuleCategory.MISC
)
public class AucHelper extends Module {

    private final BooleanSetting showPriceForOne =
            new BooleanSetting(
                    "Show price for one item?",
                    this,
                    true
            );

    private final BooleanSetting highlightCheap =
            new BooleanSetting(
                    "Highlight cheap items",
                    this,
                    true
            );

    private final ListSetting choosedEnchants =
            new ListSetting(
                    "Highlite enchants: ",
                    this,
                    "Sharpness",
                    "Knockback",
                    "Fire Aspect",
                    "Looting",
                    "Efficiency",
                    "Бульдозер"
            );

    public AucHelper() {
        addSettings(
                showPriceForOne,
                highlightCheap,
                choosedEnchants
        );
    }


    
    
    

    private static final Pattern PRICE_VALUE =
            Pattern.compile(
                    "[$＄]\\s*([\\d][\\d,]*(?:\\.\\d+)?)"
            );

    private static final String PER_ITEM_PREFIX =
            "Цена за 1 шт:";

    private static final int LABEL_COLOR =
            16777190;

    private static final int PRICE_COLOR =
            65280;

    private static final DecimalFormat WHOLE_FORMAT =
            new DecimalFormat(
                    "#,###",
                    DecimalFormatSymbols.getInstance(Locale.US)
            );

    private static final DecimalFormat FRACTION_FORMAT =
            new DecimalFormat(
                    "#,##0.00",
                    DecimalFormatSymbols.getInstance(Locale.US)
            );


    
    
    

    private static final int CHEAPEST_COLOR = 0xFF4BFF4B;
    private static final int BEST_VALUE_COLOR = 0xFF33AAFF;

    private Slot cheapestSlot;
    private Slot bestValueSlot;

    private int lastUpdateTick = 0;
    private int lastSlotCount = 0;


    
    
    

    public static void process(
            ItemStack stack,
            List<Text> lines
    ) {

        


        if (stack.isEmpty())
            return;

        


        if (stack.getCount() <= 1)
            return;

        


        if (lines == null || lines.isEmpty())
            return;


        


        AucHelper module =
                Cheap.getInstance()
                        .getModuleStorage()
                        .getModules()
                        .stream()
                        .filter(m -> m instanceof AucHelper)
                        .map(m -> (AucHelper) m)
                        .findFirst()
                        .orElse(null);

        if (module == null)
            return;


        


        if (!module.isEnabled())
            return;


        
        
        

        module.highlightEnchantments(stack, lines);


        
        
        

        if (!module.showPriceForOne.getValue())
            return;


        


        LoreComponent lore =
                stack.get(DataComponentTypes.LORE);

        if (lore == null || lore.lines().isEmpty())
            return;


        


        for (
                int loreIndex = 0;
                loreIndex < lore.lines().size();
                loreIndex++
        ) {

            Text loreLine =
                    lore.lines().get(loreIndex);

            String plain =
                    normalize(
                            loreLine.getString()
                    );


            



            if (plain.contains(PER_ITEM_PREFIX))
                return;


            



            if (!containsPriceLabel(plain))
                continue;


            


            Optional<Double> totalPrice =
                    parsePrice(plain);

            if (totalPrice.isEmpty())
                continue;


            



            int insertIndex =
                    findTooltipInsertIndex(
                            lines,
                            loreIndex,
                            plain
                    );

            if (insertIndex < 0)
                continue;


            


            if (insertIndex + 1 < lines.size()) {

                String nextLine =
                        normalize(
                                lines.get(insertIndex + 1)
                                        .getString()
                        );

                if (nextLine.contains(PER_ITEM_PREFIX))
                    return;
            }


            


            double perItem =
                    totalPrice.get()
                            / stack.getCount();


            


            lines.add(
                    insertIndex + 1,
                    buildPerItemLine(perItem)
            );

            return;
        }
    }


    
    
    

    @Subscribe
    public void onUpdate(EventUpdate event) {

        if (mc.player == null)
            return;

        if (!(mc.currentScreen instanceof GenericContainerScreen)) {
            cheapestSlot = null;
            bestValueSlot = null;
            lastSlotCount = 0;
            return;
        }

        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) {
            cheapestSlot = null;
            bestValueSlot = null;
            return;
        }

        int currentSlotCount = handler.slots.size();

        if (currentSlotCount != lastSlotCount || mc.player.age - lastUpdateTick > 5) {
            lastUpdateTick = mc.player.age;
            lastSlotCount = currentSlotCount;
            updateCheapSlots(handler);
        }
    }

    @Subscribe
    public void onHandledScreenRender(EventHandledScreenRender event) {

        if (!highlightCheap.getValue())
            return;

        if (!(event.getScreen() instanceof GenericContainerScreen screen))
            return;

        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        DrawContext context = event.getDrawContext();

        long time = System.currentTimeMillis();

        if (cheapestSlot != null && isValidSlot(cheapestSlot, screen)) {
            int color = getBlinkingColor(CHEAPEST_COLOR, time, 500);
            highlightSlot(context, accessor.ogi$getX(), accessor.ogi$getY(), cheapestSlot, color);
        }

        if (bestValueSlot != null && isValidSlot(bestValueSlot, screen) && bestValueSlot != cheapestSlot) {
            int color = getBlinkingColor(BEST_VALUE_COLOR, time, 600);
            highlightSlot(context, accessor.ogi$getX(), accessor.ogi$getY(), bestValueSlot, color);
        }
    }

    private void updateCheapSlots(GenericContainerScreenHandler handler) {

        cheapestSlot = null;
        bestValueSlot = null;

        int bestTotal = Integer.MAX_VALUE;
        int bestPerItem = Integer.MAX_VALUE;

        for (Slot slot : handler.slots) {

            if (slot.inventory == mc.player.getInventory())
                continue;

            ItemStack stack = slot.getStack();

            if (stack.isEmpty())
                continue;

            int totalPrice = parsePriceFromLore(stack);

            if (totalPrice <= 0)
                continue;

            int perItem = totalPrice / stack.getCount();

            if (totalPrice < bestTotal) {
                bestTotal = totalPrice;
                cheapestSlot = slot;
            }

            if (perItem < bestPerItem) {
                bestPerItem = perItem;
                bestValueSlot = slot;
            }
        }
    }

    private boolean isValidSlot(Slot slot, GenericContainerScreen screen) {

        if (slot == null)
            return false;

        if (slot.id < 0 || slot.id >= screen.getScreenHandler().slots.size())
            return false;

        Slot currentSlot = screen.getScreenHandler().getSlot(slot.id);

        return currentSlot.hasStack()
                && !currentSlot.getStack().isEmpty();
    }

    private void highlightSlot(
            DrawContext context,
            int offsetX,
            int offsetY,
            Slot slot,
            int color
    ) {

        context.fill(
                offsetX + slot.x,
                offsetY + slot.y,
                offsetX + slot.x + 16,
                offsetY + slot.y + 16,
                color
        );
    }

    private int getBlinkingColor(int color, long time, int periodMs) {

        float alpha =
                (float) (
                        Math.sin((double) time / periodMs * Math.PI)
                                * 0.3f
                                + 0.7f
                );

        alpha = Math.max(0.3f, Math.min(1f, alpha));

        int a = (int) (255 * alpha);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }


    
    
    

    private void highlightEnchantments(ItemStack stack, List<Text> lines) {

        if (choosedEnchants.getSelected().isEmpty())
            return;

        ItemEnchantmentsComponent enchants =
                stack.getOrDefault(
                        DataComponentTypes.ENCHANTMENTS,
                        ItemEnchantmentsComponent.DEFAULT
                );

        if (enchants.isEmpty())
            return;

        for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {

            int level = enchants.getLevel(entry);

            String displayName =
                    Enchantment.getName(entry, level)
                            .getString();

            String path =
                    entry.getKey()
                            .map(key -> key.getValue().getPath())
                            .orElse(null);

            String option = findSelectedOption(path, displayName);

            if (option == null)
                continue;

            int color = getEnchantColor(option);
            String target = normalize(displayName);

            for (int i = 0; i < lines.size(); i++) {

                String linePlain =
                        normalize(
                                lines.get(i)
                                        .getString()
                        );

                if (linePlain.contains(target)) {

                    lines.set(
                            i,
                            Text.literal(linePlain)
                                    .setStyle(
                                            lines.get(i).getStyle()
                                                    .withColor(color)
                                                    .withItalic(false)
                                    )
                    );
                }
            }
        }
    }

    private String findSelectedOption(String path, String displayName) {

        for (String option : choosedEnchants.getSelected()) {
            if (matchesEnchant(option, path, displayName))
                return option;
        }

        return null;
    }

    private boolean matchesEnchant(String option, String path, String displayName) {

        String normalized = normalizeEnchantKey(option);

        if (normalized.isEmpty())
            return false;

        



        if (path != null) {
            String pathNormalized = normalizeEnchantKey(path);
            if (pathNormalized.contains(normalized))
                return true;
            if (normalized.contains(pathNormalized))
                return true;
        }

        



        if (displayName == null || displayName.isEmpty())
            return false;

        String optionLower = option.toLowerCase(Locale.ROOT).trim();
        String displayLower = displayName.toLowerCase(Locale.ROOT).trim();

        return displayLower.contains(optionLower);
    }

    private int getEnchantColor(String option) {

        switch (option) {
            case "Sharpness":
                return 0x00FF00;
            case "Knockback":
                return 0xFFFF00;
            case "Fire Aspect":
                return 0xFF0000;
            case "Looting":
                return 0x00FFFF;
            case "Efficiency":
                return 0x5555FF;
            case "Бульдозер":
                return 0xFF55FF;
            default:
                return 0xFFFFFF;
        }
    }

    private static String normalizeEnchantKey(String text) {

        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-zа-яё0-9]", "");
    }


    
    
    

    private static int findTooltipInsertIndex(
            List<Text> lines,
            int loreIndex,
            String lorePlain
    ) {

        


        for (int i = 0; i < lines.size(); i++) {

            String tooltipPlain =
                    normalize(
                            lines.get(i)
                                    .getString()
                    );

            if (tooltipPlain.equals(lorePlain))
                return i;
        }


        



        for (int i = 0; i < lines.size(); i++) {

            String plain =
                    normalize(
                            lines.get(i)
                                    .getString()
                    );

            if (
                    containsPriceLabel(plain)
                            && parsePrice(plain).isPresent()
            ) {

                return i;
            }
        }


        





        int itemNameSkipped = 1;

        int target =
                itemNameSkipped + loreIndex;

        return target < lines.size()
                ? target
                : -1;
    }


    
    
    

    private static boolean containsPriceLabel(
            String plain
    ) {

        String lower =
                plain.toLowerCase(
                        Locale.ROOT
                );

        return lower.contains("цен")
                && lower.contains(":");
    }


    
    
    

    private static Optional<Double> parsePrice(
            String plain
    ) {

        Matcher matcher =
                PRICE_VALUE.matcher(plain);

        Double last = null;

        while (matcher.find()) {

            String raw =
                    matcher.group(1)
                            .replace(",", "");

            try {

                last =
                        Double.parseDouble(raw);

            } catch (NumberFormatException ignored) {

                return Optional.empty();
            }
        }

        return Optional.ofNullable(last);
    }

    private static int parsePriceFromLore(ItemStack stack) {

        LoreComponent lore =
                stack.get(DataComponentTypes.LORE);

        if (lore == null || lore.lines().isEmpty())
            return 0;

        for (Text line : lore.lines()) {

            String plain =
                    normalize(line.getString());

            if (!containsPriceLabel(plain))
                continue;

            Optional<Double> price = parsePrice(plain);

            if (price.isPresent())
                return (int) Math.round(price.get());
        }

        return 0;
    }


    
    
    

    private static Text buildPerItemLine(
            double perItem
    ) {

        return Text.literal("$").styled(style -> style.withColor(PRICE_COLOR).withItalic(false)).append(Text.literal(" " + PER_ITEM_PREFIX + " ").styled(style -> style.withColor(LABEL_COLOR).withItalic(false))).append(Text.literal("$" + formatPrice(perItem)).styled(style -> style.withColor(PRICE_COLOR).withItalic(false)));
    }


    
    
    

    private static String normalize(
            String text
    ) {

        if (text == null)
            return "";

        


        text =
                text.replaceAll(
                        "§[0-9a-fk-or]",
                        ""
                );

        


        return text
                .replace('\u00A0', ' ')
                .trim();
    }


    
    
    

    private static String formatPrice(
            double price
    ) {

        





        if (Math.rint(price) == price)
            return WHOLE_FORMAT.format(price);


        




        return FRACTION_FORMAT.format(price);
    }
}

