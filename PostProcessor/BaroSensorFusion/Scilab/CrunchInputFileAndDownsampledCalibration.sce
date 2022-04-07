//CrunchInputFileAndDownsampledCalibration
//Script that run barometer calibration FindBestBaroCalibrationLight.sce(set DownSamplingFactor variable in FindBestBaroCalibrationLight.sce) and sensor fusion  DisplayData.sce on input.txt data file from GPSLogger
//Scilab File 6.1.1  JL @ Basicairdata.eu 
clear
exec('FindBestBaroCalibrationLight.sce',-1);
exec('DisplayData.sce',-1);
