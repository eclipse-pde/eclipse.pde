/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
@interface Ann {
	int key() default 4;
}
enum E {
	A, B,  C, D
}
@interface Annot {
	int value() default 0;
	E e() default E.D;
	Ann annotation();
}
public @interface X2 {
	int id() default 0;
	String[] name() default { "toto", "tata" };
	Annot annotat() default @Annot(value=10,e=E.C,annotation=@Ann());
}