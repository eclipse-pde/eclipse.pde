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
package org.eclipse.pde.internal.ui.wizards.extension;

import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.search.ShowDescriptionAction;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;

public class PointSelectionPage
	extends WizardPage
	implements ISelectionChangedListener {
	private TableViewer fPointListViewer;
	private IPluginBase fPluginBase;
	private Text fPointIdText;
	private Text fPointNameText;
	private Label fDescription;
	private Button fDescriptionButton;
	private Button fFilterCheck;
	private IPluginExtensionPoint fCurrentPoint;
	private HashSet fAvailableImports;

	private IPluginExtension fNewExtension;
	private ShowDescriptionAction fShowDescriptionAction;
	
	class PointFilter extends ViewerFilter {
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (!fFilterCheck.getSelection())
				return true;
			IPluginExtensionPoint point = (IPluginExtensionPoint) element;
			return fAvailableImports.contains(point.getPluginBase().getId());
		}
	}

	class ContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			HashSet extPoints = new HashSet();
			PluginModelManager manager = (PluginModelManager)parent;
			IPluginModelBase[] plugins = manager.getPlugins();
			for (int i = 0; i < plugins.length; i++) {
				IPluginExtensionPoint[] points = plugins[i].getPluginBase().getExtensionPoints();
				for (int j = 0; j < points.length; j++)
					extPoints.add(points[j]);
			}
			return extPoints.toArray();
		}
	}

	class PointLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getText(Object obj) {
			return getColumnText(obj, 0);
		}
		public String getColumnText(Object obj, int index) {
			PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
			if (provider.isFullNameModeEnabled())
				return provider.getText((IPluginExtensionPoint) obj);
			return ((IPluginExtensionPoint) obj).getFullId();
		}
		
		public Image getImage(Object obj) {
			return getColumnImage(obj, 0);
		}
		
		public Image getColumnImage(Object obj, int index) {
			IPluginExtensionPoint exp = (IPluginExtensionPoint) obj;
			int flag =
				fAvailableImports.contains(exp.getPluginBase().getId())
					? 0
					: SharedLabelProvider.F_WARNING;
			return PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_EXT_POINT_OBJ,
				flag);
		}
	}

	public PointSelectionPage(IPluginBase model) {
		super("pointSelectionPage");
		this.fPluginBase = model;
		fAvailableImports = PluginSelectionDialog.getExistingImports(model);
		setTitle(PDEPlugin.getResourceString("NewExtensionWizard.PointSelectionPage.title"));
		setDescription(PDEPlugin.getResourceString("NewExtensionWizard.PointSelectionPage.desc"));
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}
	
	public void createControl(Composite parent) {
		// top level group
		Composite outerContainer = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		outerContainer.setLayout(layout);
		outerContainer.setLayoutData(
			new GridData(
				GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

		fFilterCheck = new Button(outerContainer, SWT.CHECK);
		fFilterCheck.setText(PDEPlugin.getResourceString("NewExtensionWizard.PointSelectionPage.filterCheck"));
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		fFilterCheck.setLayoutData(gd);
		fFilterCheck.setSelection(true);
		fFilterCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fPointListViewer.refresh();
			}
		});

		fPointListViewer =
			new TableViewer(
				outerContainer,
				SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		fPointListViewer.setContentProvider(new ContentProvider());
		fPointListViewer.setLabelProvider(new PointLabelProvider());
		fPointListViewer.addSelectionChangedListener(this);
		fPointListViewer.setSorter(ListUtil.NAME_SORTER);

		gd =
			new GridData(
				GridData.FILL_BOTH
					| GridData.GRAB_HORIZONTAL
					| GridData.GRAB_VERTICAL);
		gd.heightHint = 300;
		gd.horizontalSpan = 2;
		fPointListViewer.getTable().setLayoutData(gd);

		fDescriptionButton = new Button(outerContainer, SWT.PUSH);
		fDescriptionButton.setText(PDEPlugin.getResourceString("NewExtensionWizard.PointSelectionPage.descButton"));
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		fDescriptionButton.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(fDescriptionButton);
		fDescriptionButton.setEnabled(false);
		fDescriptionButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doShowDescription();
			}
		});

		Label label = new Label(outerContainer, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("NewExtensionWizard.PointSelectionPage.pointId"));
		fPointIdText = new Text(outerContainer, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		fPointIdText.setLayoutData(gd);
		new Label(outerContainer, SWT.NULL);

		label = new Label(outerContainer, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("NewExtensionWizard.PointSelectionPage.pointName"));
		fPointNameText = new Text(outerContainer, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		fPointNameText.setLayoutData(gd);
		new Label(outerContainer, SWT.NULL);

		createDescriptionIn(outerContainer);
		initialize();
		setControl(outerContainer);
		Dialog.applyDialogFont(outerContainer);
		WorkbenchHelp.setHelp(
			outerContainer,
			IHelpContextIds.ADD_EXTENSIONS_SCHEMA_BASED);
	}

	public boolean canFinish() {
		if (fPointListViewer != null) {
			ISelection selection = fPointListViewer.getSelection();
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ssel = (IStructuredSelection) selection;
				if (ssel.isEmpty() == false)
					return true;
			}
		}
		return false;
	}

	public void createDescriptionIn(Composite composite) {
		fDescription = new Label(composite, SWT.NONE);
		GridData gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		gd.horizontalSpan = 2;
		fDescription.setLayoutData(gd);
	}
	
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}
	
	public boolean finish() {
		String id = fPointIdText.getText();
		if (id.length() == 0)
			id = null;

		String name = fPointNameText.getText();
		if (name.length() == 0)
			name = null;

		String point = fCurrentPoint.getFullId();

		try {
			IPluginExtension extension =
				fPluginBase.getModel().getFactory().createExtension();
			extension.setName(name);
			extension.setPoint(point);
			if (id != null)
				extension.setId(id);
			fPluginBase.add(extension);
			
			String pluginID = fCurrentPoint.getPluginBase().getId();
			if (!fAvailableImports.contains(pluginID)) {
				IPluginModelBase modelBase = ((IPluginModelBase) fPluginBase.getModel());
				IPluginImport importNode = modelBase.getPluginFactory().createImport();
				importNode.setId(pluginID);
				fPluginBase.add(importNode);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return true;
	}

	private void doShowDescription() {
		if (fShowDescriptionAction == null)
			fShowDescriptionAction = new ShowDescriptionAction(fCurrentPoint);
		else
			fShowDescriptionAction.setExtensionPoint(fCurrentPoint);
		BusyIndicator.showWhile(fDescriptionButton.getDisplay(), new Runnable() {
			public void run() {
				fShowDescriptionAction.run();
			}
		});
	}

	public IPluginExtension getNewExtension() {
		return fNewExtension;
	}
	
	protected void initialize() {
		fPointListViewer.addFilter(new PointFilter());
		fPointListViewer.setInput(PDECore.getDefault().getModelManager());
		fPointListViewer.getTable().setFocus();
	}
	
	public void selectionChanged(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		setDescription("");
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel != null && !ssel.isEmpty()) {
				fCurrentPoint = (IPluginExtensionPoint) ssel.getFirstElement();
				if (fAvailableImports.contains(fCurrentPoint.getPluginBase().getId()))
					setMessage(null);
				else
					setMessage(
						PDEPlugin.getResourceString("NewExtensionWizard.PointSelectionPage.message"),
						INFORMATION);
				setDescription(fCurrentPoint.getFullId());
				fDescriptionButton.setEnabled(true);
			}
		}
		getContainer().updateButtons();
	}
	
	public void setDescriptionText(String text) {
		fDescription.setText(text);
	}
}
