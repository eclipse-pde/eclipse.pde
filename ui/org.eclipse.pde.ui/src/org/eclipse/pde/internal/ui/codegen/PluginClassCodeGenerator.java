package org.eclipse.pde.internal.ui.codegen;

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.wizards.templates.*;
import org.eclipse.pde.ui.*;

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
		if (fPluginData.isLegacy()) {
			generateLegacyPluginClass(packageName, className, writer);
		} else {
			generatePluginClass(packageName, className, writer);
		}
	}
	
	private void generatePluginClass(String packageName, String className, PrintWriter writer) {
		if (!packageName.equals("")) { //$NON-NLS-1$
			writer.println("package " + packageName + ";"); //$NON-NLS-1$ //$NON-NLS-2$
			writer.println();
		}
		if (fPluginData.isUIPlugin())
			writer.println("import org.eclipse.ui.plugin.*;"); //$NON-NLS-1$
		else
			writer.println("import org.eclipse.core.runtime.Plugin;");
		writer.println("import org.osgi.framework.BundleContext;"); //$NON-NLS-1$
		writer.println("import java.util.*;"); //$NON-NLS-1$
		writer.println();
		writer.println("/**"); //$NON-NLS-1$
		writer.println(" * The main plugin class to be used in the desktop."); //$NON-NLS-1$
		writer.println(" */"); //$NON-NLS-1$
		if (fPluginData.isUIPlugin())
			writer.println("public class " + className + " extends AbstractUIPlugin {"); //$NON-NLS-1$ //$NON-NLS-2$
		else
			writer.println("public class " + className + " extends Plugin {"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("\t//The shared instance."); //$NON-NLS-1$
		writer.println("\tprivate static " + className + " plugin;"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("\t//Resource bundle."); //$NON-NLS-1$
		writer.println("\tprivate ResourceBundle resourceBundle;"); //$NON-NLS-1$
		writer.println("\t"); //$NON-NLS-1$
		writer.println("\t/**"); //$NON-NLS-1$
		writer.println("\t * The constructor."); //$NON-NLS-1$
		writer.println("\t */"); //$NON-NLS-1$
		writer.println("\tpublic " + className + "() {"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("\t\tsuper();"); //$NON-NLS-1$
		writer.println("\t\tplugin = this;"); //$NON-NLS-1$
		writer.println("\t\ttry {"); //$NON-NLS-1$
		writer.println("\t\t\tresourceBundle = ResourceBundle.getBundle(\"" //$NON-NLS-1$
				+ packageName + "." + className + "Resources\");"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("\t\t} catch (MissingResourceException x) {"); //$NON-NLS-1$
		writer.println("\t\t\tresourceBundle = null;"); //$NON-NLS-1$
		writer.println("\t\t}"); //$NON-NLS-1$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println();
		
		writer.println("\t/**"); //$NON-NLS-1$
		writer.println("\t * This method is called upon plug-in activation"); //$NON-NLS-1$
		writer.println("\t */"); //$NON-NLS-1$
		writer.println("\tpublic void start(BundleContext context) throws Exception {"); //$NON-NLS-1$
		writer.println("\t\tsuper.start(context);"); //$NON-NLS-1$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println();

		writer.println("\t/**"); //$NON-NLS-1$
		writer.println("\t * This method is called when the plug-in is stopped"); //$NON-NLS-1$
		writer.println("\t */"); //$NON-NLS-1$
		writer.println("\tpublic void stop(BundleContext context) throws Exception {"); //$NON-NLS-1$
		writer.println("\t\tsuper.stop(context);"); //$NON-NLS-1$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println();

		writer.println("\t/**"); //$NON-NLS-1$
		writer.println("\t * Returns the shared instance."); //$NON-NLS-1$
		writer.println("\t */"); //$NON-NLS-1$
		writer.println("\tpublic static " + className + " getDefault() {"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("\t\treturn plugin;"); //$NON-NLS-1$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println();
		writer.println("\t/**"); //$NON-NLS-1$
		writer
				.println("\t * Returns the string from the plugin's resource bundle,"); //$NON-NLS-1$
		writer.println("\t * or 'key' if not found."); //$NON-NLS-1$
		writer.println("\t */"); //$NON-NLS-1$
		writer
				.println("\tpublic static String getResourceString(String key) {"); //$NON-NLS-1$
		writer.println("\t\tResourceBundle bundle = " + className //$NON-NLS-1$
				+ ".getDefault().getResourceBundle();"); //$NON-NLS-1$
		writer.println("\t\ttry {"); //$NON-NLS-1$
		writer
				.println("\t\t\treturn (bundle != null) ? bundle.getString(key) : key;"); //$NON-NLS-1$
		writer.println("\t\t} catch (MissingResourceException e) {"); //$NON-NLS-1$
		writer.println("\t\t\treturn key;"); //$NON-NLS-1$
		writer.println("\t\t}"); //$NON-NLS-1$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println();
		writer.println("\t/**"); //$NON-NLS-1$
		writer.println("\t * Returns the plugin's resource bundle,"); //$NON-NLS-1$
		writer.println("\t */"); //$NON-NLS-1$
		writer.println("\tpublic ResourceBundle getResourceBundle() {"); //$NON-NLS-1$
		writer.println("\t\treturn resourceBundle;"); //$NON-NLS-1$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println("}"); //$NON-NLS-1$
	}
	private void generateLegacyPluginClass(String packageName, String className,
			PrintWriter writer) {
		if (!packageName.equals("")) { //$NON-NLS-1$
			writer.println("package " + packageName + ";"); //$NON-NLS-1$ //$NON-NLS-2$
			writer.println();
		}
		if (fPluginData.isUIPlugin())
			writer.println("import org.eclipse.ui.plugin.*;"); //$NON-NLS-1$
		writer.println("import org.eclipse.core.runtime.*;"); //$NON-NLS-1$
		writer.println("import java.util.*;"); //$NON-NLS-1$
		writer.println();
		writer.println("/**"); //$NON-NLS-1$
		writer.println(" * The main plugin class to be used in the desktop."); //$NON-NLS-1$
		writer.println(" */"); //$NON-NLS-1$
		if (fPluginData.isUIPlugin())
			writer.println("public class " + className + " extends AbstractUIPlugin {"); //$NON-NLS-1$ //$NON-NLS-2$
		else
			writer.println("public class " + className + " extends Plugin {"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("\t//The shared instance."); //$NON-NLS-1$
		writer.println("\tprivate static " + className + " plugin;"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("\t//Resource bundle."); //$NON-NLS-1$
		writer.println("\tprivate ResourceBundle resourceBundle;"); //$NON-NLS-1$
		writer.println("\t"); //$NON-NLS-1$
		writer.println("\t/**"); //$NON-NLS-1$
		writer.println("\t * The constructor."); //$NON-NLS-1$
		writer.println("\t */"); //$NON-NLS-1$
		writer.println("\tpublic " + className //$NON-NLS-1$
				+ "(IPluginDescriptor descriptor) {"); //$NON-NLS-1$
		writer.println("\t\tsuper(descriptor);"); //$NON-NLS-1$
		writer.println("\t\tplugin = this;"); //$NON-NLS-1$
		writer.println("\t\ttry {"); //$NON-NLS-1$
		writer.println("\t\t\tresourceBundle   = ResourceBundle.getBundle(\"" //$NON-NLS-1$
				+ packageName + "." + className + "Resources\");"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("\t\t} catch (MissingResourceException x) {"); //$NON-NLS-1$
		writer.println("\t\t\tresourceBundle = null;"); //$NON-NLS-1$
		writer.println("\t\t}"); //$NON-NLS-1$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println();
		writer.println("\t/**"); //$NON-NLS-1$
		writer.println("\t * Returns the shared instance."); //$NON-NLS-1$
		writer.println("\t */"); //$NON-NLS-1$
		writer.println("\tpublic static " + className + " getDefault() {"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("\t\treturn plugin;"); //$NON-NLS-1$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println();
		writer.println("\t/**"); //$NON-NLS-1$
		writer
				.println("\t * Returns the string from the plugin's resource bundle,"); //$NON-NLS-1$
		writer.println("\t * or 'key' if not found."); //$NON-NLS-1$
		writer.println("\t */"); //$NON-NLS-1$
		writer
				.println("\tpublic static String getResourceString(String key) {"); //$NON-NLS-1$
		writer.println("\t\tResourceBundle bundle = " + className //$NON-NLS-1$
				+ ".getDefault().getResourceBundle();"); //$NON-NLS-1$
		writer.println("\t\ttry {"); //$NON-NLS-1$
		writer
				.println("\t\t\treturn (bundle != null) ? bundle.getString(key) : key;"); //$NON-NLS-1$
		writer.println("\t\t} catch (MissingResourceException e) {"); //$NON-NLS-1$
		writer.println("\t\t\treturn key;"); //$NON-NLS-1$
		writer.println("\t\t}"); //$NON-NLS-1$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println();
		writer.println("\t/**"); //$NON-NLS-1$
		writer.println("\t * Returns the plugin's resource bundle,"); //$NON-NLS-1$
		writer.println("\t */"); //$NON-NLS-1$
		writer.println("\tpublic ResourceBundle getResourceBundle() {"); //$NON-NLS-1$
		writer.println("\t\treturn resourceBundle;"); //$NON-NLS-1$
		writer.println("\t}"); //$NON-NLS-1$
		writer.println("}"); //$NON-NLS-1$
	}
	
	public IPluginReference[] getDependencies() {
		ArrayList result = new ArrayList();
		if (fPluginData.isUIPlugin())
			result.add(new PluginReference("org.eclipse.ui", null, 0)); //$NON-NLS-1$
		if (!fPluginData.isLegacy())
			result.add(new PluginReference("org.eclipse.core.runtime", null, 0)); //$NON-NLS-1$
		return (IPluginReference[]) result.toArray(new IPluginReference[result.size()]);
	}
	
}
