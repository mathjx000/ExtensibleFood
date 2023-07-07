package me.mathjx.extensiblefood.src.format;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({})
public @interface Value {

	public String value();

	public String description() default "";

	public String[] notes() default {};

	/**
	 * A unique id that can be referenced.
	 */
	public String id() default "";

}
