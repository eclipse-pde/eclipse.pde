package org.eclipse.pde.build.tests;

import java.util.*;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.Property;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.Platform;

public class BuildConfiguration {
	public static final String TRUE = "true";
	public static final String FALSE = "false";

	private static Properties defaultBuildConfig = new Properties();
	private static Properties defaultScriptConfig = new Properties();
	static {
		defaultBuildConfig.put("runPackager", TRUE);
		defaultBuildConfig.put("archiveNamePrefix", "");
		defaultBuildConfig.put("archivePrefix", "eclipse");
		defaultBuildConfig.put("collectingFolder", "eclipse");
		defaultBuildConfig.put("configs", "*,*,*");
		defaultBuildConfig.put("buildType", "I");
		defaultBuildConfig.put("buildId", "TestBuild");
		defaultBuildConfig.put("buildLabel", "I.TestBuild");
		defaultBuildConfig.put("base", Platform.getInstallLocation().getURL().getPath());
		defaultBuildConfig.put("baseLocation", Platform.getInstallLocation().getURL().getPath());
		defaultBuildConfig.put("baseos", Platform.getOS());
		defaultBuildConfig.put("basews", Platform.getWS());
		defaultBuildConfig.put("basearch", Platform.getOSArch());
		defaultBuildConfig.put("zipargs", "");
		defaultBuildConfig.put("tarargs", "");
		defaultBuildConfig.put("timestamp", "007");
		defaultBuildConfig.put("filteredDependencyCheck", FALSE);
		defaultBuildConfig.put("resolution.devMode", FALSE);
		defaultBuildConfig.put("skipBase", TRUE);
		defaultBuildConfig.put("skipMaps", TRUE);
		defaultBuildConfig.put("skipFetch", TRUE);
		defaultBuildConfig.put("logExtension", ".log");
		defaultBuildConfig.put("javacDebugInfo", FALSE);
		defaultBuildConfig.put("javacFailOnError", TRUE);
		defaultBuildConfig.put("javacVerbose", FALSE);
		
		defaultScriptConfig.put("configs", "*,*,*");
		defaultScriptConfig.put("baseLocation", Platform.getInstallLocation().getURL().getPath());
		defaultScriptConfig.put("buildingOSGi", TRUE);
		defaultScriptConfig.put("outputUpdateJars", FALSE);
		defaultScriptConfig.put("archivesFormat", "");
		defaultScriptConfig.put("product", "");
		defaultScriptConfig.put("forceContextQualifier", "");
		defaultScriptConfig.put("generateJnlp", FALSE);
		defaultScriptConfig.put("signJars", FALSE);
		defaultScriptConfig.put("generateFeatureVersionSuffix", FALSE);
		defaultScriptConfig.put("significantVersionDigits", "");
		defaultScriptConfig.put("generateVersionsLists", TRUE);
		defaultScriptConfig.put("groupConfigurations", FALSE);
		defaultScriptConfig.put("pluginPath", "");
		defaultScriptConfig.put("filteredDepenedencyCheck", FALSE);
		defaultScriptConfig.put("platformProperties", "");
	}

	
	private static Properties getBuildConfig() {
		Properties properties = new Properties();
		
		List antProperties = AntCorePlugin.getPlugin().getPreferences().getDefaultProperties();
		ListIterator li = antProperties.listIterator();
		while (li.hasNext()) {
			Property prop = (Property)li.next();
			properties.put(prop.getName(), prop.getValue());
		}
		
		Enumeration e = defaultBuildConfig.keys();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			properties.put(key, defaultBuildConfig.get(key));
		}
		
		
		return properties;	
	}
	
	public static Properties getBuilderProperties(IFolder buildFolder) {
		String builder = buildFolder.getLocation().toOSString();
		Properties builderProperties = getBuildConfig();
		builderProperties.put("buildDirectory", builder);
		builderProperties.put("builder", builder);
		return builderProperties;
	}
	
	public static Properties getScriptGenerationProperties(IFolder buildFolder, String type, String id ) {
		Properties properties = (Properties) defaultScriptConfig.clone();
		properties.put("type", type);
		properties.put("id", id);
		properties.put("buildDirectory", buildFolder.getLocation().toOSString());
		return properties;
	}
}
