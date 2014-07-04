package placer.custom;

public class OrientationObject {
	
	public double pitch;
	public double roll;
	public double azimuth;
	
	public OrientationObject(double pitch, double roll, double azimuth) {
		this.pitch = pitch;
		this.roll = roll;
		this.azimuth = azimuth;
	}
	
	public void setPitch(double pitch) {
		this.pitch = pitch;
	}
	
	public void setRoll(double roll) {
		this.roll = roll;
	}
	
	public void setAzimuth(double azimuth) {
		this.azimuth = azimuth;
	}
	
	public double getPitch() {
		return this.pitch;
	}
	
	public double getRoll() {
		return this.roll;
	}
	
	public double getAzimuth() {
		return this.azimuth;
	}
	
}