package mapswt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
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

    private final Translation translation = new Translation();
    private int zoom = 0;

    public MapView(Composite parent) {
        this.canvas = new Canvas(parent, SWT.NONE);
        Display disp = parent.getDisplay();
        grey = disp.getSystemColor(SWT.COLOR_GRAY);
        white = disp.getSystemColor(SWT.COLOR_WHITE);
        black = disp.getSystemColor(SWT.COLOR_BLACK);
        canvas.addPaintListener(e -> render(e.gc));

        // add mouse listeners
        canvas.addMouseWheelListener(e -> {
            translation.updateCenter(e.x, e.y, zoom);
            if (e.count > 0) {
                zoomIn();
            } else {
                zoomOut();
            }
        });
        canvas.addMouseListener(new DragSupport());
    }

    public void update() {
        canvas.redraw();
    }

    public void zoomIn() {
        if (zoom >= 21)
            return;
        zoom += 1;
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
        show(coll, null);
    }

    public void show(FeatureCollection coll, String parameter) {
        features = coll;
        if (coll == null) {
            projection = null;
            return;
        }
        Bounds bounds = Bounds.of(coll);
        Point center = bounds.center();
        translation.center.x = center.x;
        translation.center.y = center.y;

        // try to find a good initial zoom
        Rectangle canvSize = canvas.getBounds();
        for (int z = 0; z < 21; z++) {
            zoom = z;
            Point topLeft = new Point();
            topLeft.x = bounds.minX;
            topLeft.y = bounds.minY;
            Point bottomRight = new Point();
            bottomRight.x = bounds.maxX;
            bottomRight.y = bounds.maxY;
            WebMercator.project(topLeft, z);
            WebMercator.project(bottomRight, z);
            if ((bottomRight.x - topLeft.x) > canvSize.width)
                break;
            if ((bottomRight.y - topLeft.y) > canvSize.height)
                break;
        }

        projection = WebMercator.apply(coll, zoom);
        this.parameter = parameter;
        initColors(coll, parameter);
        canvas.redraw();
    }

    private void initColors(FeatureCollection coll, String parameter) {
        if (colorScale != null) {
            colorScale.dispose();
        }
        if (parameter == null)
            return;

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
            if (!init) {
                min = v;
                max = v;
                init = true;
            } else {
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
        }
        colorScale = new ColorScale(
                canvas.getDisplay(), min, max);
    }

    private void render(GC gc) {

        Rectangle canvasSize = canvas.getBounds();
        translation.update(canvasSize, zoom);

        // white background
        gc.setBackground(white);
        gc.fillRectangle(canvasSize);

        if (projection == null)
            return;
        if (parameter == null) {
            gc.setBackground(black);
        }

        for (Feature f : projection.features) {
            if (!translation.visible(f)) {
                continue;
            }
            // TODO: currently only polygons are displayed
            // TODO: fill inner rings as white polygons
            // overlapping features can anyhow cause problems
            if (!(f.geometry instanceof Polygon))
                continue;
            Polygon polygon = (Polygon) f.geometry;
            int[] points = translation.translate(polygon);

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

    /**
     * Translates between the projection and canvas pixels.
     */
    private class Translation {

        /**
         * The translation in x direction:
         * canvas.width / 2 - projectedCenter.x
         */
        double x;

        /**
         * The translation in y direction:
         * canvas.height / 2 - projectedCenter.y
         */
        double y;

        /**
         * The center of the map in WGS 84 coordinates.
         */
        final Point center = new Point();

        /**
         * The projected pixel area that is visible on
         * the canvas.
         */
        final Bounds view = new Bounds();

        void update(Rectangle canvasSize, int zoom) {
            Point t = center.clone();
            WebMercator.project(t, zoom);
            double cWidth = canvasSize.width / 2.0;
            double cHeight = canvasSize.height / 2.0;
            x = cWidth - t.x;
            y = cHeight - t.y;
            view.minX = t.x - cWidth;
            view.maxX = t.x + cWidth;
            view.minY = t.y - cHeight;
            view.maxY = t.y + cHeight;
            view.isNil = false;
        }

        void updateCenter(int canvasX, int canvasY, int zoom) {
            Point c = new Point();
            c.x = canvasX - x;
            c.y = canvasY - y;
            WebMercator.unproject(c, zoom);
            center.x = c.x;
            center.y = c.y;
        }

        boolean visible(Feature f) {
            if (f == null || f.geometry == null)
                return false;
            // TODO: other geometries
            if (!(f.geometry instanceof Polygon))
                return false;
            Bounds bounds = Bounds.of(f.geometry);
            return bounds.intersects(view);

            //return true;
        }

        int[] translate(Polygon polygon) {
            if (polygon == null || polygon.rings.size() < 1)
                return null;
            LineString ring = polygon.rings.get(0);
            int[] seq = new int[ring.points.size() * 2];
            for (int i = 0; i < ring.points.size(); i++) {
                Point p = ring.points.get(i);
                seq[2 * i] = (int) (p.x + x);
                seq[2 * i + 1] = (int) (p.y + y);
            }
            return seq;
        }
    }

    private class DragSupport extends MouseAdapter {
        int startX;
        int startY;

        @Override
        public void mouseDown(MouseEvent e) {
            startX = e.x;
            startY = e.y;
            setCursor(SWT.CURSOR_SIZEALL);
            super.mouseDown(e);
        }

        @Override
        public void mouseUp(MouseEvent e) {
            int dx = startX - e.x;
            int dy = startY  - e.y;
            if (dx != 0 || dy != 0) {
                Rectangle r = canvas.getBounds();
                translation.updateCenter(
                        r.width / 2 + dx,
                        r.height / 2 + dy, zoom);
            }
            setCursor(SWT.CURSOR_ARROW);
            canvas.redraw();
        }

        private void setCursor(int c) {
            Display display = canvas.getDisplay();
            canvas.setCursor(display.getSystemCursor(c));
        }
    }
}
