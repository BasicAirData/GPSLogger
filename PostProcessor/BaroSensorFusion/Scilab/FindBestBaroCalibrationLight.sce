//FindBestBaroCalibrationLight  
//Script that find best calibration parameter between a reduced dataset from source file, to be used with DataDisplay script
//Test on barometer data for GPS Logger, based on pdf BAROMETRIC AND GPS ALTITUDE SENSOR FUSION
//Vadim Zaliva and Franz Franchetti  Carnegie Mellon University (1)
//and https://www.basicairdata.eu/attachments/blog/scilab/post18/barometricr2.sce script
//Scilab File 6.1.1  JL @ Basicairdata.eu 
//The barometer is zeroed with the altitude at the best found point between a reduced set
//BAROP Array of measured pressures
//BAROA Array with calibrated barometric altitude
//GPSA GPS recorded value
clear
calibrated=0; //Not calibrated
function F=isaaltitude(x)
    F=pressure_reading-p0isa*(1-0.0065*x/T0)^(g/Rair/0.0065);
endfunction
function C=isacalib(x)
    C=pressure_reading-x*(1-0.0065*hm/T0)^(g/Rair/0.0065);
endfunction
//ISA atmosphere
p0isa=101325 //Pa ISA standard
T0=288.15//Â°K ISA STANDARD
g=9.80665;
R=8.314462175//J/K/mol
Mwa=28.9644;//kg/1000/mol
Rair=R*1000/Mwa;
DownSamplingFactor =10 //2, one sample is analized every two measured values


DI = csvRead('input.txt'); //Load csv file
[rDI cDI]=size(DI)
GPSA=DI(2:rDI,6); //GPS Altitude column  <- To be changed
BAROP=100*DI(2:rDI,12) //Barometer pressure column [Pa]  
BAROPDOWN=BAROP(1:DownSamplingFactor:length(BAROP))
GPSADOWN=GPSA(1:DownSamplingFactor:length(GPSA))
[rs cs] =size(BAROPDOWN)
nsample=1:rs; //Number of discrete samples
JO=0; //merit figure old value
J=0; //merif figure for correspondance between barometric and GPS Altitude
ibest=0;//index of best value
for i =1:rs  //Shoul find  best  calibration  sample
    hm=GPSA(i,1); //Altitudine GPS
    pressure_reading=BAROPDOWN(i,1) //Pressione Barometrica
    p0isacalibrato=fsolve(0,isacalib)
    //calculate  merit figure  for  present calibration  sample  J= SUMi(DIFF(i)^2)
    p0isa=p0isacalibrato
    //Calculate altitude vector for picked calibration data
    J=0;
    for j =1:rs
        pressure_reading=BAROPDOWN(j,1)
        BAROA(j,1)= fsolve(0,isaaltitude)
        DIFF(j,1)=BAROA(j,1)-GPSADOWN(j,1)
        J=J+DIFF(j,1)^2
        advance=i/rs*100
        printf("Calibration %.1f %% completed\n",advance)
    end
    if i==1
        JO=J;
    end

    if J<JO
        JO=J;    //Update best result
        ibest=i; 
        p0isacalibrato1=p0isacalibrato
        BAROAC=BAROA
        DIFF=BAROAC-GPSADOWN
    end
end    
printf("Best data sample for calibration is %.0f\n",ibest)
calibrated=1;







