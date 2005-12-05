package org.eclipse.pde.internal.ui.compare;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IViewerCreator;
import org.eclipse.compare.structuremergeviewer.StructureDiffViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;

public class ManifestStructureMergeViewerCreator implements IViewerCreator {

	public Viewer createViewer(Composite parent, CompareConfiguration config) {
		StructureDiffViewer diffViewer = new StructureDiffViewer(parent, config);
		diffViewer.setStructureCreator(new ManifestStructureCreator());
		return diffViewer;
	}

}
