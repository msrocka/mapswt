package mapswt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.openlca.geo.geojson.Feature;
import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.geo.geojson.LineString;
import org.openlca.geo.geojson.Point;
import org.openlca.geo.geojson.Polygon;

public class Map {

    private final Canvas canvas;
    private final Color grey;
    private final Color white;
    private final Color black;

    private ColorScale colorScale;
    private FeatureCollection features;
    private FeatureCollection projection;
    private String parameter;
    private int zoom = 0;

    public Map(Composite parent) {
        this.canvas = new Canvas(parent, SWT.NONE);
        Display disp = parent.getDisplay();
        grey = disp.getSystemColor(SWT.COLOR_GRAY);
        white = disp.getSystemColor(SWT.COLOR_WHITE);
        black = disp.getSystemColor(SWT.COLOR_BLACK);
        canvas.addPaintListener(e -> render(e.gc));
    }

    public void show(FeatureCollection coll) {
        features = coll;
        parameter = null;
        if (colorScale != null) {
            colorScale.dispose();
            colorScale = null;
        }
        if (coll == null) {
            projection = null;
        } else {
            projection = WebMercator.apply(coll, zoom);
        }
        canvas.redraw();
    }

    public void show(FeatureCollection coll, String parameter) {
        if (coll == null || parameter == null) {
            show(coll);
            return;
        }
        features = coll;
        this.parameter = parameter;
        projection = WebMercator.apply(coll, zoom);

        boolean init = false;
        double min = 0;
        double max = 0;
        for (Feature f : coll.features) {
            if (f.properties == null || f.geometry == null)
                continue;
            Object val = f.properties.get(parameter);
            if (!(val instanceof Number))
                continue;
            double v = ((Number) val).doubleValue();
            if (v < -99) // TODO: fix AWARE
                continue;
            if (!init) {
                min = v;
                max = v;
                init = true;
            } else {
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
        }

        if (colorScale != null) {
            colorScale.dispose();
        }
        colorScale = new ColorScale(canvas.getDisplay(), min, max);
        canvas.redraw();
    }

    private void render(GC gc) {

        // white background
        Rectangle bounds = canvas.getBounds();
        gc.setBackground(white);
        gc.fillRectangle(bounds);

        if (projection == null)
            return;
        if (parameter == null) {
            gc.setBackground(black);
        }

        for (Feature f : projection.features) {
            if (f == null || f.geometry == null)
                continue;
            // TODO: currently only polygons are displayed
            // TODO: fill inner rings as white polygons
            // overlapping features can anyhow cause problems
            if (!(f.geometry instanceof Polygon))
                continue;
            Polygon polygon = (Polygon) f.geometry;
            int[] points = translate(polygon, bounds.width, bounds.height);

            if (parameter == null) {
                gc.drawPolygon(points);
            } else {
                Color color = getColor(f);
                gc.setBackground(color);
                gc.fillPolygon(points);
                gc.setBackground(black);
                gc.drawPolygon(points);
            }
        }
    }

    private Color getColor(Feature f) {
        if (f == null || f.properties == null)
            return grey;
        if (colorScale == null || parameter == null)
            return grey;
        Object val = f.properties.get(parameter);
        if (!(val instanceof Number))
            return grey;
        double v = ((Number) val).doubleValue();
        return colorScale.get(v);
    }

    private int[] translate(Polygon polygon, int width, int height) {
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
}
