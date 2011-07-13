/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.dependencies;

import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.util.*;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class PackageFinder {

	public static Set findPackagesInClassFiles(IClassFile[] files, IProgressMonitor monitor) {
		Set packages = new HashSet();
		monitor.beginTask(PDEUIMessages.PackageFinder_taskName, files.length);
		for (int i = 0; i < files.length; i++) {
			IClassFileReader reader = ToolFactory.createDefaultClassFileReader(files[i], IClassFileReader.ALL);
			if (reader != null)
				computeReferencedTypes(reader, packages);
			monitor.worked(1);
		}
		monitor.done();
		return packages;
	}

	static void computeReferencedTypes(IClassFileReader cfr, Set packages) {

		char[][] interfaces = cfr.getInterfaceNames();
		if (interfaces != null) {
			for (int i = 0; i < interfaces.length; i++) {
				//note: have to convert names like Ljava/lang/Object; to java.lang.Object
				packages.add(getPackage(new String(interfaces[i]).replace('/', '.')));
			}
		}

		char[] scn = cfr.getSuperclassName();
		if (scn != null) {
			packages.add(getPackage(new String(scn).replace('/', '.')));
		}

		IFieldInfo[] fieldInfo = cfr.getFieldInfos();
		for (int i = 0; i < fieldInfo.length; i++) {

			String fieldName = new String(fieldInfo[i].getDescriptor());
			if (!isPrimitiveTypeSignature(fieldName)) {
				String fieldDescriptor = extractFullyQualifiedTopLevelType(fieldName);
				packages.add(getPackage(new String(fieldDescriptor)));
			}
		}

		IMethodInfo[] methodInfo = cfr.getMethodInfos();
		for (int i = 0; i < methodInfo.length; i++) {
			IExceptionAttribute exceptionAttribute = methodInfo[i].getExceptionAttribute();
			if (exceptionAttribute != null) {
				char[][] exceptionNames = exceptionAttribute.getExceptionNames();
				for (int j = 0; j < exceptionNames.length; j++) {
					packages.add(getPackage(new String(exceptionNames[j]).replace('/', '.')));
				}
			}

			String descriptor = new String(methodInfo[i].getDescriptor());
			//add parameter types
			String[] parameterTypes = Signature.getParameterTypes(descriptor);
			for (int j = 0; j < parameterTypes.length; j++) {
				//have to parse to convert [Ljava/lang/String; to java.lang.String
				if (!isPrimitiveTypeSignature(parameterTypes[j])) {
					packages.add(getPackage(new String(extractFullyQualifiedTopLevelType(parameterTypes[j]))));
				}
			}
			//add return type
			String returnType = Signature.getReturnType(descriptor);
			if (!isPrimitiveTypeSignature(returnType)) {
				returnType = extractFullyQualifiedTopLevelType(returnType);
				packages.add(getPackage(returnType));
			}
		}

		// Is there more to extract from the constant pool??
		IConstantPoolEntry entry;
		IConstantPool pool = cfr.getConstantPool();
		int length = pool.getConstantPoolCount();
		for (int i = 1; i < length; i++) {
			switch (pool.getEntryKind(i)) {
				case IConstantPoolConstant.CONSTANT_Class :
					// add reference to the class
					entry = pool.decodeEntry(i);
					//note: may have to convert names like Ljava/lang/Object; to java.lang.Object
					String className = new String(entry.getClassInfoName()).replace('/', '.');
					className = className.indexOf(';') >= 0 ? extractFullyQualifiedTopLevelType(className) : className;
					packages.add(getPackage(className));
					break;

				case IConstantPoolConstant.CONSTANT_NameAndType :
					// add reference to the name and type
					entry = pool.decodeEntry(i);
					int descIndex = entry.getNameAndTypeInfoDescriptorIndex();
					if (pool.getEntryKind(descIndex) == IConstantPoolConstant.CONSTANT_Utf8) {
						entry = pool.decodeEntry(descIndex);
						char[] type = entry.getUtf8Value();
						if (type[0] == '(') {
							// Method signature.

							//add parameter types
							String descriptor = new String(type);
							String[] parameterTypes = Signature.getParameterTypes(descriptor);
							for (int j = 0; j < parameterTypes.length; j++) {
								if (!isPrimitiveTypeSignature(parameterTypes[j])) {
									packages.add(getPackage(extractFullyQualifiedTopLevelType(parameterTypes[j])));
								}
							}
							//add return type
							String returnType = Signature.getReturnType(descriptor);
							if (!isPrimitiveTypeSignature(returnType)) {
								returnType = extractFullyQualifiedTopLevelType(returnType);
								packages.add(getPackage(returnType));
							}

						} else {
							// Field type.
							String typeString = new String(type);
							if (!isPrimitiveTypeSignature(typeString)) {
								packages.add(getPackage(extractFullyQualifiedTopLevelType(typeString)));
							}
						}
					}
					break;
			}
		}
		packages.remove(""); // removes default package if it exists //$NON-NLS-1$
	}

	static boolean isPrimitiveTypeSignature(String typeSig) {
		//check for array of primitives
		/* bug 101514 - changed >= 2 and typeSig.subString(1, typeSig.length) to incorporate multi dimensional arrays of primitives */
		if (typeSig.length() >= 2 && typeSig.startsWith("[") && isPrimitiveTypeSignature(typeSig.substring(1, typeSig.length())))return true; //$NON-NLS-1$

		//check for primitives
		if (typeSig.length() != 1)
			return false;
		if (typeSig.equals(Signature.SIG_VOID) || typeSig.equals(Signature.SIG_BOOLEAN) || typeSig.equals(Signature.SIG_BYTE) || typeSig.equals(Signature.SIG_CHAR) || typeSig.equals(Signature.SIG_DOUBLE) || typeSig.equals(Signature.SIG_FLOAT) || typeSig.equals(Signature.SIG_INT) || typeSig.equals(Signature.SIG_LONG) || typeSig.equals(Signature.SIG_SHORT)) {

			return true;
		}
		return false;
	}

	static String extractFullyQualifiedTopLevelType(String typeName) {

		//first convert from / to .
		typeName = typeName.replace('/', '.');

		//get rid of anonymous and/or possible inner classes (bug 330278)
		//(they aren't relevant for packages)
		int innerClassIndicator = typeName.indexOf('$');
		typeName = innerClassIndicator > 0 ? typeName.substring(0, innerClassIndicator).concat(";") : typeName; //$NON-NLS-1$

		//create signature
		typeName = Signature.toString(typeName);

		//remove array indicator if it is there
		typeName = typeName.endsWith("[]") ? typeName.substring(0, typeName.length() - 2) : typeName; //$NON-NLS-1$

		return typeName;
	}

	static String getPackage(String classType) {
		int period = classType.lastIndexOf('.');
		return (period == -1) ? "" : classType.substring(0, period); // if no period, then we have a class in the default package, return "" for packagename //$NON-NLS-1$
	}

	public static IClassFile[] getClassFiles(IProject project, IBundlePluginModelBase base) {
		ArrayList classFiles = new ArrayList();
		IBundle bundle = base.getBundleModel().getBundle();
		String value = bundle.getHeader(Constants.BUNDLE_CLASSPATH);
		if (value == null)
			value = "."; //$NON-NLS-1$
		ManifestElement elems[] = null;
		try {
			elems = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, value);
		} catch (BundleException e) {
			return new IClassFile[0];
		}
		for (int i = 0; i < elems.length; i++) {
			String lib = elems[i].getValue();
			IResource res = project.findMember(lib);
			if (res != null) {
				addClassFilesFromResource(res, classFiles);
			}
		}
		return (IClassFile[]) classFiles.toArray(new IClassFile[classFiles.size()]);
	}

	private static void addClassFilesFromResource(IResource res, List classFiles) {
		if (res == null)
			return;
		Stack stack = new Stack();
		if (res instanceof IContainer) {
			stack.push(res);
			while (!stack.isEmpty()) {
				try {
					IResource[] children = ((IContainer) stack.pop()).members();
					for (int i = 0; i < children.length; i++)
						if (children[i] instanceof IFile && "class".equals(children[i].getFileExtension())) { //$NON-NLS-1$
							classFiles.add(JavaCore.createClassFileFrom((IFile) children[i]));
						} else if (children[i] instanceof IContainer)
							stack.push(children[i]);
				} catch (CoreException e) {
				}
			}
		} else if (res instanceof IFile) {
			if (res.getFileExtension().equals("jar") || res.getFileExtension().equals("zip")) { //$NON-NLS-1$ //$NON-NLS-2$
				IPackageFragmentRoot root = JavaCore.create(res.getProject()).getPackageFragmentRoot(res);
				if (root == null)
					return;
				try {
					IJavaElement[] children = root.getChildren();
					for (int i = 0; i < children.length; i++) {
						if (children[i] instanceof IPackageFragment) {
							IPackageFragment frag = (IPackageFragment) children[i];
							IClassFile[] files = frag.getClassFiles();
							for (int j = 0; j < files.length; j++)
								classFiles.add(files[j]);
						}
					}
				} catch (JavaModelException e) {
				}
			} else if (res.getFileExtension().equals("class")) //$NON-NLS-1$
				JavaCore.createClassFileFrom((IFile) res);
		}
	}

}
