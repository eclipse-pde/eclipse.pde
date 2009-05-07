/*******************************************************************************
 * Copyright (c) 2003, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Deepak Azad <deepak.azad@in.ibm.com> - bug 249066
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.builders.DependencyLoop;
import org.eclipse.pde.internal.core.builders.DependencyLoopFinder;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.search.dependencies.UnusedDependenciesAction;
import org.eclipse.pde.internal.ui.views.dependencies.OpenPluginDependenciesAction;
import org.eclipse.pde.internal.ui.views.dependencies.OpenPluginReferencesAction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.*;

public class DependencyAnalysisSection extends PDESection {
	private FormText formText;

	public DependencyAnalysisSection(PDEFormPage page, Composite parent, int style) {
		super(page, parent, ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE | style);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	private String getFormText() {
		boolean editable = getPage().getModel().isEditable();
		if (getPage().getModel() instanceof IPluginModel) {
			if (editable)
				return PDEUIMessages.DependencyAnalysisSection_plugin_editable;
			return PDEUIMessages.DependencyAnalysisSection_plugin_notEditable;
		}
		if (editable)
			return PDEUIMessages.DependencyAnalysisSection_fragment_editable;
		return PDEUIMessages.DependencyAnalysisSection_fragment_notEditable;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.DependencyAnalysisSection_title);

		formText = toolkit.createFormText(section, true);
		formText.setText(getFormText(), true, false);
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		formText.setImage("loops", lp.get(PDEPluginImages.DESC_LOOP_OBJ)); //$NON-NLS-1$
		formText.setImage("search", lp.get(PDEPluginImages.DESC_PSEARCH_OBJ)); //$NON-NLS-1$
		formText.setImage("hierarchy", lp.get(PDEPluginImages.DESC_CALLEES)); //$NON-NLS-1$
		formText.setImage("dependencies", lp.get(PDEPluginImages.DESC_CALLERS)); //$NON-NLS-1$
		formText.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				if (e.getHref().equals("unused")) //$NON-NLS-1$
					doFindUnusedDependencies();
				else if (e.getHref().equals("loops")) //$NON-NLS-1$
					doFindLoops();
				else if (e.getHref().equals("references")) //$NON-NLS-1$
					new OpenPluginReferencesAction(PluginRegistry.findModel(getPlugin().getId())).run();
				else if (e.getHref().equals("hierarchy")) //$NON-NLS-1$
					new OpenPluginDependenciesAction(PluginRegistry.findModel(getPlugin().getId())).run();
			}
		});

		section.setClient(formText);
	}

	protected IPlugin getPlugin() {
		IBaseModel model = getPage().getModel();
		IPlugin plugin = null;
		if (model instanceof IPluginModel) {
			plugin = ((IPluginModel) model).getPlugin();
		}
		return plugin;
	}

	protected void doFindLoops() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IPluginModel) {
			IPlugin plugin = ((IPluginModel) model).getPlugin();
			DependencyLoop[] loops = DependencyLoopFinder.findLoops(plugin);
			if (loops.length == 0)
				MessageDialog.openInformation(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.DependencyAnalysisSection_loops, PDEUIMessages.DependencyAnalysisSection_noCycles); // 
			else {
				LoopDialog dialog = new LoopDialog(PDEPlugin.getActiveWorkbenchShell(), loops);
				dialog.open();
			}
		}
	}

	protected void doFindUnusedDependencies() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IPluginModelBase) {
			new UnusedDependenciesAction((IPluginModelBase) model, false).run();
		}
	}

}
