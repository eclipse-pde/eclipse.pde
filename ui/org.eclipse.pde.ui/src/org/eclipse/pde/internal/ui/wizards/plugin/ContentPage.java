package org.eclipse.pde.internal.ui.wizards.plugin;

import java.util.StringTokenizer;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
 * @author melhem
 *
 */
public class ContentPage extends WizardPage {
	private boolean fIsFragment;
	private boolean fFirstVisible = true;

	private Text fIdText;
	private Text fVersionText;
	private Text fNameText;
	private Text fProviderText;
	private Text fPluginIdText;
	private Text fPluginVersion;
	private Combo fMatchCombo;
	private Button fLegacyButton;
	private AbstractFieldData fData;
	private IProjectProvider fProjectProvider;
	
	private static final String KEY_MATCH_PERFECT =
		"ManifestEditor.MatchSection.perfect"; //$NON-NLS-1$
	private static final String KEY_MATCH_EQUIVALENT =
		"ManifestEditor.MatchSection.equivalent"; //$NON-NLS-1$
	private static final String KEY_MATCH_COMPATIBLE =
		"ManifestEditor.MatchSection.compatible"; //$NON-NLS-1$
	private static final String KEY_MATCH_GREATER =
		"ManifestEditor.MatchSection.greater"; //$NON-NLS-1$
	private Text fClassText;
	private Button fGenerateClass;
	private Button fUIPlugin;
	private Label fClassLabel;
	private ProjectStructurePage fStructurePage;

	public ContentPage(String pageName, IProjectProvider provider, ProjectStructurePage page1, AbstractFieldData data, boolean isFragment) {
		super(pageName);
		fIsFragment = isFragment;
		fProjectProvider = provider;
		fStructurePage = page1;
		fData = data;
		if (isFragment) {
			setTitle(PDEPlugin.getResourceString("ContentPage.ftitle")); //$NON-NLS-1$
			setDescription(PDEPlugin.getResourceString("ContentPage.fdesc")); //$NON-NLS-1$
		} else {
			setTitle(PDEPlugin.getResourceString("ContentPage.title")); //$NON-NLS-1$
			setDescription(PDEPlugin.getResourceString("ContentPage.desc")); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		
		Label label = new Label(container, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ContentPage.id"));		 //$NON-NLS-1$
		fIdText = createText(container);
		
		label = new Label(container, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ContentPage.version"));		 //$NON-NLS-1$
		fVersionText = createText(container);	
		
		label = new Label(container, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ContentPage.name"));		 //$NON-NLS-1$
		fNameText = createText(container);	
		
		label = new Label(container, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ContentPage.provider"));		 //$NON-NLS-1$
		fProviderText = createText(container);
		
		if (fIsFragment) {
			addFragmentSpecificControls(container);			
		} else {
			addPluginSepecificControls(container);
		}
		
		label = new Label(container, SWT.NONE);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		
		fLegacyButton = new Button(container, SWT.CHECK);
		gd = new GridData();
		gd.horizontalSpan = 2;
		fLegacyButton.setLayoutData(gd);
		fLegacyButton.setText(PDEPlugin.getResourceString("ContentPage.legacy")); //$NON-NLS-1$
		fLegacyButton.setSelection(!PDECore.getDefault().getModelManager().isOSGiRuntime());
		validatePage();	
		Dialog.applyDialogFont(container);
		setControl(container);
	}
	
	/**
	 * @param container
	 */
	private void addPluginSepecificControls(Composite container) {
		Label label = new Label(container, SWT.NONE);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		
		fGenerateClass = new Button(container, SWT.CHECK);
		fGenerateClass.setText(PDEPlugin.getResourceString("ContentPage.generate")); //$NON-NLS-1$
		fGenerateClass.setSelection(true);
		gd = new GridData();
		gd.horizontalSpan = 2;
		fGenerateClass.setLayoutData(gd);
		fGenerateClass.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fClassLabel.setEnabled(fGenerateClass.getSelection());
				fClassText.setEnabled(fGenerateClass.getSelection());
				fUIPlugin.setEnabled(fGenerateClass.getSelection());
				validatePage();
			}
		});
		
		fClassLabel = new Label(container, SWT.NONE);
		fClassLabel.setText(PDEPlugin.getResourceString("ContentPage.classname")); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalIndent = 30;
		fClassLabel.setLayoutData(gd);
		fClassText = createText(container);
		
		fUIPlugin = new Button(container, SWT.CHECK);
		fUIPlugin.setText(PDEPlugin.getResourceString("ContentPage.uicontribution")); //$NON-NLS-1$
		fUIPlugin.setSelection(true);
		gd = new GridData();
		gd.horizontalIndent = 30;
		gd.horizontalSpan = 2;
		fUIPlugin.setLayoutData(gd);
		fUIPlugin.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getContainer().updateButtons();
			}
		});
	}

	/**
	 * @param container
	 */
	private void addFragmentSpecificControls(Composite container) {
		Label label = new Label(container, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ContentPage.pid")); //$NON-NLS-1$
		createPluginIdContainer(container);
		label = new Label(container, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ContentPage.pversion")); //$NON-NLS-1$
		fPluginVersion = createText(container);
		label = new Label(container, SWT.NONE);
		label.setText(PDEPlugin.getResourceString(PDEPlugin.getResourceString("ContentPage.matchRule"))); //$NON-NLS-1$
		fMatchCombo = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
		fMatchCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fMatchCombo.setItems(new String[]{
				"", //$NON-NLS-1$
				PDEPlugin.getResourceString(KEY_MATCH_EQUIVALENT),
				PDEPlugin.getResourceString(KEY_MATCH_COMPATIBLE),
				PDEPlugin.getResourceString(KEY_MATCH_PERFECT),
				PDEPlugin.getResourceString(KEY_MATCH_GREATER)});
		fMatchCombo.setText(fMatchCombo.getItem(0));
	}

	private void createPluginIdContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fPluginIdText = createText(container);
		
		Button browse = new Button(container, SWT.PUSH);
		browse.setText(PDEPlugin.getResourceString("ContentPage.browse")); //$NON-NLS-1$
		browse.setLayoutData(new GridData());
		browse.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			BusyIndicator.showWhile(fPluginIdText.getDisplay(), new Runnable() {
				public void run() {
					PluginSelectionDialog dialog =
						new PluginSelectionDialog(fPluginIdText.getShell(), false, false);
					dialog.create();
					if (dialog.open() == PluginSelectionDialog.OK) {
						IPluginModel model = (IPluginModel) dialog.getFirstResult();
						IPlugin plugin = model.getPlugin();
						fPluginIdText.setText(plugin.getId());
						fPluginVersion.setText(plugin.getVersion());
					}
				}
			});
		}});
		SWTUtil.setButtonDimensionHint(browse);
	}

	private Text createText(Composite parent) {
		Text text = new Text(parent, SWT.BORDER | SWT.SINGLE);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validatePage();
			}
		});
		return text;
	}
	
	private void validatePage() {
		setMessage(null);
		String errorMessage = validateId();
		if (errorMessage == null) {
			if (fVersionText.getText().trim().length() == 0) {
				errorMessage = PDEPlugin.getResourceString("ContentPage.noversion"); //$NON-NLS-1$
			} else if (!isVersionValid(fVersionText.getText().trim())) {
				errorMessage = PDEPlugin.getResourceString("ContentPage.badversion"); //$NON-NLS-1$
			} else if (fNameText.getText().trim().length() == 0) {
				errorMessage = PDEPlugin.getResourceString("ContentPage.noname"); //$NON-NLS-1$
			}
		}
		if (errorMessage == null) {
			if (fIsFragment) {
				String pluginID = fPluginIdText.getText().trim();
				if (pluginID.length() == 0) {
					errorMessage = PDEPlugin.getResourceString("ContentPage.nopid"); //$NON-NLS-1$
				} else if (PDECore.getDefault().getModelManager().findEntry(
						pluginID) == null) {
					errorMessage = PDEPlugin.getResourceString("ContentPage.pluginNotFound"); //$NON-NLS-1$
				} else if (fPluginVersion.getText().trim().length() == 0) {
					errorMessage = PDEPlugin.getResourceString("ContentPage.nopversion"); //$NON-NLS-1$
				} else if (!isVersionValid(fPluginVersion.getText().trim())) {
					errorMessage = PDEPlugin.getResourceString("ContentPage.badpversion"); //$NON-NLS-1$
				}
			} else if (fGenerateClass.getSelection()){
				IStatus status = JavaConventions.validateJavaTypeName(fClassText.getText().trim());
				if (status.getSeverity() == IStatus.ERROR) {
					errorMessage = status.getMessage();
				} else if (status.getSeverity() == IStatus.WARNING) {
					setMessage(status.getMessage(), DialogPage.WARNING);
				}
			}
		}
		setErrorMessage(errorMessage);
		setPageComplete(errorMessage == null);
	}

	private String validateId() {
		String id = fIdText.getText().trim();
		if (id.length() == 0)
			return PDEPlugin.getResourceString("ContentPage.noid"); //$NON-NLS-1$
		
		StringTokenizer stok = new StringTokenizer(id, "."); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			for (int i = 0; i < token.length(); i++) {
				if (!Character.isLetterOrDigit(token.charAt(i)) && '_' != token.charAt(i))
					return PDEPlugin.getResourceString("ContentPage.invalidId"); //$NON-NLS-1$
			}
		}
		return null;
	}
	
	private boolean isVersionValid(String version) {
		try {
			new PluginVersionIdentifier(version);
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		String id = computeId();			
		fIdText.setText(id);
		if (visible) {
			if (fStructurePage.hasBundleStructure()) {
				fLegacyButton.setEnabled(false);
				fLegacyButton.setSelection(false);
			} else {
				fLegacyButton.setEnabled(true);
			}	
			if (!fIsFragment){
				if (fStructurePage.isSimpleProject()) {
					fGenerateClass.setSelection(false);
					fGenerateClass.setEnabled(false);
					fClassLabel.setEnabled(false);
					fClassText.setEnabled(false);
					fUIPlugin.setEnabled(false);
				} else {
					fGenerateClass.setEnabled(true);
				}
			}
		}
		if (visible && fFirstVisible) {
			fFirstVisible = false;
			fVersionText.setText("1.0.0"); //$NON-NLS-1$
			presetNameField(id);
			presetProviderField(id);
			if (!fIsFragment)
				presetClassField();
		}
		validatePage();		
		if (!visible) 
			updateData();
		super.setVisible(visible);
	}
	
	private String computeId() {
		String fullName = fProjectProvider.getProjectName();
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < fullName.length(); i++) {
			char ch = fullName.charAt(i);
			if (Character.isLetterOrDigit(ch) || ch == '.' || ch == '_')
				buffer.append(ch);
			else
				buffer.append('_');
		}
		return buffer.toString();
	}
	
	private void presetNameField(String id) {
		StringTokenizer tok = new StringTokenizer(id, "."); //$NON-NLS-1$
		while (tok.hasMoreTokens()) {
			String token = tok.nextToken();
			if (!tok.hasMoreTokens()) {
				fNameText.setText(Character.toUpperCase(token.charAt(0))
						+ ((token.length() > 1) ? token.substring(1) : "") //$NON-NLS-1$
						+ " " + (fIsFragment ? PDEPlugin.getResourceString("ContentPage.fragment") : PDEPlugin.getResourceString("ContentPage.plugin"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}		
	}
	
	private void presetProviderField(String id) {
		StringTokenizer tok = new StringTokenizer(id, "."); //$NON-NLS-1$
		int count = tok.countTokens();
		if (count > 2 && tok.nextToken().equals("com")) //$NON-NLS-1$
			fProviderText.setText(tok.nextToken().toUpperCase());
	}

	private void presetClassField() {
		String name = fProjectProvider.getProjectName();
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
			if (buffer.length() == 0) {
				if (Character.isJavaIdentifierStart(ch))
					buffer.append(ch);
			} else {
				if (Character.isJavaIdentifierPart(ch) || ch == '.')
					buffer.append(ch);
			}
		}
		StringTokenizer tok = new StringTokenizer(buffer.toString(), "."); //$NON-NLS-1$
		while (tok.hasMoreTokens()) {
			String token = tok.nextToken();
			if (!tok.hasMoreTokens())
				buffer.append("." + Character.toUpperCase(token.charAt(0)) + token.substring(1)+ "Plugin"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		fClassText.setText(buffer.toString());
	}

	public void updateData() {
		fData.setId(fIdText.getText().trim());
		fData.setVersion(fVersionText.getText().trim());
		fData.setName(fNameText.getText().trim());
		fData.setProvider(fProviderText.getText().trim());
		fData.setIsLegacy(fLegacyButton.getSelection());
		if (fIsFragment) {
			((FragmentFieldData)fData).setPluginId(fPluginIdText.getText().trim());
			((FragmentFieldData)fData).setPluginVersion(fPluginVersion.getText().trim());
			((FragmentFieldData)fData).setMatch(fMatchCombo.getSelectionIndex());
		} else {
			((PluginFieldData)fData).setClassname(fClassText.getText().trim());
			((PluginFieldData)fData).setIsUIPlugin(fUIPlugin.getSelection());
			((PluginFieldData)fData).setDoGenerateClass(fGenerateClass.getSelection());
		}
	}
	
	public IFieldData getData() {
		return fData;
	}
	
	public String getId() {
		return fIdText.getText().trim();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#canFlipToNextPage()
	 */
	public boolean canFlipToNextPage() {
		if (fIsFragment)
			return super.canFlipToNextPage();
		return (fGenerateClass.getSelection() && fUIPlugin.getSelection()) || !fGenerateClass.getSelection();
	}
	
}
