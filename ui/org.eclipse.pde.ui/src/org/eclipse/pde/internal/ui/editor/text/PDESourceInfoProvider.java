/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
 *     Les Jones <lesojones@gmail.com> - bug 211698
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.IInformationProviderExtension;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;

/**
 * PDESourceInfoProvider
 */
public class PDESourceInfoProvider implements IInformationProvider, IInformationProviderExtension {

	private PDESourcePage fSourcePage;

	public PDESourceInfoProvider(PDESourcePage sourcePage) {
		fSourcePage = sourcePage;
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	@Override
	public String getInformation(ITextViewer textViewer, IRegion subject) {
		// This method is deprecated.  Call the non-deprecated method
		return getInformation2(textViewer, subject).toString();
	}

	@Override
	public IRegion getSubject(ITextViewer textViewer, int offset) {
		// Subject used in getInformation2
		if ((textViewer == null) || (fSourcePage == null)) {
			return null;
		}
		// Get the selected region
		IRegion region = PDEWordFinder.findWord(textViewer.getDocument(), offset);
		// Ensure the region is defined.  Define an empty one if it is not.
		if (region == null) {
			return new Region(offset, 0);
		}
		return region;
	}

	@Override
	public Object getInformation2(ITextViewer textViewer, IRegion subject) {
		// Calls setInput on the quick outline popup dialog
		if ((textViewer == null) || (fSourcePage == null)) {
			return null;
		}

		Object selection;
		selection = fSourcePage.getSelection();

		// If the input is null, then the dialog does not open
		// Define an empty object for no selection instead of null
		if (selection == null) {
			selection = new Object();
		}
		return selection;
		// TODO: MP: QO: LOW: Determine range on MANIFEST.MF to do initial
		// dynamic selection.  Use IRegion.  Already implemented for plugin.xml file
		// see XMLContentAssistProcessor.assignRange()
	}

}
