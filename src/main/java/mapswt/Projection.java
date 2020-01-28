package mapswt;

import java.util.List;

import org.openlca.geo.geojson.Feature;
import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.geo.geojson.Geometry;
import org.openlca.geo.geojson.GeometryCollection;
import org.openlca.geo.geojson.LineString;
import org.openlca.geo.geojson.MultiLineString;
import org.openlca.geo.geojson.MultiPoint;
import org.openlca.geo.geojson.MultiPolygon;
import org.openlca.geo.geojson.Point;
import org.openlca.geo.geojson.Polygon;

public abstract class Projection {

    /**
     * Mutates the given point by applying this projection on
     * its coordinates.
     */
    protected abstract void apply(Point point);

    public Geometry project(Geometry geometry) {
        if (geometry == null)
            return null;
        Geometry g = geometry.clone();
        onGeometry(g);
        return g;
    }

    public Feature project(Feature feature) {
        if (feature == null)
            return null;
        Feature f = feature.clone();
        if (f.geometry != null) {
            onGeometry(f.geometry);
        }
        return f;
    }

    public FeatureCollection project(FeatureCollection coll) {
        if (coll == null)
            return null;
        FeatureCollection c = coll.clone();
        for (Feature f : c.features) {
            onGeometry(f.geometry);
        }
        return c;
    }

    private void onGeometry(Geometry g) {
        if (g == null)
            return;

        if (g instanceof Point) {
            apply((Point) g);
            return;
        }

        if (g instanceof MultiPoint) {
            onPoints(((MultiPoint) g).points);
            return;
        }

        if (g instanceof LineString) {
            onPoints(((LineString) g).points);
            return;
        }

        if (g instanceof MultiLineString) {
            onLines(((MultiLineString) g).lineStrings);
            return;
        }

        if (g instanceof Polygon) {
            onLines(((Polygon) g).rings);
            return;
        }

        if (g instanceof MultiPolygon) {
            onPolygons(((MultiPolygon) g).polygons);
            return;
        }

        if (g instanceof GeometryCollection) {
            GeometryCollection coll = (GeometryCollection) g;
            for (Geometry cg : coll.geometries) {
                onGeometry(cg);
            }
        }
    }

    private void onPoints(List<Point> points) {
        if (points == null)
            return;
        for (Point p : points) {
            if (p != null) {
                apply(p);
            }
        }
    }

    private void onLines(List<LineString> lines) {
        if (lines == null)
            return;
        for (LineString line : lines) {
            if (line == null)
                continue;
            onPoints(line.points);
        }
    }

    private void onPolygons(List<Polygon> polygons) {
        if (polygons == null)
            return;
        for (Polygon p : polygons) {
            if (p == null)
                continue;
            onLines(p.rings);
        }
    }
}