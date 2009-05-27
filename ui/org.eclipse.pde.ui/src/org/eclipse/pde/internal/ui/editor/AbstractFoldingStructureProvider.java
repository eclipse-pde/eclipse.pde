/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import java.util.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.core.text.IEditingModel;

public abstract class AbstractFoldingStructureProvider implements IFoldingStructureProvider, IModelChangedListener {

	private PDESourcePage fEditor;
	private IEditingModel fModel;

	public AbstractFoldingStructureProvider(PDESourcePage editor, IEditingModel model) {
		this.fEditor = editor;
		this.fModel = model;
	}

	public void update() {
		ProjectionAnnotationModel annotationModel = (ProjectionAnnotationModel) fEditor.getAdapter(ProjectionAnnotationModel.class);
		if (annotationModel == null)
			return;

		Set currentRegions = new HashSet();
		try {
			addFoldingRegions(currentRegions, fModel);
			updateFoldingRegions(annotationModel, currentRegions);
		} catch (BadLocationException e) {
		}
	}

	public void updateFoldingRegions(ProjectionAnnotationModel model, Set currentRegions) {
		Annotation[] deletions = computeDifferences(model, currentRegions);

		Map additionsMap = new HashMap();
		for (Iterator iter = currentRegions.iterator(); iter.hasNext();) {
			Object position = iter.next();
			additionsMap.put(new ProjectionAnnotation(false), position);
		}

		if ((deletions.length != 0 || additionsMap.size() != 0)) {
			model.modifyAnnotations(deletions, additionsMap, new Annotation[] {});
		}
	}

	private Annotation[] computeDifferences(ProjectionAnnotationModel model, Set additions) {
		List deletions = new ArrayList();
		for (Iterator iter = model.getAnnotationIterator(); iter.hasNext();) {
			Object annotation = iter.next();
			if (annotation instanceof ProjectionAnnotation) {
				Position position = model.getPosition((Annotation) annotation);
				if (additions.contains(position)) {
					additions.remove(position);
				} else {
					deletions.add(annotation);
				}
			}
		}
		return (Annotation[]) deletions.toArray(new Annotation[deletions.size()]);
	}

	public void initialize() {
		update();
	}

	public void modelChanged(IModelChangedEvent event) {
		update();
	}

	public void reconciled(IDocument document) {
		update();
	}

}
