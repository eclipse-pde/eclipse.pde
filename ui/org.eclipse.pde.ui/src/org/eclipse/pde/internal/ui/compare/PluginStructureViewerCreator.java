/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.compare;

import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.*;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.widgets.Composite;

public class PluginStructureViewerCreator implements IViewerCreator {

	public PluginStructureViewerCreator() {
		// Nothing to do
	}

	public Viewer createViewer(Composite parent, CompareConfiguration config) {
		StructureDiffViewer viewer = new StructureDiffViewer(parent, config);
		viewer.setStructureCreator(new PluginStructureCreator());
		viewer.setComparator(new ViewerComparator() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				if (e1 instanceof DiffNode) {
					if (e2 instanceof DiffNode) {
						ITypedElement e1Element = ((DiffNode) e1).getAncestor();
						ITypedElement e2Element = ((DiffNode) e2).getAncestor();
						if (!(e1Element instanceof DocumentRangeNode))
							e1Element = ((DiffNode) e1).getLeft();
						if (!(e2Element instanceof DocumentRangeNode))
							e2Element = ((DiffNode) e2).getLeft();
						if (e1Element instanceof DocumentRangeNode && e2Element instanceof DocumentRangeNode) {
							float e1off = getRelativeOffset(((DocumentRangeNode) e1Element));
							float e2off = getRelativeOffset(((DocumentRangeNode) e2Element));
							return e1off - e2off < 0 ? -1 : 1;
						}
						return 0;
					}
					return -1;
				}
				return 1;
			}

			// we may be comparing the ancestor to the left (local) document
			// since the lengths may be different, base the comparison on the relative position in the doc
			private float getRelativeOffset(DocumentRangeNode node) {
				float absoluteOffset = node.getRange().getOffset();
				float documentLength = node.getDocument().getLength();
				return absoluteOffset / documentLength;
			}
		});
		return viewer;
	}

}
