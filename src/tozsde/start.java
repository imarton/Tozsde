package tozsde;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
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
        try {
            //teszt
            ChartDataManager cman = new ChartDataManager(parseFile(new File("F:\\munka\\PowerCharts_XT_Eval\\Tozsde\\data\\nyers\\20140131.txt")));
            
            FileOutputStream fos = new FileOutputStream("F:\\munka\\PowerCharts_XT_Eval\\Tozsde\\data\\Tozsde1.xml");
            OutputStreamWriter osw = null;
            try {
                osw = new OutputStreamWriter(fos, "UTF-8");
                osw.write(cman.createChart().makeXML());
                //System.out.println(cman.createChart().makeXML());
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(start.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(start.class.getName()).log(Level.SEVERE, null, ex);
            }finally{
                if(osw != null)
                    try {
                        osw.close();
                } catch (IOException ex) {
                    Logger.getLogger(start.class.getName()).log(Level.SEVERE, null, ex);
                }                
            }
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(start.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println(" -- KÉSZ -- ");
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
            trans.time = d; //time - yyyy.MMdd HHmmss
            trans.price = Integer.parseInt(data[4]); //price - 4500
            trans.amount = Integer.parseInt(data[5]); //amount - 100

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
//        int i = 0;
        while (sc.hasNextLine()) {
//            i++;
            String line = sc.nextLine();
            try {
                Trans t = parseLine(line);
//                System.out.format("%d. %s\n", i, t);
                elemList.add(t);
            } catch (ParseException ex) {
                Logger.getLogger(start.class.getName()).log(Level.SEVERE, "Sor kihagyva! " + line, ex);
            }
        }
        return elemList;
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
    int price;
    int amount;

    @Override
    public String toString() {
        SimpleDateFormat sf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        return String.format("%s: %s.- (%s db)", sf.format(time), price, amount);
    }
    
    
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
    private Date startTime;
    
    /**
     * Candle period time in second
     * default: 1 minute;
     */
    private int periodTime = 1*60; 
    

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
        
        try {
            this.startTime = (new SimpleDateFormat("yyyy.MM.dd HH:mm")).parse("2014.01.31 09:00");
        } catch (ParseException ex) {
            Logger.getLogger(ChartDataManager.class.getName()).log(Level.SEVERE, null, ex);
            this.startTime = new Date();
        }
        
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
        
        this.resetPosition(); // chart gyertyáinak kiszámítsa előtt lapahelyzetbe állítjuk az adatok feldolgozottságát jelző mutatót.
        while(this.hasNextSet()){ // chart gyertyáinak kiszámítsa 
            Set s = this.nextSet();
            if(!s.empty())
                ds.addChild(s);
        }
        
        
        Categories cats = new Categories();
        chart.addChild(cats);
        SimpleDateFormat sf = new SimpleDateFormat("yyyy.MM.dd HH:mm");
        Category cat = new Category();
        cat.label = sf.format(chart.getStartTime());
        cat.x = 0;
        cats.addChild(cat);
        
        cat = new Category();
        cat.label = sf.format(chart.getEndTime());
        cat.x = chart.getNumberOfCandle();
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
    private Date getEndTime(){
        return new Date(this.startTime.getTime() + this.periodTime*1000 );
    }
    
    /**
     * Megvizsgálja hogy a rendelkezésre álló, még fel nem dolgozott kereskedési adatok alapján 
     * ki lehet-e számolni a következő Gyertyát.
     * <br> Használd a nextSet() metódust a Gyertya elem előállításához.
     * 
     * @return előállítható a a következő Gyertya elem 
     */
    private boolean hasNextSet() {
        //ha nincs feldolgozatlan adat a listában
        if(lastPosition >= this.datas.size())
            return false;
        return true;
//        //meghatározzuk az utolsó kereskedési adat idejét
//        //majd eldöntjük, hogy ez az időpont az aktuális Gyerta által lefedett időintervallum záró ideje ELŐTT van-e
//        // - ha igen, akkor hamissal térünk vissza ugyanis ez azt jelenti, hogy nincs a Gyerta befejezéséhez elegendő adat
//        // - ha nem, akkor igazzal térünk vissza
//        return !this.datas.get(datas.size()-1).time.before(this.getEndTime());
    }

    /**
     * A feldolgozatlandatokból  - amelyek a megfelelő időintervallumban vannak - 
     * előállít egy Gyertya elemet.
     * @return 
     */
    private Set nextSet() {
        if(!this.hasNextSet())
           throw new NoSuchElementException();
        
        Set set = new Set(); 
//        SimpleDateFormat sf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
//        System.out.format("Set start -- start time: %s, start pos: %d, end time: %s  \n",sf.format(startTime), lastPosition, sf.format(getEndTime()) );
//        int i = 0;
        while(lastPosition < this.datas.size()){
            if(this.datas.get(lastPosition).time.after(this.getEndTime())) //ha az aktuális pozicíóban lévő adat már nincs benne az aktuális gyertya intervallumában
                break;
//            i++;
//            System.out.println("\t"+i + "." +datas.get(lastPosition));
            set.add(this.datas.get(lastPosition));
            lastPosition++;
        }
        this.startTime = this.getEndTime();
//        System.out.println("Set end -- " + set);
        return set;
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
    /**
     * A gyertya által lefedett időintervallum nyitó ideje
     */
    private Date startTime;
    /**
     * A gyertya által lefedett időintervallum záró ideje
     */
    private Date endTime;
    
    /**
     * Nyitóár
     */
    public Integer open; 
    /**
     * Záróár
     */
    public Integer close;
    /**
     * Legmagasabb ár
     */
    public Integer high;
    /**
     * Legalacsonyabb át
     */
    public Integer low;
    /**
     * Tranzakciók összértéke
     */
    public Integer volume;
    /**
     * Egy sorszám: The x-axis value for the plot. The candlestick point will be placed horizontally on the x-axis based on this value.
     */
    public Integer x;
    /**
     * A charton a gyertya fölött megjelenithető érték szöveg 
     */
    public String valuetext; 
    /**
     * Gyertya színe: kitöltés
     * hexa érték
     */
    public String color;
    /**
     * Gyertya színe: körvonal
     * hexa érték
     */
    public String bordercolor;
    
    
    /**
     * A tranzakció adatait beépíti a gyertyába
     * @param t Tranzakció
     */
    public void add(Trans t){
        if(this.open == null){ //az első tranzakció árfolyama a nyitó ár és annak időpontja
            this.open = t.price; 
            this.startTime = t.time;
            this.volume = 0;
            this.high = t.price;
            this.low = t.price;
        }
        
        this.close = t.price;  // a záróárba is mindig eltároljuk a tranzakció árát, így a végén az utólsó tranzakció ára lesz benne.
        this.endTime = t.time; // hasonlóan az időt is
        
        if(t.price > this.high)
            this.high = t.price;
        if(t.price < this.low)
            this.low = t.price;
        
        this.volume += t.amount;
    }

//    /**
//     * Beállítja a gyertya által lefedett időintervallum kezdetét
//     * @param endTime 
//     */
//    public void setEndTime(Date endTime) {
//        this.endTime = endTime;
//    }
//    /**
//     * Beállítja a gyertya által lefedett időintervallum végét
//     * @param startTime 
//     */
//    public void setStartTime(Date startTime) {
//        this.startTime = startTime;
//    }
    
    
    /**
     * Visszaadja a gyertya által lefedett időintervallum végét
     * @return 
     */
    public Date getEndTime() {
        return endTime;
    }
    /**
     * Visszaadja a gyertya által lefedett időintervallum kezdetét
     * @return 
     */
    public Date getStartTime() {
        return startTime;
    }

    @Override
    public String toString() {
        if(this.empty())
            return "empty set";
        
        SimpleDateFormat sf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        return String.format("Open-Close: %d-%d; High-Low: %d-%d; Start-End time: %s - %s; Amount: %d", open, close, high, low, sf.format(startTime), sf.format(endTime), volume );
        
    }    

    public boolean empty() {
        return this.open == null;
    }
}
class DataSet extends ChildElem{  

    @Override
    public void addChild(ChildElem elem) {
        ((Set)elem).x = this.getNumberOfChildren()+1;
        super.addChild(elem);         
    }
    /**
     * Visszaadja az első gyertya elemet.     
     * @return az első Gyertya elem illetve null ha nincs eleme a DataSet-nek.
     */
    protected Set getFirstCandle() {        
        return this.getNumberOfChildren()==0?null:(Set)this.getChild(0);
    }

    
    /**
     * Visszaadja az utolsó gyertya elemet.     
     * @return az utolsó Gyertya elem illetve null ha nincs eleme a DataSet-nek.
     */
    protected Set getLastCandle() {
        return this.getNumberOfChildren()==0?null:(Set)this.getChild(this.getNumberOfChildren()-1);
    }
    
}


class Categories extends ChildElem{    
}
class Category extends ChildElem{
    public String label;
    public Integer x;
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

    /**
     * Chartban lévő Gyertyák száma
     * @return 
     */
    protected int getNumberOfCandle() {        
        //megkeressük a Chart DataSet típusú elemét és lekérdezzük az abban lévő gyermekek (azaz Gyertyák) számát
        DataSet ds = this.getDataSet();
        return ds==null?0:ds.getNumberOfChildren();
    }
    
    /**
     * visszaadja a Chart-hoz tartozó DataSet-t (gyertyák halmaza)
     * @return 
     */
    private DataSet getDataSet(){
        Iterator<ChildElem> it = this.childList();
        while(it.hasNext()){
            ChildElem ce = it.next();
            if(ce instanceof DataSet){                
                return ((DataSet)ce);
            }
        }
        return null;
    }

    public Date getStartTime() {
        return this.getDataSet().getFirstCandle().getStartTime();
    }

    public Date getEndTime() {
        return this.getDataSet().getLastCandle().getEndTime();
    }
}



class ChildElem {
    private final ArrayList<ChildElem> children = new ArrayList<>();
    protected final char IDENTCHAR = '\t';
    
    public String makeXML() {
        return this.makeXML("");
    }
    private String makeXML(String ident) {
        //System.out.println(ident + this.getClass().getSimpleName());
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
    
    protected ChildElem getChild(int index){
        return this.children.get(index);
    }
    
    /**
     * az adott elemhez tartozó gyermekelemek száma
     * @return 0 vagy nagyobb
     */
    public int getNumberOfChildren(){
        return this.children.size();
    }
    
    public void addChild(ChildElem elem){
        this.children.add(elem);
    }
}
