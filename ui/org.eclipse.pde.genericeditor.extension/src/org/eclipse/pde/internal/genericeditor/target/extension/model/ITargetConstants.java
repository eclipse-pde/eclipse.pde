/*******************************************************************************
 * Copyright (c) 2016, 2022 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *     Lucas Bullen (Red Hat Inc.) - [Bug 522317] Support environment arguments tags in Generic TP editor
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.model;

/**
 * Placeholder for all the tags and tag attributes of a normal target
 * definition. Notice that the structure is not API so this is subject to change
 * and potentially incomplete. At present we are offering a best-effort support
 * which aims to support most of the known tags and attributes with completion
 * and syntax highlighting.
 */
public interface ITargetConstants {

	String TARGET_TAG = "target"; //$NON-NLS-1$
	String TARGET_JRE_TAG = "targetJRE"; //$NON-NLS-1$
	String LOCATION_TAG = "location"; //$NON-NLS-1$
	String LOCATIONS_TAG = "locations"; //$NON-NLS-1$
	String REPOSITORY_TAG = "repository"; //$NON-NLS-1$
	String UNIT_TAG = "unit"; //$NON-NLS-1$
	String LAUNCHER_ARGS_TAG = "launcherArgs"; //$NON-NLS-1$
	String VM_ARGS_TAG = "vmArgs";
	String PROGRAM_ARGS_TAG = "programArgs";
	String ENVIRONMENT_TAG = "environment"; //$NON-NLS-1$
	String OS_TAG = "os"; //$NON-NLS-1$
	String WS_TAG = "ws"; //$NON-NLS-1$
	String ARCH_TAG = "arch"; //$NON-NLS-1$
	String NL_TAG = "nl"; //$NON-NLS-1$
	String DEPENDENCIES_TAG = "dependencies";//$NON-NLS-1$
	String DEPENDENCY_TAG = "dependency";//$NON-NLS-1$
	String GROUP_ID_TAG = "groupId";//$NON-NLS-1$
	String ARTIFACT_ID_TAG = "artifactId";//$NON-NLS-1$
	String VERSION_TAG = "version";//$NON-NLS-1$
	String TYPE_TAG = "type";//$NON-NLS-1$
	String UNIT_ID_ATTR = "id"; //$NON-NLS-1$
	String UNIT_VERSION_ATTR = "version"; //$NON-NLS-1$
	String TARGET_NAME_ATTR = "name"; //$NON-NLS-1$
	String TARGET_SEQ_NO_ATTR = "sequenceNumber"; //$NON-NLS-1$
	String LOCATION_INCLUDE_PLATFORMS_ATTR = "includeAllPlatforms"; //$NON-NLS-1$
	String LOCATION_INCLUDE_CONFIG_PHASE_ATTR = "includeConfigurePhase"; //$NON-NLS-1$
	String LOCATION_FOLLOW_REPOSITORY_REFERENCES_ATTR = "followRepositoryReferences";
	String LOCATION_INCLUDE_MODE_ATTR = "includeMode"; //$NON-NLS-1$
	String LOCATION_INCLUDE_SOURCE_ATTR = "includeSource"; //$NON-NLS-1$
	String LOCATION_ID_ATTR = "id"; //$NON-NLS-1$
	String LOCATION_PATH_ATTR = "path"; //$NON-NLS-1$
	String LOCATION_TYPE_ATTR = "type"; //$NON-NLS-1$
	String REPOSITORY_LOCATION_ATTR = "location"; //$NON-NLS-1$
	String TARGET_JRE_PATH_ATTR = "path"; //$NON-NLS-1$
	String INCLUDE_DEPENDENCY_DEPTH = "includeDependencyDepth"; //$NON-NLS-1$
	String INCLUDE_DEPENDENCY_SCOPES = "includeDependencyScopes"; //$NON-NLS-1$
	String MISSING_MANIFEST = "missingManifest"; //$NON-NLS-1$
	String UNIT_VERSION_ATTR_GENERIC = "0.0.0"; //$NON-NLS-1$
	String LOCATION_TYPE_ATTR_VALUE_IU = "InstallableUnit"; //$NON-NLS-1$
	String LOCATION_TYPE_ATTR_VALUE_DIRECTORY = "Directory"; //$NON-NLS-1$
	String LOCATION_TYPE_ATTR_VALUE_PROFILE = "Profile"; //$NON-NLS-1$
	String LOCATION_TYPE_ATTR_VALUE_FEATURE = "Feature"; //$NON-NLS-1$
	String LOCATION_IU_COMPLETION_LABEL = "location (Installable Unit)"; //$NON-NLS-1$
	String LOCATION_DIRECTORY_COMPLETION_LABEL = "location (Directory)"; //$NON-NLS-1$
	String LOCATION_PROFILE_COMPLETION_LABEL = "location (Profile)"; //$NON-NLS-1$
	String LOCATION_FEATURE_COMPLETION_LABEL = "location (Feature)"; //$NON-NLS-1$

}
