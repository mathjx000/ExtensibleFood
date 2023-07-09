package me.mathjx.extensiblefood.src.format.processor.test;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic.Kind;

@Deprecated(forRemoval = true)
public record Identified<T>(Spanned span, T value) implements Spanned {

	@Override
	public void message(Messager messager, Kind kind, CharSequence message) {
		if (span != null)
			span.message(messager, kind, message);
	}

}
