/*******************************************************************************
 *  Copyright (c) 2006, 2016 IBM Corporation and others.
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
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 507831
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
		ProjectionAnnotationModel annotationModel = fEditor.getAdapter(ProjectionAnnotationModel.class);
		if (annotationModel == null)
			return;

		Set<Position> currentRegions = new HashSet<>();
		try {
			addFoldingRegions(currentRegions, fModel);
			updateFoldingRegions(annotationModel, currentRegions);
		} catch (BadLocationException e) {
		}
	}

	public void updateFoldingRegions(ProjectionAnnotationModel model, Set<Position> currentRegions) {
		Annotation[] deletions = computeDifferences(model, currentRegions);

		Map<ProjectionAnnotation, Position> additionsMap = new HashMap<>();
		for (Position position : currentRegions) {
			additionsMap.put(new ProjectionAnnotation(false), position);
		}

		if ((deletions.length != 0 || !additionsMap.isEmpty())) {
			model.modifyAnnotations(deletions, additionsMap, new Annotation[] {});
		}
	}

	private Annotation[] computeDifferences(ProjectionAnnotationModel model, Set<?> additions) {
		List<Object> deletions = new ArrayList<>();
		for (Iterator<?> iter = model.getAnnotationIterator(); iter.hasNext();) {
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
		return deletions.toArray(new Annotation[deletions.size()]);
	}

	@Override
	public void initialize() {
		update();
	}

	@Override
	public void modelChanged(IModelChangedEvent event) {
		update();
	}

	@Override
	public void reconciled(IDocument document) {
		update();
	}

}
