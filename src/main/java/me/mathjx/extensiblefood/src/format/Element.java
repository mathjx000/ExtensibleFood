package me.mathjx.extensiblefood.src.format;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, ANNOTATION_TYPE, RECORD_COMPONENT })
@Repeatable(Elements.class)
public @interface Element {

	public String[] path();

	public boolean optional() default false;

	public String description() default "";

	public Type[] type() default {};

	public String[] notes() default {};

	public Id id() default @Id;

	public Relation[] relation() default {};

	public Group group() default @Group;

	public int order() default 0;

}
