package marytts.util.math;

import Jama.Matrix;
import Jama.EigenvalueDecomposition;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Vector;

import marytts.tools.voiceimport.DurationSoPTrainer;
import marytts.util.math.MathUtils;

/***
 * Multiple linear regresion
 * For the case of k independent variables 
 *   x_1, x_2, ... x_k
 * the sample regression equation is given by
 *   y_i = b_0 + b_1*x_1i + b_2*x_2i + ... + b_k*x_ki + e_i
 *     y = X*b
 *   
 * Ref: Walpole, Myers, Myers and Ye, "Probability and statistics for engineers and scientists", 
 *      Prentice Hall, chapter 12, pg. 400, 2002. 
 *   
 * @author marcela 
 *  */
public class Regression {

  
  private double[] coeffs;          // keep coefficient in a Matrix
  private double[] residuals;       // duration(i) - predicted_duration(i)
  private double[] predictedValues; // predicted_duration(i) --> y(i)
  private double correlation;       // correlation between predicted_duration and duration
  private Matrix X;
  private Matrix y;
  private Matrix b;
  
  public Regression(){
    predictedValues = null;
    X = null;
    y = null;
    b = null;
  }
 
  
  /***
   * 
   * @param data         dependent and independent variables
   *                     data={{y1, x11, x12, ... x1k},
   *                           {y2, x21, x22, ... x2k},
   *                           ...
   *                           {yn, xn1, xn2, ... xnk}}
   * @param dependentVar number of the column that will be used as dependent variable --> vector y
   *                     by default the first column is y
   * @param rows number of rows 
   * @param cols number of cols including the dependent variable  
   * @return coefficients or null if problems found
   */
  public double[] multipleLinearRegression(double[] data, int rows, int cols, boolean intercepTerm){   
    if (data == null) throw new NullPointerException("Null data");
    if (rows < 0 || cols < 0) throw new IllegalArgumentException("Number of rows and cols must be greater than 0");

    Matrix dataX; 
    if(intercepTerm){ // first column of X is filled with 1s if b_0 != 0
      dataX = new Matrix(rows,cols);
      coeffs = new double[cols];
    }
    else{
      dataX = new Matrix(rows,cols-1);
      coeffs = new double[cols-1];
    }     
    double[] datay = new double[rows];
    
    // Fill the data in the matrix X (independent variables) and vector y (dependet variable)
    int n = 0;  // number of data points
    for (int i=0; i<rows; i++) {
      if(intercepTerm) {
        dataX.set(i, 0, 1.0);
        datay[i] = data[n++];  // first column is the dependent variable
        for (int j=1; j< cols; j++)
          dataX.set(i, j, data[n++]);
      } else { // No intercepTerm so no need to fill the first column with 1s
        datay[i] = data[n++]; // first column is the dependent variable
        for (int j=0; j< cols-1; j++)
          dataX.set(i, j, data[n++]);
      }      
    }
    multipleLinearRegression(datay, dataX);
    return coeffs;    
   }
  
  
  public double[] multipleLinearRegression(double[] datay, double[][] datax, boolean intercepTerm){
    if (datay == null || datax==null) throw new NullPointerException("Null data");

    int rows = datay.length;
    int cols = datax[0].length;
    Matrix dataX; 
    if(intercepTerm){ // first column of X is filled with 1s if b_0 != 0
      dataX = new Matrix(rows,cols+1);
      coeffs = new double[cols+1];
    }
    else{
      dataX = new Matrix(datax);
      coeffs = new double[cols];
    }
     
    // If intercept, we need to add a ones column to dataX
    if(intercepTerm) {
      for (int i=0; i<rows; i++) {
        dataX.set(i, 0, 1.0);
        for (int j=1; j< cols+1; j++)
          dataX.set(i, j, datax[i][j-1]);
      }      
    }    
    multipleLinearRegression(datay, dataX);    
    return coeffs;      
  }


  public double[] multipleLinearRegression(Vector<Double> vectory, Vector<Double> vectorx, int rows, int cols, boolean intercepTerm){
    if (vectory == null || vectorx==null) throw new NullPointerException("Null data");

    Matrix dataX; 
    if(intercepTerm){ // first column of X is filled with 1s if b_0 != 0
      dataX = new Matrix(rows,cols+1);
      coeffs = new double[cols+1];
    }
    else{
      dataX = new Matrix(rows,cols);
      coeffs = new double[cols];
    }     
    double[] datay = new double[rows];
    
    // Fill the data in the matrix X (independent variables) and vector y (dependet variable)
    int n = 0;  // number of data points
    for (int i=0; i<rows; i++) {
      if(intercepTerm) {
        datay[i] = vectory.elementAt(i);  // first column is the dependent variable        
        dataX.set(i, 0, 1.0);
        for (int j=1; j< cols+1; j++) {
          dataX.set(i, j, vectorx.elementAt(n++));
        } 
      } else { // No intercepTerm so no need to fill the first column with 1s
        datay[i] = vectory.elementAt(i); // first column is the dependent variable
        for (int j=0; j< cols; j++) {
          dataX.set(i, j, vectorx.elementAt(n++));
        }         
      }      
    }
    multipleLinearRegression(datay, dataX);    
    return coeffs;      
  }
  
  
  
  /***
   * 
   * @param data Vector contains dependent variable first and then independent variables
   * @param rows 
   * @param cols
   * @param intercepTerm
   * @return
   */
  public double[] multipleLinearRegression(Vector<Double> data, int rows, int cols, boolean intercepTerm){
    
    if (data == null) throw new NullPointerException("Null data");
    if (rows < 0 || cols < 0) throw new IllegalArgumentException("Number of rows and cols must be greater than 0");

    Matrix dataX; 
    if(intercepTerm){ // first column of X is filled with 1s if b_0 != 0
      dataX = new Matrix(rows,cols);
      coeffs = new double[cols];
    }
    else{
      dataX = new Matrix(rows,cols-1);
      coeffs = new double[cols-1];
    }
     
    double[] datay = new double[rows];
    
    // Fill the data in the matrix X (independent variables) and vector y (dependet variable)
    int n = 0;  // number of data points
    for (int i=0; i<rows; i++) {
      if(intercepTerm) {
        dataX.set(i, 0, 1.0);
        datay[i] = data.elementAt(n++);  // first column is the dependent variable
        for (int j=1; j< cols; j++)
          dataX.set(i, j, data.elementAt(n++));
      } else { // No intercepTerm so no need to fill the first column with 1s
        datay[i] = data.elementAt(n++); // first column is the dependent variable
        for (int j=0; j< cols-1; j++)
          dataX.set(i, j, data.elementAt(n++));
      }      
    }   
    multipleLinearRegression(datay, dataX);
    return coeffs;  
   }  

  /***
   *  Least-square solution y = X * b where:
   *  y_i = b_0 + b_1*x_1i + b_2*x_2i + ... + b_k*x_ki  including intercep term
   *  y_i =       b_1*x_1i + b_2*x_2i + ... + b_k*x_ki  without intercep term
   * 
   * @param datay
   * @param dataX 
   */
  public void multipleLinearRegression(double[] datay, Matrix dataX){

    System.out.println("X=");
    dataX.print(dataX.getRowDimension(), 3);
    try {
      X = dataX;
      y = new Matrix(datay,datay.length);
      b = X.solve(y);
      coeffs = new double[b.getRowDimension()];
      for (int j=0; j<b.getRowDimension(); j++) {
          coeffs[j] = b.get(j, 0);
          System.out.println("coeff[" + j + "]=" + coeffs[j]);
      } 
      
    } catch (RuntimeException re) {
        throw new Error("Error solving Least-square solution: y = X * b");
    }  
  }
  
  public double[] getResiduals(){
    // Residuals
    if( X != null && y != null){
      Matrix r = X.times(b).minus(y);
      residuals = r.getColumnPackedCopy();
      return residuals;
    } else {
       System.out.println("No values set for matrix X and y"); 
       return null;
    }  
  }
  
  public double[] getPredictedValues(){
    // Residuals
    if( X != null && y != null && b != null){
      Matrix p = X.times(b);
      predictedValues = p.getColumnPackedCopy();
      return predictedValues;
    } else {
      System.out.println("No values set for matrix X and y"); 
      return null;      
    }
  }

  /***
   * Correlation between original values and predicted ones.
   * @return
   */
  public double getCorrelation(){
    double r;
    double oriValues[];
    if( X != null && y != null && b != null){
      Matrix p = X.times(b);
      predictedValues = p.getColumnPackedCopy();
      oriValues = y.getColumnPackedCopy();      
      r = MathUtils.correlation(predictedValues, oriValues);
      return r;
    } else {
      System.out.println("No values set for matrix X and y"); 
      return 0.0;            
    }    
  }
  
  public static void main(String[] args) throws Exception
  {
    Regression reg = new Regression();
    double[] yvals = {25.5, 31.2, 25.9, 38.4, 18.4, 26.7, 26.4, 25.9, 32.0, 25.2, 39.7, 35.7, 26.5};      
    double[][] xvals = {{1.74, 5.30, 10.8},
        {6.32, 5.42, 9.4},
        {6.22, 8.41, 7.2},
        {10.52, 4.63, 8.5},
        {1.19,11.60, 9.4},
        {1.22, 5.85, 9.9},
        {4.10, 6.62, 8.0},
        {6.32, 8.72, 9.1},
        {4.08, 4.42, 8.7},
        {4.15, 7.60, 9.2},
        {10.15, 4.83, 9.4},
        {1.72, 3.12, 7.6},
        {1.70, 5.30, 8.2}};   
    
    boolean intercepTerm = false;

    double coeffs[] = reg.multipleLinearRegression(yvals, xvals, intercepTerm);
    
    Vector<Double> y = new Vector<Double>();
    Vector<Double> x = new Vector<Double>();
    Vector<Double> data = new Vector<Double>();
    double array[] = new double[13*4];
    
    int cols = 3;
    int rows = yvals.length;
    
    int n = 0;
    for(int i=0; i<rows; i++){
      y.add(yvals[i]);
      data.add(yvals[i]);
      array[n++] = yvals[i];
      for(int j=0; j<cols; j++){
        x.add(xvals[i][j]);
        data.add(xvals[i][j]);
        array[n++] = xvals[i][j];
      }
    }    
    System.out.println("Vectors y and x:");
    coeffs = reg.multipleLinearRegression(y, x, rows, cols, intercepTerm);  
     
    // All the data in only one Vector<Double>
    cols = 4; // because includes the dependent variable
    rows = yvals.length;
    System.out.println("Vector data:");
    coeffs = reg.multipleLinearRegression(data, rows, cols, intercepTerm);  
    
    // array
    System.out.println("Vector data:");
    coeffs = reg.multipleLinearRegression(array, rows, cols, intercepTerm);  
    
    
    
  }

  
  
   
  
}
