package me.mathjx.extensiblefood.src.format.processor.test;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic.Kind;

public final class PElement implements Spanned {

	public final PElement parent;
	public final String pathElement;

	public final Spanned defSite;

	public boolean optional;

	public String description;

	public String[] notes;

	public int order;

	public PElement(PElement parent, String pathElement, Spanned defSite) {
		this.parent = parent;
		this.pathElement = pathElement;
		this.defSite = defSite;
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

}
