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
package org.eclipse.pde.internal.ui.neweditor.runtime;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.neweditor.PDESection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Button;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.IPartSelectionListener;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.forms.widgets.Section;

public class LibraryTypeSection extends PDESection implements IPartSelectionListener {
	
	private Button codeButton;
	private Button resourcesButton;
	private IPluginLibrary currentLibrary;
	 

	public LibraryTypeSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION);
		getSection().setText(PDEPlugin.getResourceString("ManifestEditor.LibraryTypeSection.title"));
		getSection().setDescription(PDEPlugin.getResourceString("ManifestEditor.LibraryTypeSection.desc"));
		createClient(getSection(), formPage.getManagedForm().getToolkit());
	}

	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		container.setLayout(layout);
		
		codeButton = toolkit.createButton(container, PDEPlugin.getResourceString("ManifestEditor.LibraryTypeSection.code"), SWT.RADIO);
		codeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					if (codeButton.getSelection())
						currentLibrary.setType(null);	
					else
						currentLibrary.setType(IPluginLibrary.RESOURCE);
				} catch (CoreException ex) {
				}
			}
		});
		resourcesButton = toolkit.createButton(container, PDEPlugin.getResourceString("ManifestEditor.LibraryTypeSection.resources"), SWT.RADIO);
		update(null);
		initialize();
		section.setClient(container);
	}
	
	private boolean isReadOnly() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IEditable) {
			return !((IEditable)model).isEditable();
		}
		return true;
	}
	
	public void initialize() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		//setReadOnly(!model.isEditable());
		model.addModelChangedListener(this);
	}
	
	public void selectionChanged(IFormPart source, ISelection selection) {
		IStructuredSelection ssel = (IStructuredSelection)selection;
		IPluginLibrary library = (IPluginLibrary)ssel.getFirstElement();
		update(library);		
	}
	
	public void update(IPluginLibrary library) {
		if (library == null) {
			codeButton.setEnabled(false);
			codeButton.setSelection(false);
			resourcesButton.setEnabled(false);
			resourcesButton.setSelection(false);
		} else {
			String type = library.getType();
			if (type != null && type.equals(IPluginLibrary.RESOURCE)) {
				codeButton.setSelection(false);
				resourcesButton.setSelection(true);
			} else {
				codeButton.setSelection(true);
				resourcesButton.setSelection(false);
			}

			codeButton.setEnabled(!isReadOnly());
			resourcesButton.setEnabled(!isReadOnly());
		}
		this.currentLibrary = library;
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			Object object = e.getChangedObjects()[0];
			if (object instanceof IPluginLibrary)
				update((IPluginLibrary)object);
		}
	}
	
	public void commit(boolean onSave) {
		try {
			if (currentLibrary != null)
				if (resourcesButton.getSelection())
					currentLibrary.setType(IPluginLibrary.RESOURCE);
				else
					currentLibrary.setType(null);
		} catch (CoreException e) {
		}
		super.commit(onSave);
	}
}
