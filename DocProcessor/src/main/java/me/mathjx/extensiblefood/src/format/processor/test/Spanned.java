package me.mathjx.extensiblefood.src.format.processor.test;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic.Kind;

public interface Spanned {

	default public Spanned span() {
		return this;
	}

	public void message(Messager messager, Kind kind, CharSequence message);

	default public void error(Messager messager, CharSequence message) {
		message(messager, Kind.ERROR, message);
	}

	default public void warn(Messager messager, CharSequence message) {
		message(messager, Kind.WARNING, message);
	}

	default public void warnM(Messager messager, CharSequence message) {
		message(messager, Kind.MANDATORY_WARNING, message);
	}

	default public void note(Messager messager, CharSequence message) {
		message(messager, Kind.NOTE, message);
	}

	default public void other(Messager messager, CharSequence message) {
		message(messager, Kind.OTHER, message);
	}

}
