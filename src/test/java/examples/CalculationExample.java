package examples;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;

import com.google.common.util.concurrent.AtomicDouble;
import gnu.trove.impl.hash.TLongDoubleHash;
import gnu.trove.map.hash.TLongDoubleHashMap;
import org.apache.commons.collections.map.SingletonMap;
import org.msgpack.core.MessagePack;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.LocationDao;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.database.derby.DerbyDatabase;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.SystemCalculator;
import org.openlca.core.matrix.IndexFlow;
import org.openlca.core.model.Flow;
import org.openlca.core.model.Location;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.results.ContributionResult;
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
        IndexFlow f = r.flowIndex.at(0);
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
