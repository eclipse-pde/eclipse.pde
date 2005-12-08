package org.eclipse.pde.internal.ui.editor.target;

import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.internal.core.itarget.IEnvironmentInfo;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class EnvironmentSection extends PDESection {
	
	private ComboPart fOSCombo;
	private ComboPart fWSCombo;
	private ComboPart fNLCombo;
	private ComboPart fArchCombo;
	
	private TreeSet fNLChoices;
	private TreeSet fOSChoices;
	private TreeSet fWSChoices;
	private TreeSet fArchChoices;

	public EnvironmentSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.EnvironmentSection_title);
		section.setDescription(PDEUIMessages.EnvironmentSection_description);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.verticalAlignment = SWT.TOP;
		section.setLayoutData(data);
		
		Composite client = toolkit.createComposite(section);
		GridLayout layout = new GridLayout(2, true);
		layout.marginHeight = layout.marginWidth = 0;
		layout.horizontalSpacing = 5;
		client.setLayout(layout);
		client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Composite left = toolkit.createComposite(client);
		left.setLayout(new GridLayout(2, false));
		left.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		IEnvironmentInfo orgEnv = getEnvironment();
		initializeChoices(orgEnv);
		
		Label label = toolkit.createLabel(left, PDEUIMessages.EnvironmentSection_operationSystem);
		label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		
		fOSCombo = new ComboPart();
		fOSCombo.createControl(left, toolkit, SWT.SINGLE | SWT.BORDER );
		fOSCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fOSCombo.setItems((String[])fOSChoices.toArray(new String[fOSChoices.size()]));

		label = toolkit.createLabel(left, PDEUIMessages.EnvironmentSection_windowingSystem);
		label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		
		fWSCombo = new ComboPart();
		fWSCombo.createControl(left, toolkit, SWT.SINGLE | SWT.BORDER);
		fWSCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fWSCombo.setItems((String[])fWSChoices.toArray(new String[fWSChoices.size()]));

		Composite right = toolkit.createComposite(client);
		right.setLayout(new GridLayout(2, false));
		right.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		label = toolkit.createLabel(right, PDEUIMessages.EnvironmentSection_architecture);
		label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		
		fArchCombo = new ComboPart();
		fArchCombo.createControl(right, toolkit, SWT.SINGLE | SWT.BORDER);
		fArchCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fArchCombo.setItems((String[])fArchChoices.toArray(new String[fArchChoices.size()]));
		
		
		label = toolkit.createLabel(right, PDEUIMessages.EnvironmentSection_locale);
		label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		
		fNLCombo = new ComboPart();
		fNLCombo.createControl(right, toolkit, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL);
		fNLCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fNLCombo.setItems((String[])fNLChoices.toArray(new String[fNLChoices.size()]));
		
		refresh();
		
		fOSCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getEnvironment().setOS(getText(fOSCombo));
			}
		});
		fWSCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getEnvironment().setWS(getText(fWSCombo));
			}
		});
		fArchCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getEnvironment().setArch(getText(fArchCombo));
			}
		});
		fNLCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String value = getText(fNLCombo);
				int index = value.indexOf("-"); //$NON-NLS-1$
				if (index > 0)
					value = value.substring(0, index);
				getEnvironment().setNL(value.trim());
			}
		});
		
		toolkit.paintBordersFor(client);
		section.setClient(client);
	}
	
	private void initializeChoices(IEnvironmentInfo orgEnv) {
		fOSChoices = new TreeSet();
		String[] os = Platform.knownOSValues();
		for (int i = 0; i < os.length; i++)
			fOSChoices.add(os[i]);
		fOSChoices.add(""); //$NON-NLS-1$
		String fileValue = orgEnv.getOS();
		if (fileValue != null)
			fOSChoices.add(fileValue);
		
		fWSChoices = new TreeSet();
		String[] ws = Platform.knownWSValues();
		for (int i = 0; i < ws.length; i++)
			fWSChoices.add(ws[i]);
		fWSChoices.add(""); //$NON-NLS-1$
		fileValue = orgEnv.getWS();
		if (fileValue != null) 
			fWSChoices.add(fileValue);
		
		fArchChoices = new TreeSet();
		String[] arch = Platform.knownOSArchValues();
		for (int i = 0; i < arch.length; i++)
			fArchChoices.add(arch[i]);
		fArchChoices.add(""); //$NON-NLS-1$
		fileValue = orgEnv.getArch();
		if (fileValue != null)
			fArchChoices.add(fileValue);
		
		fNLChoices = new TreeSet();
		initializeAllLocales();
		fNLChoices.add(""); //$NON-NLS-1$
	}
	
	private void initializeAllLocales() {
		String[] nl = getLocales();
		for (int i = 0; i < nl.length; i++)
			fNLChoices.add(nl[i]);
		String fileValue = getEnvironment().getNL();
		if (fileValue != null)
			fNLChoices.add(expandLocaleName(fileValue));
	}
	
	private static String[] getLocales() {
		Locale[] locales = Locale.getAvailableLocales();
		String[] result = new String[locales.length];
		for (int i = 0; i < locales.length; i++) {
			Locale locale = locales[i];
			StringBuffer buffer = new StringBuffer();
			buffer.append(locale.toString());
			buffer.append(" - "); //$NON-NLS-1$
			buffer.append(locale.getDisplayName());
			result[i] = buffer.toString();
		}
		return result;
	}
	
	private String expandLocaleName(String name) {
		String language = ""; //$NON-NLS-1$
		String country = ""; //$NON-NLS-1$
		String variant = ""; //$NON-NLS-1$
		
		StringTokenizer tokenizer = new StringTokenizer(name, "_"); //$NON-NLS-1$
		if (tokenizer.hasMoreTokens())
			language = tokenizer.nextToken();
		if (tokenizer.hasMoreTokens())
			country = tokenizer.nextToken();
		if (tokenizer.hasMoreTokens())
			variant = tokenizer.nextToken();
			
		Locale locale = new Locale(language, country, variant);
		return locale.toString() + " - " + locale.getDisplayName(); //$NON-NLS-1$
	}
	
	private String getText(ComboPart combo) {
		Control control = combo.getControl();
		if (control instanceof Combo)
			return ((Combo) control).getText();
		return ((CCombo) control).getText();
	}
	
	private IEnvironmentInfo getEnvironment() {
		IEnvironmentInfo info = getTarget().getEnvironment();
		if (info == null) {
			info = getModel().getFactory().createEnvironment();
			getTarget().setEnvironment(info);
		}
		return info;
	}
	
	private ITarget getTarget() {
		return getModel().getTarget();
	}
	
	private ITargetModel getModel() {
		return (ITargetModel)getPage().getPDEEditor().getAggregateModel();
	}
	
	public void refresh() {
		IEnvironmentInfo orgEnv = getEnvironment();
		String presetValue = (orgEnv.getOS() == null) ? "" : orgEnv.getOS(); //$NON-NLS-1$
		fOSCombo.setText(presetValue);
		presetValue = (orgEnv.getWS() == null) ? "" : orgEnv.getWS(); //$NON-NLS-1$
		fWSCombo.setText(presetValue);
		presetValue = (orgEnv.getArch() == null) ? "" : orgEnv.getArch(); //$NON-NLS-1$
		fArchCombo.setText(presetValue);
		presetValue = (orgEnv.getNL() == null) ? "" : expandLocaleName(orgEnv.getNL()); //$NON-NLS-1$
		fNLCombo.setText(presetValue);
		
		super.refresh();
	}

}
