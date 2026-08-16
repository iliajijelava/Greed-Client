
package fun.ogi.screens;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.CubeMapRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import java.util.function.Function;






public final class BackgroundManager {

    





    private static final Identifier CUSTOM_PHOTO_ID =
            Identifier.of(
                    "cheap",
                    "textures/mainmenu/menu.png"
            );

    


    private static final Identifier CUSTOM_PANORAMA_ID =
            Identifier.of(
                    "cheap",
                    "mainmenu/panorama/panorama"
            );

    private static final Identifier VANILLA_PANORAMA_ID =
            Identifier.ofVanilla(
                    "textures/gui/title/background/panorama"
            );

    private static BackgroundType current =
            BackgroundType.CUSTOM_PANORAMA;

    private static float driftTime = 0.0f;

    private static final CubeMapRenderer CUSTOM_CUBE_MAP =
            new CubeMapRenderer(
                    Identifier.of(
                            "cheap",
                            "mainmenu/panorama/panorama"
                    )
            );

    private static final RotatingCubeMapRenderer CUSTOM_PANORAMA_RENDERER =
            new RotatingCubeMapRenderer(CUSTOM_CUBE_MAP);

    private static final CubeMapRenderer VANILLA_CUBE_MAP =
            new CubeMapRenderer(
                    Identifier.ofVanilla(
                            "textures/gui/title/background/panorama"
                    )
            );

    private static final RotatingCubeMapRenderer VANILLA_PANORAMA_RENDERER =
            new RotatingCubeMapRenderer(VANILLA_CUBE_MAP);

    private BackgroundManager() {
    }

    public enum BackgroundType {

        VANILLA(
                "Vanilla",
                VANILLA_PANORAMA_ID
        ),

        CUSTOM_PHOTO(
                "Photo",
                CUSTOM_PHOTO_ID
        ),

        CUSTOM_PANORAMA(
                "Custom Panorama",
                CUSTOM_PANORAMA_ID
        );

        public final String displayName;
        public final Identifier textureId;

        BackgroundType(
                String displayName,
                Identifier textureId
        ) {
            this.displayName = displayName;
            this.textureId = textureId;
        }
    }

    public static BackgroundType getCurrent() {
        return current;
    }

    public static void setCurrent(BackgroundType type) {
        if (type != null) {
            current = type;
        }
    }

    


    public static void render(
            DrawContext context,
            int width,
            int height,
            int mouseX,
            int mouseY,
            float delta,
            float appearProgress
    ) {

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client == null) {
            return;
        }

        driftTime += delta;

        switch (current) {

            case CUSTOM_PANORAMA -> {
                renderTestPanorama(
                        context,
                        width,
                        height,
                        delta
                );
            }

            case CUSTOM_PHOTO -> {
                renderCustomPhoto(
                        context,
                        width,
                        height
                );
            }

            case VANILLA -> {
                VANILLA_PANORAMA_RENDERER.render(
                        context,
                        width,
                        height,
                        1.0f,
                        delta
                );
            }
        }
    }

    





    public static void renderPanorama(
            DrawContext context,
            int width,
            int height,
            float delta
    ) {
        switch (current) {
            case CUSTOM_PANORAMA -> {
                renderTestPanorama(
                        context,
                        width,
                        height,
                        delta
                );
            }
            case CUSTOM_PHOTO -> {
                renderCustomPhoto(
                        context,
                        width,
                        height
                );
            }
            case VANILLA -> {
                VANILLA_PANORAMA_RENDERER.render(
                        context,
                        width,
                        height,
                        1.0f,
                        delta
                );
            }
        }
    }

    





    private static void renderTestPanorama(
            DrawContext context,
            int width,
            int height,
            float delta
    ) {

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client == null) {
            return;
        }

        CUSTOM_PANORAMA_RENDERER.render(
                context,
                width,
                height,
                1.0f,
                delta
        );
    }

    


    private static void renderCustomPhoto(
            DrawContext context,
            int width,
            int height
    ) {

        Function<Identifier, RenderLayer> renderLayer =
                RenderLayer::getGuiTextured;

        context.drawTexture(
                renderLayer,
                CUSTOM_PHOTO_ID,
                0,
                0,
                0.0f,
                0.0f,
                width,
                height,
                width,
                height
        );
    }
}


