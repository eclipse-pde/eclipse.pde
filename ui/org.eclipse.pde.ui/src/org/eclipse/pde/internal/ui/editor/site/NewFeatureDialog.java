package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

public class NewFeatureDialog extends BaseDialog {
	private static final String KEY_TITLE = "NewFeatureDialog.title"; //$NON-NLS-1$
	private static final String KEY_URL = "NewFeatureDialog.url"; //$NON-NLS-1$
	private static final String KEY_EMPTY = "NewFeatureDialog.empty"; //$NON-NLS-1$
	private static final String SETTINGS_SECTION = "NewFeatureDialog";
	private static final String S_URL = "url";
	private Text urlText;

	public NewFeatureDialog(Shell shell, ISiteModel siteModel) {
		super(shell, siteModel, null);
	}

	protected void createEntries(Composite container) {
		GridData gd;
		Label label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_URL));
		urlText = new Text(container, SWT.SINGLE | SWT.BORDER);
		presetURL();
		gd = new GridData(GridData.FILL_HORIZONTAL);
		urlText.setLayoutData(gd);
	}

	private void presetURL() {
		IDialogSettings settings = getDialogSettings(SETTINGS_SECTION);
		String initialText = settings.get(S_URL);
		if (initialText == null) {
			ISiteBuildModel siteBuildModel = getSiteModel().getBuildModel();
			IPath featureLocation =
				siteBuildModel.getSiteBuild().getFeatureLocation();
			initialText = featureLocation.addTrailingSeparator().toString();
		}
		urlText.setText(initialText);
		urlText.setSelection(urlText.getText().length());
	}

	protected String getDialogTitle() {
		return PDEPlugin.getResourceString(KEY_TITLE);
	}

	protected String getHelpId() {
		return IHelpContextIds.NEW_FEATURE_DIALOG;
	}

	protected String getEmptyErrorMessage() {
		return PDEPlugin.getResourceString(KEY_EMPTY);
	}

	protected void hookListeners(ModifyListener modifyListener) {
		urlText.addModifyListener(modifyListener);
	}

	protected void dialogChanged() {
		IStatus status = null;
		String text = urlText.getText();
		if (text.length() == 0)
			status = getEmptyErrorStatus();
		else {
			if (sameURL(text))
				status = createErrorStatus(PDEPlugin.getResourceString("NewFeatureDialog.alreadyDefined")); //$NON-NLS-1$
		}
		if (status == null)
			status = getOKStatus();
		updateStatus(status);

	}

	private boolean sameURL(String url) {
		ISiteFeature[] features = getSiteModel().getSite().getFeatures();
		for (int i = 0; i < features.length; i++) {
			ISiteFeature feature = features[i];
			String furl = feature.getURL();
			if (furl != null && furl.equals(url)) {
				return true;
			}
		}
		return false;
	}

	protected void execute() {
		ISiteModel siteModel = getSiteModel();
		ISiteFeature feature = siteModel.getFactory().createFeature();

		try {
			feature.setURL(urlText.getText());
			IPath path = new Path(feature.getURL());
			String jarName = path.removeFileExtension().lastSegment();
			int underLoc = jarName.indexOf('_');
			if (underLoc != -1) {
				String id = jarName.substring(0, underLoc);
				String version = jarName.substring(underLoc + 1);
				feature.setId(id);
				feature.setVersion(version);
			}
			siteModel.getSite().addFeatures(new ISiteFeature[] { feature });
			getDialogSettings(SETTINGS_SECTION).put(S_URL, feature.getURL());
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
}
