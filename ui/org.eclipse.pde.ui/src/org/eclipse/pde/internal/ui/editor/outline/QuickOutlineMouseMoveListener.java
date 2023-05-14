/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
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
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.outline;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * QuickOutlineMouseMoveListener
 *
 */
public class QuickOutlineMouseMoveListener implements MouseMoveListener {

	private TreeItem fLastItem;

	private TreeViewer fTreeViewer;

	/**
	 *
	 */
	public QuickOutlineMouseMoveListener(TreeViewer treeViewer) {
		fLastItem = null;
		fTreeViewer = treeViewer;
	}

	@Override
	public void mouseMove(MouseEvent e) {
		Tree tree = fTreeViewer.getTree();
		if (tree.equals(e.getSource())) {
			Object o = tree.getItem(new Point(e.x, e.y));
			if (o instanceof TreeItem) {
				if (!o.equals(fLastItem)) {
					fLastItem = (TreeItem) o;
					tree.setSelection(new TreeItem[] {fLastItem});
				} else if (e.y < tree.getItemHeight() / 4) {
					// Scroll up
					Point p = tree.toDisplay(e.x, e.y);
					Item item = fTreeViewer.scrollUp(p.x, p.y);
					if (item instanceof TreeItem) {
						fLastItem = (TreeItem) item;
						tree.setSelection(new TreeItem[] {fLastItem});
					}
				} else if (e.y > tree.getBounds().height - tree.getItemHeight() / 4) {
					// Scroll down
					Point p = tree.toDisplay(e.x, e.y);
					Item item = fTreeViewer.scrollDown(p.x, p.y);
					if (item instanceof TreeItem) {
						fLastItem = (TreeItem) item;
						tree.setSelection(new TreeItem[] {fLastItem});
					}
				}
			}
		}
	}

}
