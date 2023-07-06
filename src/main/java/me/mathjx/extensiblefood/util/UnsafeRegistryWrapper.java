package me.mathjx.extensiblefood.util;

import net.minecraft.registry.RegistryWrapper;

/**
 * <strong>WARNING: {@link RegistryWrapper} gives access to things that are
 * available only for specific games saves.</strong>
 * 
 * Must be used with care to only use it for static known things.
 */
//@Deprecated(forRemoval = false)
public record UnsafeRegistryWrapper<T>(RegistryWrapper<T> unsafeWrapper) {
}
