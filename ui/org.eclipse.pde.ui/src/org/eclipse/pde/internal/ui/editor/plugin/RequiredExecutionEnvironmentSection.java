/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.pde.internal.ui.model.bundle.Bundle;
import org.eclipse.pde.internal.ui.model.bundle.ManifestHeader;
import org.eclipse.pde.internal.ui.model.bundle.RequiredExecutionEnvironmentHeader;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.osgi.framework.Constants;

public class RequiredExecutionEnvironmentSection extends PDESection {

	private ComboPart fJRECombo;
	private ComboPart fJ2MECombo;
	
	public RequiredExecutionEnvironmentSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.TITLE_BAR);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText("Execution Environment"); 
		section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		Composite client = toolkit.createComposite(section);
		TableWrapLayout layout = new TableWrapLayout();
		layout.leftMargin = layout.rightMargin = toolkit.getBorderStyle() != SWT.NULL ? 0 : 2;
		layout.numColumns = 2;
		client.setLayout(layout);
		section.setClient(client);
		
		createJRECombo(client, toolkit);
		createCDCJRECombo(client, toolkit);
		refresh();
		hookComboListeners();
		
		toolkit.paintBordersFor(client);
		
		IBaseModel model = getPage().getModel();
		if (model instanceof IModelChangeProvider)
			((IModelChangeProvider) model).addModelChangedListener(this);
	}
	
	private void createJRECombo(Composite client, FormToolkit toolkit) {
		TableWrapData twd = new TableWrapData();
		twd.colspan = 2;
		Label descLabel = toolkit.createLabel(client, "Specify the minimum JRE level on which this plugin can run:");
		descLabel.setLayoutData(twd);
		
		Label standardLabel = toolkit.createLabel(client, "JRE profile:"); 
		standardLabel.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		TableWrapData td = new TableWrapData();
		td.valign = TableWrapData.MIDDLE;
		standardLabel.setLayoutData(td);
		
		fJRECombo = new ComboPart();
		fJRECombo.createControl(client, toolkit, SWT.READ_ONLY);
		td = new TableWrapData(TableWrapData.FILL_GRAB);
		td.valign = TableWrapData.MIDDLE;
		fJRECombo.getControl().setLayoutData(td);
		fJRECombo.setItems(RequiredExecutionEnvironmentHeader.getJRES());
		fJRECombo.add("", 0); //$NON-NLS-1$
	}
	
	private void createCDCJRECombo(Composite client, FormToolkit toolkit) {
		TableWrapData twd = new TableWrapData();
		twd.colspan = 2;
		Label descLabel = toolkit.createLabel(client, "Specify the J2ME environment on which this plugin can run:");
		descLabel.setLayoutData(twd);
		
		Label foundationLabel = toolkit.createLabel(client, "J2ME profile:"); 
		foundationLabel.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		TableWrapData td = new TableWrapData();
		td.valign = TableWrapData.MIDDLE;
		foundationLabel.setLayoutData(td);
		
		fJ2MECombo = new ComboPart();
		fJ2MECombo.createControl(client, toolkit, SWT.READ_ONLY);
		td = new TableWrapData(TableWrapData.FILL_GRAB);
		td.valign = TableWrapData.MIDDLE;
		fJ2MECombo.getControl().setLayoutData(td);
		fJ2MECombo.setItems(RequiredExecutionEnvironmentHeader.getJ2MES());
		fJ2MECombo.add("", 0); //$NON-NLS-1$
	}
	
	public void refresh() {
		RequiredExecutionEnvironmentHeader header = getHeader();
		if (header != null) {
			String minJ2ME = header.getMinimumJ2ME();
			String minJRE = header.getMinimumJRE();
			if (minJ2ME != null) fJ2MECombo.setText(minJ2ME);
			if (minJRE != null) fJRECombo.setText(minJRE);
		} else {
			fJ2MECombo.setText("");
			fJRECombo.setText("");
		}
	}
	
	public void hookComboListeners() {
		fJRECombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSelection(fJRECombo.getSelection(), true);
			}
		});
		fJ2MECombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSelection(fJ2MECombo.getSelection(), false);
			}
		});
	}
	
	private BundleInputContext getBundleContext() {
		InputContextManager manager = getPage().getPDEEditor().getContextManager();
		return (BundleInputContext) manager.findContext(BundleInputContext.CONTEXT_ID);
	}
	
	private Bundle getBundle() {
		BundleInputContext context = getBundleContext();
		if (context != null) {
			IBundleModel model = (IBundleModel)context.getModel();
			return (Bundle)model.getBundle();
		}
		return null;
	}
	
	private void handleSelection(String newValue, boolean isJRE) {
		RequiredExecutionEnvironmentHeader header = getHeader();
		if (header != null) {
			if (isJRE)
				newValue = header.updateJRE(newValue);
			else
				newValue = header.updateJ2ME(newValue);
		}
		getBundle().setHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, newValue);
	}
	
	private RequiredExecutionEnvironmentHeader getHeader() {
		ManifestHeader header = getBundle().getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		if (header != null && header instanceof RequiredExecutionEnvironmentHeader)
			return (RequiredExecutionEnvironmentHeader) header;
		return null;
	}
}
