/*--------------------------------------------------------------------------
*    This file is part of InversionKit.
*
*    InversionKit is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    InversionKit is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with InversionKit.  If not, see <http://www.gnu.org/licenses/>.
*
****************************************************************************
*
*    Copyright (c) Daniel Reese, Sergei Zharkov, 2008, 2009, 2011, 2014, 2016
*
****************************************************************************/
package graph;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.font.LineMetrics;
import java.awt.font.FontRenderContext;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import java.util.Vector;

/****************************************************************************
* The purpose of this class is to draw plots in Java.
****************************************************************************/

public class Plot extends JComponent
{
  // the size of the planet:
  public double planetSize = 0.1;

  // number of projectiles
  private int np = 0;
  public Color[] pColors;

  // past positions:
  public int npast = 1;
  private Vector<double[]> xpos = new Vector<double[]>();
  private Vector<double[]> ypos = new Vector<double[]>();

  // The x and y bounds:
  public double xminTarget=-1.0, xmaxTarget=1.0, yminTarget=-1.0, ymaxTarget=1.0;
  //private double xmin=-1.0, xmax=1.0, ymin=-1.0, ymax=1.0;
  public double xmin, xmax, ymin, ymax;

  // These keep track of the size of the plot area:
  private float hspan, vspan;

  // These are the coordinates of the four corners
  // of the plot area.  They are calculated using
  // the margins given above and the bounds of the
  // component.  By declaring xleft as a method, it
  // automatically adjusts to any changes in the left
  // margin.
  private float xleft()  {return 0.0F;}
  private float xright() {return hspan;}
  private float yup()    {return 0.0F;}
  private float ydown()  {return vspan;}

  // boolean indicating whether to draw a rectangle or not
  private boolean drawRect = false;
  // coordinates of the rectangle
  private int xr0, xr1, yr0, yr1;

  /////////////////////////////////////////////////////////////////////////////
  // Various line patterns:
  // The conventions used here are similar to IDL.
  /** No line style. This is useful for plotting only symbols. */
  public static final int noLine = -1;
  /** Solid line style. This is the default value. */
  public static final int solid = 0;      // the default
  /** Dotted line style. */
  public static final int dotted = 1;
  /** Dashed line style. */
  public static final int dashed = 2;
  /** Dot-dashed line style. */
  public static final int dotDash = 3;
  /** Dot-dot-dashed line style. */
  public static final int dotDotDash = 4;
  /** Line style with long dashes. */
  public static final int longDash = 5;
  
  private static final Stroke[] linestyle = new Stroke[]
    {
      // Stroke pattern corresponding to a solid line
      new BasicStroke(2F),
      // Stroke pattern corresponding to a dotted line 
      new BasicStroke(2F,                  // Width
          BasicStroke.CAP_BUTT,            // End cap
          BasicStroke.JOIN_MITER,          // Join style
          3.0F,                            // Miter limit
          new float[] {2F,2F},             // Dash pattern
          0.0F),                           // Dash phase
      // Stroke pattern corresponding to a dashed line 
      new BasicStroke(2F,                  // Width
          BasicStroke.CAP_BUTT,            // End cap
          BasicStroke.JOIN_MITER,          // Join style
          3.0F,                            // Miter limit
          new float[] {6F,6F},             // Dash pattern
          0.0F),                           // Dash phase
      // Stroke pattern corresponding to a dot-dash line
      new BasicStroke(2F,                  // Width
          BasicStroke.CAP_BUTT,            // End cap
          BasicStroke.JOIN_MITER,          // Join style
          3.0F,                            // Miter limit
          new float[] {6F,6F,2F,6F},       // Dash pattern
          0.0F),                           // Dash phase
      // Stroke pattern corresponding to a dot-dot-dash line
      new BasicStroke(2F,                  // Width
          BasicStroke.CAP_BUTT,            // End cap
          BasicStroke.JOIN_MITER,          // Join style
          3.0F,                            // Miter limit
          new float[] {6F,6F,2F,2F,2F,6F}, // Dash pattern
          0.0F),                           // Dash phase
      // Stroke pattern corresponding to a line with long dashes
      new BasicStroke(2F,                  // Width
          BasicStroke.CAP_BUTT,            // End cap
          BasicStroke.JOIN_MITER,          // Join style
          3.0F,                            // Miter limit
          new float[] {12F,12F},           // Dash pattern
          0.0F)                            // Dash phase
    };

  /////////////////////////////////////////////////////////////////////////////
  // Various symbols:
  // The conventions used here are similar to IDL.
  /** No symbols. This is useful for plotting only with a linestyle.
      This is the default value. */
  /** No symbols. */
  public static final int noSymbol = 0;      // the default
  /** Plus symbols. */
  public static final int plus = 1;
  /** Asterisks. */
  public static final int asterisk = 2;
  /** Single dots. */
  public static final int singleDot = 3;
  /** Diamonds. */
  public static final int diamond = 4;
  /** Triangles. */
  public static final int triangle = 5;
  /** Squares. */
  public static final int square = 6;
  /** Time symbols. */
  public static final int times = 7;
  
  // This part codes the different segments as a series
  // of line segments (x0,y0,x1,y1,x2,y2 ...) where:
  // (x0,y0)-(x1,y1) = first segment
  // (x2,y2)-(x3,y3) = second segment
  // ...
  // Note the y axis is inverted (this affects the triangle).
  private static final float[][] psym = new float[][]
  {
    null,
    new float[] {0F,5F,0F,-5F,-5F,0F,5F,0F},
    new float[] {0F,5F,0F,-5F,-5F,0F,5F,0F,-5F,-5F,5F,5F,-5F,5F,5F,-5F},
    new float[] {-1F,1F,1F,1F,1F,1F,1F,-1F,1F,-1F,-1F,-1F,-1F,-1F,-1F,1F},
    new float[] {-5F,0F,0F,5F,0F,5F,5F,0F,5F,0F,0F,-5F,0F,-5F,-5F,0F},
    new float[] {0F,-6F,5.2F,3F,5.2F,3F,-5.2F,3F,-5.2F,3F,0F,-6F},    
    new float[] {-5F,5F,5F,5F,5F,5F,5F,-5F,5F,-5F,-5F,-5F,-5F,-5F,-5F,5F},
    new float[] {-5F,-5F,5F,5F,-5F,5F,5F,-5F}
  };

  // The stroke pattern used for creating the symbols:
  private static final Stroke psymStroke = new BasicStroke(1F);

  /////////////////////////////////////////////////////////////////////////////
  /**
   * A constructor.
   *
   * @param  planetSize    size of the planet
   * @param  nprojectiles  number of projectiles gravitating around the planet
   */
  /////////////////////////////////////////////////////////////////////////////
  public Plot(double planetSize, Color[] pColors) {
    this.planetSize = planetSize;
    this.pColors = pColors;
    this.np = pColors.length;
  }

  /////////////////////////////////////////////////////////////////////////////
  /* replace planet size, colors and number of particles */
  /////////////////////////////////////////////////////////////////////////////
  public void replace(double planetSize, Color[] pColors) {
    this.planetSize = planetSize;
    this.pColors = pColors;
    this.np = pColors.length;
  }

  /////////////////////////////////////////////////////////////////////////////
  /* Set position of a projectile */
  /////////////////////////////////////////////////////////////////////////////
  public void setPositions(double[] x, double[] y) {
    // add positions:
    xpos.add(x);
    ypos.add(y);

    // remove past positions if need be:
    while (xpos.size() > npast) {
      xpos.removeElementAt(0);
      ypos.removeElementAt(0);
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  /** This allows the user to set xmin.  If Xlog = true, then setting a
   *  negative or zero value will set xmin to the default value 1.0.
   *
   *  @param xmin new value for xmin. */
  /////////////////////////////////////////////////////////////////////////////
  public void setXmin(double xmin) { this.xmin  = xmin; }

  /////////////////////////////////////////////////////////////////////////////
  /** This allows the user to set xmax.  If Xlog = true, then setting a
   *  negative or zero value will set xmax to the default value 1.0.
   *
   *  @param xmax new value for xmax. */
  /////////////////////////////////////////////////////////////////////////////
  public void setXmax(double xmax) { this.xmax  = xmax; }

  /////////////////////////////////////////////////////////////////////////////
  /** This allows the user to set ymin.  If Ylog = true, then setting a
   *  negative or zero value will set ymin to the default value 1.0.
   *
   *  @param ymin new value for ymin. */
  /////////////////////////////////////////////////////////////////////////////
  public void setYmin(double ymin) { this.ymin  = ymin; }

  /////////////////////////////////////////////////////////////////////////////
  /** This allows the user to set ymax.  If Ylog = true, then setting a
   *  negative or zero value will set ymax to the default value 1.0.
   *
   *  @param ymax new value for ymax. */
  /////////////////////////////////////////////////////////////////////////////
  public void setYmax(double ymax) { this.ymax  = ymax; }

  /////////////////////////////////////////////////////////////////////////////
  /** This allows the user to obtain xmin.
   *
   *  @return the value of xmin. */
  /////////////////////////////////////////////////////////////////////////////
  public double getXmin() {return xmin;}

  /////////////////////////////////////////////////////////////////////////////
  /** This allows the user to obtain xmax.
   *
   *  @return the value of xmax. */
  /////////////////////////////////////////////////////////////////////////////
  public double getXmax() {return xmax;}

  /////////////////////////////////////////////////////////////////////////////
  /** This allows the user to obtain ymin
   *
   *  @return the value of ymin. */
  /////////////////////////////////////////////////////////////////////////////
  public double getYmin() {return ymin;}

  /////////////////////////////////////////////////////////////////////////////
  /** This allows the user to obtain ymax
   *
   *  @return the value of ymax. */
  /////////////////////////////////////////////////////////////////////////////
  public double getYmax() {return ymax;}


  /////////////////////////////////////////////////////////////////////////////
  /** This draws a rectangle which delimits the zoom window.
   *  @param x0 x coordinate of one corner of the zoom window
   *  @param y0 corresponding y coordinate (x0, y0)
   *  @param x1 x coordinate of opposite corner
   *  @param y1 corresponding y coordinate (x1, y1) */
  /////////////////////////////////////////////////////////////////////////////
  public void setRect(int x0, int y0, int x1, int y1) {
    drawRect = true;
    xr0 = x0;
    xr1 = x1;
    yr0 = y0;
    yr1 = y1;
  }

  /////////////////////////////////////////////////////////////////////////////
  /** This removes the rectangle from the plot */
  /////////////////////////////////////////////////////////////////////////////
  public void removeRect() { drawRect = false; }

  /////////////////////////////////////////////////////////////////////////////
  /** This zooms to the specified coordinates.  IMPORTANT: these coordinates
  *   are given in terms of the graphics window rather than in terms of
  *   the coordinate system associated with the data sets.
  *
  *   @param xleft   coordinate of left bound of the zoom window
  *   @param xright  coordinate of right bound of the zoom window
  *   @param ybottom coordinate of lower bound of the zoom window
  *   @param ytop    coordinate of upper bound of the zoom window */
  /////////////////////////////////////////////////////////////////////////////
  public void zoom(int xleft, int xright, int ybottom, int ytop) {
    double xxmin, xxmax, yymin, yymax;

    // Important: xValue and yValue depend on xmin, xmax, ymin and ymax.
    // Therefore, these should be modified only after calling xValue
    // and yValue.

    xxmin = xValue(xleft);
    xxmax = xValue(xright);
    yymin = yValue(ybottom);
    yymax = yValue(ytop);
    
    xminTarget = xxmin;
    xmaxTarget = xxmax;
    yminTarget = yymin;
    ymaxTarget = yymax;
  }

  /////////////////////////////////////////////////////////////////////////////
  /** This shifts to the specified coordinates.  IMPORTANT: these coordinates
  *   are given in terms of the graphics window rather than in terms of
  *   the coordinate system associated with the data sets.
  *
  *   @param xx0  initial real x coordinate from which window is shifting
  *   @param yy0  initial real y coordinate from which window is shifting
  *   @param x1   current x coordinate to which window is shifting
  *   @param y1   current y coordinate to which window is shifting */
  /////////////////////////////////////////////////////////////////////////////
  public void shift(double xx0, double yy0, int x1, int y1) {
    double DeltaX, DeltaY;

    DeltaX = xmax - xmin;
    xminTarget = xx0 - DeltaX*(double)(x1-xleft())/(double)(xright()-xleft());
    xmaxTarget = xminTarget + DeltaX;

    DeltaY = ymax - ymin;
    yminTarget = yy0 - DeltaY*(double)(y1-ydown())/(double)(yup()-ydown());
    ymaxTarget = yminTarget + DeltaY;
  }

  /////////////////////////////////////////////////////////////////////////////
  /** This creates the plot.
   *
   *  @param g graphics frame in which to create the plot */
  /////////////////////////////////////////////////////////////////////////////
  public void paint(Graphics g) {
    Graphics2D g2D = (Graphics2D)g;
    Rectangle r = this.getBounds();

    hspan = (float) r.getWidth();
    vspan = (float) r.getHeight();
    double dx = xmaxTarget-xminTarget;
    double dy = ymaxTarget-yminTarget;
    float ratio1 = (float)(dy/dx);
    float ratio2 = vspan/hspan;
    if (ratio1 < ratio2) {
      // expand dy
      ymin = yminTarget - dx*(ratio2-ratio1)/2.0;
      ymax = ymaxTarget + dx*(ratio2-ratio1)/2.0;
      xmin = xminTarget;
      xmax = xmaxTarget;
    } else {
      // expand dx
      xmin = xminTarget - dy*(1.0/ratio2-1.0/ratio1)/2.0;
      xmax = xmaxTarget + dy*(1.0/ratio2-1.0/ratio1)/2.0;
      ymin = yminTarget;
      ymax = ymaxTarget;
    }
    
    // I think that r.x, r.y are the offset from the
    // complete frame, and should therefore be removed:
    r.x = 0;
    r.y = 0;
    g2D.setColor(Color.black);
    g2D.fill(r);
    
    // Make a nice frame around the plot:
    r.height--; // this is to see the lower part of the frame
    r.width--;  // this is to see the right part of the frame
    g2D.setColor(Color.white);
    g2D.draw(r);

    int i, j, k;
    double theta;
    GeneralPath f = new GeneralPath();
    f.moveTo(xCoor(planetSize),yCoor(0.0));
    for(i=1; i<=40; i++) {
      theta = 3.141592653589793*(double)(i)/20.0;
      f.lineTo(xCoor(planetSize*Math.cos(theta)),yCoor(planetSize*Math.sin(theta)));
    }
    g2D.setStroke(linestyle[dotted]);
    g2D.draw(f);

    float xx, yy;
    for(k = 0; k<np; k++) {
      g2D.setColor(pColors[k]);
      GeneralPath f2 = new GeneralPath();
      for(i = 0; i<xpos.size(); i++) {
        xx = xCoor(xpos.get(i)[k]);
        yy = yCoor(ypos.get(i)[k]);
        for (j = 0; j < psym[singleDot].length; j+=4) {
          f2.moveTo(xx+psym[singleDot][j],yy+psym[singleDot][j+1]);
          f2.lineTo(xx+psym[singleDot][j+2],yy+psym[singleDot][j+3]);
        }
      }
      g2D.setStroke(Plot.psymStroke);
      g2D.draw(f2);
    }

    if (drawRect) {
      g2D.setColor(Color.white);
      GeneralPath f3 = new GeneralPath();
      f3.moveTo(xr0,yr0);
      f3.lineTo(xr0,yr1);
      f3.lineTo(xr1,yr1);
      f3.lineTo(xr1,yr0);
      f3.lineTo(xr0,yr0);

      //g2D.setXORMode(Color.white);
      g2D.setStroke(new BasicStroke(1F));
      g2D.draw(f3);
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  // This function returns the squared norm of vector (xx,yy).
  /////////////////////////////////////////////////////////////////////////////
  // @param  xx       x coordinate of vector
  // @param  yy       y coordinate of vector
  // @return squared norm of vector
  /////////////////////////////////////////////////////////////////////////////
  private static float norm2(float xx, float yy) {
    return xx*xx+yy*yy;
  }

  /////////////////////////////////////////////////////////////////////////////
  // This converts x values to x coordinates:
  //
  // @param  xvalue x value in the same units as the data sets
  // @return x value in terms of the graphics area
  /////////////////////////////////////////////////////////////////////////////
  public float xCoor(double xvalue) {
    return (float)(xleft() + (xright()-xleft())*(xvalue-xmin)/(xmax-xmin));
  }

  /////////////////////////////////////////////////////////////////////////////
  // This converts y values to y coordinates:
  //
  // @param  yvalue y value in the same units as the data sets
  // @return y value in terms of the graphics area
  /////////////////////////////////////////////////////////////////////////////
  public float yCoor(double yvalue) {
    return (float)(ydown() + (yup()-ydown())*(yvalue-ymin)/(ymax-ymin));
  }

  /////////////////////////////////////////////////////////////////////////////
  // This converts x coordinates to x values:
  //
  // @param  xcoor x value in terms of the graphics area
  // @return x value in the same units as the data sets
  /////////////////////////////////////////////////////////////////////////////
  public double xValue(int xcoor) {
    double lambda = ((double)xcoor-(double)xleft())/(double)(xright()-xleft());
    return (1.0-lambda)*xmin+lambda*xmax;
  }

  /////////////////////////////////////////////////////////////////////////////
  // This converts y coordinates to y values:
  //
  // @param  ycoor y value in terms of the graphics area
  // @return y value in the same units as the data sets
  /////////////////////////////////////////////////////////////////////////////
  public double yValue(int ycoor) {
    double lambda = ((double)ycoor-(double)ydown())/(double)(yup()-ydown());
    return (1.0-lambda)*ymin+lambda*ymax;
  }

}
