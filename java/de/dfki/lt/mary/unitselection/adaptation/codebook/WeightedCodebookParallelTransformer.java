/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

package de.dfki.lt.mary.unitselection.adaptation.codebook;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.mary.unitselection.adaptation.BaselineAdaptationItem;
import de.dfki.lt.mary.unitselection.adaptation.BaselineAdaptationSet;
import de.dfki.lt.mary.unitselection.adaptation.prosody.PitchStatistics;
import de.dfki.lt.mary.unitselection.adaptation.prosody.ProsodyTransformerParams;
import de.dfki.lt.mary.unitselection.voiceimport.BasenameList;
import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.mary.util.StringUtil;
import de.dfki.lt.signalproc.analysis.LsfFileHeader;

/**
 * @author oytun.turk
 *
 * This class implements transformation for weighted codebook mapping based voice conversion
 * using parallel training data (i.e. source and target data in pairs of audio recordings which have identical content)
 */
public class WeightedCodebookParallelTransformer extends
        WeightedCodebookTransformer {
    
    public WeightedCodebookMapper mapper;
    public WeightedCodebook codebook;
    private WeightedCodebookFile codebookFile;

    public WeightedCodebookParallelTransformer(WeightedCodebookPreprocessor pp,
            WeightedCodebookFeatureExtractor fe,
            WeightedCodebookPostprocessor po,
            WeightedCodebookTransformerParams pa) {
        super(pp, fe, po, pa);
        
        codebook = null;
        mapper = null;
    }
    
    public void run() throws IOException, UnsupportedAudioFileException
    {
        if (checkParams())
        {
            BaselineAdaptationSet inputSet = getInputSet(params.inputFolder);
            if (inputSet==null)
                System.out.println("No input files found in " + params.inputFolder);
            else
            {
                BaselineAdaptationSet outputSet = getOutputSet(inputSet, params.outputFolder);

                transform(inputSet, outputSet);
            }
        }
    }
    
    public boolean checkParams() throws IOException
    {
        params.inputFolder = StringUtil.checkLastSlash(params.inputFolder);
        params.outputBaseFolder = StringUtil.checkLastSlash(params.outputBaseFolder);
        codebookFile = null;
        
        if (!FileUtils.exists(params.codebookFile))
        {
            System.out.println("Error: Codebook file " + params.codebookFile + " not found!");
            return false;     
        }
        else //Read lsfParams from the codebook header
        {
            codebookFile = new WeightedCodebookFile(params.codebookFile, WeightedCodebookFile.OPEN_FOR_READ);
            codebook = new WeightedCodebook();
            
            codebook.header = codebookFile.readCodebookHeader();
            params.lsfParams = new LsfFileHeader(codebook.header.lsfParams);
            params.mapperParams.lpOrder = params.lsfParams.lpOrder;
        }
            
        if (!FileUtils.exists(params.inputFolder) || !FileUtils.isDirectory(params.inputFolder))
        {
            System.out.println("Error: Input folder " + params.inputFolder + " not found!");
            return false; 
        }
        
        if (!FileUtils.isDirectory(params.outputBaseFolder))
        {
            System.out.println("Creating output base folder " + params.outputBaseFolder + "...");
            FileUtils.createDirectory(params.outputBaseFolder);
        }
        
        if (params.outputFolderInfoString!="")
        {
            params.outputFolder = params.outputBaseFolder + params.outputFolderInfoString + 
                                  "_best" + String.valueOf(params.mapperParams.numBestMatches) + 
                                  "_steep" + String.valueOf(params.mapperParams.weightingSteepness) +
                                  "_prosody" + String.valueOf(params.prosodyParams.pitchStatisticsType) + "x" + String.valueOf(params.prosodyParams.pitchTransformationMethod);
        }
        else
        {
            params.outputFolder = params.outputBaseFolder + 
                                  "best" + String.valueOf(params.mapperParams.numBestMatches) +
                                  "_steep" + String.valueOf(params.mapperParams.weightingSteepness) +
                                  "_prosody" + String.valueOf(params.prosodyParams.pitchStatisticsType) + "x" + String.valueOf(params.prosodyParams.pitchTransformationMethod);
        }
            
        if (!FileUtils.isDirectory(params.outputFolder))
        {
            System.out.println("Creating output folder " + params.outputFolder + "...");
            FileUtils.createDirectory(params.outputFolder);
        }
        
        return true;
    }
    
    //Create list of input files
    public BaselineAdaptationSet getInputSet(String inputFolder)
    {   
        BasenameList b = new BasenameList(inputFolder, wavExt);
        
        BaselineAdaptationSet inputSet = new BaselineAdaptationSet(b.getListAsVector().size());
        
        for (int i=0; i<inputSet.items.length; i++)
            inputSet.items[i].setFromWavFilename(inputFolder + b.getName(i) + wavExt);
        
        return inputSet;
    }
    //
    
    //Create list of output files using input set
    public BaselineAdaptationSet getOutputSet(BaselineAdaptationSet inputSet, String outputFolder)
    {   
        BaselineAdaptationSet outputSet  = null;

        outputFolder = StringUtil.checkLastSlash(outputFolder);
        
        if (inputSet!=null && inputSet.items!=null)
        {
            outputSet = new BaselineAdaptationSet(inputSet.items.length);

            for (int i=0; i<inputSet.items.length; i++)
                outputSet.items[i].audioFile = outputFolder + StringUtil.getFileName(inputSet.items[i].audioFile) + "_output" + wavExt;
        }

        return outputSet;
    }
    //
    
    
    public void transform(BaselineAdaptationSet inputSet, BaselineAdaptationSet outputSet) throws UnsupportedAudioFileException
    {
        System.out.println("Transformation started...");
        
        if (inputSet.items!=null && outputSet.items!=null)
        {
            int numItems = Math.min(inputSet.items.length, outputSet.items.length);
            
            if (numItems>0)
            {
                preprocessor.run(inputSet);
                
                int desiredFeatures = WeightedCodebookFeatureExtractor.F0_FEATURES;
                
                try {
                    featureExtractor.run(inputSet, params, desiredFeatures);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            
            //Read the codebook
            codebookFile.readCodebookFileExcludingHeader(codebook);
            
            //Create a mapper object
            mapper = new WeightedCodebookMapper(params.mapperParams);
            
            //Do the transformations now
            for (int i=0; i<numItems; i++)
            {
                try {
                    transformOneItem(inputSet.items[i], outputSet.items[i], params, mapper, codebook);
                } catch (UnsupportedAudioFileException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
                System.out.println("Transformed file " + String.valueOf(i+1) + " of "+ String.valueOf(numItems));
            }
        } 
        
        System.out.println("Transformation completed...");
    }
    
    //This function performs the actual voice conversion
    public static void transformOneItem(BaselineAdaptationItem inputItem, 
                                        BaselineAdaptationItem outputItem,
                                        WeightedCodebookTransformerParams wctParams,
                                        WeightedCodebookMapper wcMapper,
                                        WeightedCodebook wCodebook
                                        ) throws UnsupportedAudioFileException, IOException
    {   
        if (wctParams.isFixedRateVocalTractConversion)
            wctParams.isSeparateProsody = true;
            
        //Desired values should be specified in the following four parameters
        double [] pscales = {1.0};
        double [] tscales = {1.0};
        double [] escales = {1.0};
        double [] vscales = {1.0};
        //
        
        //These are for fixed rate vocal tract transformation: Do not change these!!!
        double [] pscalesTemp = {1.0};
        double [] tscalesTemp = {1.0};
        double [] escalesTemp = {1.0};
        double [] vscalesTemp = {1.0};
        //
        
        WeightedCodebookFdpsolaAdapter adapter = null;

        String firstPassOutputWavFile = "";
        
        if (wctParams.isSeparateProsody)
        {
            firstPassOutputWavFile = StringUtil.getFolderName(outputItem.audioFile) + StringUtil.getFileName(outputItem.audioFile) + "_vt.wav";
            int tmpPitchTransformationMethod = wctParams.prosodyParams.pitchTransformationMethod;
            wctParams.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.NO_TRANSFORMATION;
            
            adapter = new WeightedCodebookFdpsolaAdapter(
                                  inputItem.audioFile, inputItem.f0File, 
                                  firstPassOutputWavFile, 
                                  wctParams.isVocalTractTransformation,
                                  wctParams.isFixedRateVocalTractConversion, 
                                  wctParams.isResynthesizeVocalTractFromSourceCodebook,
                                  wctParams.isVocalTractMatchUsingTargetCodebook,
                                  pscalesTemp, tscalesTemp, escalesTemp, vscalesTemp);
            
            //Separate prosody modification
            if (adapter!=null)
            {
                adapter.bSilent = !wctParams.isDisplayProcessingFrameCount;
                adapter.fdpsolaOnline(wctParams, wcMapper, wCodebook); //Call voice conversion version

                if (isScalingsRequired(pscales, tscales, escales, vscales) || tmpPitchTransformationMethod!=ProsodyTransformerParams.NO_TRANSFORMATION)
                {
                    System.out.println("Performing prosody modifications...");

                    wctParams.prosodyParams.pitchTransformationMethod = tmpPitchTransformationMethod;

                    adapter = new WeightedCodebookFdpsolaAdapter(
                            firstPassOutputWavFile, inputItem.f0File, 
                            outputItem.audioFile, 
                            false, //isVocalTractTransformation should be false 
                            false, //isFixedRateVocalTractConversion should be false to enable prosody modifications with FD-PSOLA
                            false, //isResynthesizeVocalTractFromSourceCodebook should be false
                            false, //isVocalTractMatchUsingTargetCodebook shuld be false
                            pscales, tscales, escales, vscales);

                    adapter.bSilent = true;
                    adapter.fdpsolaOnline(wctParams, null, wCodebook);
                }
                else //Copy output file
                    FileUtils.copy(firstPassOutputWavFile, outputItem.audioFile);

                //Delete first pass output file
                if (!wctParams.isSaveVocalTractOnlyVersion)
                    FileUtils.delete(firstPassOutputWavFile);

                System.out.println("Done...");
            }
        }
        else
        {
            adapter = new WeightedCodebookFdpsolaAdapter(
                                  inputItem.audioFile, inputItem.f0File, 
                                  outputItem.audioFile, 
                                  wctParams.isVocalTractTransformation,
                                  wctParams.isFixedRateVocalTractConversion, 
                                  wctParams.isResynthesizeVocalTractFromSourceCodebook,
                                  wctParams.isVocalTractMatchUsingTargetCodebook,
                                  pscales, tscales, escales, vscales);
            
            adapter.bSilent = !wctParams.isDisplayProcessingFrameCount;
            adapter.fdpsolaOnline(wctParams, wcMapper, wCodebook); //Call voice conversion version
        }
    }
    
    public static boolean isScalingsRequired(double[] pscales, double[] tscales, double[] escales, double[] vscales)
    {
        int i;
        for (i=0; i<pscales.length; i++)
        {
            if (pscales[i]!=1.0)
                return true;
        }
        
        for (i=0; i<tscales.length; i++)
        {
            if (tscales[i]!=1.0)
                return true;
        }
        
        for (i=0; i<escales.length; i++)
        {
            if (escales[i]!=1.0)
                return true;
        }
        
        for (i=0; i<vscales.length; i++)
        {
            if (vscales[i]!=1.0)
                return true;
        }
        
        return false;
    }

    public static void main(String[] args) throws IOException, UnsupportedAudioFileException {
        WeightedCodebookPreprocessor pp = new WeightedCodebookPreprocessor();
        WeightedCodebookFeatureExtractor fe = new WeightedCodebookFeatureExtractor();
        WeightedCodebookPostprocessor po = new WeightedCodebookPostprocessor();
        WeightedCodebookTransformerParams pa = new WeightedCodebookTransformerParams();
        
        pa.isDisplayProcessingFrameCount = true;
        
        pa.inputFolder = "d:\\1\\angry50\\test1";
        pa.outputBaseFolder = "d:\\1\\neutral_X_angry_50\\neutral2angryOut5";
        
        pa.codebookFile = "d:\\1\\neutral_X_angry_50\\neutralF_X_angryF.wcf";
        pa.outputFolderInfoString = "labels";
        
        //Set codebook mapper parameters
        pa.mapperParams.numBestMatches = 1; // Number of best matches in codebook
        pa.mapperParams.weightingSteepness = 1.0; // Steepness of weighting function in range [WeightedCodebookMapperParams.MIN_STEEPNESS, WeightedCodebookMapperParams.MAX_STEEPNESS]
        pa.mapperParams.freqRange = 5000.0; //Frequency range to be considered around center freq when matching LSFs (note that center freq is estimated automatically as the middle of most closest LSFs)
        
        // Distance measure for comparing source training and transformation features
        //pa.mapperParams.distanceMeasure = WeightedCodebookMapperParams.LSF_INVERSE_HARMONIC_DISTANCE;
        pa.mapperParams.distanceMeasure = WeightedCodebookMapperParams.LSF_INVERSE_HARMONIC_DISTANCE_SYMMETRIC; pa.mapperParams.alphaForSymmetric = 0.5; //Weighting factor for using weights of two lsf vectors in distance computation relatively. The range is [0.0,1.0]
        //pa.mapperParams.distanceMeasure = WeightedCodebookMapperParams.LSF_EUCLIDEAN_DISTANCE;
        //pa.mapperParams.distanceMeasure = WeightedCodebookMapperParams.LSF_MAHALANOBIS_DISTANCE;
        //pa.mapperParams.distanceMeasure = WeightedCodebookMapperParams.LSF_ABSOLUTE_VALUE_DISTANCE;
        
        // Method for weighting best codebook matches
        pa.mapperParams.weightingMethod = WeightedCodebookMapperParams.EXPONENTIAL_HALF_WINDOW;
        //pa.mapperParams.weightingMethod = WeightedCodebookMapperParams.TRIANGLE_HALF_WINDOW;

        ////Mean and variance of a specific distance measure can be optionally kept in the following
        // two parameters for z-normalization
        pa.mapperParams.distanceMean = 0.0; 
        pa.mapperParams.distanceVariance = 1.0;
        //
        
        pa.isForcedAnalysis = false;
        pa.isSourceVocalTractSpectrumFromCodebook = false;
        pa.isVocalTractTransformation = true;
        pa.isResynthesizeVocalTractFromSourceCodebook = false;
        pa.isVocalTractMatchUsingTargetCodebook = false;
        
        pa.isSeparateProsody = true;
        pa.isSaveVocalTractOnlyVersion = true;
        pa.isFixedRateVocalTractConversion = true;
        
        //Prosody transformation
        pa.prosodyParams.pitchStatisticsType = PitchStatistics.STATISTICS_IN_HERTZ;
        //pa.prosodyParams.pitchStatisticsType = PitchStatistics.STATISTICS_IN_LOGHERTZ;
        
        pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_MEAN;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_STDDEV;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_RANGE;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_SLOPE;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_INTERCEPT;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_MEAN_STDDEV;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_MEAN_SLOPE;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_INTERCEPT_STDDEV;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_INTERCEPT_SLOPE;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_MEAN;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_STDDEV;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_RANGE;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_SLOPE;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_INTERCEPT;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_MEAN_STDDEV;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_MEAN_SLOPE;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_INTERCEPT_STDDEV;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_INTERCEPT_SLOPE;
        
        pa.prosodyParams.isUseInputMean = false;
        pa.prosodyParams.isUseInputStdDev = false;
        pa.prosodyParams.isUseInputRange = false;
        pa.prosodyParams.isUseInputIntercept = false;
        pa.prosodyParams.isUseInputSlope = false;
        //
        
        WeightedCodebookParallelTransformer t = new WeightedCodebookParallelTransformer(pp, fe, po, pa);
        t.run();
    }
}