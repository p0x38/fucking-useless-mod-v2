package me.p0x38.fuckinguselessmod.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigOption {
    String name() default "";
    String category() default "General";
    boolean hasMin() default false;
    double min() default 0.0;
    boolean hasMax() default false;
    double max() default 0.0;
}
