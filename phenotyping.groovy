def nothing = getPathClass('Unidentified')
def proliferative = getPathClass('Proliferative Tcell')
def nonproliferative = getPathClass('Non proliferative T cell')

def opal_CD8Tcell = getPathClass('ChS2-T3')
def opal_CD4Tcell = getPathClass('Ch2-T1')
def opal_RegTcell = getPathClass('Ch2-T1: ChS2-T3')


for (cell in getCellObjects()){
    //print(cell)
    
    def pathClass = cell.getPathClass()
    str_class = pathClass.toString()
   
    //print(pathClass.toString())
    //def parent = cell.getParent()
    //print(parent)
       
    if (str_class.contains('ChS2-T3') && str_class.contains('Ch2-T1'))
        cell.setPathClass(proliferative)
    else if (str_class.contains('ChS2-T3') )
        cell.setPathClass(nonproliferative)
    else{
        print(str_class)
        cell.setPathClass(nothing)}
    

}
fireHierarchyUpdate()