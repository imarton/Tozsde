package tozsde;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author martoni
 */
public class start {
    public static void main(String[] args) {
        //teszt
 
        
    }
    /**
     * Adat forrás egy sorát alakítja át PowerChart XMLnek megfelelő alakúra
     * Forrás pl:
     * <TICKER>,<PER>,<DTYYYYMMDD>,<TIME>,<CLOSE>,<VOLUME>
     * OTP,0,20140121,090032,4415,775
     * 
     * @param line
     * @return 
     */
    private static Trans parseLine(String line) throws ParseException{
        Trans trans;
        String[] data = line.split(","); //OTP,0,20140121,090032,4415,775
        String datestr = data[2] + " " + data[3];
        DateFormat df = new SimpleDateFormat("yyyyMMdd HHmmss");                
//        try {
            Date d = df.parse(datestr);
            trans = new Trans();
            trans.name = data[0]; //name - "OTP"
            trans.time = d; //time - yyyyMMdd HHmmss
            trans.price = data[4]; //price - 4500
            trans.amount = data[5]; //amount - 100

//        } catch (ParseException e) {  
//            System.out.println("Sor kihagyva! Hiba a dátum konverziója közben: " + e.getLocalizedMessage());
//            return null;
//        } 
        return trans;
    }

    /**
     * A megadott file tartalmát soronként feldolgozza, a sorokból a későbbi XML file
     * előállításához megfelelő Trans pédányokat készít és ezeket Listában adja vissza.
     * @param file
     * @throws FileNotFoundException 
     */
    private static ArrayList<Trans> parseFile(File file) throws FileNotFoundException{
        ArrayList<Trans> elemList = new ArrayList<>();
        Scanner sc = new Scanner(file);
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            try {
                elemList.add(parseLine(line));
            } catch (ParseException ex) {
                Logger.getLogger(start.class.getName()).log(Level.SEVERE, "Sor kihagyva! " + line, ex);
            }
        }
        return elemList;
    }
    
    private static void createXMLData(ArrayList<Trans> transList){
        
    }
}
/**
 * Tranzakciók tárolását szolgáló osztály
 * Egy adott részvény egy vásárlás/eladását mutatja, illeve ennek
 * idejét, árfolyamát, mennyiségét
 * @author martoni
 */
class Trans{
    String name;
    Date time;
    String price;
    String amount;
    
}

/**
 * A diagram kirajzolásához szükséges adatokat tartalmazza:
 * - részvény vásárlás adatok
 * - gyertya periódus ideje
 * 
 * @author martoni
 */
class ChartDataManager{
    private final ArrayList<Trans> datas = new ArrayList<>();
    /**
     * A hasNextSet() és a nextSet() metóduskhoz egy mutató, amely
     * meadja hol tart a feldolgozás
     */
    private int lastPosition = 0;
    /**
     * A hasNextSet() és a nextSet() metóduskhoz egy mutató,
     * amely megadja mely időpillant utáni elemeket kell figyelembe venni.
     * A végét a periodTime határozza meg: beginofTimeWindow + periodTime
     * 
     * Megadott idő intervallum-nak megfelelően lépdelünk a kereskedési adatok lsitájában.
     * Egy gyertya mindig a beginofTimeWindow -tól a periódus idő által meghatározott endofTimeWindow -ig tart
     * azaz a gyertya ebben az időintervallumban lévő adatok alapján kerül majd kiszámolásra a nextSet()
     * metódus segítségével.
     */
    private Date beginofTimeWindow;
    
    /**
     * Candle period time in second
     * default: 1 minute;
     */
    private int periodTime = 1*60; 
    
    /**
     * A grafikon milyen időintervallumbeli adatok jelenjenek meg
     * Ez egy szűkítési lehetőség, ha nincs megadva az összes adat megjelenik
     */
    private Date startTime;
    private Date endTime;

    // chart grafikus jellemzői
    private short numPDivLines = 5;
    private String numberSuffix = " Ft";
    private String bearBorderColor = "E33C3C";
    private String bearFillColor = "E33C3C";
    private String bullBorderColor = "1F3165";
    private byte volumeHeightPercent = 20;
    private String caption = "2014.01.20";
    private String PYAxisName = "Árfolyam";
    private String VYAxisName = "Mennyiség";
    
    public ChartDataManager(ArrayList<Trans> datas) {
        if(datas != null)
            for (Trans t : datas) { //adatok bemásolása az osztály saját listájába
                this.datas.add(t);
            }
    }
   
    public Chart createChart(){
        Chart chart = new Chart();
        chart.PYAxisName = this.PYAxisName;
        chart.VYAxisName = this.VYAxisName;
        chart.bearBorderColor = this.bearBorderColor;
        chart.bearFillColor = this.bearFillColor;
        chart.bullBorderColor = this.bullBorderColor;
        chart.caption = this.caption;
        chart.numPDivLines = this.numPDivLines;
        chart.numberSuffix = this.numberSuffix;
        chart.volumeHeightPercent = this.volumeHeightPercent;
        
        DataSet ds = new DataSet();
        chart.addChild(ds);
        
        this.resetPosition();
        while(this.hasNextSet()){
            ds.addChild(this.nextSet());
        }
        
        Categories cats = new Categories();
        chart.addChild(cats);
        
        Category cat = new Category();
        cat.label = "9:00";
        cat.x = 0;
        cats.addChild(cat);
        
        cat = new Category();
        cat.label = "17:15";
        cat.x = 30;
        cats.addChild(cat);                
        
        
        
        
        return chart;
    }
    
    
    /**
     * Set period time in second
     * @param periodTime 
     */
    public void setPeriodTime(int periodTime) {
        this.periodTime = periodTime;
    }
    
    /**
     * Get period time in second
     * @return 
     */
    public int getPeriodTime() {
        return periodTime;
    }    
    
    /**
     * Az aktuális gyergya időintervallum végét határozza meg a 
     * az időintervallum kezdetéből (beginofTimeWindow) és a periódusidőből.
     * 
     * @return end time of candle stick
     */
    private Date getEndofTimeWindow(){
        return new Date(this.beginofTimeWindow.getTime() + this.periodTime*1000 );
    }
    
    /**
     * Megvizsgálja hogy a rendelkezésre álló kereskedési adatok alapján 
     * ki lehet-e számolni a következő Gyertyát.

     * 
     * @return 
     */
    private boolean hasNextSet() {
        if(lastPosition < this.datas.size())
            return false;
        if(this.datas.get(datas.size()-1).time.before(this.getEndofTimeWindow()) )
            return false;
        return true;
    }

    private ChildElem nextSet() {
        if(!this.hasNextSet())
           throw new NoSuchElementException();
        
        Set set = new Set();        
        while(true){
            if(this.lastPosition < this.datas.size())
                throw new IndexOutOfBoundsException(String.format("Last Position (%d) > data.size (%d)",this.lastPosition, this.datas.size()));            
        }
        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void resetPosition() {
        this.lastPosition = 0;
    }
    
}

/**
 * Chart gyertya elemek
 * http://docs.fusioncharts.com/powercharts/Contents/ChartSS/Candlestick.htm#Anchor20
 * @author martoni
 */
class Set extends ChildElem{
    public Integer open;
    public Integer close;
    public Integer hight;
    public Integer low;
    public Integer volume;
    public Integer x; //Egy sorszám: The x-axis value for the plot. The candlestick point will be placed horizontally on the x-axis based on this value.
    public String valuetext; //érték szöveg megjelenítése
    public String color; //hexa érték
    public String bordercolor;
    
}
class DataSet extends ChildElem{    
}


class Categories extends ChildElem{    
}
class Category extends ChildElem{
    public String label;
    public Short x;
}

class Chart extends ChildElem{
    public Short numPDivLines;
    public String caption;
    public String numberSuffix;
    public String bearBorderColor;
    public String bearFillColor;
    public String bullBorderColor;
    public String PYAxisName;
    public String VYAxisName;
    public Byte volumeHeightPercent; 
}



class ChildElem {
    private final ArrayList<ChildElem> children = new ArrayList<>();
    protected final char IDENTCHAR = '\t';
    
    public String makeXML() {
        return this.makeXML("");
    }
    private String makeXML(String ident) {
        StringBuilder sb = new StringBuilder();
        sb.append(ident).append("<").append(this.getClass().getSimpleName().toLowerCase());
        Field[] fields = this.getClass().getFields();
        try {
            for (Field field : fields) {
                if (field.get(this) != null) {
                    sb.append(" ").append(field.getName()).append("='")
                      .append(field.get(this))
                      .append("'");
                }//if
            }//for
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            Logger.getLogger(ChildElem.class.getName()).log(Level.SEVERE, null, ex);
        }
        Iterator<ChildElem> it = this.childList();
        if (!it.hasNext()) sb.append(" />");
        else {
            sb.append(">");
            while(it.hasNext()){
                sb.append("\n").append(it.next().makeXML(ident+IDENTCHAR));
            }
            sb.append("\n").append(ident).append("</").append(this.getClass().getSimpleName().toLowerCase()).append(">");            
        }
        
        return sb.toString();
    }
    
    public Iterator<ChildElem> childList(){
        return this.children.iterator();
    }
    
    public void addChild(ChildElem elem){
        this.children.add(elem);
    }
}
