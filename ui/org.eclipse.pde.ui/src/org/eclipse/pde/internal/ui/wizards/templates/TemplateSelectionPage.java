/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.templates;

import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.ui.templates.ITemplateSection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;

public class TemplateSelectionPage extends WizardPage {
	private ArrayList candidates;
	private ArrayList visiblePages;
	private WizardCheckboxTablePart tablePart;
	private FormBrowser descriptionBrowser;
	private static final String NL_TITLE = "TemplateSelectionPage.title"; //$NON-NLS-1$
	private static final String NL_DESC = "TemplateSelectionPage.desc"; //$NON-NLS-1$
	private static final String NL_TABLE = "TemplateSelectionPage.table"; //$NON-NLS-1$
	private static final String NL_CNAME = "TemplateSelectionPage.column.name"; //$NON-NLS-1$
	private static final String NL_CPOINT = "TemplateSelectionPage.column.point"; //$NON-NLS-1$

	class TablePart extends WizardCheckboxTablePart {
		public TablePart(String mainLabel) {
			super(mainLabel);
		}
		protected StructuredViewer createStructuredViewer(
			Composite parent,
			int style,
			FormToolkit toolkit) {
			return super.createStructuredViewer(
				parent,
				style | SWT.FULL_SELECTION,
				toolkit);
		}
		protected void updateCounter(int amount) {
			super.updateCounter(amount);
			if (getContainer() != null)
				getContainer().updateButtons();
		}
	}

	class ListContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return candidates.toArray();
		}
	}

	class ListLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			ITemplateSection section = (ITemplateSection) obj;
			if (index == 0)
				return section.getLabel();
			return section.getUsedExtensionPoint();
		}
		public Image getColumnImage(Object obj, int index) {
			if (index == 0)
				return PDEPlugin.getDefault().getLabelProvider().get(
					PDEPluginImages.DESC_EXTENSION_OBJ);
			return PDEPlugin.getDefault().getLabelProvider().get(
					PDEPluginImages.DESC_EXT_POINT_OBJ);
		}
	}

	/**
	 * Constructor for TemplateSelectionPage.
	 * @param pageName
	 */
	public TemplateSelectionPage() {
		super("templateSelection"); //$NON-NLS-1$
		setTitle(PDEPlugin.getResourceString(NL_TITLE));
		setDescription(PDEPlugin.getResourceString(NL_DESC));
		initializeTemplates();
	}

	private void initializeTemplates(){
		createCandidates();
		tablePart = new TablePart(PDEPlugin.getResourceString(NL_TABLE));
		descriptionBrowser = new FormBrowser(SWT.BORDER | SWT.V_SCROLL);
		descriptionBrowser.setText(""); //$NON-NLS-1$
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		visiblePages = new ArrayList();
	}

	public void dispose() {
		super.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	private void createCandidates() {
		candidates = new ArrayList();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements =
			registry.getConfigurationElementsFor(PDEPlugin.getPluginId(), "templates"); //$NON-NLS-1$
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			addTemplate(element, candidates);
		}
	}

	private void addTemplate(IConfigurationElement config, ArrayList result) {
		if (config.getName().equalsIgnoreCase("template") == false) //$NON-NLS-1$
			return;

		try {
			Object template = config.createExecutableExtension("class"); //$NON-NLS-1$
			if (template instanceof ITemplateSection) {
				result.add(template);
			}
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 2;
		tablePart.createControl(container);
		CheckboxTableViewer viewer = tablePart.getTableViewer();
		viewer.setContentProvider(new ListContentProvider());
		viewer.setLabelProvider(new ListLabelProvider());
		initializeTable(viewer.getTable());

		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel = (IStructuredSelection) event.getSelection();
				handleSelectionChanged((ITemplateSection) sel.getFirstElement());
			}
		});
		descriptionBrowser.createControl(container);
		Control c = descriptionBrowser.getControl();
		GridData gd =
			new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_VERTICAL);
		gd.heightHint = 100;
		//gd.horizontalSpan = 2;
		c.setLayoutData(gd);
		viewer.setInput(PDEPlugin.getDefault());
		tablePart.selectAll(true);
		setControl(container);
		Dialog.applyDialogFont(container);
		WorkbenchHelp.setHelp(container, IHelpContextIds.TEMPLATE_SELECTION);
	}

	public ITemplateSection[] getSelectedTemplates() {
		Object[] elements = tablePart.getTableViewer().getCheckedElements();
		ITemplateSection[] result = new ITemplateSection[elements.length];
		System.arraycopy(elements, 0, result, 0, elements.length);
		return result;
	}

	private void initializeTable(Table table) {
		table.setHeaderVisible(true);
		TableColumn column = new TableColumn(table, SWT.NULL);
		column.setText(PDEPlugin.getResourceString(NL_CNAME));
		column.setResizable(true);
		column = new TableColumn(table, SWT.NULL);
		column.setText(PDEPlugin.getResourceString(NL_CPOINT));
		column.setResizable(true);

		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(50));
		layout.addColumnData(new ColumnWeightData(50));
		table.setLayout(layout);
	}

	private void handleSelectionChanged(ITemplateSection section) {
		String text = section != null ? section.getDescription() : ""; //$NON-NLS-1$
		if (text.length() > 0)
			text = "<p>" + text + "</p>"; //$NON-NLS-1$ //$NON-NLS-2$
		descriptionBrowser.setText(text);
	}

	public boolean canFlipToNextPage() {
		if (tablePart.getSelectionCount() == 0)
			return false;
		return super.canFlipToNextPage();
	}

	public IWizardPage getNextPage() {
		ITemplateSection[] sections = getSelectedTemplates();
		visiblePages.clear();

		for (int i = 0; i < sections.length; i++) {
			ITemplateSection section = sections[i];
			if (section.getPagesAdded() == false)
				section.addPages((Wizard) getWizard());

			for (int j = 0; j < section.getPageCount(); j++) {
				visiblePages.add(section.getPage(j));
			}
		}
		if (visiblePages.size() > 0)
			return (IWizardPage) visiblePages.get(0);
		
		return null;
	}

	public IWizardPage getNextVisiblePage(IWizardPage page) {
		if (page == this)
			return page.getNextPage();
		int index = visiblePages.indexOf(page);
		if (index >= 0 && index < visiblePages.size() - 1)
			return (IWizardPage) visiblePages.get(index + 1);
		return null;
	}

}
