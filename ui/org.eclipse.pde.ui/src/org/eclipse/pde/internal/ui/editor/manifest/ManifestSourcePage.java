package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.action.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.builders.*;
import org.eclipse.pde.internal.builders.ManifestConsistencyChecker;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.text.*;

public class ManifestSourcePage extends PDESourcePage {
	public static final String MANIFEST_TYPE = "__plugin_manifest";
	protected IColorManager colorManager = new ColorManager();

	public ManifestSourcePage(ManifestEditor editor) {
		super(editor);
		initializeViewerConfiguration();
	}
	protected void initializeViewerConfiguration() {
		setSourceViewerConfiguration(new XMLConfiguration(colorManager));
	}
	public IContentOutlinePage createContentOutlinePage() {
		return new ManifestSourceOutlinePage(
			getEditorInput(),
			getDocumentProvider(),
			this);
	}
	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}
	protected void editorContextMenuAboutToShow(MenuManager menu) {
		getEditor().editorContextMenuAboutToShow(menu);
		menu.add(new Separator());
		super.editorContextMenuAboutToShow(menu);
	}
	protected boolean validateModelSemantics() {
		IPluginModelBase model = (IPluginModelBase)getEditor().getModel();
		IPluginBase pluginBase = model.getPluginBase();
		PluginErrorReporter reporter = new PluginErrorReporter(null);
		ManifestConsistencyChecker.validateRequiredAttributes(pluginBase, reporter);
		return reporter.getErrorCount()==0;
	}
}
