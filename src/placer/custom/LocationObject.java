package placer.custom;

/**
 * 3D location point that takes into consideration both terrain elevation & AGL.
 * Will be used in both cases:
 *   1. To determine phone location, then the height (AGL) is a constant average of 1.5 meters.
 *   2. To calculate 3D LOS vector, then the height (AGL) is calculated via trigonometry and for each value 1.5 constant is added here automatically. 
 */

public class LocationObject {
	
	public static final double BASE_AGL = 1.5;
	public double latitude;
	public double longitude;
	public double elevation;	//Above MSL
	public double height;		//AGL	
	
	public LocationObject(double lat, double lon, double elev, double height) {
		this.latitude = lat;
		this.longitude = lon;
		this.elevation = elev;
		this.height = height + BASE_AGL;
	}
	
	public void setLatitude(double lat) {
		this.latitude = lat;
	}
	
	public void setLongitude(double lon) {
		this.longitude = lon;
	}
	
	public void setElevation(double elev) {
		this.elevation = elev;
	}
	
	public double getLatitude() {
		return this.latitude;
	}
	
	public double getLongitude() {
		return this.longitude;
	}
	
	public double getElevation() {
		return this.elevation;
	}
	
}