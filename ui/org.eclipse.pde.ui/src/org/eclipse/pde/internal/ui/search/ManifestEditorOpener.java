package org.eclipse.pde.internal.ui.search;

import org.eclipse.jface.text.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.editor.plugin.*;
import org.eclipse.search.ui.text.*;
import org.eclipse.ui.*;


public class ManifestEditorOpener {

	public static IEditorPart open(Match match, boolean activate) throws PartInitException {
		IEditorPart editorPart = null;
		Object element = match.getElement();
		if (element instanceof IPluginObject) {
			ISharedPluginModel model = ((IPluginObject)element).getModel();
			if (model instanceof WorkspaceExtensionsModel) {
				model = ((WorkspaceExtensionsModel)model).getBundlePluginModel();
			}
			if (model instanceof IPluginModelBase)
				editorPart = ManifestEditor.openPluginEditor(((IPluginModelBase)model).getPluginBase());
		}
		if (editorPart != null && editorPart instanceof ManifestEditor) {
			ManifestEditor editor = (ManifestEditor)editorPart;
			IDocument doc = editor.getDocument(match);
			if (doc != null) {
				Match exact = findExactMatch(doc, match);
				editor.openToSourcePage(element, exact.getOffset(), exact.getLength());
			}
		}
		return editorPart;
	}
	
	public static Match findExactMatch(IDocument document, Match match) {
		if (match.getOffset() == -1 && match.getBaseUnit() == Match.UNIT_LINE)
			return new Match(match.getElement(), Match.UNIT_CHARACTER, 0,0);
		IPluginObject element = (IPluginObject)match.getElement();
		String name = null;
		String value = null;
		if (element instanceof IPluginExtension) {
			name = "point"; //$NON-NLS-1$
			value = ((IPluginExtension)element).getPoint();
		} else if (element instanceof IPluginExtensionPoint) {
			name = "id"; //$NON-NLS-1$
			value = ((IPluginExtensionPoint)element).getId();
		} else if (element instanceof IPluginImport) {
			name = "plugin"; //$NON-NLS-1$
			value = ((IPluginImport)element).getId();
		} else if (element instanceof IPlugin) {
			name = "id"; //$NON-NLS-1$
			value = ((IPlugin)element).getId();
		} else if (element instanceof IFragment) {
			name = "id"; //$NON-NLS-1$
			value = ((IFragment)element).getId();
		}
		IRegion region = getAttributeRegion(document, name, value, match.getOffset());
		if (region != null) {
			return new Match(element, Match.UNIT_CHARACTER, region.getOffset(), region.getLength());
		}	
		return match;
	}
	
	private static IRegion getAttributeRegion(IDocument document, String name, String value, int line) {
		try {
			int offset = document.getLineOffset(line) + document.getLineLength(line);
			FindReplaceDocumentAdapter findReplaceAdapter = new FindReplaceDocumentAdapter(document);
			IRegion nameRegion = findReplaceAdapter.find(offset, name+"\\s*=\\s*\""+value, false, false, false, true); //$NON-NLS-1$
			if (nameRegion != null) {
				if (document.get(nameRegion.getOffset() + nameRegion.getLength() - value.length(), value.length()).equals(value))
					return new Region(nameRegion.getOffset() + nameRegion.getLength() - value.length(), value.length());
			}
		} catch (BadLocationException e) {
		}
		return null;
	}


}
