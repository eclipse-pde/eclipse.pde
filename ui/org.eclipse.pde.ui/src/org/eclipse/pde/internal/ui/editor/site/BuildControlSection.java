package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.core.site.WorkspaceSiteBuildModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormSection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;

public class BuildControlSection extends PDEFormSection {
	public static final String SECTION_TITLE =
		"SiteEditor.BuildControlSection.title";
	public static final String SECTION_DESC =
		"SiteEditor.BuildControlSection.desc";
	public static final String SECTION_PLUGIN_DEST =
		"SiteEditor.BuildControlSection.pluginDest";
	public static final String SECTION_FEATURE_DEST =
		"SiteEditor.BuildControlSection.featureDest";
	public static final String SECTION_CONSOLE =
		"SiteEditor.BuildControlSection.console";
	public static final String SECTION_AUTOBUILD =
		"SiteEditor.BuildControlSection.autobuild";
	public static final String SECTION_BUILD =
		"SiteEditor.BuildControlSection.build";

	private FormEntry pluginDest;
	private FormEntry featureDest;

	private Button consoleButton;
	private Button autobuildButton;
	private Button buildButton;

	private boolean updateNeeded;

	public BuildControlSection(BuildPage page) {
		super(page);
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		setDescription(PDEPlugin.getResourceString(SECTION_DESC));
	}
	public void commitChanges(boolean onSave) {
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
	public Composite createClient(
		Composite parent,
		FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		layout.horizontalSpacing = 6;
		container.setLayout(layout);
		Button browse;

		pluginDest =
			new FormEntry(
				createText(
					container,
					PDEPlugin.getResourceString(SECTION_PLUGIN_DEST),
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
		browse= factory.createButton(container, "Browse...", SWT.PUSH);
		browse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
			}
		});
		featureDest =
			new FormEntry(
				createText(
					container,
					PDEPlugin.getResourceString(SECTION_FEATURE_DEST),
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
		browse= factory.createButton(container, "Browse...", SWT.PUSH);
		browse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
			}
		});
		consoleButton =
			factory.createButton(
				container,
				PDEPlugin.getResourceString(SECTION_CONSOLE),
				SWT.CHECK);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 3;
		consoleButton.setLayoutData(gd);
		consoleButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setShowConsole(consoleButton.getSelection());
			}
		});

		autobuildButton =
			factory.createButton(
				container,
				PDEPlugin.getResourceString(SECTION_AUTOBUILD),
				SWT.CHECK);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 3;
		autobuildButton.setLayoutData(gd);
		autobuildButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setAutobuild(autobuildButton.getSelection());
			}
		});

		Composite buttonContainer = factory.createComposite(container);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
		gd.horizontalSpan = 3;
		buttonContainer.setLayoutData(gd);
		GridLayout blayout = new GridLayout();
		buttonContainer.setLayout(blayout);
		//blayout.makeColumnsEqualWidth = true;
		//blayout.numColumns = 2;
		blayout.marginWidth = 0;

		buildButton =
			factory.createButton(
				buttonContainer,
				PDEPlugin.getResourceString(SECTION_BUILD),
				SWT.PUSH);
		buildButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBuild();
			}
		});
		gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_BEGINNING
					| GridData.VERTICAL_ALIGN_BEGINNING);
		buildButton.setLayoutData(gd);

		factory.paintBordersFor(container);
		return container;
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

	private void setAutobuild(boolean value) {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISiteBuildModel buildModel = model.getBuildModel();
		if (buildModel == null)
			return;
		ISiteBuild siteBuild = buildModel.getSiteBuild();
		try {
			siteBuild.setAutobuild(value);
			forceDirty();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void setShowConsole(boolean value) {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISiteBuildModel buildModel = model.getBuildModel();
		if (buildModel == null)
			return;
		ISiteBuild siteBuild = buildModel.getSiteBuild();
		try {
			siteBuild.setShowConsole(value);
			forceDirty();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void forceDirty() {
		setDirty(true);
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISiteBuildModel buildModel = model.getBuildModel();

		if (buildModel instanceof IEditable) {
			IEditable editable = (IEditable) buildModel;
			editable.setDirty(true);
			if (model instanceof IEditable) {
				((IEditable) model).setDirty(true);
			}
			getFormPage().getEditor().fireSaveNeeded();
		}
	}

	public void dispose() {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		model.removeModelChangedListener(this);
		super.dispose();
	}

	private void handleBuild() {
		/*
			final FeatureEditorContributor contributor =
				(FeatureEditorContributor) getFormPage().getEditor().getContributor();
			BusyIndicator.showWhile(createJarButton.getDisplay(), new Runnable() {
				public void run() {
					contributor.getBuildAction().run();
				}
			});
		*/
	}

	public void initialize(Object input) {
		ISiteModel model = (ISiteModel) input;
		update(input);
		ISiteBuildModel buildModel = model.getBuildModel();
		if (buildModel.isEditable() == false) {
			featureDest.getControl().setEditable(false);
			pluginDest.getControl().setEditable(false);
			autobuildButton.setEnabled(false);
			consoleButton.setEnabled(false);
		}
		model.addModelChangedListener(this);
	}
/*
	public boolean isDirty() {
		return pluginDest.isDirty() || featureDest.isDirty();
	}
*/
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			updateNeeded = true;
		} else if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			Object objs[] = e.getChangedObjects();
			if (objs.length > 0 && objs[0] instanceof ISite) {
				updateNeeded = true;
				if (getFormPage().isVisible())
					update();
			}
		}
	}
	public void setFocus() {
		if (pluginDest != null)
			pluginDest.getControl().setFocus();
	}
	private void setIfDefined(FormEntry formText, String value) {
		if (value != null) {
			formText.setValue(value, true);
		}
	}
	private void setIfDefined(Text text, String value) {
		if (value != null)
			text.setText(value);
	}
	public void update() {
		if (updateNeeded) {
			this.update(getFormPage().getModel());
		}
	}
	public void update(Object input) {
		ISiteModel model = (ISiteModel) input;
		ISiteBuildModel buildModel = model.getBuildModel();
		if (buildModel != null) {
			ISiteBuild siteBuild = buildModel.getSiteBuild();
			setIfDefined(
				featureDest,
				siteBuild.getFeatureLocation() != null
					? siteBuild.getFeatureLocation().toOSString()
					: null);
			setIfDefined(
				pluginDest,
				siteBuild.getPluginLocation() != null
					? siteBuild.getPluginLocation().toOSString()
					: null);
			consoleButton.setSelection(siteBuild.getShowConsole());
			autobuildButton.setSelection(siteBuild.isAutobuild());
		}
		updateNeeded = false;
	}
	/**
	 * @see org.eclipse.update.ui.forms.internal.FormSection#canPaste(Clipboard)
	 */
	public boolean canPaste(Clipboard clipboard) {
		return (clipboard.getContents(TextTransfer.getInstance()) != null);
	}

}