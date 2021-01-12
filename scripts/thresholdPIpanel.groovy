import qupath.tensorflow.stardist.StarDist2D

// Specify the model directory (you will need to change this!)
def pathModel = '/home/ninatubau/dsb2018_heavy_augment'

def stardist = StarDist2D.builder(pathModel)
        .threshold(0.5)              // Probability (detection) threshold
        .channels('DAPI')
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
def imageData = getCurrentImageData()
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




def measurementName_DAPI = 'DAPI: Nucleus: Mean'
def measurementName_570 = 'Opal 570: Nucleus: Mean'
def measurementName_690 = 'Opal 690: Nucleus: Mean'
def measurementName_480 = 'Opal 480: Nucleus: Mean'
def measurementName_620 = 'Opal 620: Nucleus: Mean'
def measurementName_780 = 'Opal 780: Nucleus: Mean'
def measurementName_520 = 'Opal 520: Nucleus: Mean'
int k_DAPI = 1
int k_570 = 1
int k_690 = 1
int k_480 = 1
int k_620 = 1
int k_780 = 1
int k_520 = 1

double getThreshold(def measurementName, int k){
    // CD8 thresholding
    def cells = getCellObjects()
    def allMeasurements = cells.stream()
            .mapToDouble({p -> p.getMeasurementList().getMeasurementValue(measurementName)})
            .filter({d -> !Double.isNaN(d)})
            .toArray()
    double median = getMedian(allMeasurements)
    
    // Subtract median & get absolute value
    def absMedianSubtracted = Arrays.stream(allMeasurements).map({d -> Math.abs(d - median)}).toArray()
    
    // Compute median absolute deviation & convert to standard deviation approximation
    double medianAbsoluteDeviation = getMedian(absMedianSubtracted)
    double sigma = medianAbsoluteDeviation / 0.6745
    
    // Return threshold
    double threshold = median + k * sigma
    return threshold
}


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

//def threshold_DAPI = getThreshold(measurementName_DAPI,k_DAPI)
def threshold_570 = getThreshold(measurementName_570,k_570)
def threshold_690 = getThreshold(measurementName_690,k_690)
def threshold_480 = getThreshold(measurementName_480,k_480)
def threshold_620 = getThreshold(measurementName_620,k_620)
def threshold_780 = getThreshold(measurementName_780,k_780)
def threshold_520 = getThreshold(measurementName_520,k_520)

//print(threshold_DAPI)
print(threshold_570)
print(threshold_690)
print(threshold_480)
print(threshold_620)
print(threshold_780)
print(threshold_520)

//classifier 
positive_epithilial = getPathClass('epithilial cell') 
positive_Tcell = getPathClass('T cell') 
positive_CD4_Tcell = getPathClass('CD4 T cell') 
positive_CD8_Tcell = getPathClass('CD8 T cell')
positive_Treg = getPathClass('T reg')
positive_PDL1 = getPathClass('PDL1+')
unidentified = getPathClass('Unidentified') 


for (cell in getCellObjects()) { 
    //chDAPI = measurement(cell, 'Nucleus: DAPI mean') 
    ch570 = measurement(cell, 'Opal 570: Nucleus: Mean') 
    ch690 = measurement(cell, 'Opal 690: Nucleus: Mean')
    ch480 = measurement(cell, 'Opal 480: Nucleus: Mean')
    ch620= measurement(cell, 'Opal 620: Nucleus: Mean')
    ch780= measurement(cell, 'Opal 780: Nucleus: Mean')
    ch520= measurement(cell, 'Opal 520: Nucleus: Mean')
    

       
    if (ch690>threshold_690 && ch570>threshold_570 && ch520>threshold_520)
        cell.setPathClass(positive_Treg)
        
    else if (ch690>threshold_690)
        //cell.setPathClass(Tcell)
        
        if (ch520>threshold_520)
            cell.setPathClass(positive_CD4_Tcell)
        
        else if (ch480>threshold_480)
            cell.setPathClass(positive_CD8_Tcell)
        
        else {
            cell.setPathClass(positive_Tcell)
            }
        
    else if (ch620>threshold_620 && ch780<threshold_780)
        cell.setPathClass(positive_PDL1)
        
    else if (ch780>threshold_780)
        cell.setPathClass(positive_epithilial)
    
    //else if (chDAPI>threshold_DAPI)
        //cell.setPathClass(DAPI)
    
    else{
        cell.setPathClass(unidentified)}
         
 } 
fireHierarchyUpdate() 

