import com.google.gson.JsonParser
import javafx.application.Platform
import javafx.beans.property.SimpleLongProperty
import javafx.geometry.Insets
import loci.formats.in.CellSensReader
import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs
import qupath.lib.regions.ImagePlane
import javafx.scene.Scene
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.TableColumn
import javafx.scene.control.ColorPicker
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.control.Tooltip
import javafx.stage.Stage
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.tools.ColorToolsFX;
import javafx.scene.paint.Color
import javafx.collections.FXCollections

import com.google.gson.GsonBuilder

import qupath.lib.scripting.QP

import javax.swing.event.ChangeListener

import static qupath.lib.gui.scripting.QPEx.*


import java.awt.Checkbox;

imageData = QPEx.getCurrentImageData();
def cells = getCellObjects();

//Settings to control the dialog boxes for the GUI
int col = 0
int row = 0
int textFieldWidth = 120
int labelWidth = 150
def gridPane = new GridPane()
gridPane.setPadding(new Insets(10, 10, 10, 10));

def server = getCurrentImageData().getServer()

def requestLabel = new Label("Set the number of phenotypes.")
gridPane.add(requestLabel,col, row++, 3, 1)
requestLabel.setAlignment(Pos.CENTER)
def path_json = buildFilePath(PROJECT_BASE_DIR,"classifiers","classes.json");
def json = new FileReader(path_json)  
def gson = new GsonBuilder()
        .setPrettyPrinting()
        .create()

def classes_map = gson.fromJson(json, Map.class)
list_classes = classes_map['pathClasses']['name']


TextField classText = new TextField("3");
classText.setMaxWidth( textFieldWidth);
classText.setAlignment(Pos.CENTER_RIGHT)
gridPane.add(classText, col++, row, 1, 1)
Button startButton = new Button()
startButton.setText("Start")
gridPane.add(startButton, col, row++, 1, 1)
startButton.setTooltip(new Tooltip("If you need to change the number of phenotypes, re-run the script"));

Button runButton = new Button()
runButton.setText('Run')


Platform.runLater {

    def stage = new Stage()
    stage.initOwner(QuPathGUI.getInstance().getStage())
    stage.setScene(new Scene( gridPane))
    stage.setTitle("Phenotypes")
    stage.setWidth(1000);
    stage.setHeight(400);
    //stage.setResizable(false);

    stage.show()

}

startButton.setOnAction {
   
    List<String> channels = new ArrayList<String>();
    for (elt in list_classes){
        channels.add(elt.split(': ')[-1]);
            }

    List<String> criteria = new ArrayList<String>();
    criteria.add("positive");
    criteria.add("negative");
    
    List<ComboBox> grid = new ArrayList<ComboBox>();
    List<TextField> phenoNames = new ArrayList<TextField>();
    List<TextField> criteriaNumbers = new ArrayList<TextField>()
    
    for (def i=0; i< Integer.parseInt(classText.getText()); i++) {
        List<ComboBox> phenotypeList = new ArrayList<ComboBox>();
        
        def loadLabel = new Label("Phenotype " + (i+1) + ":");
        gridPane.add(loadLabel, 0, i+2);
        TextField phenoName = new TextField("Name ");
        phenoNames.add(phenoName)
        gridPane.add(phenoName,1,i+2);
        TextField criteriaNumber = new TextField("2");
        cpt = 0;

        gridPane.add(criteriaNumber,2,i+2);
        criteriaNumbers.add(criteriaNumber)

        criteriaNumber.setOnAction() {

            for (def j=0; j< Integer.parseInt(criteriaNumber.getText()); j++) {
                def comboBoxChannel = new ComboBox(FXCollections.observableArrayList(channels));
                def comboBoxCriteria = new ComboBox(FXCollections.observableArrayList(criteria));

                phenotypeList.add(comboBoxChannel);
                phenotypeList.add(comboBoxCriteria);
                gridPane.add(comboBoxChannel,3+j*2,cpt+2);
                gridPane.add(comboBoxCriteria,4+j*2,cpt+2);

            }
            cpt++;
            grid.add(phenotypeList);

    }
}

    gridPane.add(runButton, 0, Integer.parseInt(classText.getText())+2);
    runButton.setOnAction {
        List<String> phenotypeNames = new ArrayList<String>();

        def nothing = getPathClass('Unidentified');

        cpt_phenotype = 0;

        List<Hashtable<String, String>> phenotypes = new ArrayList<Hashtable<String, String>>()

        for (i = 0; i < Integer.parseInt(classText.getText()); i++) {
            List<String> channelsList = new ArrayList<String>();
            List<String> criteriaList = new ArrayList<String>();
            def name = getPathClass(phenoNames[i].getText());

            for (j = 0; j < Integer.parseInt(criteriaNumbers[i].getText()) * 2; j++) {
                if (j % 2 == 0)
                    channelsList.add(grid[i][j].getValue());
                else
                    criteriaList.add(grid[i][j].getValue());
            }
            Hashtable<String, String> phenotype_dict = new Hashtable<String, String>();
            phenotype_dict["name"] = name
            phenotype_dict["channels"] = channelsList
            phenotype_dict["criterias"] = criteriaList
            phenotypes.add(phenotype_dict)
        }

        for (cell in cells){
            String str_class = cell.getPathClass().toString();
            Boolean cell_undefined = true
            
            for (phenotype in phenotypes){
                Iterator<String> iteratorChannels = phenotype["channels"].iterator();
                Iterator<String> iteratorCriterias = phenotype["criterias"].iterator();
                Boolean phenotype_valid = true

                while (iteratorChannels.hasNext() && iteratorCriterias.hasNext()) {
                    String channel = iteratorChannels.next()
                    String presence = iteratorCriterias.next()

                    if(str_class.contains(channel) && presence == "negative")
                    {
                        phenotype_valid = false
                        break;
                    }

                    if(!str_class.contains(channel) && presence == "positive")
                    {
                        phenotype_valid = false
                        break;
                    }

                }
                if(phenotype_valid){
                    //No need to check the other phenotypes
                    cell.setPathClass(phenotype["name"])
                    cell_undefined = false
                    break
                }

            }

            if (cell_undefined){
                cell.setPathClass(nothing)
            }

        }
    }
}




