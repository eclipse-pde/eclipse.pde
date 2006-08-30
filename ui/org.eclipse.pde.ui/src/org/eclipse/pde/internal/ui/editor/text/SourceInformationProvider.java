/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;


import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
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
	
	public void partOpened(IWorkbenchPart part) {
	}

	public void partDeactivated(IWorkbenchPart part) {
	}

	public void partClosed(IWorkbenchPart part) {
		if (fSourcePage != null && part == fSourcePage.getEditor() && fImpType != F_NO_IMP)
			fSourcePage.getSite().getWorkbenchWindow().getPartService().removePartListener(this);
	}

	public void partActivated(IWorkbenchPart part) {
		update();
	}

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
		IWorkbenchPage page = fSourcePage.getSite().getWorkbenchWindow().getActivePage();
		if (page != null) {
			IPerspectiveDescriptor perspective = page.getPerspective();
			if (perspective != null)  {
				String perspectiveId = perspective.getId();
				if (fCurrentPerspective == null || fCurrentPerspective != perspectiveId) {
					fCurrentPerspective = perspectiveId;
					switch (fImpType) {
					case F_MANIFEST_IMP:
						fImplementation = new ManifestTextHover(fSourcePage);
						break;
					case F_XML_IMP:
						fImplementation = new PluginXMLTextHover(fSourcePage);
						break;
					}
				}
			}
		}
	}

	public IRegion getSubject(ITextViewer textViewer, int offset) {
		if (textViewer != null)
			return findWord(textViewer.getDocument(), offset);
		return null;
	}

	public String getInformation(ITextViewer textViewer, IRegion subject) {
		if (fImplementation != null) {
			String s = fImplementation.getHoverInfo(textViewer, subject);
			if (s != null && s.trim().length() > 0)
				return s;
		}
		return null;
	}

	public IInformationControlCreator getInformationPresenterControlCreator() {
		return fPresenterControlCreator;
	}
	
	/*
	 * from org.eclipse.jdt.internal.ui.text.JavaWordFinder
	 */
	public IRegion findWord(IDocument document, int offset) {
		int start = -2;
		int end = -1;
		
		try {
			int pos = offset;
			char c;
			while (pos >= 0) {
				c = document.getChar(pos);
				if (!Character.isJavaIdentifierPart(c))
					break;
				--pos;
			}
			start = pos;

			pos = offset;
			int length = document.getLength();

			while (pos < length) {
				c = document.getChar(pos);
				if (!Character.isJavaIdentifierPart(c))
					break;
				++pos;
			}
			end = pos;

		} catch (BadLocationException x) {
		}

		if (start >= -1 && end > -1) {
			if (start == offset && end == offset)
				return new Region(offset, 0);
			else if (start == offset)
				return new Region(start, end - start);
			else
				return new Region(start + 1, end - start - 1);
		}
		return null;
	}
}
