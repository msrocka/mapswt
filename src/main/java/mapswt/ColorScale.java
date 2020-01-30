package mapswt;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ColorScale {

    private final double min;
    private final double max;

    private final Color[] colors;

    public ColorScale(Display display, double min, double max) {
        this.min = min;
        this.max = max;

        if (min == max) {
            colors = new Color[] { new Color(display, new RGB(30, 1, 1)) };
        } else {
            colors = new Color[100];
            // blue => white
            for (int i = 0; i < 50; i++) {
                double s = 1 - i * (0.9 / 49);
                colors[i] = new Color(display, new RGB(220f, (float) s, 1f));
            }
            // white => orange
            for (int i = 50; i < 100; i++) {
                double s = 0.1 + (i - 50) * (0.9 / 49);
                colors[i] = new Color(display, new RGB(30f, (float) s, 1f));
            }
        }
    }

    public void dispose() {
        for (Color c : colors) {
            if (!c.isDisposed()) {
                c.dispose();
            }
        }
    }

    public Color get(double val) {
        if (colors.length == 1)
            return colors[0];

        if (val < min) {
            val = min;
        }
        if (val > max) {
            val = max;
        }
        double share = (val - min) / (max - min);
        if (share > 1) {
            share = 1;
        }
        if (share < 0) {
            share = 0;
        }

        int i = (int) (share * (colors.length - 1));
        if (i < 0) {
            i = 0;
        }
        if (i >= colors.length) {
            i = colors.length - 1;
        }

        return colors[i];
    }
}