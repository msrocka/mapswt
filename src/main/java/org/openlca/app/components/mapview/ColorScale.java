package org.openlca.app.components.mapview;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.openlca.app.util.Colors;

class ColorScale {

    private final double min;
    private final double max;

    private final Color[] colors;

    private ColorScale(double min, double max, Color[] colors) {
        this.min = min;
        this.max = max;
        this.colors = colors;
    }

    static ColorScale magenta(double min, double max) {
        Color[] colors;
        if (min == max) {
            colors = new Color[]{Colors.get(new RGB(0f, 1f, 1f), 100)};
        } else {
            colors = new Color[25];
            float h = 60;
            for (int i = 0; i < 25; i++) {
                colors[i] = Colors.get(new RGB(h, 1f, 1f));
                if (h > 0) {
                    h -= 5f;
                } else {
                    h = 355;
                }
            }
        }
        return new ColorScale(min, max, colors);
    }

    static ColorScale classic(double min, double max) {
        if (min == max) {
            Color[] colors = new Color[]{Colors.get(new RGB(0f, 1f, 1f), 100)};
            return new ColorScale(min, max, colors);
        }
        Color[] colors = colors = new Color[25];

        float h;
        float step;
        if (min < 0 && max > 0) {
            // green -> blue -> red
            h = 120;
            step = 10;
        } else {
            // blue -> red
            h = 240;
            step = 5;
        }
        for (int i = 0; i < 25; i++) {
            colors[i] = Colors.get(new RGB(h, 1f, 1f));
            h += step;
        }
        return new ColorScale(min, max, colors);
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