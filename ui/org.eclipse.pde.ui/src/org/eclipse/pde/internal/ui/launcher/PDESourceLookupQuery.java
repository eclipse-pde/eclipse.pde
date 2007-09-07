/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.jdt.debug.core.IJavaClassType;
import org.eclipse.jdt.debug.core.IJavaFieldVariable;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaReferenceType;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatformHelper;

public class PDESourceLookupQuery implements ISafeRunnable {
	
	protected static String OSGI_CLASSLOADER = "org.eclipse.osgi.internal.baseadaptor.DefaultClassLoader"; //$NON-NLS-1$
	private static String LEGACY_ECLIPSE_CLASSLOADER = "org.eclipse.core.runtime.adaptor.EclipseClassLoader"; //$NON-NLS-1$
	private static String MAIN_CLASS = "org.eclipse.core.launcher.Main"; //$NON-NLS-1$
	private static String MAIN_PLUGIN = "org.eclipse.platform"; //$NON-NLS-1$
	
	private Object fElement;
	private Object fResult;
	private PDESourceLookupDirector fDirector;
	
	public PDESourceLookupQuery(PDESourceLookupDirector director, Object object) {
		fElement = object;		
		fDirector = director;
	}

	public void handleException(Throwable exception) {
	}

	public void run() throws Exception {
		IJavaObject classLoaderObject = null;
		String declaringTypeName = null;
		String sourcePath = null;
		if (fElement instanceof IJavaStackFrame) {
			IJavaStackFrame stackFrame = (IJavaStackFrame)fElement;
			classLoaderObject = stackFrame.getReferenceType().getClassLoaderObject();
			declaringTypeName = stackFrame.getDeclaringTypeName();
			sourcePath = generateSourceName(declaringTypeName);
		} else if (fElement instanceof IJavaObject){
			IJavaObject object = (IJavaObject)fElement;
			IJavaReferenceType type = (IJavaReferenceType)object.getJavaType();
			classLoaderObject = type.getClassLoaderObject();
			if (object.getJavaType() != null){
				declaringTypeName = object.getJavaType().getName();
			}
			if (declaringTypeName != null){
				sourcePath = generateSourceName(declaringTypeName);
			}	
		} else if (fElement instanceof IJavaReferenceType){
			IJavaReferenceType type = (IJavaReferenceType)fElement;
			classLoaderObject = type.getClassLoaderObject();
			declaringTypeName = type.getName();
			sourcePath = generateSourceName(declaringTypeName);
		}
			
		if (classLoaderObject != null) {
			IJavaClassType type = (IJavaClassType)classLoaderObject.getJavaType();
			if (OSGI_CLASSLOADER.equals(type.getName())) {
				fResult = findSourceElement(classLoaderObject, sourcePath);
			} else if (LEGACY_ECLIPSE_CLASSLOADER.equals(type.getName())) {
				fResult = findSourceElement_legacy(classLoaderObject, sourcePath);		
			} else if (MAIN_CLASS.equals(declaringTypeName)){
				IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(MAIN_PLUGIN);
				if (model != null)
					fResult = getSourceElement(model.getInstallLocation(), MAIN_PLUGIN, sourcePath);
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
	
	protected Object findSourceElement(IJavaObject object, String typeName) throws CoreException {
		IJavaObject manager = getObject(object, "manager", false); //$NON-NLS-1$
		if (manager != null) {
			IJavaObject data = getObject(manager, "data", false); //$NON-NLS-1$
			if (data != null) {
				String location = getValue(data, "fileName"); //$NON-NLS-1$
				String id = getValue(data, "symbolicName"); //$NON-NLS-1$
				return getSourceElement(location, id, typeName);			
			}
		}	
		return null;
	}
	
	private IJavaObject getObject(IJavaObject object, String field, boolean superfield) throws DebugException {
		IJavaFieldVariable variable = object.getField(field, superfield);
		if (variable != null) {
			IValue value = variable.getValue();
			if (value instanceof IJavaObject) 
				return (IJavaObject)value;
		}
		return null;
	}
	
	private Object findSourceElement_legacy(IJavaObject object, String typeName) throws CoreException {
		IJavaObject hostdata = getObject(object, "hostdata", true); //$NON-NLS-1$
		if (hostdata != null) {
			String location = getValue(hostdata, "fileName"); //$NON-NLS-1$
			String id = getValue(hostdata, "symbolicName"); //$NON-NLS-1$
			return getSourceElement(location, id, typeName);
		}
		return null;
	}
	
	private Object getSourceElement(String location, String id, String typeName) throws CoreException {
		if (location != null && id != null) {
			Object result = findSourceElement(getSourceContainers(location, id), typeName);
			if (result != null)
				return result;
			
			// don't give up yet, search fragments attached to this host
			State state = TargetPlatformHelper.getState();
			BundleDescription desc = state.getBundle(id, null);
			if (desc != null) {
				BundleDescription[] fragments = desc.getFragments();
				for (int i = 0; i < fragments.length; i++) {
					location = fragments[i].getLocation();
					id = fragments[i].getSymbolicName();
					result = findSourceElement(getSourceContainers(location, id), typeName);
					if (result != null)
						return result;
				}
			}
		}
		return null;
	}
	
	private Object findSourceElement(ISourceContainer[] containers, String typeName) throws CoreException {
		for (int i = 0; i < containers.length; i++) {
			Object[] result = containers[i].findSourceElements(typeName);
			if (result.length > 0)
				return result[0];
		}
		return null;
	}
	
	protected ISourceContainer[] getSourceContainers(String location, String id) throws CoreException {
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

}