package org.eclipse.pde.internal.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.feature.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.*;
import org.eclipse.core.resources.*;
import org.eclipse.swt.events.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.pde.model.*;
import org.eclipse.swt.*;
import org.eclipse.ui.*;
import org.eclipse.pde.internal.PDEPlugin;

public class DescriptionSection extends PDEFormSection {
	public static final String SECTION_TITLE = "FeatureEditor.DescriptionSection.title";
	private boolean updateNeeded;
	private boolean ignoreChange=false;
	private FormEntry descriptionText;

public DescriptionSection(FeatureFormPage page) {
	super(page);
	setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
}
public void commitChanges(boolean onSave) {
	descriptionText.commit();
}
public Composite createClient(Composite parent, FormWidgetFactory factory) {
	Composite container = factory.createComposite(parent);
	GridLayout layout = new GridLayout();
	layout.verticalSpacing = 9;
	layout.horizontalSpacing = 6;
	layout.marginWidth = 2;
	layout.marginHeight = 5;
	container.setLayout(layout);

	final IFeatureModel model = (IFeatureModel) getFormPage().getModel();
	final IFeature component = model.getFeature();

	Text descControl =
		new Text(container, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL);
	GridData gd = new GridData(GridData.FILL_BOTH);
	gd.heightHint = 48;
	descControl.setLayoutData(gd);
	descriptionText = new FormEntry(descControl);
	descControl.addModifyListener(new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			if (ignoreChange) return;
			setDirty(true);
			((IEditable) model).setDirty(true);
			getFormPage().getEditor().fireSaveNeeded();
		}
	});
	/*
	descriptionText.addFormTextListener(new IFormTextListener() {
		public void textValueChanged(FormEntry text) {
			try {
				component.setDescription(text.getValue());
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
		public void textDirty(FormEntry text) {
		}
	});
	*/
	if (SWT.getPlatform().equals("motif")==false)
	   factory.paintBordersFor(container);
	return container;
}
public void dispose() {
	IFeatureModel model = (IFeatureModel) getFormPage().getModel();
	model.removeModelChangedListener(this);
	super.dispose();
}
public void initialize(Object input) {
	IFeatureModel model = (IFeatureModel)input;
	update(input);
	if (model.isEditable()==false) {
		descriptionText.getControl().setEnabled(false);
	}
	model.addModelChangedListener(this);
}
public boolean isDirty() {
	return descriptionText.isDirty();
}
public void modelChanged(IModelChangedEvent e) {
	if (e.getChangeType()==IModelChangedEvent.WORLD_CHANGED) {
		updateNeeded=true;
	}
}
public void setFocus() {
	if (descriptionText != null)
		descriptionText.getControl().setFocus();
}
private void setIfDefined(FormEntry formText, String value) {
	if (value != null) {
		formText.setValue(value);
		formText.setDirty(false);
	}
}
public void update() {
	if (updateNeeded) {
		this.update(getFormPage().getModel());
	}
}
public void update(Object input) {
	IFeatureModel model = (IFeatureModel)input;
	IFeature component = model.getFeature();
	ignoreChange=true;
	//setIfDefined(descriptionText, component.getDescription());
	ignoreChange=false;
	updateNeeded=false;
}
}
