/*******************************************************************************
 * Copyright (c) 2017 Ecliptical Software Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DSEnums {

	private static final Map<String, String> CONFIGURATION_OPTION;

	static {
		HashMap<String, String> m = new HashMap<>(3);
		m.put("OPTIONAL", "optional"); //$NON-NLS-1$ //$NON-NLS-2$
		m.put("REQUIRE", "require"); //$NON-NLS-1$ //$NON-NLS-2$
		m.put("IGNORE", "ignore"); //$NON-NLS-1$ //$NON-NLS-2$
		CONFIGURATION_OPTION = Collections.unmodifiableMap(m);
	}

	private static final Map<String, String> REFERENCE_CARDINALITY;

	static {
		HashMap<String, String> m = new HashMap<>(4);
		m.put("OPTIONAL", "0..1"); //$NON-NLS-1$ //$NON-NLS-2$
		m.put("MANDATORY", "1..1"); //$NON-NLS-1$ //$NON-NLS-2$
		m.put("MULTIPLE", "0..n"); //$NON-NLS-1$ //$NON-NLS-2$
		m.put("AT_LEAST_ONE", "1..n"); //$NON-NLS-1$ //$NON-NLS-2$
		REFERENCE_CARDINALITY = Collections.unmodifiableMap(m);
	}

	private static final Map<String, String> REFERENCE_POLICY;

	static {
		HashMap<String, String> m = new HashMap<>(2);
		m.put("STATIC", "static"); //$NON-NLS-1$ //$NON-NLS-2$
		m.put("DYNAMIC", "dynamic"); //$NON-NLS-1$ //$NON-NLS-2$
		REFERENCE_POLICY = Collections.unmodifiableMap(m);
	}

	private static final Map<String, String> REFERENCE_POLICY_OPTION;

	static {
		HashMap<String, String> m = new HashMap<>(2);
		m.put("RELUCTANT", "reluctant"); //$NON-NLS-1$ //$NON-NLS-2$
		m.put("GREEDY", "greedy"); //$NON-NLS-1$ //$NON-NLS-2$
		REFERENCE_POLICY_OPTION = Collections.unmodifiableMap(m);
	}

	private static final Map<String, String> SERVICE_SCOPE;

	static {
		HashMap<String, String> m = new HashMap<>(4);
		m.put("SINGLETON", "singleton"); //$NON-NLS-1$ //$NON-NLS-2$
		m.put("BUNDLE", "bundle"); //$NON-NLS-1$ //$NON-NLS-2$
		m.put("PROTOTYPE", "prototype"); //$NON-NLS-1$ //$NON-NLS-2$
		m.put("DEFAULT", "<<default>>"); //$NON-NLS-1$ //$NON-NLS-2$
		SERVICE_SCOPE = Collections.unmodifiableMap(m);
	}

	private static final Map<String, String> REFERENCE_SCOPE;

	static {
		HashMap<String, String> m = new HashMap<>(3);
		m.put("BUNDLE", "bundle"); //$NON-NLS-1$ //$NON-NLS-2$
		m.put("PROTOTYPE", "prototype"); //$NON-NLS-1$ //$NON-NLS-2$
		m.put("PROTOTYPE_REQUIRED", "prototype_required"); //$NON-NLS-1$ //$NON-NLS-2$
		REFERENCE_SCOPE = Collections.unmodifiableMap(m);
	}

	private static final Map<String, String> FIELD_OPTION;

	static {
		HashMap<String, String> m = new HashMap<>(2);
		m.put("UPDATE", "update"); //$NON-NLS-1$ //$NON-NLS-2$
		m.put("REPLACE", "replace"); //$NON-NLS-1$ //$NON-NLS-2$
		FIELD_OPTION = Collections.unmodifiableMap(m);
	}

	private DSEnums() {
		super();
	}

	public static String getConfigurationPolicy(String literal) {
		return CONFIGURATION_OPTION.get(literal);
	}

	public static String getReferenceCardinality(String literal) {
		return REFERENCE_CARDINALITY.get(literal);
	}

	public static String getReferencePolicy(String literal) {
		return REFERENCE_POLICY.get(literal);
	}

	public static String getReferencePolicyOption(String literal) {
		return REFERENCE_POLICY_OPTION.get(literal);
	}

	public static String getServiceScope(String literal) {
		return SERVICE_SCOPE.get(literal);
	}

	public static String getReferenceScope(String literal) {
		return REFERENCE_SCOPE.get(literal);
	}

	public static String getFieldOption(String literal) {
		return FIELD_OPTION.get(literal);
	}
}
