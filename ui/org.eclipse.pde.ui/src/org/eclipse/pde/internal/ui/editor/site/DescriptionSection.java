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
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.core.site.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormSection;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.model.*;
import org.eclipse.update.ui.forms.internal.*;

public class DescriptionSection extends PDEFormSection {
	private FormEntry url;
	private FormEntry text;

	private boolean updateNeeded;
	private FormEntry featureDest;
	private FormEntry pluginDest;

	class ContentProvider extends WorkbenchContentProvider {
		public boolean hasChildren(Object element) {
			Object[] children = getChildren(element);
			for (int i = 0; i < children.length; i++) {
				if (children[i] instanceof IFolder) {
					return true;
				}
			}
			return false;
		}
		
	}

	public DescriptionSection(SitePage page) {
		super(page);
		setHeaderText(PDEPlugin.getResourceString("SiteEditor.DescriptionSection.header")); //$NON-NLS-1$
		setDescription(PDEPlugin.getResourceString("SiteEditor.DescriptionSection.desc")); //$NON-NLS-1$
	}
	
	public void commitChanges(boolean onSave) {
		url.commit();
		text.commit();
		pluginDest.commit();
		featureDest.commit();
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISiteBuildModel buildModel = model.getBuildModel();
		if (onSave
			&& buildModel instanceof WorkspaceSiteBuildModel
			&& ((WorkspaceSiteBuildModel) buildModel).isDirty()) {
			((WorkspaceSiteBuildModel) buildModel).save();
		}
	}

	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = true;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 6;
		container.setLayout(layout);

		createLeftContainer(container, factory);
		createRightContainer(container, factory);
		factory.paintBordersFor(container);
		return container;
	}
	
	private void createRightContainer(Composite parent, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		container.setLayoutData(
			new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING));

		Text area =
			createText(
				container,
				PDEPlugin.getResourceString("SiteEditor.DescriptionSection.descLabel"), //$NON-NLS-1$
				factory,
				1,
				FormWidgetFactory.BORDER_STYLE | SWT.WRAP | SWT.MULTI);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 100;
		area.setLayoutData(gd);
		
		text = new FormEntry(area);
		text.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				setDescriptionText(text.getValue());
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		factory.paintBordersFor(container);
	}
	
	private void createLeftContainer(Composite parent, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		url =
			new FormEntry(
				createText(
					container,
					PDEPlugin.getResourceString("SiteEditor.DescriptionSection.urlLabel"), //$NON-NLS-1$
					factory,
					2));
		url.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				setDescriptionURL(text.getValue());
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		
		pluginDest =
			new FormEntry(
				createText(
					container,
					PDEPlugin.getResourceString(PDEPlugin.getResourceString("SiteEditor.DescriptionSection.pluginLocation")), //$NON-NLS-1$
					factory,
					1));
		pluginDest.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				setPluginDestination(text.getValue());
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		Button browse = factory.createButton(container, PDEPlugin.getResourceString("SiteEditor.DescriptionSection.browse"), SWT.PUSH); //$NON-NLS-1$
		browse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IFolder folder = handleFindContainer();
				if (folder != null)
					pluginDest.setValue(folder.getProjectRelativePath().addTrailingSeparator().toString());
			}
		});
			
		featureDest =
			new FormEntry(
				createText(
					container,
					PDEPlugin.getResourceString(PDEPlugin.getResourceString("SiteEditor.DescriptionSection.featureLocation")), //$NON-NLS-1$
					factory,
					1));
		featureDest.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				setFeatureDestination(text.getValue());
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		
		browse = factory.createButton(container, PDEPlugin.getResourceString("SiteEditor.DescriptionSection.browse"), SWT.PUSH); //$NON-NLS-1$
		browse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IFolder folder = handleFindContainer();
				if (folder != null)
					featureDest.setValue(folder.getProjectRelativePath().addTrailingSeparator().toString());
			}
		});	
		
		factory.paintBordersFor(container);	
	}

	private IFolder handleFindContainer() {
		FolderSelectionDialog dialog =
			new FolderSelectionDialog(
				PDEPlugin.getActiveWorkbenchShell(),
				new WorkbenchLabelProvider(),
				new ContentProvider() {
		});
		dialog.setInput(PDEPlugin.getWorkspace());
		dialog.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IProject) {
					IResource resource = ((ISiteModel)getFormPage().getModel()).getUnderlyingResource();
					if (resource != null)
					return ((IProject)element).equals(resource.getProject());
				}
				return element instanceof IFolder;
			}			
		});
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEPlugin.getResourceString("SiteEditor.DescriptionSection.folderSelection")); //$NON-NLS-1$
		dialog.setValidator(new ISelectionStatusValidator() {
			public IStatus validate(Object[] selection) {
				if (selection != null
					&& selection.length > 0
					&& selection[0] instanceof IFolder)
					return new Status(
						IStatus.OK,
						PDEPlugin.getPluginId(),
						IStatus.OK,
						"", //$NON-NLS-1$
						null);
				return new Status(
					IStatus.ERROR,
					PDEPlugin.getPluginId(),
					IStatus.ERROR,
					"", //$NON-NLS-1$
					null);
			}
		});
		if (dialog.open() == FolderSelectionDialog.OK) {
			return (IFolder) dialog.getFirstResult();
		}
		return null;
	}
	private void setPluginDestination(String text) {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISiteBuildModel buildModel = model.getBuildModel();
		if (buildModel == null)
			return;
		ISiteBuild siteBuild = buildModel.getSiteBuild();
		try {
			siteBuild.setPluginLocation(new Path(text));
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void setFeatureDestination(String text) {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISiteBuildModel buildModel = model.getBuildModel();
		if (buildModel == null)
			return;
		ISiteBuild siteBuild = buildModel.getSiteBuild();
		try {
			siteBuild.setFeatureLocation(new Path(text));
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	private void setDescriptionURL(String text) {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISite site = model.getSite();
		ISiteDescription description = site.getDescription();
		boolean defined = false;
		if (description == null) {
			description = model.getFactory().createDescription(null);
			defined = true;
		}
		try {
			description.setURL(text);
			if (defined) {
				site.setDescription(description);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void setDescriptionText(String text) {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISite site = model.getSite();
		ISiteDescription description = site.getDescription();
		boolean defined = false;
		if (description == null) {
			description = model.getFactory().createDescription(null);
			defined = true;
		}
		try {
			description.setText(text);
			if (defined) {
				site.setDescription(description);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void forceDirty() {
		setDirty(true);
		ISiteModel model = (ISiteModel) getFormPage().getModel();

		if (model instanceof IEditable) {
			((IEditable) model).setDirty(true);
		}
		getFormPage().getEditor().fireSaveNeeded();
	}

	public void dispose() {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		model.removeModelChangedListener(this);
		model.getBuildModel().removeModelChangedListener(this);
		super.dispose();
	}

	public void initialize(Object input) {
		ISiteModel model = (ISiteModel) input;
		update(input);

		if (model.isEditable() == false) {
			url.getControl().setEditable(false);
			text.getControl().setEditable(false);
		}
		
		ISiteBuildModel buildModel = model.getBuildModel();
		if (!buildModel.isEditable()) {
			featureDest.getControl().setEditable(false);
			pluginDest.getControl().setEditable(false);
		}
		
		model.addModelChangedListener(this);
		buildModel.addModelChangedListener(this);
	}

	public void modelChanged(IModelChangedEvent e) {
		updateNeeded = true;
		update();
	}
	
	public void setFocus() {
		if (url != null)
			url.getControl().setFocus();
	}
	
	private void setIfDefined(FormEntry formText, String value) {
		if (value != null) {
			formText.setValue(value, true);
		}
	}
	
	public void update() {
		if (updateNeeded) {
			update(getFormPage().getModel());
			
		}
	}
	
	public void update(Object input) {
		ISiteModel model = (ISiteModel) input;
		ISite site = model.getSite();
		setIfDefined(
			url,
			site.getDescription() != null
				? site.getDescription().getURL()
				: null);
		setIfDefined(
			text,
			site.getDescription() != null
				? site.getDescription().getText()
				: null);
		ISiteBuildModel buildModel = model.getBuildModel();
		if (buildModel != null) {
			ISiteBuild siteBuild = buildModel.getSiteBuild();
			setIfDefined(
				featureDest,
				siteBuild.getFeatureLocation() != null
					? siteBuild.getFeatureLocation().toString()
					: null);
			setIfDefined(
				pluginDest,
				siteBuild.getPluginLocation() != null
					? siteBuild.getPluginLocation().toString()
					: null);
		}
		updateNeeded = false;
	}
	/**
	 * @see org.eclipse.update.ui.forms.internal.FormSection#canPaste(Clipboard)
	 */
	public boolean canPaste(Clipboard clipboard) {
		TransferData[] types = clipboard.getAvailableTypes();
		Transfer[] transfers =
			new Transfer[] { TextTransfer.getInstance(), RTFTransfer.getInstance()};
		for (int i = 0; i < types.length; i++) {
			for (int j = 0; j < transfers.length; j++) {
				if (transfers[j].isSupportedType(types[i]))
					return true;
			}
		}
		return false;
	}

}
