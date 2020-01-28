package mapswt;


import java.io.File;

import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.geo.geojson.GeoJSON;

public class Main {
    public static void main(String[] args) {
        System.out.println("parse file ...");
        File file = new File("test_data/aware.geojson");
        FeatureCollection coll = GeoJSON.read(file);
        System.out.println(coll.features.size() + " features");
        System.out.println("calculate bounds ...");
        Bounds bounds = Bounds.of(coll);
        System.out.println(bounds);
        System.out.println("project ...");
        coll = new WebMercator(0).project(coll);
        System.out.println("calculate bounds ...");
        bounds = Bounds.of(coll);
        System.out.println(bounds);
    }
}