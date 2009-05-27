/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class AddSingletonToSymbolicName extends AbstractManifestMarkerResolution {

	private boolean fisDirective;

	public AddSingletonToSymbolicName(int type, boolean directive) {
		super(type);
		fisDirective = directive;
	}

	public String getDescription() {
		if (fisDirective)
			return PDEUIMessages.AddSingleon_dir_desc;
		return PDEUIMessages.AddSingleon_att_desc;
	}

	public String getLabel() {
		if (fisDirective)
			return PDEUIMessages.AddSingleon_dir_label;
		return PDEUIMessages.AddSingleon_att_label;
	}

	protected void createChange(BundleModel model) {
		IBundle bundle = model.getBundle();
		if (bundle instanceof Bundle) {
			Bundle bun = (Bundle) bundle;
			IManifestHeader header = bun.getManifestHeader(Constants.BUNDLE_SYMBOLICNAME);
			if (header instanceof BundleSymbolicNameHeader) {
				if (fisDirective && TargetPlatformHelper.getTargetVersion() >= 3.1)
					bundle.setHeader(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
				else if (!fisDirective && TargetPlatformHelper.getTargetVersion() < 3.1)
					bundle.setHeader(Constants.BUNDLE_MANIFESTVERSION, null);
				((BundleSymbolicNameHeader) header).setSingleton(true);
			}
		}
	}
}
