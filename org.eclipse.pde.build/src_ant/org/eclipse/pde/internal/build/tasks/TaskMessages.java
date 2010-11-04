/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.tasks;

import org.eclipse.osgi.util.NLS;

public class TaskMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.build.tasks.messages";//$NON-NLS-1$

	public static String error_creatingFeature;
	public static String error_readingDirectory;
	public static String error_missingElement;
	public static String error_missingDirectoryEntry;
	public static String error_runningRetrieve;

	public static String error_noArtifactRepo;
	public static String error_noMetadataRepo;
	public static String error_metadataRepoManagerService;
	public static String error_agentService;
	public static String error_artifactRepoManagerService;
	public static String error_loadRepository;
	public static String error_unmodifiableRepository;

	public static String error_invalidConfig;
	public static String error_branding;

	public static String error_callingGenerator;
	public static String error_artifactRepoNotURI;
	public static String error_metadataRepoNotURI;
	public static String error_locationNotURI;
	public static String error_noCallAfterReplace;
	public static String error_licenseRootWithoutLicenseRef;
	public static String error_conflictingProperties;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, TaskMessages.class);
	}
}
