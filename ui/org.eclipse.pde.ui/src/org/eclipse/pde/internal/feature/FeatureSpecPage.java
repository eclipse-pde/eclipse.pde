package org.eclipse.pde.internal.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.base.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.*;
import org.eclipse.core.runtime.*;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import java.util.StringTokenizer;

public class FeatureSpecPage extends WizardPage {
	public static final String PAGE_TITLE = "NewFeatureWizard.SpecPage.title";
	public static final String PAGE_ID = "NewFeatureWizard.SpecPage.id";
	public static final String PAGE_NAME = "NewFeatureWizard.SpecPage.name";
	public static final String PAGE_VERSION = "NewFeatureWizard.SpecPage.version";
	public static final String PAGE_PROVIDER = "NewFeatureWizard.SpecPage.provider";
	public static final String PAGE_DESC = "NewFeatureWizard.SpecPage.desc";
	public static final String KEY_VERSION_FORMAT = "NewFeatureWizard.SpecPage.versionFormat";
	public static final String KEY_MISSING = "NewFeatureWizard.SpecPage.missing";
	public static final String KEY_INVALID_ID = "NewFeatureWizard.SpecPage.invalidId";
	private WizardNewProjectCreationPage mainPage;
	private Text idText;
	private Text nameText;
	private Text versionText;
	private Text providerText;

protected FeatureSpecPage(WizardNewProjectCreationPage mainPage) {
	super("specPage");
	setTitle(PDEPlugin.getResourceString(PAGE_TITLE));
	setDescription(PDEPlugin.getResourceString(PAGE_DESC));
	this.mainPage = mainPage;
}
public void createControl(Composite parent) {
	Composite container = new Composite(parent, SWT.NULL);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	layout.verticalSpacing = 9;
	layout.horizontalSpacing = 9;
	container.setLayout(layout);

	ModifyListener listener = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			verifyComplete();
		}
	};

	Label label = new Label(container, SWT.NULL);
	label.setText(PDEPlugin.getResourceString(PAGE_ID));
	idText = new Text(container, SWT.BORDER);
	GridData gd = new GridData(GridData.FILL_HORIZONTAL);
	idText.setLayoutData(gd);
	idText.addModifyListener(listener);

	label = new Label(container, SWT.NULL);
	label.setText(PDEPlugin.getResourceString(PAGE_NAME));
	nameText = new Text(container, SWT.BORDER);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	nameText.setLayoutData(gd);
	nameText.addModifyListener(listener);

	label = new Label(container, SWT.NULL);
	label.setText(PDEPlugin.getResourceString(PAGE_VERSION));
	versionText = new Text(container, SWT.BORDER);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	versionText.setLayoutData(gd);
	versionText.addModifyListener(listener);

	label = new Label(container, SWT.NULL);
	label.setText(PDEPlugin.getResourceString(PAGE_PROVIDER));
	providerText = new Text(container, SWT.BORDER);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	providerText.setLayoutData(gd);
	providerText.addModifyListener(listener);

	verifyComplete();
	setControl(container);
}

private void initialize() {
	String projectName = mainPage.getProjectName();
	idText.setText(projectName);
	nameText.setText(projectName);
	versionText.setText("1.0.0");	
}

public void setVisible(boolean visible) {
	initialize();
	super.setVisible(visible);
}

public boolean finish() {
	return true;
}
public FeatureData getFeatureData() {
	FeatureData data = new FeatureData();
	data.id = idText.getText();
	try {
	   PluginVersionIdentifier pvi = new PluginVersionIdentifier(versionText.getText());
	   data.version = pvi.toString();
	}
	catch (NumberFormatException e) {
	   data.version = versionText.getText();
	}
	data.provider = providerText.getText();
	data.name = nameText.getText();
	return data;
}
private void verifyComplete() {
   boolean complete =
	   idText.getText().length() > 0;
   setPageComplete(complete);
   if (complete) {
   	  String message = verifyIdRules();
   	  if (message!=null) {
   	  	 setPageComplete(false);
   	  	 setErrorMessage(message);
   	  }
   	  else {
         setErrorMessage(null);
         verifyVersion();
   	  }
   }
   else 
      setErrorMessage(PDEPlugin.getResourceString(KEY_MISSING));
}

private boolean verifyVersion() {
	String value = versionText.getText();
	boolean result = true;
	if (value.length()==0) result = false;
	try {
	   PluginVersionIdentifier pvi = new PluginVersionIdentifier(value);
	}
	catch (Throwable e) {
		result = false;
	}
	if (result==false) {
		setPageComplete(false);
		setErrorMessage(PDEPlugin.getResourceString(KEY_VERSION_FORMAT));
	}
	return result;
}

private String verifyIdRules() {
	String problemText = PDEPlugin.getResourceString(KEY_INVALID_ID);
	String name = idText.getText();
	StringTokenizer stok = new StringTokenizer(name, ".");
	while (stok.hasMoreTokens()) {
		String token = stok.nextToken();
		for (int i=0; i<token.length(); i++) {
			if (Character.isLetterOrDigit(token.charAt(i))==false)
			   return problemText;
		}
	}
	return null;
}
}
