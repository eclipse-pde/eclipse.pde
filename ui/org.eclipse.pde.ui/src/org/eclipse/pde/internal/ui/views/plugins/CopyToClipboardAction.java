/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.views.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.FileAdapter;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;

public class CopyToClipboardAction extends Action {
	IStructuredSelection selection;
	private Clipboard clipboard;

	/**
	 * Constructor for CopyToClipboardAction.
	 */
	protected CopyToClipboardAction(Clipboard clipboard) {
		setEnabled(false);
		this.clipboard = clipboard;
	}

	/**
	 * Constructor for CopyToClipboardAction.
	 */
	protected CopyToClipboardAction(String text) {
		super(text);
	}

	public void setSelection(IStructuredSelection selection) {
		this.selection = selection;
		setEnabled(canCopy(selection));
	}

	private boolean canCopy(IStructuredSelection selection) {
		if (selection.isEmpty())
			return false;
		for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (!(obj instanceof FileAdapter))
				return false;
		}
		return true;
	}

	@Override
	public void run() {
		if (selection.isEmpty())
			return;
		ArrayList<Object> files = new ArrayList<>();
		for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (obj instanceof FileAdapter)
				files.add(obj);
		}
		doCopy(files);
	}

	private void doCopy(ArrayList<Object> files) {
		// Get the file names and a string representation
		int len = files.size();
		String[] fileNames = new String[len];
		StringBuilder buf = new StringBuilder();
		for (int i = 0, length = len; i < length; i++) {
			FileAdapter adapter = (FileAdapter) files.get(i);
			File file = adapter.getFile();
			fileNames[i] = file.getAbsolutePath();
			if (i > 0)
				buf.append("\n"); //$NON-NLS-1$
			buf.append(file.getName());
		}

		// set the clipboard contents
		clipboard.setContents(new Object[] {fileNames, buf.toString()}, new Transfer[] {FileTransfer.getInstance(), TextTransfer.getInstance()});
	}
}
