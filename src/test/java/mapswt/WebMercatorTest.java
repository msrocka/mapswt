package mapswt;

import org.junit.Assert;
import org.junit.Test;
import org.openlca.geo.geojson.Point;

public class WebMercatorTest {

    @Test
    public void testProjectUnproject() {
        for (int zoom = 0; zoom < 10; zoom++) {
            for (int lon = -180; lon <= 180; lon+=10) {
                for (int lat = -80; lat <= 80; lat+=10) {
                    Point p = new Point();
                    p.x = lon;
                    p.y = lat;
                    WebMercator.project(p, zoom);
                    WebMercator.unproject(p, zoom);
                    Assert.assertEquals(p.x, lon, 1e-10);
                    Assert.assertEquals(p.x, lat, 1e-10);
                }
            }
        }

    }

}
