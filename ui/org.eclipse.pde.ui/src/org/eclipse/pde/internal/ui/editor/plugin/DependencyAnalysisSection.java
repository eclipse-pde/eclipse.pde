/*******************************************************************************
 * Copyright (c) 2003, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Deepak Azad <deepak.azad@in.ibm.com> - bug 249066
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.plugin.VersionMatchRule;
import org.eclipse.pde.internal.core.builders.DependencyLoop;
import org.eclipse.pde.internal.core.builders.DependencyLoopFinder;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.search.dependencies.UnusedDependenciesAction;
import org.eclipse.pde.internal.ui.views.dependencies.OpenPluginDependenciesAction;
import org.eclipse.pde.internal.ui.views.dependencies.OpenPluginReferencesAction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class DependencyAnalysisSection extends PDESection {

	public DependencyAnalysisSection(PDEFormPage page, Composite parent, int style) {
		super(page, parent, ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE | style);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	private String getFormText() {
		IBaseModel model = getPage().getModel();
		if (model == null) {
			return ""; // we don't know the type so we can't give //$NON-NLS-1$
						// a suitable hint (at the momment)
		}
		if (model instanceof IPluginModel) {
			return model.isEditable() //
					? PDEUIMessages.DependencyAnalysisSection_plugin_editable
					: PDEUIMessages.DependencyAnalysisSection_plugin_notEditable;
		}
		return model.isEditable() //
				? PDEUIMessages.DependencyAnalysisSection_fragment_editable
				: PDEUIMessages.DependencyAnalysisSection_fragment_notEditable;
	}

	@Override
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.DependencyAnalysisSection_title);

		FormText formText = toolkit.createFormText(section, true);
		formText.setText(getFormText(), true, false);
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		formText.setImage("loops", lp.get(PDEPluginImages.DESC_LOOP_OBJ)); //$NON-NLS-1$
		formText.setImage("search", lp.get(PDEPluginImages.DESC_PSEARCH_OBJ)); //$NON-NLS-1$
		formText.setImage("hierarchy", lp.get(PDEPluginImages.DESC_CALLEES)); //$NON-NLS-1$
		formText.setImage("dependencies", lp.get(PDEPluginImages.DESC_CALLERS)); //$NON-NLS-1$
		formText.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				if (getPage().getModel() instanceof IPluginModelBase pluginModel) {
					if (e.getHref().equals("unused")) { //$NON-NLS-1$
						new UnusedDependenciesAction(pluginModel, false).run();

					} else if (e.getHref().equals("loops")) { //$NON-NLS-1$
						doFindLoops(pluginModel);

					} else {
						IPluginModelBase plugin = getStatePlugin(pluginModel);
						if (e.getHref().equals("references")) { //$NON-NLS-1$
							new OpenPluginReferencesAction(plugin).run();
						} else if (e.getHref().equals("hierarchy")) { //$NON-NLS-1$
							new OpenPluginDependenciesAction(plugin).run();
						}
					}
				}
			}
		});
		section.setClient(formText);
	}

	private IPluginModelBase getStatePlugin(IPluginModelBase pluginModel) {
		// The pluginModel set for this page does not have a BundleDescriptor
		// set, therefore search the pluginModel from the registry that has it
		IPluginBase pluginBase = pluginModel.getPluginBase();
		return PluginRegistry.findModel(pluginBase.getId(), pluginBase.getVersion(), VersionMatchRule.PERFECT);
	}

	private void doFindLoops(IPluginModelBase pluginModelBase) {
		if (pluginModelBase instanceof IPluginModel pluginModel) {
			DependencyLoop[] loops = DependencyLoopFinder.findLoops(pluginModel.getPlugin());
			if (loops.length == 0)
				MessageDialog.openInformation(PDEPlugin.getActiveWorkbenchShell(),
						PDEUIMessages.DependencyAnalysisSection_loops,
						PDEUIMessages.DependencyAnalysisSection_noCycles);
			else {
				LoopDialog dialog = new LoopDialog(PDEPlugin.getActiveWorkbenchShell(), loops);
				dialog.open();
			}
		}
	}

}
