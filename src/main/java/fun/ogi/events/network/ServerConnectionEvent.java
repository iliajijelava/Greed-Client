package fun.ogi.events.network;


import fun.ogi.events.Event;
import net.minecraft.client.network.CookieStorage;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

public class ServerConnectionEvent extends Event {
   private final ServerAddress address;
   private final ServerInfo info;
   private final CookieStorage cookieStorage;


   public ServerAddress getAddress() {
      return this.address;
   }


   public ServerInfo getInfo() {
      return this.info;
   }


   public CookieStorage getCookieStorage() {
      return this.cookieStorage;
   }


   public ServerConnectionEvent(ServerAddress address, ServerInfo info, CookieStorage cookieStorage) {
      this.address = address;
      this.info = info;
      this.cookieStorage = cookieStorage;
   }
}

