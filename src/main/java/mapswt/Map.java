package mapswt;

import java.util.Random;

import org.eclipse.swt.SWT;
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

    public static void show(FeatureCollection coll) {

        FeatureCollection projection = new WebMercator(0).project(coll);

        Display display = new Display();
        Shell shell = new Shell();
        shell.setSize(800, 800);
        shell.setLayout(new FillLayout());

        Canvas canvas = new Canvas(shell, SWT.NONE);
        canvas.addPaintListener(e -> {
            e.gc.setBackground(display.getSystemColor(SWT.COLOR_BLUE));
            for (Feature f : projection.features) {
                if (f == null || f.geometry == null)
                    continue;
                if (!(f.geometry instanceof Polygon))
                    continue;
                Polygon polygon = (Polygon) f.geometry;
                int[] points = translate(polygon, 800, 800);
                e.gc.fillPolygon(points);
            }
        });

        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
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

}
