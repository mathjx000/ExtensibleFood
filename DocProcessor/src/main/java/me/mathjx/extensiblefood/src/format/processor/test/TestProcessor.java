package me.mathjx.extensiblefood.src.format.processor.test;

import java.util.HashMap;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import me.mathjx.extensiblefood.src.format.DefineGroup;
import me.mathjx.extensiblefood.src.format.DefineGroups;
import me.mathjx.extensiblefood.src.format.Element;
import me.mathjx.extensiblefood.src.format.Elements;
import me.mathjx.extensiblefood.src.format.Group;

@SupportedAnnotationTypes({ "me.mathjx.extensiblefood.src.format.DefineGroup",
		"me.mathjx.extensiblefood.src.format.DefineGroups", "me.mathjx.extensiblefood.src.format.Group",
		"me.mathjx.extensiblefood.src.format.Groups", "me.mathjx.extensiblefood.src.format.Element",
		"me.mathjx.extensiblefood.src.format.Elements", })
public final class TestProcessor extends AbstractProcessor {

	private int round = 0;

	/** id -> element */
	private final HashMap<String, Spanned> idMap = new HashMap<>();

	private final PGroup rootGroup = new PGroup(null, "", null);

	public TestProcessor() {
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		processingEnv.getMessager().printMessage(Kind.OTHER, "Round " + (++round));

		Stream.concat(
				roundEnv.getElementsAnnotatedWith(DefineGroup.class).stream()
						.map(elem -> SpannedAnnon.getAnnotation(elem, DefineGroup.class)),
				roundEnv.getElementsAnnotatedWith(DefineGroups.class).stream().flatMap(elem -> {
					return SpannedAnnon.getAnnotation(elem, DefineGroups.class).getValue("value").get().streamArray()
							.map(SpannedValue::asAnnotation);
				})).forEach(annotation -> {
					var path = annotation.getValue("path").get();
					var pathStr = path.cast(String.class);
					var group = rootGroup.getOrCreateGroup(pathStr, annotation);

					if (group.isDefined()) {
						path.error(processingEnv.getMessager(), "Group '" + pathStr + "' is already defined");
						group.note(processingEnv.getMessager(), "Group '" + pathStr + "' is first defined here");
					} else {
						group.name = annotation.getValue("name").get().cast(String.class);
						group.description = annotation.getValue(processingEnv, "description").cast(String.class)
								.strip();
						group.order = annotation.getValue(processingEnv, "order").cast(Integer.class);

						annotation.getValue("id").ifPresent(id -> {
							var idStr = id.cast(String.class);

							if (registerId(idStr, group))
								group.id = idStr;
						});
					}
				});

		roundEnv.getElementsAnnotatedWith(Group.class).stream()
				.map(elem -> SpannedAnnon.getAnnotation(elem, Group.class)).forEach(annotation -> {
					// TODO
				});

		Stream.concat(
				roundEnv.getElementsAnnotatedWith(Element.class).stream()
						.map(elem -> SpannedAnnon.getAnnotation(elem, Element.class)),
				roundEnv.getElementsAnnotatedWith(Elements.class).stream().flatMap(elem -> {
					return SpannedAnnon.getAnnotation(elem, Elements.class).getValue("value").get().streamArray()
							.map(SpannedValue::asAnnotation);
				})).forEach(annotation -> {
					// TODO
				});

		if (roundEnv.processingOver()) {
			; // TODO finalize and output
		}

		return true;
	}

	private boolean registerId(String id, Spanned value) {
		var existing = idMap.putIfAbsent(id, value);

		if (existing != null) {
			value.error(processingEnv.getMessager(), "Duplicated id '" + id + '\'');
			existing.note(processingEnv.getMessager(), "Id '" + id + "' first defined here");

			return false;
		}

		return true;
	}

}
