/*******************************************************************************
 * Copyright (c) 2005, 2025 IBM Corporation and others.
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
 *     Tue Ton - support for FreeBSD
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.product;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IExtensionsModelFactory;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.pde.internal.ui.wizards.templates.ControlStack;
import org.eclipse.pde.ui.templates.IVariableProvider;
import org.eclipse.swt.widgets.Shell;

public class ProductIntroOperation extends BaseManifestOperation implements IVariableProvider {

	protected String fIntroId;
	private Shell fShell;
	private final IProduct fProduct;
	private final IProject fProject;
	private static final String INTRO_POINT = "org.eclipse.ui.intro"; //$NON-NLS-1$
	private static final String INTRO_CONFIG_POINT = "org.eclipse.ui.intro.config"; //$NON-NLS-1$
	private static final String INTRO_CLASS = "org.eclipse.ui.intro.config.CustomizableIntroPart"; //$NON-NLS-1$
	private static final String KEY_PRODUCT_NAME = "productName"; //$NON-NLS-1$

	public ProductIntroOperation(IProduct product, String pluginId, String introId, Shell shell) {
		super(shell, pluginId);
		fIntroId = introId;
		fProduct = product;
		fProject = PluginRegistry.findModel(pluginId).getUnderlyingResource().getProject();
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		try {
			IFile file = getFile();
			if (!file.exists()) {
				createNewFile(file);
			} else {
				modifyExistingFile(file, monitor);
			}
			updateSingleton(monitor);
			generateFiles(monitor);
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
	}

	private void createNewFile(IFile file) throws CoreException {
		WorkspacePluginModelBase model = (WorkspacePluginModelBase) getModel(file);
		IPluginBase base = model.getPluginBase();
		base.setSchemaVersion(TargetPlatformHelper.getSchemaVersion());
		base.add(createIntroExtension(model));
		base.add(createIntroConfigExtension(model));
		model.save();
	}

	private IPluginExtension createIntroExtension(IPluginModelBase model) throws CoreException {
		IPluginExtension extension = model.getFactory().createExtension();
		extension.setPoint(INTRO_POINT);
		extension.add(createIntroExtensionContent(extension));
		extension.add(createIntroBindingExtensionContent(extension));
		return extension;
	}

	private IPluginExtension createIntroConfigExtension(IPluginModelBase model) throws CoreException {
		IPluginExtension extension = model.getFactory().createExtension();
		extension.setPoint(INTRO_CONFIG_POINT);
		extension.add(createIntroConfigExtensionContent(extension));
		return extension;
	}

	private IPluginElement createIntroExtensionContent(IPluginExtension extension) throws CoreException {
		IPluginElement element = extension.getModel().getFactory().createElement(extension);
		element.setName("intro"); //$NON-NLS-1$
		element.setAttribute("id", fIntroId); //$NON-NLS-1$
		element.setAttribute("class", INTRO_CLASS); //$NON-NLS-1$
		return element;
	}

	private IPluginElement createIntroBindingExtensionContent(IPluginExtension extension) throws CoreException {
		IPluginElement element = extension.getModel().getFactory().createElement(extension);
		element.setName("introProductBinding"); //$NON-NLS-1$
		element.setAttribute("productId", fProduct.getProductId()); //$NON-NLS-1$
		element.setAttribute("introId", fIntroId); //$NON-NLS-1$
		return element;
	}

	private IPluginElement createIntroConfigExtensionContent(IPluginExtension extension) throws CoreException {
		IPluginElement element = extension.getModel().getFactory().createElement(extension);
		element.setName("config"); //$NON-NLS-1$
		element.setAttribute("id", fPluginId + ".introConfigId"); //$NON-NLS-1$ //$NON-NLS-2$
		element.setAttribute("introId", fIntroId); //$NON-NLS-1$
		element.setAttribute("content", "introContent.xml"); //$NON-NLS-1$ //$NON-NLS-2$
		element.add(createPresentationElement(element));

		return element;
	}

	private IPluginElement createPresentationElement(IPluginElement parent) throws CoreException {
		IExtensionsModelFactory factory = parent.getModel().getFactory();

		IPluginElement presentation = factory.createElement(parent);
		presentation.setName("presentation"); //$NON-NLS-1$
		presentation.setAttribute("home-page-id", "root"); //$NON-NLS-1$ //$NON-NLS-2$

		IPluginElement implementation = factory.createElement(presentation);
		implementation.setName("implementation"); //$NON-NLS-1$
		implementation.setAttribute("kind", "html"); //$NON-NLS-1$ //$NON-NLS-2$
		implementation.setAttribute("style", "content/shared.css"); //$NON-NLS-1$ //$NON-NLS-2$
		implementation.setAttribute("os", "win32,linux,freebsd,macosx"); //$NON-NLS-1$ //$NON-NLS-2$

		presentation.add(implementation);

		return presentation;
	}

	private void modifyExistingFile(IFile file, IProgressMonitor monitor) throws CoreException {
		IStatus status = PDEPlugin.getWorkspace().validateEdit(new IFile[] {file}, fShell);
		if (!status.isOK()) {
			throw new CoreException(Status.error(NLS.bind(PDEUIMessages.ProductDefinitionOperation_readOnly, fPluginId)));
		}

		ModelModification mod = new ModelModification(file) {
			@Override
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (!(model instanceof IPluginModelBase pluginModel)) {
					return;
				}
				IPluginExtension extension = getExtension(pluginModel, INTRO_POINT);
				if (extension == null) {
					extension = createIntroExtension(pluginModel);
					pluginModel.getPluginBase().add(extension);
				} else {
					extension.add(createIntroExtensionContent(extension));
					extension.add(createIntroBindingExtensionContent(extension));
				}

				extension = getExtension(pluginModel, INTRO_CONFIG_POINT);
				if (extension == null) {
					extension = createIntroConfigExtension(pluginModel);
					pluginModel.getPluginBase().add(extension);
				} else {
					extension.add(createIntroConfigExtensionContent(extension));
				}
			}
		};
		PDEModelUtility.modifyModel(mod, monitor);
	}

	private IPluginExtension getExtension(IPluginModelBase model, String tPoint) {
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		for (IPluginExtension extension : extensions) {
			String point = extension.getPoint();
			if (tPoint.equals(point)) {
				return extension;
			}
		}
		return null;
	}

	protected void generateFiles(IProgressMonitor monitor) throws CoreException {
		monitor.setTaskName(PDEUIMessages.AbstractTemplateSection_generating);

		URL locationUrl = null;
		try {
			locationUrl = new URL(PDEPlugin.getDefault().getInstallURL(), "templates_3.1/intro/"); //$NON-NLS-1$
		} catch (MalformedURLException e1) {
			return;
		}
		try {
			locationUrl = FileLocator.resolve(locationUrl);
			locationUrl = FileLocator.toFileURL(locationUrl);
		} catch (IOException e) {
			return;
		}
		if ("file".equals(locationUrl.getProtocol())) { //$NON-NLS-1$
			File templateDirectory = new File(locationUrl.getFile());
			if (!templateDirectory.exists()) {
				return;
			}
			generateFiles(templateDirectory, fProject, true, false, monitor);
		}
		monitor.subTask(""); //$NON-NLS-1$
		monitor.worked(1);
	}

	private void generateFiles(File src, IContainer dst, boolean firstLevel, boolean binary, IProgressMonitor monitor) throws CoreException {
		File[] members = src.listFiles();

		for (File member : members) {
			if (member.getName().equals("ext.xml") || //$NON-NLS-1$
					member.getName().equals("java") || //$NON-NLS-1$
					member.getName().equals("concept3.xhtml") || //$NON-NLS-1$
					member.getName().equals("extContent.xhtml")) { //$NON-NLS-1$
				continue;
			} else if (member.isDirectory()) {
				IContainer dstContainer = null;
				if (firstLevel) {
					binary = false;
					if (member.getName().equals("bin")) { //$NON-NLS-1$
						binary = true;
						dstContainer = dst;
					}
				}
				if (dstContainer == null) {
					dstContainer = dst.getFolder(IPath.fromOSString(member.getName()));
				}
				if (dstContainer instanceof IFolder && !dstContainer.exists()) {
					((IFolder) dstContainer).create(true, true, monitor);
				}
				generateFiles(member, dstContainer, false, binary, monitor);
			} else {
				if (firstLevel) {
					binary = false;
				}
				try (InputStream in = new FileInputStream(member);) {
					copyFile(member.getName(), in, dst, binary, monitor);
				} catch (IOException ioe) {
				}
			}
		}
	}

	private void copyFile(String fileName, InputStream input, IContainer dst, boolean binary, IProgressMonitor monitor) throws CoreException {

		monitor.subTask(fileName);
		IFile dstFile = dst.getFile(IPath.fromOSString(fileName));

		try (InputStream stream = getProcessedStream(fileName, input, binary)) {
			if (dstFile.exists()) {
				dstFile.setContents(stream, true, true, monitor);
			} else {
				dstFile.create(stream, true, monitor);
			}
		} catch (IOException e) {
		}
	}

	private InputStream getProcessedStream(String fileName, InputStream stream, boolean binary) throws IOException, CoreException {
		if (binary) {
			return stream;
		}

		InputStreamReader reader = new InputStreamReader(stream);
		int bufsize = 1024;
		char[] cbuffer = new char[bufsize];
		int read = 0;
		StringBuilder keyBuffer = new StringBuilder();
		StringBuilder outBuffer = new StringBuilder();
		ControlStack preStack = new ControlStack();
		preStack.setValueProvider(this);

		boolean replacementMode = false;
		while (read != -1) {
			read = reader.read(cbuffer);
			for (int i = 0; i < read; i++) {
				char c = cbuffer[i];

				if (preStack.getCurrentState() == false) {
					continue;
				}

				if (c == '$') {
					if (replacementMode) {
						replacementMode = false;
						String key = keyBuffer.toString();
						String value = key.length() == 0 ? "$" //$NON-NLS-1$
								: getReplacementString(fileName, key);
						outBuffer.append(value);
						keyBuffer.delete(0, keyBuffer.length());
					} else {
						replacementMode = true;
					}
				} else {
					if (replacementMode) {
						keyBuffer.append(c);
					} else {
						outBuffer.append(c);
					}
				}
			}
		}
		return new ByteArrayInputStream(outBuffer.toString().getBytes(fProject.getDefaultCharset()));
	}

	private String getReplacementString(String fileName, String key) {
		if (key.equals(KEY_PRODUCT_NAME)) {
			return fProduct.getName();
		}
		return key;
	}

	@Override
	public Object getValue(String variable) {
		return null;
	}
}
