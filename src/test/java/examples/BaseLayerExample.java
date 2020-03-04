package examples;

import org.openlca.app.components.mapview.MapView;

public class BaseLayerExample {

    public static void main(String[] args) {
        Examples.withMap(MapView::addBaseLayers);
    }

}
