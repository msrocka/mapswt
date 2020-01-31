package mapswt;

import org.openlca.geo.geojson.Point;

// see https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
public class WebMercator {

    private WebMercator() {
    }

    /**
     * Projects a WGS 84 (longitude, latitude)-point to a (x,y)- pixel
     * coordinate. It directly mutates the coordinates of the point.
     */
    public static void project(Point p, int zoom) {
        if (p == null)
            return;
        double lon = p.x;
        if (lon < -180) {
            lon = -180;
        } else if (lon > 180) {
            lon = 180;
        }
        double lat = p.y;
        if (lat < -85.0511) {
            lat = -85.0511;
        } else if (lat > 85.0511) {
            lat = 85.0511;
        }

        lon *= Math.PI / 180;
        lat *= p.y * Math.PI / 180;
        double scale = (256 / (2 * Math.PI)) * Math.pow(2, zoom);
        p.x = scale * (lon + Math.PI);
        p.y = scale * (Math.PI - Math.log(Math.tan(Math.PI / 4 + lat / 2)));
    }

    /**
     * The inverse operation of project. Calculates a WGS 84 (longitude,
     * latitude)-point from a pixel coordinate. It directly mutates the
     * given point.
     */
    public static void unproject(Point p, int zoom) {
        if (p == null)
            return;
        double scale = (256 / (2 * Math.PI)) * Math.pow(2, zoom);
        p.x = (p.x / scale) - Math.PI;
        p.y = 2 * Math.atan(Math.exp(Math.PI - p.y / scale)) - Math.PI / 2;
        p.x *= 180 / Math.PI;
        p.y *= 180 / Math.PI;
    }


}
