/*******************************************************************************
 * Copyright (c) 2017, 2019 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lucas Bullen (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.reconciler.folding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionViewer;

public class FoldingReconcilerStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

	private IDocument document;
	private String oldDocument;
	private ProjectionViewer projectionViewer;
	private List<Annotation> oldAnnotations = new ArrayList<>();
	private List<Position> oldPositions = new ArrayList<>();

	@Override
	public void setDocument(IDocument document) {
		this.document = document;
	}

	public void setProjectionViewer(ProjectionViewer projectionViewer) {
		this.projectionViewer = projectionViewer;
	}

	@Override
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		initialReconcile();
	}

	@Override
	public void reconcile(IRegion partition) {
		initialReconcile();
	}

	@Override
	public void initialReconcile() {
		ProjectionAnnotationModel projectionAnnotationModel = projectionViewer.getProjectionAnnotationModel();
		if (document.get().equals(oldDocument) || projectionAnnotationModel == null)
			return;
		oldDocument = document.get();

		List<Position> positions = getNewPositionsOfAnnotations();

		List<Position> positionsToRemove = new ArrayList<>();
		List<Annotation> annotationToRemove = new ArrayList<>();

		for (Position position : oldPositions) {
			if (!positions.contains(position)) {
				projectionAnnotationModel.removeAnnotation(oldAnnotations.get(oldPositions.indexOf(position)));
				positionsToRemove.add(position);
				annotationToRemove.add(oldAnnotations.get(oldPositions.indexOf(position)));
			} else {
				positions.remove(position);
			}
		}
		oldPositions.removeAll(positionsToRemove);
		oldAnnotations.removeAll(annotationToRemove);

		for (Position position : positions) {
			Annotation annotation = new ProjectionAnnotation();
			projectionAnnotationModel.addAnnotation(annotation, position);
			oldPositions.add(position);
			oldAnnotations.add(annotation);
		}
	}

	private static enum SearchingFor {
		START_OF_TAG, START_OF_WORD, END_OF_WORD, END_OF_COMMENT, END_OF_LINE
	}

	private List<Position> getNewPositionsOfAnnotations() {
		List<Position> positions = new ArrayList<>();
		Map<String, List<Integer>> startOfAnnotation = new HashMap<>();
		SearchingFor searchingFor = SearchingFor.START_OF_TAG;

		int characters = document.getLength();
		int currentCharIndex = 0;

		int wordStartIndex = 0;
		int sectionStartIndex = 0;
		boolean isCurrentTagClosing = false;
		String word = "";

		try {
			while (currentCharIndex < characters) {
				char currentChar = document.getChar(currentCharIndex);
				switch (searchingFor) {
				case START_OF_TAG:
					if (currentChar == '<') {
						char nextChar = document.getChar(currentCharIndex + 1);
						if (nextChar == '/') {
							isCurrentTagClosing = true;
						} else if (nextChar == '?') {
							searchingFor = SearchingFor.START_OF_TAG;
							break;
						} else if (document.get(currentCharIndex + 1, 3).equals("!--")) {
							word = "!--";
							sectionStartIndex = currentCharIndex;

							if (startOfAnnotation.containsKey(word)) {
								startOfAnnotation.get(word).add(sectionStartIndex);
							} else {
								List<Integer> startingIndexes = new ArrayList<>();
								startingIndexes.add(sectionStartIndex);
								startOfAnnotation.put(word, startingIndexes);
							}
							searchingFor = SearchingFor.END_OF_COMMENT;
							break;
						} else {
							isCurrentTagClosing = false;
						}
						sectionStartIndex = currentCharIndex;
						searchingFor = SearchingFor.START_OF_WORD;
					}
					break;
				case START_OF_WORD:
					if (Character.isLetter(currentChar)) {
						wordStartIndex = currentCharIndex;
						searchingFor = SearchingFor.END_OF_WORD;
					}
					break;
				case END_OF_COMMENT:
					if (document.get(currentCharIndex, 3).equals("-->")) {
						searchingFor = SearchingFor.END_OF_LINE;
					}
					break;
				case END_OF_WORD:
					if (!Character.isLetter(currentChar)) {
						word = document.get(wordStartIndex, currentCharIndex - wordStartIndex);
						if (isCurrentTagClosing) {
							searchingFor = SearchingFor.END_OF_LINE;
							if (characters == currentCharIndex + 1) {
								continue;
							}
						} else {
							if (startOfAnnotation.containsKey(word)) {
								startOfAnnotation.get(word).add(sectionStartIndex);
								searchingFor = SearchingFor.START_OF_TAG;
							} else {
								List<Integer> startingIndexes = new ArrayList<>();
								startingIndexes.add(sectionStartIndex);
								startOfAnnotation.put(word, startingIndexes);
								searchingFor = SearchingFor.START_OF_TAG;
							}
						}
					}
					break;
				case END_OF_LINE:
					if (startOfAnnotation.containsKey(word)) {
						if (currentChar == '\n' || characters == currentCharIndex + 1) {
							List<Integer> listOfAnnotationIndexes = startOfAnnotation.get(word);
							int start = listOfAnnotationIndexes.get(0);
							if (document.getLineOfOffset(start) != document.getLineOfOffset(currentCharIndex)) {
								positions.add(new Position(start, currentCharIndex + 1 - start));
							}
							if (listOfAnnotationIndexes.size() > 0) {
								listOfAnnotationIndexes.remove(0);
							} else {
								startOfAnnotation.remove(word);
							}
						}
						searchingFor = SearchingFor.START_OF_TAG;
					}
					break;
				}
				currentCharIndex++;
			}
		} catch (BadLocationException e) {
			// skip the remainder of file due to error
		}
		return positions;
	}

	@Override
	public void setProgressMonitor(IProgressMonitor monitor) {
		// no progress monitor used
	}

}