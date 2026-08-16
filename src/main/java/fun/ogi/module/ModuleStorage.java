package fun.ogi.module;

import fun.ogi.module.impl.list.combat.*;
import fun.ogi.module.impl.list.combat.TapeMouse;
import fun.ogi.module.impl.list.misc.FakePlayer;
import fun.ogi.module.impl.list.misc.WardenHelper;
import fun.ogi.module.impl.list.movement.*;
import fun.ogi.module.impl.list.movement.NoFall;
import fun.ogi.module.impl.list.player.*;
import fun.ogi.module.impl.list.render.*;
import fun.ogi.module.impl.list.misc.*;
import java.util.ArrayList;
import java.util.List;

public class ModuleStorage {
    private final List<Module> modules = new ArrayList<>();

    public void injectRegisterModules() {
        modules.addAll(List.of(
                new AucHelper(),
                new AimAssist(),
                new AimBot(),
                new AirStuck(),
                new AutoApple(),
                new Arrows(),
                new AutoTrade(),
                new AspectRatio(),
                new AutoArmor(),
                new AutoBrew(),
                new AutoRespawn(),
                new AutoSwap(),
                new AutoEvent(),
                new AutoAccept(),
                new AutoResell(),
                new AutoTotem(),
                new AutoPotion(),
                new AutoUse(),
                new AttackAura(),
                new AutoLeave(),
                new BlockOverlay(),
                new BoxEsp(),
                new BaseFinder(),
                new ChinaHat(),
                new Chams(),
                new ClanUpgrade(),
                new ClientSounds(),
                new ClickGui(),

                new ClickPearl(),
                new ClickFriend(),
                new DesYavkaOverlay(),

                new ElytraSwap(),
//                new ElytraTarget(),

                new EndHelper(),
                new FakePlayer(),
                new FastItemUse(),

                new FreeCam(),
                new FullBright(),
                new GrimGlide(),
                new GhostEsp(),



                new HitMarker(),
                new Hud(),
                new InventoryMove(),
                new InventoryCleaner(),
                new ItemScroller(),
                new JumpCircle(),
                new KTLeave(),
                new LayerCooldown(),
                new LeaveTracker(),
                new LockSlot(),
                new MineHelper(),
                new NameProtect(),
                new Nametags(),
                new NoDelay(),
                new NoFall(),
                new NoEntityTrace(),
                new NoFriendDamage(),
                new NoInteract(),
                new NoPush(),
                new NoSlow(),
                new Nimb(),
                new PacketCriticals(),
                new Particle(),
                new PotionTracker(),
                new PotionCombiner(),
                new PlayerUtils(),
                new ProjectilePredictions(),
                new Removals(),
                new ShaderESP(),

                new ShaderHands(),
                new HandFire(),
                new SkeletonEsp(),
                new Spider(),
                new Speed(),
                new Sprint(),
                new Stealer(),
                new StorageESP(),
                new ServerHelper(),
                new SwordFarm(),
                new SwingAnimations(),
                new TargetEsp(),
                new TapeMouse(),
                new Trails(),
                new Trajectories(),
                new TrapOverlay(),
                new TriggerBot(),
                new Test(),


                new ViewModel(),
                new WardenHelper(),
                new WayPanel(),
                new Wings(),
                new WheelSwap(),
                new WindHop(),
                new WorldTweaks(),
                new XCarry(),
                new XRay(),
                new LockSlot(),
                new FireFly()
        ));
    }

    public List<Module> getModules() {
        return modules;
    }

    public Module getModuleByName(String name) {
        return modules.stream()
                .filter(module -> module.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public List<Module> getCategory(ModuleCategory category) {
        return modules.stream()
                .filter(module -> module.getCategory().equals(category))
                .toList();
    }

    public <T extends Module> T get(final Class<T> clazz) {
        return modules.stream()
                .filter(module -> clazz.isAssignableFrom(module.getClass()))
                .map(clazz::cast)
                .findFirst()
                .orElse(null);
    }
}

