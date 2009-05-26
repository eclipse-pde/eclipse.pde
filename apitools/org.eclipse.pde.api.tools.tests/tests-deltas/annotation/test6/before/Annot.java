/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

public @interface Annot {
	String[] name() default {"test"};
	Class<? extends Object>[] getClazz() default { String.class };
	ElementType[] getElementType() default { ElementType.METHOD };
	Target[] getTarget() default { @Target({ElementType.FIELD}) };
	float[] getF() default { 1.0f };
	double[] getD() default { 1.0 };
	short[] getS() default { 1 };
	boolean[] getB() default { true };
	char[] getC() default { 'C' };
	long[] getL() default { Long.MIN_VALUE };
	byte[] getByte() default { 2 };
	int[] getInts() default { 4 };
}