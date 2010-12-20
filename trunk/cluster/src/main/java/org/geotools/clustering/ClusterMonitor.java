/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2004-2010, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */

package org.geotools.clustering;


import org.opengis.util.InternationalString;
import org.opengis.util.ProgressListener;

/**
 * @author ijt1
 *
 */
public class ClusterMonitor implements ProgressListener {
    InternationalString task;
    float progress = 0.0f;
    private String description;
    /* (non-Javadoc)
     * @see org.opengis.util.ProgressListener#getTask()
     */
    public InternationalString getTask() {
        // TODO Auto-generated method stub
        return task;
    }

    /* (non-Javadoc)
     * @see org.opengis.util.ProgressListener#setTask(org.opengis.util.InternationalString)
     */
    public void setTask(InternationalString task) {
        // TODO Auto-generated method stub
        this.task = task;
    }

    /* (non-Javadoc)
     * @see org.opengis.util.ProgressListener#getProgress()
     */
    public float getProgress() {
        // TODO Auto-generated method stub
        return progress;
    }

    /* (non-Javadoc)
     * @see org.geotools.util.ProgressListener#getDescription()
     */
    public String getDescription() {
        // TODO Auto-generated method stub
        return description;
    }

    /* (non-Javadoc)
     * @see org.geotools.util.ProgressListener#setDescription(java.lang.String)
     */
    public void setDescription(String description) {
        // TODO Auto-generated method stub
        this.description = description;
    }

    /* (non-Javadoc)
     * @see org.geotools.util.ProgressListener#started()
     */
    public void started() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.geotools.util.ProgressListener#progress(float)
     */
    public void progress(float percent) {
        // TODO Auto-generated method stub
        progress=percent;
    }

    /* (non-Javadoc)
     * @see org.geotools.util.ProgressListener#complete()
     */
    public void complete() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.geotools.util.ProgressListener#dispose()
     */
    public void dispose() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.geotools.util.ProgressListener#isCanceled()
     */
    public boolean isCanceled() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.geotools.util.ProgressListener#setCanceled(boolean)
     */
    public void setCanceled(boolean cancel) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.geotools.util.ProgressListener#warningOccurred(java.lang.String, java.lang.String, java.lang.String)
     */
    public void warningOccurred(String source, String margin, String warning) {
        // TODO Auto-generated method stub
        System.out.println(source+" "+warning);
    }

    /* (non-Javadoc)
     * @see org.geotools.util.ProgressListener#exceptionOccurred(java.lang.Throwable)
     */
    public void exceptionOccurred(Throwable exception) {
        // TODO Auto-generated method stub
        exception.printStackTrace();
        System.out.println(exception.getLocalizedMessage());
    }

}
