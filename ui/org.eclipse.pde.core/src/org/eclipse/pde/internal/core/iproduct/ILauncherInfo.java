package org.eclipse.pde.internal.core.iproduct;

public interface ILauncherInfo extends IProductObject {
	
	public static final String LINUX_ICON = "linuxIcon";
	
	public static final String MACOSX_ICON = "macosxIcon";
	
	public static final String SOLARIS_LARGE = "solarisLarge";
	public static final String SOLARIS_MEDIUM = "solarisMedium";
	public static final String SOLARIS_SMALL = "solarisSmall";
	public static final String SOLARIS_TINY = "solarisTiny";
	
	public static final String WIN32_16_LOW = "winSmallLow";
	public static final String WIN32_16_HIGH = "winSmallHigh";
	public static final String WIN32_32_LOW = "winMediumLow";
	public static final String WIN32_32_HIGH = "winMediumHigh";
	public static final String WIN32_48_LOW = "winLargeLow";
	public static final String WIN32_48_HIGH = "winLargeHigh";
	
	public static final String P_USE_ICO = "useIco";
	public static final String P_ICO_PATH = "icoPath";
	public static final String P_LAUNCHER = "launcher";
	
	String getLauncherName();
	
	void setLauncherName(String name);
	
	void setIconPath(String iconId, String path);
	
	String getIconPath(String iconId);
	
	boolean usesWinIcoFile();
	
	void setUseWinIcoFile(boolean use);
	
	void setIcoFilePath(String path);
	
	String getIcoFilePath();
}
