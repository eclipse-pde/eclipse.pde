/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.*;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDEManager;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.text.plugin.PluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.IDE;

public class TranslationHyperlink extends AbstractHyperlink {

	private IModel fBase;

	private boolean fOpened;

	public TranslationHyperlink(IRegion region, String element, IModel base) {
		super(region, element);
		fBase = base;
	}

	private String getLocalization() {
		String localiz = null;
		if (fBase instanceof PluginModelBase) {
			if (((PluginModelBase) fBase).getNLResourceHelper() != null)
				localiz = ((PluginModelBase) fBase).getNLResourceHelper().getNLFileBasePath();
		} else if (fBase instanceof IPluginModelBase)
			localiz = PDEManager.getBundleLocalization((IPluginModelBase) fBase);
		else if (fBase instanceof IBundleModel)
			localiz = ((IBundleModel) fBase).getBundle().getLocalization();
		return localiz;
	}

	public boolean getOpened() {
		return fOpened;
	}

	public void open() {
		fOpened = openHyperLink();
	}

	public boolean openHyperLink() {
		String localiz = getLocalization();
		if (localiz == null) {
			return false;
		} else if (fBase.getUnderlyingResource() == null) {
			return false;
		} else if (fElement.length() == 0 || fElement.charAt(0) != '%') {
			return false;
		}

		IProject proj = fBase.getUnderlyingResource().getProject();
		IFile file = proj.getFile(localiz + ".properties"); //$NON-NLS-1$
		if (!file.exists())
			return false;

		try {
			IEditorPart editor = IDE.openEditor(PDEPlugin.getActivePage(), file);
			if (!(editor instanceof TextEditor))
				return false;
			TextEditor tEditor = (TextEditor) editor;
			IDocument doc = tEditor.getDocumentProvider().getDocument(tEditor.getEditorInput());
			if (doc == null)
				return false;

			try {
				String key = fElement.substring(1);
				int keyLen = key.length();
				int length = doc.getLength();
				int start = 0;
				IRegion region = null;
				FindReplaceDocumentAdapter docSearch = new FindReplaceDocumentAdapter(doc);
				while ((region = docSearch.find(start, key, true, false, false, false)) != null) {
					int offset = region.getOffset();
					if (offset > 0) {
						// check for newline before
						char c = doc.getChar(offset - 1);
						if (c != '\n' && c != '\r') {
							start += keyLen;
							continue;
						}
					}
					if (offset + keyLen < length) {
						// check for whitespace / assign symbol after
						char c = doc.getChar(offset + keyLen);
						if (!Character.isWhitespace(c) && c != '=' && c != ':') {
							start += keyLen;
							continue;
						}
					}
					tEditor.selectAndReveal(offset, keyLen);
					break;
				}
			} catch (BadLocationException e) {
				PDEPlugin.log(e);
			}

		} catch (PartInitException e) {
			return false;
		}
		return true;
	}
}
