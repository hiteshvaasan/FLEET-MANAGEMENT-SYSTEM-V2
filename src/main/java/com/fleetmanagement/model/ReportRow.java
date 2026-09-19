package com.fleetmanagement.model;

import javafx.beans.property.*;

public class ReportRow {
    private final StringProperty col1 = new SimpleStringProperty();
    private final StringProperty col2 = new SimpleStringProperty();
    private final StringProperty col3 = new SimpleStringProperty();
    private final StringProperty col4 = new SimpleStringProperty();
    private final StringProperty col5 = new SimpleStringProperty();
    private final StringProperty col6 = new SimpleStringProperty();
    private final StringProperty col7 = new SimpleStringProperty();
    private final StringProperty col8 = new SimpleStringProperty();

    public ReportRow(String c1, String c2, String c3, String c4, String c5, String c6, String c7, String c8) {
        setCol1(c1); setCol2(c2); setCol3(c3); setCol4(c4);
        setCol5(c5); setCol6(c6); setCol7(c7); setCol8(c8);
    }

    public String getCol1() { return col1.get(); } public void setCol1(String v) { col1.set(v); } public StringProperty col1Property() { return col1; }
    public String getCol2() { return col2.get(); } public void setCol2(String v) { col2.set(v); } public StringProperty col2Property() { return col2; }
    public String getCol3() { return col3.get(); } public void setCol3(String v) { col3.set(v); } public StringProperty col3Property() { return col3; }
    public String getCol4() { return col4.get(); } public void setCol4(String v) { col4.set(v); } public StringProperty col4Property() { return col4; }
    public String getCol5() { return col5.get(); } public void setCol5(String v) { col5.set(v); } public StringProperty col5Property() { return col5; }
    public String getCol6() { return col6.get(); } public void setCol6(String v) { col6.set(v); } public StringProperty col6Property() { return col6; }
    public String getCol7() { return col7.get(); } public void setCol7(String v) { col7.set(v); } public StringProperty col7Property() { return col7; }
    public String getCol8() { return col8.get(); } public void setCol8(String v) { col8.set(v); } public StringProperty col8Property() { return col8; }
}