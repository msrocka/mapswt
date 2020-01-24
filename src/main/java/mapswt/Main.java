package mapswt;

import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class Main {
    public static void main(String[] args) {
        Display display = new Display();
        Shell shell = new Shell();
        shell.setSize(800, 800);
        shell.setLayout(new FillLayout());

        Canvas canvas = new Canvas(shell, SWT.NONE);
        canvas.addPaintListener(e -> {
            Random rand = new Random();
            int width = canvas.getBounds().width;
            int height = canvas.getBounds().height;
            e.gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
            e.gc.fillRectangle(0, 0, width, height);

            int size = 500;

            int[] points = new int[size];
            for (int i = 0; i < size / 2; i++) {
                points[i * 2] = rand.nextInt(width);
                points[i * 2 + 1] = rand.nextInt(height);
            }
            points[size - 2] = points[0];
            points[size - 1] = points[1];
            e.gc.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
            e.gc.fillPolygon(points);
        });

        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
    }
}