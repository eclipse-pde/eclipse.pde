package org.eclipse.pde.internal.wizards.project;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.codegen.*;
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
		writer.println("   //The shared instance.");
		writer.println("   private static " + className + " plugin;");
	}
	if ((flags & F_BUNDLES) != 0) {
		writer.println("   //Resource bundle.");
		writer.println("   private ResourceBundle resourceBundle;");
	}
	writer.println("   ");
	writer.println("  /**");
	writer.println("   * The constructor.");
	writer.println("   */");
	writer.println("   public " + className + "(IPluginDescriptor descriptor) {");
	writer.println("      super(descriptor);");
	if ((flags & F_THIS) != 0) {
		writer.println("      plugin = this;");
	}
	if ((flags & F_BUNDLES) != 0) {
		writer.println("      try {");
		writer.println(
			"         resourceBundle= ResourceBundle.getBundle(\""
				+ packageName
				+ "."
				+ className
				+ "Resources\");"); 
		writer.println("      } catch (MissingResourceException x) {");
		writer.println("         resourceBundle = null;");
		writer.println("      }");
	}
	writer.println("   }");
	if ((flags & F_THIS) != 0) {
		writer.println();
		writer.println("  /**");
		writer.println("   * Returns the shared instance.");
		writer.println("   */");
		writer.println("   public static " + className + " getDefault() {");
		writer.println("      return plugin;");
		writer.println("   }");
	}
	if ((flags & F_WORKSPACE) != 0) {
		writer.println();
		writer.println("  /**");
		writer.println("   * Returns the workspace instance.");
		writer.println("   */");
		writer.println("   public static IWorkspace getWorkspace() {");
		writer.println(
			"      return ResourcesPlugin.getWorkspace();"); 
		writer.println("   }");
	}
	if ((flags & F_BUNDLES) != 0) {
		writer.println();
		writer.println("  /**");
		writer.println("   * Returns the string from the plugin's resource bundle,");
		writer.println("   * or 'key' if not found.");
		writer.println("   */");
		writer.println("   public static String getResourceString(String key) {");
		writer.println(
			"      ResourceBundle bundle= "
				+ className
				+ ".getDefault().getResourceBundle();"); 
		writer.println("      try {");
		writer.println("         return bundle.getString(key);");
		writer.println("      } catch (MissingResourceException e) {");
		writer.println("         return key;");
		writer.println("      }");
		writer.println("   }");
		writer.println();
		writer.println("  /**");
		writer.println("   * Returns the plugin's resource bundle,");
		writer.println("   */");
		writer.println("   public ResourceBundle getResourceBundle() {");
		writer.println("      return resourceBundle;");
		writer.println("   }");
	}
	writer.println("}");
}
}
