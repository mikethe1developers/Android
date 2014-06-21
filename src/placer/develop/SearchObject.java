package placer.develop;

public class SearchObject {
	
	private Double latitude;
	private Double longitude;
	private Float bearing;
	private Float axisX;
	private Float axisY;
	private Float axisZ;
	
	SearchObject(Double lat, Double lon, Float bearing, Float x, Float y, Float z) {
		this.latitude = lat;
		this.longitude = lon;
		this.bearing = bearing;
		this.axisX = x;
		this.axisY = y;
		this.axisZ = z;
	}
	
	Double getLatitude() {
		return this.latitude;
	}
	
	Double getLongitude() {
		return this.longitude;
	}
	
	Float getBearing() {
		return this.bearing;
	}
	
	Float getX() {
		return this.axisX;
	}
	
	Float getY() {
		return this.axisY;
	}
	
	Float getZ() {
		return this.axisZ;
	}
	
	void setLatitude(Double lat) {
		this.latitude = lat;
	}
	
	void setLongitude(Double lon) {
		this.longitude = lon;
	}
	
	void setBearing(Float bearing) {
		this.bearing = bearing;
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