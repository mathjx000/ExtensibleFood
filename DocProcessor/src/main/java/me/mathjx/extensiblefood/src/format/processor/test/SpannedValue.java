package me.mathjx.extensiblefood.src.format.processor.test;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.util.SimpleAnnotationValueVisitor9;
import javax.tools.Diagnostic.Kind;

record SpannedValue(SpannedAnnon annotation, AnnotationValue value) implements Spanned {

	public Object rawValue() {
		return value.getValue();
	}

	@SuppressWarnings("unchecked")
	public <V> V cast(Class<V> type) {
		var val = value.getValue();

		if (type.isInstance(val))
			return (V) val;
		else
			throw new ClassCastException();
	}

	public Stream<SpannedValue> streamArray() {
		return value.accept(new SimpleAnnotationValueVisitor9<Stream<SpannedValue>, Void>() {
			@Override
			protected Stream<SpannedValue> defaultAction(Object o, Void p) {
				throw new IllegalArgumentException("Expected an array, got " + String.valueOf(o));
			}

			@Override
			public Stream<SpannedValue> visitArray(List<? extends AnnotationValue> vals, Void p) {
				return vals.stream().map(value -> new SpannedValue(annotation, value));
			}

		}, null);
	}

	public SpannedAnnon asAnnotation() {
		return value.accept(new SimpleAnnotationValueVisitor9<SpannedAnnon, Void>() {
			@Override
			protected SpannedAnnon defaultAction(Object o, Void p) {
				throw new IllegalArgumentException("Expected an array, got " + String.valueOf(o));
			}

			@Override
			public SpannedAnnon visitAnnotation(AnnotationMirror a, Void p) {
				return new SpannedAnnon(annotation.annotated(), a);
			}
		}, null);
	}

	@Override
	public void message(Messager messager, Kind kind, CharSequence message) {
		messager.printMessage(kind, message, annotation.annotated(), annotation.mirror(), value);
	}

}