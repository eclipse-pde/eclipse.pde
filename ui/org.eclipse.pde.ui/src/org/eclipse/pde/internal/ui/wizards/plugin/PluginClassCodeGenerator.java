package org.eclipse.pde.internal.ui.wizards.plugin;
import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.codegen.*;
import org.eclipse.pde.internal.ui.wizards.templates.*;
import org.eclipse.pde.ui.*;
/**
 * @author melhem
 *  
 */
public class PluginClassCodeGenerator extends JavaCodeGenerator {
	private IPluginFieldData fPluginData;

	public PluginClassCodeGenerator(IFolder sourceFolder,
			String qualifiedClassName, IPluginFieldData data) {
		super(sourceFolder.getProject(), sourceFolder, qualifiedClassName);
		fPluginData = data;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.codegen.JavaCodeGenerator#generateContents(java.lang.String,
	 *      java.lang.String, java.io.PrintWriter)
	 */
	public void generateContents(String packageName, String className,
			PrintWriter writer) {
		if (fPluginData.hasBundleStructure()) {
			generatePluginClass(packageName, className, writer);
		} else {
			generateLegacyPluginClass(packageName, className, writer);
		}
	}
	
	private void generatePluginClass(String packageName, String className, PrintWriter writer) {
		if (!packageName.equals("")) {
			writer.println("package " + packageName + ";");
			writer.println();
		}
		if (fPluginData.isUIPlugin())
			writer.println("import org.eclipse.ui.plugin.*;");
		writer.println("import org.osgi.framework.BundleContext;");
		writer.println("import java.util.*;");
		writer.println();
		writer.println("/**");
		writer.println(" * The main plugin class to be used in the desktop.");
		writer.println(" */");
		if (fPluginData.isUIPlugin())
			writer.println("public class " + className + " extends AbstractUIPlugin {");
		else
			writer.println("public class " + className + " extends Plugin {");
		writer.println("\t//The shared instance.");
		writer.println("\tprivate static " + className + " plugin;");
		writer.println("\t//Resource bundle.");
		writer.println("\tprivate ResourceBundle resourceBundle;");
		writer.println("\t");
		writer.println("\t/**");
		writer.println("\t * The constructor.");
		writer.println("\t */");
		writer.println("\tpublic " + className + "() {");
		writer.println("\t\tsuper();");
		writer.println("\t\tplugin = this;");
		writer.println("\t\ttry {");
		writer.println("\t\t\tresourceBundle = ResourceBundle.getBundle(\""
				+ packageName + "." + className + "Resources\");");
		writer.println("\t\t} catch (MissingResourceException x) {");
		writer.println("\t\t\tresourceBundle = null;");
		writer.println("\t\t}");
		writer.println("\t}");
		writer.println();
		
		writer.println("\t/**");
		writer.println("\t * This method is called upon plug-in activation");
		writer.println("\t */");
		writer.println("\tpublic void start(BundleContext context) throws Exception {");
		writer.println("\t\tsuper.start(context);");
		writer.println("\t}");
		writer.println();

		writer.println("\t/**");
		writer.println("\t * This method is called when the plug-in is stopped");
		writer.println("\t */");
		writer.println("\tpublic void stop(BundleContext context) throws Exception {");
		writer.println("\t\tsuper.stop(context);");
		writer.println("\t}");
		writer.println();

		writer.println("\t/**");
		writer.println("\t * Returns the shared instance.");
		writer.println("\t */");
		writer.println("\tpublic static " + className + " getDefault() {");
		writer.println("\t\treturn plugin;");
		writer.println("\t}");
		writer.println();
		writer.println("\t/**");
		writer
				.println("\t * Returns the string from the plugin's resource bundle,");
		writer.println("\t * or 'key' if not found.");
		writer.println("\t */");
		writer
				.println("\tpublic static String getResourceString(String key) {");
		writer.println("\t\tResourceBundle bundle = " + className
				+ ".getDefault().getResourceBundle();");
		writer.println("\t\ttry {");
		writer
				.println("\t\t\treturn (bundle != null) ? bundle.getString(key) : key;");
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
		writer.println("}");
	}
	private void generateLegacyPluginClass(String packageName, String className,
			PrintWriter writer) {
		if (!packageName.equals("")) {
			writer.println("package " + packageName + ";");
			writer.println();
		}
		if (fPluginData.isUIPlugin())
			writer.println("import org.eclipse.ui.plugin.*;");
		writer.println("import org.eclipse.core.runtime.*;");
		writer.println("import java.util.*;");
		writer.println();
		writer.println("/**");
		writer.println(" * The main plugin class to be used in the desktop.");
		writer.println(" */");
		if (fPluginData.isUIPlugin())
			writer.println("public class " + className + " extends AbstractUIPlugin {");
		else
			writer.println("public class " + className + " extends Plugin {");
		writer.println("\t//The shared instance.");
		writer.println("\tprivate static " + className + " plugin;");
		writer.println("\t//Resource bundle.");
		writer.println("\tprivate ResourceBundle resourceBundle;");
		writer.println("\t");
		writer.println("\t/**");
		writer.println("\t * The constructor.");
		writer.println("\t */");
		writer.println("\tpublic " + className
				+ "(IPluginDescriptor descriptor) {");
		writer.println("\t\tsuper(descriptor);");
		writer.println("\t\tplugin = this;");
		writer.println("\t\ttry {");
		writer.println("\t\t\tresourceBundle   = ResourceBundle.getBundle(\""
				+ packageName + "." + className + "Resources\");");
		writer.println("\t\t} catch (MissingResourceException x) {");
		writer.println("\t\t\tresourceBundle = null;");
		writer.println("\t\t}");
		writer.println("\t}");
		writer.println();
		writer.println("\t/**");
		writer.println("\t * Returns the shared instance.");
		writer.println("\t */");
		writer.println("\tpublic static " + className + " getDefault() {");
		writer.println("\t\treturn plugin;");
		writer.println("\t}");
		writer.println();
		writer.println("\t/**");
		writer
				.println("\t * Returns the string from the plugin's resource bundle,");
		writer.println("\t * or 'key' if not found.");
		writer.println("\t */");
		writer
				.println("\tpublic static String getResourceString(String key) {");
		writer.println("\t\tResourceBundle bundle = " + className
				+ ".getDefault().getResourceBundle();");
		writer.println("\t\ttry {");
		writer
				.println("\t\t\treturn (bundle != null) ? bundle.getString(key) : key;");
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
		writer.println("}");
	}
	
	public IPluginReference[] getDependencies() {
		ArrayList result = new ArrayList();
		if (fPluginData.isUIPlugin())
			result.add(new PluginReference("org.eclipse.ui", null, 0));
		if (fPluginData.hasBundleStructure())
			result.add(new PluginReference("org.eclipse.core.runtime", null, 0));
		else if (!fPluginData.isLegacy())
			result.add(new PluginReference("org.eclipse.core.runtime.compatibility", null, 0));
		return (IPluginReference[]) result.toArray(new IPluginReference[result.size()]);
	}
	
}
