package mapswt;


import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.geo.geojson.GeoJSON;

public class Example {
    public static void main(String[] args) {
        System.out.println("parse files ...");
        // File file = new File("test_data/aware.geojson");
        
        FeatureCollection countries = GeoJSON.read(
            new File("test_data/countries.geojson"));
        FeatureCollection berlinDistricts = GeoJSON.read(
            new File("test_data/berlin_districts.geojson"));
        FeatureCollection berlinStreets = GeoJSON.read(
            new File("test_data/berlin_streets.geojson")); 
        FeatureCollection berlinBuildings = GeoJSON.read(
            new File("test_data/berlin_buildings.geojson")); 
           

        System.out.println("create map ...");
        Display display = new Display();
        Shell shell = new Shell();
        shell.setSize(800, 500);
        shell.setLayout(new FillLayout());
        
        MapView map = new MapView(shell);
        map.addLayer(countries);
        map.addLayer(berlinDistricts)
            .center(); // center the map around this layer
        map.addLayer(berlinBuildings)
            .fillColor(display.getSystemColor(SWT.COLOR_DARK_MAGENTA));

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
    }
}