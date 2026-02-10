package com.tarron.marketsim.model;

public class Item {
	
	private final String itemName;
	private final double price;
	private final int quality;
	
	public Item(String name, double price, int quality) {
		this.itemName = name;
		this.price = price;
		this.quality = quality;
	}
	
	public double getPrice() {
		return price;
	}
	
	public String getName() {
		return itemName;
	}
	
	public int getQuality() {
		return quality;
	}
	
	@Override
	public boolean equals(Object obj) {
	    if (this == obj) return true;
	    if (obj == null || getClass() != obj.getClass()) return false;

	    Item other = (Item) obj;

	    if (quality != other.quality) return false;
	    if (Double.compare(other.price, price) != 0) return false;
	    return itemName != null ? itemName.equals(other.itemName) : other.itemName == null;
	}

	@Override
	public int hashCode() {
	    int result = (itemName != null) ? itemName.hashCode() : 0;
	    long temp = Double.doubleToLongBits(price);
	    result = 31 * result + (int) (temp ^ (temp >>> 32));
	    result = 31 * result + quality;
	    return result;
	}
}
