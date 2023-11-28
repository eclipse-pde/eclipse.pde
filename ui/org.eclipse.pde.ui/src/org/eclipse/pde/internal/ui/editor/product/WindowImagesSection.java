/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Peter Friese <peter.friese@gentleware.com> - bug 201956
 *     Lars Vogel <Lars.Vogel@gmail.com> - Bug 424113
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.core.resources.IFile;
import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;
import org.eclipse.jface.util.BidiUtils;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IWindowImages;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.EditorUtilities;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.validation.TextValidator;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.util.FileExtensionsFilter;
import org.eclipse.pde.internal.ui.util.FileValidator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.GridData;
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

	private TextValidator[] fWinImageEntryValidator;

	private static final int[][] F_ICON_DIMENSIONS = new int[][] { {16, 16}, {32, 32}, {48, 48}, {64, 64}, {128, 128}, {256, 256}};
	private static final String[] F_ICON_LABELS = new String[] {PDEUIMessages.WindowImagesSection_16, PDEUIMessages.WindowImagesSection_32, PDEUIMessages.WindowImagesSection_48, PDEUIMessages.WindowImagesSection_64, PDEUIMessages.WindowImagesSection_128, PDEUIMessages.WindowImagesSection_256};
	private final FormEntry[] fImages = new FormEntry[F_ICON_LABELS.length];

	public WindowImagesSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	@Override
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		section.setLayoutData(data);

		section.setText(PDEUIMessages.WindowImagesSection_title);
		section.setDescription(PDEUIMessages.WindowImagesSection_desc);

		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 3));
		client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		// Store all image entry validators
		fWinImageEntryValidator = new TextValidator[F_ICON_LABELS.length];
		for (int i = 0; i < fImages.length; i++) {
			final int index = i;
			fImages[index] = new FormEntry(client, toolkit, F_ICON_LABELS[index], PDEUIMessages.WindowImagesSection_browse, isEditable());
			BidiUtils.applyBidiProcessing(fImages[index].getText(), StructuredTextTypeHandlerFactory.FILE);
			fImages[index].setEditable(isEditable());
			// Create validator
			fWinImageEntryValidator[index] = new TextValidator(getManagedForm(), fImages[index].getText(), getProject(), true) {
				@Override
				protected boolean validateControl() {
					return validateWinImageEntry(index);
				}
			};
			fImages[index].setFormEntryListener(new FormEntryAdapter(this, actionBars) {
				@Override
				public void textValueChanged(FormEntry entry) {
					getWindowImages().setImagePath(entry.getValue(), index);
				}

				@Override
				public void browseButtonSelected(FormEntry entry) {
					handleBrowse(entry);
				}

				@Override
				public void linkActivated(HyperlinkEvent e) {
					EditorUtilities.openImage(fImages[index].getValue(), getProduct().getDefiningPluginId());
				}
			});
		}

		toolkit.paintBordersFor(client);
		section.setClient(client);
		// Register to be notified when the model changes
		getModel().addModelChangedListener(this);
	}

	@Override
	public void refresh() {
		IWindowImages images = getWindowImages();
		// Turn off auto message update until after values are set
		fWinImageEntryValidator[0].setRefresh(false);
		for (int i = 0; i < F_ICON_LABELS.length; i++) {
			fImages[i].setValue(images.getImagePath(i), true);
		}
		// Turn back on auto message update
		fWinImageEntryValidator[0].setRefresh(true);
		super.refresh();
	}

	private boolean validateWinImageEntry(int index) {
		return EditorUtilities.imageEntryHasExactSize(fWinImageEntryValidator[index], fImages[index], getProduct(), F_ICON_DIMENSIONS[index][0], F_ICON_DIMENSIONS[index][1]);
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
		return (IProductModel) getPage().getPDEEditor().getAggregateModel();
	}

	@Override
	public void commit(boolean onSave) {
		for (int i = 0; i < F_ICON_LABELS.length; i++) {
			fImages[i].commit();
		}
		super.commit(onSave);
	}

	@Override
	public void cancelEdit() {
		for (int i = 0; i < F_ICON_LABELS.length; i++) {
			fImages[i].cancelEdit();
		}
		super.cancelEdit();
	}

	private void handleBrowse(FormEntry entry) {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getSection().getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());

		dialog.setValidator(new FileValidator());
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEUIMessages.WindowImagesSection_dialogTitle);
		dialog.setMessage(PDEUIMessages.WindowImagesSection_dialogMessage);
		FileExtensionsFilter filter = new FileExtensionsFilter();
		filter.addFileExtension("gif"); //$NON-NLS-1$
		filter.addFileExtension("png"); //$NON-NLS-1$
		dialog.addFilter(filter);
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());

		if (dialog.open() == Window.OK) {
			IFile file = (IFile) dialog.getFirstResult();
			entry.setValue(file.getFullPath().toString());
		}
	}

	@Override
	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		// No need to call super, handling world changed event here
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged(e);
		}
	}

	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		refresh();
	}

	@Override
	public void dispose() {
		IProductModel model = getModel();
		if (model != null) {
			model.removeModelChangedListener(this);
		}
		super.dispose();
	}
}
