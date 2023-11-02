/*******************************************************************************
 * Copyright (c) 2005, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 268363
 *     Rapicorp Corporation - ongoing enhancements
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 547322
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import java.io.File;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProduct.ProductType;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.ILauncherFormPageHelper;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDELauncherFormEditor;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.FileStoreEditorInput;

public class ProductEditor extends PDELauncherFormEditor {

	private ProductExportAction fExportAction;
	private ILauncherFormPageHelper fLauncherHelper;

	public ProductEditor() {
		super();
	}

	@Override
	protected String getEditorID() {
		return IPDEUIConstants.PRODUCT_EDITOR_ID;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	@Override
	public String getContextIDForSaveAs() {
		return ProductInputContext.CONTEXT_ID;
	}

	@Override
	protected InputContextManager createInputContextManager() {
		return new ProductInputContextManager(this);
	}

	@Override
	protected void createResourceContexts(InputContextManager manager, IFileEditorInput input) {
		manager.putContext(input, new ProductInputContext(this, input, true));
		manager.monitorFile(input.getFile());
	}

	@Override
	protected void createSystemFileContexts(InputContextManager manager, FileStoreEditorInput input) {
		File file = new File(input.getURI());
		String name = file.getName();
		if (name.endsWith(".product")) { //$NON-NLS-1$
			IFileStore store;
			try {
				store = EFS.getStore(file.toURI());
				IEditorInput in = new FileStoreEditorInput(store);
				manager.putContext(in, new ProductInputContext(this, in, true));
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}

	@Override
	protected void createStorageContexts(InputContextManager manager, IStorageEditorInput input) {
		if (input.getName().endsWith(".product")) { //$NON-NLS-1$
			manager.putContext(input, new ProductInputContext(this, input, true));
		}
	}

	@Override
	protected ISortableContentOutlinePage createContentOutline() {
		return new ProductOutlinePage(this);
	}

	@Override
	protected PDESourcePage createSourcePage(PDEFormEditor editor, String title, String name, String contextId) {
		return new ProductSourcePage(editor, title, name);
	}

	@Override
	protected InputContext getInputContext(Object object) {
		return fInputContextManager.findContext(ProductInputContext.CONTEXT_ID);
	}

	@Override
	protected void addEditorPages() {
		try {
			addPage(new OverviewPage(this));
			addPage(new DependenciesPage(this, getProductType()));
			addPage(new ConfigurationPage(this, false));
			addPage(new LaunchingPage(this));
			addPage(new SplashPage(this));
			addPage(new BrandingPage(this));
			addPage(new CustomizationPage(this));
			addPage(new LicensingPage(this));
			addPage(new UpdatesPage(this));
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
		addSourcePage(ProductInputContext.CONTEXT_ID);
	}

	public void updateConfigurationPage() {
		try {
			removePage(1);
			addPage(1, new DependenciesPage(this, getProductType()));
		} catch (PartInitException e) {
		}
	}

	public ProductType getProductType() {
		IBaseModel model = getAggregateModel();
		return model != null ? ((IProductModel) model).getProduct().getType() : null;
	}

	@Override
	public void editorContextAdded(InputContext context) {
		addSourcePage(context.getId());
	}

	@Override
	public void contextRemoved(InputContext context) {
		close(false);
	}

	@Override
	public void monitoredFileAdded(IFile monitoredFile) {
	}

	@Override
	public boolean monitoredFileRemoved(IFile monitoredFile) {
		return true;
	}

	@Override
	public void contributeToToolbar(IToolBarManager manager) {
		contributeLaunchersToToolbar(manager);
		manager.add(getExportAction());
		if (getAggregateModel() instanceof IProductModel productModel) {
			productModel.addModelChangedListener(e -> {
				if (IProduct.P_TYPE.equals(e.getChangedProperty())) {
					setExportActionState();
				}
			});
			manager.add(new ProductValidateAction(productModel.getProduct()));
		}
	}

	private ProductExportAction getExportAction() {
		if (fExportAction == null) {
			fExportAction = new ProductExportAction(this);
			fExportAction.setImageDescriptor(PDEPluginImages.DESC_EXPORT_PRODUCT_TOOL);
			setExportActionState();
		}
		return fExportAction;
	}

	private void setExportActionState() {
		if (getAggregateModel() instanceof IProductModel productModel) {
			boolean isMixed = productModel.getProduct().getType() == ProductType.MIXED;
			fExportAction.setEnabled(!isMixed);
			fExportAction.setToolTipText(isMixed //
					? PDEUIMessages.ProductEditor_exportTooltip_mixed
					: PDEUIMessages.ProductEditor_exportTooltip);
		}
	}

	@Override
	protected ILauncherFormPageHelper getLauncherHelper() {
		if (fLauncherHelper == null)
			fLauncherHelper = new ProductLauncherFormPageHelper(this);
		return fLauncherHelper;
	}
}
