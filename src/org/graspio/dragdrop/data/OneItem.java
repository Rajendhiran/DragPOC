
package org.graspio.dragdrop.data;

public class OneItem {

	private int id,type;
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	private String name;


	public OneItem(OneItem op){
		this.id = op.id;
		this.name = op.name;
		this.type = op.type;
	}

	public OneItem(int id, String name, int type) {
		this.id = id;
		this.name = name;
		this.type=type;

	}

	public int getId(){
		return this.id;
	}

	public String getName(){
		return this.name;
	}


}
