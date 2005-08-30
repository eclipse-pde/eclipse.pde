/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.ide.*;
import org.eclipse.ui.model.*;


public class WindowImagesSection extends PDESection {

	private FormEntry fImage16;
	private FormEntry fImage32;
	private FormEntry fImage48;
	private FormEntry fImage64;
	private FormEntry fImage128;

	public WindowImagesSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.WindowImagesSection_title); 
		section.setDescription(PDEUIMessages.WindowImagesSection_desc); 

		Composite client = toolkit.createComposite(section);
		client.setLayout(new GridLayout(3, false));
		
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fImage16 = new FormEntry(client, toolkit, PDEUIMessages.WindowImagesSection_16, PDEUIMessages.WindowImagesSection_browse, isEditable()); // 
		fImage16.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getWindowImages().setImagePath(entry.getValue(), 0);
			}
			public void browseButtonSelected(FormEntry entry) {
				handleBrowse(entry);
			}
			public void linkActivated(HyperlinkEvent e) {
				openImage(fImage16.getValue());
			}
		});
		fImage16.setEditable(isEditable());
		
		fImage32 = new FormEntry(client, toolkit, PDEUIMessages.WindowImagesSection_32, PDEUIMessages.WindowImagesSection_browse, isEditable()); // 
		fImage32.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getWindowImages().setImagePath(entry.getValue(), 1);
			}
			public void browseButtonSelected(FormEntry entry) {
				handleBrowse(entry);
			}
			public void linkActivated(HyperlinkEvent e) {
				openImage(fImage32.getValue());
			}
		});
		fImage32.setEditable(isEditable());
		
		fImage48 = new FormEntry(client, toolkit, PDEUIMessages.WindowImagesSection_48, PDEUIMessages.WindowImagesSection_browse, isEditable()); // 
		fImage48.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getWindowImages().setImagePath(entry.getValue(), 2);
			}
			public void browseButtonSelected(FormEntry entry) {
				handleBrowse(entry);
			}
			public void linkActivated(HyperlinkEvent e) {
				openImage(fImage48.getValue());
			}
		});
		fImage48.setEditable(isEditable());
		
		fImage64 = new FormEntry(client, toolkit, PDEUIMessages.WindowImagesSection_64, PDEUIMessages.WindowImagesSection_browse, isEditable()); // 
		fImage64.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getWindowImages().setImagePath(entry.getValue(), 3);
			}
			public void browseButtonSelected(FormEntry entry) {
				handleBrowse(entry);
			}
			public void linkActivated(HyperlinkEvent e) {
				openImage(fImage64.getValue());
			}
		});
		fImage64.setEditable(isEditable());
		
		fImage128 = new FormEntry(client, toolkit, PDEUIMessages.WindowImagesSection_128, PDEUIMessages.WindowImagesSection_browse, isEditable()); // 
		fImage128.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getWindowImages().setImagePath(entry.getValue(), 4);
			}
			public void browseButtonSelected(FormEntry entry) {
				handleBrowse(entry);
			}
			public void linkActivated(HyperlinkEvent e) {
				openImage(fImage128.getValue());
			}
		});
		fImage128.setEditable(isEditable());
		
		toolkit.paintBordersFor(client);
		section.setClient(client);
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING));
	}
	
	public void refresh() {
		IWindowImages images = getWindowImages();
		fImage16.setValue(images.getImagePath(0), true);
		fImage32.setValue(images.getImagePath(1), true);
		fImage48.setValue(images.getImagePath(2), true);
		fImage64.setValue(images.getImagePath(3), true);
		fImage128.setValue(images.getImagePath(4), true);
		super.refresh();
	}

	private IWindowImages getWindowImages() {
		IWindowImages images = getProduct().getWindowImages();
		if (images == null) {
			images = getModel().getFactory().createWindowImages();
			getProduct().setWindowImages(images);
		}
		return images;
	}
	
	private IProduct getProduct() {
		return getModel().getProduct();
	}
	
	private IProductModel getModel() {
		return (IProductModel)getPage().getPDEEditor().getAggregateModel();
	}

	public void commit(boolean onSave) {
		fImage16.commit();
		fImage32.commit();
		fImage48.commit();
		fImage64.commit();
		fImage128.commit();
		super.commit(onSave);
	}
	
	public void cancelEdit() {
		fImage16.cancelEdit();
		fImage32.cancelEdit();
		fImage48.cancelEdit();
		fImage64.cancelEdit();
		fImage128.cancelEdit();
		super.cancelEdit();
	}
	
	private void handleBrowse(FormEntry entry) {
		ElementTreeSelectionDialog dialog =
			new ElementTreeSelectionDialog(
				getSection().getShell(),
				new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());
				
		dialog.setValidator(new FileValidator());
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEUIMessages.WindowImagesSection_dialogTitle);  
		dialog.setMessage(PDEUIMessages.WindowImagesSection_dialogMessage); 
		dialog.addFilter(new FileExtensionFilter("gif")); //$NON-NLS-1$
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());

		if (dialog.open() == ElementTreeSelectionDialog.OK) {
			IFile file = (IFile)dialog.getFirstResult();
			entry.setValue(file.getFullPath().toString());
		}
	}
	
	private void openImage(String value) {
		IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
		IPath path = new Path(value);
		if(path.isEmpty()){
			MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.WindowImagesSection_open, PDEUIMessages.WindowImagesSection_emptyPath); // 
			return;
		}
		if (!path.isAbsolute()) {
			path = getFullPath(path);
		}
		IResource resource = root.findMember(path);
		try {
			if (resource != null && resource instanceof IFile)
				IDE.openEditor(PDEPlugin.getActivePage(), (IFile)resource, true);
			else
				MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.WindowImagesSection_open, PDEUIMessages.WindowImagesSection_warning); // 
		} catch (PartInitException e) {
		}			
	}
	
	private IPath getFullPath(IPath path) {
		String productId = getProduct().getId();
		int dot = productId.lastIndexOf('.');
		String pluginId = (dot != -1) ? productId.substring(0, dot) : ""; //$NON-NLS-1$
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(pluginId);
		if (model != null && model.getUnderlyingResource() != null) {
			IPath newPath = new Path(model.getInstallLocation()).append(path);
			IContainer container = PDEPlugin.getWorkspace().getRoot().getContainerForLocation(newPath);
			if (container != null) {
				return container.getFullPath();
			}
		}
		return path;
	}

	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}

}
