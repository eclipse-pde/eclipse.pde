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
package org.eclipse.pde.internal.ui.wizards.project;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.imports.*;

public class MigratePluginWizard extends Wizard {
	private MigratePluginWizardPage page1;
	private IPluginModelBase [] selected;
	private static final String STORE_SECTION = "MigrationWizard";
	
	public MigratePluginWizard(IPluginModelBase[] selected) {
		IDialogSettings masterSettings = PDEPlugin.getDefault().getDialogSettings();
		setDialogSettings(getSettingsSection(masterSettings));
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_MIGRATE_30_WIZ);
		setWindowTitle(PDEPlugin.getResourceString("MigrationWizard.title"));
		setNeedsProgressMonitor(true);
		this.selected = selected;
	}
	
	public boolean performFinish() {
		final IPluginModelBase[] models = page1.getSelected();
		page1.storeSettings();
		final boolean doUpdateClasspath = page1.isUpdateClasspathRequested();
		
		IRunnableWithProgress operation = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
				int numUnits = doUpdateClasspath ? models.length * 2 : models.length;
				monitor.beginTask(PDEPlugin.getResourceString("MigrationWizard.progress"), numUnits);
				try {
					for (int i = 0; i < models.length; i++) {
						monitor.subTask(models[i].getPluginBase().getId());
						transform(models[i]);
						models[i].getUnderlyingResource().refreshLocal(
							IResource.DEPTH_ZERO,
							null);
						monitor.worked(1);
					}
					if (doUpdateClasspath) {
						UpdateClasspathAction.doUpdateClasspath(
							new SubProgressMonitor(monitor, models.length),
							models,
							null);
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					monitor.done();
				}
			}
		};

		try {
			getContainer().run(true, false, operation);
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		}
		return true;
	}
	
	private IDialogSettings getSettingsSection(IDialogSettings master) {
		IDialogSettings setting = master.getSection(STORE_SECTION);
		if (setting == null) {
			setting = master.addNewSection(STORE_SECTION);
		}
		return setting;
	}
	
	public void addPages() {
		page1 = new MigratePluginWizardPage(selected);
		addPage(page1);
	}
	
	private void transform(IPluginModelBase model) throws Exception {
		IResource file = model.getUnderlyingResource();
		IDocument document = createDocument(file);
		FindReplaceDocumentAdapter findAdapter = new FindReplaceDocumentAdapter(document);
		addEclipseProcessingInstruction(document, findAdapter);
		updateExtensions(document, findAdapter);
		addNewImports(document, findAdapter, getAdditionalUIImports(model));
		writeFile(document, file);
	}
	
	private IDocument createDocument(IResource file) throws Exception {
		BufferedReader reader =
		new BufferedReader(new FileReader(file.getLocation().toOSString()));
		StringBuffer buffer = new StringBuffer();
		while (reader.ready()) {
			String line = reader.readLine();
			if (line != null) {
				buffer.append(line);
				buffer.append(System.getProperty("line.separator"));
			}
		}
		reader.close();
		return new Document(buffer.toString());		
	}
	
	private void writeFile(IDocument document, IResource file) throws Exception {
		PrintWriter writer =
			new PrintWriter(new FileWriter(file.getLocation().toOSString()));
		writer.write(document.get());
		writer.close();
	}
	
	private void addEclipseProcessingInstruction(
		IDocument document,
		FindReplaceDocumentAdapter adapter) {
		try {
			IRegion region = adapter.search(-1, "<\\?xml.*\\?>", true, true, false, true);
			if (region != null) {
				String text = document.get(region.getOffset(), region.getLength());
				adapter.replace(
					text
						+ System.getProperty("line.separator")
						+ "<?eclipse version=\"3.0\"?>",
					false);
			}
		} catch (BadLocationException e) {
		}
	}
	
	private void updateExtensions(
		IDocument document,
		FindReplaceDocumentAdapter adapter) {
		int start = 0;
		for (;;) {
			try {
				IRegion region = findNextExtension(adapter, start);
				if (region == null)
					break;
				IRegion idRegion = findPointAttributeRegion(adapter, region);
				if (idRegion != null) {
					String point =
						document.get(idRegion.getOffset(), idRegion.getLength());
					if (ExtensionPointMappings.isDeprecated(point.trim())) {
						adapter.replace(
							ExtensionPointMappings.getNewId(point.trim()),
							false);
					}
				}
				start = region.getOffset() + region.getLength();
			} catch (BadLocationException e) {
			}
		}
	}

	private IRegion findPointAttributeRegion(FindReplaceDocumentAdapter adapter, IRegion parentRegion) {
		try {
			IRegion region =
			adapter.search(
					parentRegion.getOffset(),
					"\\s+point\\s*=\\s*\"",
					true,
					true,
					false,
					true);
			if (region != null
					&& region.getOffset() + region.getLength()
					<= parentRegion.getOffset() + parentRegion.getLength()) {
				region =
				adapter.search(
						region.getOffset() + region.getLength(),
						"[^\"]*",
						true,
						true,
						false,
						true);
				if (region != null
						&& region.getOffset() + region.getLength()
						< parentRegion.getOffset() + parentRegion.getLength()) {
					return region;
				}
			}
		} catch (BadLocationException e) {
		}
		return null;
	}

	private IRegion findNextExtension(FindReplaceDocumentAdapter adapter, int start) {
		int offset = -1;
		int length = -1;
		try {
			IRegion region =
			adapter.search(start, "<extension\\s+", true, true, false, true);
			if (region != null) {
				offset = region.getOffset();
				region = adapter.search(offset, ">", true, true, false, false);
				if (region != null) {
					length = region.getOffset() - offset + 1;
				}
			}
		} catch (BadLocationException e) {
		}
		return (offset != -1 && length != -1) ? new Region(offset, length) : null;
	}
	
	private String[] getAdditionalUIImports(IPluginModelBase model) {
		ArrayList result = new ArrayList();
		IPluginImport uiImport = findImport(model, "org.eclipse.ui");
		if (uiImport != null) {
			ArrayList list = new ArrayList();
			list.add("org.eclipse.ui.ide");
			list.add("org.eclipse.ui.views");
			list.add("org.eclipse.jface.text");
			list.add("org.eclipse.ui.workbench.texteditor");
			list.add("org.eclipse.ui.editors");
			IPluginImport[] imports = model.getPluginBase().getImports();
			for (int i = 0; i < imports.length; i++) {
				if (list.contains(imports[i].getId())) {
					list.remove(imports[i].getId());
				}
			}
			for (int i = 0; i < list.size(); i++) {
				StringBuffer buffer = new StringBuffer("<import plugin=\"");
				buffer.append(list.get(i) + "\"");
				if (uiImport.isReexported()) {
					buffer.append(" export=\"true\"");
				}
				buffer.append(" optional=\"true\"/>");
				result.add(buffer.toString());
			}
		} else if (needsAdditionalUIImport(model)) {
			result.add("<import plugin=\"org.eclipse.ui\"/>");
		}
		if (needsHelpBaseImport(model))
			result.add("<import plugin=\"org.eclipse.help.base\"/>");
		
		return (String[]) result.toArray(new String[result.size()]);
	}

	private void addNewImports(IDocument document, FindReplaceDocumentAdapter adapter, String[] imports) {
		try {
			if (imports.length == 0)
				return;
			
			String space = "";
			IRegion requiresRegion = adapter.search(0, "<requires>", true, false, false, false);
			if (requiresRegion != null) {
				IRegion spacerRegion = adapter.search(requiresRegion.getOffset() + requiresRegion.getLength(), "\\s*", true, true, false, true);
				if (spacerRegion != null) {
					space = document.get(spacerRegion.getOffset(), spacerRegion.getLength());
				}
			}
			StringBuffer buffer = new StringBuffer(space);
			for (int i = 0; i < imports.length; i++) {
				buffer.append(imports[i] + space);
			}
			adapter.replace(buffer.toString(), false);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}
	
	private boolean needsAdditionalUIImport(IPluginModelBase model) {
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			if (ExtensionPointMappings.hasMovedFromHelpToUI(extensions[i].getPoint())
				&& findImport(model, "org.eclipse.ui") == null)
				return true;
		}
		return false;
	}

	private boolean needsHelpBaseImport(IPluginModelBase model) {
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			if (ExtensionPointMappings.hasMovedFromHelpToBase(extensions[i].getPoint())
				&& findImport(model, "org.eclipse.help.base") == null) {
				return true;
			}
		}
		return false;
	}
	
	private IPluginImport findImport(IPluginModelBase model, String importID) {
		IPluginImport[] imports = model.getPluginBase().getImports();
		for (int i = 0; i < imports.length; i++) {
			if (imports[i].getId().equals(importID)) {
				return imports[i];
			}			
		}
		return null;
	}
	
}
