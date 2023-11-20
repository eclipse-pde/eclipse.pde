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

import java.util.Map;

public class DSEnums {

	private static final Map<String, String> CONFIGURATION_OPTION = Map.of( //
			"OPTIONAL", "optional", //
			"REQUIRE", "require", //
			"IGNORE", "ignore");

	private static final Map<String, String> REFERENCE_CARDINALITY = Map.of( //
			"OPTIONAL", "0..1", //
			"MANDATORY", "1..1", //
			"MULTIPLE", "0..n", //
			"AT_LEAST_ONE", "1..n");

	private static final Map<String, String> REFERENCE_POLICY = Map.of( //
			"STATIC", "static", //
			"DYNAMIC", "dynamic");

	private static final Map<String, String> REFERENCE_POLICY_OPTION = Map.of( //
			"RELUCTANT", "reluctant", //
			"GREEDY", "greedy");

	private static final Map<String, String> SERVICE_SCOPE = Map.of( //
			"SINGLETON", "singleton", //
			"BUNDLE", "bundle", //
			"PROTOTYPE", "prototype", //
			"DEFAULT", "<<default>>");

	private static final Map<String, String> REFERENCE_SCOPE = Map.of( //
			"BUNDLE", "bundle", //
			"PROTOTYPE", "prototype", //
			"PROTOTYPE_REQUIRED", "prototype_required");

	private static final Map<String, String> FIELD_OPTION = Map.of( //
			"UPDATE", "update", //
			"REPLACE", "replace");

	public static final Map<String, String> COLLECTION_TYPE_OPTION = Map.of( //
			"SERVICE", "service", //
			"REFERENCE", "reference", //
			"SERVICEOBJECTS", "serviceobjects", //
			"PROPERTIES", "properties", //
			"TUPLE", "tuple");

	private DSEnums() {
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
