/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.core.runtime.preferences.*;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Disallows importing of target platform preferences. Target platform should not
 * be modified by preference import.
 * 
 * @since 3.5
 */
public class TargetPreferenceModifyListener extends PreferenceModifyListener {

	class Visitor implements IPreferenceNodeVisitor {

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.preferences.IPreferenceNodeVisitor#visit(org.eclipse.core.runtime.preferences.IEclipsePreferences)
		 */
		public boolean visit(IEclipsePreferences node) throws BackingStoreException {
			if (node.name().equals(PDECore.PLUGIN_ID)) {
				// remove all target platform setting preferences
				node.remove(ICoreConstants.ADDITIONAL_LOCATIONS);
				node.remove(ICoreConstants.ARCH);
				node.remove(ICoreConstants.CHECKED_PLUGINS);
				node.remove(ICoreConstants.CHECKED_VERSION_PLUGINS);
				node.remove(ICoreConstants.IMPLICIT_DEPENDENCIES);
				node.remove(ICoreConstants.NL);
				node.remove(ICoreConstants.PLATFORM_PATH);
				node.remove(ICoreConstants.POOLED_BUNDLES);
				node.remove(ICoreConstants.POOLED_URLS);
				node.remove(ICoreConstants.PROGRAM_ARGS);
				node.remove(ICoreConstants.OS);
				for (int i = 0; i < 4; i++) {
					StringBuffer key = new StringBuffer();
					key.append(ICoreConstants.SAVED_PLATFORM);
					key.append(i);
					node.remove(key.toString());
				}
				node.remove(ICoreConstants.TARGET_MODE);
				node.remove(ICoreConstants.TARGET_PLATFORM_REALIZATION);
				node.remove(ICoreConstants.TARGET_PROFILE);
				node.remove(ICoreConstants.VM_ARGS);
				node.remove(ICoreConstants.WORKSPACE_TARGET_HANDLE);
				node.remove(ICoreConstants.WS);
				return false;
			}
			return true;
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.PreferenceModifyListener#preApply(org.eclipse.core.runtime.preferences.IEclipsePreferences)
	 */
	public IEclipsePreferences preApply(IEclipsePreferences node) {
		try {
			node.accept(new Visitor());
		} catch (BackingStoreException e) {
			PDECore.log(e);
		}
		return node;
	}
}
