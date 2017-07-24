package $packageName$;

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.reconciler.*;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionViewer;

public class $javaClassPrefix$ReconcilerStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {
    // TODO this is logic for .project file to fold tags. Replace with your language logic!
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
		if(document.get().equals(oldDocument)) return;
		oldDocument = document.get();

		List<Position> positions = getNewPositionsOfAnnotations();

		List<Position> positionsToRemove = new ArrayList<>();
		List<Annotation> annotationToRemove = new ArrayList<>();

		for (Position position : oldPositions) {
			if(!positions.contains(position)) {
				projectionViewer.getProjectionAnnotationModel().removeAnnotation(oldAnnotations.get(oldPositions.indexOf(position)));
				positionsToRemove.add(position);
				annotationToRemove.add(oldAnnotations.get(oldPositions.indexOf(position)));
			}else {
				positions.remove(position);
			}
		}
		oldPositions.removeAll(positionsToRemove);
		oldAnnotations.removeAll(annotationToRemove);

		for (Position position : positions) {
			Annotation annotation = new ProjectionAnnotation();
			projectionViewer.getProjectionAnnotationModel().addAnnotation(annotation, position);
			oldPositions.add(position);
			oldAnnotations.add(annotation);
		}
    }

    private static enum SearchingFor {
         START_OF_TAG, START_OF_WORD, END_OF_WORD, END_OF_LINE
    }

    private List<Position> getNewPositionsOfAnnotations(){
        List<Position> positions = new ArrayList<>();
        Map<String, Integer> startOfAnnotation = new HashMap<>();
        SearchingFor searchingFor = SearchingFor.START_OF_TAG;

        int characters = document.getLength();
        int currentCharIndex = 0;

        int wordStartIndex = 0;
        int sectionStartIndex = 0;
        String word = "";

        try {
            while (currentCharIndex < characters) {
                char currentChar = document.getChar(currentCharIndex);
                switch (searchingFor) {
                case START_OF_TAG:
                    if(currentChar == '<') {
                        char nextChar = document.getChar(currentCharIndex+1);
                        if(nextChar != '?') {
                            sectionStartIndex = currentCharIndex;
                            searchingFor = SearchingFor.START_OF_WORD;
                        }
                    }
                    break;
                case START_OF_WORD:
                    if(Character.isLetter(currentChar)) {
                        wordStartIndex = currentCharIndex;
                        searchingFor = SearchingFor.END_OF_WORD;
                    }
                    break;
                case END_OF_WORD:
                    if(!Character.isLetter(currentChar)) {
                        word = document.get(wordStartIndex, currentCharIndex - wordStartIndex);
                        if(startOfAnnotation.containsKey(word)) {
                            searchingFor = SearchingFor.END_OF_LINE;
                        }else {
                            startOfAnnotation.put(word, sectionStartIndex);
                            searchingFor = SearchingFor.START_OF_TAG;
                        }
                    }
                    break;
                case END_OF_LINE:
                    if(currentChar == '\n') {
                        int start = startOfAnnotation.get(word);
                        if(document.getLineOfOffset(start) != document.getLineOfOffset(currentCharIndex)) {
                            positions.add(new Position(start,currentCharIndex + 1 - start));
                        }
                        startOfAnnotation.remove(word);
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