package org.eclipse.pde.internal.ui.editor;

import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.core.runtime.IAdaptable;
import java.io.*;

/**
 * Insert the type's description here.
 */
public class SystemFileEditorInputFactory implements IElementFactory {
	/**
	 * The constructor.
	 */
	public SystemFileEditorInputFactory() {
	}
	
	public IAdaptable createElement (IMemento memento) {
		String path = memento.getString("path");
		File file = new File(path);
		SystemFileEditorInput input = new SystemFileEditorInput(file);
		return input;
	}
}
