import qupath.tensorflow.stardist.StarDist2D
def project = getProject()
for (entry in project.getImageList()) {

    def imageData = entry.readImageData()
    def hierarchy = imageData.getHierarchy()
    setColorDeconvolutionStains('{"Name" : "H-DAB modified", "Stain 1" : "Hematoxylin", "Values 1" : "0.6717 0.69069 0.26788 ", "Stain 2" : "DAB", "Values 2" : "0.32504 0.69709 0.63908 ", "Stain 3" : "Residual", "Values 3" : "0.21296 0.95881 0.18796 ", "Background" : " 255 255 255 "}');
    def annotations = hierarchy.getAnnotationObjects()
    print entry.getImageName() + '\t' + annotations.size()
    annotations.each{
        selectAnnotation()
        // Specify the model directory (you will need to change this!)
        def pathModel = '/home/ninatubau/he_heavy_augment'
        
        def stardist = StarDist2D.builder(pathModel)
                .threshold(0.6)              // Probability (detection) threshold
                .normalizePercentiles(1, 99) // Percentile normalization
                .pixelSize(0.25)              // Resolution for detection
                .cellExpansion(0.1)          // Approximate cells based upon nucleus expansion
                .cellConstrainScale(1.5)     // Constrain cell expansion using nucleus size
                .ignoreCellOverlaps(false)
                .measureShape()              // Add shape measurements
                .measureIntensity()          // Add cell measurements (in all compartments)
                .includeProbability(true)    // Add probability as a measurement (enables later filtering)
                .build()
               
        
        // Run detection for the selected objects
        //def imageData = getCurrentImageData()
        def pathObjects = getSelectedObjects()
        if (pathObjects.isEmpty()) {
            Dialogs.showErrorMessage("StarDist", "Please select a parent object!")
            return
        }
        
        stardist.detectObjects(imageData, pathObjects)
        
        def cellList = getCellObjects()
        println 'Detection Done!'
        
        //classifier red
        import org.apache.commons.math3.stat.descriptive.SummaryStatistics
        
        //def measurementName = 'Ch2-T1: Nucleus: Mean'
        double k =1
        
        def cells = getCellObjects()
        
        def measurementName_cd8 = 'DAB: Nucleus: Mean'
        def measurementName_ki67 = 'Residual: Nucleus: Mean'
        double k_cd8 = 5
        double k_ki67 = 8
        
        // CD8 thresholding
        def allMeasurements_cd8 = cells.stream()
                .mapToDouble({p -> p.getMeasurementList().getMeasurementValue(measurementName_cd8)})
                .filter({d -> !Double.isNaN(d)})
                .toArray()
        double median_cd8 = getMedian(allMeasurements_cd8)
        
        // Subtract median & get absolute value
        def absMedianSubtracted_cd8 = Arrays.stream(allMeasurements_cd8).map({d -> Math.abs(d - median_cd8)}).toArray()
        
        // Compute median absolute deviation & convert to standard deviation approximation
        double medianAbsoluteDeviation_cd8 = getMedian(absMedianSubtracted_cd8)
        double sigma_cd8 = medianAbsoluteDeviation_cd8 / 0.6745
        
        // Return threshold
        double threshold_cd8 = median_cd8 + k_cd8 * sigma_cd8
        
        //Ki67 thesholding
        def allMeasurements_ki67 = cells.stream()
                .mapToDouble({p -> p.getMeasurementList().getMeasurementValue(measurementName_ki67)})
                .filter({d -> !Double.isNaN(d)})
                .toArray()
        double median_ki67 = getMedian(allMeasurements_ki67)
        
        // Subtract median & get absolute value
        def absMedianSubtracted_ki67 = Arrays.stream(allMeasurements_ki67).map({d -> Math.abs(d - median_ki67)}).toArray()
        
        // Compute median absolute deviation & convert to standard deviation approximation
        double medianAbsoluteDeviation_ki67 = getMedian(absMedianSubtracted_ki67)
        double sigma_ki67 = medianAbsoluteDeviation_ki67 / 0.6745
        
        // Return threshold
        double threshold_ki67 = median_ki67 + k_ki67 * sigma_ki67
        
        /**
         * Get median value from array (this will sort the array!)
         */
        double getMedian(double[] vals) {
            if (vals.length == 0)
                return Double.NaN
            Arrays.sort(vals)
            if (vals.length % 2 == 1)
                return vals[(int)(vals.length / 2)]
            else
                return (vals[(int)(vals.length / 2)-1] + vals[(int)(vals.length / 2)]) / 2.0
        }
        //classifier green
        positive_both = getPathClass('CD8+, Ki67+') 
        positive_ki67 = getPathClass('Ki67+') 
        positive_cd8 = getPathClass('CD8+') 
        red_blood_cell = getPathClass('Red blood cell')
        unidentified = getPathClass('Unidentified') 
        
        print(threshold_cd8)
        print(threshold_ki67)
        
        //threshold_red =11.8
        //rthreshold_green=5.5
        
        for (cell in getCellObjects()) { 
             ch1 = measurement(cell, 'DAB: Nucleus: Mean') 
             ch2 = measurement(cell, 'Residual: Nucleus: Mean') 
             ch_rbc = measurement(cell, 'Nucleus: Circularity')
             //ch_rbc2 = measurement(cell, 'Nucleus: Area µm^2')
             ch_rbc3 = measurement(cell, 'Hematoxylin: Nucleus: Mean')
             //print(ch_rbc2)
             //ch2 = measurement(cell, 'Cell: Channel 2 mean') 
             if (ch1 >threshold_cd8 && ch2 > threshold_ki67) 
                 cell.setPathClass(positive_both) 
             else if (ch1 > threshold_cd8) 
                 cell.setPathClass(positive_cd8) 
             else if (ch2 > threshold_ki67) 
                 cell.setPathClass(positive_ki67) 
             else if(ch_rbc>0.7 && ch_rbc3<0.11)
                 cell.setPathClass(red_blood_cell)
             else 
                 cell.setPathClass(unidentified)
                 
         } 
        //fireHierarchyUpdate() 
        // classifier green
        
        
        fireHierarchyUpdate() 
        createAnnotationsFromPixelClassifier("bestClass3", 0.0, 0.0)
        selectObjectsByClassification("abnormal");
        runPlugin('qupath.lib.plugins.objects.SplitAnnotationsPlugin', '{}');
        def Annotations = getAnnotationObjects()
        def smallAnnotations = getAnnotationObjects().findAll {it.getROI().getArea() < 25000}
        getCurrentHierarchy().getSelectionModel().setSelectedObjects(smallAnnotations, null)
        smallAnnotations.each {
            it.setPathClass(getPathClass("normal"))  
        }     
    }
}
