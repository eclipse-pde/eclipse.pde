package $packageName$;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

public class ColorManager {

	protected Map<RGB, Color> fColorTable = new HashMap<>(10);

	public void dispose() {
		fColorTable.values().forEach(Color::dispose);
	}
	
	public Color getColor(RGB rgb) {
		Color color = fColorTable.get(rgb);
		if (color == null) {
			color = new Color(rgb);
			fColorTable.put(rgb, color);
		}
		return color;
	}
}
