package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
 * @author melhem
 *
 */
public class FeaturePropertiesDialog extends BaseNewDialog {
	
	private Text fURLText;
	private Text fIdText;
	private Text fVersionText;
	private Text fLabelText;
	private Text fTypeText;
	private Text fOSText;
	private Text fWSText;
	private Text fNLText;
	private Text fArchText;
	private Button fIsPatch;
	
	/**
	 * @param shell
	 * @param siteModel
	 * @param siteObject
	 */
	public FeaturePropertiesDialog(Shell shell, ISiteModel siteModel,
			ISiteFeature siteObject) {
		super(shell, siteModel, siteObject);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.site.BaseNewDialog#getDialogTitle()
	 */
	protected String getDialogTitle() {
		return "Feature JAR Properties";
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.site.BaseNewDialog#getHelpId()
	 */
	protected String getHelpId() {
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.site.BaseNewDialog#createEntries(org.eclipse.swt.widgets.Composite)
	 */
	protected void createEntries(Composite container) {
		ISiteFeature feature = (ISiteFeature)getSiteObject();
		
		Label label = new Label(container, SWT.NONE);
		label.setText("URL:");
		fURLText = new Text(container, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 250;
		fURLText.setLayoutData(gd);
		setIfDefined(fURLText, feature.getURL());
		
		label = new Label(container, SWT.NONE);
		label.setText("Id:");
		fIdText = new Text(container, SWT.SINGLE | SWT.BORDER);
		fIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fIdText.setEnabled(false);
		setIfDefined(fIdText, feature.getId());
		
		label = new Label(container, SWT.NONE);
		label.setText("Version:");
		fVersionText = new Text(container, SWT.SINGLE | SWT.BORDER);
		fVersionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fVersionText.setEnabled(false);
		setIfDefined(fVersionText, feature.getVersion());
		
		label = new Label(container, SWT.NONE);
		label.setText("Label:");
		fLabelText = new Text(container, SWT.SINGLE | SWT.BORDER);
		fLabelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		setIfDefined(fLabelText, feature.getLabel());
		
		label = new Label(container, SWT.NONE);
		label.setText("Type:");
		fTypeText = new Text(container, SWT.SINGLE | SWT.BORDER);
		fTypeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		setIfDefined(fTypeText, feature.getType());
		
		label = new Label(container, SWT.NONE);
		label.setText("O/S:");
		fOSText = new Text(container, SWT.SINGLE | SWT.BORDER);
		fOSText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		setIfDefined(fOSText, feature.getOS());
		
		label = new Label(container, SWT.NONE);
		label.setText("W/S:");
		fWSText = new Text(container, SWT.SINGLE | SWT.BORDER);
		fWSText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		setIfDefined(fWSText, feature.getWS());
		
		label = new Label(container, SWT.NONE);
		label.setText("Languages:");
		fNLText = new Text(container, SWT.SINGLE | SWT.BORDER);
		fNLText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		setIfDefined(fNLText, feature.getNL());
		
		label = new Label(container, SWT.NONE);
		label.setText("Architectures:");
		fArchText = new Text(container, SWT.SINGLE | SWT.BORDER);
		fArchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		setIfDefined(fArchText, feature.getArch());
		
		fIsPatch = new Button(container, SWT.CHECK);
		fIsPatch.setText("Patch");
		gd = new GridData();
		gd.horizontalSpan = 2;
		fIsPatch.setLayoutData(gd);
		fIsPatch.setSelection(feature.isPatch());
		
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.site.BaseNewDialog#hookListeners(org.eclipse.swt.events.ModifyListener)
	 */
	protected void hookListeners(ModifyListener listener) {
		fURLText.addModifyListener(listener);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.site.BaseNewDialog#dialogChanged()
	 */
	protected void dialogChanged() {
		IStatus status = (fURLText.getText().trim().length() == 0) ? createErrorStatus(getEmptyErrorMessage()) : getOKStatus();
		updateStatus(status);			
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.site.BaseNewDialog#getEmptyErrorMessage()
	 */
	protected String getEmptyErrorMessage() {
		return "URL is a required attribute";
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.site.BaseNewDialog#execute()
	 */
	protected void execute() {
		try {
			ISiteFeature feature = (ISiteFeature)getSiteObject();
			feature.setIsPatch(fIsPatch.getSelection());
			feature.setURL(fURLText.getText().trim());
			
			String text = fLabelText.getText().trim();
			feature.setLabel(text.length() == 0 ? null : text);
			
			text = fTypeText.getText().trim();
			feature.setType(text.length() == 0 ? null : text);
			
			text = fOSText.getText().trim();
			feature.setOS(text.length() == 0 ? null : text);
			
			text = fWSText.getText().trim();
			feature.setWS(text.length() == 0 ? null : text);
			
			text = fNLText.getText().trim();
			feature.setNL(text.length() == 0 ? null : text);
			
			text = fArchText.getText().trim();
			feature.setArch(text.length() == 0 ? null : text);
		} catch (CoreException e) {
		}
	}
}
