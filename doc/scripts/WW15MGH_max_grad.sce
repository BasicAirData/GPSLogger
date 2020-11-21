fd1 = mopen('./WW15MGH.DAC','rb', 0);
x = mget(1440*721, 'sb', fd1);
mclose(fd1);

grid = zeros (721, 1440);
for clat = 1:721;
    for clong = 1:1440;
        grid(clat, clong) = x((clat-1) * 1440 + clong);
    end;
end;

a = zeros (4);
maxgrad = 0;
maxlong = 0;
maxlat = 0;
gradgrid = zeros (721, 1440);
for clat = 1:720;
    for clong = 1:1439;
        a(1) = abs(grid(clat, clong) - grid(clat+1, clong));
        a(2) = abs(grid(clat, clong) - grid(clat, clong+1));
        a(3) = abs(grid(clat, clong) - grid(clat+1, clong+1)) / sqrt(2);
        if (clong == 1) then
            a(4) = abs(grid(clat, clong) - grid(clat+1, 1440)) / sqrt(2);
        else
            a(4) = abs(grid(clat, clong) - grid(clat+1, clong-1)) / sqrt(2);
        end
        
        gradgrid(clat, clong) = max(a);
        
        if gradgrid(clat, clong) > maxgrad then;
            maxgrad = gradgrid(clat, clong);
            maxlong = clong;
            maxlat = clat;
            disp (maxgrad, [maxlat maxlong]);
        end;
    end;
end;

// Extents of 3D graph
glat1 = maxlat - 50;
if glat1<1 then;
    glat1 = 1;
end;

glong1 = maxlong - 100;
if glong1<1 then;
    glong1 = 1;
end;

glat2 = maxlat + 50;
if glat2>721 then;
    glat2 = 721;
end;

glong2 = maxlong + 100;
if glong2>1440 then;
    glong2 = 1440;
end;

vlat = linspace(90-glat1*0.25, 90-glat2*0.25, (glat2-glat1)+1);
vlong = linspace(glong1*0.25, glong2*0.25, (glong2-glong1)+1);
xset("colormap",jetcolormap(64));
surf(vlong, vlat, grid(glat1:glat2, glong1:glong2));
xlabel('Longitude');
ylabel('Latitude');
zlabel('Altitude Correction');
