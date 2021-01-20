import qupath.lib.objects.PathObject
import qupath.lib.objects.classes.PathClassFactory
import qupath.lib.scripting.QP


// Check if the version is ok
print 'Warning! This script requires the latest QuPath updates (currently v0.1.3 beta)'
def version = qupath.lib.gui.QuPathGUI.getInstance().versionString
if (version == null) {
    print 'Could not find the version string...'
} else if (version <= '0.1.2') {
    print 'This QuPath version is not supported!'
    return
}
print 'here'

// Create some simple classifiers based on a single measurement value,
// using either absolute thresholds or one computed using the median absolute deviation
double kHigh = 8
double kLow = 4
def kThreshold = {def a, def b, def c -> return MeasurementClassifier.createFromK(a, b, c)}
def absThreshold = {def a, def b, def c -> return MeasurementClassifier.createFromAbsoluteThreshold(a, b, c)}
def classifiers = [
        //kThreshold('beta-tubulin+','Target:beta-tubulin: Cytoplasm: Mean',
        kThreshold('Alexa_Fluor_488','Target:Alexa_Fluor_488: Cell: Mean',  kLow),  kLow),
        kThreshold('CD11b','Target:CD11b: Cell: Mean',  kLow),
        kThreshold('CD11c','Target:CD11c: Cell: Mean',  kLow),
        kThreshold('CD163','Target:CD163: Cell: Mean',  kLow),
        kThreshold('CD20','Target:CD20: Cell: Mean',  kLow),
        kThreshold('CD3','Target:CD3: Cell: Mean',  kLow),
        kThreshold('CD4','Target:CD4: Cell: Mean',  kLow),
        kThreshold('CD45','Target:CD45: Cell: Mean',  kLow),
        kThreshold('CD45RO','Target:CD45RO: Cell: Mean',  kLow),
        kThreshold('CD56','Target:CD56: Cell: Mean',  kLow),
        kThreshold('CD68','Target:CD68: Cell: Mean',  kLow),
        kThreshold('CD8','Target:CD8: Cell: Mean',  kLow),
        kThreshold('DC-SIGN','Target:CD8: Cell: Mean',  kLow),
        kThreshold('FOXP3','Target:FOXP3: Cell: Mean', kHigh),
        kThreshold('Granzyme_B','Target:Granzyme_B: Cell: Mean', kHigh),
        kThreshold('HLA_class_1_A,_B,_and_C,_Na-K-ATPase_alpha1','Target:HLA_class_1_A,_B,_and_C,_Na-K-ATPase_alpha1: Cell: Mean', kHigh),
        kThreshold('HLA_DR','Target:HLA_DR: Cell: Mean', kHigh),
        kThreshold('IDO-1','Target:IDO-1: Cell: Mean', kHigh),
        kThreshold('Keratin','Target:Keratin: Cell: Mean', kHigh),
        kThreshold('Ki-67','Target:Ki-67: Cell: Mean', kHigh),
        kThreshold('LAG3','Target:LAG3: Cell: Mean', kHigh),
        kThreshold('PD-1','Target:PD-1: Cell: Mean', kHigh),
        kThreshold('PD-L1','Target:PD-L1: Cell: Mean', kHigh),
        kThreshold('Podoplanin','Target:Podoplanin: Cell: Mean', kHigh),
        kThreshold('Vimentin','Target:Vimentin: Cell: Mean', kHigh),
]

// Lets set the default color to be the same as the default corresponding channel, if we can
try {
    def server = QPEx.getCurrentImageData().getServer()
    def previousColors = []
    for (int c = 0; c < server.nChannels(); c++) {
        def channel = server.getChannel(c)
        def channelName = channel.name
        //def color = channel.getColor()
        String[] splited = channel.toString().split("=");
        int R = Integer.parseInt(splited[2].split(',')[0])
        int G = Integer.parseInt(splited[3].split(',')[0])
        int B = Integer.parseInt(splited[4].substring(0,splited[4].length()-1))
        color_2 = getColorRGB(R,G,B)
        // Don't reuse colors!
        if (color_2 in previousColors)
            continue
        for (classifier in classifiers) {
            if (channelName.contains(classifier.classificationName)) {
                classifier.defaultColor = [R,G,B]
                previousColors.add(color_2)
                break
            }
        }
    }
} catch (Exception e) {
    println 'Unable to set colors from channels: ' + e.getLocalizedMessage()
}

// Apply classifications
def cells = QPEx.getCellObjects()
def undefined = getPathClass('Undefined')
//cells.each {it.setPathClass(undefined)}
for (classifier in classifiers) {
    println classifier.classifyObjects(cells)
}

println 'Undefined: \t' + cells.count {it.getPathClass() == 'Undefined'}
QPEx.fireHierarchyUpdate()


/**
 * Helper class to calculate & apply thresholds, resulting in object classifications being set.
 */
class MeasurementClassifier {

    String classificationName
    String measurementName
    double threshold = Double.NaN
    double k
    Integer[] defaultColor

    /**
     * Create a classifier using a calculated threshold value applied to a single measurement.
     * The actual threshold is derived from the measurement of a collection of objects
     * as median + k x sigma, where sigma is a standard deviation estimate derived from the median
     * absolute deviation.
     *
     * @param classificationName
     * @param measurementName
     * @param threshold
     * @return
     */
    static MeasurementClassifier createFromK(String classificationName, String measurementName, double k) {
        def mc = new MeasurementClassifier()
        mc.classificationName = classificationName
        mc.measurementName = measurementName
        mc.k = k
        return mc
    }

    /**
     * Create a classifier using a specific absolute threshold value applied to a single measurement.
     *
     * @param classificationName
     * @param measurementName
     * @param threshold
     * @return
     */
    static MeasurementClassifier createFromAbsoluteThreshold(String classificationName, String measurementName, double threshold) {
        def mc = new MeasurementClassifier()
        mc.classificationName = classificationName
        mc.measurementName = measurementName
        mc.threshold = threshold
        return mc
    }

    /**
     * Calculate threshold for measurementName as median + k x sigma,
     * where sigma is a standard deviation estimate
     * derived from the median absolute deviation.
     *
     * @param pathObjects
     * @return
     */
    double calculateThresholdFromK(Collection<PathObject> pathObjects) {
        // Create an array of all non-NaN values
        def allMeasurements = pathObjects.stream()
                .mapToDouble({p -> p.getMeasurementList().getMeasurementValue(measurementName)})
                .filter({d -> !Double.isNaN(d)})
                .toArray()
        double median = getMedian(allMeasurements)
        print(median)
        // Subtract median & get absolute value
        def absMedianSubtracted = Arrays.stream(allMeasurements).map({d -> Math.abs(d - median)}).toArray()
        Arrays.sort(absMedianSubtracted)

        // Compute median absolute deviation & convert to standard deviation approximation
        double medianAbsoluteDeviation = getMedian(absMedianSubtracted)
        double sigma = medianAbsoluteDeviation / 0.6745

        // Return threshold
        return median + k * sigma
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

    /**
     * Classify objects using the threshold defined here, calculated if necessary.
     *
     * @param pathObjects
     * @return
     */
    ClassificationResults classifyObjects(Collection<PathObject> pathObjects) {
        double myThreshold = Double.isNaN(threshold) ? calculateThresholdFromK(pathObjects) : threshold
        def positive = pathObjects.findAll {it.getMeasurementList().getMeasurementValue(measurementName) > myThreshold}
        positive.each {
            def currentClass = it.getPathClass()
            print(currentClass)
            def pathClass
            // Create a classifier - only assign the color if this is a single classification
            if (currentClass == 'undefined') {
                pathClass = PathClassFactory.getPathClass(classificationName, defaultColor)
                if (defaultColor != null)
                    pathClass.setColor(getColorRGB(defaultColor[0],defaultColor[1], defaultColor[2]))
            }
            else
                pathClass = PathClassFactory.getDerivedPathClass(currentClass, classificationName,null)
            it.setPathClass(pathClass)
        }
        return new ClassificationResults(actualThreshold: myThreshold, nPositive: positive.size())
    }


    class ClassificationResults {

        double actualThreshold
        int nPositive

        String toString() {
            String.format('%s: \t %d \t (%s > %.20f)', classificationName, nPositive, measurementName, actualThreshold)
        }

    }

}
