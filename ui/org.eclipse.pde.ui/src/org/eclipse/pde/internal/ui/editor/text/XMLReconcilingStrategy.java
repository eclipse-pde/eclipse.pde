/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.pde.internal.ui.editor.context.XMLDocumentSetupParticpant;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.spelling.*;

/**
 * Reconcile strategy used for spell checking XML documents.
 *
 */
public class XMLReconcilingStrategy extends SpellingReconcileStrategy {

	/**
	 * Spelling problem collector.
	 */
	private class SpellingProblemCollector implements ISpellingProblemCollector {

		/** Annotation model. */
		private IAnnotationModel fAnnotationModel;

		/** Annotations to add. */
		private Map<SpellingAnnotation, Position> fAddAnnotations;

		/** Lock object for modifying the annotations. */
		private Object fLockObject;

		/**
		 * Initializes this collector with the given annotation model.
		 *
		 * @param annotationModel the annotation model
		 */
		public SpellingProblemCollector(IAnnotationModel annotationModel) {
			Assert.isLegal(annotationModel != null);
			fAnnotationModel = annotationModel;
			if (fAnnotationModel instanceof ISynchronizable)
				fLockObject = ((ISynchronizable) fAnnotationModel).getLockObject();
			else
				fLockObject = fAnnotationModel;
		}

		@Override
		public void accept(SpellingProblem problem) {
			fAddAnnotations.put(new SpellingAnnotation(problem), new Position(problem.getOffset(), problem.getLength()));
		}

		@Override
		public void beginCollecting() {
			fAddAnnotations = new HashMap<>();
		}

		@Override
		public void endCollecting() {
			synchronized (fLockObject) {
				for (Entry<SpellingAnnotation, Position> entry : fAddAnnotations.entrySet()) {
					fAnnotationModel.addAnnotation(entry.getKey(), entry.getValue());
				}
				deleteNonstringSpellingAnnotations(fAddAnnotations.keySet().iterator());
			}

			fAddAnnotations = null;
		}
	}

	public XMLReconcilingStrategy(ISourceViewer viewer) {
		super(viewer, EditorsUI.getSpellingService());
	}

	@Override
	public void reconcile(IRegion region) {

		deleteAllAnnotations(region);
		super.reconcile(region);

	}

	/**
	 * Deletes all the spelling annotations located inside the area marked by <code>region</code>
	 */
	private void deleteAllAnnotations(IRegion region) {
		IAnnotationModel model = getAnnotationModel();
		if (model == null)
			return;
		Iterator<?> iter = model.getAnnotationIterator();

		while (iter.hasNext()) {
			Annotation annotation = (Annotation) iter.next();
			if (annotation instanceof SpellingAnnotation) {
				SpellingAnnotation spellingAnnotation = (SpellingAnnotation) annotation;
				Position position = model.getPosition(spellingAnnotation);
				if (position.overlapsWith(region.getOffset(), region.getLength())) {
					model.removeAnnotation(spellingAnnotation);
				}
			}
		}
	}

	/**
	 * Deletes the spelling annotations marked for XML Tags
	 */
	private void deleteNonstringSpellingAnnotations(Iterator<SpellingAnnotation> iter) {
		if (!(getDocument() instanceof IDocumentExtension3)) { //can not proceed otherwise
			return;
		}
		IDocumentExtension3 document = (IDocumentExtension3) getDocument();
		IDocumentPartitioner docPartitioner = document.getDocumentPartitioner(XMLStringPartitionScanner.XML_STRING);
		IDocumentPartitioner pdeXMLPartitioner = document.getDocumentPartitioner(XMLDocumentSetupParticpant.XML_PARTITIONING);
		IAnnotationModel model = getAnnotationModel();

		while (iter.hasNext()) {
			Object annotation = iter.next();
			if (annotation instanceof SpellingAnnotation) {
				SpellingAnnotation spellingAnnotation = (SpellingAnnotation) annotation;
				Position position = model.getPosition(spellingAnnotation);
				String docContentType = docPartitioner.getContentType(position.getOffset());
				String pdeXMLContentType = pdeXMLPartitioner.getContentType(position.getOffset());
				if ((!XMLStringPartitionScanner.XML_STRING.equalsIgnoreCase(docContentType) && !XMLStringPartitionScanner.CUSTOM_TAG.equalsIgnoreCase(docContentType)) // delete the annotation if the marked position was not selected by XML_STRING document partitioner
						|| (XMLStringPartitionScanner.CUSTOM_TAG.equalsIgnoreCase(docContentType)) && XMLPartitionScanner.XML_TAG.equalsIgnoreCase(pdeXMLContentType)) { // also delete the annotations that are positioned at special XML Tags
					model.removeAnnotation(spellingAnnotation);
				}
			}
		}
	}

	@Override
	protected ISpellingProblemCollector createSpellingProblemCollector() {
		IAnnotationModel model = getAnnotationModel();
		if (model == null)
			return null;
		return new SpellingProblemCollector(model);

	}

}
