/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.Locale;
import java.util.TreeMap;

import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

public class PlatformFilterSection extends PDESection {
	
	private static String PLATFORM_FILTER = "Eclipse-PlatformFilter";

	private ComboPart fNL;
	private ComboPart fArch;
	private ComboPart fWS;
	private ComboPart fOS;
	
	private ModifyListener adapter = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			writePlatformFilterHeader();
		}
	};

	public PlatformFilterSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText("Supported Environment");
		section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		section.setDescription(getDescription());
		
		Composite client = toolkit.createComposite(section);
		TableWrapLayout layout = new TableWrapLayout();
		layout.leftMargin = layout.rightMargin = toolkit.getBorderStyle() != SWT.NULL ? 0 : 2;
		layout.numColumns = 2;
		client.setLayout(layout);
		section.setClient(client);
		
		toolkit.createLabel(client, "Operating System:");
		fOS = new ComboPart();
		fOS.createControl(client, toolkit, SWT.NONE);
		TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
		td.maxWidth = 50;
		fOS.getControl().setLayoutData(td);
		fOS.setItems(Platform.knownOSValues());
		fOS.add("", 0);
		fOS.addModifyListener(adapter);
		
		toolkit.createLabel(client, "Windowing System:");
		fWS = new ComboPart();
		fWS.createControl(client, toolkit, SWT.NONE);
		td = new TableWrapData(TableWrapData.FILL_GRAB);
		td.maxWidth = 50;
		fWS.getControl().setLayoutData(td);
		fWS.setItems(Platform.knownWSValues());
		fWS.add("", 0);
		fWS.addModifyListener(adapter);
		
		toolkit.createLabel(client, "Architecture:");
		fArch = new ComboPart();
		fArch.createControl(client, toolkit, SWT.NONE);
		td = new TableWrapData(TableWrapData.FILL_GRAB);
		td.maxWidth = 50;
		fArch.getControl().setLayoutData(td);
		fArch.setItems(Platform.knownOSArchValues());
		fArch.add("", 0);
		fArch.addModifyListener(adapter);
		
		toolkit.createLabel(client, "Locale:");
		fNL = new ComboPart();
		fNL.createControl(client, toolkit, SWT.NONE);
		td = new TableWrapData(TableWrapData.FILL_GRAB);
		td.maxWidth = 50;
		fNL.getControl().setLayoutData(td);
		fNL.setItems(getLocales());
		fNL.add("", 0);
		fNL.addModifyListener(adapter);
	}
	
	private String[] getLocales() {
		Locale[] locales = Locale.getAvailableLocales();
		TreeMap map = new TreeMap();
		for (int i = 0; i < locales.length; i++) {
			Locale locale = locales[i];
			map.put(locale.toString(), locale.toString() + " - " + locale.getDisplayName()); //$NON-NLS-1$
		}
		return (String[])map.values().toArray(new String[map.size()]);
	}

	private String getDescription() {
		return "If this plug-in contains platform-specific code, specify the environment in which it can run.  Otherwise, leave blank.";
	}
	
	private void writePlatformFilterHeader() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(getProperty(fOS, "osgi.os"));
		buffer.append(getProperty(fWS, "osgi.ws"));
		buffer.append(getProperty(fArch, "osgi.arch"));
		buffer.append(getProperty(fNL, "osgi.nl"));
		if (buffer.length() > 0) {
			buffer.insert(0, "(&");
			buffer.append(")");
		}
		getBundleModel().getBundle().setHeader(PLATFORM_FILTER, buffer.toString());
	}
	
	private String getProperty(ComboPart combo, String prop) {
		if (combo.getSelectionIndex() == -1)
			return "";
		String value = combo.getSelection();
		return value.length() > 0 ? "(" + prop + "=" + value + ")" : "";
	}
	
	private IBundleModel getBundleModel() {
		InputContext context = getPage().getPDEEditor().getContextManager()
				.findContext(BundleInputContext.CONTEXT_ID);
		return context != null ? (IBundleModel) context.getModel() : null;
	}

}
