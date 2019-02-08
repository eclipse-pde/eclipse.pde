/*******************************************************************************
 * Copyright (c) 2008, 2019 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.LazyStartHeader;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class AddActivationHeaderResolution extends AbstractManifestMarkerResolution {

	String fHeader = null;

	public AddActivationHeaderResolution(int type, String currentHeader, IMarker marker) {
		super(type, marker);
		fHeader = currentHeader;
	}

	@Override
	protected void createChange(BundleModel model) {
		marker.getAttribute("header", ICoreConstants.ECLIPSE_AUTOSTART); //$NON-NLS-1$
		IBundle bundle = model.getBundle();
		IManifestHeader header = bundle.getManifestHeader(fHeader);
		if (header instanceof LazyStartHeader) {
			LazyStartHeader lheader = (LazyStartHeader) header;
			String exceptions = lheader.getAttribute("exceptions"); //$NON-NLS-1$
			if (TargetPlatformHelper.getTargetVersion() >= 3.4) {
				bundle.setHeader(Constants.BUNDLE_ACTIVATIONPOLICY, Constants.ACTIVATION_LAZY);
				if (exceptions != null) {
					LazyStartHeader newHeader = (LazyStartHeader) bundle.getManifestHeader(Constants.BUNDLE_ACTIVATIONPOLICY);
					// if lazystart = true, then use the exclude directive.
					// if lazystart = false, then use the include directive making sure to set the Bundle-ActivationPolicy header to lazy (or true).
					String directive = (lheader.isLazyStart()) ? Constants.EXCLUDE_DIRECTIVE : Constants.INCLUDE_DIRECTIVE;
					newHeader.setDirective(directive, exceptions);
				}
			} else {
				bundle.setHeader(ICoreConstants.ECLIPSE_LAZYSTART, Boolean.toString(lheader.isLazyStart()));
				if (exceptions != null) {
					lheader = (LazyStartHeader) bundle.getManifestHeader(ICoreConstants.ECLIPSE_LAZYSTART);
					lheader.setAttribute("exceptions", exceptions); //$NON-NLS-1$
				}
			}
		}

	}

	@Override
	public String getLabel() {
		String header = (TargetPlatformHelper.getTargetVersion() >= 3.4) ? Constants.BUNDLE_ACTIVATIONPOLICY : ICoreConstants.ECLIPSE_LAZYSTART;
		return NLS.bind(PDEUIMessages.AddActivationHeaderResolution_label, header);
	}

}
