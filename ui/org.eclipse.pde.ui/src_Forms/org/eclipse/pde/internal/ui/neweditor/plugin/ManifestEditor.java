/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;

import java.io.File;
import java.util.Dictionary;

import org.eclipse.core.resources.*;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.ui.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ManifestEditor extends PDEFormEditor {
	protected void createInputContexts(Dictionary contexts) {
		IEditorInput input = getEditorInput();
		IEditorInput [] inputs = getRelevantInputs(input);
		for (int i=0; i<inputs.length; i++) {
			contexts.put(inputs[i], createInputContext(inputs[i]));
		}
	}
	protected IEditorInput [] getRelevantInputs(IEditorInput input) {
		if (input instanceof IFileEditorInput) {
			// resource - find the project
			return getRelevantProjectInputs((IFileEditorInput)input);
		}
		else if (input instanceof SystemFileEditorInput) {
			// system file - find the plug-in folder
			return getRelevantSystemFileInputs((SystemFileEditorInput)input);
		}
		return new IEditorInput [] { input };
	}
	
	protected IEditorInput [] getRelevantProjectInputs(IFileEditorInput input) {
		IFile file = input.getFile();
		IProject project = file.getProject();
		return new IEditorInput [] { input };
	}

	protected IEditorInput [] getRelevantSystemFileInputs(SystemFileEditorInput input) {
		File file = (File)input.getAdapter(File.class);
		File manifestFile=null;
		File buildFile = null;
		File pluginFile = null;
		String name = file.getName().toLowerCase();
		File rootFolder;
		if (name.equals("manifest.mf")) {
			manifestFile = file;
			File dir = file.getParentFile().getParentFile();
			buildFile = new File(dir, "build.properties");
			pluginFile = new File(dir, "plugin.xml");
			if (!pluginFile.exists())
				pluginFile = new File(dir, "fragment.xml");
		}
		return new IEditorInput [] { input };
	}

	protected void contextMenuAboutToShow(IMenuManager manager) {
	}

	protected InputContext createInputContext(IEditorInput input) {
		return null;
	}

	protected void addPages() {
	}
}