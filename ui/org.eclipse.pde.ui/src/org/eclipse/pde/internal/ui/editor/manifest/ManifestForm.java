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
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.ui.forms.internal.*;

public class ManifestForm extends ScrollableSectionForm {
	private PluginSpecSection pluginSection;
	private ExtensionSection extensionSection;
	private RuntimeSection runtimeSection;
	private AlertSection alertSection;
	private ExtensionPointSection extensionPointSection;
	private RequiresSection requiresSection;
	private ManifestFormPage page;

	public ManifestForm(ManifestFormPage page) {
		this.page = page;
		setVerticalFit(true);
	}
	protected void createFormClient(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 10;
		layout.horizontalSpacing = 15;
		parent.setLayout(layout);

		GridData gd;
		Control control;
		FormWidgetFactory factory = getFactory();
		//factory.setHyperlinkUnderlineMode(HyperlinkSettings.UNDERLINE_ROLLOVER);

		alertSection = new AlertSection(page);
		alertSection.setCollapsable(true);
		control = alertSection.createControl(parent, factory);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		control.setLayoutData(gd);

		Composite leftColumn = getFactory().createComposite(parent);
		gd = new GridData(GridData.FILL_BOTH);
		leftColumn.setLayoutData(gd);
		GridLayout leftLayout = new GridLayout();
		leftLayout.verticalSpacing = 10;
		leftLayout.marginWidth = 0;
		leftColumn.setLayout(leftLayout);

		Composite rightColumn = getFactory().createComposite(parent);
		gd = new GridData(GridData.FILL_BOTH);
		rightColumn.setLayoutData(gd);
		GridLayout rightLayout = new GridLayout();
		rightLayout.verticalSpacing = 10;
		rightLayout.marginWidth = 0;
		rightColumn.setLayout(rightLayout);

		pluginSection = new PluginSpecSection(page);
		pluginSection.setCollapsable(true);
		control = pluginSection.createControl(leftColumn, factory);
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		control.setLayoutData(gd);

		extensionSection = new ExtensionSection(page);
		extensionSection.setCollapsable(true);
		control = extensionSection.createControl(leftColumn, factory);
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		control.setLayoutData(gd);

		runtimeSection = new RuntimeSection(page);
		runtimeSection.setCollapsable(true);
		control = runtimeSection.createControl(rightColumn, factory);
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		control.setLayoutData(gd);

		requiresSection = new RequiresSection(page);
		requiresSection.setCollapsable(true);
		control = requiresSection.createControl(rightColumn, factory);
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		control.setLayoutData(gd);

		extensionPointSection = new ExtensionPointSection(page);
		extensionPointSection.setCollapsable(true);
		control = extensionPointSection.createControl(rightColumn, factory);
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		gd.grabExcessVerticalSpace = true;
		control.setLayoutData(gd);

		registerSection(alertSection);
		registerSection(pluginSection);
		registerSection(runtimeSection);
		registerSection(requiresSection);
		registerSection(extensionSection);
		registerSection(extensionPointSection);
		if (((ManifestEditor)page.getEditor()).isFragmentEditor()	)
			WorkbenchHelp.setHelp(parent,IHelpContextIds.MANIFEST_FRAGMENT_OVERVIEW);
		else
			WorkbenchHelp.setHelp(parent,IHelpContextIds.MANIFEST_PLUGIN_OVERVIEW);		
	}

	public void initialize(Object modelObject) {
		IPluginModelBase model = (IPluginModelBase) modelObject;
		IPluginBase plugin = model.getPluginBase();
		String name = plugin.getTranslatedName();
		if (model.isEditable() == false) {
			name = PDEPlugin.getFormattedMessage(ManifestEditor.KEY_READ_ONLY, name);
		}
		super.initialize(model);
		setHeadingText(name);
	}
	
	public void setFocus() {
	}
	
	public void setHeadingText(String text) {
		super.setHeadingText(text);
		Composite control = (Composite)getControl();
		if (control!=null) {
			control.layout(true);
			control.redraw();
		}
	}
		

}
