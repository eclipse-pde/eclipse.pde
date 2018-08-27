/*******************************************************************************
 * Copyright (c) 2015 Rapicorp Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Rapicorp Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.BidiUtils;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.iproduct.ICSSInfo;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.util.FileExtensionsFilter;
import org.eclipse.pde.internal.ui.util.FileValidator;
import org.eclipse.pde.internal.ui.wizards.product.SynchronizationOperation;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.progress.IProgressService;

public class CSSSection extends PDESection {
	private FormEntry fFileEntry;

	public CSSSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	@Override
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setLayout(FormLayoutFactory.createClearTableWrapLayout(false, 1));
		TableWrapData data = new TableWrapData(TableWrapData.FILL_GRAB);
		section.setLayoutData(data);

		section.setText(PDEUIMessages.CSSSection_title);
		section.setDescription(PDEUIMessages.CSSSection_description);

		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientTableWrapLayout(false, 1));
		client.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();

		Composite child = toolkit.createComposite(client);
		child.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 3));
		child.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		fFileEntry = new FormEntry(child, toolkit, PDEUIMessages.CSSSection_file, PDEUIMessages.CSSSection_browse, false);
		BidiUtils.applyBidiProcessing(fFileEntry.getText(), StructuredTextTypeHandlerFactory.FILE);
		fFileEntry.setEditable(isEditable());
		fFileEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {

			@Override
			public void textValueChanged(FormEntry entry) {
				getCSSInfo().setFilePath(entry.getValue());
			}

			@Override
			public void browseButtonSelected(FormEntry entry) {
				handleBrowse();
			}
		});

		FormText text = toolkit.createFormText(client, true);
		text.setText(PDEUIMessages.CSSSection_synchronize, true, true);
		data = new TableWrapData(TableWrapData.FILL_GRAB);
		text.setLayoutData(data);

		text.addHyperlinkListener(new IHyperlinkListener() {
			@Override
			public void linkEntered(HyperlinkEvent e) {
				IStatusLineManager mng = getPage().getEditor().getEditorSite().getActionBars().getStatusLineManager();
				mng.setMessage(e.getLabel());
			}

			@Override
			public void linkExited(HyperlinkEvent e) {
				IStatusLineManager mng = getPage().getEditor().getEditorSite().getActionBars().getStatusLineManager();
				mng.setMessage(null);
			}

			@Override
			public void linkActivated(HyperlinkEvent e) {
				String href = (String) e.getHref();
				if (href.equals("command.synchronize")) { //$NON-NLS-1$
					handleSynchronize();
				}
			}
		});

		toolkit.paintBordersFor(client);
		section.setClient(client);
		// Register to be notified when the model changes
		getModel().addModelChangedListener(this);
	}

	private void handleBrowse() {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getSection().getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());

		dialog.setValidator(new FileValidator());
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEUIMessages.CSSSection_fileTitle);
		dialog.setMessage(PDEUIMessages.CSSSection_fileMessage);
		FileExtensionsFilter filter = new FileExtensionsFilter();
		filter.addFileExtension("css"); //$NON-NLS-1$
		dialog.addFilter(filter);
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());

		if (dialog.open() == Window.OK) {
			IFile file = (IFile) dialog.getFirstResult();
			fFileEntry.setValue(file.getFullPath().toString());
		}
	}


	void handleSynchronize() {
		// Before we synchronize to the plugin, we want to ensure that the CSS file is in the build.properties
		ProductEditor editor = (ProductEditor) getPage().getEditor();
		try {
			IProgressService service = PlatformUI.getWorkbench().getProgressService();
			// ensure the newly created CSS file is in the build.properties of the defining plug-in.
			IPluginModelBase model = PluginRegistry.findModel(getProduct().getDefiningPluginId());
			if (model == null) {
				MessageDialog.openError(getSection().getShell(), PDEUIMessages.CSSSection_errorNoDefiningPluginTitle, PDEUIMessages.CSSSection_errorNoDefiningPlugin);
				return;
			}
			IProject project = model.getUnderlyingResource().getProject();
			IFile buildProps = PDEProject.getBuildProperties(project);
			if (buildProps.exists()) {
				WorkspaceBuildModel wkspc = new WorkspaceBuildModel(buildProps);
				wkspc.load();
				if (wkspc.isLoaded()) {
					IBuildEntry entry = wkspc.getBuild().getEntry("bin.includes"); //$NON-NLS-1$
					if (entry == null) {
						entry = wkspc.getFactory().createEntry("bin.includes"); //$NON-NLS-1$
						wkspc.getBuild().add(entry);
					}
					IPath pth = new Path(getCSSInfo().getFilePath());
					String path = pth.removeFirstSegments(1).toString();
					if (!entry.contains(path))
						entry.addToken(path);
					wkspc.save();
				}
			}
			SynchronizationOperation op = new SynchronizationOperation(getProduct(), editor.getSite().getShell(), project);
			service.runInUI(service, op, PDEPlugin.getWorkspace().getRoot());
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			MessageDialog.openError(editor.getSite().getShell(), "Synchronize", e.getTargetException().getMessage()); //$NON-NLS-1$
		} catch (CoreException e) {
			PDEPlugin.logException(e, "Synchronize", null); //$NON-NLS-1$
		}
	}

	@Override
	public void refresh() {
		fFileEntry.setValue(getCSSInfo().getFilePath(), true);
		super.refresh();
	}

	@Override
	public void commit(boolean onSave) {
		fFileEntry.commit();
		super.commit(onSave);
	}

	@Override
	public void cancelEdit() {
		fFileEntry.cancelEdit();
		super.cancelEdit();
	}

	private ICSSInfo getCSSInfo() {
		ICSSInfo info = getProduct().getCSSInfo();
		if (info == null) {
			info = getModel().getFactory().createCSSInfo();
			getProduct().setCSSInfo(info);
		}
		return info;
	}

	private IProduct getProduct() {
		return getModel().getProduct();
	}

	private IProductModel getModel() {
		return (IProductModel) getPage().getPDEEditor().getAggregateModel();
	}

	@Override
	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		// No need to call super, handling world changed event here
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged(e);
		}
	}

	/**
	 * @param event
	 */
	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		refresh();
	}

	@Override
	public void dispose() {
		IProductModel model = getModel();
		if (model != null) {
			model.removeModelChangedListener(this);
		}
		super.dispose();
	}
}
