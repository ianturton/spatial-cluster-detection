/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geotools.clustering.utils;

import com.vividsolutions.jts.geom.Polygon;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.geotools.clustering.Circle;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author ijt1
 */
public class Utilities {

    static public void writeCircles(File name, FeatureCollection outfeatures) throws IOException, MalformedURLException {
        DataStoreFactorySpi dataStoreFactory = new ShapefileDataStoreFactory();

        System.out.println("writing " + name.getAbsolutePath());
        URL url = DataUtilities.fileToURL(name);
        Map<String, Serializable> params2 = new HashMap<String, Serializable>();
        params2.put("url", url);
        params2.put("create spatial index", Boolean.TRUE);
        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params2);
        newDataStore.createSchema((SimpleFeatureType) outfeatures.getSchema());
        Transaction transaction = new DefaultTransaction("create");
        String typeName = newDataStore.getTypeNames()[0];
        FeatureSource outfeatureSource = newDataStore.getFeatureSource(typeName);
        if (outfeatureSource instanceof FeatureStore) {
            FeatureStore featureStore = (FeatureStore) outfeatureSource;
            featureStore.setTransaction(transaction);
            try {
                featureStore.addFeatures(outfeatures);
                transaction.commit();
            } catch (Exception problem) {
                problem.printStackTrace();
                transaction.rollback();
            } finally {
                transaction.close();
            }
        }
    }

    public static void writeCircles(String name, FeatureCollection circles) throws IOException {
        File newFile = new File(name);
        writeCircles(newFile, circles);
    }

    static public void writeGrid(File out, GridCoverage2D grid) throws IndexOutOfBoundsException, IOException, IllegalArgumentException {
        GeoTiffWriter gtw = new GeoTiffWriter(out);
        gtw.write(grid, null);
    }

    static public void writeGrid(String name, GridCoverage2D grid) throws IndexOutOfBoundsException, IOException {
        if (!name.endsWith(".tiff")) {
            name = name + ".tiff";
        }
        File out = new File(name);
        writeGrid(out, grid);
    }

    static public FeatureCollection circles2FeatureCollection(ArrayList<Circle> results, CoordinateReferenceSystem crs) {
        SimpleFeature feature;
        FeatureCollection circles = FeatureCollections.newCollection();
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("Location");
        builder.setCRS(crs); // <- Coordinate reference system
        // add attributes in order
        builder.add("Circle", Polygon.class);
        builder.add("Statisic", Double.class);
        builder.add("X", Double.class);
        builder.add("Y", Double.class);
        builder.add("R", Double.class);
        SimpleFeatureType TYPE = builder.buildFeatureType();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
        for (Circle c : results) {
            featureBuilder.add(c.toPolygon());
            featureBuilder.add(c.getStatistic());
            featureBuilder.add(c.getCentre().getCoordinate().x);
            featureBuilder.add(c.getCentre().getCoordinate().y);
            featureBuilder.add(c.getRadius());
            feature = featureBuilder.buildFeature(null);
            circles.add(feature);
        }
        return circles;
    }
}
