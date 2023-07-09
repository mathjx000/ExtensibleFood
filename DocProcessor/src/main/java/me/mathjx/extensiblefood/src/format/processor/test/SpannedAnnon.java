package me.mathjx.extensiblefood.src.format.processor.test;

import java.lang.annotation.Annotation;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;

record SpannedAnnon(Element annotated, AnnotationMirror mirror) implements Spanned {

	public static SpannedAnnon getAnnotation(Element annotated, Class<? extends Annotation> annotationType) {
//		TypeElement annotationType = environment.getElementUtils().getTypeElement(annotation.getCanonicalName());

		var annotationTypeName = annotationType.getName();

		for (var mirror : annotated.getAnnotationMirrors()) {
			if (mirror.getAnnotationType().toString().equals(annotationTypeName)) {
				return new SpannedAnnon(annotated, mirror);
			}
		}

		throw new NoSuchElementException();
	}

	public Optional<SpannedValue> getValue(String name) {
		for (var entry : mirror.getElementValues().entrySet()) {
			if (entry.getKey().getSimpleName().contentEquals(name)) {
				return Optional.of(new SpannedValue(this, entry.getValue()));
			}
		}

		return Optional.empty();
	}

	public SpannedValue getValue(ProcessingEnvironment env, String name) {
		for (var entry : env.getElementUtils().getElementValuesWithDefaults(mirror).entrySet()) {
			if (entry.getKey().getSimpleName().contentEquals(name)) {
				return new SpannedValue(this, entry.getValue());
			}
		}

		throw new NoSuchElementException();
	}

	@Override
	public void message(Messager messager, Kind kind, CharSequence message) {
		messager.printMessage(kind, message, annotated, mirror);
	}

}