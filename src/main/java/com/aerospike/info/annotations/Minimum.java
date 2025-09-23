package com.aerospike.info.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * For numeric fields, merge values from different nodes together selecting the minimum value.
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface Minimum {

}
