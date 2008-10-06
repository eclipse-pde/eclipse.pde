/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.jdt.core.JavaCore;

public class ExecutionEnvironmentAnalyzer {

	private static final String JavaSE_1_6 = "JavaSE-1.6"; //$NON-NLS-1$
	private static final String J2SE_1_5 = "J2SE-1.5"; //$NON-NLS-1$
	private static final String J2SE_1_4 = "J2SE-1.4"; //$NON-NLS-1$
	private static final String J2SE_1_3 = "J2SE-1.3"; //$NON-NLS-1$
	private static final String J2SE_1_2 = "J2SE-1.2"; //$NON-NLS-1$
	private static final String JRE_1_1 = "JRE-1.1"; //$NON-NLS-1$

	private static final String CDC_FOUNDATION_1_1 = "CDC-1.1/Foundation-1.1"; //$NON-NLS-1$
	private static final String CDC_FOUNDATION_1_0 = "CDC-1.0/Foundation-1.0"; //$NON-NLS-1$

	private static final String OSGI_MINIMUM_1_0 = "OSGi/Minimum-1.0"; //$NON-NLS-1$
	private static final String OSGI_MINIMUM_1_1 = "OSGi/Minimum-1.1"; //$NON-NLS-1$

	private static final Map mappings = new HashMap();

	static {
		// table where the key is the EE and the value is an array of EEs that it is a super-set of
		mappings.put(CDC_FOUNDATION_1_0, new String[] {OSGI_MINIMUM_1_0});
		mappings.put(CDC_FOUNDATION_1_1, new String[] {CDC_FOUNDATION_1_0, OSGI_MINIMUM_1_1});
		mappings.put(OSGI_MINIMUM_1_1, new String[] {OSGI_MINIMUM_1_0});
		mappings.put(J2SE_1_2, new String[] {JRE_1_1});
		mappings.put(J2SE_1_3, new String[] {J2SE_1_2, CDC_FOUNDATION_1_0, OSGI_MINIMUM_1_0});
		mappings.put(J2SE_1_4, new String[] {J2SE_1_3, CDC_FOUNDATION_1_1, OSGI_MINIMUM_1_1});
		mappings.put(J2SE_1_5, new String[] {J2SE_1_4});
		mappings.put(JavaSE_1_6, new String[] {J2SE_1_5});
	}

	public static String getCompliance(String ee) {
		if (ee == null)
			return null;
		if (JavaSE_1_6.equals(ee))
			return JavaCore.VERSION_1_6;
		if (J2SE_1_5.equals(ee))
			return JavaCore.VERSION_1_5;
		if (J2SE_1_4.equals(ee) || CDC_FOUNDATION_1_1.equals(ee))
			return JavaCore.VERSION_1_4;
		return JavaCore.VERSION_1_3;
	}

	public static String[] getKnownExecutionEnvironments() {
		return new String[] {JRE_1_1, J2SE_1_2, OSGI_MINIMUM_1_0, OSGI_MINIMUM_1_1, CDC_FOUNDATION_1_0, J2SE_1_3, CDC_FOUNDATION_1_1, J2SE_1_4, J2SE_1_5, JavaSE_1_6};
	}

}
