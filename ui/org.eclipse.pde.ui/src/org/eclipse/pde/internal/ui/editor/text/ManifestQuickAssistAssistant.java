package org.eclipse.pde.internal.ui.editor.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.correction.AbstractPDEMarkerResolution;
import org.eclipse.pde.internal.ui.correction.ResolutionGenerator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.texteditor.MarkerAnnotation;

public class ManifestQuickAssistAssistant extends QuickAssistAssistant {

	private Image fCreateImage;
	private Image fRenameImage;
	private Image fRemoveImage;
	
	class ManifestCompletionProposal implements ICompletionProposal {

		Position fPosition;
		IMarkerResolution fResolution;
		IMarker fMarker;
		
		public ManifestCompletionProposal(IMarkerResolution resolution, Position pos, IMarker marker) {
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
			// TODO Auto-generated method stub
			return null;
		}

		public String getDisplayString() {
			return fResolution.getLabel();
		}

		public Image getImage() {
			if (fResolution instanceof AbstractPDEMarkerResolution) {
				switch (((AbstractPDEMarkerResolution)fResolution).getType()) {
				case AbstractPDEMarkerResolution.CREATE_TYPE:
					return fCreateImage;
				case AbstractPDEMarkerResolution.REMOVE_TYPE:
					return fRemoveImage;
				case AbstractPDEMarkerResolution.RENAME_TYPE:
					return fRenameImage;
				}
			}
			return null;
		}

		public IContextInformation getContextInformation() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
	
	class ManifestQuickAssistProcessor implements IQuickAssistProcessor {

		ResolutionGenerator fGenerator = new ResolutionGenerator();
		HashMap fResolutionMapping = new HashMap();
		
		public String getErrorMessage() {
			return null;
		}

		public boolean canFix(Annotation annotation) {
			if (!(annotation instanceof MarkerAnnotation))
				return false;
			MarkerAnnotation markerAnnotation = (MarkerAnnotation)annotation;
			IMarkerResolution[] resolutions = fGenerator.getResolutions(markerAnnotation.getMarker());
			boolean canFix = resolutions.length > 0;
			if (canFix)
				if (!fResolutionMapping.containsKey(markerAnnotation))
					fResolutionMapping.put(markerAnnotation, resolutions);
			return canFix;
		}

		public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
			return false;
		}

		public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext invocationContext) {
			IAnnotationModel amodel = invocationContext.getSourceViewer().getAnnotationModel();
			Iterator it = amodel.getAnnotationIterator();
			ArrayList list = new ArrayList();
			while (it.hasNext()) {
				Object key = it.next();
				Object mapping = fResolutionMapping.get(key);
				if (key instanceof MarkerAnnotation) {
					Position pos = amodel.getPosition((Annotation)key);
					if (pos.getOffset() != invocationContext.getOffset())
						continue;
					if (mapping instanceof IMarkerResolution[]) {
						for (int i = 0; i < ((IMarkerResolution[])mapping).length; i++) {
							list.add(new ManifestCompletionProposal(
									((IMarkerResolution[])mapping)[i], pos,
									((MarkerAnnotation)key).getMarker()));
						}
					}
				}
			}
			return (ICompletionProposal[]) list.toArray(new ICompletionProposal[list.size()]);
		}
		
	}

	public ManifestQuickAssistAssistant() {
		setQuickAssistProcessor(new ManifestQuickAssistProcessor());
		fCreateImage = PDEPluginImages.DESC_ADD_ATT.createImage();
		fRemoveImage = PDEPluginImages.DESC_REMOVE_ATT.createImage();
		fRenameImage = PDEPluginImages.DESC_REFRESH.createImage();
	}
	
	public void dispose() {
		fCreateImage.dispose();
		fRemoveImage.dispose();
		fRenameImage.dispose();
	}
}
