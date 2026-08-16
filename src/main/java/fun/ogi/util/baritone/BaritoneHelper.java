package fun.ogi.util.baritone;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import fun.ogi.util.ClientLogger;

public final class BaritoneHelper {
   private static final MinecraftClient MC = MinecraftClient.getInstance();

   private BaritoneHelper() {
   }

   public static boolean isAvailable() {
      return classExists("baritone.api.BaritoneAPI") || classExists("baritone.BaritoneProvider");
   }

   public static void sendCommand(String command) {
      if (MC.getNetworkHandler() == null || command == null || command.isBlank()) {
         return;
      }

      String trimmed = command.trim();
      if (trimmed.startsWith("/")) {
         MC.getNetworkHandler().sendChatCommand(trimmed.substring(1));
      } else if (trimmed.startsWith("#")) {
         MC.getNetworkHandler().sendChatMessage(trimmed);
      } else {
         MC.getNetworkHandler().sendChatMessage(trimmed);
      }
   }

   public static void stop() {
      try {
         Object pathingBehavior = pathingBehavior();
         pathingBehavior.getClass().getMethod("cancelEverything").invoke(pathingBehavior);
      } catch (ReflectiveOperationException | RuntimeException exception) {
         ClientLogger.warn("Failed to stop Baritone pathing", exception);
      }
   }

   public static boolean isPathing() {
      try {
         Object pathingBehavior = pathingBehavior();
         return (Boolean)pathingBehavior.getClass().getMethod("isPathing").invoke(pathingBehavior);
      } catch (ReflectiveOperationException | RuntimeException exception) {
         ClientLogger.warn("Failed to read Baritone pathing state", exception);
         return false;
      }
   }

   public static boolean pathTo(BlockPos pos) {
      try {
         Object customGoalProcess = customGoalProcess();
         Class<?> goalClass = Class.forName("baritone.api.pathing.goals.Goal");
         Class<?> goalBlockClass = Class.forName("baritone.api.pathing.goals.GoalBlock");
         Constructor<?> constructor = goalBlockClass.getConstructor(BlockPos.class);
         customGoalProcess.getClass().getMethod("setGoalAndPath", goalClass).invoke(customGoalProcess, constructor.newInstance(pos));
         return true;
      } catch (ReflectiveOperationException | RuntimeException exception) {
         ClientLogger.warn("Failed to path Baritone to block", exception);
         return false;
      }
   }

   public static boolean pathToNear(BlockPos pos, int radius) {
      try {
         Object customGoalProcess = customGoalProcess();
         Class<?> goalClass = Class.forName("baritone.api.pathing.goals.Goal");
         Class<?> goalNearClass = Class.forName("baritone.api.pathing.goals.GoalNear");
         Constructor<?> constructor = goalNearClass.getConstructor(BlockPos.class, int.class);
         customGoalProcess.getClass().getMethod("setGoalAndPath", goalClass).invoke(customGoalProcess, constructor.newInstance(pos, radius));
         return true;
      } catch (ReflectiveOperationException | RuntimeException exception) {
         ClientLogger.warn("Failed to path Baritone to near block", exception);
         return false;
      }
   }

   public static void cancelMine() {
      try {
         Object mineProcess = mineProcess();
         mineProcess.getClass().getMethod("cancel").invoke(mineProcess);
      } catch (ReflectiveOperationException | RuntimeException exception) {
         ClientLogger.warn("Failed to cancel Baritone mine process", exception);
      }
   }

   public static boolean isMineActive() {
      try {
         Object mineProcess = mineProcess();
         return (Boolean)mineProcess.getClass().getMethod("isActive").invoke(mineProcess);
      } catch (ReflectiveOperationException | RuntimeException exception) {
         ClientLogger.warn("Failed to read Baritone mine state", exception);
         return false;
      }
   }

   public static boolean mine(Block block) {
      try {
         Object mineProcess = mineProcess();
         mineProcess.getClass().getMethod("mine", Block[].class).invoke(mineProcess, (Object)new Block[]{block});
         return true;
      } catch (ReflectiveOperationException | RuntimeException exception) {
         ClientLogger.warn("Failed to start Baritone mine", exception);
         return false;
      }
   }

   public static void cancelGoal() {
      try {
         Object customGoalProcess = customGoalProcess();
         customGoalProcess.getClass().getMethod("onLostControl").invoke(customGoalProcess);
      } catch (ReflectiveOperationException | RuntimeException exception) {
         ClientLogger.warn("Failed to cancel Baritone goal", exception);
      }
   }

   public static boolean runAway(BlockPos from, int distance) {
      try {
         Object customGoalProcess = customGoalProcess();
         Class<?> goalClass = Class.forName("baritone.api.pathing.goals.Goal");
         Class<?> runAwayClass = Class.forName("baritone.api.pathing.goals.GoalRunAway");
         Object goal = createRunAwayGoal(runAwayClass, from, distance);
         if (goal == null) {
            return false;
         }

         customGoalProcess.getClass().getMethod("setGoalAndPath", goalClass).invoke(customGoalProcess, goal);
         return true;
      } catch (ReflectiveOperationException | RuntimeException exception) {
         ClientLogger.warn("Failed to start Baritone run-away goal", exception);
         return false;
      }
   }

   private static boolean classExists(String className) {
      try {
         Class.forName(className);
         return true;
      } catch (ClassNotFoundException ignored) {
         return false;
      }
   }

   private static Object primaryBaritone() throws ReflectiveOperationException {
      Class<?> api = Class.forName("baritone.api.BaritoneAPI");
      Object provider = api.getMethod("getProvider").invoke(null);
      return provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
   }

   private static Object pathingBehavior() throws ReflectiveOperationException {
      Object baritone = primaryBaritone();
      return baritone.getClass().getMethod("getPathingBehavior").invoke(baritone);
   }

   private static Object customGoalProcess() throws ReflectiveOperationException {
      Object baritone = primaryBaritone();
      return baritone.getClass().getMethod("getCustomGoalProcess").invoke(baritone);
   }

   private static Object mineProcess() throws ReflectiveOperationException {
      Object baritone = primaryBaritone();
      return baritone.getClass().getMethod("getMineProcess").invoke(baritone);
   }

   private static Object createRunAwayGoal(Class<?> runAwayClass, BlockPos from, int distance) throws ReflectiveOperationException {
      for (Constructor<?> constructor : runAwayClass.getConstructors()) {
         Class<?>[] params = constructor.getParameterTypes();
         if (params.length == 2 && params[1].isArray()) {
            Object posArray = Array.newInstance(params[1].getComponentType(), 1);
            Array.set(posArray, 0, from);
            if (params[0] == int.class || params[0] == Integer.class) {
               return constructor.newInstance(distance, posArray);
            }
            if (params[0] == double.class || params[0] == Double.class) {
               return constructor.newInstance((double)distance, posArray);
            }
         }
      }

      return null;
   }
}

