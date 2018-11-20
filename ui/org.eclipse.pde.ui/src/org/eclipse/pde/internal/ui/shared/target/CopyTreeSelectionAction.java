/*******************************************************************************
 *  Copyright (c) 2018 Julian Honnen
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Julian Honnen <julian.honnen@vector.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import java.util.Collection;
import java.util.LinkedHashSet;
import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

class CopyTreeSelectionAction extends Action {

	private Tree fTree;

	public CopyTreeSelectionAction(Tree tree) {
		fTree = tree;

		setText(PDEUIMessages.EditorActions_copy);
		ISharedImages workbenchImages = PlatformUI.getWorkbench().getSharedImages();
		setImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		setDisabledImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));

		setActionDefinitionId(ActionFactory.COPY.getCommandId());
	}

	@Override
	public void run() {
		TreeItem[] selection = fTree.getSelection();
		if (selection.length == 0) {
			return;
		}

		Collection<TreeItem> itemsToCopy = filterDuplicates(selection);
		StringBuilder buffer = new StringBuilder();
		itemsToCopy.forEach(item -> appendItem(buffer, item, 0));

		Clipboard clipboard = new Clipboard(fTree.getDisplay());
		try {
			clipboard.setContents(new Object[] { buffer.toString() }, new Transfer[] { TextTransfer.getInstance() });
		} finally {
			clipboard.dispose();
		}
	}

	private Collection<TreeItem> filterDuplicates(TreeItem[] items) {
		LinkedHashSet<TreeItem> deduplicated = new LinkedHashSet<>();

		for (int i = 0; i < items.length; i++) {
			TreeItem item = items[i];
			if (!containsAnyParent(item, deduplicated)) {
				deduplicated.add(item);
			}
		}

		return deduplicated;
	}

	private boolean containsAnyParent(TreeItem item, Collection<TreeItem> items) {
		TreeItem parent = item.getParentItem();
		while (parent != null) {
			if (items.contains(parent)) {
				return true;
			}

			parent = parent.getParentItem();
		}

		return false;
	}

	private void appendItem(StringBuilder buffer, TreeItem item, int indent) {
		if (buffer.length() > 0) {
			buffer.append(System.lineSeparator());
		}

		for (int i = 0; i < indent; i++) {
			buffer.append('\t');
		}

		buffer.append(item.getText());
		if (item.getExpanded()) {
			TreeItem[] children = item.getItems();
			for (int i = 0; i < children.length; i++) {
				appendItem(buffer, children[i], indent + 1);
			}
		}
	}

}
