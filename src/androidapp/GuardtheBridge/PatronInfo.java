package androidapp.GuardtheBridge;

import java.io.Serializable;

public class PatronInfo implements Serializable {

	/**
	 * Generated SerVersID
	 */
	private static final long serialVersionUID = 7039685816067074222L;
	private String name, pickup, dropoff, phone, status, notes, timeTaken, timeAssigned, timeDone;
	private int passangers, pid;
	
	public PatronInfo (String val[], int val1[]){		
			name = val[0];
			passangers = val1[0];
			pickup = val[1];
			pid = val1[1];
			dropoff = val[2];
			phone = val[3];
			status = val[4];
			notes = val[5];
			timeTaken = val[6];
			timeAssigned = val[7];
			timeDone = val[8];
	}
	
	/*
	 * ACCESSORS
	 */
	
	public String getName(){
		return name;
	}
	
	public String getPickUp(){
		return pickup;
	}
	
	public String getDropOff(){
		return dropoff;
	}
	
	public String getPhone(){
		return phone;
	}
	
	public String getStatus(){
		return status;
	}
	
	public String getNotes(){
		return notes;
	}
	
	public String getTimeTakes(){
		return timeTaken;
	}
	
	public String getTimeAssigned(){
		return timeAssigned;
	}
	
	public String getTimeDone(){
		return timeDone;
	}
	
	public int getPassangers(){
		return passangers;
	}
	
	public int getPid(){
		return pid;
	}
	
	/*
	 * MUTATORS
	 */
	
	public void setName(String _name){
		name = _name;
	}
	
	public void setPickUp(String _pickup){
		pickup = _pickup;
	}
	
	public void setDropOff(String _dropoff){
		dropoff = _dropoff;
	}
	
	public void setPhone(String _phone){
		phone = _phone;
	}
	
	public void setStatus(String _status){
		status = _status;
	}
	
	public void setNotes(String _notes){
		notes = _notes;
	}
	
	public void setTimeTakes(String _timeTaken){
		timeTaken = _timeTaken;
	}
	
	public void setTimeAssigned(String _timeAssigned){
		timeAssigned = _timeAssigned;
	}
	
	public void setTimeDone(String _timeDone){
		timeDone = _timeDone;
	}
	
	public void setPassangers(int _pass){
		passangers = _pass;
	}
	
	public void setPid(int _pid){
		pid = _pid;
	}
	
}
