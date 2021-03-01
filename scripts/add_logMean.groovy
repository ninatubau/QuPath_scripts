/**
 * Apply log to mean intensity measurements
 * Replaces the column 
*/

import qupath.imagej.objects.*

getCellObjects().each{

    def ml = it.getMeasurementList();
    String[] nameMeasurements = ml.getMeasurementNames();       
    def meanMeasurements = nameMeasurements.grep{it.contains("mean")}
    
    for (m in meanMeasurements){
     
        def mean = ml.getMeasurementValue(m)
        def log_mean = Math.log(mean)
        ml.putMeasurement(m, log_mean)
            
    }
    
}
