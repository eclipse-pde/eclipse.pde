/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.swt.dnd.*;

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
	 * @param text
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
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (!(obj instanceof FileAdapter))
				return false;
		}
		return true;
	}

	public void run() {
		if (selection.isEmpty())
			return;
		ArrayList files = new ArrayList();
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (obj instanceof FileAdapter)
				files.add(obj);
		}
		doCopy(files);
	}

	private void doCopy(ArrayList files) {
		// Get the file names and a string representation
		int len = files.size();
		String[] fileNames = new String[len];
		StringBuffer buf = new StringBuffer();
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
