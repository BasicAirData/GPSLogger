//This is a test!
//DisplayData Script. Display input file
//Test on barometer data for GPS Logger, based on pdf BAROMETRIC AND GPS ALTITUDE SENSOR FUSION
//Vadim Zaliva and Franz Franchetti  Carnegie Mellon University (1)
//and https://www.basicairdata.eu/attachments/blog/scilab/post18/barometricr2.sce script
//Scilab File 6.1.1  JL @ Basicairdata.eu 

//The barometer is zeroed with the altitude of the first point
//BAROA Array with calibrated barometric altitude
//BAROP Array with meaured pressures
//GPSA GPS recorded values
clear
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
tsample=1/3600; //Sample time in hours
ncampionecalibrazionebaro=2159;  //  <----- Value to update with ibest attained with FindBestBaroCalibration.sce
m=20; //Windows for correction, the same for GPS and Barosensor (same data rate)
D=1;//Coverage factor D*sigma for corrected altitude 
ph=40000;//Maximum pressure drift in one hour
//filenameinput="inputtest_step.txt" //Name of the test data file with step
//filenameinput="inputtest.txt" //Name of the test data file with costant inputs
filenameinput="input.txt" //Name of the test data file with costant inputs
DI = csvRead(filenameinput); //Load csv file
[rDI cDI]=size(DI)
GPSAF=DI(2:rDI,6); //GPS Altitude values column
//Low pass filter GPS signal
h = [0 1/20 1/20 1/20 1/20 1/20 1/20 1/20 1/20 1/20 1/20 1/20 1/20 1/20 1/20 1/20 1/20 1/20 1/20 1/20 1/20];
GPSA = filter(h, 1, GPSAF)
//GPSA=GPSAF //To remove filter
BAROP=100*DI(2:rDI,12) //Barometer pressure values column [Pa]
hm=GPSA(ncampionecalibrazionebaro,1); //GPS Altitude for calibration
pressure_reading=BAROP(ncampionecalibrazionebaro,1) //barometric pressure for calibration
p0isacalibrato=fsolve(0,isacalib)
p0isa=p0isacalibrato
//Temporary disable calibration   <--------------------------------
//p0isa=101325 //Pa ISA standard  <--------------------------------
[rs cs] =size(BAROP)
for i =1:rs
    pressure_reading=BAROP(i,1)
    BAROA(i,1)= fsolve(0,isaaltitude)
end
nsample=1:rs; //Number of discrete samples
//Plotting 
DIFF=BAROA-GPSA;

for i =(m+1):(rs)
    Mediab=mean(BAROA((i-m+1):i,1))
    Mediag=mean(GPSA((i-m+1):i,1))
    delta=Mediab-Mediag;
    CORRA(i,1)=BAROA(i,1)-delta;
    //Calculate variance of corrected value
    Varb=variance(BAROA((i-m+1):i,1))
    Varg=variance(GPSA((i-m+1):i,1))
    VarCorrected=(Varb+Varb/m+Varg/m)^0.5
    //Calculate maximun variation of barometric pressure
    //time = sample interval * m
    tdrift=tsample*m
    deltab=max(BAROA(i,1)*(p0isa-tdrift*ph/3600)-BAROA(i,1)*p0isa ,BAROA(i,1)*p0isa-BAROA(i,1)*(p0isa-tdrift*ph/3600))
    AbsLim=D*VarCorrected+deltab/2
    SUPLIM(i,1)=CORRA(i,1)+AbsLim;
    INFLIM(i,1)=CORRA(i,1)-AbsLim;        
end
CORRA(1:m,1)=CORRA(m+1,1)

scf(1);
plot2d(nsample,[BAROA,GPSA],[1 2 ])
[rCORRA cCORRA]=size(CORRA);
plot2d(nsample,CORRA,[5])
errbar(nsample, CORRA, AbsLim, AbsLim)
legends(['Baro Altitude [m]';'GPS Altitude[m]';'Corrected[m]'],[1 2 5],opt="lr")
xgrid
xtitle("Barometric Altitude vs GPS Altitude m value is " + string(m) +" Data file "+string(filenameinput))
scf(2);
plot2d(nsample,[DIFF],[3])
legends(['Altitude difference [m]]'],[1],opt="lr")
xgrid
xtitle("BARO measured altitude and GPS Altitude differences, used sample" + string(ncampionecalibrazionebaro)+" for barometer calibration"+" data file "+string(filenameinput))









