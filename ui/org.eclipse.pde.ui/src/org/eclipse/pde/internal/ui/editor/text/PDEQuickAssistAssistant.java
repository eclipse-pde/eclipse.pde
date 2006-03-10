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

public class PDEQuickAssistAssistant extends QuickAssistAssistant {

	private Image fCreateImage;
	private Image fRenameImage;
	private Image fRemoveImage;
	
	class PDECompletionProposal implements ICompletionProposal {

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
			if (fResolution instanceof AbstractPDEMarkerResolution)
				return ((AbstractPDEMarkerResolution)fResolution).getDescription();
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
	
	class PDEQuickAssistProcessor implements IQuickAssistProcessor {

		ResolutionGenerator fGenerator = new ResolutionGenerator();
		HashMap fResMap = new HashMap();
		
		public String getErrorMessage() {
			return null;
		}

		public boolean canFix(Annotation annotation) {
			if (!(annotation instanceof MarkerAnnotation))
				return false;
			IMarker marker = ((MarkerAnnotation)annotation).getMarker();
			IMarkerResolution[] resolutions = fGenerator.getResolutions(marker);
			boolean canFix = resolutions.length > 0;
			if (canFix)
				if (!fResMap.containsKey(marker))
					fResMap.put(marker, resolutions);
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
				if (!(key instanceof MarkerAnnotation))
					continue;
				
				MarkerAnnotation annotation = (MarkerAnnotation)key;
				IMarker marker = annotation.getMarker();
				IMarkerResolution[] mapping = (IMarkerResolution[])fResMap.get(marker);
				if (mapping != null) {
					Position pos = amodel.getPosition(annotation);
					if (pos.getOffset() != invocationContext.getOffset())
						continue;
					for (int i = 0; i < mapping.length; i++)
						list.add(new PDECompletionProposal(mapping[i], pos, marker));
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
