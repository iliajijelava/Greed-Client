package fun.ogi;

import com.ferra13671.discordipc.AvatarType;
import com.ferra13671.discordipc.DiscordIPC;
import com.ferra13671.discordipc.UserAvatar;
import com.ferra13671.discordipc.activity.Button;
import com.ferra13671.discordipc.activity.RichPresence;
import com.google.common.eventbus.EventBus;
import fun.ogi.command.CommandManager;
import fun.ogi.module.ModuleStorage;
import fun.ogi.module.theme.ThemeManager;
import fun.ogi.util.altmanager.AltManager;
import fun.ogi.util.config.ConfigManager;
import fun.ogi.util.friend.FriendManager;
import fun.ogi.util.funevents.FunEventsUtil;
import fun.ogi.util.neuro.rotation.NeuroOverlay;
import fun.ogi.util.rotation.FreeLookComponent;
import fun.ogi.util.rotation.RotationComponent;
import fun.ogi.util.staff.StaffManager;
import fun.ogi.util.storages.WaypointStorage;
import fun.ogi.util.tps.TPSCalc;
import net.fabricmc.api.ModInitializer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Cheap implements ModInitializer {
	public static final String MOD_ID = "cheap";
    public MinecraftClient mc = MinecraftClient.getInstance();
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public ModuleStorage moduleStorage;
    public EventBus eventBus = new EventBus();
    private CommandManager commandManager;
    private FriendManager friendManager;
    private StaffManager staffManager;
    private ConfigManager configManager;
    private static Cheap instance;
    private File cheapDir;

    public TPSCalc tpsCalc;

	@Override
	public void onInitialize() {
        instance = this;
        cheapDir = new File(net.minecraft.client.MinecraftClient.getInstance().runDirectory, "Greed");
        if(!cheapDir.exists()) cheapDir.mkdirs();
        moduleStorage = new ModuleStorage();
        moduleStorage.injectRegisterModules();
        AltManager.load();
        commandManager = new CommandManager();
        friendManager = new FriendManager();
        eventBus.register(FreeLookComponent.getInstance());
        eventBus.register(RotationComponent.getInstance());
        eventBus.register(new NeuroOverlay());
        eventBus.register(WaypointStorage.getInstance());
        staffManager = new StaffManager();
        configManager = new ConfigManager();
        tpsCalc= new TPSCalc();
        ThemeManager.getInstance().initialize();
        registerSounds();
        startRPCThread();

		LOGGER.info("WELCOME MY GOOOD BOOOOOOOOOOOOY!");
	}

    private void startRPCThread() {
        Thread thread = new Thread(Cheap::startRPC, "Discord-IPC");
        thread.setDaemon(true);
        thread.start();
        Runtime.getRuntime().addShutdownHook(new Thread(Cheap::stopRPC, "Discord-IPC-Shutdown"));
    }

    private void debugFunEvents() {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(5000);
                new FunEventsUtil().logRandomEventDebug();
            } catch (InterruptedException ignored) {
            }
        }, "FunEvents-Debug");
        thread.setDaemon(true);
        thread.start();
    }
    public static Cheap getInstance() {
        return instance;
    }

    public ModuleStorage getModuleStorage(){
        return moduleStorage;
    }
    public EventBus getEventBus() {
        return eventBus;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }
    public FriendManager getFriendManager() {
        return friendManager;
    }

    public StaffManager getStaffManager() {
        return staffManager;
    }
    public ConfigManager getConfigManager() {
        return configManager;
    }
    public File getCheapDir(){
        return cheapDir;
    }

    private void registerSounds() {
        registerSound("opengui");
        registerSound("closegui");
        registerSound("first");
        registerSound("second");
        registerSound("third");
        registerSound("fourth");
        registerSound("fifth");
        registerSound("sixth");
    }

    private void registerSound(String name) {
        Identifier id = Identifier.of(MOD_ID, name);
        Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }
    public static void startRPC() {

        
        if (!DiscordIPC.start(1537370405067694140L, () -> System.out.println("Discord IPC подключен для пользователя: " + DiscordIPC.getUser().username()))) {
            System.out.println("Не удалось запустить Discord IPC");
            return;
        }

        
        RichPresence presence = new RichPresence();
       




        presence.update(activityInfo ->
                activityInfo
                        .setDetails("VER 1.0.0")
                        .setState("DEV")
                        .setLargeImage("discord.png")
                        .setLargeText("dev")
                        .setSmallImage("b")
                        .setSmallText("Small Image")
                        
                        .setButtons(new Button("Download", "https://t.me/greedvlog")) 
        );
        
        DiscordIPC.setRichPresence(presence);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }



        



        UserAvatar userAvatar = DiscordIPC.getUser().getAvatarImage();
        if (userAvatar != null) {
            Path path = Paths.get("Avatar." + (userAvatar.avatarType() == AvatarType.Image ? "png" : "gif"));
            try (OutputStream outputStream = Files.newOutputStream(path)) {
                outputStream.write(userAvatar.inputStream().readAllBytes());
                userAvatar.inputStream().close();
                System.out.printf("Аватарка пользователя была сохранена по пути '%s'", path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void stopRPC() {
        
        DiscordIPC.stop();
    }
}