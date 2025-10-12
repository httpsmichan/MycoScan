package com.example.myapplication;

import org.osmdroid.util.GeoPoint;
import java.util.ArrayList;
import java.util.List;

public class DavaoGeoFence {

    private static final double[][] DAVAO_BOUNDARY = {
            {6.963546318000056, 125.4843038470001},
            {6.984360566000021, 125.48904228300012},
            {7.039341186000057, 125.53634369500003},
            {7.045080675000065, 125.60752460900005},
            {7.121591885000043, 125.66294121700003},
            {7.212164260000065, 125.6465141000001},
            {7.261418147000027, 125.66790772400009},
            {7.264272240000025, 125.623197376},
            {7.223487898000031, 125.58313921400008},
            {7.242435550000036, 125.5522667990001},
            {7.345220359000052, 125.54851244200005},
            {7.412268592000032, 125.55773789200008},
            {7.425852213000041, 125.543806366},
            {7.565447, 125.5436054380001},
            {7.564791229000035, 125.26559191100012},
            {7.506819022000058, 125.26359983300007},
            {7.500939476000043, 125.23062998900004},
            {7.428194494000024, 125.2325044490001},
            {7.374365237000062, 125.27566577000005},
            {7.356423014000029, 125.23120292600004},
            {7.259347723000076, 125.255211141},
            {7.210867900000039, 125.22347184200011},
            {7.175858360000063, 125.22876936500006},
            {7.105673260000061, 125.29163036900002},
            {7.03411257700003, 125.29176710700006},
            {7.029416808000064, 125.25922056900002},
            {7.000539738000044, 125.24862393400008},
            {6.985103854000045, 125.2724863730001},
            {6.960936564000063, 125.46757875500009},
            {6.963546318000056, 125.4843038470001}
    };

    public static List<GeoPoint> getBoundary() {
        List<GeoPoint> boundaryPoints = new ArrayList<>();
        for (double[] coord : DAVAO_BOUNDARY) {
            boundaryPoints.add(new GeoPoint(coord[0], coord[1]));
        }
        return boundaryPoints;
    }

    public static boolean isInsideDavao(double lat, double lng) {
        boolean inside = false;
        int n = DAVAO_BOUNDARY.length;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double latI = DAVAO_BOUNDARY[i][0];
            double lngI = DAVAO_BOUNDARY[i][1];
            double latJ = DAVAO_BOUNDARY[j][0];
            double lngJ = DAVAO_BOUNDARY[j][1];

            if ((lngI > lng) != (lngJ > lng) &&
                    lat < (latJ - latI) * (lng - lngI) / (lngJ - lngI) + latI) {
                inside = !inside;
            }
        }
        return inside;
    }
}

