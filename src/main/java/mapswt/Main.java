package mapswt;


import java.io.File;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.geo.geojson.GeoJSON;

public class Main {
    public static void main(String[] args) {
        System.out.println("parse file ...");
        File file = new File("test_data/aware.geojson");
        FeatureCollection coll = GeoJSON.read(file);

        System.out.println("create map ...");
        Display display = new Display();
        Shell shell = new Shell();
        shell.setSize(800, 800);
        shell.setLayout(new FillLayout());
        Map map = new Map(shell);
        // map.show(coll, "Annual non-agri");
        map.show(coll);

        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();

        /**
        System.out.println(coll.features.size() + " features");
        System.out.println("calculate bounds ...");
        Bounds bounds = Bounds.of(coll);
        System.out.println(bounds);
        System.out.println("project ...");
        coll = new WebMercator(0).project(coll);
        System.out.println("calculate bounds ...");
        bounds = Bounds.of(coll);
        System.out.println(bounds);
         */
    }
}