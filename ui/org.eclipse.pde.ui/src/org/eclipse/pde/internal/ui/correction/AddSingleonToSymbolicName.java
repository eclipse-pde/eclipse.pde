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

import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.model.bundle.Bundle;
import org.eclipse.pde.internal.ui.model.bundle.BundleModel;
import org.eclipse.pde.internal.ui.model.bundle.ManifestHeader;
import org.osgi.framework.Constants;

public class AddSingleonToSymbolicName extends ManifestHeaderErrorResolution {

	private boolean fisDirective;
	
	public AddSingleonToSymbolicName(int type, boolean directive) {
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
			Bundle bun = (Bundle)bundle;
			ManifestHeader header = bun.getManifestHeader(Constants.BUNDLE_SYMBOLICNAME);
			if (header == null)
				return;
			header.setDirective(Constants.SINGLETON_DIRECTIVE, fisDirective ?
					new String[] {Boolean.toString(true)} : null);
			header.setAttribute(ICoreConstants.SINGLETON_ATTRIBUTE, fisDirective ?
					null : new String[] {Boolean.toString(true)});
		}
	}
}