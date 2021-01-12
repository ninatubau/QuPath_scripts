// https://github.com/qupath/qupath/issues/223
//Note that this only applies while the current image is active. Reopening or switching images resets to the image's own metadata.
// Set the magnification & pixel size (be cautious!!!)
def metadata = getCurrentImageData().getServer().getOriginalMetadata()
metadata.magnification = 40
metadata.pixelWidthMicrons = 0.25
metadata.pixelHeightMicrons = 0.25

// If you want to trigger the 'Image' tab on the left to update, try setting a property to something different (and perhaps back again)
type = getCurrentImageData().getImageType()
setImageType(null)
setImageType(type)

//THIS HAS CHANGED IN MORE RECENT VERSIONS. I do not know exactly when, but at least as of M7, adjusting the metada should be done as follows
import qupath.lib.images.servers.ImageServerMetadata

def imageData = getCurrentImageData()
def server = imageData.getServer()

def oldMetadata = server.getMetadata()
def newMetadata = new ImageServerMetadata.Builder(oldMetadata)
    .magnification(10.0)
    .pixelSizeMicrons(1.25, 1.25)
    .build()
imageData.updateServerMetadata(newMetadata)