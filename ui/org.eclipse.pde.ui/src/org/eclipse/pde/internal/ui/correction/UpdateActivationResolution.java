/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import java.util.Map;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.text.bundle.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class UpdateActivationResolution extends AbstractManifestMarkerResolution {

	private String fHeader = null;

	public UpdateActivationResolution(int type, String currentHeader) {
		super(type);
		fHeader = currentHeader;
	}

	protected void createChange(BundleModel model) {
		if (TargetPlatformHelper.getTargetVersion() >= 3.4) {
			// get the header we wish to replace
			LazyStartHeader header = (LazyStartHeader) model.getBundle().getManifestHeader(fHeader);
			if (header != null) {
				// create a new header and copy over information
				LazyStartHeader newHeader = (LazyStartHeader) model.getFactory().createHeader(Constants.BUNDLE_ACTIVATIONPOLICY, Constants.ACTIVATION_LAZY);
				newHeader.setLazyStart(header.isLazyStart());
				String excludes = header.getAttribute("exceptions"); //$NON-NLS-1$
				// handle 'exceptions' attribute.
				if (excludes != null) {
					// if lazystart = true, then use the exclude directive.
					// if lazystart = false, then use the include directive making sure to set the Bundle-ActivationPolicy header to lazy (or true).
					String directive = (header.isLazyStart()) ? Constants.EXCLUDE_DIRECTIVE : Constants.INCLUDE_DIRECTIVE;
					newHeader.setDirective(directive, excludes);
					if (!header.isLazyStart())
						newHeader.setLazyStart(true);
				}

				// This is a HACK to overwrite the existing header with the new header.  Since newHeader has the same length/offset as the old header, the
				// BundleTextChangeListener will create a ReplaceTextEdit and will keep the same ordering.  NOTE: NOT recommended usage for modifying a header!!
				newHeader.setOffset(header.getOffset());
				newHeader.setLength(header.getLength());
				// remove old header from Bundle object.  Add new header to Bundle object
				Bundle bundle = (Bundle) model.getBundle();
				Map map = bundle.getHeaders();
				map.remove(fHeader);
				map.put(Constants.BUNDLE_ACTIVATIONPOLICY, newHeader);
				// fire ModelChanged so that way the BundleTextChangeListener will make proper ReplaceTextEdits
				model.fireModelObjectChanged(newHeader, Constants.BUNDLE_ACTIVATIONPOLICY, null, header.getValue());
			}
		} else {
			// if we should not use the Bundle-ActivationPolicy header, then we know we are renaming the Eclipse-AutoStart header to Eclipse-LazyStart
			model.getBundle().renameHeader(ICoreConstants.ECLIPSE_AUTOSTART, ICoreConstants.ECLIPSE_LAZYSTART);
		}
	}

	public String getDescription() {
		if (TargetPlatformHelper.getTargetVersion() >= 3.4)
			return PDEUIMessages.UpdateActivationResolution_bundleActivationPolicy_label;
		return PDEUIMessages.UpdateActivationResolution_lazyStart_label;
	}

	public String getLabel() {
		if (TargetPlatformHelper.getTargetVersion() >= 3.4)
			return NLS.bind(PDEUIMessages.UpdateActivationResolution_bundleActivationPolicy_desc, fHeader);
		return PDEUIMessages.UpdateActivationResolution_lazyStart_desc;
	}
}
