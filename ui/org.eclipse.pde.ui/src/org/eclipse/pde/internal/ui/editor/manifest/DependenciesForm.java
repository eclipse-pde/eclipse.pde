/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.manifest;

import org.eclipse.pde.core.plugin.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.jface.action.*;
import org.eclipse.core.resources.*;

public class DependenciesForm extends ScrollableSectionForm {
	public static final String TITLE = "ManifestEditor.DependenciesForm.title";
	private ManifestDependenciesPage page;
	private ImportListSection importListSection;
	private MatchSection matchSection;
	private ImportStatusSection importStatusSection;

	public DependenciesForm(ManifestDependenciesPage page) {
		this.page = page;
		setVerticalFit(true);
		setScrollable(true);
	}
	public void commitChanges(boolean onSave) {
		if (importListSection == null)
			return;
		if (onSave || importListSection.isDirty())
			importListSection.commitChanges(onSave);
	}
	protected void createFormClient(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = true;
		layout.marginWidth = 10;
		layout.horizontalSpacing = 15;
		parent.setLayout(layout);

		GridData gd;
		Control control;

		importListSection = new ImportListSection(page);
		control = importListSection.createControl(parent, getFactory());
		gd = new GridData(GridData.FILL_BOTH);
		control.setLayoutData(gd);

		Composite column = factory.createComposite(parent);
		gd = new GridData(GridData.FILL_BOTH);
		column.setLayoutData(gd);
		layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		column.setLayout(layout);

		matchSection = new MatchSection(page);
		control = matchSection.createControl(column, getFactory());
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
		control.setLayoutData(gd);

		if (!((ManifestEditor) page.getEditor()).isFragmentEditor()) {
			importStatusSection = new ImportStatusSection(page);
			control = importStatusSection.createControl(column, getFactory());
			gd = new GridData(GridData.FILL_BOTH);
			control.setLayoutData(gd);
		} // Link forms
		SectionChangeManager manager = new SectionChangeManager();
		manager.linkSections(importListSection, matchSection);
		registerSection(importListSection);
		registerSection(matchSection);
		if (!((ManifestEditor) page.getEditor()).isFragmentEditor())
			registerSection(importStatusSection);
			
		if (((ManifestEditor)page.getEditor()).isFragmentEditor()	)
			WorkbenchHelp.setHelp(parent,IHelpContextIds.MANIFEST_FRAGMENT_DEPENDENCIES);
		else
			WorkbenchHelp.setHelp(parent,IHelpContextIds.MANIFEST_PLUGIN_DEPENDENCIES);		
	}

	public void initialize(Object input) {
		IPluginModelBase model = (IPluginModelBase) input;
		setHeadingText(PDEPlugin.getResourceString(TITLE));
		super.initialize(model);
		((Composite) getControl()).layout(true);
	}

	public void expandTo(Object object) {
		importListSection.expandTo(object);
	}

	public boolean fillContextMenu(IMenuManager manager) {
		IResource resource =
			((IPluginModelBase) page.getModel()).getUnderlyingResource();
		if (resource != null
			&& WorkspaceModelManager.isJavaPluginProject(resource.getProject())) {
			manager.add(importListSection.getBuildpathAction());
			manager.add(new Separator());
		}
		return true;
	}
}
