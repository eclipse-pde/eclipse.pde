package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDEManager;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
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

	private String getLocaliation() {
		String localiz = null;
		if (fBase instanceof IPluginModelBase)
			localiz = PDEManager.getBundleLocalization((IPluginModelBase)fBase);
		else if (fBase instanceof IBundleModel)
			localiz = ((IBundleModel)fBase).getBundle().getLocalization();
		return localiz;
	}
	
	public boolean getOpened() {
		return fOpened;
	}
	
	public void open() {
		fOpened = openHyperLink();
	}
	
	public boolean openHyperLink() {
		String localiz = getLocaliation();
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
			TextEditor tEditor = (TextEditor)editor;
			IDocument doc = tEditor.getDocumentProvider().getDocument(tEditor.getEditorInput());
			if (doc == null)
				return false;
			
			String key = fElement.substring(1);
			int keyLen = key.length();
			String contents = doc.get();
			int length = contents.length();
			int start = 0;
			int index;
			while ((index = contents.indexOf(key, start)) >= 0) {
				if (index > 0) {
					// check for newline before
					char c = contents.charAt(index - 1);
					if (c != '\n' && c != '\r') {
						start += keyLen;
						continue;
					}
				}
				if (index + keyLen < length) {
					// check for whitespace / assign symbol after
					char c = contents.charAt(index + keyLen);
					if (!Character.isWhitespace(c) && c != '=' && c != ':') {
						start += keyLen;
						continue;
					}
				}
				tEditor.selectAndReveal(index, keyLen);
				break;
			}
		} catch (PartInitException e) {
			return false;
		}
		return true;
	}
}
