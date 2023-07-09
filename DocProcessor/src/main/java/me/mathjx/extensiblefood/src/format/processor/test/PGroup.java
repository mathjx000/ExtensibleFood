package me.mathjx.extensiblefood.src.format.processor.test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic.Kind;

final class PGroup implements Spanned {

	public final PGroup parent;
	public final String pathElement;

	/**
	 * May be not null even if not explicitly defined.
	 */
	public final Spanned defSite;

	public String name, description;
	public String id;

	public int order;

	public final Map<String, PGroup> subGroups = new HashMap<>();

	public PGroup(PGroup parent, String pathElement, Spanned defSite, String name, String description, String id,
			int order) {
		this.parent = parent;
		this.pathElement = pathElement;
		this.defSite = defSite;
		this.name = name;
		this.description = description;
		this.id = id;
		this.order = order;
	}

	public PGroup(PGroup parent, String pathElement, Spanned defSite) {
		this.parent = parent;
		this.pathElement = pathElement;
		this.defSite = defSite;
	}

	public boolean isDefined() {
		return name != null;
	}

	public void defineWith(PGroup other) {
		this.name = other.name;
		this.description = other.description;
		this.id = other.id;
		this.order = other.order;
	}

	public PGroup getRoot() {
		PGroup group = this;
		while (group.parent != null)
			group = group.parent;
		return group;
	}

	public Optional<PGroup> getGroup(String path) {
		PGroup current = getRoot();

		for (var pelem : splitPath(path))
			if ((current = current.subGroups.get(pelem)) == null)
				return Optional.empty();

		return Optional.of(current);
	}

	public PGroup getOrCreateGroup(String path, Spanned defSite) {
		PGroup current = getRoot();

		for (var pelem : splitPath(path)) {
			final var closureVar = current;
			current = current.subGroups.computeIfAbsent(pelem,
					pelemLocal -> new PGroup(closureVar, pelemLocal, defSite));
		}

		return current;
	}

	@Override
	public void message(Messager messager, Kind kind, CharSequence message) {
		if (defSite != null)
			defSite.message(messager, kind, message);
	}

	@Override
	public Spanned span() {
		return defSite;
	}

	public static String[] splitPath(String path) {
		return path.split("\\.");
	}

}
