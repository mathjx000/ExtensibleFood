package me.mathjx.extensiblefood.src.format;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import me.mathjx.extensiblefood.src.format.Type.Kind;

@Retention(RetentionPolicy.SOURCE)
@Target({})
public @interface Relation {

	public String[] path();

	public Kind[] kind() default {};

	public boolean conflicts() default false;

	public String[] values() default {};

	public String[] notes() default {};

	/**
	 * A unique id that can be referenced.
	 */
	public String id() default "";

}
