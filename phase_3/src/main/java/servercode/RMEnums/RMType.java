package servercode.RMEnums;

public enum RMType {

    CAR("Car"),
    FLIGHT("Flight"),
    ROOM("Room");
    
    private String name;
    
    RMType(String name) {
	    this.name = name;
    }
    
    public String toString() {
	    return this.name;
    }

}
