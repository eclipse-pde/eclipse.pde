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
 *     Peter Friese <peter.friese@gentleware.com> - bug 194529
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class FormFilteredTree extends FilteredTree {

	private FormToolkit toolkit;

	private FormEntry fEntryFilter;

	public FormFilteredTree(Composite parent, int treeStyle, PatternFilter filter) {
		super(parent, treeStyle, filter, true);
	}

	@Override
	protected void createControl(Composite parent, int treeStyle) {
		toolkit = new FormToolkit(parent.getDisplay());
		GridLayout layout = FormLayoutFactory.createClearGridLayout(false, 1);
		// Space between filter text field and tree viewer
		layout.verticalSpacing = 3;
		super.createControl(parent, treeStyle);
		setLayout(layout);
	}

	@Override
	public void dispose() {
		if (toolkit != null) {
			toolkit.dispose();
			toolkit = null;
		}
		super.dispose();
	}

	@Override
	protected Text doCreateFilterText(Composite parent) {
		int borderStyle = toolkit.getBorderStyle();

		toolkit.setBorderStyle(SWT.NONE); // we don't want Forms border around tree filter

		Text temp = super.doCreateFilterText(parent);
		int style = temp.getStyle();
		temp.dispose();

		fEntryFilter = new FormEntry(parent, toolkit, null, style);
		toolkit.setBorderStyle(borderStyle); // restore Forms border settings

		setBackground(toolkit.getColors().getBackground());
		return fEntryFilter.getText();
	}

	@Override
	protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
		TreeViewer viewer = super.doCreateTreeViewer(parent, toolkit.getBorderStyle() | style);
		toolkit.paintBordersFor(viewer.getTree().getParent());
		return viewer;
	}

	/**
	 * @param part
	 */
	public void createUIListenerEntryFilter(IContextPart part) {
		// Required to enable Ctrl-V initiated paste operation on first focus
		// See Bug # 157973
		fEntryFilter.setFormEntryListener(new FormEntryAdapter(part) {
			// Override all callback methods except focusGained
			// See Bug # 184085
			@Override
			public void browseButtonSelected(FormEntry entry) {
				// NO-OP
			}

			@Override
			public void linkActivated(HyperlinkEvent e) {
				// NO-OP
			}

			@Override
			public void linkEntered(HyperlinkEvent e) {
				// NO-OP
			}

			@Override
			public void linkExited(HyperlinkEvent e) {
				// NO-OP
			}

			@Override
			public void selectionChanged(FormEntry entry) {
				// NO-OP
			}

			@Override
			public void textDirty(FormEntry entry) {
				// NO-OP
			}

			@Override
			public void textValueChanged(FormEntry entry) {
				// NO-OP
			}
		});
	}

	/**
	 * @return a boolean indicating whether the tree is filtered or not.
	 */
	public boolean isFiltered() {
		Text filterText = getFilterControl();
		if (filterText != null) {
			String filterString = filterText.getText();
			boolean filtered = (filterString != null && filterString.length() > 0 && !filterString.equals(getInitialText()));
			return filtered;
		}
		return false;
	}
}
