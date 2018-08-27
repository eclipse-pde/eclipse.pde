/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
package x.y.z;

import java.util.Arrays;

import c.ClassUsageClass;
import m.MethodUsageClass;
import f.FieldUsageClass;

/**
 * Tests for illegal use inside lambdas - lambda block statement
 *
 */
public class testLambdaBlockStatement {
	
	void m1() {
		FieldUsageClass noRef = new FieldUsageClass();
		String[] array = {"one"};
		Arrays.sort(array, (String s1, String s2) -> {
			ClassUsageClass noRef2 = new ClassUsageClass();
			return noRef.f1;
		});
		Arrays.sort(array, (String s1, String s2) -> {
			ClassUsageClass.inner noRef2 = new ClassUsageClass.inner();
			return FieldUsageClass.f2;
		});
	}

}

