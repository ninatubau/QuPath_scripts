import qupath.tensorflow.stardist.StarDist2D

// Specify the model directory (you will need to change this!)
def pathModel = 'C:/Users/tubau.n/Desktop/dsb2018_heavy_augment'


def stardist = StarDist2D.builder(pathModel)
        .threshold(0.6)              // Probability (detection) threshold
        .channels('Ch1-T4')            // Select detection channel
        .normalizePercentiles(1, 99) // Percentile normalization
        .pixelSize(0.5)              // Resolution for detection
        .cellExpansion(1.5)          // Approximate cells based upon nucleus expansion
        .cellConstrainScale(1.5)     // Constrain cell expansion using nucleus size
        .ignoreCellOverlaps(true)
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
println 'Done!'