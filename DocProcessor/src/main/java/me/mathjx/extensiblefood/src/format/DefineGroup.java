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

/**
 * Definition of a group.
 * 
 * Groups can be nested.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, ANNOTATION_TYPE, RECORD_COMPONENT })
@Repeatable(DefineGroups.class)
public @interface DefineGroup {

	/**
	 * Dot separated path of the group
	 */
	public String path();

	/**
	 * The (human) name of the group
	 */
	public String name();

	/**
	 * A description of the group
	 */
	public String description() default "";

	/**
	 * A unique id that can be referenced.
	 */
	public String id() default "";

	public int order() default 0;

}
