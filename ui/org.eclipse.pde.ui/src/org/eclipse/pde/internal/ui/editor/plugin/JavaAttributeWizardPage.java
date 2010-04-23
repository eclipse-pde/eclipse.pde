/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.ArrayList;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.wizards.NewClassWizardPage;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class JavaAttributeWizardPage extends NewClassWizardPage {
	private String className;
	private IProject project;
	private ISchemaAttribute attInfo;
	private IPluginModelBase model;
	private InitialClassProperties initialValues;
	private IJavaProject javaProject;

	class InitialClassProperties {
		// populate new wizard page
		IType superClassType;
		String superClassName;
		IType interfaceType;
		String interfaceName;
		String className;
		String classArgs;
		String packageName;
		IPackageFragmentRoot packageFragmentRoot;
		IPackageFragment packageFragment;

		public InitialClassProperties() {
			this.superClassType = null;
			this.superClassName = ""; //$NON-NLS-1$
			this.interfaceName = null;
			this.interfaceType = null;
			this.className = null;
			this.classArgs = null;
			this.packageName = null;
			this.packageFragment = null;
			this.packageFragmentRoot = null;
		}
	}

	public JavaAttributeWizardPage(IProject project, IPluginModelBase model, ISchemaAttribute attInfo, String className) {
		super();
		this.className = className;
		this.model = model;
		this.project = project;
		this.attInfo = attInfo;
		try {
			if (project.hasNature(JavaCore.NATURE_ID))
				this.javaProject = JavaCore.create(project);
			else
				this.javaProject = null;
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		initialValues = new InitialClassProperties();
		initialValues.className = className;
	}

	public Object getValue() {
		return new JavaAttributeValue(project, model, attInfo, className);
	}

	public void init() {
		initializeExpectedValues();
		initializeWizardPage();
	}

	protected void initializeWizardPage() {
		setPackageFragmentRoot(initialValues.packageFragmentRoot, true);
		setPackageFragment(initialValues.packageFragment, true);
		setEnclosingType(null, true);
		setEnclosingTypeSelection(false, true);
		setTypeName(initialValues.className, true);
		setSuperClass(initialValues.superClassName, true);
		if (initialValues.interfaceName != null) {
			ArrayList interfaces = new ArrayList();
			interfaces.add(initialValues.interfaceName);
			setSuperInterfaces(interfaces, true);
		}
		boolean hasSuperClass = initialValues.superClassName != null && initialValues.superClassName.length() > 0;
		boolean hasInterface = initialValues.interfaceName != null && initialValues.interfaceName.length() > 0;
		setMethodStubSelection(false, hasSuperClass, hasInterface || hasSuperClass, true);
	}

	private IType findTypeForName(String typeName) throws JavaModelException {
		if (typeName == null || typeName.length() == 0)
			return null;
		IType type = null;
		String fileName = typeName.replace('.', '/') + ".java"; //$NON-NLS-1$
		IJavaElement element = javaProject.findElement(new Path(fileName));
		if (element == null)
			return null;
		if (element instanceof IClassFile) {
			type = ((IClassFile) element).getType();
		} else if (element instanceof ICompilationUnit) {
			IType[] types = ((ICompilationUnit) element).getTypes();
			type = types[0];
		}
		return type;
	}

	private void initializeExpectedValues() {

		//			source folder name, package name, class name
		int loc = className.indexOf(":"); //$NON-NLS-1$
		if (loc != -1) {
			if (loc < className.length()) {
				initialValues.classArgs = className.substring(loc + 1, className.length());
				className = className.substring(0, loc);
			}
			if (loc > 0)
				initialValues.className = className.substring(0, loc);
			else if (loc == 0)
				initialValues.className = ""; //$NON-NLS-1$
		}

		loc = className.lastIndexOf('.');
		if (loc != -1) {
			initialValues.packageName = className.substring(0, loc);
			initialValues.className = className.substring(loc + 1);
		}
		if (javaProject == null)
			return;
		try {
			if (initialValues.packageFragmentRoot == null) {
				IPackageFragmentRoot srcEntryDft = null;
				IPackageFragmentRoot[] roots = javaProject.getPackageFragmentRoots();
				for (int i = 0; i < roots.length; i++) {
					if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE) {
						srcEntryDft = roots[i];
						break;
					}
				}
				if (srcEntryDft != null)
					initialValues.packageFragmentRoot = srcEntryDft;
				else {
					initialValues.packageFragmentRoot = javaProject.getPackageFragmentRoot(javaProject.getResource());
				}
				if (initialValues.packageFragment == null && initialValues.packageFragmentRoot != null && initialValues.packageName != null && initialValues.packageName.length() > 0) {
					IFolder packageFolder = project.getFolder(initialValues.packageName);
					initialValues.packageFragment = initialValues.packageFragmentRoot.getPackageFragment(packageFolder.getProjectRelativePath().toOSString());
				}
			}
			//			superclass and interface
			if (attInfo == null) {
				initialValues.interfaceName = "org.osgi.framework.BundleActivator"; //$NON-NLS-1$
				initialValues.interfaceType = findTypeForName(initialValues.interfaceName);
				IEclipsePreferences prefs = new ProjectScope(project).getNode(PDECore.PLUGIN_ID);
				if (prefs != null && !prefs.getBoolean(ICoreConstants.EXTENSIONS_PROPERTY, true)) {
					return;
				}
				if (model != null) {
					IPluginImport[] imports = model.getPluginBase().getImports();
					for (int i = 0; i < imports.length; i++) {
						if (imports[i].getId().equals("org.eclipse.ui")) { //$NON-NLS-1$
							initialValues.superClassName = "org.eclipse.ui.plugin.AbstractUIPlugin"; //$NON-NLS-1$
							initialValues.interfaceName = null;
							initialValues.interfaceType = null;
							break;
						}
					}
				}
				initialValues.superClassType = findTypeForName(initialValues.superClassName);
				return;
			}
			String schemaBasedOn = attInfo.getBasedOn();
			if (schemaBasedOn == null || schemaBasedOn.length() == 0) {
				initialValues.superClassName = "java.lang.Object"; //$NON-NLS-1$
				initialValues.superClassType = findTypeForName(initialValues.superClassName);
				return;
			}
			int del = schemaBasedOn.indexOf(':');
			if (del != -1) {
				if (del == 0) {
					initialValues.superClassName = "java.lang.Object"; //$NON-NLS-1$
				} else {
					initialValues.superClassName = schemaBasedOn.substring(0, del);
				}
				initialValues.superClassType = findTypeForName(initialValues.superClassName);
				if (del < schemaBasedOn.length() - 1) {
					initialValues.interfaceName = schemaBasedOn.substring(del + 1);
					initialValues.interfaceType = findTypeForName(initialValues.interfaceName);
				}
			} else {
				int schemaLoc = schemaBasedOn.lastIndexOf("."); //$NON-NLS-1$
				if (schemaLoc != -1 && schemaLoc < schemaBasedOn.length()) {
					IType type = findTypeForName(schemaBasedOn);
					if (type != null && type.isInterface()) {
						initialValues.interfaceName = schemaBasedOn;
						initialValues.interfaceType = type;
					} else if (type != null && type.isClass()) {
						initialValues.superClassName = schemaBasedOn;
						initialValues.superClassType = type;
					}
				}
			}

		} catch (JavaModelException e) {
			PDEPlugin.logException(e);
		}
	}

	public String getClassArgs() {
		if (initialValues.classArgs == null)
			return ""; //$NON-NLS-1$
		return initialValues.classArgs;
	}

}
