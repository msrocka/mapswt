package examples;

import java.io.File;
import java.util.Collections;

import com.google.common.primitives.Doubles;
import gnu.trove.map.hash.TLongDoubleHashMap;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.LocationDao;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.database.derby.DerbyDatabase;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.SystemCalculator;
import org.openlca.core.matrix.IndexFlow;
import org.openlca.core.model.Location;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.results.ContributionResult;
import org.openlca.geo.calc.Bounds;
import org.openlca.geo.geojson.Feature;
import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.geo.geojson.MsgPack;
import org.openlca.julia.Julia;
import org.openlca.julia.JuliaSolver;
import org.openlca.util.BinUtils;

public class CalculationExample {

    public static void main(String[] args) throws Exception {

        // load the native calculation libraries
        // you have to put them into the `lib` folder
        // of this project
        Julia.loadFromDir(new File("lib"));
        if (!Julia.isLoaded())
            throw new RuntimeException(
                    "Could not load math libs from lib-folder");
        JuliaSolver solver = new JuliaSolver();

        // adopt the database path and the ID of the product system
        String dbDir = "C:/Users/Win10/openLCA-data-1.4/databases/ecoinvent_2_2_unit";
        IDatabase db = new DerbyDatabase(new File(dbDir));
        ProductSystem sys = new ProductSystemDao(db)
                .getForRefId("7d1cbce0-b5b3-47ba-95b5-014ab3c7f569");

        // run the regionalized calculation
        CalculationSetup setup = new CalculationSetup(sys);
        setup.withRegionalization = true;
        SystemCalculator calc = new SystemCalculator(db, new JuliaSolver());
        ContributionResult r = calc.calculateContributions(setup);

        // calculate the location contributions of a flow
        IndexFlow f = r.flowIndex.at(1042);
        FlowDescriptor flow = f.flow;
        TLongDoubleHashMap contributions = new TLongDoubleHashMap();
        r.flowIndex.each((i, iFlow) -> {
            if (flow.id != iFlow.flow.id)
                return;
            double val = r.getTotalFlowResult(iFlow);
            if (val == 0)
                return;
            long locID = iFlow.location != null
                    ? iFlow.location.id
                    : -1L;
            contributions.put(locID, val);
        });

        // create the feature layer
        LocationDao dao = new LocationDao(db);
        FeatureCollection coll = new FeatureCollection();
        for (long locID : contributions.keys()) {
            Location loc = dao.getForId(locID);
            if (loc == null || loc.geodata == null)
                continue;
            byte[] geodata = BinUtils.gunzip(loc.geodata);
            FeatureCollection fc = MsgPack.unpack(geodata);
            if (fc == null || fc.features.isEmpty())
                continue;
            Feature feature = fc.features.get(0);
            double val = contributions.get(locID);
            feature.properties = Collections.singletonMap("r", val);
            coll.features.add(feature);
        }
        coll.features.sort((f1, f2) -> {
            Bounds b1 = Bounds.of(f1);
            Bounds b2 = Bounds.of(f2);
            double a1 = Math.abs(b1.maxX - b1.minX)
                    * Math.abs(b1.maxY - b1.minY);
            double a2 = Math.abs(b2.maxX - b2.minX)
                    * Math.abs(b2.maxY - b2.minY);
            return -Doubles.compare(a1, a2);
        });

        Examples.withMap(map -> {
            map.addBaseLayers();
            map.addLayer(coll)
                    .fillScale("r")
                    .center();
        });

        // finally, close the database
        db.close();
    }
}
