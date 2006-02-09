package org.eclipse.pde.internal.ui.wizards.tools;

public interface IOrganizeManifestsSettings {

	public static final String PROP_ADD_MISSING = "OrganizeManifests.ExportedPackages.addMissing"; //$NON-NLS-1$
	public static final String PROP_MARK_INTERNAL = "OrganizeManifests.ExportedPackages.makeInternal"; //$NON-NLS-1$
	public static final String PROP_INTERAL_PACKAGE_FILTER = "OrganizeManifests.ExportedPackages.packageFilter"; //$NON-NLS-1$
	public static final String VALUE_DEFAULT_FILTER = "*.internal.*"; //$NON-NLS-1$
	public static final String PROP_REMOVE_UNRESOLVED_EX = "OrganizeManifests.ExportedPackages.removeUnresolved"; //$NON-NLS-1$
	public static final String PROP_MODIFY_DEP = "OrganizeManifests.RequireImport.modifyDep"; //$NON-NLS-1$
	public static final String PROP_RESOLVE_IMPORTS = "OrganizeManifests.RequireImport.resolveImports"; //$NON-NLS-1$
	public static final String VALUE_REMOVE_IMPORT = "removeImport"; //$NON-NLS-1$
	public static final String VALUE_IMPORT_OPTIONAL = "importOptional"; //$NON-NLS-1$
	public static final String PROP_UNUSED_DEPENDENCIES = "OrganizeManifests.RequireImport.findRemoveUnused"; //$NON-NLS-1$
	public static final String PROP_REMOVE_LAZY = "OrganizeManifests.General.cleanup"; //$NON-NLS-1$
	public static final String PROP_NLS_PATH = "OrganizeManifests.Translation.nls"; //$NON-NLS-1$
	public static final String PROP_UNUSED_KEYS = "OrganizeManifests.Translation.unusedKeys"; //$NON-NLS-1$
}