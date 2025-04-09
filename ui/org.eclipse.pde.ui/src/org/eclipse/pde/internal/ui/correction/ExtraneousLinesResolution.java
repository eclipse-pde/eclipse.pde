/*******************************************************************************
 *  Copyright (c) 2023 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Gireesh Punathil <gpunathi@in.ibm.com> Initial implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.correction;

import java.util.StringTokenizer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.*;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;

/**
 * <p>Represents a resolution to the problem of the bundle manifest
 * having extraneous lines.</p>
 */
public class ExtraneousLinesResolution extends AbstractManifestMarkerResolution {
	/**
	 * Creates a new resolution
	 *
	 * @param type
	 *            {@link AbstractPDEMarkerResolution#REMOVE_TYPE} to delete
	 *            lines
	 */
	public ExtraneousLinesResolution(int type, IMarker marker) {
		super(type, marker);
	}

	/**
	 * Resolves the problem by extracting the empty line and removing it.
	 */
	@Override
	protected void createChange(BundleModel model) {
		IDocument doc = model.getDocument();
		int line = 0;
		try {
			String emptyLines = (String) marker.getAttribute(PDEMarkerFactory.EMPTY_LINE);
			StringTokenizer st = new StringTokenizer(emptyLines, ","); //$NON-NLS-1$
			while (st.hasMoreElements()) {
				line = Integer.parseInt(st.nextToken());
				IRegion l = doc.getLineInformation(line);
				doc.replace(l.getOffset(), l.getLength() + 1, ""); //$NON-NLS-1$
			}
		} catch (BadLocationException | CoreException | NumberFormatException e) {
			PDEPlugin.log(e);
		}
	}

	@Override
	public String getDescription() {
		return PDEUIMessages.ExtraneousLineResolutionRemove_description;
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.ExtraneousLineResolutionRemove_label;
	}

}
