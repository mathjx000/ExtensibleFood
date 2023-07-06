package me.mathjx.extensiblefood.src.format;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({})
public @interface Type {

	public Kind kind();

	public Value[] values() default {};

	public String description() default "";

	public String[] notes() default {};

	public String reference() default "";

	public static enum Kind {

		STRING, INTEGER, FLOAT, BOOLEAN, OBJECT, ARRAY;

	}

}
