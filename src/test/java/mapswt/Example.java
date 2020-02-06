package mapswt;


import java.io.File;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.geo.geojson.GeoJSON;

public class Example {
    public static void main(String[] args) {
        System.out.println("parse file ...");
        File file = new File("test_data/aware.geojson");
        // File file = new File("test_data/countries.geojson");
        FeatureCollection coll = GeoJSON.read(file);

        System.out.println("create map ...");
        Display display = new Display();
        Shell shell = new Shell();
        shell.setSize(800, 800);
        shell.setLayout(new FillLayout());
        MapView map = new MapView(shell);
        // map.show(coll);

        shell.open();
        map.show(coll, "Annual non-agri");
        // map.show(coll);

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
    }
}