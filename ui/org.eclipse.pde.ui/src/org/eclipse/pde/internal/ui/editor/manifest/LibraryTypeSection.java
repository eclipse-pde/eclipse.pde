package org.eclipse.pde.internal.ui.editor.manifest;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDEFormSection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.update.ui.forms.internal.FormSection;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;


public class LibraryTypeSection extends PDEFormSection {
	
	private Button codeButton;
	private Button resourcesButton;
	private IPluginLibrary currentLibrary;
	 

	public LibraryTypeSection(PDEFormPage formPage) {
		super(formPage);
		setHeaderText(PDEPlugin.getResourceString("ManifestEditor.LibraryTypeSection.title"));
		setDescription(PDEPlugin.getResourceString("ManifestEditor.LibraryTypeSection.desc"));
	}

	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		container.setLayout(layout);
		
		codeButton = factory.createButton(container, PDEPlugin.getResourceString("ManifestEditor.LibraryTypeSection.code"), SWT.RADIO);
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
		resourcesButton = factory.createButton(container, PDEPlugin.getResourceString("ManifestEditor.LibraryTypeSection.resources"), SWT.RADIO);
		update(null);
		return container;
	}
	
	public void initialize(Object input) {
		IPluginModelBase model = (IPluginModelBase) input;
		setReadOnly(!model.isEditable());
		model.addModelChangedListener(this);
	}
	
	public void sectionChanged(FormSection source, int changeType, Object changeObject) {
		update((IPluginLibrary)changeObject);		
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
	
	public void commitChanges(boolean onSave) {
		try {
			if (currentLibrary != null)
				if (resourcesButton.getSelection())
					currentLibrary.setType(IPluginLibrary.RESOURCE);
				else
					currentLibrary.setType(null);
			setDirty(false);
		} catch (CoreException e) {
		}
	}


}
