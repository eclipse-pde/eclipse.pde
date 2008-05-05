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
package org.eclipse.pde.api.tools.internal.builder;

import org.eclipse.osgi.util.NLS;

public class BuilderMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.api.tools.internal.builder.buildermessages"; //$NON-NLS-1$
	public static String api_analysis_builder;
	public static String api_analysis_on_0;
	public static String building_workspace_profile;
	public static String checking_api_usage;

	public static String ApiAnalysisBuilder_finding_affected_source_files;
	public static String ApiAnalysisBuilder_initializing_analyzer;
	public static String ApiProblemFactory_problem_message_not_found;
	public static String CleaningAPIDescription;
	public static String Compatibility_Analysis;
	public static String Analyzing_0_1;
	public static String BaseApiAnalyzer_analyzing_api;
	public static String BaseApiAnalyzer_checking_api_usage;
	public static String BaseApiAnalyzer_comparing_api_profiles;
	public static String BaseApiAnalyzer_scanning_0;
	public static String BaseApiAnalyzer_validating_javadoc_tags;
	public static String build_wrongFileFormat;
	public static String build_readStateProgress;
	public static String build_saveStateComplete;
	public static String build_cannotSaveState;

	public static String TagValidator_a_class;
	public static String TagValidator_a_constructor;
	public static String TagValidator_a_field;
	public static String TagValidator_a_final_field;
	public static String TagValidator_a_method;
	public static String TagValidator_an_interface;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, BuilderMessages.class);
	}

	private BuilderMessages() {
	}
}
