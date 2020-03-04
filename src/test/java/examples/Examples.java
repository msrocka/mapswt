package examples;

import java.util.function.Consumer;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.openlca.app.components.mapview.MapView;
import org.openlca.app.util.Colors;

class Examples {

    private Examples() {
    }

    static void withMap(Consumer<MapView> fn) {
        Display display = new Display();
        Colors.setDisplay(display);
        Shell shell = new Shell();
        shell.setSize(800, 500);
        shell.setLayout(new FillLayout());
        MapView map = new MapView(shell);
        fn.accept(map);
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
    }
}
