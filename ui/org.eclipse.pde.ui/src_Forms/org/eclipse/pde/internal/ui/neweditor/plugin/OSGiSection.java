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
package org.eclipse.pde.internal.ui.neweditor.plugin;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.newparts.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;
/**
 * @author cgwong
 *  
 */
public class OSGiSection extends TableSection implements IModelChangedListener {
	private TableViewer osgiTableViewer;
	private Button autoActivateButton;
	private Button nonAutoActivateButton;
	private Font boldFont;
	
	class TableContentProvider extends DefaultContentProvider
			implements
				IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IPluginLibrary)
				return ((IPluginLibrary) parent).getContentFilters();
			return new Object[0];
		}
	}
	class TableLabelProvider extends LabelProvider
			implements
				ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if (obj instanceof IPackageFragment)
				return ((IPackageFragment) obj).getElementName();
			return obj.toString();
		}
		public Image getColumnImage(Object obj, int index) {
			return JavaUI.getSharedImages().getImage(
					ISharedImages.IMG_OBJS_PACKAGE);
		}
	}
	public OSGiSection(PDEFormPage page, Composite parent) {
		super(
				page,
				parent,
				Section.DESCRIPTION,
				new String[]{
						PDEPlugin
								.getResourceString("ManifestEditor.OSGiSection.add"),
						PDEPlugin
								.getResourceString("ManifestEditor.OSGiSection.remove")});
		getSection().setText("Plug-in Activation (3.0 Plug-ins Only)");
		getSection().setDescription("In order to improve performance, specify the conditions under which the plug-in should be activated.");
	}
	public void initialize() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		model.addModelChangedListener(this);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		model.removeModelChangedListener(this);
		boldFont.dispose();
		super.dispose();
	}
	
	private void initializeFonts(){
		FontData[] fontData = getSection().getFont().getFontData();
		FontData data;
		if (fontData.length >0)
			data = fontData[0];
		else
			data = new FontData();
		data.setStyle(SWT.BOLD);
		boldFont = new Font(getSection().getDisplay(), fontData);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section,
	 *      org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		initializeFonts();
		Composite mainContainer = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 2;
		layout.makeColumnsEqualWidth = false;
		layout.numColumns = 3;
		mainContainer.setLayout(layout);
		mainContainer.setLayoutData(new GridData());
		
		/*
		 * create new manifest part
		 */
		Composite createManifestContainer = toolkit.createComposite(mainContainer);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 2;
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth= false;
		createManifestContainer.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		createManifestContainer.setLayoutData(gd);
		Label manifestLabel = toolkit.createLabel(createManifestContainer, "To take advantage of this feature, the plug-in must contain a manifest.mf file.");
		gd = new GridData();
		manifestLabel.setLayoutData(gd);
		Hyperlink manifestLink = toolkit.createHyperlink(createManifestContainer, "Create manifest file",SWT.NULL);
		manifestLink.addHyperlinkListener(new IHyperlinkListener(){
			public void linkActivated(HyperlinkEvent e) {
				/**
				 * TODO: hook code to create manifest.mf here
				 */
			}
			/* (non-Javadoc)
			 * @see org.eclipse.ui.forms.events.IHyperlinkListener#linkExited(org.eclipse.ui.forms.events.HyperlinkEvent)
			 */
			public void linkExited(HyperlinkEvent e) {
			}
			/* (non-Javadoc)
			 * @see org.eclipse.ui.forms.events.IHyperlinkListener#linkEntered(org.eclipse.ui.forms.events.HyperlinkEvent)
			 */
			public void linkEntered(HyperlinkEvent e) {
			}
		});
		manifestLink.setLayout(new GridLayout());
		gd = new GridData();
		manifestLink.setLayoutData(gd);
		/*
		 * Activation rule part
		 */
		Composite ruleContainer = toolkit.createComposite(mainContainer);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 2;
		layout.numColumns = 1;
		ruleContainer.setLayout(layout);
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		ruleContainer.setLayoutData(gd);
		Label activateLabel = toolkit.createLabel(ruleContainer, "Activation Rule");
		gd = new GridData(GridData.FILL_BOTH);
		activateLabel.setLayoutData(gd);
		activateLabel.setFont(boldFont);

		autoActivateButton = toolkit
				.createButton(
						ruleContainer,
						"Always activate this plug-in",
						SWT.RADIO);
		autoActivateButton
				.setLayoutData(new GridData(GridData.FILL_BOTH));
		autoActivateButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {

			}
		});
		/*
		 * auto-activate should be set to true by default with empty exceptions package list
		 */
		autoActivateButton.setSelection(true);
		nonAutoActivateButton = toolkit
				.createButton(
						ruleContainer,
						"Do not activate this plug-in",
						SWT.RADIO);
		nonAutoActivateButton.setLayoutData(new GridData(
				GridData.FILL_BOTH));
		nonAutoActivateButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {

			}
		});
				Label label = toolkit.createLabel(ruleContainer,"");
				label.setLayoutData(new GridData());
		
		/*
		 * Exceptions part
		 */
		Composite exceptionsContainer = toolkit.createComposite(mainContainer);
		layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 2;
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = false;
		exceptionsContainer.setLayout(layout);
		exceptionsContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		Label exceptionLabel = toolkit.createLabel(exceptionsContainer, "Exceptions to the Rule");
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		exceptionLabel.setLayoutData(gd);
		exceptionLabel.setFont(boldFont);
		Label exceptionPkgLabel = toolkit.createLabel(exceptionsContainer, "Ignore the activation rule when loaded classes belong to the following subset of packages:");
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		exceptionPkgLabel.setLayoutData(gd);
		EditableTablePart tablePart = getTablePart();
		IModel model = (IModel) getPage().getModel();
		tablePart.setEditable(model.isEditable());
		createViewerPartControl(exceptionsContainer, SWT.FULL_SELECTION, 2, toolkit);
		osgiTableViewer = tablePart.getTableViewer();
		osgiTableViewer.setContentProvider(new TableContentProvider());
		osgiTableViewer.setLabelProvider(new TableLabelProvider());
		toolkit.paintBordersFor(exceptionsContainer);
		section.setClient(mainContainer);
		initialize();
	}
	protected void buttonSelected(int index) {
		if (index == 0)
			handleAdd();
		else if (index == 1)
			handleRemove();
	}
	private void handleAdd() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		IProject project = model.getUnderlyingResource().getProject();
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				TableItem[] existingPackages = osgiTableViewer.getTable()
						.getItems();
				Vector existing = new Vector();
				for (int i = 0; i < existingPackages.length; i++) {
					existing.add(existingPackages[i].getText());
				}
				ILabelProvider labelProvider = new JavaElementLabelProvider();
				PackageSelectionDialog dialog = new PackageSelectionDialog(
						osgiTableViewer.getTable().getShell(), labelProvider,
						JavaCore.create(project), existing);
				if (dialog.open() == PackageSelectionDialog.OK) {
					Object[] elements = dialog.getResult();
					for (int i = 0; i < elements.length; i++) {
						/**
						 * TODO: add these packages to exceptions header
						 */
						osgiTableViewer.add((IPackageFragment) elements[i]);
					}
				}
				labelProvider.dispose();
			}
		} catch (CoreException e) {
		}
	}
	private void handleRemove() {
		IStructuredSelection ssel = (IStructuredSelection) osgiTableViewer
				.getSelection();
		Object[] items = ssel.toArray();
		for (int i = 0; i < items.length; i++) {
			osgiTableViewer.remove(items[i]);
		}
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.TableSection#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		Object item = selection.getFirstElement();
		getTablePart().setButtonEnabled(1,
				item != null && item instanceof IPackageFragment);
	}
}
