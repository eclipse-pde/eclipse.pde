package org.eclipse.pde.internal.ui.codegen;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.*;
import java.io.*;
import org.eclipse.core.runtime.*;
import java.util.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.codegen.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.jdt.core.*;
import java.io.PrintWriter;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.jface.dialogs.MessageDialog;

public class AttributeClassCodeGenerator extends JavaCodeGenerator {
	private static final String KEY_MISSING_TITLE = "CodeGenerator.missing.title";
	private static final String KEY_MISSING_TYPE = "CodeGenerator.missing.type";
	private static final String KEY_MISSING_TYPES = "CodeGenerator.missing.types";
	private ISchemaAttribute attInfo;
	private IJavaProject javaProject;
	private IType expectedType;
	private IType expectedInterface;
	private Vector requiredMethods;
	private Vector requiredImports;

public AttributeClassCodeGenerator(
	IJavaProject javaProject,
	IFolder sourceFolder,
	String fullyQualifiedClassName, 
	ISchemaAttribute attInfo) {
	super(javaProject.getProject(), sourceFolder, fullyQualifiedClassName);
	this.attInfo = attInfo;
	this.javaProject = javaProject;
	requiredImports = new Vector();
}
private void addAbstractMethod(IMethod method) throws JavaModelException {
	IMethod matchingMethod = findMatchingMethod(method);
	if (matchingMethod == null)
		requiredMethods.addElement(method);
}
private void addImports(PrintWriter writer) {
	for (int i=0; i<requiredImports.size(); i++) {
		String type = requiredImports.elementAt(i).toString();
		writer.println("import "+type+";");
	}
	writer.println();
}

private void addRequiredMethodsFor(String typeName) throws JavaModelException {
	IType type = findTypeForName(typeName);
	if (type != null)
		addRequiredMethodsFor(type);
}

private void addRequiredMethodsFor(IType type) throws JavaModelException {
	// Check the super-interfaces
	String[] interfaceNames = type.getSuperInterfaceNames();
	for (int i = 0; i < interfaceNames.length; i++) {
		addRequiredMethodsFor(interfaceNames[i]);
	}

	if (type.isClass()) {
		// Check the superclass
		String superclassName = type.getSuperclassName();
		if (superclassName != null && !superclassName.equals("java.lang.Object"))
			addRequiredMethodsFor(superclassName);
	}

	IMethod[] methods = type.getMethods();

	for (int i = 0; i < methods.length; i++) {
		IMethod method = methods[i];
		if (type.isClass()) {
			int flags = method.getFlags();
			if (Flags.isPublic(flags) || Flags.isProtected(flags)) {
				if (Flags.isAbstract(flags)) {
					addAbstractMethod(method);
				} else {
					// check if this method is implementing
					// required abstract method
					removeImplementedMethod(method);
				}
			}
		} else
			addAbstractMethod(method);
	}
}
private String calculateReturnValue(String signature) {
	switch (signature.charAt(0)) {
		case Signature.C_BOOLEAN :
			return "false";
		case Signature.C_BYTE :
			return "0";
		case Signature.C_CHAR :
			return "0";
		case Signature.C_DOUBLE :
			return "(double)0.0";
		case Signature.C_FLOAT :
			return "(float)0.0";
		case Signature.C_INT :
			return "0";
		case Signature.C_LONG :
			return "(long)0";
		case Signature.C_SHORT :
			return "(short)0";
		case Signature.C_VOID :
			return null;
		case Signature.C_ARRAY :
		case Signature.C_RESOLVED :
		case Signature.C_UNRESOLVED :
			return "null";
		default :
			return null;
	}
}
private IMethod findMatchingMethod(IMethod method)
	throws JavaModelException {
	for (int i = 0; i < requiredMethods.size(); i++) {
		IMethod requiredMethod = (IMethod) requiredMethods.elementAt(i);
		if (requiredMethod.getElementName().equals(method.getElementName())
			&& requiredMethod.getSignature().equals(method.getSignature())) {
			return requiredMethod;
		}
	}
	return null;
}
private void findRequiredMethods() {
	String expectedTypeName = attInfo.getBasedOn();
	String expectedClassName = null;
	String expectedInterfaceName = null;
	if (expectedTypeName==null) return;
	int del = expectedTypeName.indexOf(':');
	if (del!= -1) {
		// class and interface
		expectedClassName = expectedTypeName.substring(0, del);
		expectedInterfaceName = expectedTypeName.substring(del+1);
		expectedTypeName = expectedClassName;
	}
	try {
		expectedType = findTypeForName(expectedTypeName);
		if (expectedType!=null && expectedType.isClass() && expectedInterfaceName!=null) 
			expectedInterface = findTypeForName(expectedInterfaceName);
		boolean missingType = expectedTypeName!=null && expectedType==null;
		boolean missingInterface = expectedInterfaceName!=null 
										&& expectedInterface==null;
		if (missingType || missingInterface) {
			String mtype = missingType?expectedTypeName:null;
			String minter = missingInterface?expectedInterfaceName:null;
			warnAboutMissingTypes(mtype, minter);
		}
		if (expectedType==null) return;
		requiredMethods = new Vector();
		if (expectedInterface!=null)
		   addRequiredMethodsFor(expectedInterface);
		addRequiredMethodsFor(expectedType);
	} catch (JavaModelException e) {
		PDEPlugin.logException(e);
	}
}

private void warnAboutMissingTypes(String typeName, String interfaceName) {
	String message;
	if (typeName==null) {
		message = PDEPlugin.getFormattedMessage(KEY_MISSING_TYPE, interfaceName);
	}
	else if (interfaceName==null) {
		message = PDEPlugin.getFormattedMessage(KEY_MISSING_TYPE, typeName);
	}
	else {
		message = PDEPlugin.getFormattedMessage(KEY_MISSING_TYPES, new String [] {
									typeName, interfaceName });
	}
	MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(),
			PDEPlugin.getResourceString(KEY_MISSING_TITLE),
			message);
}


private IType findTypeForName(String typeName) throws JavaModelException {
	IType type = null;
	String fileName = typeName.replace('.', '/') + ".java";

	IJavaElement element = javaProject.findElement(new Path(fileName));
	if (element == null)
		return null;
	if (element instanceof IClassFile) {
		type = ((IClassFile) element).getType();
	} else
		if (element instanceof ICompilationUnit) {
			IType[] types = ((ICompilationUnit) element).getTypes();
			type = types[0];
		}
	return type;
}

public void generateContents(
	String packageName,
	String className,
	PrintWriter writer) {
	try {
		findRequiredMethods();
		String methodsBuffer = null;
		if (expectedType==null) {
			generateUnknownContents(packageName, className, writer);
			return;
		}
		if (requiredMethods != null) {
			methodsBuffer = generateMethods();
		}
		String extending = expectedType.isInterface() ? " implements " : " extends ";
		String interfaceExtending = "";
		if (expectedInterface!=null) {
			interfaceExtending = " implements "+
			getSimpleName(expectedInterface.getFullyQualifiedName());
		}

		requiredImports.add(expectedType.getFullyQualifiedName());
		if (expectedInterface!=null)
		   requiredImports.add(expectedInterface.getFullyQualifiedName());

		writer.println("package " + packageName + ";");
		writer.println();
		addImports(writer);
		writer.println("/**");
		writer.println(" * Insert the type's description here.");
		writer.println(" * @see " + expectedType.getElementName());
		writer.println(" */");
		writer.println(
			"public class "
				+ className
				+ extending
				+ getSimpleName(expectedType.getFullyQualifiedName())
				+ interfaceExtending
				+ " {");
		writer.println("\t/**");
		writer.println("\t * The constructor.");
		writer.println("\t */");
		writer.println("\tpublic " + className + "() {");
		writer.println("\t}");
		if (methodsBuffer != null) {
			writer.println();
			writer.print(methodsBuffer);
		}

		writer.println("}");
	} catch (JavaModelException e) {
		PDEPlugin.logException(e);
	}
}

public void generateUnknownContents(
	String packageName,
	String className,
	PrintWriter writer) {
	writer.println("package " + packageName + ";");
	writer.println();
	writer.println("/**");
	writer.println(" * Insert the type's description here.");
	writer.println(" */");
	writer.println(
		"public class "
			+ className
			+ " {");
	writer.println("\t/**");
	writer.println("\t * The constructor.");
	writer.println("\t */");
	writer.println("\tpublic " + className + "() {");
	writer.println("\t}");
	writer.println("}");
}

private String generateMethods() throws JavaModelException {
	StringWriter swriter = new StringWriter();
	PrintWriter writer = new PrintWriter(swriter, true);
	if (requiredMethods != null) {
		for (int i = 0; i < requiredMethods.size(); i++) {
			if (i>0) writer.println();
			IMethod method = (IMethod) requiredMethods.elementAt(i);
			generateRequiredMethod(method, writer);
		}
	}
	writer.close();
	return swriter.toString();
}
private void generateRequiredMethod(IMethod method, PrintWriter writer)
	throws JavaModelException {
	int flags = method.getFlags();
	boolean isProtected = Flags.isProtected(flags);
	String access = isProtected ? "protected" : "public";
	String returnType = parseSignature(method.getReturnType());
	writer.println("\t/**");
	writer.println("\t * Insert the method's description here.");
	writer.println(
		"\t * @see "
			+ getSimpleName(expectedType.getElementName())
			+ "#"
			+ method.getElementName());
	writer.println("\t */");
	writer.print(
		"\t" + access + " " + returnType + " " + method.getElementName() + "("); 
	String[] parameterNames = method.getParameterNames();
	String[] parameterTypes = method.getParameterTypes();
	for (int i = 0; i < method.getNumberOfParameters(); i++) {
		if (i > 0)
			writer.print(", ");
		writer.print(parseSignature(parameterTypes[i]));
		writer.print(" " + parameterNames[i]);
	}

	writer.print(") ");
	String[] exceptionTypes = method.getExceptionTypes();
	for (int i = 0; i < exceptionTypes.length; i++) {
		if (i == 0)
			writer.print("throws ");
		else
			writer.print(", ");
		writer.print(parseSignature(exceptionTypes[i]));
	}
	writer.println(" {");
	String returnValue = calculateReturnValue(method.getReturnType());
	if (returnValue != null)
		writer.println("\t\treturn " + returnValue + ";");
	writer.println("\t}");
}
private String getSimpleName(String fullyQualifiedName) {
	int dot = fullyQualifiedName.lastIndexOf('.');
	if (dot != -1)
		return fullyQualifiedName.substring(dot + 1);
	else
		return fullyQualifiedName;
}
private String parseSignature(String signature) {
	int dimensions = 0;
	StringBuffer buffer = new StringBuffer();
	int nameLoc = 0;
	boolean inTypeName = false;

	for (int i = 0; i < signature.length(); i++) {
		char c = signature.charAt(i);
		if (inTypeName) {
			if (c == Signature.C_NAME_END) {
				String typeName = signature.substring(nameLoc, i);
				String shortTypeName = getSimpleName(typeName);
				if (shortTypeName.length() < typeName.length()) {
					// Ditching package prefix - must add import
					if (!requiredImports.contains(typeName))
						requiredImports.addElement(typeName);
				}
				buffer.append(shortTypeName);
				inTypeName = false;
			}
			continue;
		}
		switch (c) {
			case Signature.C_BOOLEAN :
				buffer.append("boolean");
				break;
			case Signature.C_BYTE :
				buffer.append("byte");
				break;
			case Signature.C_CHAR :
				buffer.append("char");
				break;
			case Signature.C_DOUBLE :
				buffer.append("double");
				break;
			case Signature.C_FLOAT :
				buffer.append("float");
				break;
			case Signature.C_INT :
				buffer.append("int");
				break;
			case Signature.C_LONG :
				buffer.append("long");
				break;
			case Signature.C_SHORT :
				buffer.append("short");
				break;
			case Signature.C_VOID :
				buffer.append("void");
				break;
			case Signature.C_ARRAY :
				dimensions++;
				break;
			case Signature.C_RESOLVED :
				nameLoc = i + 1;
				inTypeName = true;
				break;
			case Signature.C_UNRESOLVED :
				nameLoc = i + 1;
				inTypeName = true;
				break;
		}
	}
	for (int i = 0; i < dimensions; i++) {
		if (i == 0)
			buffer.append(" ");
		buffer.append("[]");
	}
	return buffer.toString();
}
private void removeImplementedMethod(IMethod method)
	throws JavaModelException {
	IMethod matchingMethod = findMatchingMethod(method);
	if (matchingMethod != null)
		requiredMethods.remove(matchingMethod);
}
}
