package examples;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.LocationDao;
import org.openlca.core.database.derby.DerbyDatabase;
import org.openlca.core.model.Location;
import org.openlca.geo.calc.Bounds;
import org.openlca.geo.geojson.Feature;
import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.geo.geojson.MsgPack;
import org.openlca.util.BinUtils;
import org.openlca.util.Pair;

public class ColorScaleExample {
    public static void main(String[] args) throws Exception {
        // adopt the database path and the ID of the product system
        String dbDir = "C:/Users/Win10/openLCA-data-1.4/databases/ecoinvent_2_2_unit";
        IDatabase db = new DerbyDatabase(new File(dbDir));

        // create the feature layer
        List<Pair<Feature, Double>> pairs = new ArrayList<>();
        for (Location loc : new LocationDao(db).getAll()) {
            if (loc == null || loc.geodata == null) {
                continue;
            }
            byte[] geodata = BinUtils.gunzip(loc.geodata);
            FeatureCollection fc = MsgPack.unpack(geodata);
            if (fc == null || fc.features.isEmpty()) {
                continue;
            }
            Feature feature = fc.features.get(0);
            double m = Math.random() > 0.5 ? 100 : -100;
            feature.properties = Collections.singletonMap(
                    "r", Math.random() * m);
            Bounds bounds = Bounds.of(feature);
            double a = Math.abs(bounds.maxX - bounds.minX)
                    * Math.abs(bounds.maxY - bounds.minY);
            pairs.add(Pair.of(feature, a));
        }
        pairs.sort((p1, p2) -> Double.compare(p2.second, p1.second));
        FeatureCollection coll = new FeatureCollection();
        pairs.stream()
                .map(p -> p.first)
                .forEachOrdered(coll.features::add);

        Examples.withMap(map -> {
            map.addBaseLayers();
            map.addLayer(coll)
                    .fillScale("r")
                    .center();
        });

        db.close();

    }
}
