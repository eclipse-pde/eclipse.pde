/*******************************************************************************
 *  Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     Christoph Läubrich - Bug 576610 - FeatureEditor should support display of non-file-based feature models
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.IInformationProviderExtension2;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;

public class SourceInformationProvider implements IInformationProvider, IInformationProviderExtension2, IPartListener {

	public static final int F_NO_IMP = 0;
	public static final int F_XML_IMP = 1;
	public static final int F_MANIFEST_IMP = 2;

	protected PDESourcePage fSourcePage;
	protected String fCurrentPerspective;
	protected ITextHover fImplementation;
	protected int fImpType;

	@Override
	public void partOpened(IWorkbenchPart part) {
	}

	@Override
	public void partDeactivated(IWorkbenchPart part) {
	}

	@Override
	public void partClosed(IWorkbenchPart part) {
		if (fSourcePage != null && part == fSourcePage.getEditor() && fImpType != F_NO_IMP)
			fSourcePage.getSite().getWorkbenchWindow().getPartService().removePartListener(this);
	}

	@Override
	public void partActivated(IWorkbenchPart part) {
		update();
	}

	@Override
	public void partBroughtToTop(IWorkbenchPart part) {
		update();
	}

	private IInformationControlCreator fPresenterControlCreator;

	public SourceInformationProvider(PDESourcePage editor, IInformationControlCreator creator, int impType) {
		fSourcePage = editor;
		fPresenterControlCreator = creator;
		fImpType = impType;
		if (fSourcePage != null && fImpType != F_NO_IMP) {
			fSourcePage.getSite().getWorkbenchWindow().getPartService().addPartListener(this);
			update();
		}
	}

	protected void update() {
		if (fSourcePage == null) {
			return;
		}
		IWorkbenchPage page = fSourcePage.getSite().getWorkbenchWindow().getActivePage();
		if (page != null) {
			IPerspectiveDescriptor perspective = page.getPerspective();
			if (perspective != null) {
				String perspectiveId = perspective.getId();
				if (fCurrentPerspective == null || fCurrentPerspective != perspectiveId) {
					fCurrentPerspective = perspectiveId;
					fImplementation = switch (fImpType) {
						case F_MANIFEST_IMP -> new ManifestTextHover(fSourcePage);
						case F_XML_IMP -> new PluginXMLTextHover(fSourcePage);
						default -> null;
						};
				}
			}
		}
	}

	@Override
	public IRegion getSubject(ITextViewer textViewer, int offset) {
		if (textViewer != null)
			return PDEWordFinder.findWord(textViewer.getDocument(), offset);
		return null;
	}

	@Override
	public String getInformation(ITextViewer textViewer, IRegion subject) {
		if (fImplementation != null) {
			String s = fImplementation.getHoverInfo(textViewer, subject);
			if (s != null && s.trim().length() > 0)
				return s;
		}
		return null;
	}

	@Override
	public IInformationControlCreator getInformationPresenterControlCreator() {
		return fPresenterControlCreator;
	}

}
