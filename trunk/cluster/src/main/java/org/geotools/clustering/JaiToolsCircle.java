/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geotools.clustering;

import jaitools.media.jai.kernel.KernelFactory;
import jaitools.media.jai.kernel.KernelUtil;

import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.KernelJAI;
import javax.media.jai.RasterFactory;

import org.geotools.clustering.utils.ClusterKernelFactory;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.TransformException;

/**
 * Like QuantizeCirle but using the JAI-Tool kit to do the kernel stuff.
 * 
 * @author ijt1
 */
public class JaiToolsCircle {

    private static final Logger LOGGER = Logger.getLogger("org.geotools.clustering.JaiToolsCircle");

    private final ReferencedEnvelope env;

    private final double cellsize;

    private final int height;

    private final int width;

    private final double[][] data;

    private final GridGeometry2D gg;

    public JaiToolsCircle(ReferencedEnvelope env, double cellSize) {
        this.env = env;
        this.cellsize = cellSize;
        System.out.println("cellsize " + cellsize);
        height = (int) Math.ceil((env.getHeight() / cellsize))+1;
        width = (int) Math.ceil((env.getWidth() / cellsize))+1;
        System.out.println("width "+width);
        System.out.println("height "+height);
        data = new double[height][width];
        GridEnvelope2D gridEnv = new GridEnvelope2D(0, 0, width, height);

        gg = new GridGeometry2D(gridEnv, (org.opengis.geometry.Envelope) env);
    }

    public double getCellsize() {
        return cellsize;
    }

    public GridCoverage2D processCircles(List<Circle> results) {
        for (Circle c : results) {
            quantize(c);
        }
        WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_DOUBLE, width,
                height, 1, null);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                raster.setSample(x, y, 0, data[y][x]);
            }
        }

        GridCoverageFactory gcf = CoverageFactoryFinder.getGridCoverageFactory(null);

        CharSequence name = "Process Results";

        GridCoverage2D grid = gcf.create(name, raster, env);

        return grid;
    }

    protected final void quantize(Circle c) {
        org.geotools.clustering.utils.ClusterKernelFactory.ValueType type = org.geotools.clustering.utils.ClusterKernelFactory.ValueType.EPANECHNIKOV; 
        float centreValue = (float) c.getStatistic();
        int radius = (int) (Math.floor(((c.radius + cellsize))) / cellsize);
        System.out.println("radius "+radius+" "+c.radius);
        double x = c.getCentre().getX();
        x = Math.ceil((x + cellsize / 2.0) / cellsize) * cellsize;
        double y = c.getCentre().getY();
        y = Math.ceil((y + cellsize / 2.0) / cellsize) * cellsize;
        DirectPosition2D pos = new DirectPosition2D(x, y);
        DirectPosition2D dest = new DirectPosition2D();
        GridCoordinates2D gridCoords = null;
       
         
            try {
                gridCoords = gg.worldToGrid(pos);
            } catch (InvalidGridGeometryException e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            } catch (TransformException e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            }
       System.out.println(gridCoords);
        KernelJAI kernel = ClusterKernelFactory.createCircle(radius, type, 1.0f);
        kernel=KernelUtil.standardize(kernel);
        float[] cdata = kernel.getKernelData();

        int k = 0;
        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++, k++) {
                System.out.println((gridCoords.x+i)+","+(gridCoords.y+j)+" "+cdata[k]);
                data[gridCoords.y+j][gridCoords.x+i]+=cdata[k]*centreValue;
            }
        }

    }

    /**
     * increment the raster at (x,y) by value;
     * 
     * @param x
     * @param y
     * @param value
     */
    protected GridCoordinates2D addToCell(double x, double y, double value) {
        GridCoordinates2D gridCoords = new GridCoordinates2D();
        int ix;
        int iy;
        DirectPosition2D pos = new DirectPosition2D(x, y);
        DirectPosition2D dest = new DirectPosition2D();
        try {
            gg.getCRSToGrid2D().transform((DirectPosition) pos, dest);
            gridCoords = gg.worldToGrid(pos);
            //System.out.println("pos " + pos + "\ndst " + dest + "\ngrd " + gridCoords);
            gridCoords.x = (int) Math.round(dest.x);
            gridCoords.y = (int) Math.round(dest.y);
        } catch (InvalidGridGeometryException e) {
            System.out.println(e);
            return null;
        } catch (TransformException e) {
            System.out.println(e);
            return null;
        }
        ix = gridCoords.x;
        iy = gridCoords.y;
        // System.out.println(ix+","+iy+" -> "+value);
        if (ix < 0 || ix >= data[0].length) {
            System.out.println("converting " + x + " gave " + ix);
        } else if (iy < 0 || iy >= data.length) {
            System.out.println("converting " + y + " gave " + iy);
        } else {
            data[iy][ix] += value;
        }
        return gridCoords;
    }
}
