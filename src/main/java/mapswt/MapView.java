package mapswt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
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

public class MapView {

    private final Canvas canvas;
    private final Color grey;
    private final Color white;
    private final Color black;

    private ColorScale colorScale;
    private FeatureCollection features;
    private FeatureCollection projection;
    private String parameter;

    private int zoom = 0;
    private Point center = new Point();

    public MapView(Composite parent) {
        this.canvas = new Canvas(parent, SWT.NONE);
        Display disp = parent.getDisplay();
        grey = disp.getSystemColor(SWT.COLOR_GRAY);
        white = disp.getSystemColor(SWT.COLOR_WHITE);
        black = disp.getSystemColor(SWT.COLOR_BLACK);
        canvas.addPaintListener(e -> render(e.gc));

        canvas.addMouseWheelListener(e -> {
            // System.out.println(e.x + ", " + e.y);
            if (e.count > 0) {
                zoomIn();
            } else {
                zoomOut();
            }
        });
    }

    public void zoomIn() {
        if (zoom >= 21)
            return;
        zoom+=1;
        projection = WebMercator.apply(features, zoom);
        canvas.redraw();
    }

    public void zoomOut() {
        if (zoom == 0)
            return;
        zoom -= 1;
        projection = WebMercator.apply(features, zoom);
        canvas.redraw();
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

        Rectangle canvasSize = canvas.getBounds();

        // white background
        gc.setBackground(white);
        gc.fillRectangle(canvasSize);

        if (projection == null)
            return;
        if (parameter == null) {
            gc.setBackground(black);
        }

        // calculate the translation
        Point tPoint = center.clone();
        WebMercator.project(tPoint, zoom);
        double translationX = (canvasSize.width / 2.0) - tPoint.x;
        double translationY = (canvasSize.height / 2.0) - tPoint.y;

        for (Feature f : projection.features) {
            if (f == null || f.geometry == null)
                continue;
            // TODO: maybe filter out features that are
            // not visible
            // TODO: currently only polygons are displayed
            // TODO: fill inner rings as white polygons
            // overlapping features can anyhow cause problems
            if (!(f.geometry instanceof Polygon))
                continue;
            Polygon polygon = (Polygon) f.geometry;
            int[] points = translate(polygon, translationX, translationY);

            if (parameter != null) {
                Color color = getColor(f);
                gc.setBackground(color);
                gc.fillPolygon(points);
                gc.setBackground(black);
            }
            gc.drawPolygon(points);
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

    private int[] translate(Polygon polygon, double tx, double ty) {
        if (polygon == null || polygon.rings.size() < 1)
            return null;
        LineString ring = polygon.rings.get(0);
        int[] seq = new int[ring.points.size() * 2];
        for (int i = 0; i < ring.points.size(); i++) {
            Point p = ring.points.get(i);
            seq[2 * i] = (int) (p.x + tx);
            seq[2 * i + 1] = (int) (p.y + ty);
        }
        return seq;
    }
}
