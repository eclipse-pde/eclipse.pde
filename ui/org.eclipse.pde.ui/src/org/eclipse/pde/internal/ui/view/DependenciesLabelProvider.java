package org.eclipse.pde.internal.ui.view;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import java.io.*;
import org.eclipse.ui.*;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.core.index.IEntryResult;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.resource.ImageDescriptor;

public class DependenciesLabelProvider extends LabelProvider {
	private PDELabelProvider sharedProvider;

	/**
	 * Constructor for PluginsLabelProvider.
	 */
	public DependenciesLabelProvider() {
		super();
		sharedProvider = PDEPlugin.getDefault().getLabelProvider();
		sharedProvider.connect(this);
	}

	public void dispose() {
		sharedProvider.disconnect(this);
		super.dispose();
	}

	public String getText(Object obj) {
		return sharedProvider.getText(obj);
	}

	public Image getImage(Object obj) {
		return sharedProvider.getImage(obj);
	}
}