package me.mathjx.extensiblefood.util;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

/**
 * <strong>WARNING: {@link CommandRegistryAccess} gives access to things that
 * are available only for specific games saves.</strong>
 * 
 * Must be used with care to only use it for static known things.
 */
//@Deprecated(forRemoval = false)
public record UnsafeCommandRegistryAccess(CommandRegistryAccess unsafeAccess) {

	public <T> UnsafeRegistryWrapper<T> unsafeCreateWrapper(RegistryKey<? extends Registry<T>> var1) {
		return new UnsafeRegistryWrapper<>(unsafeAccess.createWrapper(var1));
	}

}
