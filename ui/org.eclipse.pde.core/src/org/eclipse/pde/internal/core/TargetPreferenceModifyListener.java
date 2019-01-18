/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferenceNodeVisitor;
import org.eclipse.core.runtime.preferences.PreferenceModifyListener;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Disallows importing of target platform preferences. Target platform should not
 * be modified by preference import.
 *
 * @since 3.5
 */
public class TargetPreferenceModifyListener extends PreferenceModifyListener {

	class Visitor implements IPreferenceNodeVisitor {

		@Override
		public boolean visit(IEclipsePreferences node) throws BackingStoreException {
			if (node.name().equals(PDECore.PLUGIN_ID)) {
				// Importing the preferences should not change the current target platform
				node.remove(ICoreConstants.WORKSPACE_TARGET_HANDLE);
				return false;
			}
			return true;
		}

	}

	@Override
	public IEclipsePreferences preApply(IEclipsePreferences node) {
		try {
			node.accept(new Visitor());
		} catch (BackingStoreException e) {
			PDECore.log(e);
		}
		return node;
	}
}
