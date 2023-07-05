package me.mathjx.extensiblefood.item;

import java.util.HashMap;
import java.util.HashSet;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.RegistryKey;

public final class ItemGroupApplier {

	private final Multimap<RegistryKey<ItemGroup>, Item> multimap = Multimaps.newSetMultimap(new HashMap<>(),
			HashSet::new);

	public ItemGroupApplier() {
	}

	public void addToExisting(Item item, RegistryKey<ItemGroup> group) {
		multimap.put(group, item);
	}

	public void apply() {
		for (var e : multimap.asMap().entrySet()) {
			var items = e.getValue();
			ItemGroupEvents.modifyEntriesEvent(e.getKey()).register(content -> items.forEach(content::add));
		}
	}

}
