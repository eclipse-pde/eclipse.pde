/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
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
 *     Mohammed Mostafa <mmostafa@ca.ibm.com> - bug 296522
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import java.util.*;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.internal.text.html.HTMLTextPresenter;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.*;
import org.eclipse.jface.text.quickassist.*;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.correction.AbstractPDEMarkerResolution;
import org.eclipse.pde.internal.ui.correction.ResolutionGenerator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;

public class PDEQuickAssistAssistant extends QuickAssistAssistant {

	private Image fCreateImage;
	private Image fRenameImage;
	private Image fRemoveImage;

	class PDECompletionProposal implements ICompletionProposal, ICompletionProposalExtension3, ICompletionProposalExtension4, Comparable<Object> {

		Position fPosition;
		IMarkerResolution fResolution;
		IMarker fMarker;

		public PDECompletionProposal(IMarkerResolution resolution, Position pos, IMarker marker) {
			fPosition = pos;
			fResolution = resolution;
			fMarker = marker;
		}

		public IMarkerResolution getResolution() {
			return fResolution;
		}
		@Override
		public void apply(IDocument document) {
			fResolution.run(fMarker);
		}

		@Override
		public Point getSelection(IDocument document) {
			return new Point(fPosition.offset, 0);
		}

		@Override
		public String getAdditionalProposalInfo() {
			if (fResolution instanceof IMarkerResolution2) {
				return ((IMarkerResolution2) fResolution).getDescription();
			}
			return null;
		}

		@Override
		public String getDisplayString() {
			return fResolution.getLabel();
		}

		@Override
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

		@Override
		public IContextInformation getContextInformation() {
			return null;
		}

		@Override
		public IInformationControlCreator getInformationControlCreator() {
			return null;
		}

		@Override
		public int getPrefixCompletionStart(IDocument document, int completionOffset) {
			return 0;
		}

		@Override
		public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
			return null;
		}

		@Override
		public boolean isAutoInsertable() {
			return true;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof PDECompletionProposal)) {
				return false;
			}
			PDECompletionProposal proposal = (PDECompletionProposal) obj;
			return proposal.fPosition.equals(fPosition) && proposal.fResolution.equals(fResolution);
		}

		@Override
		public int hashCode() {
			return fPosition.hashCode() + fResolution.hashCode();
		}

		@Override
		public int compareTo(Object arg0) {
			if (!(arg0 instanceof PDECompletionProposal))
				return -1;
			PDECompletionProposal obj = (PDECompletionProposal) arg0;
			if (getDisplayString() != null) {
				if (obj.getDisplayString() != null) {
					return getDisplayString().compareTo(obj.getDisplayString());
				}
				return 0;
			}
			return -1;
		}

	}

	class PDEQuickAssistProcessor implements IQuickAssistProcessor {

		ResolutionGenerator fGenerator = new ResolutionGenerator();
		HashMap<IMarker, IMarkerResolution[]> fResMap = new HashMap<>();

		@Override
		public String getErrorMessage() {
			return null;
		}

		@Override
		public boolean canFix(Annotation annotation) {
			boolean bRetVal = false;
			if (annotation instanceof SimpleMarkerAnnotation) {
				// check local resolutions first
				IMarker marker = ((SimpleMarkerAnnotation) annotation).getMarker();
				IMarkerResolution[] localResolutions = fGenerator.getResolutions(marker);
				if (localResolutions.length > 0) {
					bRetVal = true;
				}
				// check the contributed resolutions if needed
				if (!bRetVal) {
					IMarkerResolution[] contributedResolutions = IDE.getMarkerHelpRegistry().getResolutions(marker);
					if (contributedResolutions.length > 0) {
						bRetVal = true;
					}
				}
			}
			return bRetVal;

		}

		private void populateDataModelForAnnotation(SimpleMarkerAnnotation annotation) {
			// grab the local resolutions first
			IMarker marker = annotation.getMarker();
			if (!fResMap.containsKey(marker)) {
				ArrayList<IMarkerResolution> resolutions = new ArrayList<>(5);
				IMarkerResolution[] localResolutions = fGenerator.getResolutions(marker);
				resolutions.addAll(Arrays.asList(localResolutions));

				// grab the contributed resolutions
				IMarkerResolution[] contributedResolutions = IDE.getMarkerHelpRegistry().getResolutions(marker);
				for (int i = 0; i < contributedResolutions.length; i++) {
					IMarkerResolution resolution = contributedResolutions[i];
					// only add contributed marker resolutions if they don't come from PDE
					if (!(resolution instanceof AbstractPDEMarkerResolution) && !resolutions.contains(contributedResolutions[i]))
						resolutions.add(contributedResolutions[i]);
				}
				if (!resolutions.isEmpty()) {
					fResMap.put(marker, resolutions.toArray(new IMarkerResolution[resolutions.size()]));
				}
			}
		}

		@Override
		public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
			return false;
		}

		@Override
		public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext invocationContext) {
			IAnnotationModel amodel = invocationContext.getSourceViewer().getAnnotationModel();
			IDocument doc = invocationContext.getSourceViewer().getDocument();

			int offset = invocationContext.getOffset();
			Iterator<?> it = amodel.getAnnotationIterator();
			TreeSet<ICompletionProposal> proposalSet = new TreeSet<>((o1, o2) -> {
				if (o1 != null && o2 != null) {
					return o1.getDisplayString().compareToIgnoreCase(o2.getDisplayString());
				}
				return 0;
			});
			while (it.hasNext()) {
				Object key = it.next();
				if (!(key instanceof SimpleMarkerAnnotation)) {
					if (key instanceof SpellingAnnotation) {
						SpellingAnnotation annotation = (SpellingAnnotation) key;
						if (amodel.getPosition(annotation).overlapsWith(offset, 1)) {
							ICompletionProposal[] proposals = annotation.getSpellingProblem().getProposals();
							for (ICompletionProposal proposal : proposals) {
								proposalSet.add(proposal);
							}
						}
					}
					continue;
				}

				SimpleMarkerAnnotation annotation = (SimpleMarkerAnnotation) key;
				populateDataModelForAnnotation(annotation);
				IMarker marker = annotation.getMarker();

				IMarkerResolution[] mapping = fResMap.get(marker);
				if (mapping != null) {
					Position pos = amodel.getPosition(annotation);
					try {
						int line = doc.getLineOfOffset(pos.getOffset());
						int start = pos.getOffset();
						String delim = doc.getLineDelimiter(line);
						int delimLength = delim != null ? delim.length() : 0;
						int end = doc.getLineLength(line) + start - delimLength;
						if (offset >= start && offset <= end) {
							for (IMarkerResolution markerResolution : mapping) {
								PDECompletionProposal proposal = new PDECompletionProposal(markerResolution, pos, marker);
								if (!proposalSet.contains(proposal)) {
									proposalSet.add(proposal);
								}
							}
						}
					} catch (BadLocationException e) {
					}

				}
			}

			ArrayList<ICompletionProposal> arrayListCompProNegativeRelevance = new ArrayList<>();
			for (ICompletionProposal prop : proposalSet) {
				if (prop instanceof PDECompletionProposal) {
					IMarkerResolution res = ((PDECompletionProposal) prop).getResolution();
					if (res instanceof IJavaCompletionProposal) {
						int rel = ((IJavaCompletionProposal) res).getRelevance();
						if (rel < 0) {
							arrayListCompProNegativeRelevance.add(prop);
						}
					}
				}
			}
			proposalSet.removeAll(arrayListCompProNegativeRelevance);
			ArrayList<ICompletionProposal> finalProposal = new ArrayList<>();
			finalProposal.addAll(proposalSet);
			finalProposal.addAll(arrayListCompProNegativeRelevance);
			return finalProposal.toArray(new ICompletionProposal[] {});
		}
	}

	public PDEQuickAssistAssistant() {
		setQuickAssistProcessor(new PDEQuickAssistProcessor());
		fCreateImage = PDEPluginImages.DESC_ADD_ATT.createImage();
		fRemoveImage = PDEPluginImages.DESC_DELETE.createImage();
		fRenameImage = PDEPluginImages.DESC_REFRESH.createImage();
		setInformationControlCreator(new AbstractReusableInformationControlCreator() {
			@Override
			public IInformationControl doCreateInformationControl(Shell parent) {
				return new DefaultInformationControl(parent, new HTMLTextPresenter(true));
			}
		});
	}

	public void dispose() {
		fCreateImage.dispose();
		fRemoveImage.dispose();
		fRenameImage.dispose();
	}
}
