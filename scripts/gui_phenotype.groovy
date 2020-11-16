import javafx.collections.FXCollections
import javafx.scene.control.ComboBox

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