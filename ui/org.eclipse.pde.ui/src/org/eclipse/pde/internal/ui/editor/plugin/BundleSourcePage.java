/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;
import java.util.*;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.model.IDocumentRange;
import org.eclipse.pde.internal.ui.model.bundle.*;
import org.eclipse.swt.graphics.*;

public class BundleSourcePage extends KeyValueSourcePage {
	class BundleOutlineContentProvider extends DefaultContentProvider
			implements ITreeContentProvider {
		public Object[] getChildren(Object parent) {
			return new Object[0];
		}
		public boolean hasChildren(Object parent) {
			return false;
		}
		public Object getParent(Object child) {
			return null;
		}
		public Object[] getElements(Object parent) {
			if (parent instanceof BundleModel) {
				BundleModel model = (BundleModel) parent;
				Dictionary manifest = ((Bundle)model.getBundle()).getHeaders();
				Object[] keys = new Object[manifest.size()];
				int i = 0;
				for (Enumeration elements = manifest.keys(); elements.hasMoreElements();) {
					keys[i++] = manifest.get(elements.nextElement());
				}
				return keys;
			}
			return new Object[0];
		}
	}
	class BundleLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			if (obj instanceof ManifestHeader) {
				return ((ManifestHeader) obj).getName();
			}
			return super.getText(obj);
		}
		public Image getImage(Object obj) {
			if (obj instanceof ManifestHeader)
				return PDEPlugin.getDefault().getLabelProvider().get(
					PDEPluginImages.DESC_BUILD_VAR_OBJ);
			return null;
		}
	}
	
	public BundleSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
	}
	
	protected ILabelProvider createOutlineLabelProvider() {
		return new BundleLabelProvider();
	}
	
	protected ITreeContentProvider createOutlineContentProvider() {
		return new BundleOutlineContentProvider();
	}
	protected IDocumentRange getRangeElement(ITextSelection selection) {
		if (selection.isEmpty())
			return null;
		IBundleModel model = (IBundleModel) getInputContext().getModel();
		Dictionary manifest = ((Bundle) model.getBundle()).getHeaders();
		int offset = selection.getOffset();
		IDocumentRange[] keys = new IDocumentRange[manifest.size()];
		int i = 0;
		for (Enumeration elements = manifest.keys(); elements.hasMoreElements();) {
			keys[i++] = (IDocumentRange) manifest.get(elements.nextElement());
		}
		IDocumentRange node = findBuildNode(keys, offset);
		return node;
	}

	private IDocumentRange findBuildNode(IDocumentRange[] nodes, int offset) {
		for (int i = 0; i < nodes.length; i++) {
			IDocumentRange node = (IDocumentRange) nodes[i];
			if (offset >= node.getOffset()
					&& offset < node.getOffset() + node.getLength()) {
				return node;
			}
		}
		return null;
	}
}
