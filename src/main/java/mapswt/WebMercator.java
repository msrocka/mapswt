package mapswt;

import org.openlca.geo.geojson.Point;

// see https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
public class WebMercator extends Projection {

    private final int zoom;

    public WebMercator(int zoom) {
        this.zoom = zoom;
    }

    @Override
    protected void apply(Point point) {
        double px = point.x;
        if (px < -180) {
            px = -180;
        }
        if (px > 180) {
            px = 180;
        }
        double py = point.y;
        if (py < -85.0511) {
            py = -85.0511;
        }
        if (py > 85.0511) {
            py = 85.0551;
        }
        double x = ((px + 180.0) / 360.0) * Math.pow(2, zoom);
        double lat = py * Math.PI / 180.0;
        double y = (1 - Math.log(Math.tan(lat) + 1.0 / Math.cos(lat)) / Math.PI)
                * Math.pow(2, zoom-1);
        point.x = x;
        point.y = y;
    }
}
