package placer.custom;

public class SearchObject {

	private String id = "";  //ID of the device who sent
	private double latitude;
	private double longitude;
	private double altitude;
	private float axisX;
	private float axisY;
	private float axisZ;
	
	public SearchObject(String id, double lat, double lon, double altitude, float x, float y, float z) {
		this.id = id;
		this.latitude = lat;
		this.longitude = lon;
		this.altitude = altitude;
		this.axisX = x;
		this.axisY = y;
		this.axisZ = z;
	}
	
	String getId() {
		return this.id;
	}
	
	double getLatitude() {
		return this.latitude;
	}
	
	double getLongitude() {
		return this.longitude;
	}
	
	double getAltitude() {
		return this.altitude;
	}
	
	float getX() {
		return this.axisX;
	}
	
	float getY() {
		return this.axisY;
	}
	
	float getZ() {
		return this.axisZ;
	}
	
	void setId(String id) {
		this.id = id;
	}
	
	void setLatitude(Double lat) {
		this.latitude = lat;
	}
	
	void setLongitude(Double lon) {
		this.longitude = lon;
	}
	
	void setAltitude(Double altitude) {
		this.altitude = altitude;
	}
	
	void setAxisX(Float x) {
		this.axisX = x;
	}
	
	void setAxisY(Float y) {
		this.axisY = y;
	}
	
	void setAxisZ(Float z) {
		this.axisZ = z;
	}
	
}