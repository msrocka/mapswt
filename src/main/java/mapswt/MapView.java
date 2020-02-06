package mapswt;

import java.util.ArrayList;
import java.util.List;

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
import org.openlca.geo.geojson.MultiPolygon;
import org.openlca.geo.geojson.Point;
import org.openlca.geo.geojson.Polygon;

public class MapView {

    private final Canvas canvas;
    private final Color white;

    private List<LayerConfig> layers = new ArrayList<>();
    private List<FeatureCollection> projections = new ArrayList<>();

    private final Translation translation = new Translation();
    private int zoom = 0;

    public MapView(Composite parent) {
        this.canvas = new Canvas(parent, SWT.NONE);
        this.white = canvas.getDisplay().getSystemColor(SWT.COLOR_WHITE);
        canvas.addPaintListener(e -> render(e.gc));

        canvas.addDisposeListener(e -> {
            for (LayerConfig config : layers) {
                config.dispose();
            }
        });

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
        projectLayers();
        canvas.redraw();
    }

    public void zoomOut() {
        if (zoom == 0)
            return;
        zoom -= 1;
        projectLayers();
        canvas.redraw();
    }

    private void projectLayers() {
        projections.clear();
        for (LayerConfig config : layers) {
            FeatureCollection projection = WebMercator.apply(
                config.layer, zoom);
            projections.add(projection);
        }
    }

    public LayerConfig addLayer(FeatureCollection layer) {
        LayerConfig config = new LayerConfig(canvas.getDisplay(), layer);
        layers.add(config);
        return config;
    }

    /**
     * Find an initial zoom and center and calculate the projections.
     */
    private void initProjection() {
        projections.clear();
        if (layers.isEmpty()) {
            return;
        }

        // find the centered projection
        FeatureCollection ref = null;
        for (LayerConfig config : layers) {
            if (config.isCenter()) {
                ref = config.layer;
                break;
            }
        }

        // TODO: otherwise take the layer
        // with the largest bounds
        if (ref == null) {
            ref = layers.get(0).layer;
        }

        // calculate the center
        Bounds bounds = Bounds.of(ref);
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

        // finally, project the layers
        projectLayers();
    }

    private void render(GC gc) {

        if(projections.size() != layers.size()) {
            initProjection();
        }

        Rectangle canvasSize = canvas.getBounds();
        translation.update(canvasSize, zoom);

        // white background
        gc.setBackground(white);
        gc.fillRectangle(canvasSize);

        if (projections.isEmpty())
            return;

        for (int i = 0; i < projections.size(); i++) {
            LayerConfig config = layers.get(i);
            gc.setBackground(config.getBorderColor());
            FeatureCollection projection = projections.get(i);
            for (Feature f : projection.features) {
                if (!translation.visible(f)) {
                    continue;
                }
                if (f.geometry instanceof Polygon) {
                    renderPolygon(gc, config, f, (Polygon) f.geometry);
                } else if (f.geometry instanceof MultiPolygon) {
                    MultiPolygon mp = (MultiPolygon) f.geometry;
                    for (Polygon polygon : mp.polygons) {
                        renderPolygon(gc, config, f, polygon);
                    }
                }
            }
        }
    }

    private void renderPolygon(GC gc, LayerConfig conf, Feature f, Polygon geom) {
        int[] points = translation.translate(geom);
        Color fillColor = conf.getFillColor(f);
        if (fillColor != null) {
            gc.setBackground(fillColor);
            gc.fillPolygon(points);
            gc.setBackground(conf.getBorderColor());
        }
        // TODO: currently only polygons are displayed
        // TODO: fill inner rings as white polygons
        // overlapping features can anyhow cause problems
        gc.drawPolygon(points);
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
            Bounds bounds = Bounds.of(f.geometry);
            return bounds.intersects(view);
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
