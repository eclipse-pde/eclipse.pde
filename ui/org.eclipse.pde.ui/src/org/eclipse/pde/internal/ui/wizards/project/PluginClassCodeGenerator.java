package org.eclipse.pde.internal.ui.wizards.project;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.ui.codegen.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.core.resources.*;
import java.io.*;

public class PluginClassCodeGenerator extends JavaCodeGenerator {
	public static final int F_THIS = 0x1;
	public static final int F_WORKSPACE = 0x2;
	public static final int F_BUNDLES = 0x8;
	private int flags;

public PluginClassCodeGenerator(IFolder sourceFolder, String fullyQualifiedClassName, int flags) {
	super(sourceFolder.getProject(), sourceFolder, fullyQualifiedClassName);
	this.flags = flags;
}
public void generateContents(
	String packageName, 
	String className, 
	PrintWriter writer) {
	writer.println("package " + packageName + ";");
	writer.println();
	writer.println("import org.eclipse.ui.plugin.*;");
	writer.println("import org.eclipse.core.runtime.*;");
	if ((flags & F_WORKSPACE) != 0) {
		writer.println("import org.eclipse.core.resources.*;");
	}
	if ((flags & F_BUNDLES) != 0) {
		writer.println("import java.util.*;");
	}
	writer.println();
	writer.println("/**");
	writer.println(" * The main plugin class to be used in the desktop.");
	writer.println(" */");
	writer.println("public class " + className + " extends AbstractUIPlugin {");
	if ((flags & F_THIS) != 0) {
		writer.println("\t//The shared instance.");
		writer.println("\tprivate static " + className + " plugin;");
	}
	if ((flags & F_BUNDLES) != 0) {
		writer.println("\t//Resource bundle.");
		writer.println("\tprivate ResourceBundle resourceBundle;");
	}
	writer.println("\t");
	writer.println("\t/**");
	writer.println("\t * The constructor.");
	writer.println("\t */");
	writer.println("\tpublic " + className + "(IPluginDescriptor descriptor) {");
	writer.println("\t\tsuper(descriptor);");
	if ((flags & F_THIS) != 0) {
		writer.println("\t\tplugin = this;");
	}
	if ((flags & F_BUNDLES) != 0) {
		writer.println("\t\ttry {");
		writer.println(
			"\t\t\tresourceBundle= ResourceBundle.getBundle(\""
				+ packageName
				+ "."
				+ className
				+ "Resources\");"); 
		writer.println("\t\t} catch (MissingResourceException x) {");
		writer.println("\t\t\tresourceBundle = null;");
		writer.println("\t\t}");
	}
	writer.println("\t}");
	if ((flags & F_THIS) != 0) {
		writer.println();
		writer.println("\t/**");
		writer.println("\t * Returns the shared instance.");
		writer.println("\t */");
		writer.println("\tpublic static " + className + " getDefault() {");
		writer.println("\t\treturn plugin;");
		writer.println("\t}");
	}
	if ((flags & F_WORKSPACE) != 0) {
		writer.println();
		writer.println("\t/**");
		writer.println("\t * Returns the workspace instance.");
		writer.println("\t */");
		writer.println("\tpublic static IWorkspace getWorkspace() {");
		writer.println(
			"\t\treturn ResourcesPlugin.getWorkspace();"); 
		writer.println("\t}");
	}
	if ((flags & F_BUNDLES) != 0) {
		writer.println();
		writer.println("\t/**");
		writer.println("\t * Returns the string from the plugin's resource bundle,");
		writer.println("\t * or 'key' if not found.");
		writer.println("\t */");
		writer.println("\tpublic static String getResourceString(String key) {");
		writer.println(
			"\t\tResourceBundle bundle= "
				+ className
				+ ".getDefault().getResourceBundle();"); 
		writer.println("\t\ttry {");
		writer.println("\t\t\treturn bundle.getString(key);");
		writer.println("\t\t} catch (MissingResourceException e) {");
		writer.println("\t\t\treturn key;");
		writer.println("\t\t}");
		writer.println("\t}");
		writer.println();
		writer.println("\t/**");
		writer.println("\t * Returns the plugin's resource bundle,");
		writer.println("\t */");
		writer.println("\tpublic ResourceBundle getResourceBundle() {");
		writer.println("\t\treturn resourceBundle;");
		writer.println("\t}");
	}
	writer.println("}");
}
}
