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
package org.eclipse.pde.internal.ui.wizards.tools;

public interface IOrganizeManifestsSettings {

	public static final String PROP_ADD_MISSING = "OrganizeManifests.ExportedPackages.addMissing"; //$NON-NLS-1$
	public static final String PROP_MARK_INTERNAL = "OrganizeManifests.ExportedPackages.makeInternal"; //$NON-NLS-1$
	public static final String PROP_INTERAL_PACKAGE_FILTER = "OrganizeManifests.ExportedPackages.packageFilter"; //$NON-NLS-1$
	public static final String VALUE_DEFAULT_FILTER = "*.internal*"; //$NON-NLS-1$
	public static final String PROP_REMOVE_UNRESOLVED_EX = "OrganizeManifests.ExportedPackages.removeUnresolved"; //$NON-NLS-1$
	public static final String PROP_CALCULATE_USES = "OrganizeManifests.calculateUses"; //$NON-NLS-1$
	public static final String PROP_MODIFY_DEP = "OrganizeManifests.RequireImport.modifyDep"; //$NON-NLS-1$
	public static final String PROP_RESOLVE_IMP_MARK_OPT = "OrganizeManifests.RequireImport.resolve:markOptional"; //$NON-NLS-1$
	public static final String PROP_UNUSED_DEPENDENCIES = "OrganizeManifests.RequireImport.findRemoveUnused"; //$NON-NLS-1$
	public static final String PROP_ADD_DEPENDENCIES = "OrganizeManifests.AddDependencies"; //$NON-NLS-1$
	public static final String PROP_REMOVE_LAZY = "OrganizeManifests.General.cleanup"; //$NON-NLS-1$
	public static final String PROP_REMOVE_USELESSFILES = "OrganizeManifests.General.cleanup.removeUselessFiles"; //$NON-NLS-1$
	public static final String PROP_NLS_PATH = "OrganizeManifests.Translation.nls"; //$NON-NLS-1$
	public static final String PROP_UNUSED_KEYS = "OrganizeManifests.Translation.unusedKeys"; //$NON-NLS-1$
}