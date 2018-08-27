/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others.
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
 *     Brock Janiczak <brockj@tpg.com.au> - bug 198881
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 262622
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.build;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.core.text.IDocumentKey;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.build.BuildEntry;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.editor.KeyValueSourcePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.text.ChangeAwareSourceViewerConfiguration;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;

public class BuildSourcePage extends KeyValueSourcePage {
	class BuildOutlineContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getChildren(Object parent) {
			return new Object[0];
		}

		@Override
		public boolean hasChildren(Object parent) {
			return false;
		}

		@Override
		public Object getParent(Object child) {
			if (child instanceof IBuildEntry)
				return ((IBuildEntry) child).getModel();
			return null;
		}

		@Override
		public Object[] getElements(Object parent) {
			if (parent instanceof IBuildModel) {
				IBuildModel model = (IBuildModel) parent;
				IBuild build = model.getBuild();
				return build.getBuildEntries();
			}
			return new Object[0];
		}
	}

	class BuildLabelProvider extends LabelProvider {
		@Override
		public String getText(Object obj) {
			if (obj instanceof IBuildEntry) {
				return ((IBuildEntry) obj).getName();
			}
			return super.getText(obj);
		}

		@Override
		public Image getImage(Object obj) {
			if (obj instanceof IBuildEntry)
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_BUILD_VAR_OBJ);
			return null;
		}
	}

	public BuildSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
	}

	@Override
	public void setPreferenceStore(IPreferenceStore store) {
		super.setPreferenceStore(store);
	}

	@Override
	public ILabelProvider createOutlineLabelProvider() {
		return new BuildLabelProvider();
	}

	@Override
	public ITreeContentProvider createOutlineContentProvider() {
		return new BuildOutlineContentProvider();
	}

	protected IDocumentRange getRangeElement(ITextSelection selection) {
		if (selection.isEmpty())
			return null;
		IBuildModel model = (IBuildModel) getInputContext().getModel();
		return findBuildNode(model.getBuild().getBuildEntries(), selection.getOffset());
	}

	private BuildEntry findBuildNode(IBuildEntry[] nodes, int offset) {
		for (IBuildEntry n : nodes) {
			BuildEntry node = (BuildEntry) n;
			if (offset >= node.getOffset() && offset < node.getOffset() + node.getLength()) {
				return node;
			}
		}
		return null;
	}

	@Override
	protected String[] collectContextMenuPreferencePages() {
		String[] ids = super.collectContextMenuPreferencePages();
		String[] more = new String[ids.length + 1];
		more[0] = "org.eclipse.jdt.ui.preferences.PropertiesFileEditorPreferencePage"; //$NON-NLS-1$
		System.arraycopy(ids, 0, more, 1, ids.length);
		return more;
	}

	@Override
	protected boolean affectsTextPresentation(PropertyChangeEvent event) {
		return ((BuildSourceViewerConfiguration) getSourceViewerConfiguration()).affectsTextPresentation(event) || super.affectsTextPresentation(event);
	}

	@Override
	public IDocumentRange getRangeElement(int offset, boolean searchChildren) {
		IBuildModel model = (IBuildModel) getInputContext().getModel();
		IBuildEntry[] buildEntries = model.getBuild().getBuildEntries();

		for (IBuildEntry entry : buildEntries) {
			IDocumentKey key = (IDocumentKey) entry;
			if (offset >= key.getOffset() && offset < key.getOffset() + key.getLength())
				return key;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (IHyperlinkDetector.class.equals(adapter))
			return (T) new BuildHyperlinkDetector(this);
		return super.getAdapter(adapter);
	}

	@Override
	public void updateSelection(Object object) {
		if (object instanceof IDocumentKey) {
			setHighlightRange((IDocumentKey) object);
		} else {
			resetHighlightRange();
		}
	}

	@Override
	protected ChangeAwareSourceViewerConfiguration createSourceViewerConfiguration(IColorManager colorManager) {
		IPreferenceStore store = PreferenceConstants.getPreferenceStore();
		IPreferenceStore generalTextStore = EditorsUI.getPreferenceStore();
		IPreferenceStore combinedStore = new ChainedPreferenceStore(new IPreferenceStore[] {store, generalTextStore});
		this.setPreferenceStore(combinedStore);
		return new BuildSourceViewerConfiguration(colorManager, combinedStore, this);
	}
}
