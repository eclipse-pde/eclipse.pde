/*******************************************************************************
 *  Copyright (c) 2005, 2017 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import java.util.Iterator;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.*;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.util.PDEJavaHelperUI;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.MarkerAnnotation;

public class ManifestTextHover extends PDETextHover {

	private PDESourcePage fSourcePage;
	private IJavaProject fJP;

	public ManifestTextHover(PDESourcePage sourcePage) {
		fSourcePage = sourcePage;
		IProject project = ((PDEFormEditor) fSourcePage.getEditor()).getCommonProject();
		fJP = JavaCore.create(project);
	}

	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		String ret = getHoverInfo2(textViewer, hoverRegion);
		if (ret == null) {
			IAnnotationModel annotationModel = fSourcePage.getViewer().getAnnotationModel();
			if (annotationModel instanceof PDEMarkerAnnotationModel) {
				int offset = hoverRegion.getOffset();
				PDEMarkerAnnotationModel pdeAnnotationModel = (PDEMarkerAnnotationModel) annotationModel;
				Iterator<Annotation> annotationIterator = pdeAnnotationModel.getAnnotationIterator(offset, 0, true,
						true);
				while (annotationIterator.hasNext()) {
					Annotation object = annotationIterator.next();
					if (object instanceof MarkerAnnotation) {
						ISourceViewer viewer = fSourcePage.getViewer();
						if (viewer instanceof PDEProjectionViewer) {
							PDEProjectionViewer pviewer = (PDEProjectionViewer) viewer;
							Display display = Display.getCurrent() != null ? Display.getCurrent()
									: Display.getDefault();
							display.asyncExec(new Runnable() {
								@Override
								public void run() {
									fSourcePage.selectAndReveal(offset, 0);
									pviewer.doOperation(ISourceViewer.QUICK_ASSIST);
								}
							});
						}
					}
				}
			}
		}
		return ret;
	}

	private String getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
		int offset = hoverRegion.getOffset();
		IDocumentRange range = fSourcePage.getRangeElement(offset, false);
		if (range instanceof IManifestHeader) {
			IManifestHeader header = (IManifestHeader) range;
			String headerName = header.getName();
			if (offset >= header.getOffset() + headerName.length())
				return checkForTranslatable(header);
			if (fJP != null)
				return PDEJavaHelperUI.getOSGIConstantJavaDoc(headerName, fJP);
		}
		return null;
	}

	private String checkForTranslatable(IManifestHeader header) {
		String name = header.getName();
		String value = header.getValue();
		for (String transalatableHeader : ICoreConstants.TRANSLATABLE_HEADERS) {
			if (name.equals(transalatableHeader) && value.startsWith("%")) { //$NON-NLS-1$
				IBaseModel model = ((PDEFormEditor) fSourcePage.getEditor()).getAggregateModel();
				if (model instanceof IModel)
					return ((IModel) model).getResourceString(value);
			}
		}
		return null;
	}
}
