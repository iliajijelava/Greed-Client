package fun.ogi.util.altmanager;

import fun.ogi.mixin.MinecraftClientAccessor;
import net.minecraft.client.session.Session;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static fun.ogi.util.MinecraftUtil.mc;


public class SessionUtil {

    public static void setOfflineSession(String username) {

        Session session = new Session(
                username,
                UUID.nameUUIDFromBytes(
                        ("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)
                ),
                "",
                Optional.empty(),
                Optional.empty(),
                Session.AccountType.LEGACY
        );

        ((MinecraftClientAccessor) mc).setSession(session);
    }
}

