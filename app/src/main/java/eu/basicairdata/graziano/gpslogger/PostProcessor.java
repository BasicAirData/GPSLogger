package eu.basicairdata.graziano.gpslogger;

/**  JL @ BasicAirData.eu
 * Helper class to post process data from GPSLogger track DB
 */
//import org.ejml.simple.SimpleMatrix; <- EJML non used right now
public class PostProcessor {
    /**
     * FindBestBaroCalibrationLight returns the best barometric altimeter initialization sample number based on minimum cumulative error
     * @param GPSA  Measured GPS altitude array [m]
     * @param BAROP Measured barometric pressure array [Pa]
     * @return ibest, index of best barometer calibration sample [Int index]
     */
    public int FindBestBaroCalibrationLight (float[] GPSA, float[] BAROP) {
        //ISA atmosphere
        int ibest=0;
        float[]BAROA=new float[BAROP.length];
        float[]DIFF=new float[BAROP.length];
        float p0isa=101325f; //Pa ISA standard
        float T0=288.15f;//°K ISA STANDARD
        float g=9.80665f;
        float R=8.314462175f ;//J/K/mol
        float Mwa = 28.9644f;//kg/1000/mol
        float Rair=R*1000/Mwa;
        float DownSamplingFactor =10f; //2, one sample is analized every teen measured values
        int rs = BAROP.length;//Number of pressure samples samples
        float hm=0f;//GPS Altitude measurements
        float pressure_reading=0f;//Pressure Measurement
        float p0isacalibrato;//Pressure calibrated value
        float J,JO =0; //Merit figure
        for (int i=0;i<rs;i++){
            hm=GPSA[i];
            p0isacalibrato = (float) (pressure_reading/(Math.pow((1f-0.0065f*hm/T0),(g/Rair/0.0065f))));
            //calculate  merit figure  for  present calibration  sample  J= SUMi(DIFF(i)^2)
            p0isa=p0isacalibrato; //Calculate altitude vector for picked calibration data
            J=0;
            for (int j=0;j<rs;j++) {
                pressure_reading = BAROP[j];
               BAROA[j]= -1*(float) ((Math.pow((pressure_reading/p0isa),(1/(g/Rair/0.0065f)))-1f)/0.0065f*T0);
               DIFF[j]=BAROA[j]-GPSA[j];
               J += Math.pow(DIFF[j], 2);
            }
            if (i==1) {
                JO = J;
            }
            if (J<JO) {
                JO = J;   //Update best result
                ibest=i;
            }
        }
        return ibest;
    }
    public float[] CorrectBaroCurve(float[] GPSA,float[]BAROP,int m,int ibest) {
        float[] BAROA = new float[BAROP.length];
        float[] CORRA = new float[BAROP.length];  //Corrected barometric altitude curve
        float p0isa = 101325f; //Pa ISA standard
        float T0 = 288.15f;//°K ISA STANDARD
        float g = 9.80665f;
        float R = 8.314462175f;//J/K/mol
        float Mwa = 28.9644f;//kg/1000/mol
        float Rair = R * 1000 / Mwa;
        float DownSamplingFactor = 10f; //2, one sample is analized every two measured values
        int rs = BAROP.length;//Number of pressure samples samples
        float hm = 0f;//GPS Altitude measurements
        float pressure_reading = 0f; //Pressure Measurement
        float p0isacalibrato; //Pressure calibrated value
        float Mediab = 0f; //Average barometric pressure
        float Mediag = 0f; //Average GPS altitude
        float delta = 0f; //Correction factor for altitude

        hm = GPSA[ibest]; //GPS Altitude for calibration
        pressure_reading = BAROP[ibest];
        //Get best calibration from indicated sample
        p0isacalibrato = (float) (pressure_reading / (Math.pow((1f - 0.0065f * hm / T0), (g / Rair / 0.0065f))));
        p0isa = p0isacalibrato; //Used caliubrated value
        for (int i = 0; i < rs; i++) {  //Calculate borometric altitude array
            pressure_reading = BAROP[i];
            BAROA[i] = -1 * (float) ((Math.pow((pressure_reading / p0isa), (1 / (g / Rair / 0.0065f))) - 1f) / 0.0065f * T0);
        }
        Mediab = 0;
        Mediag = 0;
        for (int j = m; j < rs; j++) {
            //Calculate Mediab
            for (int k = (j - m + 1); k < (j + 1); k++) {
                Mediab = Mediab + BAROA[k];
            }
            Mediab = Mediab / m;
            //Calculate Mediag
            for (int k = (j - m + 1); k < (j + 1); k++) {
                Mediag = Mediag + GPSA[k];
            }
            Mediag = Mediag / m;
            delta = Mediab - Mediag;
            CORRA[j] = BAROA[j] - delta;
        }
        return CORRA;
    }
}
