/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
 *     Stephan Herrmann - Bug 242461 - JSR045 support
 *******************************************************************************/
package org.eclipse.pde.internal.launching.sourcelookup;

import com.sun.jdi.VMDisconnectedException;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.*;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.jdt.debug.core.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatformHelper;

public class PDESourceLookupQuery implements ISafeRunnable {

	private static final String OSGI_CLASSLOADER = "org.eclipse.osgi.internal.baseadaptor.DefaultClassLoader"; //$NON-NLS-1$
	/**
	 * Support for refactored bundle class loaders (bug 402005)
	 */
	private static final String OSGI_LUNA_CLASSLOADER1 = "org.eclipse.osgi.internal.loader.ModuleClassLoader"; //$NON-NLS-1$
	private static final String OSGI_LUNA_CLASSLOADER2 = "org.eclipse.osgi.internal.loader.EquinoxClassLoader"; //$NON-NLS-1$
	private static final String OSGI_CURRENT_CLASSLOADER;
	static {
		ClassLoader loader = PDESourceLookupQuery.class.getClassLoader();
		if (loader != null) {
			OSGI_CURRENT_CLASSLOADER = loader.getClass().getName();
		} else {
			OSGI_CURRENT_CLASSLOADER = "unknown"; //$NON-NLS-1$
		}
	}

	private static final String LEGACY_ECLIPSE_CLASSLOADER = "org.eclipse.core.runtime.adaptor.EclipseClassLoader"; //$NON-NLS-1$
	private static final String LEGACY_MAIN_CLASS = "org.eclipse.core.launcher.Main"; //$NON-NLS-1$
	private static final String LEGACY_MAIN_PLUGIN = "org.eclipse.platform"; //$NON-NLS-1$
	private static final String MAIN_CLASS = "org.eclipse.equinox.launcher.Main"; //$NON-NLS-1$
	private static final String LAUNCHER_PLUGIN = "org.eclipse.equinox.launcher"; //$NON-NLS-1$
	private static final String STARTUP_CLASSLOADER = "org.eclipse.equinox.launcher.Main$StartupClassLoader"; //$NON-NLS-1$
	private static final String OSGI_FRAMEWORK_PLUGIN = "org.eclipse.osgi"; //$NON-NLS-1$

	private Object fElement;
	private Object fResult;
	private PDESourceLookupDirector fDirector;

	public PDESourceLookupQuery(PDESourceLookupDirector director, Object object) {
		fElement = object;
		fDirector = director;
	}

	@Override
	public void handleException(Throwable exception) {
	}

	@Override
	public void run() throws Exception {
		try {
			findSourceElement();
		} catch (DebugException e) {
			boolean isDebugTargetRunning = isDebugTargetRunning();
			if (isDebugTargetRunning) {
				throw e;
			}
			boolean isVmDisconnectException = e.getCause() instanceof VMDisconnectedException;
			if (!isVmDisconnectException) {
				throw e;
			}
		}
	}

	private void findSourceElement() throws Exception {
		IJavaReferenceType declaringType = null;
		String sourcePath = null;
		if (fElement instanceof IJavaStackFrame) {
			IJavaStackFrame stackFrame = (IJavaStackFrame) fElement;
			declaringType = stackFrame.getReferenceType();
			// under JSR 45 source path from the stack frame is more precise than anything derived from the type:
			sourcePath = stackFrame.getSourcePath();
		} else if (fElement instanceof IJavaObject) {
			IJavaType javaType = ((IJavaObject) fElement).getJavaType();
			if (javaType instanceof IJavaReferenceType) {
				declaringType = (IJavaReferenceType) javaType;
			}
		} else if (fElement instanceof IJavaReferenceType) {
			declaringType = (IJavaReferenceType) fElement;
		}
		if (declaringType != null) {
			IJavaObject classLoaderObject = declaringType.getClassLoaderObject();
			String declaringTypeName = declaringType.getName();
			if (sourcePath == null) {
				String[] sourcePaths = declaringType.getSourcePaths(null);
				if (sourcePaths != null) {
					sourcePath = sourcePaths[0];
				}
				if (sourcePath == null) {
					sourcePath = generateSourceName(declaringTypeName);
				}
			}

			if (classLoaderObject != null) {
				IJavaClassType type = (IJavaClassType) classLoaderObject.getJavaType();
				String classLoaderName = type.getName();
				if (OSGI_CLASSLOADER.equals(classLoaderName)) {
					if (fDirector.getOSGiRuntimeVersion() < 3.5) {
						fResult = findSourceElement34(classLoaderObject, sourcePath);
					} else {
						fResult = findSourceElement(classLoaderObject, sourcePath);
					}
				} else if (OSGI_LUNA_CLASSLOADER1.equals(classLoaderName) || OSGI_LUNA_CLASSLOADER2.equals(classLoaderName) || OSGI_CURRENT_CLASSLOADER.equals(classLoaderName)) {
					fResult = findSourceElement(classLoaderObject, sourcePath);
				} else if (LEGACY_ECLIPSE_CLASSLOADER.equals(classLoaderName)) {
					fResult = findSourceElement_legacy(classLoaderObject, sourcePath);
				} else if (STARTUP_CLASSLOADER.equals(classLoaderName)) {
					fResult = findSourceElementInModel(OSGI_FRAMEWORK_PLUGIN, sourcePath);
				} else if (MAIN_CLASS.equals(declaringTypeName)) {
					fResult = findSourceElementInModel(LAUNCHER_PLUGIN, sourcePath);
				} else if (LEGACY_MAIN_CLASS.equals(declaringTypeName)) {
					fResult = findSourceElementInModel(LEGACY_MAIN_PLUGIN, sourcePath);
				}
			} else {
				// declaringType was loaded by bootstrap classloader --> part of JRE
				fResult = findJreSourceElement(sourcePath);
			}
		}
	}

	protected Object getResult() {
		return fResult;
	}

	private String getValue(IJavaObject object, String variable) throws DebugException {
		IJavaFieldVariable var = object.getField(variable, false);
		return var == null ? null : var.getValue().getValueString();
	}

	/**
	 * Finds a source element in a 3.4 OSGi runtime.
	 *
	 * @param object Bundle class loader object
	 * @param typeName fully qualified name of the source type being searched for
	 * @return source element
	 */
	protected Object findSourceElement34(IJavaObject object, String typeName) throws CoreException {
		IJavaObject manager = getObject(object, "manager", false); //$NON-NLS-1$
		if (manager != null) {
			IJavaObject data = getObject(manager, "data", false); //$NON-NLS-1$
			if (data != null) {
				String location = getValue(data, "fileName"); //$NON-NLS-1$
				String id = getValue(data, "symbolicName"); //$NON-NLS-1$
				return getSourceElement(location, id, typeName, true);
			}
		}
		return null;
	}

	/**
	 * Finds source in a 3.5 runtime. In 3.5, the OSGi runtime provides hooks to properly
	 * lookup source in fragments that replace/prepend jars in their host.
	 *
	 * @param object Bundle class loader object
	 * @param typeName fully qualified name of the source type being searched for
	 * @return source element
	 */
	protected Object findSourceElement(IJavaObject object, String typeName) throws CoreException {
		IJavaObject manager = getObject(object, "manager", false); //$NON-NLS-1$
		if (manager != null) {
			// search manager's class path for location
			Object result = searchClasspathEntries(manager, typeName);
			if (result != null) {
				return result;
			}
			// then check its fragments
			IJavaObject frgArray = getObject(manager, "fragments", false); //$NON-NLS-1$
			if (frgArray instanceof IJavaArray) {
				IJavaArray fragments = (IJavaArray) frgArray;
				for (int i = 0; i < fragments.getLength(); i++) {
					IJavaObject fragment = (IJavaObject) fragments.getValue(i);
					if (!fragment.isNull()) {
						// search fragment class path
						result = searchClasspathEntries(fragment, typeName);
						if (result != null) {
							return result;
						}
					}

				}
			}
		}
		return null;
	}

	/**
	 * Search a bundle's class path entries for source for the type of given name.
	 * This is used for 3.5 and greater.
	 *
	 * @param entriesOwner the java object providing the classpath entries
	 * @param typeName qualified name of source
	 * @return source object or <code>null</code>
	 */
	private Object searchClasspathEntries(IJavaObject entriesOwner, String typeName) throws CoreException {
		IJavaObject cpeArray = getObject(entriesOwner, "entries", false); //$NON-NLS-1$
		if (cpeArray instanceof IJavaArray) {
			IJavaArray entries = (IJavaArray) cpeArray;
			for (int i = 0; i < entries.getLength(); i++) {
				IJavaObject entry = (IJavaObject) entries.getValue(i);
				if (!entry.isNull()) {
					IJavaObject baseData = getObject(entry, "data", false); //$NON-NLS-1$
					if (baseData != null && !baseData.isNull()) {
						IJavaObject fileName = getObject(baseData, "fileName", false); //$NON-NLS-1$
						if (fileName != null && !fileName.isNull()) {
							String location = fileName.getValueString();
							String symbolicName = getValue(baseData, "symbolicName"); //$NON-NLS-1$
							Object el = getSourceElement(location, symbolicName, typeName, false);
							if (el != null) {
								return el;
							}
						}
					}
				}
			}
		}
		return null;
	}

	private IJavaObject getObject(IJavaObject object, String field, boolean superfield) throws DebugException {
		IJavaFieldVariable variable = object.getField(field, superfield);
		if (variable != null) {
			IValue value = variable.getValue();
			if (value instanceof IJavaObject)
				return (IJavaObject) value;
		}
		return null;
	}

	private Object findSourceElement_legacy(IJavaObject object, String typeName) throws CoreException {
		IJavaObject hostdata = getObject(object, "hostdata", true); //$NON-NLS-1$
		if (hostdata != null) {
			String location = getValue(hostdata, "fileName"); //$NON-NLS-1$
			String id = getValue(hostdata, "symbolicName"); //$NON-NLS-1$
			return getSourceElement(location, id, typeName, true);
		}
		return null;
	}

	private Object findSourceElementInModel(String modelId, String sourcePath) throws CoreException {
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(modelId);
		if (model == null) {
			return null;
		}

		return getSourceElement(model.getInstallLocation(), modelId, sourcePath, true);
	}

	/**
	 * Looks up source in the source containers associated with the bundle at the given location.
	 * Searches associated fragments if source is not found in that location only if
	 * <code>chechFragments</code> is <code>true</code> (which should only be done when < 3.5,
	 * as this is just a guess in random order).
	 *
	 * @param location location of bundle jar / class file folder
	 * @param id symbolic name of bundle or fragment
	 * @param typeName qualified name of source
	 * @param checkFragments whether to guess at fragments
	 * @return source element or <code>null</code>
	 */
	private Object getSourceElement(String location, String id, String typeName, boolean checkFragments) throws CoreException {
		if (location != null && id != null) {
			Object result = findSourceElement(getSourceContainers(location, id), typeName);
			if (result != null)
				return result;

			// don't give up yet, search fragments attached to this host
			if (checkFragments) {
				State state = TargetPlatformHelper.getState();
				BundleDescription desc = state.getBundle(id, null);
				if (desc != null) {
					BundleDescription[] fragments = desc.getFragments();
					for (BundleDescription fragment : fragments) {
						location = fragment.getLocation();
						id = fragment.getSymbolicName();
						result = findSourceElement(getSourceContainers(location, id), typeName);
						if (result != null)
							return result;
					}
				}
			}
		}
		return null;
	}

	private Object findJreSourceElement(String sourcePath) throws CoreException {
		List<ISourceContainer> jreSourceContainers = Arrays.asList(fDirector.getJreSourceContainers());
		return findSourceElement(jreSourceContainers, sourcePath);
	}

	private Object findSourceElement(List<ISourceContainer> containers, String typeName) throws CoreException {
		for (ISourceContainer container : containers) {
			Object[] result = container.findSourceElements(typeName);
			if (result.length > 0)
				return result[0];
		}
		return null;
	}

	protected List<ISourceContainer> getSourceContainers(String location, String id) throws CoreException {
		return fDirector.getSourceContainers(location, id);
	}

	/**
	 * Generates and returns a source file path based on a qualified type name.
	 * For example, when <code>java.lang.String</code> is provided,
	 * the returned source name is <code>java/lang/String.java</code>.
	 *
	 * @param qualifiedTypeName fully qualified type name that may contain inner types
	 *  denoted with <code>$</code> character
	 * @return a source file path corresponding to the type name
	 */
	private static String generateSourceName(String qualifiedTypeName) {
		int index = qualifiedTypeName.indexOf('$');
		if (index >= 0)
			qualifiedTypeName = qualifiedTypeName.substring(0, index);
		return qualifiedTypeName.replace('.', File.separatorChar) + ".java"; //$NON-NLS-1$
	}

	private boolean isDebugTargetRunning() {
		boolean isDebugTargetRunning = false;
		if (fElement instanceof IDebugElement) {
			IDebugElement debugElement = (IDebugElement) fElement;
			IDebugTarget debugTarget = debugElement.getDebugTarget();
			isDebugTargetRunning = debugTarget != null && !debugTarget.isDisconnected() && !debugTarget.isTerminated();
		}
		return isDebugTargetRunning;
	}

}