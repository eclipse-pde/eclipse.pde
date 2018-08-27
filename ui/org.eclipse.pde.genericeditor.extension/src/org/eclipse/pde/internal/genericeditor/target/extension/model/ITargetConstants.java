/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others
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

	public static final String TARGET_TAG = "target"; //$NON-NLS-1$
	public static final String TARGET_JRE_TAG = "targetJRE"; //$NON-NLS-1$
	public static final String LOCATION_TAG = "location"; //$NON-NLS-1$
	public static final String LOCATIONS_TAG = "locations"; //$NON-NLS-1$
	public static final String REPOSITORY_TAG = "repository"; //$NON-NLS-1$
	public static final String UNIT_TAG = "unit"; //$NON-NLS-1$
	public static final String LAUNCHER_ARGS_TAG = "launcherArgs"; //$NON-NLS-1$
	public static final String VM_ARGS_TAG = "vmArgs";
	public static final String PROGRAM_ARGS_TAG = "programArgs";
	public static final String ENVIRONMENT_TAG = "environment"; //$NON-NLS-1$
	public static final String OS_TAG = "os"; //$NON-NLS-1$
	public static final String WS_TAG = "ws"; //$NON-NLS-1$
	public static final String ARCH_TAG = "arch"; //$NON-NLS-1$
	public static final String NL_TAG = "nl"; //$NON-NLS-1$
	public static final String UNIT_ID_ATTR = "id"; //$NON-NLS-1$
	public static final String UNIT_VERSION_ATTR = "version"; //$NON-NLS-1$
	public static final String TARGET_NAME_ATTR = "name"; //$NON-NLS-1$
	public static final String TARGET_SEQ_NO_ATTR = "sequenceNumber"; //$NON-NLS-1$
	public static final String LOCATION_INCLUDE_PLATFORMS_ATTR = "includeAllPlatforms"; //$NON-NLS-1$
	public static final String LOCATION_INCLUDE_CONFIG_PHASE_ATTR = "includeConfigurePhase"; //$NON-NLS-1$
	public static final String LOCATION_INCLUDE_MODE_ATTR = "includeMode"; //$NON-NLS-1$
	public static final String LOCATION_INCLUDE_SOURCE_ATTR = "includeSource"; //$NON-NLS-1$
	public static final String LOCATION_ID_ATTR = "id"; //$NON-NLS-1$
	public static final String LOCATION_PATH_ATTR = "path"; //$NON-NLS-1$
	public static final String LOCATION_TYPE_ATTR = "type"; //$NON-NLS-1$
	public static final String REPOSITORY_LOCATION_ATTR = "location"; //$NON-NLS-1$
	public static final String TARGET_JRE_PATH_ATTR = "path"; //$NON-NLS-1$
	public static final String UNIT_VERSION_ATTR_GENERIC = "0.0.0"; //$NON-NLS-1$
	public static final String LOCATION_TYPE_ATTR_VALUE_IU = "InstallableUnit"; //$NON-NLS-1$
	public static final String LOCATION_TYPE_ATTR_VALUE_DIRECTORY = "Directory"; //$NON-NLS-1$
	public static final String LOCATION_TYPE_ATTR_VALUE_PROFILE = "Profile"; //$NON-NLS-1$
	public static final String LOCATION_TYPE_ATTR_VALUE_FEATURE = "Feature"; //$NON-NLS-1$
	public static final String LOCATION_IU_COMPLETION_LABEL = "location (Installable Unit)"; //$NON-NLS-1$
	public static final String LOCATION_DIRECTORY_COMPLETION_LABEL = "location (Directory)"; //$NON-NLS-1$
	public static final String LOCATION_PROFILE_COMPLETION_LABEL = "location (Profile)"; //$NON-NLS-1$
	public static final String LOCATION_FEATURE_COMPLETION_LABEL = "location (Feature)"; //$NON-NLS-1$

}
