package fun.ogi.module;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.lwjgl.glfw.GLFW;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ModuleInformation {
    String moduleName();
    String moduleDesc() default "";
    ModuleCategory moduleCategory();
    int moduleKeybind() default GLFW.GLFW_KEY_UNKNOWN;
}

