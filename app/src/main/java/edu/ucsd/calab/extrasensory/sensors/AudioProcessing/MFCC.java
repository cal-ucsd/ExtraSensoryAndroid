package edu.ucsd.calab.extrasensory.sensors.AudioProcessing;

/**Calculates the mel-based cepstra coefficients for one frame of speech.
 * Based on the original MFCC implementation described in:  
 * [1] Davis & Mermelstein - IEEE Transactions on ASSP, August 1980.  
 * Additional references are:  
 * [2] Joseph Picone, Proceedings of the IEEE, Sep. 1993.  
 * [3] Jankowski et al. IEEE Trans. on Speech and Audio Processing. July, 1995.  
 * [4] Cardin et al, ICASSP'93 - pp. II-243  
 *
 * Notice that there are several different implementations of the mel filter  
 * bank. For example, the log is usually implementated after having the filter  
 * outputs calculated, but could be implemented before filtering. Besides, there are  
 * differences in the specification of the filter frequencies. [1]  
 * suggested linear scale until 1000 Hz and logarithm scale afterwards.  
 * This implementation uses the equation (10) in [2]:  
 *      mel frequency = 2595 log(1 + (f/700)), where log is base 10  
 * to find the filter bank center frequencies.  
 *
 * @author Aldebaro Klautau  
 * @version 2.0 - March 07, 2001  
 */

// if m_oisZeroThCepstralCoefficientCalculated is true,   
// this class decrements m_nnumberOfParameters by 1 and   
// adds the 0-th coefficient to complete a vector with   
// the number of MFCC's specified by the user.   
public class MFCC {

    //parameter USEPOWER in HTK, where default is false
    private static final boolean m_ousePowerInsteadOfMagnitude = false;

    /**Number of MFCCs per speech frame.  
     */
    private final int m_nnumberOfParameters;
    /**Sampling frequency.  
     */
    private final double m_dsamplingFrequency;
    /**Number of filter in mel filter bank.  
     */
    private final int m_nnumberOfFilters;
    /**Number of FFT points.  
     */
    private final int m_nFFTLength;
    /**Coefficient of filtering performing in cepstral domain  
     * (called 'liftering' operation). It is not used if  
     * m_oisLifteringEnabled is false.  
     */
    private final int m_nlifteringCoefficient;
    /**True enables liftering.  
     */
    private final boolean m_oisLifteringEnabled;
    /**Minimum value of filter output, otherwise the log is not calculated  
     * and m_dlogFilterOutputFloor is adopted.  
     * ISIP implementation assumes m_dminimumFilterOutput = 1 and this value is used  
     * here.  
     */
    private final double m_dminimumFilterOutput = 1.0;

    /**True if the zero'th MFCC should be calculated.  
     */
    private final boolean m_oisZeroThCepstralCoefficientCalculated;

    /**Floor value for filter output in log domain.  
     * ISIP implementation assumes m_dlogFilterOutputFloor = 0 and this value is used  
     * here.  
     */
    private final double m_dlogFilterOutputFloor = 0.0;
    private int[][] m_nboundariesDFTBins;
    private double[][] m_dweights;
    private FFT m_fft;
    private double[][] m_ddCTMatrix;

    private double[] m_dfilterOutput;
    private final double[] m_nlifteringMultiplicationFactor;

    //things to be calculated just once:   
    private final double m_dscalingFactor;

    /**The 0-th coefficient is included in nnumberOfParameters.  
     * So, if one wants 12 MFCC's and additionally the 0-th  
     * coefficient, one should call the constructor with  
     * nnumberOfParameters = 13 and  
     * oisZeroThCepstralCoefficientCalculated = true  
     */
    public MFCC(int nnumberOfParameters,
                double dsamplingFrequency,
                int nnumberofFilters,
                int nFFTLength,
                boolean oisLifteringEnabled,
                int nlifteringCoefficient,
                boolean oisZeroThCepstralCoefficientCalculated) {

        m_oisZeroThCepstralCoefficientCalculated = oisZeroThCepstralCoefficientCalculated;
        if (m_oisZeroThCepstralCoefficientCalculated) {
            //the user shouldn't notice that nnumberOfParameters was   
            //decremented internally   
            m_nnumberOfParameters = nnumberOfParameters - 1;
        } else {
            m_nnumberOfParameters = nnumberOfParameters;
        }

        m_dsamplingFrequency = dsamplingFrequency;
        m_nnumberOfFilters = nnumberofFilters;
        m_nFFTLength = nFFTLength;

        //the filter bank weights, FFT's cosines and sines   
        //and DCT matrix are initialized once to save computations.   

        //initializes the mel-based filter bank structure   
        calculateMelBasedFilterBank(dsamplingFrequency,
                nnumberofFilters,
                nFFTLength);
        m_fft = new FFT(m_nFFTLength); //initialize FFT   
        initializeDCTMatrix();
        m_nlifteringCoefficient = nlifteringCoefficient;
        m_oisLifteringEnabled = oisLifteringEnabled;

        //avoid allocating RAM space repeatedly, m_dfilterOutput is   
        //going to be used in method getParameters()   
        m_dfilterOutput = new double[m_nnumberOfFilters];

        //needed in method getParameters()   
        //m_dscalingFactor shouldn't be necessary because it's only   
        //a scaling factor, but I'll implement it   
        //for the sake of getting the same numbers ISIP gets   
        m_dscalingFactor = Math.sqrt(2.0 / m_nnumberOfFilters);

        //for liftering method   
        if (m_oisLifteringEnabled) {
            //note that:   
            int nnumberOfCoefficientsToLift = m_nnumberOfParameters;
            //even when m_oisZeroThCepstralCoefficientCalculated is true   
            //because if 0-th cepstral coefficient is included,   
            //it is not liftered   
            m_nlifteringMultiplicationFactor = new double[m_nlifteringCoefficient];
            double dfactor = m_nlifteringCoefficient / 2.0;
            double dfactor2 = Math.PI / m_nlifteringCoefficient;
            for (int i=0; i<m_nlifteringCoefficient; i++) {
                m_nlifteringMultiplicationFactor[i] = 1.0 + dfactor * Math.sin(dfactor2*(i+1));
            }
            if (m_nnumberOfParameters > m_nlifteringCoefficient) {
                new Error("Liftering is enabled and the number " +
                        "of parameters = " + m_nnumberOfParameters + ", while " +
                        "the liftering coefficient is " + m_nlifteringCoefficient +
                        ". In this case some cepstrum coefficients would be made " +
                        "equal to zero due to liftering, what does not make much " +
                        "sense in a speech recognition system. You may want to " +
                        "increase the liftering coefficient or decrease the number " +
                        "of MFCC parameters.");
            }
        } else {
            m_nlifteringMultiplicationFactor = null;
        }
    }

    /**Initializes the DCT matrix.*/
    private void initializeDCTMatrix() {
        m_ddCTMatrix = new double[m_nnumberOfParameters][m_nnumberOfFilters];
        for(int i=0;i<m_nnumberOfParameters;i++) {
            for(int j=0;j<m_nnumberOfFilters;j++) {
                m_ddCTMatrix[i][j] = Math.cos((i+1.0)*(j+1.0-0.5)*(Math.PI/m_nnumberOfFilters));
            }
        }
    }

    /**Converts frequencies in Hz to mel scale according to  
     * mel frequency = 2595 log(1 + (f/700)), where log is base 10  
     * and f is the frequency in Hz.  
     */
    public static double[] convertHzToMel(double[] dhzFrequencies, double dsamplingFrequency) {
        double[] dmelFrequencies = new double[dhzFrequencies.length];
        for (int k=0; k<dhzFrequencies.length; k++) {
            dmelFrequencies[k] = 2595.0*( Math.log(1.0 + (dhzFrequencies[k] / 700.0) ) / Math.log(10) );
        }
        return dmelFrequencies;
    }

    /**Calculates triangular filters.  
     */
    private void calculateMelBasedFilterBank(double dsamplingFrequency,
                                             int nnumberofFilters,
                                             int nfftLength) {

        //frequencies for each triangular filter   
        double[][] dfrequenciesInMelScale = new double[nnumberofFilters][3];
        //the +1 below is due to the sample of frequency pi (or fs/2)   
        double[] dfftFrequenciesInHz = new double[nfftLength/2 + 1];
        //compute the frequency of each FFT sample (in Hz):   
        double ddeltaFrequency = dsamplingFrequency / nfftLength;
        for (int i=0; i<dfftFrequenciesInHz.length; i++) {
            dfftFrequenciesInHz[i] = i * ddeltaFrequency;
        }
        //convert Hz to Mel   
        double[] dfftFrequenciesInMel = this.convertHzToMel(dfftFrequenciesInHz,dsamplingFrequency);

        //compute the center frequencies. Notice that 2 filters are   
        //"artificially" created in the endpoints of the frequency   
        //scale, correspondent to 0 and fs/2 Hz.   
        double[] dfilterCenterFrequencies = new double[nnumberofFilters + 2];
        //implicitly: dfilterCenterFrequencies[0] = 0.0;   
        ddeltaFrequency = dfftFrequenciesInMel[dfftFrequenciesInMel.length-1] / (nnumberofFilters+1);
        for (int i = 1; i < dfilterCenterFrequencies.length; i++) {
            dfilterCenterFrequencies[i] = i * ddeltaFrequency;
        }

        //initialize member variables   
        m_nboundariesDFTBins= new int[m_nnumberOfFilters][2];
        m_dweights = new double[m_nnumberOfFilters][];

        //notice the loop starts from the filter i=1 because i=0 is the one centered at DC   
        for (int i=1; i<= nnumberofFilters; i++) {
            m_nboundariesDFTBins[i-1][0] = Integer.MAX_VALUE;
            //notice the loop below doesn't include the first and last FFT samples   
            for (int j=1; j<dfftFrequenciesInMel.length-1; j++) {
                //see if frequency j is inside the bandwidth of filter i   
                if ( (dfftFrequenciesInMel[j] >= dfilterCenterFrequencies[i-1]) &
                        (dfftFrequenciesInMel[j] <= dfilterCenterFrequencies[i+1]) ) {
                    //the i-1 below is due to the fact that we discard the first filter i=0   
                    //look for the first DFT sample for this filter   
                    if (j < m_nboundariesDFTBins[i-1][0]) {
                        m_nboundariesDFTBins[i-1][0] = j;
                    }
                    //look for the last DFT sample for this filter   
                    if (j > m_nboundariesDFTBins[i-1][1]) {
                        m_nboundariesDFTBins[i-1][1] = j;
                    }
                }
            }
        }
        //check for consistency. The problem below would happen just   
        //in case of a big number of MFCC parameters for a small DFT length.   
        for (int i=0; i< nnumberofFilters; i++) {
            if(m_nboundariesDFTBins[i][0]==m_nboundariesDFTBins[i][1]) {
                new Error("Error in MFCC filter bank. In filter "+i+" the first sample is equal to the last sample !" +
                        " Try changing some parameters, for example, decreasing the number of filters.");
            }
        }

        //allocate space   
        for(int i=0;i<nnumberofFilters;i++) {
            m_dweights[i] = new double[m_nboundariesDFTBins[i][1]-m_nboundariesDFTBins[i][0]+1];
        }

        //calculate the weights   
        for(int i=1;i<=nnumberofFilters;i++) {
            for(int j=m_nboundariesDFTBins[i-1][0],k=0;j<=m_nboundariesDFTBins[i-1][1];j++,k++) {
                if (dfftFrequenciesInMel[j] < dfilterCenterFrequencies[i]) {
                    m_dweights[i-1][k] = (dfftFrequenciesInMel[j]-dfilterCenterFrequencies[i-1]) /
                            (dfilterCenterFrequencies[i] - dfilterCenterFrequencies[i-1]);
                } else {
                    m_dweights[i-1][k] = 1.0 -( (dfftFrequenciesInMel[j]-dfilterCenterFrequencies[i]) /
                            (dfilterCenterFrequencies[i+1] - dfilterCenterFrequencies[i]) );
                }
            }
        }
    }

    /**Returns the MFCC coefficients for the given speech frame.  
     * If calculated, the 0-th coefficient is added to the  
     * end of the vector (for compatibility with HTK). The order  
     * of an output vector x with 3 MFCC's, including the 0-th, would be:  
     * x = {MFCC1, MFCC2, MFCC0}
     */
    public double[] getParameters(double[] fspeechFrame) {

        // First, calculate the spectrum (added by Yonatan, to do just once, outside the loop):
        double[] spectrum = m_ousePowerInsteadOfMagnitude ? m_fft.calculateFFTPower(fspeechFrame) : m_fft.calculateFFTMagnitude(fspeechFrame);

        //use mel filter bank
        for(int i=0; i < m_nnumberOfFilters; i++) {
            m_dfilterOutput[i] = 0.0;
            //Notice that the FFT samples at 0 (DC) and fs/2 are not considered on this calculation

            // Added by Yonatan: using the pre-calculated spectrum (either power or magnitude):
            for(int j=m_nboundariesDFTBins[i][0], k=0;j<=m_nboundariesDFTBins[i][1];j++,k++) {
                m_dfilterOutput[i] += spectrum[j] * m_dweights[i][k];
            }

/*
            if (m_ousePowerInsteadOfMagnitude) {
                double[] fpowerSpectrum = m_fft.calculateFFTPower(fspeechFrame);
                for(int j=m_nboundariesDFTBins[i][0], k=0;j<=m_nboundariesDFTBins[i][1];j++,k++) {
                    m_dfilterOutput[i] += fpowerSpectrum[j] * m_dweights[i][k];
                }
            } else {
                double[] fmagnitudeSpectrum = m_fft.calculateFFTMagnitude(fspeechFrame);
                for(int j=m_nboundariesDFTBins[i][0], k=0;j<=m_nboundariesDFTBins[i][1];j++,k++) {
                    m_dfilterOutput[i] += fmagnitudeSpectrum[j] * m_dweights[i][k];
                }
            }
*/

            //ISIP (Mississipi univ.) implementation   
            if (m_dfilterOutput[i] > m_dminimumFilterOutput) {//floor power to avoid log(0)   
                m_dfilterOutput[i] = Math.log(m_dfilterOutput[i]); //using ln   
            } else {
                m_dfilterOutput[i] = m_dlogFilterOutputFloor;
            }
        }

        //need to allocate space for output array   
        //because it allows the user to call this method   
        //many times, without having to do a deep copy   
        //of the output vector   
        double[] dMFCCParameters = null;
        if (m_oisZeroThCepstralCoefficientCalculated) {
            dMFCCParameters = new double[m_nnumberOfParameters + 1];
            //calculates zero'th cepstral coefficient and pack it   
            //after the MFCC parameters of each frame for the sake   
            //of compatibility with HTK   
            double dzeroThCepstralCoefficient = 0.0;
            for(int j=0;j<m_nnumberOfFilters;j++) {
                dzeroThCepstralCoefficient += m_dfilterOutput[j];
            }
            dzeroThCepstralCoefficient *= m_dscalingFactor;
            dMFCCParameters[dMFCCParameters.length-1] = dzeroThCepstralCoefficient;
        } else {
            //allocate space   
            dMFCCParameters = new double[m_nnumberOfParameters];
        }

        //cosine transform   
        for(int i=0;i<m_nnumberOfParameters;i++) {
            for(int j=0;j<m_nnumberOfFilters;j++) {
                dMFCCParameters[i] += m_dfilterOutput[j]*m_ddCTMatrix[i][j];
                //the original equations have the first index as 1   
            }
            //could potentially incorporate liftering factor and   
            //factor below to save multiplications, but will not   
            //do it for the sake of clarity   
            dMFCCParameters[i] *= m_dscalingFactor;
        }

        //debugging purposes   
        //System.out.println("Windowed speech");   
        //IO.DisplayVector(fspeechFrame);   
        //System.out.println("FFT spectrum");   
        //IO.DisplayVector(fspectrumMagnitude);   
        //System.out.println("Filter output in dB");   
        //IO.DisplayVector(dfilterOutput);   
        //System.out.println("DCT matrix");   
        //IO.DisplayMatrix(m_ddCTMatrix);   
        //System.out.println("MFCC before liftering");   
        //IO.DisplayVector(dMFCCParameters);   

        if (m_oisLifteringEnabled) {
            // Implements liftering to smooth the cepstral coefficients according to   
            // [1] Rabiner, Juang, Fundamentals of Speech Recognition, pp. 169,   
            // [2] The HTK Book, pp 68 and   
            // [3] ISIP package - Mississipi Univ. Picone's group.   
            //if 0-th coefficient is included, it is not liftered   
            for (int i=0; i<m_nnumberOfParameters; i++) {
                dMFCCParameters[i] *= m_nlifteringMultiplicationFactor[i];
            }
        }

        return dMFCCParameters;
    } //end method   

    /**Returns the sampling frequency.  
     */
    public double getSamplingFrequency() {
        return this.m_dsamplingFrequency;
    }

    /**Returns the number of points of the Fast Fourier  
     * Transform (FFT) used in the calculation of this MFCC.  
     */
    public int getFFTLength() {
        return m_nFFTLength;
    }

    /**Returns the number of MFCC coefficients,  
     * including the 0-th if required by user in the object construction.  
     */
    public int getNumberOfCoefficients() {
        return (m_oisZeroThCepstralCoefficientCalculated ? (m_nnumberOfParameters + 1) : m_nnumberOfParameters);
    }

    /**Return a string with all important parameters of this object.  
     */
    public String toString() {
        return
                "MFCC.nnumberOfParameters = " + (m_oisZeroThCepstralCoefficientCalculated ? (m_nnumberOfParameters + 1) : m_nnumberOfParameters) +
                        "\n" + "MFCC.nnumberOfFilters = " + m_nnumberOfFilters +
                        "\n" + "MFCC.nFFTLength = " + m_nFFTLength +
                        "\n" + "MFCC.dsamplingFrequency = " + m_dsamplingFrequency +
                        "\n" + "MFCC.nlifteringCoefficient = " + m_nlifteringCoefficient +
                        "\n" + "MFCC.oisLifteringEnabled = " + m_oisLifteringEnabled +
                        "\n" + "MFCC.oisZeroThCepstralCoefficientCalculated = " + m_oisZeroThCepstralCoefficientCalculated;
    }

    public double[] getFilterBankOutputs(double[] fspeechFrame) {
        // First, calculate the spectrum (added by Yonatan, to be outside the loop):
        double[] spectrum;
        if (m_ousePowerInsteadOfMagnitude) {
            spectrum = m_fft.calculateFFTPower(fspeechFrame);
        }
        else {
            spectrum = m_fft.calculateFFTMagnitude(fspeechFrame);
        }

        //use mel filter bank   
        double dfilterOutput[] = new double[m_nnumberOfFilters];
        for(int i=0; i < m_nnumberOfFilters; i++) {
            //Notice that the FFT samples at 0 (DC) and fs/2 are not considered on this calculation
            for(int j=m_nboundariesDFTBins[i][0], k=0;j<=m_nboundariesDFTBins[i][1];j++,k++) {
                dfilterOutput[i] += spectrum[j] * m_dweights[i][k];
            }
/*
            if (m_ousePowerInsteadOfMagnitude) {
                double[] fpowerSpectrum = m_fft.calculateFFTPower(fspeechFrame);
                for(int j=m_nboundariesDFTBins[i][0], k=0;j<=m_nboundariesDFTBins[i][1];j++,k++) {
                    dfilterOutput[i] += fpowerSpectrum[j] * m_dweights[i][k];
                }
            } else {
                double[] fmagnitudeSpectrum = m_fft.calculateFFTMagnitude(fspeechFrame);
                for(int j=m_nboundariesDFTBins[i][0], k=0;j<=m_nboundariesDFTBins[i][1];j++,k++) {
                    dfilterOutput[i] += fmagnitudeSpectrum[j] * m_dweights[i][k];
                }
            }
*/

            //ISIP (Mississipi univ.) implementation   
            if (dfilterOutput[i] > m_dminimumFilterOutput) {//floor power to avoid log(0)   
                dfilterOutput[i] = Math.log(dfilterOutput[i]); //using ln   
            } else {
                dfilterOutput[i] = m_dlogFilterOutputFloor;
            }
        }
        return dfilterOutput;
    }

    /*
    public static void main(String[] args) {

        int nnumberofFilters = 24;
        int nlifteringCoefficient = 22;
        boolean oisLifteringEnabled = true;
        boolean oisZeroThCepstralCoefficientCalculated = false;
        int nnumberOfMFCCParameters = 12; //without considering 0-th
        double dsamplingFrequency = 8000.0;
        int nFFTLength = 512;
        if (oisZeroThCepstralCoefficientCalculated) {
            //take in account the zero-th MFCC   
            nnumberOfMFCCParameters = nnumberOfMFCCParameters + 1;
        }
        else {
            nnumberOfMFCCParameters = nnumberOfMFCCParameters;
        }

        MFCC mfcc = new MFCC(nnumberOfMFCCParameters,
                dsamplingFrequency,
                nnumberofFilters,
                nFFTLength,
                oisLifteringEnabled,
                nlifteringCoefficient,
                oisZeroThCepstralCoefficientCalculated);

        System.out.println(mfcc.toString());

        //simulate a frame of speech
        double[] x = new double[160];
        x[2]=10; x[4]=14;
        double[] dparameters = mfcc.getParameters(x);
        System.out.println("MFCC parameters:");
        for (int i = 0; i < dparameters.length; i++) {
            System.out.print(" " + dparameters[i]);

        }

    }
*/

} // end of class 