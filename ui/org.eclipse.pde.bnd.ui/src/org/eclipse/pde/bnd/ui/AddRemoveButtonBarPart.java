/*******************************************************************************
 * Copyright (c) 2015, 2019 bndtools project and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com> - initial API and implementation
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *******************************************************************************/
package org.bndtools.utils.swt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class AddRemoveButtonBarPart {

	public interface AddRemoveListener {
		void addSelected();

		void removeSelected();
	}

	private final List<AddRemoveListener>	listeners	= new ArrayList<>();

	private ToolBar							toolbar;

	private ToolItem						btnAdd;
	private ToolItem						btnRemove;

	public ToolBar createControl(Composite parent, int style) {
		toolbar = new ToolBar(parent, style);

		btnAdd = new ToolItem(toolbar, SWT.PUSH);
		btnAdd.setImage(PlatformUI.getWorkbench()
			.getSharedImages()
			.getImage(ISharedImages.IMG_OBJ_ADD));
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (AddRemoveListener l : listeners) {
					l.addSelected();
				}
			}
		});

		btnRemove = new ToolItem(toolbar, SWT.PUSH);
		btnRemove.setImage(PlatformUI.getWorkbench()
			.getSharedImages()
			.getImage(ISharedImages.IMG_TOOL_DELETE));
		btnRemove.setDisabledImage(PlatformUI.getWorkbench()
			.getSharedImages()
			.getImage(ISharedImages.IMG_TOOL_DELETE_DISABLED));
		btnRemove.setToolTipText("Remove");
		btnRemove.setEnabled(false);
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (AddRemoveListener l : listeners) {
					l.removeSelected();
				}
			}
		});

		return toolbar;
	}

	public void setAddEnabled(boolean enable) {
		btnAdd.setEnabled(enable);
	}

	public void setRemoveEnabled(boolean enable) {
		btnRemove.setEnabled(enable);
	}

	public void addListener(AddRemoveListener l) {
		listeners.add(l);
	}

	public void removeListener(AddRemoveListener l) {
		listeners.remove(l);
	}

}
