/*******************************************************************************
 *  Copyright (c) 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.jface.text.*;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;

/**
 * <p>Represents a resolution to the problem of the bundle manifest not ending
 * with a line termination character.</p>
 */
public class NoLineTerminationResolution extends AbstractManifestMarkerResolution {

	/**
	 * Creates a new resolution
	 * @param type Either {@link AbstractPDEMarkerResolution#REMOVE_TYPE} to delete a whitespace only line 
	 * or {@link AbstractPDEMarkerResolution#CREATE_TYPE} to add a new line
	 */
	public NoLineTerminationResolution(int type) {
		super(type);
	}

	/**
	 * <p>Resolves the problem using one of two methods, depending on the
	 * content of manifest.</p>
	 * 
	 * <ul>
	 *   <li>If the final line of the manifest contains only whitespace, delete
	 *   all the whitespace.</li>
	 *   <li>If the final line of the manifest contains actual content, add a
	 *   new line.</li>
	 * </ul>
	 * 
	 * <p>The post-condition will be that the manifest ends with a line delimiter.</p>
	 */
	protected void createChange(BundleModel model) {
		if (getType() == AbstractPDEMarkerResolution.REMOVE_TYPE) {
			// indicates last line is purely whitespace; we need to delete the whitespace.
			IDocument doc = model.getDocument();
			try {
				IRegion lastLine = doc.getLineInformation(doc.getNumberOfLines() - 1);
				doc.replace(lastLine.getOffset(), lastLine.getLength(), ""); //$NON-NLS-1$
			} catch (BadLocationException e) {
			}
		} else if (getType() == AbstractPDEMarkerResolution.CREATE_TYPE) {
			// indicates last line is true content; we need to add a new line.
			IDocument doc = model.getDocument();
			try {
				String lineDelimiter = doc.getLineDelimiter(0);
				if (lineDelimiter == null) {
					lineDelimiter = ""; //$NON-NLS-1$
				}
				doc.replace(doc.getLength(), 0, lineDelimiter);
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}
	}

	public String getDescription() {
		if (getType() == AbstractPDEMarkerResolution.REMOVE_TYPE) {
			return PDEUIMessages.NoLineTerminationResolutionRemove_description;
		}
		return PDEUIMessages.NoLineTerminationResolutionCreate_description;
	}

	public String getLabel() {
		if (getType() == AbstractPDEMarkerResolution.REMOVE_TYPE) {
			return PDEUIMessages.NoLineTerminationResolutionRemove_label;
		}
		return PDEUIMessages.NoLineTerminationResolutionCreate_label;
	}

}
