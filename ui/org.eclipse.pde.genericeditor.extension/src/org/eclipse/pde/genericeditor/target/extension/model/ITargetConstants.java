/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.genericeditor.target.extension.model;

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
	public static final String UNIT_ID_ATTR = "id"; //$NON-NLS-1$
	public static final String UNIT_VERSION_ATTR = "version"; //$NON-NLS-1$
	public static final String TARGET_NAME_ATTR = "name"; //$NON-NLS-1$
	public static final String TARGET_SEQ_NO_ATTR = "sequenceNumber"; //$NON-NLS-1$
	public static final String LOCATION_INCLUDE_PLATFORMS_ATTR = "includeAllPlatforms"; //$NON-NLS-1$
	public static final String LOCATION_INCLUDE_CONFIG_PHASE_ATTR = "includeConfigurePhase"; //$NON-NLS-1$
	public static final String LOCATION_INCLUDE_MODE_ATTR = "includeMode"; //$NON-NLS-1$
	public static final String LOCATION_INCLUDE_SOURCE_ATTR = "includeSource"; //$NON-NLS-1$
	public static final String LOCATION_TYPE_ATTR = "type"; //$NON-NLS-1$
	public static final String REPOSITORY_LOCATION_ATTR = "location"; //$NON-NLS-1$
	public static final String TARGET_JRE_PATH_ATTR = "path"; //$NON-NLS-1$
	public static final String UNIT_VERSION_ATTR_GENERIC = "0.0.0"; //$NON-NLS-1$

}
