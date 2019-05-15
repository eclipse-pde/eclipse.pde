/*******************************************************************************
 *  Copyright (c) 2019 Julian Honnen
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Julian Honnen <julian.honnen@vector.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.category;

import java.util.function.Function;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ifeature.IVersionable;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IPartSelectionListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

abstract class IUDetailsSection<T extends IVersionable> extends PDESection implements IPartSelectionListener {

	private final Function<Object, T> fSelectionExtractor;

	private T fCurrentItem;

	private FormEntry fIdText;
	private FormEntry fVersionText;

	public IUDetailsSection(PDEFormPage page, Composite parent, String title, String desc,
			Function<Object, T> selectionExtractor) {
		super(page, parent, Section.DESCRIPTION);
		fSelectionExtractor = selectionExtractor;
		getSection().setText(title);
		getSection().setDescription(desc);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	@Override
	public void cancelEdit() {
		fIdText.cancelEdit();
		fVersionText.cancelEdit();
		super.cancelEdit();
	}

	@Override
	public boolean canPaste(Clipboard clipboard) {
		TransferData[] types = clipboard.getAvailableTypes();
		Transfer[] transfers = new Transfer[] { TextTransfer.getInstance(), RTFTransfer.getInstance() };
		for (TransferData type : types) {
			for (Transfer transfer : transfers) {
				if (transfer.isSupportedType(type)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void commit(boolean onSave) {
		fIdText.commit();
		fVersionText.commit();
		super.commit(onSave);
	}

	@Override
	public final void createClient(Section section, FormToolkit toolkit) {
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		Composite container = toolkit.createComposite(section);
		container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		GridData data = new GridData(GridData.FILL_BOTH);
		section.setLayoutData(data);

		fIdText = new FormEntry(container, toolkit, PDEUIMessages.SiteContentDetails_id, null, false);
		fIdText.setFormEntryListener(new FormEntryAdapter(this) {
			@Override
			public void textValueChanged(FormEntry text) {
				if (fCurrentItem != null) {
					try {
						String value = text.getValue();
						applyId(value.isEmpty() ? null : value);
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
				}
			}
		});
		limitTextWidth(fIdText);
		fIdText.setEditable(isEditable());

		fVersionText = new FormEntry(container, toolkit, PDEUIMessages.SiteContentDetails_version, null, false);
		fVersionText.setFormEntryListener(new FormEntryAdapter(this) {

			@Override
			public void textValueChanged(FormEntry text) {
				if (fCurrentItem != null) {
					try {
						String value = text.getValue();
						applyVersion(value.isEmpty() ? null : value);
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
				}
			}

		});
		limitTextWidth(fVersionText);
		fVersionText.setEditable(isEditable());

		onCreateClient(container, toolkit);

		toolkit.paintBordersFor(container);
		section.setClient(container);

		ISiteModel model = (ISiteModel) getPage().getModel();
		if (model != null) {
			model.addModelChangedListener(this);
		}
	}

	protected void onCreateClient(Composite container, FormToolkit toolkit) {
	}

	protected void applyId(String value) throws CoreException {
		fCurrentItem.setId(value);
	}

	protected void applyVersion(String value) throws CoreException {
		fCurrentItem.setVersion(value);
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		markStale();
	}

	@Override
	public final void refresh() {
		if (fCurrentItem == null) {
			clearFields();
		} else {
			fillControls(fCurrentItem);
		}

		super.refresh();
	}

	protected void fillControls(T currentItem) {
		fIdText.setValue(currentItem.getId(), true);
		fVersionText.setValue(currentItem.getVersion(), true);
	}

	@Override
	public void selectionChanged(IFormPart part, ISelection selection) {
		T item = null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structured = (IStructuredSelection) selection;
			item = fSelectionExtractor.apply(structured.getFirstElement());
		}
		fCurrentItem = item;
		refresh();
	}

	@Override
	public void setFocus() {
		if (fIdText != null) {
			fIdText.getText().setFocus();
		}
	}

	protected void clearFields() {
		fIdText.setValue(null, true);
		fVersionText.setValue(null, true);
	}

	@Override
	public void dispose() {
		ISiteModel model = (ISiteModel) getPage().getModel();
		if (model != null) {
			model.removeModelChangedListener(this);
		}
		super.dispose();
	}

	protected void limitTextWidth(FormEntry entry) {
		GridData gd = (GridData) entry.getText().getLayoutData();
		gd.widthHint = 30;
	}

	protected T getCurrentItem() {
		return fCurrentItem;
	}
}
