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

import org.eclipse.core.resources.IFile;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IWindowImages;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.AbstractFormValidator;
import org.eclipse.pde.internal.ui.editor.EditorUtilities;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.util.FileExtensionFilter;
import org.eclipse.pde.internal.ui.util.FileValidator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;


public class WindowImagesSection extends PDESection {

	private static final String[] F_ICON_LABELS = new String[] {
		PDEUIMessages.WindowImagesSection_16,
		PDEUIMessages.WindowImagesSection_32,
		PDEUIMessages.WindowImagesSection_48,
		PDEUIMessages.WindowImagesSection_64,
		PDEUIMessages.WindowImagesSection_128
	};
	private FormEntry[] fImages = new FormEntry[F_ICON_LABELS.length];
	
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
		
		for (int i = 0; i < fImages.length; i++) {
			final int index = i;
			fImages[index] = new FormEntry(client, 
					toolkit, F_ICON_LABELS[index], 
					PDEUIMessages.WindowImagesSection_browse, 
					isEditable());
			fImages[index].setFormEntryListener(new FormEntryAdapter(this, actionBars) {
				public void textValueChanged(FormEntry entry) {
					getWindowImages().setImagePath(entry.getValue(), index);
				}
				public void browseButtonSelected(FormEntry entry) {
					handleBrowse(entry);
				}
				public void linkActivated(HyperlinkEvent e) {
					EditorUtilities.openImage(fImages[index].getValue(), getProduct().getDefiningPluginId());
				}
			});
			fImages[index].setEditable(isEditable());
			fImages[index].setValidator(new AbstractFormValidator(this) {
				public boolean inputValidates() {
					return EditorUtilities.isValidImage(
							fImages[index],	getProduct(),
							EditorUtilities.F_ICON_DIMENSIONS[index],
							EditorUtilities.F_EXACTIMAGE);
				}
			});
		}
		
		toolkit.paintBordersFor(client);
		section.setClient(client);
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING));
	}
	
	public void refresh() {
		IWindowImages images = getWindowImages();
		for (int i = 0; i < F_ICON_LABELS.length; i++) {
			fImages[i].setValue(images.getImagePath(i), true);
		}
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
		for (int i = 0; i < F_ICON_LABELS.length; i++) {
			fImages[i].commit();
		}
		super.commit(onSave);
	}
	
	public void cancelEdit() {
		for (int i = 0; i < F_ICON_LABELS.length; i++) {
			fImages[i].cancelEdit();
		}
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

	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}
}
