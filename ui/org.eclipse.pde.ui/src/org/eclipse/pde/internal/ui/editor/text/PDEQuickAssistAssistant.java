/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import java.util.*;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.*;
import org.eclipse.jface.text.quickassist.*;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.correction.AbstractPDEMarkerResolution;
import org.eclipse.pde.internal.ui.correction.ResolutionGenerator;
import org.eclipse.pde.internal.ui.editor.contentassist.display.BrowserInformationControl;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;

public class PDEQuickAssistAssistant extends QuickAssistAssistant {

	private Image fCreateImage;
	private Image fRenameImage;
	private Image fRemoveImage;

	private IInformationControlCreator fCreator;

	class PDECompletionProposal implements ICompletionProposal, ICompletionProposalExtension3 {

		Position fPosition;
		IMarkerResolution fResolution;
		IMarker fMarker;

		public PDECompletionProposal(IMarkerResolution resolution, Position pos, IMarker marker) {
			fPosition = pos;
			fResolution = resolution;
			fMarker = marker;
		}

		public void apply(IDocument document) {
			fResolution.run(fMarker);
		}

		public Point getSelection(IDocument document) {
			return new Point(fPosition.offset, 0);
		}

		public String getAdditionalProposalInfo() {
			if (fResolution instanceof IMarkerResolution2)
				return ((IMarkerResolution2) fResolution).getDescription();
			return null;
		}

		public String getDisplayString() {
			return fResolution.getLabel();
		}

		public Image getImage() {
			if (fResolution instanceof AbstractPDEMarkerResolution) {
				switch (((AbstractPDEMarkerResolution) fResolution).getType()) {
					case AbstractPDEMarkerResolution.CREATE_TYPE :
						return fCreateImage;
					case AbstractPDEMarkerResolution.REMOVE_TYPE :
						return fRemoveImage;
					case AbstractPDEMarkerResolution.RENAME_TYPE :
						return fRenameImage;
				}
			}
			if (fResolution instanceof IMarkerResolution2) {
				IMarkerResolution2 resolution = (IMarkerResolution2) fResolution;
				return resolution.getImage();
			}
			return null;
		}

		public IContextInformation getContextInformation() {
			return null;
		}

		public IInformationControlCreator getInformationControlCreator() {
			// if browser isn't available, use the default information control
			if (!BrowserInformationControl.isAvailable(null))
				fCreator = new AbstractReusableInformationControlCreator() {
					public IInformationControl doCreateInformationControl(Shell parent) {
						return new DefaultInformationControl(parent, EditorsUI.getTooltipAffordanceString());
					}
				};

			// if the browser is available, let's use the browser one
			if (fCreator == null) {
				fCreator = new AbstractReusableInformationControlCreator() {
					public IInformationControl doCreateInformationControl(Shell parent) {
						return new BrowserInformationControl(parent, SWT.NO_TRIM | SWT.TOOL, SWT.NONE);
					}
				};
			}
			return fCreator;
		}

		public int getPrefixCompletionStart(IDocument document, int completionOffset) {
			return 0;
		}

		public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
			return null;
		}

	}

	class PDEQuickAssistProcessor implements IQuickAssistProcessor {

		ResolutionGenerator fGenerator = new ResolutionGenerator();
		HashMap fResMap = new HashMap();

		public String getErrorMessage() {
			return null;
		}

		public boolean canFix(Annotation annotation) {
			if (!(annotation instanceof SimpleMarkerAnnotation))
				return false;

			ArrayList resolutions = new ArrayList(5);

			// grab the local resolutions first
			IMarker marker = ((SimpleMarkerAnnotation) annotation).getMarker();
			IMarkerResolution[] localResolutions = fGenerator.getResolutions(marker);
			resolutions.addAll(Arrays.asList(localResolutions));

			// grab the contributed resolutions
			IMarkerResolution[] contributedResolutions = IDE.getMarkerHelpRegistry().getResolutions(marker);
			for (int i = 0; i < contributedResolutions.length; i++) {
				IMarkerResolution resolution = contributedResolutions[i];
				// only add contributed marker resolutions if they don't come from PDE
				if (!(resolution instanceof AbstractPDEMarkerResolution))
					resolutions.add(contributedResolutions[i]);
			}

			boolean canFix = resolutions.size() > 0;
			if (canFix)
				if (!fResMap.containsKey(marker))
					fResMap.put(marker, resolutions.toArray(new IMarkerResolution[resolutions.size()]));

			return canFix;
		}

		public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
			return false;
		}

		public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext invocationContext) {
			IAnnotationModel amodel = invocationContext.getSourceViewer().getAnnotationModel();
			IDocument doc = invocationContext.getSourceViewer().getDocument();

			int offset = invocationContext.getOffset();
			Iterator it = amodel.getAnnotationIterator();
			ArrayList list = new ArrayList();
			while (it.hasNext()) {
				Object key = it.next();
				if (!(key instanceof SimpleMarkerAnnotation))
					continue;

				SimpleMarkerAnnotation annotation = (SimpleMarkerAnnotation) key;
				IMarker marker = annotation.getMarker();

				IMarkerResolution[] mapping = (IMarkerResolution[]) fResMap.get(marker);
				if (mapping != null) {
					Position pos = amodel.getPosition(annotation);
					try {
						int line = doc.getLineOfOffset(pos.getOffset());
						int start = pos.getOffset();
						String delim = doc.getLineDelimiter(line);
						int delimLength = delim != null ? delim.length() : 0;
						int end = doc.getLineLength(line) + start - delimLength;
						if (offset >= start && offset <= end) {
							for (int i = 0; i < mapping.length; i++) {
								list.add(new PDECompletionProposal(mapping[i], pos, marker));
							}
						}
					} catch (BadLocationException e) {
					}

				}
			}
			return (ICompletionProposal[]) list.toArray(new ICompletionProposal[list.size()]);
		}
	}

	public PDEQuickAssistAssistant() {
		setQuickAssistProcessor(new PDEQuickAssistProcessor());
		fCreateImage = PDEPluginImages.DESC_ADD_ATT.createImage();
		fRemoveImage = PDEPluginImages.DESC_DELETE.createImage();
		fRenameImage = PDEPluginImages.DESC_REFRESH.createImage();
	}

	public void dispose() {
		fCreateImage.dispose();
		fRemoveImage.dispose();
		fRenameImage.dispose();
	}
}
