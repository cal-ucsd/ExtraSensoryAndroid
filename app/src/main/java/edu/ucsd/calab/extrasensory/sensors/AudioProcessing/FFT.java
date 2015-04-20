package edu.ucsd.calab.extrasensory.sensors.AudioProcessing;

/**Calculates the DFT using a split-radix Fast Fourier Transform algorithm.
 * @author Junan Zhang  
 */

public final class FFT {
    public int logm;
    final  int  MAXLOGM=20;     /* max FFT length      2^MAXLOGM */
    final  double TWOPI=6.28318530717958647692;
    final  double SQHALF=0.707106781186547524401;
    int brseed[]= new int[4048];
    float tab[][];

    public FFT(int nlength ) {
        double dtemp = Math.log(nlength) / Math.log(2);
        if ( (dtemp - (int) dtemp) != 0.0) {
            throw new Error("FFT length must be a power of 2.");
        } else {
            this.logm = (int) dtemp;
        }
        if (logm>=4) {
            creattab(logm);
        }
    }

    /** Calculates the magnitude spectrum of a real signal.
     * The returned vector contains only the positive frequencies.
     */
    public float[] calculateFFTMagnitude(float x[]) {
        int i,n;
        n=1<<this.logm;

        if (x.length > n) {
            throw new Error("Tried to use a " + n + "-points FFT for a vector with " +
                    x.length + " samples!");
        }




        rsfft(x);

        float[] mag = new float[n/2 + 1];
        mag[0] = x[0]; //DC frequency must be positive always   

        if (n==1) {
            return mag;
        }
        mag[n/2] = (float) Math.abs(x[n/2]); //pi (meaning: fs / 2)   

        //System.out.println("FFT before magnitude");   
        //IO.DisplayVector(x);   

        for (i=1;i<n/2;i++) {
            mag[i] = (float) Math.sqrt(x[i]*x[i]+x[n-i]*x[n-i]);
            //System.out.println(mag[i] + " " + x[i] + " " + x[n-i]);   
        }

        //IO.DisplayVector(mag);   
        return mag;
    }

    /** Calculates the magnitude spectrum of a real signal.
     * The returned vector contains only the positive frequencies.
     */
    public double[] calculateFFTMagnitude(double inputData[]) {
        int i,n;
        n=1<<this.logm;
        if (inputData.length > n) {
            throw new Error("Tried to use a " + n + "-points FFT for a vector with " +
                    inputData.length + " samples!");
        }

        //System.out.println("magnitude test");   
        //double[] dtest = DSP.DFTMagnitude(inputData);   
        //IO.DisplayVector(dtest);   

        float[] x = new float[n];
        for (i=0; i<inputData.length; i++) {
            x[i] = (float) inputData[i];
        }

        rsfft(x);

        //System.out.println("FFT before magnitude");   
        //IO.DisplayVector(x);   

        double[] mag = new double[n/2 + 1];
        mag[0] = x[0]; //DC frequency must be positive always   

        if (n==1) {
            return mag;
        }
        mag[n/2] = Math.abs(x[n/2]); //pi (meaning: fs / 2)   

        for (i=1;i<n/2;i++) {
            mag[i] = Math.sqrt(x[i]*x[i]+x[n-i]*x[n-i]);
            //System.out.println(mag[i] + " " + x[i] + " " + x[n-i]);   
        }

        //IO.DisplayVector(mag);   
        return mag;
    }

    /** Calculates the power (magnitude squared) spectrum of a real signal.
     * The returned vector contains only the positive frequencies.
     */
    public double[] calculateFFTPower(double inputData[]) {
        int i,n;
        n=1<<this.logm;

        //System.out.println("magnitude test");   
        //double[] dtest = DSP.DFTMagnitude(inputData);   
        //IO.DisplayVector(dtest);   

        float[] x = new float[n];
        for (i=0; i<inputData.length; i++) {
            x[i] = (float) inputData[i];
        }

        rsfft(x);

        //System.out.println("FFT before magnitude");   
        //IO.DisplayVector(x);   

        double[] mag = new double[n/2 + 1];
        mag[0] = x[0]; //DC frequency must be positive always   

        if (n==1) {
            return mag;
        }
        mag[n/2] = Math.abs(x[n/2]); //pi (meaning: fs / 2)   

        for (i=1;i<n/2;i++) {
            mag[i] = x[i]*x[i]+x[n-i]*x[n-i];
            //mag[i] = Math.sqrt(x[i]*x[i]+x[n-i]*x[n-i]);   
            //System.out.println(mag[i] + " " + x[i] + " " + x[n-i]);   
        }

        //IO.DisplayVector(mag);   
        return mag;
    }

    /**In place calculation of FFT magnitude.
     */
    public void FFTMagnitude(float x[])
    { int i,n;
        rsfft(x);
        n=1<<this.logm;
        if (n==1) return;
        for (i=1;i<n/2;i++)
        {x[i]=(float)Math.sqrt(x[i]*x[i]+x[n-i]*x[n-i]);
            x[n-i]=x[i];
        }
        //Aldebaro modification:
        x[n/2] = Math.abs(x[n/2]);
    }

    void rsfft(float x[])
    {
      /*creat table*/
        //  if(logm>=4) creattab(logm);
      /* Call recursive routine */

        rsrec(x,logm);
   
    /* Output array unshuffling using bit-reversed indices */
        if (logm > 1) {
            BR_permute(x, logm);
            return ;
        }
    }
   
/* -------------------------------------------------------------------- *  
   *   Inverse  transform  for  real  inputs                              *  
   *--------------------------------------------------------------------  */

    void  rsifft(float x[])
    {
        int       i, m;
        float     fac;
        int         xp;
   
   /* Output array unshuffling using bit-reversed indices */
        if (logm > 1) {
            BR_permute(x, logm);
        }
        x[0] *= 0.5;
        if (logm > 0) x[1] *= 0.5;
 /*creat table*/
        //if(logm>=4) creattab(logm);
   /*  Call  recursive  routine */

        rsirec(x, logm);
   
   /* Normalization */
        m = 1 << logm;
        fac = (float)2.0 / m;
        xp = 0;

        for (i = 0; i < m; i++) {
            x[xp++] *= fac;
        }
    }
   
/* -------------------------------------------------------------------- *  
 *     Creat multiple fator table                                       *  
 * -------------------------------------------------------------------- */

    void   creattab(int logm)
    { int       m, m2, m4, m8, nel, n, rlogm;
        int       cn, spcn, smcn, c3n, spc3n, smc3n;
        double    ang, s, c;
        tab=new float [logm-4+1][6*((1<<logm)/4-2)];
        for(rlogm=logm; rlogm>=4;rlogm--)

        {m = 1 << rlogm; m2 = m / 2; m4 = m2 / 2; m8 = m4 / 2; nel=m4-2;
 /* Initialize pointers */

            cn =0; spcn = cn + nel;  smcn = spcn + nel;c3n = smcn + nel; spc3n = c3n + nel; smc3n = spc3n + nel;
   
   
    /* Compute tables */
            for (n = 1; n < m4; n++) {
                if (n == m8) continue;
                ang = n * TWOPI / m;
                c = Math.cos(ang);  s = Math.sin(ang);
                tab[rlogm-4][cn++] = (float)c;  tab[rlogm-4][spcn++] = (float)(- (s + c)); tab[rlogm-4][smcn++] =(float)( s - c);

                ang = 3 * n * TWOPI / m;
                c = Math.cos(ang);  s = Math. sin(ang);
                tab[rlogm-4][c3n++] = (float)c; tab[rlogm-4][spc3n++] = (float)(- (s + c)); tab[rlogm-4][smc3n++] = (float)(s - c);
            }
        }
    }
   
/* -------------------------------------------------------------------- *  
 *     Recursive part of the RSFFT algorithm.       Not externally      *  
 *     callable.                                                        *  
 * -------------------------------------------------------------------- */

    void  rsrec(float x[],int logm)
    {
        int       m, m2, m4, m8, nel, n;
        int       x0=0;
        int       xr1, xr2, xi1;
        int       cn=0;
        int       spcn=0;
        int       smcn=0;
        float     tmp1, tmp2;
        double    ang, c, s;
   
   
   
   
     /* Check range   of logm */
        try{ if ((logm < 0) || (logm > MAXLOGM)) {
            System.err.println("FFT length m is too big: log2(m) = "+logm+"is out of bounds ["+0+","+MAXLOGM+"]");

            throw new OutofborderException(logm);
        }}
        catch( OutofborderException e)
        {throw new OutOfMemoryError();}
   
     /* Compute trivial cases */

        if (logm < 2) {
            if (logm == 1) {    /* length m = 2 */
                xr2  = x0 + 1;
                tmp1 = x[x0] + x[xr2];
                x[xr2] = x[x0] - x[xr2];
                x[x0]   =  tmp1;
                return;
            }
            else if (logm == 0) return;      /* length m = 1 */
        }
   
     /* Compute a few constants */
        m = 1 << logm; m2 = m / 2; m4 = m2 / 2; m8 = m4 / 2;
   
     /* Build tables of butterfly coefficients, if necessary */
        //if ((logm >= 4) && (tab[logm-4][0] == 0)) {
   
         /* Allocate memory for tables */
        //  nel = m4 - 2;
   
    /*if ((tab[logm-4] = (float *) calloc(3 * nel, sizeof(float)))  
       == NULL) {  
       printf("Error : RSFFT : not enough memory for cosine tables.\n");  
       error_exit();  
    }*/   
   
   
    /* Initialize pointers */
        //tabi=logm-4;
        //   cn  =0; spcn = cn + nel;  smcn = spcn + nel;
   
    /* Compute tables */   
    /*for (n = 1; n < m4; n++) {  
       if (n == m8) continue;  
       ang = n * (float)TWOPI / m;  
       c = Math.cos(ang);  s = Math.sin(ang);  
       tab[tabi][cn++] = (float)c;  tab[tabi][spcn++] = (float)(- (s + c)); tab[tabi][smcn++] =(float)( s - c);  
   }  
}  
  
/*  Step  1 */
        xr1 = x0;  xr2 = xr1 + m2;
        for (n = 0; n < m2; n++) {
            tmp1 = x[xr1] + x[xr2];
            x[xr2] = x[xr1] - x[xr2];
            x[xr1] = tmp1;
            xr1++; xr2++;
        }
   
/*  Step  2        */
        xr1 = x0 + m2 + m4;
        for (n = 0; n < m4; n++) {
            x[xr1] = - x[xr1];
            xr1++;
        }
   
/*  Steps 3 &  4 */
        xr1 = x0 + m2; xi1 = xr1 + m4;
        if (logm >= 4) {
            nel = m4 - 2;
            cn  = 0; spcn = cn + nel;  smcn = spcn + nel;
        }

        xr1++; xi1++;
        for (n = 1; n < m4; n++) {
            if (n == m8) {
                tmp1 = (float)( SQHALF * (x[xr1] + x[xi1]));
                x[xi1]  = (float)(SQHALF * (x[xi1] - x[xr1]));
                x[xr1]  = tmp1;
            }  else {//System.out.println ("logm-4="+(logm-4));
                tmp2 = tab[logm-4][cn++] * (x[xr1] + x[xi1]);
                tmp1 = tab[logm-4][spcn++] * x[xr1] + tmp2;
                x[xr1] = tab[logm-4][smcn++] * x[xi1] + tmp2;
                x[xi1] = tmp1;
            }
            //System.out.println ("logm-4="+(logm-4));
            xr1++; xi1++;
        }
   
    /*  Call rsrec again with half DFT length */
        rsrec(x,logm-1);
   
    /* Call complex DFT routine, with quarter DFT length.  
        Constants have to be recomputed, because they are static! */
        m = 1 << logm; m2 = m / 2; m4 = 3 * (m / 4);
        srrec(x,x0 + m2, x0 + m4, logm-2);
   
    /* Step 5: sign change & data reordering */
        m = 1 << logm; m2 = m / 2; m4 = m2 / 2; m8 = m4 / 2;
        xr1 = x0 + m2 + m4;
        xr2 = x0 + m - 1;
        for (n = 0; n < m8; n++) {
            tmp1   = x[xr1];
            x[xr1++] = - x[xr2];
            x[xr2--] = - tmp1;
        }
        xr1 = x0 + m2 + 1;
        xr2 = x0 + m - 2;
        for (n = 0; n < m8; n++) {
            tmp1   =   x[xr1];
            x[xr1++] = - x[xr2];
            x[xr2--] =   tmp1;
            xr1++;
            xr2--;
        }
        if (logm == 2) x[3] = -x[3];
    }
/* --------------------------------------------------------------------- *  
 *  Recursive part of the inverse RSFFT algorithm.  Not externally       *  
 *  callable.                                                            *  
 *  -------------------------------------------------------------------- */

    void  rsirec(float  x[],  int   logm)
    {
        int       m, m2, m4, m8, nel, n;
        int       xr1, xr2, xi1;
        int       x0=0;
        int       cn, spcn, smcn;
        float     tmp1, tmp2;
        cn=0;smcn=0;spcn=0;
   
     /* Check  range  of logm */
        try{ if ((logm < 0) || (logm > MAXLOGM)) {
            System.err.println("FFT length m is too big: log2(m) = "+logm+"is out of bounds ["+0+","+MAXLOGM+"]");
            throw new OutofborderException(logm);
        }}
        catch( OutofborderException e)
        {throw new OutOfMemoryError();}
   
     /*  Compute  trivial  cases */
        if (logm < 2) {
            if (logm == 1) {     /* length m = 2 */
                xr2  = x0 + 1;
                tmp1 = x[x0] + x[xr2];
                x[xr2] = x[x0] - x[xr2];
                x[0]= tmp1;
                return;
            }
            else if (logm == 0) return;       /* length m = 1 */
        }
   
     /* Compute a few constants */
        m = 1 << logm; m2 = m / 2; m4 = m2 / 2; m8 = m4 / 2;
   
     /* Build tables of butterfly    coefficients, if necessary */
        // if((logm >= 4) && (tab[logm-4] == NULL)) {
   
       /* Allocate memory for tables */   
      /*el = m4 - 2;  
       if ((tab[logm-4] = (float *) calloc(3 * nel, sizeof(float)))  
           == NULL) {  
           printf("Error : RSFFT : not enough memory for cosine tables.\n");  
           error_exit();  
       }  
  
       /*  Initialize   pointers */
        //cn  = tab[logm-4] ; spcn = cn + nel; smcn = spcn + nel;
   
     /*  Compute  tables */   
     /* (n = 1; n < m4; n++) {  
         if (n == m8) continue;  
         ang = n * TWOPI / m;  
         c = cos(ang); s = sin(ang);  
         *cn++ = c; *spcn++ = - (s + c); *smcn++ = s - c;  
    }  
}  
 /* Reverse Step 5: sign change & data reordering */
        if (logm == 2) x[3] = -x[3];
        xr1 = x0+ m2 + 1;
        xr2 = x0+ m - 2;
        for (n = 0; n < m8; n++) {
            tmp1   =   x[xr1];
            x[xr1++] =   x[xr2];
            x[xr2--] = - tmp1;
            xr1++;
            xr2--;
        }
        xr1 = x0 + m2 + m4;
        xr2 = x0 + m - 1;
        for (n = 0; n < m8; n++) {
            tmp1   =   x[xr1];
            x[xr1++] = - x[xr2];
            x[xr2--] = - tmp1;
        }
 /*  Call   rsirec again with half DFT length */
        rsirec(x, logm-1);
   
 /* Call complex DFT routine, with quarter DFT length.  
     Constants have to be recomputed, because they are static! */   
   
 /*Now in Java version, we set the multiple Constant to be global*/
        m = 1 << logm; m2 = m / 2; m4 = 3 * (m / 4);
        srrec(x,x0 + m4, x0 + m2, logm-2);
   
 /* Reverse Steps 3 & 4 */
        m = 1 << logm; m2 = m / 2; m4 = m2 / 2; m8 = m4 / 2;
        xr1 = x0 + m2; xi1 = xr1 + m4;
        if (logm >= 4) {
            nel = m4 - 2;
            cn  = 0; spcn = cn + nel; smcn = spcn + nel;
        }
        xr1++; xi1++;
        for (n = 1; n < m4; n++) {
            if (n == m8) {
                tmp1 = (float)(SQHALF * (x[xr1] - x[xi1]));
                x[xi1] = (float)(SQHALF * (x[xi1] + x[xr1]));
                x[xr1] = tmp1;
            }    else {
                tmp2 = tab[logm-4][cn++] * (x[xr1] + x[xi1]);
                tmp1 = tab[logm-4][smcn++] * x[xr1] + tmp2;
                x[xr1] = tab[logm-4][spcn++] * x[xi1] + tmp2;
                x[xi1] = tmp1;
            }
            xr1++; xi1++;
        }
   
 /* Reverse Step 2 */
        xr1 = x0 + m2 + m4;
        for (n = 0; n < m4; n++) {
            x[xr1] = - x[xr1];
            xr1++;
        }
   
 /* Reverse  Step  1 */
        xr1 = x0; xr2 = xr1 + m2;
        for (n = 0; n < m2; n++) {
            tmp1 = x[xr1] + x[xr2];
            x[xr2] = x[xr1] - x[xr2];
            x[xr1] = tmp1;
            xr1++; xr2++;
        }
    }
   
   
  /* -------------------------------------------------------------------- *  
 *      Recursive part of the SRFFT algorithm.                          *  
 * -------------------------------------------------------------------- */

    void srrec(float x[],int xr, int xi, int logm)
    {
        int        m, m2, m4, m8, nel, n;
        // int        x0=0;
        int        xr1, xr2, xi1, xi2;
        int        cn, spcn, smcn, c3n, spc3n, smc3n;
        float      tmp1, tmp2;
        cn=0; spcn=0; smcn=0; c3n=0; spc3n=0; smc3n=0;
   
   
   
   
/* Check range of logm */
        try
        {if ((logm < 0) || (logm > MAXLOGM)) {
            System.err.println("FFT length m is too big: log2(m) = "+logm+"is out of bounds ["+0+","+MAXLOGM+"]");

            throw new OutofborderException(logm) ;
        }
        }
        catch ( OutofborderException e)
        {throw new OutOfMemoryError();}
   
/*  Compute trivial cases */
        if (logm < 3) {
            if (logm == 2) {  /* length m = 4 */
                xr2  = xr + 2;
                xi2  = xi + 2;
                tmp1 = x[xr] + x[xr2];
                x[xr2] = x[xr] - x[xr2];
                x[xr]  = tmp1;
                tmp1 = x[xi] + x[xi2];
                x[xi2] = x[xi] - x[xi2];
                x[xi]  = tmp1;
                xr1  = xr + 1;
                xi1  = xi + 1;
                xr2++;
                xi2++;
                tmp1 = x[xr1] + x[xr2];
                x[xr2] = x[xr1] - x[xr2];
                x[xr1] = tmp1;
                tmp1 = x[xi1] + x[xi2];
                x[xi2] = x[xi1] - x[xi2];
                x[xi1] = tmp1;
                xr2  = xr + 1;
                xi2  = xi + 1;
                tmp1 = x[xr] + x[xr2];
                x[xr2] = x[xr] - x[xr2];
                x[xr]  = tmp1;
                tmp1 = x[xi] + x[xi2];
                x[xi2] = x[xi] - x[xi2];
                x[xi]  = tmp1;
                xr1  = xr + 2;
                xi1  = xi + 2;
                xr2  = xr + 3;
                xi2  = xi + 3;
                tmp1 = x[xr1] + x[xi2];
                tmp2 = x[xi1] + x[xr2];
                x[xi1] = x[xi1] - x[xr2];
                x[xr2] = x[xr1] - x[xi2];
                x[xr1] =tmp1;
                x[xi2] =tmp2;
                return;
            }

            else  if (logm == 1) { /* length m = 2 */
                xr2   = xr +  1;
                xi2   = xi +  1;
                tmp1  = x[xr] + x[xr2];
                x[xr2]  = x[xr] - x[xr2];
                x[xr]   = tmp1;
                tmp1  = x[xi] + x[xi2];
                x[xi2]  = x[xi] - x[xi2];
                x[xi]   = tmp1;
                return;
            }
            else if (logm == 0) return;     /* length m = 1*/
        }
   
/* Compute a few constants */
        m = 1 << logm; m2 = m / 2; m4 = m2 / 2; m8 = m4 / 2;
   
/* Build tables of butterfly coefficients, if necessary */
//if ((logm >= 4) && (tab[logm-4] == NULL)) {   
   
    /* Allocate memory for tables */   
  /*  nel = m4 - 2;  
    if ((tab[logm-4] = (float *) calloc(6 * nel, sizeof(float)))  
       == NULL) {  
       exit(1);  
     }  
    /* Initialize pointers */   
   
    /*cn  = tab[logm-4]; spcn = cn + nel;  smcn = spcn + nel;  
    c3n = smcn + nel; spc3n = c3n + nel; smc3n = spc3n + nel;  
  
  
    /* Compute tables */   
    /*for (n = 1; n < m4; n++) {  
       if (n == m8) continue;  
       ang = n * TWOPI / m;  
       c = cos(ang); s = sin(ang);  
       *cn++ = c; *spcn++ = - (s + c); *smcn++ = s - c;  
       ang = 3 * n * TWOPI / m;  
       c = cos(ang); s = sin(ang);  
       *c3n++ = c; *spc3n++ = - (s + c); *smc3n++ = s - c;  
   }  
}  
  
  
/*  Step 1 */
        xr1 = xr;  xr2 = xr1  +  m2;
        xi1 = xi;  xi2 = xi1  +  m2;

        for (n = 0; n < m2; n++) {
            tmp1 = x[xr1] + x[xr2];
            x[xr2] = x[xr1] - x[xr2];
            x[xr1] = tmp1;
            tmp2 = x[xi1] + x[xi2];
            x[xi2] = x[xi1] - x[xi2];
            x[xi1] = tmp2;
            xr1++;  xr2++;  xi1++;  xi2++;
        }
/*   Step 2  */
        xr1 = xr + m2; xr2 = xr1 + m4;
        xi1 = xi + m2; xi2 = xi1 + m4;
        for (n = 0; n < m4; n++) {
            tmp1 = x[xr1] + x[xi2];
            tmp2 = x[xi1] + x[xr2];
            x[xi1] = x[xi1] - x[xr2];
            x[xr2] = x[xr1] - x[xi2];
            x[xr1] = tmp1;
            x[xi2] = tmp2;
            xr1++;  xr2++;  xi1++;  xi2++;
        }
   
/*   Steps  3 & 4 */
        xr1 = xr + m2; xr2 = xr1 + m4;
        xi1 = xi + m2; xi2 = xi1 + m4;
        if (logm >= 4) {
            nel = m4 - 2;
            cn  = 0; spcn  = cn + nel;  smcn  = spcn + nel;
            c3n = smcn + nel;  spc3n = c3n + nel; smc3n = spc3n + nel;
        }
        xr1++; xr2++; xi1++; xi2++;
        for (n = 1; n < m4; n++) {
            if (n == m8) {
                tmp1 = (float)(SQHALF * (x[xr1] + x[xi1]));
                x[xi1] = (float)(SQHALF * (x[xi1] - x[xr1]));
                x[xr1] = tmp1;
                tmp2 = (float)(SQHALF * (x[xi2] - x[xr2]));
                x[xi2] = (float)(-SQHALF * (x[xr2] + x[xi2]));
                x[xr2] = tmp2;
            }     else {
                tmp2 = tab[logm-4][cn++] * (x[xr1] + x[xi1]);
                tmp1 = tab[logm-4][spcn++] * x[xr1] + tmp2;
                x[xr1] = tab[logm-4][smcn++] * x[xi1] + tmp2;
                x[xi1] = tmp1;
                tmp2 = tab[logm-4][c3n++] * (x[xr2] + x[xi2]);
                tmp1 = tab[logm-4][spc3n++] * x[xr2] + tmp2;
                x[xr2] = tab[logm-4][smc3n++] * x[xi2] + tmp2;
                x[xi2] = tmp1;
            }
            // System.out.println ("logm-4="+(logm-4));
            xr1++; xr2++; xi1++; xi2++;
        }
   /* Call ssrec again with half DFT length  */
        srrec(x,xr, xi, logm-1);
   
   /* Call ssrec again twice with one quarter DFT length.  
     Constants have to be recomputed, because they are static!*/
        m = 1 << logm; m2 = m / 2;
        srrec(x,xr + m2, xi + m2, logm-2);
        m = 1 << logm; m4 = 3 * (m / 4);
        srrec(x,xr + m4, xi + m4, logm-2);
    }
   
/* -------------------------------------------------------------------- *  
 *    Data unshuffling according to bit-reversed indexing.              *  
 *                                                                      *  
 *                                                                      *  
 *    Bit reversal is done using Evan's algorithm (Ref: D. M. W.        *  
 *    Evans, "An improved digit-reversal permutation algorithm...",     *  
 *    IEEE Trans.  ASSP, Aug. 1987, pp. 1120-1125).                     *  
 * -------------------------------------------------------------------- */

//static    int   brseed[256];     /* Evans' seed table */   
//static    int     brsflg;         /* flag for table building */   


    void creatbrseed( int logm)
    {int lg2,n;
        lg2 = logm >> 1;
        n =1 << lg2;
        if (logm!=(logm>>1)<<1) lg2++;
        brseed[0] = 0;
        brseed[1] = 1;
        for  (int j=2; j <= lg2; j++) {
            int imax = 1 << (j-1);
            for (int i = 0; i < imax; i++) {
                brseed[i] <<= 1;
                brseed[i + imax] = brseed[i] + 1;
            }
        }
    }
    void BR_permute(float x[], int logm)
    {
        int       i, j, imax, lg2, n;
        int       off, fj, gno;
        float     tmp;
        int       xp, xq, brp;
        int       x0=0;

        lg2 =  logm >> 1;
        n = 1  << lg2;
        if  (logm !=(logm>>1)<<1) lg2++;
   
   /*  Create seed table if not yet built */   
  /* if  (brsflg != logm) {  
       brsflg = logm;  
       brseed[0] = 0;  
       brseed[1] = 1;  
       for  (j=2; j <= lg2; j++) {  
          imax = 1 << (j-1);  
          for (i = 0; i < imax; i++) {  
             brseed[i] <<= 1;  
             brseed[i + imax] = brseed[i] + 1;  
          }  
       }  
   }*/
        creatbrseed(logm);
   
    /*  Unshuffling   loop */
        for (off = 1; off < n; off++) {
            fj = n * brseed[off]; i = off; j = fj;
            tmp = x[i]; x[i] = x[j]; x[j] = tmp;
            xp = i;
            brp = 1;

            for (gno = 1; gno < brseed[off]; gno++) {
                xp += n;
                j = fj + brseed[brp++];
                xq = x0 + j;
                tmp = x[xp]; x[xp] = x[xq]; x[xq] = tmp;
            }
        }

    }
    class OutofborderException extends Exception {
        public OutofborderException (int logm)
        {super();
        }
    }
}   