/*******************************************************************************
 * Copyright (c) 2008, 2025 IBM Corporation and others.
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
 *     Alexander Fedorov (ArSysOp) - support records
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.builder;

import org.eclipse.osgi.util.NLS;

public class BuilderMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.api.tools.internal.builder.buildermessages"; //$NON-NLS-1$
	public static String api_analysis_builder;
	public static String api_analysis_on_0;
	public static String building_workspace_profile;
	public static String checking_api_usage;
	public static String checking_external_dependencies;

	public static String AbstractTypeLeakDetector_vis_type_has_no_api_description;
	public static String ApiAnalysisBuilder_builder_for_project;
	public static String ApiAnalysisBuilder_finding_affected_source_files;
	public static String ApiAnalysisBuilder_initializing_analyzer;
	public static String ApiProblemFactory_problem_message_not_found;
	public static String CleaningAPIDescription;
	public static String BaseApiAnalyzer_analyzing_api;
	public static String BaseApiAnalyzer_checking_compat;
	public static String BaseApiAnalyzer_checking_since_tags;
	public static String BaseApiAnalyzer_comparing_api_profiles;
	public static String BaseApiAnalyzer_Constructor;
	public static String BaseApiAnalyzer_Method;
	public static String BaseApiAnalyzer_more_version_problems;
	public static String BaseApiAnalyzer_processing_deltas;
	public static String BaseApiAnalyzer_scanning_0;
	public static String BaseApiAnalyzer_validating_javadoc_tags;
	public static String build_wrongFileFormat;
	public static String build_saveStateComplete;
	public static String build_cannotSaveState;
	public static String undefinedRange;
	public static String reportUnsatisfiedConstraint;

	public static String ReferenceAnalyzer_analyzing_api_checking_use;
	public static String ReferenceAnalyzer_api_analysis_error;
	public static String ReferenceAnalyzer_checking_api_used_by;
	public static String ReferenceExtractor_failed_to_lookup_method;
	public static String ReferenceExtractor_failed_to_lookup_field;

	public static String TagValidator_a_private_class;
	public static String TagValidator_a_package_default_class;
	public static String TagValidator_a_package_default_constructor;
	public static String TagValidator_a_package_default_enum;
	public static String TagValidator_a_package_default_method;
	public static String TagValidator_a_static_constructor;
	public static String TagValidator_a_static_package_default_method;
	public static String TagValidator_a_package_default_field;

	public static String TagValidator_a_class;
	public static String TagValidator_a_class_that_is_not_visible;
	public static String TagValidator_a_constructor;
	public static String TagValidator_a_field;
	public static String TagValidator_a_field_that_is_not_visible;
	public static String TagValidator_a_final_class;
	public static String TagValidator_a_final_method;
	public static String TagValidator_a_method;
	public static String TagValidator_a_method_in_a_final_class;
	public static String TagValidator_a_method_that_is_not_visible;
	public static String TagValidator_a_record;
	public static String TagValidator_a_static_final_method;
	public static String TagValidator_a_static_method;
	public static String TagValidator_an_abstract_class;
	public static String TagValidator_an_annotation;
	public static String TagValidator_an_annotation_method;
	public static String TagValidator_an_enum;
	public static String TagValidator_an_enum_constant;
	public static String TagValidator_an_enum_method;
	public static String TagValidator_an_interface;
	public static String TagValidator_an_interface_field;
	public static String TagValidator_an_interface_method;
	public static String TagValidator_an_interface_that_is_not_visible;
	public static String TagValidator_annotation_field;
	public static String TagValidator_annotation_not_visible;
	public static String TagValidator_constructor_in_final_class;
	public static String TagValidator_enum_field;
	public static String TagValidator_enum_not_visible;
	public static String TagValidator_final_constructor;
	public static String TagValidator_nondefault_interface_method;
	public static String TagValidator_not_visible_constructor;
	public static String TagValidator_not_visible_enum_field;
	public static String TagValidator_not_visible_enum_method;
	public static String TagValidator_not_visible_interface_field;
	public static String TagValidator_not_visible_interface_method;
	public static String TagValidator_private_constructor;
	public static String TagValidator_private_enum_field;
	public static String TagValidator_private_enum_method;
	public static String TagValidator_private_field;
	public static String TagValidator_private_method;
	public static String TagValidator_record_not_visible;
	public static String TagValidator_static_final_constructor;
	public static String TagValidator_static_package_constructor;
	public static String IncrementalBuilder_builder_for_project;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, BuilderMessages.class);
	}

	private BuilderMessages() {
	}
}
