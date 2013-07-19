package org.t2.dataouthandler.classes;

public class SqlPacket {

	private String packetId = "";
	private String packet = "";
	private String recordId = "";
	private String drupalId = "";

	
	public String getPacket() {
		return packet;
	}


	public String getPacketId() {
		return packetId;
	}


	public void setPacketId(String packetId) {
		this.packetId = packetId;
	}


	public String getDrupalId() {
		return drupalId;
	}


	public void setDrupalId(String drupalId) {
		this.drupalId = drupalId;
	}


	public void setPacket(String packet) {
		this.packet = packet;
	}


	public String getRecordId() {
		return recordId;
	}


	public void setRecordId(String recordId) {
		this.recordId = recordId;
	}


	public SqlPacket() {
	}
	
	public String toString() {
		return "packetId: " + packetId + ", recordId: " + recordId + ", drupalId: " + drupalId + ", packet: " + packet;
	}

}
