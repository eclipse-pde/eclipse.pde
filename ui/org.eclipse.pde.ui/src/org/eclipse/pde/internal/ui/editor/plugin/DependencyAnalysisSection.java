/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.core.resources.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.builders.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.search.*;
import org.eclipse.pde.internal.ui.search.dependencies.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;

public class DependencyAnalysisSection extends PDESection implements IPartSelectionListener {
	private FormText formText;
	private ImportObject fSelectedDependency;
	/**
	 * @param page
	 * @param parent
	 * @param style
	 */
	public DependencyAnalysisSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.TWISTIE|Section.EXPANDED);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IPartSelectionListener#selectionChanged(org.eclipse.ui.forms.IFormPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IFormPart part, ISelection selection) {
		if (selection == null || selection.isEmpty()) {
			fSelectedDependency = null;
		} else {
			IStructuredSelection ssel = (IStructuredSelection)selection;
			fSelectedDependency = (ImportObject)ssel.getFirstElement();
		}
	}
	
	private String getFormText() {
		boolean editable = getPage().getModel().isEditable();
		if (getPage().getModel() instanceof IPluginModel) {
			if (editable)
				return PDEPlugin.getResourceString("DependencyAnalysisSection.plugin.editable");  //$NON-NLS-1$
			return PDEPlugin.getResourceString("DependencyAnalysisSection.plugin.notEditable"); //$NON-NLS-1$
		}
		if (editable)
			return PDEPlugin.getResourceString("DependencyAnalysisSection.fragment.editable");  //$NON-NLS-1$
		return PDEPlugin.getResourceString("DependencyAnalysisSection.fragment.notEditable"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEPlugin.getResourceString("DependencyAnalysisSection.title")); //$NON-NLS-1$
		//toolkit.createCompositeSeparator(section);
		
		formText = toolkit.createFormText(section, true);
		formText.setText(getFormText(), true, false);		
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		formText.setImage("loops", lp.get(PDEPluginImages.DESC_LOOP_OBJ)); //$NON-NLS-1$
		formText.setImage("search", lp.get(PDEPluginImages.DESC_PSEARCH_OBJ)); //$NON-NLS-1$
		formText.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				if (e.getHref().equals("extent")) //$NON-NLS-1$
					doFindPlugins();
				else if (e.getHref().equals("unused")) //$NON-NLS-1$
					doFindUnusedDependencies();
				else if (e.getHref().equals("loops")) //$NON-NLS-1$
					doFindLoops();
				else if (e.getHref().equals("references")) //$NON-NLS-1$
					doFindReferences();
			}
		});
		section.setClient(formText);
	}

	protected void doFindLoops() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IPluginModel) {
			IPlugin plugin = ((IPluginModel)model).getPlugin();
			DependencyLoop[] loops = DependencyLoopFinder.findLoops(plugin);
			if (loops.length == 0)
				MessageDialog.openInformation(PDEPlugin.getActiveWorkbenchShell(), PDEPlugin.getResourceString("DependencyAnalysisSection.loops"), PDEPlugin.getResourceString("DependencyAnalysisSection.noCycles")); //$NON-NLS-1$ //$NON-NLS-2$
			else {
				LoopDialog dialog = new LoopDialog(PDEPlugin.getActiveWorkbenchShell(), loops);
				dialog.open();
			}
		}	
	}

	protected void doFindPlugins() {
		if (fSelectedDependency != null) {
			getPage().getPDEEditor().doSave(null);
			IPluginImport dep = fSelectedDependency.getImport();
			IResource resource = dep.getModel().getUnderlyingResource();
			new DependencyExtentAction(resource.getProject(), dep.getId()).run();
		} else {
			MessageDialog.openInformation(getPage().getSite().getShell(), PDEPlugin.getResourceString("DependencyAnalysisSection.dialogtitle"), PDEPlugin.getResourceString("DependencyAnalysisSection.message")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	protected void doFindUnusedDependencies() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IPluginModelBase) {
			new UnusedDependenciesAction((IPluginModelBase)model, false).run();
		}		
	}
	
	private void doFindReferences() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IPluginModel) {
			new FindReferencesAction(((IPluginModel)model).getPlugin()).run();
		} else if (model instanceof IFragmentModel){
			IFragment fragment = ((IFragmentModel)model).getFragment();
			String id = fragment.getPluginId();
			ModelEntry entry = PDECore.getDefault().getModelManager().findEntry(id);
			if (entry != null) {
				IPluginModelBase pluginModel = entry.getActiveModel();
				new FindDeclarationsAction(pluginModel.getPluginBase()).run();
			} else {
				MessageDialog.openInformation(
						PDEPlugin.getActiveWorkbenchShell(), 
						PDEPlugin.getResourceString(PDEPlugin.getResourceString("DependencyAnalysisSection.references")),  //$NON-NLS-1$
						PDEPlugin.getResourceString(PDEPlugin.getResourceString("DependencyAnalysisSection.noReferencesFound")));  //$NON-NLS-1$
			}
		}
	}

}
