/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.pde.internal.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

public class ResolutionGenerator implements IMarkerResolutionGenerator2 {
	
	private static IMarkerResolution[] NO_RESOLUTIONS = new IMarkerResolution[0];

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolutionGenerator#getResolutions(org.eclipse.core.resources.IMarker)
	 */
	public IMarkerResolution[] getResolutions(IMarker marker) {
		int problemID = marker.getAttribute("id", PDEMarkerFactory.NO_RESOLUTION); //$NON-NLS-1$
		switch (problemID) {
			case PDEMarkerFactory.DEPRECATED_AUTOSTART:
				return new IMarkerResolution[] {new RenameAutostartResolution(AbstractPDEMarkerResolution.RENAME_TYPE)};
			case PDEMarkerFactory.JAVA_PACKAGE__PORTED:
				return new IMarkerResolution[] {new CreateJREBundleHeaderResolution(AbstractPDEMarkerResolution.CREATE_TYPE)};
			case PDEMarkerFactory.SINGLETON_DIR_NOT_SET:
				return new IMarkerResolution[] {new AddSingletonToSymbolicName(AbstractPDEMarkerResolution.RENAME_TYPE, true)};
			case PDEMarkerFactory.SINGLETON_ATT_NOT_SET:
				return new IMarkerResolution[] {new AddSingletonToSymbolicName(AbstractPDEMarkerResolution.RENAME_TYPE, false)};
			case PDEMarkerFactory.PROJECT_BUILD_ORDER_ENTRIES:
				return new IMarkerResolution[] {new RemoveStaticProjectReferences(AbstractPDEMarkerResolution.REMOVE_TYPE)};
			case PDEMarkerFactory.EXPORT_PKG_NOT_EXIST:
				return getUnresolvedExportProposals(marker);
			case PDEMarkerFactory.IMPORT_PKG_NOT_AVAILABLE:
				return getUnresolvedImportPackageProposals(marker);
			case PDEMarkerFactory.REQ_BUNDLE_NOT_AVAILABLE:
				return getUnresolvedBundle(marker);
		}
		return NO_RESOLUTIONS;
	}
	
	private IMarkerResolution[] getUnresolvedExportProposals(IMarker marker) {
		String packageName = marker.getAttribute("packageName", (String)null); //$NON-NLS-1$
		if (packageName != null) {
			IResource res = marker.getResource();
			if (res != null)
				return new IMarkerResolution[] {
						new RemoveExportPackageResolution(AbstractPDEMarkerResolution.REMOVE_TYPE, packageName),
						new OrganizeExportPackageResolution(AbstractPDEMarkerResolution.RENAME_TYPE, res.getProject())};
		}
		return NO_RESOLUTIONS;
	}
	
	private IMarkerResolution[] getUnresolvedImportPackageProposals(IMarker marker) {
		String packageName = marker.getAttribute("packageName", (String)null); //$NON-NLS-1$
		if (packageName == null)
			return NO_RESOLUTIONS;
		
		boolean optionalPkg = marker.getAttribute("optional", false); //$NON-NLS-1$
		if (optionalPkg) 
			return new IMarkerResolution[] {new RemoveImportPackageResolution(AbstractPDEMarkerResolution.REMOVE_TYPE, packageName)};

		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		boolean removeImports = store.getString(IPreferenceConstants.PROP_RESOLVE_IMPORTS).equals(IPreferenceConstants.VALUE_REMOVE_IMPORT);
		return new IMarkerResolution[] {
				new RemoveImportPackageResolution(AbstractPDEMarkerResolution.REMOVE_TYPE, packageName),
				new OptionalImportPackageResolution(AbstractPDEMarkerResolution.RENAME_TYPE, packageName),
				new OrganizeImportPackageResolution(AbstractPDEMarkerResolution.RENAME_TYPE, removeImports)};		
	}
	
	private IMarkerResolution[] getUnresolvedBundle(IMarker marker) {
		String bundleId = marker.getAttribute("bundleId", (String)null); //$NON-NLS-1$
		if (bundleId == null)
			return NO_RESOLUTIONS;
		
		boolean optionalBundle = marker.getAttribute("optional", false); //$NON-NLS-1$
		if (optionalBundle) 
			return new IMarkerResolution[] {new RemoveRequireBundleResolution(AbstractPDEMarkerResolution.REMOVE_TYPE, bundleId)};

		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		boolean removeImports = store.getString(IPreferenceConstants.PROP_RESOLVE_IMPORTS).equals(IPreferenceConstants.VALUE_REMOVE_IMPORT);
		return new IMarkerResolution[] {
				new RemoveRequireBundleResolution(AbstractPDEMarkerResolution.REMOVE_TYPE, bundleId),
				new OptionalRequireBundleResolution(AbstractPDEMarkerResolution.RENAME_TYPE, bundleId),
				new OrganizeRequireBundleResolution(AbstractPDEMarkerResolution.RENAME_TYPE, removeImports)};		
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolutionGenerator2#hasResolutions(org.eclipse.core.resources.IMarker)
	 */
	public boolean hasResolutions(IMarker marker) {
		return marker.getAttribute("id", PDEMarkerFactory.NO_RESOLUTION) > 0; //$NON-NLS-1$
	}

}
