package org.eclipse.pde.internal.core.iproduct;

public interface ILauncherInfo extends IProductObject {
	
	public static final String LINUX_ICON = "linuxIcon"; //$NON-NLS-1$
	
	public static final String MACOSX_ICON = "macosxIcon"; //$NON-NLS-1$
	
	public static final String SOLARIS_LARGE = "solarisLarge"; //$NON-NLS-1$
	public static final String SOLARIS_MEDIUM = "solarisMedium"; //$NON-NLS-1$
	public static final String SOLARIS_SMALL = "solarisSmall"; //$NON-NLS-1$
	public static final String SOLARIS_TINY = "solarisTiny"; //$NON-NLS-1$
	
	public static final String WIN32_16_LOW = "winSmallLow"; //$NON-NLS-1$
	public static final String WIN32_16_HIGH = "winSmallHigh"; //$NON-NLS-1$
	public static final String WIN32_32_LOW = "winMediumLow"; //$NON-NLS-1$
	public static final String WIN32_32_HIGH = "winMediumHigh"; //$NON-NLS-1$
	public static final String WIN32_48_LOW = "winLargeLow"; //$NON-NLS-1$
	public static final String WIN32_48_HIGH = "winLargeHigh"; //$NON-NLS-1$
	
	public static final String P_USE_ICO = "useIco"; //$NON-NLS-1$
	public static final String P_ICO_PATH = "icoFile"; //$NON-NLS-1$
	public static final String P_LAUNCHER = "launcher"; //$NON-NLS-1$
	public static final String P_DIRECTORY = "directory"; //$NON-NLS-1$
	
	String getLauncherName();
	
	void setLauncherName(String name);
	
	String getRootDirectory();
	
	void setRootDirectory(String directory);
	
	void setIconPath(String iconId, String path);
	
	String getIconPath(String iconId);
	
	boolean usesWinIcoFile();
	
	void setUseWinIcoFile(boolean use);
}
