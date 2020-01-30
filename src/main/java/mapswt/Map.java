package mapswt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.openlca.geo.geojson.Feature;
import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.geo.geojson.LineString;
import org.openlca.geo.geojson.Point;
import org.openlca.geo.geojson.Polygon;

public class Map {

    private static Color grey;
    private static Color color1;
    private static Color color2;
    private static Color color3;
    private static Color color4;
    private static Color color5;

    public static void show(FeatureCollection coll) {

        FeatureCollection projection = new WebMercator(0).project(coll);

        Display display = new Display();
        ColorScale colorScale = new ColorScale(display, 0, 100);

        grey = new Color(display, new RGB(207, 216, 220));
        color1 = new Color(display, new RGB(255, 255, 179));
        color2 = new Color(display, new RGB(255, 255, 141));
        color3 = new Color(display, new RGB(255, 158, 128));
        color4 = new Color(display, new RGB(255, 61, 0));
        color5 = new Color(display, new RGB(163, 0, 0));

        Shell shell = new Shell();
        shell.setSize(800, 800);
        shell.setLayout(new FillLayout());

        Canvas canvas = new Canvas(shell, SWT.NONE);
        canvas.addPaintListener(e -> {
            Rectangle cb = canvas.getBounds();
            e.gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
            e.gc.fillRectangle(cb);

            for (Feature f : projection.features) {
                if (f == null || f.geometry == null)
                    continue;
                if (!(f.geometry instanceof Polygon))
                    continue;
                Polygon polygon = (Polygon) f.geometry;
                int[] points = translate(polygon, cb.width, cb.height);
                Color color = getColor(f, colorScale);
                e.gc.setBackground(color);
                e.gc.fillPolygon(points);
            }
        });

        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        colorScale.dispose();
        display.dispose();
    }

    private static int[] translate(Polygon polygon, int width, int height) {
        if (polygon == null || polygon.rings.size() < 1)
            return null;
        LineString ring = polygon.rings.get(0);
        int[] seq = new int[ring.points.size() * 2];
        for (int i = 0; i < ring.points.size(); i++) {
            Point p = ring.points.get(i);
            seq[2 * i] = (int) (p.x * width);
            seq[2 * i + 1] = (int) (p.y * height);
        }
        return seq;
    }

    private static Color getColor(Feature f, ColorScale scale) {
        if (f == null || f.properties == null)
            return grey;
        Object val = f.properties != null
                ? f.properties.get("Annual non-agri")
                : null;
        if (!(val instanceof Number))
            return grey;
        double v = ((Number) val).doubleValue();
        if (v < 0)
            return grey;
        
        v = Math.log10(v) * 100 / 2;
        return scale.get(v);

        /*
        v = Math.log10(v) * 100 / 2;
        if (v < 10)
            return color1;
        if (v < 30)
            return color2;
        if (v < 50)
            return color3;
        if (v < 75)
            return color4;
        return color5;
        */
    }

}
