import graph.Plot;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Orbite extends JComponent {

  private Plot graphArea = null;
  private JPanel buttonBox, pBox;
  private JButton accelereButton, ralentitButton, pauseButton;
  private JButton loadConfig, saveConfig;
  private JSpinner pSpinner;
  private JButton colorButton;
  private JLabel vLabel, LLabel, historyLabel, dvLabel;
  private SpinnerNumberModel pSpinnerModel;
  private JTextField historyField, dvField;

  private double[][] xx;
  private double[] v;
  private double[] L;
  private double x0=0.0, y0=0.0;
  private int ip = 0;  // index of projectile to be "piloted"
  private int np; // number of projectiles
  private double dv = 0.1;
  private static double G = 1.0, M = 1.0, dt=0.005;
  private boolean running = true;

  /////////////////////////////////////////////////////////////////////////////
  // The main program
  /////////////////////////////////////////////////////////////////////////////
  public static void main(String args[]) {
    Orbite monOrbite = null;
    if (args.length == 0) {
        monOrbite = new Orbite("");
    } else {
        monOrbite = new Orbite(args[0]);
    }
    JFrame f = new JFrame("Orbite 1.0");
    f.getContentPane().add("Center",monOrbite);
    f.setSize(800,800);
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    f.setVisible(true);
    monOrbite.lanceOrbite();
  }

  /////////////////////////////////////////////////////////////////////////////
  /* a constructor */
  /////////////////////////////////////////////////////////////////////////////
  public Orbite(String filename) {
    // set English locale to avoid things like decimal commas
    Locale.setDefault(new Locale("en","US"));

    np = 2;
    xx = new double[np][];
    v = new double[np];
    L = new double[np];
    xx[0] = new double[] {0.0, 0.5, 1.5, 0.0};
    xx[1] = new double[] {0.0, 0.6, 1.5, 0.0};
    double planetSize = 0.1;
    Color[] pColors = {Color.green, Color.blue};
    find_vL();
    graphArea = new Plot(planetSize,pColors);

    if (filename.length() > 0) readConfig(filename);
    PlotMouseListener l = new PlotMouseListener(graphArea);
    graphArea.addMouseListener(l);
    graphArea.addMouseMotionListener(l);
    graphArea.addMouseWheelListener(l);
    setLayout(new BorderLayout());
    add("Center",graphArea);

    initButtons();
    buttonBox = new JPanel();
    buttonBox.setLayout(new FlowLayout(FlowLayout.CENTER));
    buttonBox.add(pauseButton);
    buttonBox.add(new JLabel("  "));
    buttonBox.add(ralentitButton);
    buttonBox.add(accelereButton);
    add("South",buttonBox);

    initPlabels();
    refreshPlabels();
    pBox = new JPanel();
    pBox.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridx = 0;
    c.gridy = 0;
    c.gridwidth = 1;
    pBox.add(loadConfig,c);
    c.gridy++;
    pBox.add(saveConfig,c);
    c.gridy++;
    pBox.add(new JLabel(" "),c);
    c.gridy++;
    pBox.add(pSpinner,c);
    c.gridy++;
    pBox.add(colorButton,c);
    c.gridy++;
    pBox.add(vLabel,c);
    c.gridy++;
    pBox.add(LLabel,c);
    c.gridy++;
    pBox.add(dvLabel,c);
    c.gridy++;
    pBox.add(dvField,c);
    c.gridy++;
    pBox.add(new JLabel(" "),c);
    c.gridy++;
    pBox.add(historyLabel,c);
    c.gridy++;
    pBox.add(historyField,c);
    add("East",pBox);
  } 

  /////////////////////////////////////////////////////////////////////////////
  // Reads a configuration file
  /////////////////////////////////////////////////////////////////////////////
  private void readConfig(String filename) {
    String line;
    BufferedReader in;
    StringTokenizer t;
    int i, j;
    double planetSize; 
    Color[] pColors;
    try {
      in = new BufferedReader(new FileReader(filename));
      this.G = parseDouble(trim(in.readLine()));
      this.M = parseDouble(trim(in.readLine()));
      planetSize = parseDouble(trim(in.readLine()));
      this.dt = parseDouble(trim(in.readLine()));
      this.np = Integer.parseInt(trim(in.readLine()).trim());
      xx = new double[np][4];
      pColors = new Color[np];
      for(i=0; i<np; i++) {
        t = new StringTokenizer(trim(in.readLine())," \t");
        for(j=0; j<4; j++) xx[i][j] = parseDouble(t.nextToken());
        pColors[i] = new Color(Integer.parseInt(t.nextToken()));
      }
      in.close();
    } catch(Exception e) {
      JOptionPane.showMessageDialog(
        loadConfig,
        "Unable to read file \""+filename+"\"",
        "Failure",
        JOptionPane.WARNING_MESSAGE
      );
      return;
    }

    graphArea.replace(planetSize,pColors);
    v = new double[np];
    L = new double[np];
  }

  /////////////////////////////////////////////////////////////////////////////
  // Reads a configuration file
  /////////////////////////////////////////////////////////////////////////////
  private void writeConfig(String filename) {
    File f = new File(filename);
    PrintWriter out;
    try {
      out = new PrintWriter(new FileWriter(f));
    } catch(Exception e) {
      JOptionPane.showMessageDialog(
        saveConfig,
        "Unable to write to file \""+filename+"\"",
        "Failure",
        JOptionPane.WARNING_MESSAGE
      );
      return;
    }
    out.printf("%.15g # Gravitational constant%n",this.G);
    out.printf("%.15g # Planet mass%n",this.M);
    out.printf("%.15g # Planet radius%n",graphArea.planetSize);
    out.printf("%.15g # Time step%n",this.dt);
    out.printf("%d # Number of projectiles%n",this.np);
    for(int i=0; i<np; i++) {
      out.printf("%g %g %g %g %s # Particule %d%n",xx[i][0],xx[i][1],xx[i][2],xx[i][3],
                 Integer.toString(graphArea.pColors[i].getRGB()),i);
    }
    out.close();
  }

  /////////////////////////////////////////////////////////////////////////////
  // Initialises the buttons on the south panel
  /////////////////////////////////////////////////////////////////////////////
  private void initButtons() {
    accelereButton = new JButton("+");
    ralentitButton = new JButton("-");
    if (running)  {
      pauseButton = new JButton("\u2016"); // (= pause)
    } else {
      pauseButton = new JButton("\u25BA"); // (= run)
    }

    accelereButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        xx[ip][2] += (dv*xx[ip][2])/v[ip];
        xx[ip][3] += (dv*xx[ip][3])/v[ip];
        v[ip] = Math.sqrt(xx[ip][2]*xx[ip][2]+xx[ip][3]*xx[ip][3]);
        L[ip] = xx[ip][3]*(xx[ip][0]-x0) - xx[ip][2]*(xx[ip][1]-y0);
        refreshPlabels();
      }
    });
    ralentitButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        xx[ip][2] -= (dv*xx[ip][2])/v[ip];
        xx[ip][3] -= (dv*xx[ip][3])/v[ip];
        v[ip] = Math.sqrt(xx[ip][2]*xx[ip][2]+xx[ip][3]*xx[ip][3]);
        L[ip] = xx[ip][3]*(xx[ip][0]-x0) - xx[ip][2]*(xx[ip][1]-y0);
        refreshPlabels();
      }
    });

    pauseButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        // \u25BA (= run) \u23F8 (= pause)
        if (running) {
          running = false;
          pauseButton.setText("\u25BA");
        } else {
          running = true;
          pauseButton.setText("\u2016");
        }
      }
    });
  }

  /////////////////////////////////////////////////////////////////////////////
  // Initialises the components the east panel
  /////////////////////////////////////////////////////////////////////////////
  private void initPlabels() {
    loadConfig = new JButton("Open");
    loadConfig.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {

        // open in current directory:
        JFileChooser fc = new JFileChooser(new File(System.getProperty("user.dir")));

        // show the filechooser
        int result = fc.showOpenDialog(loadConfig);  // put parent component

        // get filename
        if(result == JFileChooser.APPROVE_OPTION) {
          File aFile = fc.getSelectedFile();

          // check to see if file exists:
          if (aFile.exists()) {
            readConfig(aFile.getPath());
          } else {
            JOptionPane.showMessageDialog(
              loadConfig,
              "File \""+aFile.getName()+"\" doesn't exist.",
              "File not found",
              JOptionPane.ERROR_MESSAGE
            );
          }
        }
      }
    });

    saveConfig = new JButton("Save");
    saveConfig.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {

        // open in current directory:
        JFileChooser fc = new JFileChooser(new File(System.getProperty("user.dir")));

        // show the filechooser
        int result = fc.showSaveDialog(saveConfig);  // put parent component

        // get filename
        if(result != JFileChooser.APPROVE_OPTION) return;

        File aFile = fc.getSelectedFile();

        // check to see if file exists:
        if (aFile.exists()) {
          result = JOptionPane.showConfirmDialog(
               saveConfig,
               "Are you sure you want to overwrite "+aFile.getName()+"?",
               "File exists",JOptionPane.YES_NO_OPTION);
        } else {
          result = JOptionPane.YES_OPTION;
        }

        if(result == JOptionPane.YES_OPTION) {
          writeConfig(aFile.getPath());
        }
      }
    });

    vLabel = new JLabel(" ");
    LLabel = new JLabel(" ");

    dvLabel = new JLabel(" Impulsion ");
    dvField = new JTextField(String.format("%f",dv));
    dvField.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Double dv_new = readDouble(dvField.getText(),"l'impulsion",0.0,10.0,dvField);
        if (dv_new == null) {
          historyField.setText(String.format("%f",dv));
        } else {
          dv = dv_new;
        }
      }
    });

    historyLabel = new JLabel(" Historique: ");
    historyField = new JTextField(String.format("%d",graphArea.npast));
    historyField.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Integer npast = readInt(historyField.getText(),"l'historique",1,9999,historyField);
        if (npast == null) {
          historyField.setText(String.format("%d",graphArea.npast));
        } else {
          graphArea.npast = npast;
        }
      }
    });

    colorButton = new JButton();
    colorButton.setBackground(graphArea.pColors[ip]);
    colorButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Color color = JColorChooser.showDialog(colorButton, "Choisissez une couleur",
                      graphArea.pColors[ip]);
        graphArea.pColors[ip] = color;
        colorButton.setBackground(color);
      }
    });
    pSpinnerModel = new SpinnerNumberModel(ip+1,1,np,1);
    pSpinner = new JSpinner(pSpinnerModel);
    pSpinner.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        ip = pSpinnerModel.getNumber().intValue()-1;
        refreshPlabels();
        colorButton.setBackground(graphArea.pColors[ip]);
      }
    });
  }

  /////////////////////////////////////////////////////////////////////////////
  // Refreshes the velocity and angular momentum labels for the current particle
  /////////////////////////////////////////////////////////////////////////////
  private void refreshPlabels() {
    ip = pSpinnerModel.getNumber().intValue()-1;
    vLabel.setText(String.format("  v = %.3g ",v[ip]));
    LLabel.setText(String.format("  L = %.3g ",L[ip]));
  }

  /////////////////////////////////////////////////////////////////////////////
  // This makes the particles orbit
  /////////////////////////////////////////////////////////////////////////////
  public void lanceOrbite() {
    int i, n = xx.length; 
    while(true){
      if (running) {
        // Euler method:
        //n = xx.length;
        //for(i=0; i<n; i++) Euler(xx[i]);

        // RK4 method:
        n = xx.length;
        for(i=0; i<n; i++) RK4(xx[i]);

        double[] xpos = new double[n];
        double[] ypos = new double[n];
        for(i=0; i<n; i++) {
          xpos[i] = xx[i][0];
          ypos[i] = xx[i][1];
        }
        find_vL();
        refreshPlabels();
        graphArea.setPositions(xpos,ypos);
      }
      graphArea.repaint();
      try {
        TimeUnit.MILLISECONDS.sleep(10L);
      } catch(InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  // gravitational acceleration function
  // xx[0] = x, xx[1] = y, xx[2] = vx, xx[3] = vy
  /////////////////////////////////////////////////////////////////////////////
  private double[] F(double[] xx) {
      double r = Math.sqrt((xx[0]-x0)*(xx[0]-x0) + (xx[1]-y0)*(xx[1]-y0));
      double[] result = new double[4];
      result[0] = xx[2];
      result[1] = xx[3];
      result[2] = (x0-xx[0])*G*M/(r*r*r);
      result[3] = (y0-xx[1])*G*M/(r*r*r);
      return result;
  }

  /////////////////////////////////////////////////////////////////////////////
  // This applies a fourth order Runge-Kutta method
  /////////////////////////////////////////////////////////////////////////////
  private void RK4(double[] xx) {
    int n = xx.length;
    double[] k1 = new double[n];
    double[] k2 = new double[n];
    double[] k3 = new double[n];
    double[] k4 = new double[n];
    k1 = F(xx);
    k2 = F(combine(1.0,xx,dt/2.0,k1));
    k3 = F(combine(1.0,xx,dt/2.0,k2));
    k4 = F(combine(1.0,xx,dt,k2));
    for(int i=0; i<n; i++) xx[i] += dt*(k1[i]+2.0*k2[i]+2.0*k3[i]+k4[i])/6.0;
  }

  /////////////////////////////////////////////////////////////////////////////
  // This applies an Euler integration method
  /////////////////////////////////////////////////////////////////////////////
  private void Euler(double[] xx) {
    int n = xx.length;
    double[] k = F(xx);
    for(int i=0; i<n; i++) xx[i] += dt*k[i];
  }

  /////////////////////////////////////////////////////////////////////////////
  // This calculates a linear combination of two arrays
  /////////////////////////////////////////////////////////////////////////////
  private double[] combine(double a1, double[] v1,double a2, double[] v2) {
    int n = v1.length;
    double[] result = new double[n];
    for(int i=0; i<n; i++) result[i] = a1*v1[i]+a2*v2[i];
    return result;
  }

  /////////////////////////////////////////////////////////////////////////////
  // This calculates the velocity and angular momentum for the particles.
  /////////////////////////////////////////////////////////////////////////////
  private void find_vL(){
    int n = xx.length, i;
    double vx, vy;
    for(i=0; i<n; i++) {
      vx = xx[i][2]; 
      vy = xx[i][3]; 
      v[i] = Math.sqrt(vx*vx+vy*vy);
      L[i] = vy*(xx[i][0]-x0) - vx*(xx[i][1]-y0);
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  // This checks a string entry to see if it's an integer.  If not, it produces
  // an error message and returns null.  It also checks to see if the integer
  // is between lowerBound and upperBound.  If not, it produces a different
  // error message and returns null.
  /////////////////////////////////////////////////////////////////////////////
  public static Integer readInt(String str, String ParamName, int lowerBound,
                            int upperBound, JComponent c) {
    Integer result;
    try {
      result = Integer.parseInt(str);
    } catch(Exception e) {
      JOptionPane.showMessageDialog(
        c, // parent component
        "Veillez entrez une valeure entière pour "+ParamName,
        "Erreur",
        JOptionPane.ERROR_MESSAGE
      );
      return null;
    }
    if ((result > upperBound)||(result < lowerBound)) {
      JOptionPane.showMessageDialog(
        c, // parent component
        ParamName.substring(0,1).toUpperCase()+ParamName.substring(1)+
        " doit être comprise entre "+((Integer)lowerBound).toString()+
        " et "+((Integer)upperBound).toString(),
        "Erreur",
        JOptionPane.ERROR_MESSAGE
      );
      return null;
    }
    return result;
  }

  /////////////////////////////////////////////////////////////////////////////
  // This checks a string entry to see if it's a number.  If not, it produces
  // an error message and returns null.  It also checks to see if the number is
  // between lowerBound and upperBound.  If not, it produces a different error
  // message and returns null.
  /////////////////////////////////////////////////////////////////////////////
  public static Double readDouble(String str, String ParamName, double lowerBound,
                            double upperBound, JComponent c) {
    Double result;
    try {
      result = Double.parseDouble(str);
    } catch(Exception e) {
      JOptionPane.showMessageDialog(
        c, // parent component
        "Veillez entrez un nombre pour "+ParamName,
        "Erreur",
        JOptionPane.ERROR_MESSAGE
      );
      return null;
    }

    if (result.isNaN()) {
      JOptionPane.showMessageDialog(
        c, // parent component
        "Veillez entrez un nombre pour "+ParamName,
        "Erreur",
        JOptionPane.ERROR_MESSAGE
      );
      return null;
    }

    if ((result > upperBound)||(result < lowerBound)) {
      JOptionPane.showMessageDialog(
        c, // parent component
        ParamName.substring(0,1).toUpperCase()+ParamName.substring(1)+
        " doit être comprise entre "+((Double)lowerBound).toString()+
        " et "+((Double)upperBound).toString(),
        "Erreur",
        JOptionPane.ERROR_MESSAGE
      );
      return null;
    }
    return result;
  }

  /////////////////////////////////////////////////////////////////////////////
  // Private class which describes the mouse listener/mouse motion listener
  // that goes with graphArea.
  /////////////////////////////////////////////////////////////////////////////
  private class PlotMouseListener implements MouseListener, MouseMotionListener, MouseWheelListener {

    // This keeps track of whether the mouse is
    // being dragged (and has stayed within
    // graphArea):
    private boolean drag = false;

    // This keeps track of whether the plot
    // is being shifted (and has stayed within
    // graphArea):
    private boolean shift = false;

    /** Zoom factor */
    private double zoomFactor = 1.1;

    // (x0,y0) = start of drag (in window coordinates)
    // (x1,y1) = end of drag (in window coordinates)
    private int x0, y0, x1, y1;

    // (xx0,yy0) = initial position when shifting (in real coordinates)
    private double xx0, yy0;

    /** A local copy of the orbit plot area to which the mouse wheel listener is added */
    private Plot graphArea;

    /** A constructor */
    PlotMouseListener(Plot graphArea) {
      this.graphArea = graphArea;
    }

    /** Unused method which still needs to be
    *   implemented. */
    public void mouseClicked(MouseEvent e) {
    }

    /** Unused method which still needs to be
    *   implemented. */
    public void mouseEntered(MouseEvent e) {
    }

    /** Unused method which still needs to be
    *   implemented. */
    public void mouseMoved(MouseEvent e) {
    }

    /** Method which erases the previous rectangle
    *   and draws a new one when the mouse is being
    *   dragged across the plot. */
    public void mouseDragged(MouseEvent e) {
      if (drag) {
        // this draws a rectangle:
        x1 = e.getX();
        y1 = e.getY();
        graphArea.setRect(x0,y0,x1,y1);
      } else if (shift) {
        x1 = e.getX();
        y1 = e.getY();
        graphArea.shift(xx0,yy0,x1,y1);
      }
    }

    /** Method which erases the last rectangle
    *   when the mouse leaves the plot area,
    *   and cancels the zoom. */
    public void mouseExited(MouseEvent e) {
      graphArea.removeRect();
      drag = false;
      shift = false;
    }

    /** Method which keeps track of the point
    *   from where the mouse is being dragged */
    public void mousePressed(MouseEvent e) {
      if (e.getButton() == MouseEvent.BUTTON1) {
        drag = true;
        x0 = e.getX();
        y0 = e.getY();
      } else if (e.getButton() == MouseEvent.BUTTON2) {
        shift = true;
        xx0 = graphArea.xValue(e.getX());
        yy0 = graphArea.yValue(e.getY());
      }
    }

    /** Method which performs the zoom-in once
    *   the mouse button has been released. */
    public void mouseReleased(MouseEvent e) {
      if (e.getButton() == MouseEvent.BUTTON1) {

        // To avoid inverting the range, we put the
        // appropriate values in the following
        // variables and use them instead:
        int xleft, xright, ytop, ybottom;

        if (drag) {
          x1 = e.getX();
          y1 = e.getY();
          if (x0==x1) return;
          if (y0==y1) return;
          xleft=x0<x1?x0:x1;
          xright=x0<x1?x1:x0;
          ytop=y0<y1?y0:y1;
          ybottom=y0<y1?y1:y0;
          graphArea.zoom(xleft,xright,ybottom,ytop);
        }
        drag = false;
        graphArea.removeRect();
      } else if (e.getButton() == MouseEvent.BUTTON2) {
        shift = false;
      }
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
      String message;
      int notches = e.getWheelRotation();
      double x = graphArea.xValue(e.getX());
      double y = graphArea.yValue(e.getY());
      double factor;
      // zoom in if notches < 0
      // zoom out if notches < 0
      factor = Math.pow(zoomFactor,notches);
      graphArea.xminTarget = x+factor*(graphArea.xmin-x);
      graphArea.xmaxTarget = x+factor*(graphArea.xmax-x);
      graphArea.yminTarget = y+factor*(graphArea.ymin-y);
      graphArea.ymaxTarget = y+factor*(graphArea.ymax-y);
    }
  }

  /////////////////////////////////////////////////////////
  // This remove comments (i.e. anything following a "#")
  // from a string and is used by the loadRotaTarget method:
  /////////////////////////////////////////////////////////
  public static String trim(String str) {
    if (str == null) return null;
    int len = str.length();
    int i = 0;
    while ((i < len)&&(str.charAt(i) != '#')) i++;
    return str.substring(0,i);
  }

  /////////////////////////////////////////////////////////////////////////////
  /* This parses a double after having replaced "d" and "D" to "e". */
  /////////////////////////////////////////////////////////////////////////////
  public static double parseDouble(String s) {
    try {
      return Double.parseDouble(s.replace('d','E').replace('D','E').replace(',','.'));
    } catch(Exception e) {
      return Double.NaN;
    }
  }
}
