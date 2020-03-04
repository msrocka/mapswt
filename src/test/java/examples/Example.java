package examples;


import java.io.File;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.openlca.app.components.mapview.MapView;
import org.openlca.app.util.Colors;
import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.geo.geojson.GeoJSON;

public class Example {
    public static void main(String[] args) {
        System.out.println("parse files ...");

        // nice example data can be found here
        // https://github.com/martynafford/natural-earth-geojson
        /*
        FeatureCollection land = GeoJSON.read(
                new File("test_data/ne_50m_land.json"));
        FeatureCollection lakes = GeoJSON.read(
                new File("test_data/ne_50m_lakes.json"));
        FeatureCollection ocean = GeoJSON.read(
                new File("test_data/ne_50m_ocean.json"));
        FeatureCollection countries = GeoJSON.read(
                new File("test_data/ne_50m_admin_0_countries.json"));
        */

        FeatureCollection aware = GeoJSON.read(
                new File("test_data/aware.geojson"));

        System.out.println("create map ...");
        Display display = new Display();
        Colors.setDisplay(display);
        Shell shell = new Shell();
        shell.setSize(800, 500);
        shell.setLayout(new FillLayout());

        Color brown = new Color(display, 255, 243, 224);
        Color blue = new Color(display, 227, 242, 253);

        MapView map = new MapView(shell);

        /*
        map.addLayer(ocean)
                .fillColor(blue)
                .borderColor(blue);
        map.addLayer(land)
                .fillColor(brown)
                .center(); // center the map around this layer
        map.addLayer(lakes)
                .fillColor(blue)
                .borderColor(blue);
        map.addLayer(countries);

         */
        map.addLayer(aware)
                .fillScale("Annual non-agri")
                .center();

        shell.open();

        // drawing something after the map was painted
        // is possible by calling .update()
        // map.addLayer(coll)
        //     .fillScale("Annual non-agri", 0, 100);
        // map.update();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
        blue.dispose();
        brown.dispose();
    }
}